package app.morphe.extension.smartlife;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import com.thingclips.smart.appshell.config.TabConfig;
import com.thingclips.smart.home.adv.api.ui.HomeCardData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Presentation-only hooks. Caller-owned data is never removed or modified. */
public final class UiHooks {
    private static boolean initialized;
    private static final Map<View, Integer> aiViews = new WeakHashMap<>();
    private UiHooks() {}

    public static void initialize(Application application) {
        Preferences.initialize(application);
        if (initialized) return;
        initialized = true;
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            private final Map<Activity, Integer> generations = new WeakHashMap<>();
            @Override public void onActivityCreated(Activity activity, Bundle state) {
                generations.put(activity, Preferences.generation());
            }
            @Override public void onActivityResumed(final Activity activity) {
                if (!activity.getClass().getName().equals("com.thingclips.smart.hometab.activity.FamilyHomeActivity")) return;
                Integer previous = generations.put(activity, Preferences.generation());
                if (previous == null || previous == Preferences.generation()) return;
                // Defer until the Settings Activity has left the foreground. Never recreate
                // a device-control Activity, or the Settings screen while switches are used.
                activity.getWindow().getDecorView().post(new Runnable() {
                    @Override public void run() {
                        if (!activity.isFinishing()) activity.recreate();
                    }
                });
            }
            @Override public void onActivityDestroyed(Activity activity) { generations.remove(activity); }
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
        });
    }

    public static boolean hideHomePlus() { return Preferences.hidden(0); }
    public static boolean hideTuyaWeb() { return Preferences.hidden(5); }
    public static boolean hideBenefits() { return Preferences.hidden(6); }

    public static int aiVisibility(View view, int requested) {
        aiViews.put(view, requested);
        return Preferences.hidden(2) ? View.GONE : requested;
    }

    public static void hideAiView(View view) {
        Integer requested = aiViews.get(view);
        view.setVisibility(requested != null ? requested : view.getVisibility());
    }

    static void refreshAiViews() {
        // Snapshot: each setVisibility call passes through aiVisibility again.
        for (Map.Entry<View, Integer> entry : new ArrayList<>(aiViews.entrySet())) {
            View view = entry.getKey();
            if (view != null) view.setVisibility(entry.getValue());
        }
    }

    public static List<?> homeCards(List<?> input) {
        if (input == null || !Preferences.hidden(1)) return input;
        List<Object> output = new ArrayList<>(input.size());
        for (Object item : input) {
            // DATA_TYPE_FIND_MORE / HomeRecoSlotsCard, including its heading and spacing.
            if (item instanceof HomeCardData && ((HomeCardData) item).getCardType() == 104) continue;
            output.add(item);
        }
        return output;
    }

    public static Map<?, ?> sceneTabs(Map<?, ?> input) {
        if (input == null || !Preferences.hidden(3)) return input;
        Map<Object, Object> output = new LinkedHashMap<>(input);
        output.remove(3000); // SceneViewPagerFragment: scene_discover.
        return output;
    }

    public static List<?> sceneTabIds(List<?> input) {
        if (input == null || !Preferences.hidden(3)) return input;
        List<Object> output = new ArrayList<>(input);
        output.remove(Integer.valueOf(3000));
        return output;
    }

    public static Parcelable saveSceneState(Parcelable state) {
        Bundle wrapped = new Bundle();
        wrapped.putParcelable("morphe_scene_state", state);
        wrapped.putBoolean("morphe_scene_hidden", Preferences.hidden(3));
        return wrapped;
    }

    public static Parcelable restoreSceneState(Parcelable state) {
        if (state instanceof Bundle && ((Bundle) state).containsKey("morphe_scene_hidden")) {
            Bundle wrapped = (Bundle) state;
            return wrapped.getBoolean("morphe_scene_hidden") == Preferences.hidden(3)
                    ? wrapped.getParcelable("morphe_scene_state") : null;
        }
        // A pre-patch position-based snapshot may contain Recommended in a position
        // now occupied by a different page. Rebuild that screen once instead.
        return Preferences.hidden(3) ? null : state;
    }

    public static List<?> navigationTabs(List<?> input) {
        if (input == null || !Preferences.hidden(4)) return input;
        List<Object> output = new ArrayList<>(input.size());
        for (Object item : input) {
            if (item instanceof TabConfig) {
                TabConfig tab = (TabConfig) item;
                if ("thing_smart_tab".equals(tab.name)
                        && "com.thingclips.smart.intelligence_bridge.SmartTabGetter".equals(tab.tabGetter)) continue;
            }
            output.add(item);
        }
        return output;
    }

    public static String navigationDestination(String requested) {
        return Preferences.hidden(4) && "thing_smart_tab".equals(requested) ? "thing_device" : requested;
    }

    // Namespace the native positional tags by navigation mode. A change of mode
    // rebuilds fragments instead of restoring Explorer into a remaining tab's slot.
    // This also supports distinct tabs backed by the same generic Fragment class.
    public static long navigationItemId(int position) {
        long mode = Preferences.hidden(4) ? 0x4d6f727100000000L : 0x4d6f727000000000L;
        return mode | (position & 0xffffffffL);
    }
}
