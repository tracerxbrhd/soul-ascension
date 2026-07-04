package dev.uapi.soulascension.config;

import dev.uapi.soulascension.data.Stat;
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
    public static final ModConfigSpec.DoubleValue INTELLIGENCE_REWARD_XP_PERCENT;
    public static final ModConfigSpec.BooleanValue DEBUG_ITEMS_ENABLED;
    public static final ModConfigSpec.BooleanValue ALTAR_ENABLED;
    public static final ModConfigSpec.BooleanValue ALTAR_ALLOW_RESPEC;
    public static final ModConfigSpec.BooleanValue ALTAR_RESPEC_CONFIRMATION;
    public static final ModConfigSpec.ConfigValue<String> ALTAR_RESPEC_COST_TYPE;
    public static final ModConfigSpec.IntValue ALTAR_RESPEC_COST_AMOUNT;
    public static final ModConfigSpec.ConfigValue<String> ALTAR_RESPEC_COST_ITEM;
    public static final ModConfigSpec.BooleanValue ALTAR_ALLOW_PROFILE_TOGGLE;
    public static final ModConfigSpec.BooleanValue PROFILE_PRIVACY_ENABLED;
    public static final ModConfigSpec.BooleanValue PROFILE_DEFAULT_HIDDEN;
    public static final ModConfigSpec.BooleanValue HIDE_FROM_SOUL_LENS;
    public static final ModConfigSpec.BooleanValue HIDE_FROM_BADGE_INSPECTION;
    public static final ModConfigSpec.BooleanValue OPERATORS_BYPASS_HIDDEN;
    public static final ModConfigSpec.BooleanValue CONCEALMENT_EMBLEM_ENABLED;
    public static final ModConfigSpec.BooleanValue EMBLEM_CRAFTABLE;
    public static final ModConfigSpec.BooleanValue EMBLEM_USE_CONSUMES_ITEM;
    public static final ModConfigSpec.BooleanValue EMBLEM_USE_UNLOCKS_TOGGLE;
    public static final ModConfigSpec.BooleanValue EMBLEM_USE_SETS_HIDDEN;
    public static final ModConfigSpec.BooleanValue EMBLEM_DIRECT_USE_WITH_ACCESSORY;
    public static final ModConfigSpec.BooleanValue ACCESSORIES_ENABLED;
    public static final ModConfigSpec.BooleanValue PREFER_UAPI_ACCESSORY_SERVICE;
    public static final ModConfigSpec.ConfigValue<String> CONCEALMENT_EMBLEM_SLOT;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_ENABLED;
    public static final ModConfigSpec.DoubleValue SOUL_LENS_RANGE;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_REQUIRE_LINE_OF_SIGHT;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_RESPECT_HIDDEN;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_OPERATOR_BYPASS;
    public static final ModConfigSpec.IntValue SOUL_LENS_UPDATE_INTERVAL;
    public static final ModConfigSpec.BooleanValue SOUL_LENS_BLOCK_HOTBAR_SCROLL;
    public static final ModConfigSpec.ConfigValue<String> STRENGTH_MODIFIERS;
    public static final ModConfigSpec.ConfigValue<String> ENDURANCE_MODIFIERS;
    public static final ModConfigSpec.ConfigValue<String> AGILITY_MODIFIERS;
    public static final ModConfigSpec.ConfigValue<String> INTELLIGENCE_MODIFIERS;
    public static final ModConfigSpec.ConfigValue<String> PERCEPTION_MODIFIERS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Character level and damage-XP progression.").push("progression");
        MAX_LEVEL = builder.comment("Maximum character level. Valid range: 1..1000000. Default: 100.")
            .defineInRange("maxLevel", 100, 1, 1_000_000);
        POINTS_PER_LEVEL = builder.comment(
            "Stat points awarded for every gained level. Valid range: 0..100. Default: 1.",
            "With maxLevel=100, a new character can earn exactly 100 points.")
            .defineInRange("pointsPerLevel", 1, 0, 100);
        BASE_REQUIRED_DAMAGE = builder.comment("Base damage XP required for the first level. Default: 100.")
            .defineInRange("baseRequiredDamage", 100.0, 0.0, 1_000_000_000.0);
        LINEAR_PER_LEVEL = builder.comment("Damage XP added for every level after the first. Default: 25.")
            .defineInRange("linearPerLevel", 25.0, 0.0, 1_000_000_000.0);
        POWER_SCALING = builder.comment("Additional power exponent; 0 disables it. Valid range: 0..8. Default: 0.75.")
            .defineInRange("powerScaling", 0.75, 0.0, 8.0);
        EXPONENTIAL_MULTIPLIER = builder.comment("Multiplier applied per level; 1 disables it. Default: 1.01.")
            .defineInRange("exponentialMultiplier", 1.01, 1.0, 10.0);
        MIN_REQUIRED_DAMAGE = builder.comment("Lower clamp for required damage XP. Default: 1.")
            .defineInRange("minimumRequiredDamage", 1.0, 1.0, 1_000_000_000.0);
        MAX_REQUIRED_DAMAGE = builder.comment("Upper clamp for required damage XP. Default: 0 (unlimited).")
            .defineInRange("maximumRequiredDamage", 0.0, 0.0, Double.MAX_VALUE);
        builder.pop();

        builder.comment("Stat allocation and reset behavior.").push("stats");
        LIMIT_STAT_POINTS = builder.comment(
            "Enforce maxPointsPerStat. Default: false, so allocated points per stat are unlimited.")
            .define("limitStatPoints", false);
        MAX_POINTS_PER_STAT = builder.comment(
            "Maximum allocated points per stat when limitStatPoints is enabled. Default: 100; 0 is unlimited.")
            .defineInRange("maxPointsPerStat", 100, 0, 1_000_000);
        ALLOW_STAT_DECREASE = builder.comment("Allow players/admin commands to decrease allocated stats. Default: true.")
            .define("allowStatDecrease", true);
        REFUND_DECREASED_POINTS = builder.comment("Return a point when a stat is decreased. Default: true.")
            .define("refundDecreasedStatPoints", true);
        ALLOW_RESET = builder.comment("Allow the Amnesia Scroll and administrative stat reset. Default: true.")
            .define("allowStatReset", true);
        AMNESIA_POINT_LOSS_ENABLED = builder.comment(
            "Permanently lose part of the allocated points when using an Amnesia Scroll.",
            "Default: false, so all allocated points are returned.")
            .define("amnesiaScrollPointLossEnabled", false);
        AMNESIA_POINT_LOSS_PERCENT = builder.comment(
            "Percentage lost when amnesiaScrollPointLossEnabled is true. Valid range: 0..100. Default: 25.")
            .defineInRange("amnesiaScrollPointLossPercent", 25.0, 0.0, 100.0);
        builder.pop();

        builder.comment("Repeated-target farming protection.").push("anti_abuse");
        IGNORE_TAMED = builder.comment("Do not grant XP for damaging tamed entities. Default: true.")
            .define("ignoreTamedTargets", true);
        IGNORE_PLAYER_CREATED = builder.comment("Do not grant XP for entities marked as player-created. Default: true.")
            .define("ignorePlayerCreatedTargets", true);
        REPEAT_THRESHOLD = builder.comment("Same-type hits allowed before penalties begin. Default: 12.")
            .defineInRange("repeatTypeThreshold", 12, 1, 1000);
        REPEAT_PENALTY = builder.comment("Penalty strength per hit above the threshold. Valid range: 0..1. Default: 0.08.")
            .defineInRange("repeatTypePenalty", 0.08, 0.0, 1.0);
        MINIMUM_FARM_MULTIPLIER = builder.comment("Minimum XP multiplier after penalties. Valid range: 0..1. Default: 0.15.")
            .defineInRange("minimumFarmMultiplier", 0.15, 0.0, 1.0);
        builder.pop();

        builder.comment("Config-driven rewards for each allocated character stat.",
            "Format per entry: attribute|amount|operation|min_final|max_final|required_mod|display|category|formatter|enabled",
            "Separate entries with semicolons. Use '-' for no min/max/required mod.",
            "Operations: ADD_VALUE, ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL.",
            "Categories: damage, defense, mobility, utility, magic, other (custom registered categories also work).",
            "Formatters: auto, number, percent, multiplier.",
            "Unknown attributes, absent required mods, and invalid entries are logged and skipped safely.",
            "The old 3-field and 9-field formats remain accepted for config migration.")
            .push("attribute_rewards");
        STRENGTH_MODIFIERS = builder.define("strength",
            "minecraft:generic.attack_damage|0.5|ADD_VALUE|-|-|-|true|damage|number|true");
        ENDURANCE_MODIFIERS = builder.define("endurance",
            "minecraft:generic.max_health|0.5|ADD_VALUE|-|-|-|true|defense|number|true;"
                + "minecraft:generic.armor|0.5|ADD_VALUE|-|-|-|true|defense|number|true;"
                + "minecraft:generic.armor_toughness|0.5|ADD_VALUE|-|-|-|true|defense|number|true;"
                + "minecraft:generic.knockback_resistance|0.05|ADD_VALUE|-|1.0|-|true|defense|percent|true");
        AGILITY_MODIFIERS = builder.define("agility",
            "minecraft:generic.movement_speed|0.02|ADD_MULTIPLIED_BASE|-|-|-|true|mobility|auto|true;"
                + "minecraft:generic.attack_speed|0.5|ADD_VALUE|-|-|-|true|damage|number|true;"
                + "minecraft:generic.step_height|0.04|ADD_VALUE|-|1.01|-|true|mobility|number|true");
        INTELLIGENCE_MODIFIERS = builder.define("intelligence",
            "irons_spellbooks:max_mana|2.0|ADD_VALUE|-|300.0|irons_spellbooks|true|magic|number|true;"
                + "irons_spellbooks:mana_regen|0.01|ADD_MULTIPLIED_BASE|-|2.0|irons_spellbooks|true|magic|multiplier|true;"
                + "irons_spellbooks:spell_power|0.01|ADD_MULTIPLIED_BASE|-|2.0|irons_spellbooks|true|magic|multiplier|true;"
                + "irons_spellbooks:cast_time_reduction|0.005|ADD_MULTIPLIED_BASE|-|1.5|irons_spellbooks|true|magic|multiplier|true;"
                + "irons_spellbooks:cooldown_reduction|0.005|ADD_MULTIPLIED_BASE|-|1.5|irons_spellbooks|true|magic|multiplier|true");
        PERCEPTION_MODIFIERS = builder.define("perception",
            "minecraft:generic.luck|1.0|ADD_VALUE|-|-|-|true|utility|number|true;"
                + "apothic_attributes:crit_chance|0.002|ADD_VALUE|-|0.20|apothic_attributes|true|damage|percent|true;"
                + "apothic_attributes:arrow_damage|0.005|ADD_MULTIPLIED_BASE|-|1.50|apothic_attributes|true|utility|multiplier|true;"
                + "apothic_attributes:draw_speed|0.005|ADD_MULTIPLIED_BASE|-|1.50|apothic_attributes|true|utility|multiplier|true");
        builder.pop();

        builder.comment("Optional integrations. Entries are ignored when the required mod is absent.")
            .push("integrations");
        INTELLIGENCE_REWARD_XP_PERCENT = builder.comment(
            "Extra instance XP per Intelligence point, in percent. Valid range: 0..100. Default: 0.25.")
            .defineInRange("intelligenceRewardXpPercent", 0.25, 0.0, 100.0);
        builder.pop();

        builder.comment("Server-controlled presentation.").push("ui");
        SHOW_TITLES_IN_NAMEPLATE = builder.comment(
            "Show the selected title above the player name to other players. Default: true.")
            .define("showSelectedTitleInDisplayName", true);
        builder.pop();

        builder.comment("General character behavior.").push("general");
        FULL_HEALTH_AFTER_RESPAWN = builder.comment(
            "Restore full modified health after respawn.",
            "Default: true. If false, vanilla/other-mod health is retained and clamped to max health.")
            .define("fullHealthAfterRespawn", true);
        builder.pop();

        builder.comment("Soul Altar gameplay and player-list presentation.").push("soul_altar");
        ALTAR_ENABLED = builder.define("enabled", true);
        ALTAR_ALLOW_RESPEC = builder.define("allow_respec", true);
        ALTAR_RESPEC_CONFIRMATION = builder.define("respec_requires_confirmation", true);
        ALTAR_RESPEC_COST_TYPE = builder.comment("Allowed: none, xp_levels, experience_points, item, disabled.")
            .define("respec_cost_type", "none", SoulAscensionServerConfig::validRespecCostType);
        ALTAR_RESPEC_COST_AMOUNT = builder.defineInRange("respec_cost_amount", 0, 0, 1_000_000);
        ALTAR_RESPEC_COST_ITEM = builder.comment("Item id used when respec_cost_type=item.")
            .define("respec_cost_item", "minecraft:amethyst_shard", SoulAscensionServerConfig::validResourceLocation);
        ALTAR_ALLOW_PROFILE_TOGGLE = builder.define("allow_profile_visibility_toggle", true);
        builder.pop();

        builder.comment("Public profile privacy rules.").push("profile_privacy");
        PROFILE_PRIVACY_ENABLED = builder.define("enabled", true);
        PROFILE_DEFAULT_HIDDEN = builder.define("default_hidden", false);
        HIDE_FROM_SOUL_LENS = builder.define("hide_from_soul_lens", true);
        HIDE_FROM_BADGE_INSPECTION = builder.define("hide_from_soul_badge_inspection", true);
        OPERATORS_BYPASS_HIDDEN = builder.define("operators_bypass_hidden_profiles", true);
        builder.pop();

        builder.comment("Emblem of Concealment behavior.").push("concealment_emblem");
        CONCEALMENT_EMBLEM_ENABLED = builder.define("enabled", true);
        EMBLEM_CRAFTABLE = builder.comment("Load the workbench recipe for the Emblem. Requires a datapack reload after changing.")
            .define("craftable", true);
        EMBLEM_USE_CONSUMES_ITEM = builder.define("use_consumes_item", true);
        EMBLEM_USE_UNLOCKS_TOGGLE = builder.define("fallback_use_unlocks_visibility_toggle", true);
        EMBLEM_USE_SETS_HIDDEN = builder.define("fallback_use_sets_hidden_immediately", true);
        EMBLEM_DIRECT_USE_WITH_ACCESSORY = builder.define("allow_direct_use_when_accessory_mod_loaded", false);
        builder.pop();

        builder.comment("Loader-safe optional accessory integration through U-API.").push("integrations").push("accessories");
        ACCESSORIES_ENABLED = builder.define("enabled", true);
        PREFER_UAPI_ACCESSORY_SERVICE = builder.define("prefer_uapi_accessory_service", true);
        CONCEALMENT_EMBLEM_SLOT = builder.define("concealment_emblem_slot", "charm",
            value -> value instanceof String string && !string.isBlank() && string.length() <= 64);
        builder.pop(2);

        builder.comment("Soul Lens inspection and overlay rules.").push("soul_lens");
        SOUL_LENS_ENABLED = builder.define("enabled", true);
        SOUL_LENS_RANGE = builder.defineInRange("inspection_range", 64.0, 1.0, 256.0);
        SOUL_LENS_REQUIRE_LINE_OF_SIGHT = builder.define("require_line_of_sight", true);
        SOUL_LENS_RESPECT_HIDDEN = builder.define("hide_profiles_respected", true);
        SOUL_LENS_OPERATOR_BYPASS = builder.define("operators_bypass_hidden_profiles", true);
        SOUL_LENS_UPDATE_INTERVAL = builder.defineInRange("overlay_update_interval_ticks", 10, 1, 200);
        SOUL_LENS_BLOCK_HOTBAR_SCROLL = builder.define("block_hotbar_scroll_while_using", true);
        builder.pop();

        builder.comment("Development tools. Disable on public servers.").push("debug");
        DEBUG_ITEMS_ENABLED = builder.comment("Expose and enable SOUL ASCENSION debug items. Default: true.")
            .define("debugItemsEnabled", true);
        builder.pop();
        SPEC = builder.build();
    }

    public static String modifiers(Stat stat) {
        return switch (stat) {
            case STRENGTH -> STRENGTH_MODIFIERS.get();
            case ENDURANCE -> ENDURANCE_MODIFIERS.get();
            case AGILITY -> AGILITY_MODIFIERS.get();
            case INTELLIGENCE -> INTELLIGENCE_MODIFIERS.get();
            case PERCEPTION -> PERCEPTION_MODIFIERS.get();
        };
    }

    private static boolean validRespecCostType(Object value) {
        return value instanceof String string && switch (string.toLowerCase(java.util.Locale.ROOT)) {
            case "none", "xp_levels", "experience_points", "item", "disabled" -> true;
            default -> false;
        };
    }

    private static boolean validResourceLocation(Object value) {
        return value instanceof String string && net.minecraft.resources.ResourceLocation.tryParse(string) != null;
    }

    private SoulAscensionServerConfig() {}
}
