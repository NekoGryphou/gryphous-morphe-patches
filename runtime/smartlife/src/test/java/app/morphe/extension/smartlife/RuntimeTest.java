package app.morphe.extension.smartlife;

import android.content.SharedPreferences;
import com.thingclips.smart.appshell.config.TabConfig;
import com.thingclips.smart.archer.api.bean.ArcherConfigGroupData;
import com.thingclips.smart.archer.api.extension.CustomCell;
import com.thingclips.smart.home.adv.api.ui.HomeCardData;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JVM tests for the actual runtime filters; Android rendering is tested separately on device. */
public final class RuntimeTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static TabConfig tab(String name, String getter) {
        TabConfig tab = new TabConfig();
        tab.name = name;
        tab.tabGetter = getter;
        return tab;
    }

    private static HomeCardData card(int type) {
        HomeCardData card = new HomeCardData();
        card.setCardType(type);
        return card;
    }

    public static void main(String[] args) throws Exception {
        Field field = Preferences.class.getDeclaredField("preferences");
        field.setAccessible(true);
        MemoryPreferences storage = new MemoryPreferences();
        field.set(null, storage);
        TabConfig home = tab("thing_device", "HomeTabGetterNew");
        TabConfig scene = tab("thing_scene", "NewSceneTabGetter");
        TabConfig explorer = tab("thing_smart_tab", "com.thingclips.smart.intelligence_bridge.SmartTabGetter");
        TabConfig profile = tab("thing_personal", "PersonalTabGetterNew");
        List<?> navigation = Collections.unmodifiableList(Arrays.asList(home, scene, explorer, profile));
        HomeCardData device = card(7), recommendations = card(104), footer = card(100);
        List<?> cards = Collections.unmodifiableList(Arrays.asList(device, recommendations, footer));
        Map<Integer, String> tabs = new LinkedHashMap<>();
        tabs.put(2, "Automation"); tabs.put(1, "Tap-to-run"); tabs.put(3000, "Recommended"); tabs.put(3, "Other");
        Map<?, ?> originalTabs = Collections.unmodifiableMap(tabs);
        List<Integer> pageIds = Collections.unmodifiableList(Arrays.asList(2, 1, 3000, 3));
        ArcherConfigGroupData originalGroup = new ArcherConfigGroupData();
        originalGroup.setName("existing");
        List<ArcherConfigGroupData> originalGroups = Collections.singletonList(originalGroup);

        // Includes zero patches, all seven singles, all pairs and every larger selection.
        for (int selected = 0; selected < 128; selected++) {
            Availability.selected = selected;
            storage.values.clear();
            check(Options.mask() == selected, "Wrong availability mask");
            for (int i = 0; i < 7; i++) check(Preferences.hidden(i) == ((selected & (1 << i)) != 0), "Default must be on only for selected patches");
            List<ArcherConfigGroupData> groups = SettingsIntegration.groups(originalGroups);
            List<CustomCell> cells = SettingsIntegration.cells(Collections.<CustomCell>emptyList());
            check(groups.get(0) == originalGroup && originalGroups.size() == 1, "Existing settings changed");
            check(groups.size() == (selected == 0 ? 1 : 2), "Missing or redundant Morphe group");
            int count = Integer.bitCount(selected);
            check(cells.size() == (selected == 0 ? 0 : count + 1), "Unselected settings cells registered");
            if (selected != 0) {
                List<ArcherConfigGroupData.Items> items = groups.get(1).getItems();
                check(items.size() == count, "Wrong toggle count");
                int cursor = 0;
                for (int i = 0; i < 7; i++) if (Options.available(i)) {
                    check(items.get(cursor++).getCellType().equals(Options.KEYS[i]), "Wrong toggle/order");
                }
                check(SettingsIntegration.groups(groups).size() == 2, "Duplicate Morphe section");
                check(SettingsIntegration.cells(cells).size() == count + 1, "Duplicate Morphe cells");
            }

            // Every runtime toggle combination for each patch selection.
            for (int enabled = 0; enabled < 128; enabled++) {
                for (int i = 0; i < 7; i++) {
                    Preferences.set(i, (enabled & (1 << i)) != 0);
                    check(Preferences.hidden(i) == ((selected & enabled & (1 << i)) != 0), "Unavailable setting affected UI");
                }
                int active = selected & enabled;
                check(UiHooks.hideHomePlus() == ((active & 1) != 0), "Plus coupled to another setting");
                for (int visibility : new int[] {0, 4, 8}) {
                    check(UiHooks.aiVisibility(null, visibility) == ((active & 4) != 0 ? 8 : visibility), "AI visibility/restore");
                }
                List<?> actualCards = UiHooks.homeCards(cards);
                check(actualCards.equals((active & 2) != 0 ? Arrays.asList(device, footer) : cards), "Home recommendation filter");
                check(UiHooks.homeCards(Collections.emptyList()).isEmpty(), "Empty Home list");
                check(UiHooks.homeCards(null) == null, "Null Home list");
                check(UiHooks.homeCards(Arrays.asList("unrelated", null)).equals(Arrays.asList("unrelated", null)), "Unrelated presentation list modified");
                List<?> actualNavigation = UiHooks.navigationTabs(navigation);
                check(actualNavigation.equals((active & 16) != 0 ? Arrays.asList(home, scene, profile) : navigation), "Explorer filter/order");
                check(UiHooks.navigationDestination("thing_smart_tab").equals((active & 16) != 0 ? "thing_device" : "thing_smart_tab"), "Selected Explorer fallback");
                check(UiHooks.navigationDestination("thing_personal").equals("thing_personal"), "Unrelated destination modified");
                check(UiHooks.navigationDestination(null) == null, "Null destination");
                Map<?, ?> actualTabs = UiHooks.sceneTabs(originalTabs);
                check(actualTabs.containsKey(3000) == ((active & 8) == 0), "Intelligence filter");
                check(new ArrayList<>(actualTabs.keySet()).equals((active & 8) != 0 ? Arrays.asList(2, 1, 3) : pageIds), "Intelligence order");
                check(UiHooks.sceneTabIds(pageIds).equals((active & 8) != 0 ? Arrays.asList(2, 1, 3) : pageIds), "Page IDs disagree with titles");
                check(UiHooks.hideTuyaWeb() == ((active & 32) != 0), "Web entry coupled to benefits");
                check(UiHooks.hideBenefits() == ((active & 64) != 0), "Benefits coupled to web entry");
                check(cards.size() == 3 && navigation.size() == 4 && originalTabs.size() == 4, "Caller data mutated");
                for (int i = 0; i < 7; i++) if ((selected & (1 << i)) == 0) {
                    check(!storage.values.containsKey(Options.KEYS[i]), "Unavailable preference written");
                }
            }
            // Reinject the same backing store to simulate preference reader reconstruction.
            field.set(null, null);
            field.set(null, storage);
            for (int i = 0; i < 7; i++) check(Preferences.hidden(i) == Options.available(i), "Stored state lost");
        }
        Availability.selected = 16;
        Preferences.set(4, true);
        long hiddenId = UiHooks.navigationItemId(2);
        Preferences.set(4, false);
        check(hiddenId != UiHooks.navigationItemId(2), "Changed navigation reused an old positional fragment");
        check(UiHooks.navigationItemId(1) != UiHooks.navigationItemId(2), "Distinct navigation slots share an ID");
        check((hiddenId >>> 32) != 0, "Legacy positional fragment tag collision");
        System.out.println("PASS: all 128 patch selections x 128 toggle combinations; defaults, settings population/idempotency, independent filters, disable/restore, preference reconstruction, navigation order/fallback, immutable inputs.");
    }

    private static final class MemoryPreferences implements SharedPreferences {
        final Map<String, Boolean> values = new HashMap<>();
        @Override public boolean getBoolean(String key, boolean fallback) { return values.containsKey(key) ? values.get(key) : fallback; }
        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return new MemoryEditor(); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}
        @Override public String getString(String key, String fallback) { throw new UnsupportedOperationException(); }
        @Override public Set<String> getStringSet(String key, Set<String> fallback) { throw new UnsupportedOperationException(); }
        @Override public int getInt(String key, int fallback) { throw new UnsupportedOperationException(); }
        @Override public long getLong(String key, long fallback) { throw new UnsupportedOperationException(); }
        @Override public float getFloat(String key, float fallback) { throw new UnsupportedOperationException(); }
        private final class MemoryEditor implements Editor {
            final Map<String, Boolean> pending = new HashMap<>();
            @Override public Editor putBoolean(String key, boolean value) { pending.put(key, value); return this; }
            @Override public boolean commit() { values.putAll(pending); return true; }
            @Override public void apply() { commit(); }
            @Override public Editor remove(String key) { values.remove(key); return this; }
            @Override public Editor clear() { values.clear(); return this; }
            @Override public Editor putString(String key, String value) { throw new UnsupportedOperationException(); }
            @Override public Editor putStringSet(String key, Set<String> value) { throw new UnsupportedOperationException(); }
            @Override public Editor putInt(String key, int value) { throw new UnsupportedOperationException(); }
            @Override public Editor putLong(String key, long value) { throw new UnsupportedOperationException(); }
            @Override public Editor putFloat(String key, float value) { throw new UnsupportedOperationException(); }
        }
    }
}
