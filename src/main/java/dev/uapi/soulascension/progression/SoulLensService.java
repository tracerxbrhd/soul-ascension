package dev.uapi.soulascension.progression;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import dev.uapi.soulascension.config.SoulAscensionRuntimeConfig;
import dev.uapi.soulascension.network.SoulLensProfilePayload;
import dev.uapi.soulascension.network.SoulLensProfileData;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulLensService {
    private record CachedProfile(int targetTick, SoulLensProfileData profile) {}
    private record DeliveredProfile(UUID targetId, long observationId, int status,
                                    SoulLensProfileData profile) {}
    private static final Map<UUID, Integer> LAST_REQUEST_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, CachedProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, DeliveredProfile> LAST_DELIVERED = new ConcurrentHashMap<>();

    private SoulLensService() {}

    public static void inspect(ServerPlayer viewer, UUID targetId, long observationId) {
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        if (!config.soulLensEnabled()
            || !viewer.isUsingItem() || !viewer.getUseItem().is(SoulAscensionMod.SOUL_LENS.get())) return;
        int now = viewer.tickCount;
        int interval = config.soulLensUpdateInterval();
        Integer previous = LAST_REQUEST_TICK.put(viewer.getUUID(), now);
        if (previous != null && now - previous < Math.max(1, interval - 1)) return;

        ServerPlayer target = viewer.level().getServer().getPlayerList().getPlayer(targetId);
        double range = config.soulLensRange();
        if (target == null || target == viewer || viewer.distanceToSqr(target) > range * range
            || config.soulLensRequireLineOfSight() && !viewer.hasLineOfSight(target)) {
            sendIfChanged(viewer, targetId, observationId, SoulLensProfilePayload.OUT_OF_RANGE, null);
            return;
        }
        if (!target.getData(SoulAscensionAttachments.PROFILE_SETTINGS).publicProfile()) {
            sendIfChanged(viewer, targetId, observationId, SoulLensProfilePayload.PRIVATE, null);
            return;
        }
        CachedProfile cached = PROFILE_CACHE.get(targetId);
        if (cached == null || target.tickCount - cached.targetTick() >= Math.max(2, interval)) {
            cached = new CachedProfile(target.tickCount, PublicProfileService.soulLensSnapshot(target));
            PROFILE_CACHE.put(targetId, cached);
        }
        sendIfChanged(viewer, targetId, observationId, SoulLensProfilePayload.VISIBLE, cached.profile());
    }

    public static void forget(UUID playerId) {
        LAST_REQUEST_TICK.remove(playerId);
        PROFILE_CACHE.remove(playerId);
        LAST_DELIVERED.remove(playerId);
        LAST_DELIVERED.entrySet().removeIf(entry -> entry.getValue().targetId().equals(playerId));
    }

    private static void sendIfChanged(ServerPlayer viewer, UUID targetId, long observationId,
                                      int status, SoulLensProfileData profile) {
        DeliveredProfile next = new DeliveredProfile(targetId, observationId, status, profile);
        if (next.equals(LAST_DELIVERED.put(viewer.getUUID(), next))) return;
        PacketDistributor.sendToPlayer(viewer,
            new SoulLensProfilePayload(targetId, observationId, status, profile));
    }
}
