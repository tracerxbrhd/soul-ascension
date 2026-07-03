package dev.uapi.soulascension.client;

import dev.uapi.integration.AttributeDisplayRegistry;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.progression.AttributeService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Cached client presentation model. It never mutates attributes and rebuilds at most twice per second. */
public final class AttributeViewModel {
    public enum SourceKind { SOUL_ASCENSION, EQUIPMENT, EFFECT, OTHER_MOD, UNKNOWN }

    public record SourceEntry(SourceKind kind, Component name, ResourceLocation modifierId,
                              ItemStack icon, double amount, AttributeModifier.Operation operation,
                              Component formattedAmount) {}

    public record DisplayEntry(ResourceLocation id, Holder<Attribute> attribute, Component name,
                               Component description, AttributeDisplayRegistry.Category category,
                               double baseValue, double currentValue, Component formattedBase,
                               Component formattedCurrent, List<SourceEntry> sources) {
        public DisplayEntry { sources = List.copyOf(sources); }
    }

    public record Group(AttributeDisplayRegistry.Category category, List<DisplayEntry> entries) {
        public Group { entries = List.copyOf(entries); }
    }

    private record ModifierKey(ResourceLocation attributeId, ResourceLocation modifierId) {}
    private record EquipmentSource(ItemStack stack, EquipmentSlot slot) {}

    private List<Group> groups = List.of();
    private Map<ResourceLocation, DisplayEntry> byId = Map.of();
    private ResourceLocation selectedId;
    private long lastFingerprint = Long.MIN_VALUE;
    private int ticksSinceRefresh = 10;

    public List<Group> groups() { return groups; }
    public Optional<DisplayEntry> selected() { return Optional.ofNullable(byId.get(selectedId)); }
    public ResourceLocation selectedId() { return selectedId; }

    public void select(ResourceLocation id) {
        if (byId.containsKey(id)) selectedId = id;
    }

    public void forceRefresh() {
        ticksSinceRefresh = 10;
        lastFingerprint = Long.MIN_VALUE;
    }

    public void tick(net.minecraft.client.player.LocalPlayer player, PlayerProgress progress) {
        ticksSinceRefresh++;
        long fingerprint = fingerprint(player, progress);
        if (fingerprint != lastFingerprint || ticksSinceRefresh >= 10) rebuild(player, fingerprint);
    }

    private void rebuild(net.minecraft.client.player.LocalPlayer player, long fingerprint) {
        Map<ModifierKey, EquipmentSource> equipment = equipmentSources(player);
        Map<ModifierKey, Component> effects = effectSources(player);
        List<Group> rebuiltGroups = new ArrayList<>();
        Map<ResourceLocation, DisplayEntry> rebuiltById = new HashMap<>();

        for (DynamicAttributeView.Group group : DynamicAttributeView.collect(player)) {
            List<DisplayEntry> entries = new ArrayList<>();
            for (DynamicAttributeView.Value value : group.values()) {
                Holder<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(value.id()).orElse(null);
                if (holder == null) continue;
                AttributeInstance instance = player.getAttribute(holder);
                if (instance == null) continue;
                List<SourceEntry> sources = collectSources(value.id(), holder, instance, equipment, effects);
                DisplayEntry entry = new DisplayEntry(value.id(), holder, value.name(), description(value.id()),
                    group.category(), instance.getBaseValue(), instance.getValue(),
                    DynamicAttributeView.formatValue(value.id(), holder.value(), instance.getBaseValue()),
                    DynamicAttributeView.formatValue(value.id(), holder.value(), instance.getValue()), sources);
                entries.add(entry);
                rebuiltById.put(entry.id(), entry);
            }
            if (!entries.isEmpty()) rebuiltGroups.add(new Group(group.category(), entries));
        }

        groups = List.copyOf(rebuiltGroups);
        byId = Map.copyOf(rebuiltById);
        if (selectedId == null || !byId.containsKey(selectedId))
            selectedId = groups.stream().flatMap(group -> group.entries().stream()).map(DisplayEntry::id)
                .findFirst().orElse(null);
        lastFingerprint = fingerprint;
        ticksSinceRefresh = 0;
    }

