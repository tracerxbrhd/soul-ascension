package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SelectTitlePayload(Identifier titleId) implements CustomPacketPayload {
    public static final Type<SelectTitlePayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(SoulAscensionMod.MOD_ID, "select_title"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SelectTitlePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SelectTitlePayload decode(RegistryFriendlyByteBuf buffer) {
            return new SelectTitlePayload(buffer.readIdentifier());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, SelectTitlePayload value) {
            buffer.writeIdentifier(value.titleId());
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
