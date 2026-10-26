package app.morphe.extension.smartlife;

import android.content.Context;
import android.content.SharedPreferences;

/** All enabled options default on; absent patches can never be enabled by stored preferences. */
public final class Preferences {
    private static SharedPreferences preferences;
    private static int generation;
    private Preferences() {}

    public static void initialize(Context context) {
        if (preferences == null) preferences = context.getApplicationContext()
                .getSharedPreferences("morphe_smartlife", Context.MODE_PRIVATE);
    }

    public static boolean hidden(int option) {
        return Options.available(option) && (preferences == null
                || preferences.getBoolean(Options.KEYS[option], true));
    }

    public static void set(int option, boolean hidden) {
        if (!Options.available(option) || preferences == null || hidden(option) == hidden) return;
        preferences.edit().putBoolean(Options.KEYS[option], hidden).apply();
        generation++;
        if (option == 2) UiHooks.refreshAiViews();
    }

    static int generation() { return generation; }
}
