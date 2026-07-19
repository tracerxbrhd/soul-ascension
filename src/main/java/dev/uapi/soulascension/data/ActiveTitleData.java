package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/** Exact-version persisted and synchronized selection used by name-tag rendering. */
public record ActiveTitleData(int dataVersion, ResourceLocation titleId) {
    public static final int DATA_VERSION = 2;

    public static final Codec<ActiveTitleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PersistedDataVersion.codec(DATA_VERSION, "Soul active title").fieldOf("dataVersion")
            .forGetter(ActiveTitleData::dataVersion),
        ResourceLocation.CODEC.fieldOf("titleId").forGetter(ActiveTitleData::titleId)
    ).apply(instance, ActiveTitleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveTitleData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ActiveTitleData decode(RegistryFriendlyByteBuf buffer) {
            int version = buffer.readVarInt();
            PersistedDataVersion.require(version, DATA_VERSION, "Soul active title");
            return new ActiveTitleData(version, buffer.readResourceLocation());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ActiveTitleData value) {
            buffer.writeVarInt(value.dataVersion());
            buffer.writeResourceLocation(value.titleId());
        }
    };

    public ActiveTitleData {
        PersistedDataVersion.require(dataVersion, DATA_VERSION, "Soul active title");
        titleId = Objects.requireNonNull(titleId, "titleId");
    }

    public static ActiveTitleData none() {
        return of(TitleProgress.NONE);
    }

    public static ActiveTitleData of(ResourceLocation titleId) {
        return new ActiveTitleData(DATA_VERSION, titleId);
    }
}
