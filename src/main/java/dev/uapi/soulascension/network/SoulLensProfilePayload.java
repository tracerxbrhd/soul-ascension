package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record SoulLensProfilePayload(UUID targetId, int status, PublicProfileData profile) implements CustomPacketPayload {
    public static final int VISIBLE = 0;
    public static final int OUT_OF_RANGE = 1;
    public static final Type<SoulLensProfilePayload> TYPE = new Type<>(SoulAscensionMod.id("soul_lens_profile"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulLensProfilePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SoulLensProfilePayload decode(RegistryFriendlyByteBuf buffer) {
            UUID id = buffer.readUUID();
            int status = buffer.readVarInt();
            return new SoulLensProfilePayload(id, status,
                status == VISIBLE ? PublicProfileData.decode(buffer) : null);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, SoulLensProfilePayload value) {
            buffer.writeUUID(value.targetId());
            buffer.writeVarInt(value.status());
            if (value.status() == VISIBLE) value.profile().encode(buffer);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
