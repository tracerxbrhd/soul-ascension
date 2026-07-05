package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StatAllocationResultPayload(boolean accepted) implements CustomPacketPayload {
    public static final Type<StatAllocationResultPayload> TYPE = new Type<>(SoulAscensionMod.id("stat_allocation_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StatAllocationResultPayload> STREAM_CODEC =
        StreamCodec.of((buffer, value) -> buffer.writeBoolean(value.accepted()),
            buffer -> new StatAllocationResultPayload(buffer.readBoolean()));

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
