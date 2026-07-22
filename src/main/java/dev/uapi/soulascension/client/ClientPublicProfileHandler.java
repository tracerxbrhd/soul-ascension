package dev.uapi.soulascension.client;

import dev.uapi.soulascension.network.PublicProfilePayload;
import net.minecraft.client.Minecraft;

public final class ClientPublicProfileHandler {
    private ClientPublicProfileHandler() {}

    public static void open(PublicProfilePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (minecraft.player.getUUID().equals(payload.profile().playerId())) {
            minecraft.setScreenAndShow(new CharacterScreen());
        } else {
            minecraft.setScreenAndShow(new CharacterScreen(payload.profile()));
        }
    }
}
