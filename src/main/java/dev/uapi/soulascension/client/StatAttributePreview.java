package dev.uapi.soulascension.client;

import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.progression.AttributeService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

    public static List<Change> change(LocalPlayer player, PlayerProgress progress, Stat stat, int delta) {
        Map<ResourceLocation, List<AttributeService.ConfiguredModifier>> grouped = new LinkedHashMap<>();
        for (AttributeService.ConfiguredModifier definition : AttributeService.definitions(stat))
            grouped.computeIfAbsent(definition.attributeId(), ignored -> new ArrayList<>()).add(definition);

        List<Change> result = new ArrayList<>();
        for (Map.Entry<ResourceLocation, List<AttributeService.ConfiguredModifier>> entry : grouped.entrySet()) {
            Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(entry.getKey()).orElse(null);
            if (holder == null) continue;
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) continue;

            double afterValue = instance.getValue();
            for (AttributeService.ConfiguredModifier definition : entry.getValue()) {
                ResourceLocation modifierId = AttributeService.modifierId(stat, definition.index());
                double amount = AttributeService.effectiveAmount(instance, modifierId, definition,
                    Math.max(0, progress.stat(stat) + delta));
                afterValue = AttributeService.valueWithReplacement(instance, modifierId, amount, definition.operation());
            }
            double beforeValue = instance.getValue();
            boolean capped = delta > 0 && Math.abs(afterValue - beforeValue) < 1.0E-9
                && entry.getValue().stream().anyMatch(definition -> definition.maximumFinal() != null
                    && definition.amountPerPoint() > 0);
            result.add(new Change(Component.translatable(holder.value().getDescriptionId()),
                holder.value().toValueComponent(null, beforeValue, TooltipFlag.NORMAL),
                holder.value().toValueComponent(null, afterValue, TooltipFlag.NORMAL), beforeValue, afterValue, capped));
        }
        return List.copyOf(result);
    }

}
