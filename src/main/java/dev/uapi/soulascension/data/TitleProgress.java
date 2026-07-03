package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record TitleProgress(ResourceLocation activeTitle, Set<ResourceLocation> unlocked) {
    public static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(SoulAscensionMod.MOD_ID, "none");
    private static final Codec<Set<ResourceLocation>> ID_SET = ResourceLocation.CODEC.listOf()
        .xmap(LinkedHashSet::new, List::copyOf);
    public static final Codec<TitleProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.optionalFieldOf("activeTitle", NONE).forGetter(TitleProgress::activeTitle),
        ID_SET.optionalFieldOf("unlocked", Set.of()).forGetter(TitleProgress::unlocked)
    ).apply(instance, TitleProgress::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleProgress> STREAM_CODEC = new StreamCodec<>() {
        @Override public TitleProgress decode(RegistryFriendlyByteBuf buffer) {
            ResourceLocation active = buffer.readResourceLocation();
            int size = buffer.readVarInt();
            if (size < 0 || size > 4096) throw new IllegalArgumentException("Invalid unlocked title count: " + size);
            Set<ResourceLocation> values = new LinkedHashSet<>();
            for (int i = 0; i < size; i++) values.add(buffer.readResourceLocation());
            return new TitleProgress(active, values);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, TitleProgress value) {
            buffer.writeResourceLocation(value.activeTitle()); buffer.writeVarInt(value.unlocked().size());
            value.unlocked().forEach(buffer::writeResourceLocation);
        }
    };

    public TitleProgress {
        activeTitle = activeTitle == null ? NONE : activeTitle;
        unlocked = unlocked == null ? Set.of() : Set.copyOf(unlocked);
    }

    public static TitleProgress initial() { return new TitleProgress(NONE, Set.of()); }

    public TitleProgress unlock(ResourceLocation id) {
        if (unlocked.contains(id)) return this;
        Set<ResourceLocation> values = new LinkedHashSet<>(unlocked); values.add(id);
        return new TitleProgress(activeTitle, values);
    }

    public TitleProgress activeTitle(ResourceLocation id) { return new TitleProgress(id, unlocked); }

    public TitleProgress retain(Set<ResourceLocation> available) {
        Set<ResourceLocation> values = new LinkedHashSet<>(unlocked);
        values.retainAll(available);
        ResourceLocation active = available.contains(activeTitle) ? activeTitle : NONE;
        return values.equals(unlocked) && active.equals(activeTitle) ? this : new TitleProgress(active, values);
    }
}
