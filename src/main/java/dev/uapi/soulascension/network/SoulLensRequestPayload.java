package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;
import java.util.Objects;

public record SoulLensRequestPayload(UUID targetId, long observationId) implements CustomPacketPayload {
    public static final Type<SoulLensRequestPayload> TYPE = new Type<>(SoulAscensionMod.id("soul_lens_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulLensRequestPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SoulLensRequestPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SoulLensRequestPayload(buffer.readUUID(), buffer.readVarLong());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, SoulLensRequestPayload value) {
            buffer.writeUUID(value.targetId());
            buffer.writeVarLong(value.observationId());
        }
    };
    public SoulLensRequestPayload {
        Objects.requireNonNull(targetId, "targetId");
        if (observationId < 0) throw new IllegalArgumentException("observationId must not be negative");
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
