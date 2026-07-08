package dev.uapi.soulascension.network;

import dev.uapi.soulascension.client.CharacterScreen;
import dev.uapi.soulascension.client.ClientPublicProfileHandler;
import dev.uapi.soulascension.client.SoulLensOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@OnlyIn(Dist.CLIENT)
public final class ClientPayloadHandlers {
    private ClientPayloadHandlers() {}

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(StatAllocationResultPayload.TYPE, StatAllocationResultPayload.STREAM_CODEC,
            (payload, context) -> CharacterScreen.receiveAllocationResult(payload.accepted()));
        registrar.playToClient(TitleDefinitionsPayload.TYPE, TitleDefinitionsPayload.STREAM_CODEC,
            (payload, context) -> ClientTitleCatalog.replace(payload.titles()));
        registrar.playToClient(PublicProfilePayload.TYPE, PublicProfilePayload.STREAM_CODEC,
            (payload, context) -> ClientPublicProfileHandler.open(payload));
        registrar.playToClient(ProgressionRulesPayload.TYPE, ProgressionRulesPayload.STREAM_CODEC,
            (payload, context) -> ClientProgressionRules.replace(payload));
        registrar.playToClient(SoulLensProfilePayload.TYPE, SoulLensProfilePayload.STREAM_CODEC,
            (payload, context) -> SoulLensOverlay.receive(payload));
    }
}
