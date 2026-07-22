package dev.uapi.soulascension.title;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.uapi.integration.IntegrationService;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.data.TitleProgress;
import dev.uapi.soulascension.data.TitleCounters;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record TitleDefinition(Identifier id, String nameKey, String descriptionKey,
                              Identifier icon, int order, boolean hidden,
                              List<String> requiredMods, Conditions conditions) {
    public TitleDefinition {
        requiredMods = List.copyOf(requiredMods);
    }

    public boolean available() { return IntegrationService.areLoaded(requiredMods); }

    public boolean matches(ServerPlayer player, PlayerProgress progress, TitleProgress titles, TitleCounters counters) {
        return available() && conditions.matches(player, progress, titles, counters);
    }

    public static TitleDefinition parse(Identifier id, JsonObject json) {
        String name = string(json, "name", "title." + id.getNamespace() + "." + id.getPath());
        String description = string(json, "description", name + ".description");
        Identifier icon = Identifier.parse(string(json, "icon", "soul_ascension:textures/gui/icons/title.png"));
        int order = integer(json, "order", 0, -1_000_000, 1_000_000);
        boolean hidden = json.has("hidden") && json.get("hidden").getAsBoolean();
        List<String> mods = strings(json, "required_mods");
        Conditions conditions = Conditions.parse(json.has("conditions") ? json.getAsJsonObject("conditions") : new JsonObject());
        return new TitleDefinition(id, name, description, icon, order, hidden, mods, conditions);
    }

    public record Conditions(int minimumLevel, Map<Stat, Integer> minimumStats,
                             Map<Identifier, Long> entityKills,
                             Map<Identifier, Long> itemsCollected,
                             Map<Identifier, Long> blocksMined,
                             Set<Identifier> requiredTitles, long playTimeTicks) {
        public Conditions {
            minimumStats = Map.copyOf(minimumStats); entityKills = Map.copyOf(entityKills);
            itemsCollected = Map.copyOf(itemsCollected); blocksMined = Map.copyOf(blocksMined);
            requiredTitles = Set.copyOf(requiredTitles);
        }

        static Conditions parse(JsonObject json) {
            Map<Stat, Integer> stats = new EnumMap<>(Stat.class);
            if (json.has("stats")) json.getAsJsonObject("stats").entrySet().forEach(entry -> {
                Stat stat = Stat.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                int value = entry.getValue().getAsInt();
                if (value < 0) throw new IllegalArgumentException("title stat requirement must not be negative");
                stats.put(stat, value);
            });
            return new Conditions(integer(json, "minimum_level", 1, 1, 1_000_000), stats,
                counters(json, "entity_kills"), counters(json, "items_collected"), counters(json, "blocks_mined"),
                new LinkedHashSet<>(ids(json, "required_titles")), longValue(json, "play_time_ticks", 0));
        }

        boolean matches(ServerPlayer player, PlayerProgress progress, TitleProgress titles, TitleCounters counters) {
            if (progress.level() < minimumLevel || !titles.unlocked().containsAll(requiredTitles)) return false;
            if (minimumStats.entrySet().stream().anyMatch(entry -> progress.stat(entry.getKey()) < entry.getValue())) return false;
            if (entityKills.entrySet().stream().anyMatch(entry -> counters.entityKills(entry.getKey()) < entry.getValue())) return false;
            if (itemsCollected.entrySet().stream().anyMatch(entry -> counters.itemsCollected(entry.getKey()) < entry.getValue())) return false;
            if (blocksMined.entrySet().stream().anyMatch(entry -> counters.blocksMined(entry.getKey()) < entry.getValue())) return false;
            return player.getStats().getValue(Stats.CUSTOM, Stats.PLAY_TIME) >= playTimeTicks;
        }
    }

    private static Map<Identifier, Long> counters(JsonObject json, String field) {
        Map<Identifier, Long> result = new LinkedHashMap<>();
        if (!json.has(field)) return result;
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(field).entrySet()) {
            long value = entry.getValue().getAsLong();
            if (value < 1) throw new IllegalArgumentException(field + " values must be positive");
            result.put(Identifier.parse(entry.getKey()), value);
        }
        return result;
    }

    private static List<Identifier> ids(JsonObject json, String field) {
        List<Identifier> result = new ArrayList<>();
        if (json.has(field)) for (JsonElement element : json.getAsJsonArray(field))
            result.add(Identifier.parse(element.getAsString()));
        return result;
    }

    private static List<String> strings(JsonObject json, String field) {
        List<String> result = new ArrayList<>();
        if (json.has(field)) for (JsonElement element : json.getAsJsonArray(field)) result.add(element.getAsString());
        return result;
    }

    private static String string(JsonObject json, String field, String fallback) {
        return json.has(field) ? json.get(field).getAsString() : fallback;
    }

    private static int integer(JsonObject json, String field, int fallback, int min, int max) {
        int value = json.has(field) ? json.get(field).getAsInt() : fallback;
        if (value < min || value > max) throw new IllegalArgumentException(field + " must be in [" + min + ", " + max + "]");
        return value;
    }

    private static long longValue(JsonObject json, String field, long fallback) {
        long value = json.has(field) ? json.get(field).getAsLong() : fallback;
        if (value < 0) throw new IllegalArgumentException(field + " must not be negative");
        return value;
    }
}
