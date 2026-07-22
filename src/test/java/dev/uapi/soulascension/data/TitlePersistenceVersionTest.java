package dev.uapi.soulascension.data;

import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TitlePersistenceVersionTest {
    @Test
    void currentTitleStructuresEncodeTheirExactVersion() {
        var titleProgressJson = TitleProgress.CODEC.encodeStart(JsonOps.INSTANCE, TitleProgress.initial())
            .getOrThrow();
        var titleCountersJson = TitleCounters.CODEC.encodeStart(JsonOps.INSTANCE, TitleCounters.initial())
            .getOrThrow();
        ActiveTitleData active = ActiveTitleData.of(Identifier.parse("soul_ascension:none"));
        var activeTitleJson = ActiveTitleData.CODEC.encodeStart(JsonOps.INSTANCE, active).getOrThrow();

        assertEquals(TitleProgress.DATA_VERSION,
            titleProgressJson.getAsJsonObject().get("dataVersion").getAsInt());
        assertEquals(TitleCounters.DATA_VERSION,
            titleCountersJson.getAsJsonObject().get("dataVersion").getAsInt());
        assertEquals(ActiveTitleData.DATA_VERSION,
            activeTitleJson.getAsJsonObject().get("dataVersion").getAsInt());
    }

    @Test
    void unversionedPreviousTitleStructuresAreRejected() {
        var oldProgress = JsonParser.parseString("{\"unlocked\":[]}");
        var oldCounters = JsonParser.parseString("{\"entityKills\":{}}");
        var oldActive = JsonParser.parseString("\"soul_ascension:none\"");

        assertTrue(TitleProgress.CODEC.parse(JsonOps.INSTANCE, oldProgress).result().isEmpty());
        assertTrue(TitleCounters.CODEC.parse(JsonOps.INSTANCE, oldCounters).result().isEmpty());
        assertTrue(ActiveTitleData.CODEC.parse(JsonOps.INSTANCE, oldActive).result().isEmpty());
    }
}
