package dev.uapi.soulascension.compat.neoorigins;

import dev.uapi.soulascension.SoulAscensionMod;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Safe NeoOrigins compatibility entry point.
 *
 * <p>NeoOrigins 2.2.21 documents a current-origin query, but the published 1.21.1 JAR does not
 * expose it. Until that accessor is present in the public API this bootstrap deliberately
 * registers no profile provider or event listener.</p>
 */
public final class NeoOriginsCompatBootstrap {
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();

    private NeoOriginsCompatBootstrap() {
    }

    public static void bootstrap() {
        if (!BOOTSTRAPPED.compareAndSet(false, true)) return;
        SoulAscensionMod.LOGGER.warn(
            "NeoOrigins is installed, but native identity integration is disabled: "
                + "the published NeoOrigins 2.2.21 API has no public "
                + "NeoOriginsAPI.currentOrigin(ServerPlayer, ResourceLocation) accessor");
    }
}
