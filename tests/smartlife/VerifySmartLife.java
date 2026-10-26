import com.android.tools.smali.dexlib2.DexFileFactory;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/** Run with Morphe Desktop's all.jar on the classpath: original.apk patched.apk selectionMask. */
public final class VerifySmartLife {
    private static final String EXT = "Lapp/morphe/extension/smartlife/";
    private static final String[] OPTIONS = {"homePlus", "homeRecommendations", "aiInputBar",
            "intelligenceRecommendations", "explorerTab", "tuyaWeb", "benefits"};

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private record ApkData(Map<String, ClassDef> classes, Map<String, Integer> counts) {}

    private static ApkData classes(String apk) throws Exception {
        var container = DexFileFactory.loadDexContainer(new File(apk), Opcodes.forApi(24));
        var result = new HashMap<String, ClassDef>();
        var counts = new HashMap<String, Integer>();
        for (var entry : container.getDexEntryNames()) {
            for (var cls : container.getEntry(entry).getDexFile().getClasses()) {
                result.putIfAbsent(cls.getType(), cls);
                counts.merge(cls.getType(), 1, Integer::sum);
            }
        }
        return new ApkData(result, counts);
    }

    private static Method method(Map<String, ClassDef> classes, String owner, String name) {
        for (var method : classes.get(owner).getMethods()) if (method.getName().equals(name)) return method;
        throw new AssertionError(owner + "->" + name);
    }

    private static boolean resolves(Map<String, ClassDef> classes, String owner, MethodReference ref, Set<String> seen) {
        if (owner == null || !seen.add(owner)) return false;
        ClassDef cls = classes.get(owner);
        if (cls == null) return false;
        for (Method method : cls.getMethods()) {
            if (method.getName().equals(ref.getName()) && method.getReturnType().equals(ref.getReturnType())
                    && method.getParameterTypes().equals(ref.getParameterTypes())) return true;
        }
        if (resolves(classes, cls.getSuperclass(), ref, seen)) return true;
        for (String it : cls.getInterfaces()) if (resolves(classes, it, ref, seen)) return true;
        return false;
    }

    private static void verifyCode(Method method) {
        var body = method.getImplementation();
        if (body == null) return;
        int count = body.getRegisterCount(), address = 0;
        var addresses = new HashSet<Integer>();
        var branches = new ArrayList<Integer>();
        for (var instruction : body.getInstructions()) {
            addresses.add(address);
            if (instruction instanceof OneRegisterInstruction x) check(x.getRegisterA() < count, "Register A out of range: " + method);
            if (instruction instanceof TwoRegisterInstruction x) check(x.getRegisterB() < count, "Register B out of range: " + method);
            if (instruction instanceof ThreeRegisterInstruction x) check(x.getRegisterC() < count, "Register C out of range: " + method);
            if (instruction instanceof FiveRegisterInstruction x) {
                int[] args = {x.getRegisterC(), x.getRegisterD(), x.getRegisterE(), x.getRegisterF(), x.getRegisterG()};
                for (int i = 0; i < x.getRegisterCount(); i++) check(args[i] < count, "Invoke register out of range: " + method);
            }
            if (instruction instanceof RegisterRangeInstruction x) check(x.getStartRegister() + x.getRegisterCount() <= count, "Invoke range out of bounds: " + method);
            if (instruction instanceof OffsetInstruction x) branches.add(address + x.getCodeOffset());
            address += instruction.getCodeUnits();
        }
        for (int target : branches) check(addresses.contains(target), "Branch is not on an instruction boundary: " + method);
    }

    private static long calls(ClassDef cls, String owner, String name) {
        long count = 0;
        for (var method : cls.getMethods()) if (method.getImplementation() != null) {
            for (var instruction : method.getImplementation().getInstructions()) {
                if (instruction instanceof ReferenceInstruction x && x.getReference() instanceof MethodReference ref
                        && ref.getDefiningClass().equals(owner) && ref.getName().equals(name)) count++;
            }
        }
        return count;
    }

