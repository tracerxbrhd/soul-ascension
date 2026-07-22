package dev.uapi.soulascension.integration.epicfight;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Version-specific Epic Fight bridge.
 *
 * <p>Epic Fight does not currently publish a Minecraft 26.2 artifact. Keeping this class free of
 * Epic Fight linkage lets the core mod remain buildable and makes an accidentally installed,
 * incompatible build fail closed through the optional-integration bootstrap guard.</p>
 */
public final class EpicFightIntegration {
    private EpicFightIntegration() {}

    public static Consumer<ServerPlayer> bootstrap(Consumer<ServerPlayer> requestAttributeRefresh) {
        throw new UnsupportedOperationException(
            "Epic Fight has no supported Minecraft 26.2 API artifact; native integration is unavailable"
        );
    }
}
