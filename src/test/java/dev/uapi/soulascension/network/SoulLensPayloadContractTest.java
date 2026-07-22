package dev.uapi.soulascension.network;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class SoulLensPayloadContractTest {
    private static final UUID TARGET = UUID.fromString("a3693e02-553d-4d8a-a9d7-09541e0e5c91");

    @Test
    void responseEchoesObservationSessionAndUsesLightweightProjection() {
        SoulLensProfileData profile = new SoulLensProfileData(TARGET, "Player", 7,
            Identifier.fromNamespaceAndPath("soul_ascension", "none"),
            1, 2, 3, 4, 5, List.of());
        SoulLensProfilePayload payload = new SoulLensProfilePayload(
            TARGET, 42L, SoulLensProfilePayload.VISIBLE, profile);

        assertEquals(42L, payload.observationId());
        assertEquals(profile, payload.profile());
        List<String> componentNames = Arrays.stream(SoulLensProfileData.class.getRecordComponents())
            .map(component -> component.getName().toLowerCase(java.util.Locale.ROOT)).toList();
        assertFalse(componentNames.stream().anyMatch(name -> name.contains("skin")));
    }

    @Test
    void statusAndProfilePresenceMustAgree() {
        SoulLensProfileData profile = new SoulLensProfileData(TARGET, "Player", 7,
            Identifier.fromNamespaceAndPath("soul_ascension", "none"),
            1, 2, 3, 4, 5, List.of());

        assertThrows(IllegalArgumentException.class, () -> new SoulLensProfilePayload(
            TARGET, 1L, SoulLensProfilePayload.VISIBLE, null));
        assertThrows(IllegalArgumentException.class, () -> new SoulLensProfilePayload(
            TARGET, 1L, SoulLensProfilePayload.OUT_OF_RANGE, profile));
        SoulLensProfileData wrongTarget = new SoulLensProfileData(UUID.randomUUID(), "Other", 7,
            Identifier.fromNamespaceAndPath("soul_ascension", "none"),
            1, 2, 3, 4, 5, List.of());
        assertThrows(IllegalArgumentException.class, () -> new SoulLensProfilePayload(
            TARGET, 1L, SoulLensProfilePayload.VISIBLE, wrongTarget));
        assertDoesNotThrow(() -> new SoulLensProfilePayload(
            TARGET, 1L, SoulLensProfilePayload.OUT_OF_RANGE, null));
    }
}
