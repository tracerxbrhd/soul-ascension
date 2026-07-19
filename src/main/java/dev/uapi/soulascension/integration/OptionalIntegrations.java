package dev.uapi.soulascension.integration;

import dev.uapi.integration.IntegrationService;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.integration.epicfight.EpicFightIntegration;
import dev.uapi.soulascension.progression.AttributeService;
import dev.uapi.soulascension.progression.SoulAscensionService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Loads direct optional-mod adapters only after their owning mod is known to be present. */
public final class OptionalIntegrations {
    private static final Consumer<ServerPlayer> NOOP = ignored -> {};
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();
    private static final Set<UUID> ATTRIBUTE_REFRESH_QUEUE = ConcurrentHashMap.newKeySet();
    private static volatile Consumer<ServerPlayer> epicFightAfterAttributes = NOOP;
    private static volatile boolean epicFightActive;

    private OptionalIntegrations() {}

    public static void bootstrap() {
        if (!BOOTSTRAPPED.compareAndSet(false, true) || !IntegrationService.isLoaded("epicfight")) return;
        try {
            epicFightAfterAttributes = EpicFightIntegration.bootstrap(OptionalIntegrations::requestAttributeRefresh);
            epicFightActive = true;
            SoulAscensionMod.LOGGER.info("Native Epic Fight integration enabled");
        } catch (LinkageError | RuntimeException exception) {
            epicFightAfterAttributes = NOOP;
            SoulAscensionMod.LOGGER.error(
                "Epic Fight is installed, but its supported API could not be initialized; native integration is disabled",
                exception);
        }
    }

    public static void afterAttributesApplied(ServerPlayer player) {
        try {
            epicFightAfterAttributes.accept(player);
        } catch (LinkageError | RuntimeException exception) {
            if (epicFightActive) {
                epicFightActive = false;
                epicFightAfterAttributes = NOOP;
                SoulAscensionMod.LOGGER.error(
                    "Epic Fight API failed while synchronizing player attributes; native integration is disabled",
                    exception);
            }
        }
    }

    public static boolean epicFightActive() {
        return epicFightActive;
    }

    /** Re-applies capped rewards after Epic Fight finishes changing weapon-dependent attributes. */
    public static void flushAttributeRefreshes(MinecraftServer server) {
        if (ATTRIBUTE_REFRESH_QUEUE.isEmpty()) return;
        for (UUID playerId : Set.copyOf(ATTRIBUTE_REFRESH_QUEUE)) {
            if (!ATTRIBUTE_REFRESH_QUEUE.remove(playerId)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) AttributeService.apply(player, SoulAscensionService.get(player));
        }
    }

    private static void requestAttributeRefresh(ServerPlayer player) {
        ATTRIBUTE_REFRESH_QUEUE.add(player.getUUID());
    }
}
