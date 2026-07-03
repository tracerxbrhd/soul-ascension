package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SpendStatPayload(int statOrdinal, int delta) implements CustomPacketPayload {
    public static final Type<SpendStatPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(SoulAscensionMod.MOD_ID, "spend_stat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpendStatPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SpendStatPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SpendStatPayload(buffer.readVarInt(), buffer.readVarInt());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, SpendStatPayload value) {
            buffer.writeVarInt(value.statOrdinal()); buffer.writeVarInt(value.delta());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
