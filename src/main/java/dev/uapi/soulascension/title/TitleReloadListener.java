package dev.uapi.soulascension.title;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class TitleReloadListener {
    private TitleReloadListener() {}

    @SubscribeEvent
    public static void addListener(AddReloadListenerEvent event) {
        event.addListener((ResourceManagerReloadListener) TitleRegistry::reload);
    }
}
