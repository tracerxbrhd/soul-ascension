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

    private ClientProgressionRules() {}

    public static void replace(ProgressionRulesPayload payload) {
        maxLevel = Math.max(1, payload.maxLevel());
        amnesiaPointLossEnabled = payload.amnesiaPointLossEnabled();
        amnesiaPointLossPercent = Math.max(0.0, Math.min(100.0, payload.amnesiaPointLossPercent()));
        limitStatPoints = payload.limitStatPoints();
        maxPointsPerStat = Math.max(0, payload.maxPointsPerStat());
        soulLensEnabled = payload.soulLensEnabled();
        soulLensRange = Math.max(1.0, Math.min(256.0, payload.soulLensRange()));
        soulLensUpdateInterval = Math.max(1, Math.min(200, payload.soulLensUpdateInterval()));
        soulLensBlockHotbarScroll = payload.soulLensBlockHotbarScroll();
        soulLensIdleOverlayOpacity = clampOpacity(payload.soulLensIdleOverlayOpacity());
        soulLensActiveOverlayOpacity = clampOpacity(payload.soulLensActiveOverlayOpacity());
        soulLensShowIdleHint = payload.soulLensShowIdleHint();
        intelligenceExperienceBonusPerPoint = Math.max(0.0, payload.intelligenceExperienceBonusPerPoint());
        intelligenceAffectsVanillaExperience = payload.intelligenceAffectsVanillaExperience();
        intelligenceAffectsSoulProgression = payload.intelligenceAffectsSoulProgression();
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

    private static double clampOpacity(double value) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : 1.0;
    }
}
