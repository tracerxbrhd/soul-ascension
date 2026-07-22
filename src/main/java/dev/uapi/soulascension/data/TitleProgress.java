package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record TitleProgress(Identifier activeTitle, Set<Identifier> unlocked) {
    public static final int DATA_VERSION = 2;
    public static final Identifier NONE = Identifier.fromNamespaceAndPath(SoulAscensionMod.MOD_ID, "none");
    private static final Codec<Set<Identifier>> ID_SET = Identifier.CODEC.listOf()
        .xmap(LinkedHashSet::new, List::copyOf);
    public static final Codec<TitleProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PersistedDataVersion.codec(DATA_VERSION, "Soul title progress").fieldOf("dataVersion")
            .forGetter(ignored -> DATA_VERSION),
        Identifier.CODEC.optionalFieldOf("activeTitle", NONE).forGetter(TitleProgress::activeTitle),
        ID_SET.optionalFieldOf("unlocked", Set.of()).forGetter(TitleProgress::unlocked)
    ).apply(instance, (dataVersion, activeTitle, unlocked) -> new TitleProgress(activeTitle, unlocked)));

    public static final StreamCodec<RegistryFriendlyByteBuf, TitleProgress> STREAM_CODEC = new StreamCodec<>() {
        @Override public TitleProgress decode(RegistryFriendlyByteBuf buffer) {
            PersistedDataVersion.require(buffer.readVarInt(), DATA_VERSION, "Soul title progress");
            Identifier active = buffer.readIdentifier();
            int size = buffer.readVarInt();
            if (size < 0 || size > 4096) throw new IllegalArgumentException("Invalid unlocked title count: " + size);
            Set<Identifier> values = new LinkedHashSet<>();
            for (int i = 0; i < size; i++) values.add(buffer.readIdentifier());
            return new TitleProgress(active, values);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, TitleProgress value) {
            buffer.writeVarInt(DATA_VERSION); buffer.writeIdentifier(value.activeTitle());
            buffer.writeVarInt(value.unlocked().size());
            value.unlocked().forEach(buffer::writeIdentifier);
        }
    };

    public TitleProgress {
        activeTitle = activeTitle == null ? NONE : activeTitle;
        unlocked = unlocked == null ? Set.of() : Set.copyOf(unlocked);
    }

    public static TitleProgress initial() { return new TitleProgress(NONE, Set.of()); }

    public TitleProgress unlock(Identifier id) {
        if (unlocked.contains(id)) return this;
        Set<Identifier> values = new LinkedHashSet<>(unlocked); values.add(id);
        return new TitleProgress(activeTitle, values);
    }

    public TitleProgress activeTitle(Identifier id) { return new TitleProgress(id, unlocked); }

    public TitleProgress retain(Set<Identifier> available) {
        Set<Identifier> values = new LinkedHashSet<>(unlocked);
        values.retainAll(available);
        Identifier active = available.contains(activeTitle) ? activeTitle : NONE;
        return values.equals(unlocked) && active.equals(activeTitle) ? this : new TitleProgress(active, values);
    }
}
