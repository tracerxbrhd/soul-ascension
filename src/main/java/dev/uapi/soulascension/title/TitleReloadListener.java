package dev.uapi.soulascension.title;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

public final class TitleReloadListener {
    private TitleReloadListener() {}

    @SubscribeEvent
    public static void addListener(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath("soul_ascension", "titles"),
            (ResourceManagerReloadListener) TitleRegistry::reload);
    }
}
