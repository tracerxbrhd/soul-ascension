package dev.uapi.soulascension.progression;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import dev.uapi.soulascension.config.SoulAscensionRuntimeConfig;
import dev.uapi.soulascension.network.SoulLensProfilePayload;
import dev.uapi.soulascension.network.PublicProfileData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulLensService {
    private record CachedProfile(int targetTick, PublicProfileData profile) {}
    private static final Map<UUID, Integer> LAST_REQUEST_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, CachedProfile> PROFILE_CACHE = new ConcurrentHashMap<>();

    private SoulLensService() {}

    public static void inspect(ServerPlayer viewer, UUID targetId) {
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        if (!config.soulLensEnabled()
            || !viewer.isUsingItem() || !viewer.getUseItem().is(SoulAscensionMod.SOUL_LENS.get())) return;
        int now = viewer.tickCount;
        int interval = config.soulLensUpdateInterval();
        Integer previous = LAST_REQUEST_TICK.put(viewer.getUUID(), now);
        if (previous != null && now - previous < Math.max(1, interval - 1)) return;

        ServerPlayer target = viewer.server.getPlayerList().getPlayer(targetId);
        double range = config.soulLensRange();
        if (target == null || target == viewer || viewer.distanceToSqr(target) > range * range
            || config.soulLensRequireLineOfSight() && !viewer.hasLineOfSight(target)) {
            send(viewer, new SoulLensProfilePayload(targetId, SoulLensProfilePayload.OUT_OF_RANGE, null));
            return;
        }
        CachedProfile cached = PROFILE_CACHE.get(targetId);
        if (cached == null || target.tickCount - cached.targetTick() >= Math.max(2, interval)) {
            cached = new CachedProfile(target.tickCount, PublicProfileService.snapshot(target));
            PROFILE_CACHE.put(targetId, cached);
        }
        send(viewer, new SoulLensProfilePayload(targetId, SoulLensProfilePayload.VISIBLE, cached.profile()));
    }

    public static void forget(UUID playerId) {
        LAST_REQUEST_TICK.remove(playerId);
        PROFILE_CACHE.remove(playerId);
    }

    private static void send(ServerPlayer player, SoulLensProfilePayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
