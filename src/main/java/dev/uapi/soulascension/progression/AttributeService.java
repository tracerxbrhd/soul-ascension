package dev.uapi.soulascension.progression;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.AttributeRewardsConfig;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.integration.OptionalIntegrations;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class AttributeService {

    private AttributeService() {}

    public record ConfiguredModifier(ResourceLocation attributeId, double amountPerPoint,
                                     AttributeModifier.Operation operation, Double minimumFinal,
                                     Double maximumFinal, String requiredMod, boolean displayInUi,
                                     String category, String formatter, boolean enabled, int index) {}
    public record ModifierReplacement(ResourceLocation id, double amount, AttributeModifier.Operation operation) {}

    public static void apply(ServerPlayer player, PlayerProgress progress) {
        // Invalid current-format data is read-only: clear stale transient modifiers and fail closed.
        removeManagedModifiers(player);
        if (!progress.isUsable()) {
            if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            OptionalIntegrations.afterAttributesApplied(player);
            return;
        }
        for (Stat stat : Stat.values()) applyStat(player, stat, progress.stat(stat));
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
        OptionalIntegrations.afterAttributesApplied(player);
    }

    private static void applyStat(ServerPlayer player, Stat stat, int points) {
        if (points <= 0) return;
        for (ConfiguredModifier definition : definitions(stat)) {
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(definition.attributeId()).orElse(null);
            if (attribute == null) continue;
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            ResourceLocation modifierId = modifierId(stat, definition.index());
            double amount = effectiveAmount(instance, modifierId, definition, points);
            if (Math.abs(amount) < 1.0E-12) continue;
            instance.addTransientModifier(new AttributeModifier(modifierId, amount, definition.operation()));
        }
    }

    public static List<ConfiguredModifier> definitions(Stat stat) {
        return AttributeRewardsConfig.definitions(stat);
    }

    public static Optional<ConfiguredModifier> displayDefinition(ResourceLocation attributeId) {
        for (Stat stat : Stat.values()) for (ConfiguredModifier definition : definitions(stat))
            if (definition.attributeId().equals(attributeId)) return Optional.of(definition);
        return Optional.empty();
    }

    /** Calculates the modifier amount after applying optional final-value bounds. */
    public static double effectiveAmount(AttributeInstance instance, ResourceLocation modifierId,
                                         ConfiguredModifier definition, int points) {
        double requested = definition.amountPerPoint() * Math.max(0, points);
        if (requested == 0 || (definition.minimumFinal() == null && definition.maximumFinal() == null)) return requested;
        double candidate = valueWithReplacement(instance, modifierId, requested, definition.operation());
        if (inside(candidate, definition.minimumFinal(), definition.maximumFinal())) return requested;

        double baseline = valueWithReplacement(instance, modifierId, 0, definition.operation());
        if (!inside(baseline, definition.minimumFinal(), definition.maximumFinal())) {
            boolean improvesMinimum = definition.minimumFinal() != null && baseline < definition.minimumFinal()
                && candidate > baseline;
            boolean improvesMaximum = definition.maximumFinal() != null && baseline > definition.maximumFinal()
                && candidate < baseline;
            if (!improvesMinimum && !improvesMaximum) return 0;
            return requested;
        }

        double low = 0;
        double high = 1;
        for (int i = 0; i < 48; i++) {
            double middle = (low + high) * 0.5;
            double value = valueWithReplacement(instance, modifierId, requested * middle, definition.operation());
            if (inside(value, definition.minimumFinal(), definition.maximumFinal())) low = middle;
            else high = middle;
        }
        return requested * low;
    }

    public static double valueWithReplacement(AttributeInstance instance, ResourceLocation modifierId,
                                              double amount, AttributeModifier.Operation operation) {
        return valueWithReplacements(instance, List.of(new ModifierReplacement(modifierId, amount, operation)));
    }

    public static double valueWithReplacements(AttributeInstance instance, List<ModifierReplacement> replacements) {
        Set<ResourceLocation> replacedIds = new HashSet<>();
        for (ModifierReplacement replacement : replacements) replacedIds.add(replacement.id());
        List<AttributeModifier> modifiers = new ArrayList<>();
        for (AttributeModifier modifier : instance.getModifiers())
            if (!replacedIds.contains(modifier.id())) modifiers.add(modifier);
        for (ModifierReplacement replacement : replacements)
            if (Math.abs(replacement.amount()) >= 1.0E-12)
                modifiers.add(new AttributeModifier(replacement.id(), replacement.amount(), replacement.operation()));

        double value = instance.getBaseValue();
        for (AttributeModifier modifier : modifiers)
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) value += modifier.amount();
        double afterBase = value;
        for (AttributeModifier modifier : modifiers)
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                afterBase += value * modifier.amount();
        for (AttributeModifier modifier : modifiers)
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                afterBase *= 1.0 + modifier.amount();
        return instance.getAttribute().value().sanitizeValue(afterBase);
    }

    private static boolean inside(double value, Double minimum, Double maximum) {
        return (minimum == null || value >= minimum - 1.0E-9)
            && (maximum == null || value <= maximum + 1.0E-9);
    }

    private static void removeManagedModifiers(ServerPlayer player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;
            for (AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
                ResourceLocation id = modifier.id();
                if (id.getNamespace().equals(SoulAscensionMod.MOD_ID) && id.getPath().startsWith("stat_"))
                    instance.removeModifier(id);
            }
        });
    }

    public static ResourceLocation modifierId(Stat stat, int index) {
        return ResourceLocation.fromNamespaceAndPath(SoulAscensionMod.MOD_ID,
            "stat_" + stat.name().toLowerCase(Locale.ROOT) + "_" + index);
    }

    public static Optional<Stat> sourceStat(ResourceLocation modifierId) {
        for (Stat stat : Stat.values()) {
            for (ConfiguredModifier definition : definitions(stat)) {
                if (modifierId(stat, definition.index()).equals(modifierId)) return Optional.of(stat);
            }
        }
        return Optional.empty();
    }

}
