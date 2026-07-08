package dev.uapi.soulascension.config;

/** Reads ConfigValue objects only from NeoForge config lifecycle events. */
public final class SoulAscensionConfigManager {
    private static volatile SoulAscensionRuntimeConfig current = SoulAscensionRuntimeConfig.defaults();
    private static volatile boolean loaded;

    private SoulAscensionConfigManager() {}

    public static SoulAscensionRuntimeConfig current() {
        return current;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void reloadFromSpec() {
        current = new SoulAscensionRuntimeConfig(
            SoulAscensionServerConfig.BASE_REQUIRED_DAMAGE.get(),
            SoulAscensionServerConfig.LINEAR_PER_LEVEL.get(),
            SoulAscensionServerConfig.POWER_SCALING.get(),
            SoulAscensionServerConfig.EXPONENTIAL_MULTIPLIER.get(),
            SoulAscensionServerConfig.MIN_REQUIRED_DAMAGE.get(),
            SoulAscensionServerConfig.MAX_REQUIRED_DAMAGE.get(),
            SoulAscensionServerConfig.MAX_LEVEL.get(),
            SoulAscensionServerConfig.POINTS_PER_LEVEL.get(),
            SoulAscensionServerConfig.LIMIT_STAT_POINTS.get(),
            SoulAscensionServerConfig.MAX_POINTS_PER_STAT.get(),
            SoulAscensionServerConfig.IGNORE_TAMED.get(),
            SoulAscensionServerConfig.IGNORE_PLAYER_CREATED.get(),
            SoulAscensionServerConfig.REPEAT_THRESHOLD.get(),
            SoulAscensionServerConfig.REPEAT_PENALTY.get(),
            SoulAscensionServerConfig.MINIMUM_FARM_MULTIPLIER.get(),
            SoulAscensionServerConfig.ALLOW_RESET.get(),
            SoulAscensionServerConfig.AMNESIA_POINT_LOSS_ENABLED.get(),
            SoulAscensionServerConfig.AMNESIA_POINT_LOSS_PERCENT.get(),
            SoulAscensionServerConfig.ALLOW_STAT_DECREASE.get(),
            SoulAscensionServerConfig.REFUND_DECREASED_POINTS.get(),
            SoulAscensionServerConfig.SHOW_TITLES_IN_NAMEPLATE.get(),
            SoulAscensionServerConfig.FULL_HEALTH_AFTER_RESPAWN.get(),
            SoulAscensionServerConfig.DEBUG_ITEMS_ENABLED.get(),
            SoulAscensionServerConfig.STAT_BOOK_LOOT_ENABLED.get(),
            SoulAscensionServerConfig.SOUL_LENS_ENABLED.get(),
            SoulAscensionServerConfig.SOUL_LENS_RANGE.get(),
            SoulAscensionServerConfig.SOUL_LENS_REQUIRE_LINE_OF_SIGHT.get(),
            SoulAscensionServerConfig.SOUL_LENS_UPDATE_INTERVAL.get(),
            SoulAscensionServerConfig.SOUL_LENS_BLOCK_HOTBAR_SCROLL.get(),
            SoulAscensionServerConfig.SOUL_LENS_IDLE_OVERLAY_OPACITY.get(),
            SoulAscensionServerConfig.SOUL_LENS_ACTIVE_OVERLAY_OPACITY.get(),
            SoulAscensionServerConfig.SOUL_LENS_SHOW_IDLE_HINT.get()
        );
        loaded = true;
    }
}
