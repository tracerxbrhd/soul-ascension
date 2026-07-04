package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SoulAltarActionPayload(BlockPos altarPos, int action, boolean value) implements CustomPacketPayload {
    public static final int RESPEC = 0;
    public static final int SET_VISIBILITY = 1;
    public static final Type<SoulAltarActionPayload> TYPE = new Type<>(SoulAscensionMod.id("soul_altar_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulAltarActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SoulAltarActionPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SoulAltarActionPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, SoulAltarActionPayload value) {
            buffer.writeBlockPos(value.altarPos()); buffer.writeVarInt(value.action()); buffer.writeBoolean(value.value());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
