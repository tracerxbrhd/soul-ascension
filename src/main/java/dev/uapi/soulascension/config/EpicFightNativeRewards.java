package dev.uapi.soulascension.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

final class EpicFightNativeRewards {
    private static final String INTEGRATION_ID = "epicfight";

    private EpicFightNativeRewards() {}

    static void addDefaults(JsonObject root) {
        JsonObject integrations = ensureObject(root, "integrations");
        JsonObject epicFight = ensureObject(integrations, INTEGRATION_ID);
        epicFight.addProperty("enabled", true);

        JsonObject stats = ensureObject(root, "stats");
        addReward(stats, "strength", "epicfight:impact", 0.05, 3.0,
            "damage", "number", true);
        addReward(stats, "strength", "epicfight:offhand_impact", 0.05, 3.0,
            "damage", "number", false);
        addReward(stats, "endurance", "epicfight:stamina", 0.5, 40.0,
            "defense", "number", true);
        addReward(stats, "agility", "epicfight:stamina_regen", 0.02, 2.0,
            "mobility", "multiplier", true);
    }

    static boolean nativeEnabled(JsonObject root, String integrationId) {
        JsonElement integrations = root.get("integrations");
        if (integrations == null || !integrations.isJsonObject()) return true;

        JsonElement integration = integrations.getAsJsonObject().get(integrationId);
        if (integration == null || !integration.isJsonObject()) return true;

        JsonObject integrationObject = integration.getAsJsonObject();
        try {
            return integrationObject.has("enabled")
                ? integrationObject.get("enabled").getAsBoolean()
                : true;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    private static void addReward(JsonObject stats, String statId, String attributeId,
                                  double amountPerPoint, double cap, String category,
                                  String formatter, boolean display) {
        JsonObject stat = ensureObject(stats, statId);
        JsonObject rewards = ensureObject(stat, "rewards");

        JsonObject reward = new JsonObject();
        reward.addProperty("enabled", true);
        reward.addProperty("amount_per_point", amountPerPoint);
        reward.addProperty("operation", "ADD_VALUE");
        reward.add("min_final", JsonNull.INSTANCE);
        reward.addProperty("cap", cap);
        reward.addProperty("required_mod", INTEGRATION_ID);
        reward.addProperty("native_integration", INTEGRATION_ID);
        reward.addProperty("display", display);
        reward.addProperty("display_category", category);
        reward.addProperty("formatter", formatter);
        rewards.add(attributeId, reward);
    }

    private static JsonObject ensureObject(JsonObject parent, String key) {
        JsonElement existing = parent.get(key);
        if (existing != null && existing.isJsonObject()) return existing.getAsJsonObject();

        JsonObject created = new JsonObject();
        parent.add(key, created);
        return created;
    }
}
