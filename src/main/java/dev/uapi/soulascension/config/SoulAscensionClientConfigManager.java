package dev.uapi.soulascension.config;

/** Client counterpart of the runtime config cache. */
public final class SoulAscensionClientConfigManager {
    private static volatile SoulAscensionClientRuntimeConfig current = SoulAscensionClientRuntimeConfig.defaults();
    private static volatile boolean loaded;

    private SoulAscensionClientConfigManager() {}

    public static SoulAscensionClientRuntimeConfig current() {
        return current;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void reloadFromSpec() {
        current = new SoulAscensionClientRuntimeConfig(
            SoulAscensionClientConfig.SHOW_ATTRIBUTE_NAMESPACES.get(),
            SoulAscensionClientConfig.HIDDEN_ATTRIBUTES.get(),
            SoulAscensionClientConfig.VISIBLE_ATTRIBUTES.get(),
            SoulAscensionClientConfig.ATTRIBUTE_CATEGORIES.get(),
            SoulAscensionClientConfig.SHOW_PLAYER_PREVIEW.get()
        );
        loaded = true;
    }
}