    public static void main(String[] args) throws Exception {
        var originalData = classes(args[0]);
        var patchedData = classes(args[1]);
        var original = originalData.classes();
        var patched = patchedData.classes();
        int mask = Integer.parseInt(args[2]);
        check(patched.keySet().containsAll(original.keySet()), "Original classes lost");
        for (var entry : patchedData.counts().entrySet()) {
            check(entry.getValue().equals(originalData.counts().getOrDefault(entry.getKey(), 1)),
                    "Class occurrence count changed: " + entry.getKey());
        }
        var availability = EXT + "Availability;";
        for (int i = 0; i < OPTIONS.length; i++) {
            var method = method(patched, availability, OPTIONS[i]);
            var first = method.getImplementation().getInstructions().iterator().next();
            check(first instanceof NarrowLiteralInstruction, "Availability constant missing");
            check(((NarrowLiteralInstruction) first).getNarrowLiteral() == ((mask >> i) & 1), "Wrong registered toggle: " + OPTIONS[i]);
            check(calls(patched.get(EXT + "Options;"), availability, OPTIONS[i]) == 1,
                    "Compiler folded an availability call: " + OPTIONS[i]);
        }
        int nativeReferences = 0;
        for (var cls : patched.values()) {
            if (!cls.getType().startsWith(EXT)) continue;
            check(!cls.getType().contains("RuntimeTest"), "Test class bundled");
            for (var method : cls.getMethods()) {
                verifyCode(method);
                if (method.getImplementation() == null) continue;
                for (var instruction : method.getImplementation().getInstructions()) {
                    if (!(instruction instanceof ReferenceInstruction x) || !(x.getReference() instanceof MethodReference ref)) continue;
                    String owner = ref.getDefiningClass();
                    if (owner.startsWith("Lcom/thingclips/") || owner.startsWith("Landroidx/") || owner.startsWith(EXT)) {
                        check(resolves(patched, owner, ref, new HashSet<>()), "Unresolved app/extension method: " + ref);
                        nativeReferences++;
                    }
                }
            }
        }
        String hooks = EXT + "UiHooks;";
        var application = patched.get("Lcom/smart/app/SmartApplication;");
        var settings = patched.get("Lcom/thingclips/smart/personal/setting/plug/activity/SettingActivity;");
        check(calls(application, hooks, "initialize") == 1, "Duplicated/missing initialization");
        check(calls(settings, EXT + "SettingsIntegration;", "groups") == 1, "Duplicated/missing settings group hook");
        check(calls(settings, EXT + "SettingsIntegration;", "cells") == 1, "Duplicated/missing custom-cell hook");
        for (var cls : patched.values()) if (!cls.getType().startsWith(EXT)) {
            for (var method : cls.getMethods()) {
                if (method.getImplementation() == null) continue;
                boolean injected = false;
                for (var instruction : method.getImplementation().getInstructions()) {
                    if (instruction instanceof ReferenceInstruction x && x.getReference() instanceof MethodReference ref
                            && ref.getDefiningClass().startsWith(EXT)) injected = true;
                }
                if (injected) verifyCode(method);
            }
        }
        check(calls(patched.get("Lga;"), hooks, "hideHomePlus") == ((mask & 1) != 0 ? 1 : 0), "Plus patch independence");
        check(calls(patched.get("Lf31;"), hooks, "homeCards") == ((mask & 2) != 0 ? 1 : 0), "Home recommendation independence");
        check(calls(patched.get("Lcom/thingclips/smart/ai/assistant/view/AIBarView;"), hooks, "aiVisibility") == ((mask & 4) != 0 ? 1 : 0), "AI patch independence");
        check(calls(patched.get("Lnwa;"), hooks, "sceneTabs") == ((mask & 8) != 0 ? 1 : 0), "Intelligence patch independence");
        check(calls(patched.get("Lmv0;"), hooks, "navigationTabs") == ((mask & 16) != 0 ? 1 : 0), "Explorer patch independence");
        check(calls(patched.get("Lcom/thingclips/smart/personal/center/plug/cell/WebAppCell;"), hooks, "hideTuyaWeb") == ((mask & 32) != 0 ? 1 : 0), "Web patch independence");
        check(calls(patched.get("Lcom/thingclips/smart/personal/center/plug/cell/WelfareEntryCell;"), hooks, "hideBenefits") == ((mask & 64) != 0 ? 1 : 0), "Benefits patch independence");
        // The runtime JAR must contain only our namespace, with no signature-only stubs.
        long newClasses = patched.keySet().stream().filter(type -> !original.containsKey(type)).count();
        long extensionClasses = patched.keySet().stream().filter(type -> type.startsWith(EXT)).count();
        check(newClasses == extensionClasses, "Unexpected/stub classes merged");
        System.out.println("PASS DEX selection " + mask + ": availability, native references (" + nativeReferences
                + "), single settings integration, isolated hooks, registers/branches, no duplicate or lost original classes.");
    }
}
