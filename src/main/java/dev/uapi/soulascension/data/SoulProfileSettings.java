package dev.uapi.soulascension.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Persisted, owner-controlled privacy for the public Soul profile. */
public record SoulProfileSettings(int dataVersion, boolean publicProfile) {
    public static final int DATA_VERSION = 2;

    public static final Codec<SoulProfileSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        PersistedDataVersion.codec(DATA_VERSION, "Soul profile settings").fieldOf("dataVersion")
            .forGetter(SoulProfileSettings::dataVersion),
        Codec.BOOL.fieldOf("publicProfile").forGetter(SoulProfileSettings::publicProfile)
    ).apply(instance, SoulProfileSettings::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulProfileSettings> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public SoulProfileSettings decode(RegistryFriendlyByteBuf buffer) {
                int version = buffer.readVarInt();
                PersistedDataVersion.require(version, DATA_VERSION, "Soul profile settings");
                return new SoulProfileSettings(version, buffer.readBoolean());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, SoulProfileSettings value) {
                buffer.writeVarInt(DATA_VERSION);
                buffer.writeBoolean(value.publicProfile());
            }
        };

    public SoulProfileSettings {
        PersistedDataVersion.require(dataVersion, DATA_VERSION, "Soul profile settings");
    }

    public static SoulProfileSettings defaults() {
        return new SoulProfileSettings(DATA_VERSION, true);
    }

    public SoulProfileSettings withPublicProfile(boolean value) {
        return new SoulProfileSettings(DATA_VERSION, value);
    }
}
