package dev.uapi.soulascension.network;

import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import dev.uapi.soulascension.config.SoulAscensionRuntimeConfig;
import dev.uapi.soulascension.config.AttributeRewardsConfig;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.progression.SoulLensService;
import dev.uapi.soulascension.title.TitleService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class SoulAscensionNetwork {
    /** Exact 2.0 wire contract, including versioned synchronized player attachments. */
    public static final String PROTOCOL_VERSION = "11";

    private SoulAscensionNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(ApplyStatAllocationPayload.TYPE, ApplyStatAllocationPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                boolean accepted = SoulAscensionService.applyAllocation(player, payload.increments());
                PacketDistributor.sendToPlayer(player, new StatAllocationResultPayload(accepted));
            }));
        registrar.playToServer(SelectTitlePayload.TYPE, SelectTitlePayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) TitleService.select(player, payload.titleId());
        }));
        registrar.playToServer(SoulLensRequestPayload.TYPE, SoulLensRequestPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                SoulLensService.inspect(player, payload.targetId(), payload.observationId());
        }));
        if (FMLEnvironment.dist == Dist.CLIENT) registerClientHandlers(registrar);
        else registerNoopClientHandlers(registrar);
    }

    private static void registerClientHandlers(PayloadRegistrar registrar) {
        ClientPayloadHandlers.register(registrar);
    }

    private static void registerNoopClientHandlers(PayloadRegistrar registrar) {
        registrar.playToClient(StatAllocationResultPayload.TYPE, StatAllocationResultPayload.STREAM_CODEC,
            (payload, context) -> {});
        registrar.playToClient(TitleDefinitionsPayload.TYPE, TitleDefinitionsPayload.STREAM_CODEC,
            (payload, context) -> {});
        registrar.playToClient(PublicProfilePayload.TYPE, PublicProfilePayload.STREAM_CODEC,
            (payload, context) -> {});
        registrar.playToClient(ProgressionRulesPayload.TYPE, ProgressionRulesPayload.STREAM_CODEC,
            (payload, context) -> {});
        registrar.playToClient(SoulLensProfilePayload.TYPE, SoulLensProfilePayload.STREAM_CODEC,
            (payload, context) -> {});
    }

    public static void syncRules(ServerPlayer player) {
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        PacketDistributor.sendToPlayer(player, new ProgressionRulesPayload(
            config.maxLevel(),
            config.amnesiaPointLossEnabled(),
            config.amnesiaPointLossPercent(),
            config.limitStatPoints(),
            config.maxPointsPerStat(),
            config.soulLensEnabled(), config.soulLensRange(),
            config.soulLensUpdateInterval(),
            config.soulLensBlockHotbarScroll(),
            config.soulLensIdleOverlayOpacity(),
            config.soulLensActiveOverlayOpacity(),
            config.soulLensShowIdleHint(),
            AttributeRewardsConfig.experienceBonusPerPoint(),
            AttributeRewardsConfig.affectsVanillaExperience(),
            AttributeRewardsConfig.affectsSoulProgression()));
    }
}
