package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SoulLensRequestPayload(UUID targetId) implements CustomPacketPayload {
    public static final Type<SoulLensRequestPayload> TYPE = new Type<>(SoulAscensionMod.id("soul_lens_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulLensRequestPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SoulLensRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SoulLensRequestPayload(buffer.readUUID());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, SoulLensRequestPayload value) {
            buffer.writeUUID(value.targetId());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
