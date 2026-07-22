package dev.uapi.soulascension.client;

import dev.uapi.integration.AttributeDisplayRegistry;
import dev.uapi.integration.IntegrationService;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.SoulAscensionClientConfig;
import dev.uapi.soulascension.progression.AttributeService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

public final class DynamicAttributeView {
    private static final Identifier DAMAGE = categoryId("damage");
    private static final Identifier DEFENSE = categoryId("defense");
    private static final Identifier MOBILITY = categoryId("mobility");
    private static final Identifier UTILITY = categoryId("utility");
    private static final Identifier MAGIC = categoryId("magic");
    private static final Identifier OTHER = categoryId("other");
    private static boolean bootstrapped;
    private static final Set<Identifier> WARNED_OPTIONAL_ATTRIBUTES = new HashSet<>();

    public record Value(Identifier id, Component name, Component formattedValue, double rawValue,
                        AttributeDisplayRegistry.Category category) {}
    public record Group(AttributeDisplayRegistry.Category category, List<Value> values) {
        public Group { values = List.copyOf(values); }
    }

    private DynamicAttributeView() {}

    public static List<Group> collect(LocalPlayer player) {
        bootstrapDefaults();
        Map<Identifier, List<Value>> grouped = new LinkedHashMap<>();
        BuiltInRegistries.ATTRIBUTE.listElements().forEach(holder -> {
            Identifier id = holder.unwrapKey().orElseThrow().identifier();
            if (!player.getAttributes().hasAttribute(holder)) return;
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null || !visible(id)) return;
            AttributeDisplayRegistry.Category category = category(id);
            Component name = Component.translatable(holder.value().getDescriptionId());
            Component value = formatValue(id, holder.value(), instance.getValue());
            grouped.computeIfAbsent(category.id(), ignored -> new ArrayList<>())
                .add(new Value(id, name, value, instance.getValue(), category));
        });

