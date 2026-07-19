package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ProgressionRulesPayload(int maxLevel, boolean amnesiaPointLossEnabled, double amnesiaPointLossPercent,
                                      boolean limitStatPoints, int maxPointsPerStat,
                                      boolean soulLensEnabled, double soulLensRange,
                                      int soulLensUpdateInterval, boolean soulLensBlockHotbarScroll,
                                      double soulLensIdleOverlayOpacity, double soulLensActiveOverlayOpacity,
                                      boolean soulLensShowIdleHint, double intelligenceExperienceBonusPerPoint,
                                      boolean intelligenceAffectsVanillaExperience,
                                      boolean intelligenceAffectsSoulProgression)
        implements CustomPacketPayload {
    public static final Type<ProgressionRulesPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(SoulAscensionMod.MOD_ID, "progression_rules"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProgressionRulesPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public ProgressionRulesPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ProgressionRulesPayload(buffer.readVarInt(), buffer.readBoolean(), buffer.readDouble(), buffer.readBoolean(),
                buffer.readVarInt(), buffer.readBoolean(), buffer.readDouble(), buffer.readVarInt(), buffer.readBoolean(), buffer.readDouble(),
                buffer.readDouble(), buffer.readBoolean(), buffer.readDouble(), buffer.readBoolean(),
                buffer.readBoolean());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, ProgressionRulesPayload value) {
            buffer.writeVarInt(value.maxLevel());
            buffer.writeBoolean(value.amnesiaPointLossEnabled());
            buffer.writeDouble(value.amnesiaPointLossPercent());
            buffer.writeBoolean(value.limitStatPoints());
            buffer.writeVarInt(value.maxPointsPerStat());
            buffer.writeBoolean(value.soulLensEnabled());
            buffer.writeDouble(value.soulLensRange());
            buffer.writeVarInt(value.soulLensUpdateInterval());
            buffer.writeBoolean(value.soulLensBlockHotbarScroll());
            buffer.writeDouble(value.soulLensIdleOverlayOpacity());
            buffer.writeDouble(value.soulLensActiveOverlayOpacity());
            buffer.writeBoolean(value.soulLensShowIdleHint());
            buffer.writeDouble(value.intelligenceExperienceBonusPerPoint());
            buffer.writeBoolean(value.intelligenceAffectsVanillaExperience());
            buffer.writeBoolean(value.intelligenceAffectsSoulProgression());
        }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
