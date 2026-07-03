package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public record TitleCounters(Map<ResourceLocation, Long> entityKills,
                            Map<ResourceLocation, Long> itemsCollected,
                            Map<ResourceLocation, Long> blocksMined) {
    private static final Codec<Map<ResourceLocation, Long>> COUNTERS =
        Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG);
    public static final Codec<TitleCounters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        COUNTERS.optionalFieldOf("entityKills", Map.of()).forGetter(TitleCounters::entityKills),
        COUNTERS.optionalFieldOf("itemsCollected", Map.of()).forGetter(TitleCounters::itemsCollected),
        COUNTERS.optionalFieldOf("blocksMined", Map.of()).forGetter(TitleCounters::blocksMined)
    ).apply(instance, TitleCounters::new));

    public TitleCounters {
        entityKills = Map.copyOf(entityKills); itemsCollected = Map.copyOf(itemsCollected); blocksMined = Map.copyOf(blocksMined);
    }
    public static TitleCounters initial() { return new TitleCounters(Map.of(), Map.of(), Map.of()); }
    public long entityKills(ResourceLocation id) { return entityKills.getOrDefault(id, 0L); }
    public long itemsCollected(ResourceLocation id) { return itemsCollected.getOrDefault(id, 0L); }
    public long blocksMined(ResourceLocation id) { return blocksMined.getOrDefault(id, 0L); }
    public TitleCounters addEntityKill(ResourceLocation id) {
        return new TitleCounters(increment(entityKills, id, 1), itemsCollected, blocksMined);
    }
    public TitleCounters addCollectedItem(ResourceLocation id, long amount) {
        return new TitleCounters(entityKills, increment(itemsCollected, id, amount), blocksMined);
    }
    public TitleCounters addMinedBlock(ResourceLocation id) {
        return new TitleCounters(entityKills, itemsCollected, increment(blocksMined, id, 1));
    }
    private static Map<ResourceLocation, Long> increment(Map<ResourceLocation, Long> source, ResourceLocation id, long amount) {
        if (amount <= 0) return source;
        Map<ResourceLocation, Long> values = new LinkedHashMap<>(source);
        long old = values.getOrDefault(id, 0L);
        values.put(id, amount > Long.MAX_VALUE - old ? Long.MAX_VALUE : old + amount);
        return values;
    }
}
