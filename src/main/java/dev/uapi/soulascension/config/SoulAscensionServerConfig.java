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

    private SoulAscensionServerConfig() {}
}
