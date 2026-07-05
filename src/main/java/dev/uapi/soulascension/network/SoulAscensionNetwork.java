package dev.uapi.soulascension.network;

import dev.uapi.soulascension.client.ClientPublicProfileHandler;
import dev.uapi.soulascension.client.CharacterScreen;
import dev.uapi.soulascension.client.SoulLensOverlay;
import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import dev.uapi.soulascension.config.SoulAscensionRuntimeConfig;
import dev.uapi.soulascension.config.AttributeRewardsConfig;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.progression.SoulLensService;
import dev.uapi.soulascension.title.TitleService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SoulAscensionNetwork {
    private SoulAscensionNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("7");
        registrar.playToServer(ApplyStatAllocationPayload.TYPE, ApplyStatAllocationPayload.STREAM_CODEC,
            (payload, context) -> {
                if (!(context.player() instanceof ServerPlayer player)) return;
                boolean accepted = SoulAscensionService.applyAllocation(player, payload.increments());
                PacketDistributor.sendToPlayer(player, new StatAllocationResultPayload(accepted));
            });
        registrar.playToClient(StatAllocationResultPayload.TYPE, StatAllocationResultPayload.STREAM_CODEC,
            (payload, context) -> CharacterScreen.receiveAllocationResult(payload.accepted()));
        registrar.playToServer(SelectTitlePayload.TYPE, SelectTitlePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) TitleService.select(player, payload.titleId());
        });
        registrar.playToClient(TitleDefinitionsPayload.TYPE, TitleDefinitionsPayload.STREAM_CODEC,
            (payload, context) -> ClientTitleCatalog.replace(payload.titles()));
        registrar.playToClient(PublicProfilePayload.TYPE, PublicProfilePayload.STREAM_CODEC,
            (payload, context) -> ClientPublicProfileHandler.open(payload));
        registrar.playToClient(ProgressionRulesPayload.TYPE, ProgressionRulesPayload.STREAM_CODEC,
            (payload, context) -> ClientProgressionRules.replace(payload));
        registrar.playToServer(SoulLensRequestPayload.TYPE, SoulLensRequestPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) SoulLensService.inspect(player, payload.targetId());
        });
        registrar.playToClient(SoulLensProfilePayload.TYPE, SoulLensProfilePayload.STREAM_CODEC,
            (payload, context) -> SoulLensOverlay.receive(payload));
    }

    public static void syncRules(ServerPlayer player) {
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        PacketDistributor.sendToPlayer(player, new ProgressionRulesPayload(
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
