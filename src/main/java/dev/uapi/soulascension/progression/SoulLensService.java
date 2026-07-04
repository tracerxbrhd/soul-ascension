package dev.uapi.soulascension.progression;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.network.SoulLensProfilePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulLensService {
    private static final Map<UUID, Integer> LAST_REQUEST_TICK = new ConcurrentHashMap<>();

    private SoulLensService() {}

    public static void inspect(ServerPlayer viewer, UUID targetId) {
        if (!SoulAscensionServerConfig.SOUL_LENS_ENABLED.get()
            || !viewer.isUsingItem() || !viewer.getUseItem().is(SoulAscensionMod.SOUL_LENS.get())) return;
        int now = viewer.tickCount;
        int interval = SoulAscensionServerConfig.SOUL_LENS_UPDATE_INTERVAL.get();
        Integer previous = LAST_REQUEST_TICK.put(viewer.getUUID(), now);
        if (previous != null && now - previous < Math.max(1, interval - 1)) return;

        ServerPlayer target = viewer.server.getPlayerList().getPlayer(targetId);
        double range = SoulAscensionServerConfig.SOUL_LENS_RANGE.get();
        if (target == null || target == viewer || viewer.distanceToSqr(target) > range * range
            || SoulAscensionServerConfig.SOUL_LENS_REQUIRE_LINE_OF_SIGHT.get() && !viewer.hasLineOfSight(target)) {
            send(viewer, new SoulLensProfilePayload(targetId, SoulLensProfilePayload.OUT_OF_RANGE, null));
            return;
        }
        if (!ProfilePrivacyService.mayInspectWithLens(viewer, target)) {
            send(viewer, new SoulLensProfilePayload(targetId, SoulLensProfilePayload.HIDDEN, null));
            return;
        }
        send(viewer, new SoulLensProfilePayload(targetId, SoulLensProfilePayload.VISIBLE,
            PublicProfileService.snapshot(target)));
    }

    private static void send(ServerPlayer player, SoulLensProfilePayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
