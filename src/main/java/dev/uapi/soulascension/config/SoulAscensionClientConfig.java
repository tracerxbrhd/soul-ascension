package dev.uapi.soulascension.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import net.minecraft.resources.Identifier;

import java.util.Locale;
import java.util.Optional;

public final class SoulAscensionClientConfig {
    static final String DEFAULT_HIDDEN_ATTRIBUTES = String.join(",",
        "minecraft:generic.max_absorption",
        "minecraft:generic.flying_speed",
        "minecraft:generic.explosion_knockback_resistance",
        "minecraft:player.block_break_speed",
        "minecraft:generic.fall_damage_multiplier",
        "minecraft:player.sweeping_damage_ratio",
        "minecraft:generic.safe_fall_distance",
        "minecraft:player.mining_efficiency",
        "minecraft:player.sneaking_speed",
        "minecraft:player.submerged_mining_speed",
        "minecraft:generic.burning_time",
        "minecraft:generic.gravity",
        "minecraft:generic.jump_strength",
        "minecraft:generic.movement_efficiency",
        "minecraft:generic.scale",
        "minecraft:generic.water_movement_efficiency",
        "minecraft:generic.oxygen_bonus",
        "neoforge:creative_flight",
        "neoforge:name_tag_distance",
        "neoforge:nametag_distance",
        "neoforge:swim_speed",
        "apothic_attributes:creative_flight",
        "apothic_attributes:mining_speed");
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_ATTRIBUTE_NAMESPACES;
    public static final ModConfigSpec.ConfigValue<String> HIDDEN_ATTRIBUTES;
    public static final ModConfigSpec.ConfigValue<String> VISIBLE_ATTRIBUTES;
    public static final ModConfigSpec.ConfigValue<String> ATTRIBUTE_CATEGORIES;
    public static final ModConfigSpec.BooleanValue SHOW_PLAYER_PREVIEW;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Attribute list, details, and centralized visibility filters.")
            .push("attribute_display");
        SHOW_ATTRIBUTE_NAMESPACES = builder.comment(
            "Show registry IDs in the attribute detail pane.",
            "Advanced tooltips (F3+H) also reveal IDs. Default: false.")
            .define("show_attribute_namespaces", false);
        HIDDEN_ATTRIBUTES = builder.comment(
            "Comma-separated attribute IDs hidden from the dynamic attribute page.",
            "Entries in visibleAttributes override this list.",
            "The defaults hide low-value engine/movement internals and unsupported creative-only values.")
            .define("hidden_attributes", DEFAULT_HIDDEN_ATTRIBUTES);
        VISIBLE_ATTRIBUTES = builder.comment(
            "Comma-separated attribute IDs that must be shown even if they are in hiddenAttributes.",
            "Default: empty. Add minecraft:generic.oxygen_bonus here to opt in to Oxygen Bonus.")
            .define("visible_attributes", "");
        ATTRIBUTE_CATEGORIES = builder.comment(
            "Semicolon-separated attribute-to-category overrides: attribute_id=category.",
            "Built-in categories: damage, defense, mobility, utility, magic, other.",
            "Example: examplemod:spell_power=magic")
            .define("attribute_categories", "");
        builder.pop();

        builder.comment("Local Character Screen presentation. Client-side only; does not affect gameplay.")
            .push("character_screen");
        SHOW_PLAYER_PREVIEW = builder.comment(
            "Render the 3D player preview in the Character Screen. Default: false.",
            "This uses Minecraft's inventory entity renderer every frame; enable it only if the FPS cost is acceptable.")
            .define("show_player_preview", false);
        builder.pop();
        SPEC = builder.build();
    }

    private SoulAscensionClientConfig() {}

    public static boolean hiddenAttribute(Identifier id) {
        SoulAscensionClientRuntimeConfig config = SoulAscensionClientConfigManager.current();
        if (contains(config.visibleAttributes(), id)) return false;
        return contains(config.hiddenAttributes(), id);
    }

    public static boolean visibleOverride(Identifier id) {
        return contains(SoulAscensionClientConfigManager.current().visibleAttributes(), id);
    }

    public static Optional<String> categoryOverride(Identifier id) {
        for (String entry : SoulAscensionClientConfigManager.current().attributeCategories().split(";")) {
            String[] parts = entry.trim().split("=", 2);
            if (parts.length == 2 && parts[0].trim().equals(id.toString())) {
                String category = parts[1].trim().toLowerCase(Locale.ROOT);
                if (!category.isEmpty()) return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    private static boolean contains(String configured, Identifier id) {
        for (String value : configured.split(",")) if (value.trim().equals(id.toString())) return true;
        return false;
    }
}
