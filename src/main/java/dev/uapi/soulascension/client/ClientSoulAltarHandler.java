package dev.uapi.soulascension.client;

import dev.uapi.soulascension.network.SoulAltarOpenPayload;
import net.minecraft.client.Minecraft;

public final class ClientSoulAltarHandler {
    private ClientSoulAltarHandler() {}

    public static void open(SoulAltarOpenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof SoulAltarScreen screen) screen.refreshFromServer(payload);
        else minecraft.setScreen(new SoulAltarScreen(payload));
    }
}
