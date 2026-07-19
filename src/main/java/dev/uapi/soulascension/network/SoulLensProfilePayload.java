package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;
import java.util.Objects;

public record SoulLensProfilePayload(UUID targetId, long observationId, int status,
                                     SoulLensProfileData profile) implements CustomPacketPayload {
    public static final int VISIBLE = 0;
    public static final int OUT_OF_RANGE = 1;
    public static final int PRIVATE = 2;
    public static final Type<SoulLensProfilePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(SoulAscensionMod.MOD_ID, "soul_lens_profile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulLensProfilePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SoulLensProfilePayload decode(RegistryFriendlyByteBuf buffer) {
            UUID id = buffer.readUUID();
            long observationId = buffer.readVarLong();
            int status = buffer.readVarInt();
            return new SoulLensProfilePayload(id, observationId, status,
                status == VISIBLE ? SoulLensProfileData.decode(buffer) : null);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, SoulLensProfilePayload value) {
            buffer.writeUUID(value.targetId());
            buffer.writeVarLong(value.observationId());
            buffer.writeVarInt(value.status());
            if (value.status() == VISIBLE) value.profile().encode(buffer);
        }
    };
    public SoulLensProfilePayload {
        Objects.requireNonNull(targetId, "targetId");
        if (observationId < 0) throw new IllegalArgumentException("observationId must not be negative");
        if (status != VISIBLE && status != OUT_OF_RANGE && status != PRIVATE)
            throw new IllegalArgumentException("Unknown Soul Lens status: " + status);
        if ((status == VISIBLE) != (profile != null))
            throw new IllegalArgumentException("Visible Soul Lens payload must contain exactly one profile");
        if (profile != null && !targetId.equals(profile.playerId()))
            throw new IllegalArgumentException("Soul Lens profile does not match the requested target");
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
