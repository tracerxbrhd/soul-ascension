package dev.uapi.soulascension.integration.epicfight;

import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.util.function.Consumer;

/** Direct Epic Fight API bridge. This class must only be loaded after the epicfight mod-id check. */
public final class EpicFightIntegration {
    private EpicFightIntegration() {}

    public static Consumer<ServerPlayer> bootstrap(Consumer<ServerPlayer> requestAttributeRefresh) {
        EpicFightEventHooks.Player.CHANGE_INNATE_SKILL.registerEvent(event ->
            requestAttributeRefresh.accept(event.getPlayerPatch().getOriginal()), "soul_ascension");
        // Returning the method reference resolves this API bridge while keeping it isolated from
        // all classes that are loaded when Epic Fight is absent.
        return EpicFightIntegration::afterAttributesApplied;
    }

    private static void afterAttributesApplied(ServerPlayer player) {
        ServerPlayerPatch patch = EpicFightCapabilities.getServerPlayerPatch(player);
        if (patch != null) patch.clampMaxAttributes();
    }
}
