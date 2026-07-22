package dev.uapi.soulascension.config;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EpicFightNativeRewardsTest {
    @Test
    void normalizesLegacyMinecraftAttributeIdsOnly() {
        assertEquals(Identifier.parse("minecraft:attack_damage"),
            AttributeRewardsConfig.canonicalAttributeId(Identifier.parse("minecraft:generic.attack_damage")));
        assertEquals(Identifier.parse("minecraft:mining_efficiency"),
            AttributeRewardsConfig.canonicalAttributeId(Identifier.parse("minecraft:player.mining_efficiency")));
        assertEquals(Identifier.parse("example:generic.attack_damage"),
            AttributeRewardsConfig.canonicalAttributeId(Identifier.parse("example:generic.attack_damage")));
    }

    @Test
    void requiresCleanVersionTwoConfig() {
        JsonObject root = new JsonObject();
        assertFalse(AttributeRewardsConfig.currentFormat(root));

        root.addProperty("format_version", AttributeRewardsConfig.FORMAT_VERSION);
        assertTrue(AttributeRewardsConfig.currentFormat(root));

        root.addProperty("format_version", 1);
        assertFalse(AttributeRewardsConfig.currentFormat(root));
    }

    @Test
    void addsExactNativeDefaults() {
        JsonObject root = new JsonObject();

        EpicFightNativeRewards.addDefaults(root);
        assertTrue(root.getAsJsonObject("integrations")
            .getAsJsonObject("epicfight").get("enabled").getAsBoolean());

        JsonObject stats = root.getAsJsonObject("stats");
        assertReward(stats, "strength", "epicfight:impact", 0.05, 3.0,
            "damage", "number", true);
        assertReward(stats, "strength", "epicfight:offhand_impact", 0.05, 3.0,
            "damage", "number", false);
        assertReward(stats, "endurance", "epicfight:stamina", 0.5, 40.0,
            "defense", "number", true);
        assertReward(stats, "agility", "epicfight:stamina_regen", 0.02, 2.0,
            "mobility", "multiplier", true);
    }

    @Test
    void readsExplicitlyDisabledIntegration() {
        JsonObject root = new JsonObject();
        JsonObject integrations = new JsonObject();
        JsonObject epicFight = new JsonObject();
        epicFight.addProperty("enabled", false);
        integrations.add("epicfight", epicFight);
        root.add("integrations", integrations);

        assertFalse(EpicFightNativeRewards.nativeEnabled(root, "epicfight"));
        assertFalse(epicFight.get("enabled").getAsBoolean());
    }

    @Test
    void missingOrMalformedToggleDefaultsToEnabled() {
        JsonObject root = new JsonObject();
        assertTrue(EpicFightNativeRewards.nativeEnabled(root, "epicfight"));

        JsonObject integrations = new JsonObject();
        JsonObject epicFight = new JsonObject();
        integrations.add("epicfight", epicFight);
        root.add("integrations", integrations);
        assertTrue(EpicFightNativeRewards.nativeEnabled(root, "epicfight"));

        epicFight.add("enabled", new JsonObject());
        assertTrue(EpicFightNativeRewards.nativeEnabled(root, "epicfight"));
    }

    @Test
    void parserPolicyDisablesOnlyMarkedNativeRewards() {
        JsonObject root = new JsonObject();
        JsonObject integrations = new JsonObject();
        JsonObject epicFight = new JsonObject();
        epicFight.addProperty("enabled", false);
        integrations.add("epicfight", epicFight);
        root.add("integrations", integrations);

        JsonObject generated = new JsonObject();
        generated.addProperty("native_integration", "epicfight");
        JsonObject manual = new JsonObject();

        assertFalse(AttributeRewardsConfig.nativeRewardEnabled(root, generated));
        assertTrue(AttributeRewardsConfig.nativeRewardEnabled(root, manual));

        epicFight.addProperty("enabled", true);
        assertTrue(AttributeRewardsConfig.nativeRewardEnabled(root, generated));
    }

    private static void assertReward(JsonObject stats, String statId, String attributeId,
                                     double amountPerPoint, double cap, String category,
                                     String formatter, boolean display) {
        JsonObject reward = stats.getAsJsonObject(statId)
            .getAsJsonObject("rewards")
            .getAsJsonObject(attributeId);
        assertEquals(10, reward.size());
        assertTrue(reward.get("enabled").getAsBoolean());
        assertEquals(amountPerPoint, reward.get("amount_per_point").getAsDouble());
        assertEquals("ADD_VALUE", reward.get("operation").getAsString());
        assertEquals(JsonNull.INSTANCE, reward.get("min_final"));
        assertEquals(cap, reward.get("cap").getAsDouble());
        assertEquals("epicfight", reward.get("required_mod").getAsString());
        assertEquals("epicfight", reward.get("native_integration").getAsString());
        assertEquals(display, reward.get("display").getAsBoolean());
        assertEquals(category, reward.get("display_category").getAsString());
        assertEquals(formatter, reward.get("formatter").getAsString());
    }
}
