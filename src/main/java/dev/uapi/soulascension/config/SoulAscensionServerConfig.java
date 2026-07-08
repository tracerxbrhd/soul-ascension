package dev.uapi.soulascension.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SoulAscensionServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue BASE_REQUIRED_DAMAGE;
    public static final ModConfigSpec.DoubleValue LINEAR_PER_LEVEL;
    public static final ModConfigSpec.DoubleValue POWER_SCALING;
    public static final ModConfigSpec.DoubleValue EXPONENTIAL_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MIN_REQUIRED_DAMAGE;
    public static final ModConfigSpec.DoubleValue MAX_REQUIRED_DAMAGE;
    public static final ModConfigSpec.IntValue MAX_LEVEL;
    public static final ModConfigSpec.IntValue POINTS_PER_LEVEL;
    public static final ModConfigSpec.BooleanValue LIMIT_STAT_POINTS;
    public static final ModConfigSpec.IntValue MAX_POINTS_PER_STAT;
    public static final ModConfigSpec.BooleanValue IGNORE_TAMED;
    public static final ModConfigSpec.BooleanValue IGNORE_PLAYER_CREATED;
    public static final ModConfigSpec.IntValue REPEAT_THRESHOLD;
    public static final ModConfigSpec.DoubleValue REPEAT_PENALTY;
    public static final ModConfigSpec.DoubleValue MINIMUM_FARM_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ALLOW_RESET;
    public static final ModConfigSpec.BooleanValue AMNESIA_POINT_LOSS_ENABLED;
    public static final ModConfigSpec.DoubleValue AMNESIA_POINT_LOSS_PERCENT;
    public static final ModConfigSpec.BooleanValue ALLOW_STAT_DECREASE;
    public static final ModConfigSpec.BooleanValue REFUND_DECREASED_POINTS;
    public static final ModConfigSpec.BooleanValue SHOW_TITLES_IN_NAMEPLATE;
    public static final ModConfigSpec.BooleanValue FULL_HEALTH_AFTER_RESPAWN;
    public static final ModConfigSpec.BooleanValue DEBUG_ITEMS_ENABLED;
    public static final ModConfigSpec.BooleanValue STAT_BOOK_LOOT_ENABLED;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_ENABLED;
    public static final ModConfigSpec.DoubleValue SOUL_LENS_RANGE;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_REQUIRE_LINE_OF_SIGHT;
    public static final ModConfigSpec.IntValue SOUL_LENS_UPDATE_INTERVAL;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_BLOCK_HOTBAR_SCROLL;
    public static final ModConfigSpec.DoubleValue SOUL_LENS_IDLE_OVERLAY_OPACITY;
    public static final ModConfigSpec.DoubleValue SOUL_LENS_ACTIVE_OVERLAY_OPACITY;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_SHOW_IDLE_HINT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("General server-side behavior.").push("general");
        FULL_HEALTH_AFTER_RESPAWN = builder.comment(
            "Restore full modified health after respawn.",
            "Default: true. If false, vanilla/other-mod health is retained and clamped to max health.")
            .define("full_health_after_respawn", true);
        SHOW_TITLES_IN_NAMEPLATE = builder.comment(
            "Show the selected title above the player name to other players. Default: true.")
            .define("show_selected_title_in_display_name", true);
        DEBUG_ITEMS_ENABLED = builder.comment("Expose and enable SOUL ASCENSION debug items. Default: true.")
            .define("debug_items_enabled", true);
        builder.pop();

        builder.comment("Character level and damage-XP progression.").push("progression");
        MAX_LEVEL = builder.comment("Maximum character level. Valid range: 1..1000000. Default: 100.")
            .defineInRange("max_level", 100, 1, 1_000_000);
        POINTS_PER_LEVEL = builder.comment(
            "Stat points awarded for every gained level. Valid range: 0..100. Default: 1.",
            "With maxLevel=100, a new character can earn exactly 100 points.")
            .defineInRange("attribute_points_per_level", 1, 0, 100);
        BASE_REQUIRED_DAMAGE = builder.comment("Base damage XP required for the first level. Default: 100.")
            .defineInRange("base_required_damage", 100.0, 0.0, 1_000_000_000.0);
        LINEAR_PER_LEVEL = builder.comment("Damage XP added for every level after the first. Default: 25.")
            .defineInRange("linear_required_damage_per_level", 25.0, 0.0, 1_000_000_000.0);
        POWER_SCALING = builder.comment("Additional power exponent; 0 disables it. Valid range: 0..8. Default: 0.75.")
            .defineInRange("power_scaling", 0.75, 0.0, 8.0);
        EXPONENTIAL_MULTIPLIER = builder.comment("Multiplier applied per level; 1 disables it. Default: 1.01.")
            .defineInRange("exponential_multiplier", 1.01, 1.0, 10.0);
        MIN_REQUIRED_DAMAGE = builder.comment("Lower clamp for required damage XP. Default: 1.")
            .defineInRange("minimum_required_damage", 1.0, 1.0, 1_000_000_000.0);
        MAX_REQUIRED_DAMAGE = builder.comment("Upper clamp for required damage XP. Default: 0 (unlimited).")
            .defineInRange("maximum_required_damage", 0.0, 0.0, Double.MAX_VALUE);
        builder.pop();

        builder.comment("Attribute point allocation and reset behavior.").push("attribute_points");
        LIMIT_STAT_POINTS = builder.comment(
            "Enforce maxPointsPerStat. Default: false, so allocated points per stat are unlimited.")
            .define("limit_stat_points", false);
        MAX_POINTS_PER_STAT = builder.comment(
            "Maximum allocated points per stat when limitStatPoints is enabled. Default: 100; 0 is unlimited.")
            .defineInRange("max_points_per_stat", 100, 0, 1_000_000);
        ALLOW_STAT_DECREASE = builder.comment(
            "Allow permission-level 2 administrative commands to decrease allocated stats. Default: true.",
            "Normal character screens can only remove unconfirmed preview points; player respec uses consumable items.")
            .define("allow_stat_decrease", true);
        REFUND_DECREASED_POINTS = builder.comment("Return a point when a stat is decreased. Default: true.")
            .define("refund_decreased_stat_points", true);
        ALLOW_RESET = builder.comment("Allow the Amnesia Scroll and administrative stat reset. Default: true.")
            .define("allow_stat_reset", true);
        AMNESIA_POINT_LOSS_ENABLED = builder.comment(
            "Permanently lose part of the allocated points when using an Amnesia Scroll.",
            "Default: false, so all allocated points are returned.")
            .define("amnesia_scroll_point_loss_enabled", false);
        AMNESIA_POINT_LOSS_PERCENT = builder.comment(
            "Percentage lost when amnesiaScrollPointLossEnabled is true. Valid range: 0..100. Default: 25.")
            .defineInRange("amnesia_scroll_point_loss_percent", 25.0, 0.0, 100.0);
        builder.pop();

        builder.comment("Repeated-target farming protection for damage-XP.").push("anti_abuse");
        IGNORE_TAMED = builder.comment("Do not grant XP for damaging tamed entities. Default: true.")
            .define("ignore_tamed_targets", true);
        IGNORE_PLAYER_CREATED = builder.comment("Do not grant XP for entities marked as player-created. Default: true.")
            .define("ignore_player_created_targets", true);
        REPEAT_THRESHOLD = builder.comment("Same-type hits allowed before penalties begin. Default: 12.")
            .defineInRange("repeat_type_threshold", 12, 1, 1000);
        REPEAT_PENALTY = builder.comment("Penalty strength per hit above the threshold. Valid range: 0..1. Default: 0.08.")
            .defineInRange("repeat_type_penalty", 0.08, 0.0, 1.0);
        MINIMUM_FARM_MULTIPLIER = builder.comment("Minimum XP multiplier after penalties. Valid range: 0..1. Default: 0.15.")
            .defineInRange("minimum_farm_multiplier", 0.15, 0.0, 1.0);
        builder.pop();

        builder.comment("Soul Lens inspection and overlay rules.").push("items").push("soul_lens");
        SOUL_LENS_ENABLED = builder.define("enabled", true);
        SOUL_LENS_RANGE = builder.defineInRange("inspection_range", 64.0, 1.0, 256.0);
        SOUL_LENS_REQUIRE_LINE_OF_SIGHT = builder.define("require_line_of_sight", true);
        SOUL_LENS_UPDATE_INTERVAL = builder.defineInRange("overlay_update_interval_ticks", 10, 1, 200);
        SOUL_LENS_BLOCK_HOTBAR_SCROLL = builder.define("block_hotbar_scroll_while_using", true);
        builder.comment("Soul Lens HUD presentation. Opacity values are clamped to 0.0..1.0.").push("ui");
        SOUL_LENS_IDLE_OVERLAY_OPACITY = builder.defineInRange("idle_overlay_opacity", 0.25, 0.0, 1.0);
        SOUL_LENS_ACTIVE_OVERLAY_OPACITY = builder.defineInRange("active_overlay_opacity", 0.85, 0.0, 1.0);
        SOUL_LENS_SHOW_IDLE_HINT = builder.define("show_idle_hint", true);
        builder.pop();
        builder.pop();

        builder.comment("Loot injection rules. Concrete chances are data-driven in loot modifier JSON files.")
            .push("loot");
        STAT_BOOK_LOOT_ENABLED = builder.comment(
            "Allow stat Black Book injection into configured vanilla chest loot tables.",
            "Default: true. Disable to remove stat books from generated loot without deleting datapack files.")
            .define("stat_book_loot_enabled", true);
        builder.pop();
        SPEC = builder.build();
    }

    private SoulAscensionServerConfig() {}
}
