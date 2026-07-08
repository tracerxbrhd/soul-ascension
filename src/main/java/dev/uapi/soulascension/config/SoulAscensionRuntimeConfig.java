package dev.uapi.soulascension.config;

/** Immutable server-side values. The defaults are safe before NeoForge loads the config file. */
public record SoulAscensionRuntimeConfig(
    double baseRequiredDamage,
    double linearPerLevel,
    double powerScaling,
    double exponentialMultiplier,
    double minRequiredDamage,
    double maxRequiredDamage,
    int maxLevel,
    int pointsPerLevel,
    boolean limitStatPoints,
    int maxPointsPerStat,
    boolean ignoreTamed,
    boolean ignorePlayerCreated,
    int repeatThreshold,
    double repeatPenalty,
    double minimumFarmMultiplier,
    boolean allowReset,
    boolean amnesiaPointLossEnabled,
    double amnesiaPointLossPercent,
    boolean allowStatDecrease,
    boolean refundDecreasedPoints,
    boolean showTitlesInNameplate,
    boolean fullHealthAfterRespawn,
    boolean debugItemsEnabled,
    boolean statBookLootEnabled,
    boolean soulLensEnabled,
    double soulLensRange,
    boolean soulLensRequireLineOfSight,
    int soulLensUpdateInterval,
    boolean soulLensBlockHotbarScroll,
    double soulLensIdleOverlayOpacity,
    double soulLensActiveOverlayOpacity,
    boolean soulLensShowIdleHint
) {
    public static SoulAscensionRuntimeConfig defaults() {
        return new SoulAscensionRuntimeConfig(
            100.0, 25.0, 0.75, 1.01, 1.0, 0.0,
            100, 1, false, 100,
            true, true, 12, 0.08, 0.15,
            true, false, 25.0, true, true,
            true, true,
            true, true,
            true, 64.0, true, 10, true, 0.25, 0.85, true
        );
    }
}
