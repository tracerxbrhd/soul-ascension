package dev.uapi.soulascension.integration.profile;

import dev.uapi.api.profile.ProfileFacetRegistration;
import dev.uapi.api.profile.ProfileFacetRegistry;
import dev.uapi.soulascension.SoulAscensionMod;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Keeps the optional facet adapter scoped to one live server lifecycle. */
@EventBusSubscriber(modid = SoulAscensionMod.MOD_ID)
public final class SoulProfileFacetEvents {
    private static final Map<MinecraftServer, ProfileFacetRegistration> REGISTRATIONS = new IdentityHashMap<>();

    private SoulProfileFacetEvents() {
    }

    @SubscribeEvent
    public static synchronized void onServerStarted(ServerStartedEvent event) {
        REGISTRATIONS.computeIfAbsent(event.getServer(), server ->
            ProfileFacetRegistry.register(server, new SoulProfileFacetProvider()));
    }

    @SubscribeEvent
    public static synchronized void onServerStopping(ServerStoppingEvent event) {
        close(event.getServer());
    }

    @SubscribeEvent
    public static synchronized void onServerStopped(ServerStoppedEvent event) {
        close(event.getServer());
    }

    private static void close(MinecraftServer server) {
        ProfileFacetRegistration registration = REGISTRATIONS.remove(server);
        if (registration != null) registration.close();
    }
}
