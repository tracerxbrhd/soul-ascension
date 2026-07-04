package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ProgressionRulesPayload(boolean amnesiaPointLossEnabled, double amnesiaPointLossPercent,
                                      boolean soulLensEnabled, double soulLensRange,
                                      int soulLensUpdateInterval, boolean soulLensBlockHotbarScroll,
                                      double soulLensIdleOverlayOpacity, double soulLensActiveOverlayOpacity,
                                      double soulLensHiddenOverlayOpacity, boolean soulLensShowIdleHint)
        implements CustomPacketPayload {
    public static final Type<ProgressionRulesPayload> TYPE = new Type<>(SoulAscensionMod.id("progression_rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionRulesPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ProgressionRulesPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ProgressionRulesPayload(buffer.readBoolean(), buffer.readDouble(), buffer.readBoolean(),
                buffer.readDouble(), buffer.readVarInt(), buffer.readBoolean(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readBoolean());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, ProgressionRulesPayload value) {
            buffer.writeBoolean(value.amnesiaPointLossEnabled());
            buffer.writeDouble(value.amnesiaPointLossPercent());
            buffer.writeBoolean(value.soulLensEnabled());
            buffer.writeDouble(value.soulLensRange());
            buffer.writeVarInt(value.soulLensUpdateInterval());
            buffer.writeBoolean(value.soulLensBlockHotbarScroll());
            buffer.writeDouble(value.soulLensIdleOverlayOpacity());
            buffer.writeDouble(value.soulLensActiveOverlayOpacity());
            buffer.writeDouble(value.soulLensHiddenOverlayOpacity());
            buffer.writeBoolean(value.soulLensShowIdleHint());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
