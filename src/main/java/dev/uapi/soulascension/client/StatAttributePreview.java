package dev.uapi.soulascension.client;

import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.progression.AttributeService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Calculates the same modifier replacement that the authoritative server performs after a stat upgrade. */
public final class StatAttributePreview {
    public record Change(Component name, Component before, Component after, double beforeValue, double afterValue,
                         boolean capped) {}

    private StatAttributePreview() {}

    /** Computes the effective value with all pending stat modifiers replaced locally. */
    public static double value(LocalPlayer player, PlayerProgress preview, Identifier attributeId,
                               AttributeInstance instance) {
        List<AttributeService.ModifierReplacement> replacements = new ArrayList<>();
        for (Stat stat : Stat.values()) {
            for (AttributeService.ConfiguredModifier definition : AttributeService.definitions(stat)) {
                if (!definition.attributeId().equals(attributeId)) continue;
                Identifier modifierId = AttributeService.modifierId(stat, definition.index());
                double amount = AttributeService.effectiveAmount(instance, modifierId, definition,
                    Math.max(0, preview.stat(stat)));
                replacements.add(new AttributeService.ModifierReplacement(modifierId, amount, definition.operation()));
            }
        }
        return replacements.isEmpty() ? instance.getValue()
            : AttributeService.valueWithReplacements(instance, replacements);
    }

    public static List<Change> change(LocalPlayer player, PlayerProgress progress, Stat stat, int delta) {
        Map<Identifier, List<AttributeService.ConfiguredModifier>> grouped = new LinkedHashMap<>();
        for (AttributeService.ConfiguredModifier definition : AttributeService.definitions(stat))
            grouped.computeIfAbsent(definition.attributeId(), ignored -> new ArrayList<>()).add(definition);

        List<Change> result = new ArrayList<>();
        for (Map.Entry<Identifier, List<AttributeService.ConfiguredModifier>> entry : grouped.entrySet()) {
            Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.get(entry.getKey()).orElse(null);
            if (holder == null) continue;
            AttributeInstance instance = player.getAttribute(holder);
            // Optional attributes may not have a client-side instance until their first
            // non-default modifier is synchronized. Use the registered default value so
            // the +1 tooltip can still describe the reward before any point is allocated.
            if (instance == null) instance = new AttributeInstance(holder, ignored -> {});

            double beforeValue = value(player, progress, entry.getKey(), instance);
            double afterValue = value(player, withStatDelta(progress, stat, delta), entry.getKey(), instance);
            boolean capped = delta > 0 && Math.abs(afterValue - beforeValue) < 1.0E-9
                && entry.getValue().stream().anyMatch(definition -> definition.maximumFinal() != null
                    && definition.amountPerPoint() > 0);
            result.add(new Change(Component.translatable(holder.value().getDescriptionId()),
                holder.value().toValueComponent(null, beforeValue, TooltipFlag.NORMAL),
                holder.value().toValueComponent(null, afterValue, TooltipFlag.NORMAL), beforeValue, afterValue, capped));
        }
        return List.copyOf(result);
    }

    private static PlayerProgress withStatDelta(PlayerProgress progress, Stat stat, int delta) {
        int[] values = {progress.strength(), progress.endurance(), progress.agility(), progress.intelligence(),
            progress.perception()};
        values[stat.ordinal()] = Math.max(0, values[stat.ordinal()] + delta);
        return new PlayerProgress(progress.level(), progress.damageProgress(), progress.requiredDamage(),
            progress.unspentPoints(), values[0], values[1], values[2], values[3], values[4]);
    }

}
