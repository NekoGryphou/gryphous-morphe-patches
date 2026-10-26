package app.morphe.extension.smartlife;

public final class Options {
    private Options() {}
    public static final String[] KEYS = {
        "hide_home_plus", "hide_home_recommendations", "hide_ai_input_bar",
        "hide_intelligence_recommendations", "hide_explorer_tab", "hide_tuya_web", "hide_benefits"
    };
    private static final String[] ENGLISH = {
        "Hide Home Plus", "Hide Home Recommendations", "Hide AI Input Bar",
        "Hide Intelligence Recommendations", "Hide Explorer Tab", "Hide Tuya AI Web Version",
        "Hide Centre d’avantages"
    };
    private static final String[] FRENCH = {
        "Masquer Plus sur l’accueil", "Masquer les recommandations de l’accueil", "Masquer la barre de saisie IA",
        "Masquer les recommandations d’Intelligence", "Masquer l’onglet Explorer", "Masquer Tuya AI Web Version",
        "Masquer le Centre d’avantages"
    };

    public static boolean available(int option) {
        switch (option) {
            case 0: return Availability.homePlus();
            case 1: return Availability.homeRecommendations();
            case 2: return Availability.aiInputBar();
            case 3: return Availability.intelligenceRecommendations();
            case 4: return Availability.explorerTab();
            case 5: return Availability.tuyaWeb();
            case 6: return Availability.benefits();
            default: return false;
        }
    }

    public static String label(int option, String language) {
        return ("fr".equals(language) ? FRENCH : ENGLISH)[option];
    }

    public static int mask() {
        int mask = 0;
        for (int i = 0; i < KEYS.length; i++) if (available(i)) mask |= 1 << i;
        return mask;
    }
}
