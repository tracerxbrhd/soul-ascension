package dev.uapi.soulascension.data;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PlayerProgressTest {
    @Test
    void currentFormatRoundTripsWithoutRepair() {
        PlayerProgress original = new PlayerProgress(12, 34.5, 67.0, 8, 1, 2, 3, 4, 5);

        JsonObject encoded = PlayerProgress.CODEC.encodeStart(JsonOps.INSTANCE, original)
            .result().orElseThrow().getAsJsonObject();
        PlayerProgress decoded = PlayerProgress.CODEC.parse(JsonOps.INSTANCE, encoded)
            .result().orElseThrow();

        assertEquals(PlayerProgress.DATA_VERSION, encoded.get("dataVersion").getAsInt());
        assertEquals(original, decoded);
        assertTrue(decoded.isUsable());
    }

    @Test
    void legacySchemaIsNotAccepted() {
        JsonObject legacy = completeProgress();
        legacy.remove("dataVersion");
        legacy.addProperty("schema_version", 130);

        assertTrue(PlayerProgress.CODEC.parse(JsonOps.INSTANCE, legacy).result().isEmpty());
    }

    @Test
    void wrongVersionIsRejectedAndInvalidValuesAreReadOnly() {
        JsonObject wrongVersion = completeProgress();
        wrongVersion.addProperty("dataVersion", PlayerProgress.DATA_VERSION + 1);
        PlayerProgress invalidValue = new PlayerProgress(0, Double.NaN, 0, 0, 0, 0, 0, 0, 0);

        assertTrue(PlayerProgress.CODEC.parse(JsonOps.INSTANCE, wrongVersion).result().isEmpty());
        assertFalse(invalidValue.isUsable());
    }

    private static JsonObject completeProgress() {
        JsonObject value = new JsonObject();
        value.addProperty("dataVersion", PlayerProgress.DATA_VERSION);
        value.addProperty("level", 0);
        value.addProperty("damageProgress", 0.0);
        value.addProperty("requiredDamage", 0.0);
        value.addProperty("unspentPoints", 0);
        value.addProperty("strength", 0);
        value.addProperty("endurance", 0);
        value.addProperty("agility", 0);
        value.addProperty("intelligence", 0);
        value.addProperty("perception", 0);
        return value;
    }
}
