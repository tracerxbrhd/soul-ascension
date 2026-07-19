package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PublicProfilePayload(PublicProfileData profile) implements CustomPacketPayload {
    public static final Type<PublicProfilePayload> TYPE = new Type<>(SoulAscensionMod.id("public_profile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PublicProfilePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public PublicProfilePayload decode(RegistryFriendlyByteBuf buffer) {
            return new PublicProfilePayload(PublicProfileData.decode(buffer));
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, PublicProfilePayload value) {
            value.profile().encode(buffer);
        }
    };

    public PublicProfilePayload {
        java.util.Objects.requireNonNull(profile, "profile");
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
