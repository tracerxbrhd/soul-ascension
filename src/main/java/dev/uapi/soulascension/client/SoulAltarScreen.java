package dev.uapi.soulascension.client;

import dev.uapi.soulascension.network.SoulAltarOpenPayload;

/** Character Screen in an authenticated Soul Altar context. */
public final class SoulAltarScreen extends CharacterScreen {
    public SoulAltarScreen(SoulAltarOpenPayload data) {
        super(CharacterScreenMode.SOUL_ALTAR, null, data);
    }

    public void refreshFromServer(SoulAltarOpenPayload data) {
        updateAltarData(data);
    }
}