        List<Group> result = new ArrayList<>();
        for (AttributeDisplayRegistry.Category category : AttributeDisplayRegistry.categories()) {
            List<Value> values = grouped.get(category.id());
            if (values == null || values.isEmpty()) continue;
            values.sort(Comparator.comparing(value -> value.id().toString()));
            result.add(new Group(category, values));
        }
        return List.copyOf(result);
    }

    public static List<Value> forNamespace(LocalPlayer player, String namespace) {
        return collect(player).stream().flatMap(group -> group.values().stream())
            .filter(value -> value.id().getNamespace().equals(namespace)).toList();
    }

    private static boolean visible(Identifier id) {
        if (SoulAscensionClientConfig.visibleOverride(id)) return true;
        if (SoulAscensionClientConfig.hiddenAttribute(id)) return false;
        Optional<AttributeDisplayRegistry.Rule> rule = AttributeDisplayRegistry.rule(id);
        if (rule.isPresent() && rule.get().visible() != null) return rule.get().visible();
        return AttributeService.displayDefinition(id).map(AttributeService.ConfiguredModifier::displayInUi).orElse(true);
    }

    private static AttributeDisplayRegistry.Category category(Identifier id) {
        Optional<String> configured = SoulAscensionClientConfig.categoryOverride(id);
        if (configured.isPresent()) return ensureCategory(categoryId(configured.get()));
        Optional<AttributeDisplayRegistry.Rule> rule = AttributeDisplayRegistry.rule(id);
        if (rule.isPresent() && rule.get().categoryId() != null)
            return ensureCategory(rule.get().categoryId());
        Optional<AttributeService.ConfiguredModifier> reward = AttributeService.displayDefinition(id);
        if (reward.isPresent()) return ensureCategory(categoryId(reward.get().category()));
        return ensureCategory(classify(id));
    }

    private static Identifier classify(Identifier id) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        if (contains(path, "mana", "spell", "cast", "school", "magic", "cooldown")) return MAGIC;
        if (contains(path, "armor", "toughness", "resistance", "defense", "protection", "health",
            "healing", "lifesteal", "life_steal", "absorption", "overheal", "dodge", "ghost")) return DEFENSE;
        if (contains(path, "movement", "step_height", "elytra", "flight")) return MOBILITY;
        if (contains(path, "attack", "damage", "critical", "crit", "pierce", "shred")) return DAMAGE;
        if (contains(path, "luck", "oxygen", "arrow", "draw", "projectile", "experience", "mining")) return UTILITY;
        return OTHER;
    }

    private static synchronized void bootstrapDefaults() {
        if (bootstrapped) return;
        bootstrapped = true;
        registerCategory(DAMAGE, 0);
        registerCategory(DEFENSE, 100);
        registerCategory(MOBILITY, 200);
        registerCategory(UTILITY, 300);
        registerCategory(MAGIC, 400);
        registerCategory(OTHER, 1000);

        rules(DAMAGE, "minecraft:generic.attack_damage", "minecraft:generic.attack_speed",
            "minecraft:generic.attack_knockback");
        rules(DEFENSE, "minecraft:generic.max_health", "minecraft:generic.armor",
            "minecraft:generic.armor_toughness", "minecraft:generic.knockback_resistance",
            "minecraft:generic.explosion_knockback_resistance");
        rules(MOBILITY, "minecraft:generic.movement_speed", "minecraft:generic.step_height");
        rules(UTILITY, "minecraft:generic.luck", "minecraft:generic.oxygen_bonus");

        optionalRules("apothic_attributes", DAMAGE, "apothic_attributes:armor_pierce", "apothic_attributes:armor_shred",
            "apothic_attributes:crit_chance", "apothic_attributes:crit_damage",
            "apothic_attributes:current_hp_damage", "apothic_attributes:fire_damage",
            "apothic_attributes:cold_damage", "apothic_attributes:prot_pierce",
            "apothic_attributes:prot_shred", "apothic_attributes:projectile_damage");
        optionalRules("apothic_attributes", DEFENSE, "apothic_attributes:dodge_chance", "apothic_attributes:ghost_health",
            "apothic_attributes:healing_received", "apothic_attributes:life_steal",
            "apothic_attributes:overheal");
        optionalRules("apothic_attributes", MOBILITY, "apothic_attributes:elytra_flight");
        optionalRules("apothic_attributes", UTILITY, "apothic_attributes:arrow_damage", "apothic_attributes:arrow_velocity",
            "apothic_attributes:draw_speed", "apothic_attributes:experience_gained",
            "apothic_attributes:mining_speed");

        optionalRules("irons_spellbooks", MAGIC,
            "irons_spellbooks:max_mana", "irons_spellbooks:mana_regen",
            "irons_spellbooks:cooldown_reduction", "irons_spellbooks:spell_power",
            "irons_spellbooks:spell_resist", "irons_spellbooks:cast_time_reduction",
            "irons_spellbooks:summon_damage", "irons_spellbooks:casting_movespeed",
            "irons_spellbooks:fire_magic_resist", "irons_spellbooks:ice_magic_resist",
            "irons_spellbooks:lightning_magic_resist", "irons_spellbooks:holy_magic_resist",
            "irons_spellbooks:ender_magic_resist", "irons_spellbooks:blood_magic_resist",
            "irons_spellbooks:evocation_magic_resist", "irons_spellbooks:nature_magic_resist",
            "irons_spellbooks:eldritch_magic_resist", "irons_spellbooks:fire_spell_power",
            "irons_spellbooks:ice_spell_power", "irons_spellbooks:lightning_spell_power",
            "irons_spellbooks:holy_spell_power", "irons_spellbooks:ender_spell_power",
            "irons_spellbooks:blood_spell_power", "irons_spellbooks:evocation_spell_power",
            "irons_spellbooks:nature_spell_power", "irons_spellbooks:eldritch_spell_power");
    }

    private static void registerCategory(Identifier id, int order) {
        AttributeDisplayRegistry.registerCategory(id,
            Component.translatable("attribute_category.soul_ascension." + id.getPath()), order);
    }

    private static void rules(Identifier category, String... ids) {
        for (String id : ids)
            AttributeDisplayRegistry.registerAttribute(Identifier.parse(id), category, null);
    }

    private static void optionalRules(String requiredMod, Identifier category, String... ids) {
        if (!IntegrationService.isLoaded(requiredMod)) return;
        for (String value : ids) {
            Identifier id = Identifier.parse(value);
            if (!BuiltInRegistries.ATTRIBUTE.containsKey(id)) {
                if (WARNED_OPTIONAL_ATTRIBUTES.add(id)) SoulAscensionMod.LOGGER.warn(
                    "Optional integration {} is loaded but attribute {} is absent; skipping display metadata",
                    requiredMod, id);
                continue;
            }
            AttributeDisplayRegistry.registerAttribute(id, category, null);
        }
    }

    public static Component formatValue(Identifier id, net.minecraft.world.entity.ai.attributes.Attribute attribute,
                                        double value) {
        String formatter = AttributeService.displayDefinition(id)
            .map(AttributeService.ConfiguredModifier::formatter).orElse("auto");
        return switch (formatter) {
            case "number" -> Component.literal(formatNumber(value));
            case "percent" -> Component.translatable("screen.soul_ascension.value.percent", formatNumber(value * 100.0));
            case "multiplier" -> Component.translatable("screen.soul_ascension.value.multiplier", formatNumber(value));
            default -> attribute.toValueComponent(null, value, TooltipFlag.NORMAL);
        };
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-9) return String.format(Locale.ROOT, "%.0f", value);
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static AttributeDisplayRegistry.Category ensureCategory(Identifier id) {
        return AttributeDisplayRegistry.category(id).orElseGet(() -> {
            AttributeDisplayRegistry.registerCategory(id,
                Component.translatable("attribute_category.soul_ascension." + id.getPath()), 900);
            return AttributeDisplayRegistry.category(id).orElseThrow();
        });
    }

    private static Identifier categoryId(String path) {
        String normalized = path.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return SoulAscensionMod.id(normalized.isEmpty() ? "other" : normalized);
    }

    private static boolean contains(String value, String... fragments) {
        for (String fragment : fragments) if (value.contains(fragment)) return true;
        return false;
    }
}
