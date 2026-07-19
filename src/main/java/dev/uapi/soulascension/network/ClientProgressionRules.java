package dev.uapi.soulascension.network;

/** Client cache populated exclusively by the authoritative server. */
public final class ClientProgressionRules {
    private static int maxLevel = 100;
    private static boolean amnesiaPointLossEnabled;
    private static double amnesiaPointLossPercent;
    private static boolean limitStatPoints;
    private static int maxPointsPerStat = 100;
    private static boolean soulLensEnabled = true;
    private static double soulLensRange = 64.0;
    private static int soulLensUpdateInterval = 10;
    private static boolean soulLensBlockHotbarScroll = true;
    private static double soulLensIdleOverlayOpacity = 0.25;
    private static double soulLensActiveOverlayOpacity = 0.85;
    private static boolean soulLensShowIdleHint = true;
    private static double intelligenceExperienceBonusPerPoint = 0.02;
    private static boolean intelligenceAffectsVanillaExperience = true;
    private static boolean intelligenceAffectsSoulProgression = true;
    private static long revision;

    private ClientProgressionRules() {}

    public static void replace(ProgressionRulesPayload payload) {
        // Zero is the server's published sentinel for unlimited progression.
        maxLevel = Math.max(0, payload.maxLevel());
        amnesiaPointLossEnabled = payload.amnesiaPointLossEnabled();
        amnesiaPointLossPercent = finiteClamp(payload.amnesiaPointLossPercent(), 0.0, 100.0, 0.0);
        limitStatPoints = payload.limitStatPoints();
        maxPointsPerStat = Math.max(0, payload.maxPointsPerStat());
        soulLensEnabled = payload.soulLensEnabled();
        soulLensRange = finiteClamp(payload.soulLensRange(), 1.0, 256.0, 64.0);
        soulLensUpdateInterval = Math.max(1, Math.min(200, payload.soulLensUpdateInterval()));
        soulLensBlockHotbarScroll = payload.soulLensBlockHotbarScroll();
        soulLensIdleOverlayOpacity = clampOpacity(payload.soulLensIdleOverlayOpacity());
        soulLensActiveOverlayOpacity = clampOpacity(payload.soulLensActiveOverlayOpacity());
        soulLensShowIdleHint = payload.soulLensShowIdleHint();
        intelligenceExperienceBonusPerPoint = finiteClamp(
            payload.intelligenceExperienceBonusPerPoint(), 0.0, Double.MAX_VALUE, 0.0);
        intelligenceAffectsVanillaExperience = payload.intelligenceAffectsVanillaExperience();
        intelligenceAffectsSoulProgression = payload.intelligenceAffectsSoulProgression();
        revision++;
    }

    public static boolean amnesiaPointLossEnabled() { return amnesiaPointLossEnabled; }
    public static int maxLevel() { return maxLevel; }
    public static double amnesiaPointLossPercent() { return amnesiaPointLossPercent; }
    public static boolean limitStatPoints() { return limitStatPoints; }
    public static int maxPointsPerStat() { return maxPointsPerStat; }
    public static boolean soulLensEnabled() { return soulLensEnabled; }
    public static double soulLensRange() { return soulLensRange; }
    public static int soulLensUpdateInterval() { return soulLensUpdateInterval; }
    public static boolean soulLensBlockHotbarScroll() { return soulLensBlockHotbarScroll; }
    public static double soulLensIdleOverlayOpacity() { return soulLensIdleOverlayOpacity; }
    public static double soulLensActiveOverlayOpacity() { return soulLensActiveOverlayOpacity; }
    public static boolean soulLensShowIdleHint() { return soulLensShowIdleHint; }
    public static double intelligenceExperienceBonusPerPoint() { return intelligenceExperienceBonusPerPoint; }
    public static boolean intelligenceAffectsVanillaExperience() { return intelligenceAffectsVanillaExperience; }
    public static boolean intelligenceAffectsSoulProgression() { return intelligenceAffectsSoulProgression; }
    public static long revision() { return revision; }

    private static double clampOpacity(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 1.0;
    }

    private static double finiteClamp(double value, double minimum, double maximum, double fallback) {
        return Double.isFinite(value) ? Math.max(minimum, Math.min(maximum, value)) : fallback;
    }
}