    private static List<SourceEntry> collectSources(ResourceLocation attributeId, Holder<Attribute> holder,
                                                     AttributeInstance instance,
                                                     Map<ModifierKey, EquipmentSource> equipment,
                                                     Map<ModifierKey, Component> effects) {
        List<SourceEntry> result = new ArrayList<>();
        for (AttributeModifier modifier : instance.getModifiers()) {
            ModifierKey key = new ModifierKey(attributeId, modifier.id());
            EquipmentSource item = equipment.get(key);
            Component effect = effects.get(key);
            SourceKind kind;
            Component name;
            ItemStack icon = ItemStack.EMPTY;
            Optional<Stat> stat = AttributeService.sourceStat(modifier.id());
            if (stat.isPresent()) {
                kind = SourceKind.SOUL_ASCENSION;
                name = Component.translatable("screen.soul_ascension.source.stat",
                    Component.translatable("stat.soul_ascension.short." + stat.get().name().toLowerCase(java.util.Locale.ROOT)));
            } else if (item != null) {
                kind = SourceKind.EQUIPMENT;
                icon = item.stack().copy();
                name = Component.translatable("screen.soul_ascension.source.equipment",
                    item.stack().getHoverName(), Component.translatable("item.modifiers." + slotGroupName(item.slot())));
            } else if (effect != null) {
                kind = SourceKind.EFFECT;
                name = Component.translatable("screen.soul_ascension.source.effect", effect);
            } else if (!modifier.id().getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)
                && !modifier.id().getNamespace().equals(SoulAscensionMod.MOD_ID)) {
                kind = SourceKind.OTHER_MOD;
                String displayName = ModList.get().getModContainerById(modifier.id().getNamespace())
                    .map(container -> container.getModInfo().getDisplayName()).orElse(modifier.id().getNamespace());
                name = Component.translatable("screen.soul_ascension.source.other_mod", displayName);
            } else {
                kind = SourceKind.UNKNOWN;
                name = Component.translatable("screen.soul_ascension.source.unknown", modifier.id().toString());
            }
            result.add(new SourceEntry(kind, name, modifier.id(), icon, modifier.amount(), modifier.operation(),
                holder.value().toValueComponent(modifier.operation(), modifier.amount(), TooltipFlag.NORMAL)));
        }
        result.sort(Comparator.comparingInt((SourceEntry source) -> source.kind().ordinal())
            .thenComparing(source -> source.modifierId().toString()));
        return result;
    }

    private static Map<ModifierKey, EquipmentSource> equipmentSources(net.minecraft.client.player.LocalPlayer player) {
        Map<ModifierKey, EquipmentSource> result = new HashMap<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            stack.forEachModifier(slot, (attribute, modifier) -> attribute.unwrapKey().ifPresent(key ->
                result.put(new ModifierKey(key.location(), modifier.id()), new EquipmentSource(stack, slot))));
        }
        return result;
    }

    private static Map<ModifierKey, Component> effectSources(net.minecraft.client.player.LocalPlayer player) {
        Map<ModifierKey, Component> result = new HashMap<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            Component name = Component.translatable(instance.getEffect().value().getDescriptionId());
            instance.getEffect().value().createModifiers(instance.getAmplifier(), (attribute, modifier) ->
                attribute.unwrapKey().ifPresent(key -> result.put(new ModifierKey(key.location(), modifier.id()), name)));
        }
        return result;
    }

    private static long fingerprint(net.minecraft.client.player.LocalPlayer player, PlayerProgress progress) {
        long value = progress.hashCode();
        for (EquipmentSlot slot : EquipmentSlot.values())
            value = value * 31L + ItemStack.hashItemAndComponents(player.getItemBySlot(slot));
        for (MobEffectInstance effect : player.getActiveEffects()) {
            ResourceLocation id = effect.getEffect().unwrapKey().map(key -> key.location())
                .orElse(ResourceLocation.fromNamespaceAndPath("minecraft", "unknown"));
            value = value * 31L + id.hashCode();
            value = value * 31L + effect.getAmplifier();
        }
        return value;
    }

    private static Component description(ResourceLocation id) {
        String key = "attribute_description.soul_ascension." + id.getNamespace() + "."
            + id.getPath().replace('/', '.');
        return I18n.exists(key) ? Component.translatable(key)
            : Component.translatable("screen.soul_ascension.attribute.no_description");
    }

    private static String slotGroupName(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> "mainhand";
            case OFFHAND -> "offhand";
            case FEET -> "feet";
            case LEGS -> "legs";
            case CHEST -> "chest";
            case HEAD -> "head";
            case BODY -> "body";
        };
    }
}
