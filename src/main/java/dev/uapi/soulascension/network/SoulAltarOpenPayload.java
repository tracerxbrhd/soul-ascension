package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SoulAltarOpenPayload(BlockPos altarPos, boolean allowRespec, boolean requireConfirmation,
                                   boolean canToggleVisibility, boolean profileHidden, boolean effectiveHidden,
                                   String costType, int costAmount) implements CustomPacketPayload {
    public static final Type<SoulAltarOpenPayload> TYPE = new Type<>(SoulAscensionMod.id("soul_altar_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulAltarOpenPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public SoulAltarOpenPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SoulAltarOpenPayload(buffer.readBlockPos(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readUtf(32), buffer.readVarInt());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, SoulAltarOpenPayload value) {
            buffer.writeBlockPos(value.altarPos());
            buffer.writeBoolean(value.allowRespec());
            buffer.writeBoolean(value.requireConfirmation());
            buffer.writeBoolean(value.canToggleVisibility());
            buffer.writeBoolean(value.profileHidden());
            buffer.writeBoolean(value.effectiveHidden());
            buffer.writeUtf(value.costType(), 32);
            buffer.writeVarInt(value.costAmount());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
