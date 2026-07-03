package dev.uapi.soulascension.network;

import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.title.TitleService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SoulAscensionNetwork {
    private SoulAscensionNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
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
    }
}
