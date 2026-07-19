package dev.uapi.soulascension.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ClientProgressionRulesTest {
    @Test
    void preservesUnlimitedLevelSentinelAndRejectsNonFinitePresentationValues() {
        ClientProgressionRules.replace(new ProgressionRulesPayload(
            0, true, Double.NaN, false, 100,
            true, Double.POSITIVE_INFINITY, 10, true,
            Double.NaN, Double.NEGATIVE_INFINITY, true,
            Double.NaN, true, true));

        assertEquals(0, ClientProgressionRules.maxLevel());
        assertEquals(0.0, ClientProgressionRules.amnesiaPointLossPercent());
        assertEquals(64.0, ClientProgressionRules.soulLensRange());
        assertEquals(1.0, ClientProgressionRules.soulLensIdleOverlayOpacity());
        assertEquals(1.0, ClientProgressionRules.soulLensActiveOverlayOpacity());
        assertEquals(0.0, ClientProgressionRules.intelligenceExperienceBonusPerPoint());
    }
}
