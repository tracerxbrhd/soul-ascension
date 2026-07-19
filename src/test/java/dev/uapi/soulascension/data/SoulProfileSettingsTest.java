package dev.uapi.soulascension.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

final class SoulProfileSettingsTest {
    @Test
    void publicChoiceRoundTripsAndCanBeChanged() {
        SoulProfileSettings hidden = SoulProfileSettings.defaults().withPublicProfile(false);
        var encoded = SoulProfileSettings.CODEC.encodeStart(JsonOps.INSTANCE, hidden).getOrThrow();
        SoulProfileSettings decoded = SoulProfileSettings.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        assertFalse(decoded.publicProfile());
        assertTrue(decoded.withPublicProfile(true).publicProfile());
    }

    @Test
    void nonCurrentSettingsAreRejected() {
        var missingVersion = JsonParser.parseString("{\"publicProfile\":true}");
        var older = JsonParser.parseString("{\"dataVersion\":1,\"publicProfile\":true}");
        var future = JsonParser.parseString("{\"dataVersion\":99,\"publicProfile\":true}");

        assertTrue(SoulProfileSettings.CODEC.parse(JsonOps.INSTANCE, missingVersion).result().isEmpty());
        assertTrue(SoulProfileSettings.CODEC.parse(JsonOps.INSTANCE, older).result().isEmpty());
        assertTrue(SoulProfileSettings.CODEC.parse(JsonOps.INSTANCE, future).result().isEmpty());
    }
}
