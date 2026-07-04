package dev.uapi.soulascension.network;

import dev.uapi.soulascension.client.ClientPublicProfileHandler;
import dev.uapi.soulascension.client.ClientSoulAltarHandler;
import dev.uapi.soulascension.client.SoulLensOverlay;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.progression.SoulAltarService;
import dev.uapi.soulascension.progression.SoulLensService;
import dev.uapi.soulascension.title.TitleService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SoulAscensionNetwork {
    private SoulAscensionNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("5");
        registrar.playToServer(SpendStatPayload.TYPE, SpendStatPayload.STREAM_CODEC, (payload, context) -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (payload.statOrdinal() >= 0 && payload.statOrdinal() < Stat.values().length
                && (payload.delta() == 1 || payload.delta() == -1)) {
                SoulAscensionService.changeStat(player, Stat.values()[payload.statOrdinal()], payload.delta());
            }
        });
        registrar.playToServer(SelectTitlePayload.TYPE, SelectTitlePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) TitleService.select(player, payload.titleId());
        });
        registrar.playToClient(TitleDefinitionsPayload.TYPE, TitleDefinitionsPayload.STREAM_CODEC,
            (payload, context) -> ClientTitleCatalog.replace(payload.titles()));
        registrar.playToClient(PublicProfilePayload.TYPE, PublicProfilePayload.STREAM_CODEC,
            (payload, context) -> ClientPublicProfileHandler.open(payload));
        registrar.playToClient(ProgressionRulesPayload.TYPE, ProgressionRulesPayload.STREAM_CODEC,
            (payload, context) -> ClientProgressionRules.replace(payload));
        registrar.playToClient(SoulAltarOpenPayload.TYPE, SoulAltarOpenPayload.STREAM_CODEC,
            (payload, context) -> ClientSoulAltarHandler.open(payload));
        registrar.playToServer(SoulAltarActionPayload.TYPE, SoulAltarActionPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player)
                SoulAltarService.handleAction(player, payload.altarPos(), payload.action(), payload.value());
        });
        registrar.playToServer(SoulLensRequestPayload.TYPE, SoulLensRequestPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) SoulLensService.inspect(player, payload.targetId());
        });
        registrar.playToClient(SoulLensProfilePayload.TYPE, SoulLensProfilePayload.STREAM_CODEC,
            (payload, context) -> SoulLensOverlay.receive(payload));
    }

    public static void syncRules(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ProgressionRulesPayload(
            SoulAscensionServerConfig.AMNESIA_POINT_LOSS_ENABLED.get(),
            SoulAscensionServerConfig.AMNESIA_POINT_LOSS_PERCENT.get(),
            SoulAscensionServerConfig.SOUL_LENS_ENABLED.get(), SoulAscensionServerConfig.SOUL_LENS_RANGE.get(),
            SoulAscensionServerConfig.SOUL_LENS_UPDATE_INTERVAL.get(),
            SoulAscensionServerConfig.SOUL_LENS_BLOCK_HOTBAR_SCROLL.get()));
    }
}
