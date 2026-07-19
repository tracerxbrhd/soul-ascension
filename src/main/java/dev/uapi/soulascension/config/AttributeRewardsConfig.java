package dev.uapi.soulascension.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.uapi.integration.IntegrationService;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.progression.AttributeService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Runtime tree config for stat rewards. Invalid entries are logged and skipped independently. */
public final class AttributeRewardsConfig {
    public static final String RELATIVE_PATH = "uapi/soul-ascension/attribute_rewards.json";
    static final int FORMAT_VERSION = 2;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile Snapshot snapshot = defaultSnapshot();

    public record Snapshot(Map<Stat, List<AttributeService.ConfiguredModifier>> rewards,
                           double experienceBonusPerPoint, boolean affectsVanillaExperience,
                           boolean affectsSoulProgression) {}

    private AttributeRewardsConfig() {}

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(RELATIVE_PATH);
    }

    public static void bootstrapDefaults() {
        Path target = path();
        if (!Files.exists(target)) {
            JsonObject root = defaultRoot();
            try {
                Files.createDirectories(target.getParent());
                Files.writeString(target, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                LOGGER.warn("Could not create {}: {}", target, exception.getMessage());
            }
        }
    }

    public static void reload() {
        Path target = path();
        try {
            JsonElement element = JsonParser.parseString(Files.readString(target, StandardCharsets.UTF_8));
            if (!element.isJsonObject()) throw new IllegalArgumentException("root must be an object");
            JsonObject root = element.getAsJsonObject();
            if (!currentFormat(root)) {
                LOGGER.error("Ignoring unsupported {}. Soul Ascension 2.0 requires a clean format_version={} file",
                    target, FORMAT_VERSION);
                snapshot = parse(defaultRoot(), true);
                return;
            }
            if (!root.has("stats") || !root.get("stats").isJsonObject())
                throw new IllegalArgumentException("stats must be an object");
            snapshot = parse(root, true);
        } catch (Exception exception) {
            LOGGER.warn("Could not load {}. Backing it up and restoring defaults: {}",
                target, exception.getMessage());
            backupBroken(target);
            JsonObject defaults = defaultRoot();
            writeConfig(target, defaults);
            snapshot = parse(defaults, true);
        }
    }

    private static Snapshot defaultSnapshot() {
        Map<Stat, List<AttributeService.ConfiguredModifier>> rewards = new EnumMap<>(Stat.class);
        for (Stat stat : Stat.values()) rewards.put(stat, List.of());
        return new Snapshot(Map.copyOf(rewards), 0.02, true, true);
    }

    private static void backupBroken(Path target) {
        if (!Files.exists(target)) return;
        Path backup = target.resolveSibling(target.getFileName() + ".broken."
            + System.currentTimeMillis() + ".bak");
        try {
            Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.warn("Could not back up broken config {}: {}", target, exception.getMessage());
        }
    }

    private static boolean writeConfig(Path target, JsonObject root) {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(temporary, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
            moveIntoPlace(temporary, target, true);
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Could not write {}: {}", target, exception.getMessage());
            return false;
        } finally {
            try { Files.deleteIfExists(temporary); }
            catch (IOException ignored) {}
        }
    }

    private static void moveIntoPlace(Path source, Path target, boolean replace) throws IOException {
        StandardCopyOption[] atomicOptions = replace
            ? new StandardCopyOption[] {StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING}
            : new StandardCopyOption[] {StandardCopyOption.ATOMIC_MOVE};
        StandardCopyOption[] fallbackOptions = replace
            ? new StandardCopyOption[] {StandardCopyOption.REPLACE_EXISTING}
            : new StandardCopyOption[0];
        try {
            Files.move(source, target, atomicOptions);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, fallbackOptions);
        }
    }

    public static List<AttributeService.ConfiguredModifier> definitions(Stat stat) {
        return snapshot.rewards().getOrDefault(stat, List.of());
    }

    public static double experienceBonusPerPoint() { return snapshot.experienceBonusPerPoint(); }
    public static boolean affectsVanillaExperience() { return snapshot.affectsVanillaExperience(); }
    public static boolean affectsSoulProgression() { return snapshot.affectsSoulProgression(); }

    public static double experienceMultiplier(int intelligence) {
        return 1.0 + Math.max(0, intelligence) * experienceBonusPerPoint();
    }

    public static Optional<String> validateText(String text) {
        try {
            JsonElement element = JsonParser.parseString(text);
            if (!element.isJsonObject()) return Optional.of("Root value must be a JSON object");
            JsonObject root = element.getAsJsonObject();
            if (!currentFormat(root)) return Optional.of(
                "Unsupported attribute reward format; expected format_version=" + FORMAT_VERSION);
            if (!root.has("stats")) return Optional.of("Missing object: stats");
            parse(root, false);
            return Optional.empty();
        } catch (RuntimeException exception) {
            return Optional.of(exception.getMessage() == null ? "Invalid JSON" : exception.getMessage());
        }
    }

    private static Snapshot parse(JsonObject root, boolean logWarnings) {
        Map<Stat, List<AttributeService.ConfiguredModifier>> rewards = new EnumMap<>(Stat.class);
        JsonObject stats = object(root, "stats");
        double bonus = 0.02;
        boolean vanilla = true;
        boolean soul = true;
        for (Stat stat : Stat.values()) {
            JsonObject statObject = object(stats, stat.name().toLowerCase(Locale.ROOT));
            if (!bool(statObject, "enabled", true)) {
                rewards.put(stat, List.of());
                if (stat == Stat.INTELLIGENCE) {
                    bonus = 0.0;
                    vanilla = false;
                    soul = false;
                }
                continue;
            }
            if (stat == Stat.INTELLIGENCE) {
                bonus = finiteNonNegative(statObject, "experience_bonus_per_point", 0.02);
                vanilla = bool(statObject, "affects_vanilla_experience", true);
                soul = bool(statObject, "affects_soul_progression", true);
            }
            JsonObject entries = object(statObject, "rewards");
            List<AttributeService.ConfiguredModifier> parsed = new ArrayList<>();
            int index = 0;
            for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
                if (index >= 64) break;
                try {
                    if (!entry.getValue().isJsonObject()) throw new IllegalArgumentException("reward must be an object");
                    JsonObject value = entry.getValue().getAsJsonObject();
                    if (!bool(value, "enabled", true)) continue;
                    ResourceLocation attributeId = ResourceLocation.parse(entry.getKey());
                    String requiredMod = nullableString(value, "required_mod");
                    if (requiredMod != null && !requiredMod.matches("[a-z][a-z0-9_]{1,63}"))
                        throw new IllegalArgumentException("invalid required_mod");
                    if (requiredMod != null && !IntegrationService.isLoaded(requiredMod)) continue;
                    if (!nativeRewardEnabled(root, value)) continue;
                    if (BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).isEmpty())
                        throw new IllegalArgumentException("unknown attribute id");
                    double amount = finite(value, "amount_per_point", 0.0);
                    AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(
                        string(value, "operation", "ADD_VALUE").toUpperCase(Locale.ROOT));
                    Double minimum = nullableFinite(value, "min_final");
                    Double cap = nullableFinite(value, "cap");
                    if (minimum != null && cap != null && minimum > cap)
                        throw new IllegalArgumentException("min_final exceeds cap");
                    parsed.add(new AttributeService.ConfiguredModifier(attributeId, amount, operation, minimum, cap,
                        requiredMod == null ? "" : requiredMod, bool(value, "display", true),
                        string(value, "display_category", "other").toLowerCase(Locale.ROOT),
                        formatter(value), true, index++));
                } catch (RuntimeException exception) {
                    String location = stat.name().toLowerCase(Locale.ROOT) + "." + entry.getKey();
                    if (!logWarnings) throw new IllegalArgumentException(location + ": " + exception.getMessage(), exception);
                    LOGGER.warn("Skipping stat reward {}: {}", location, exception.getMessage());
                }
            }
            rewards.put(stat, List.copyOf(parsed));
        }
        return new Snapshot(Map.copyOf(rewards), bonus, vanilla, soul);
    }

    private static JsonObject defaultRoot() {
        JsonObject root = new JsonObject();
        root.addProperty("format_version", FORMAT_VERSION);
        JsonObject stats = new JsonObject();
        root.add("stats", stats);
        for (Stat stat : Stat.values()) {
            JsonObject value = new JsonObject();
            value.addProperty("enabled", true);
            value.add("rewards", new JsonObject());
            stats.add(stat.name().toLowerCase(Locale.ROOT), value);
        }
        JsonObject intelligence = stats.getAsJsonObject("intelligence");
        intelligence.addProperty("experience_bonus_per_point", 0.02);
        intelligence.addProperty("affects_vanilla_experience", true);
        intelligence.addProperty("affects_soul_progression", true);

        reward(stats, "strength", "minecraft:generic.attack_damage", 0.5, "ADD_VALUE", null, null, "damage", "number");
        reward(stats, "endurance", "minecraft:generic.max_health", 0.5, "ADD_VALUE", null, null, "defense", "number");
        reward(stats, "endurance", "minecraft:generic.armor", 0.5, "ADD_VALUE", null, null, "defense", "number");
        reward(stats, "endurance", "minecraft:generic.armor_toughness", 0.5, "ADD_VALUE", null, null, "defense", "number");
        reward(stats, "endurance", "minecraft:generic.knockback_resistance", 0.05, "ADD_VALUE", 1.0, null, "defense", "percent");
        reward(stats, "agility", "minecraft:generic.movement_speed", 0.02, "ADD_MULTIPLIED_BASE", null, null, "mobility", "auto");
        reward(stats, "agility", "minecraft:generic.attack_speed", 0.5, "ADD_VALUE", null, null, "damage", "number");
        reward(stats, "agility", "minecraft:generic.step_height", 0.04, "ADD_VALUE", 1.01, null, "mobility", "number");
        reward(stats, "intelligence", "irons_spellbooks:max_mana", 2.0, "ADD_VALUE", 300.0, "irons_spellbooks", "magic", "number");
        reward(stats, "intelligence", "irons_spellbooks:mana_regen", 0.01, "ADD_MULTIPLIED_BASE", 2.0, "irons_spellbooks", "magic", "multiplier");
        reward(stats, "intelligence", "irons_spellbooks:spell_power", 0.01, "ADD_MULTIPLIED_BASE", 2.0, "irons_spellbooks", "magic", "multiplier");
        reward(stats, "intelligence", "irons_spellbooks:cast_time_reduction", 0.005, "ADD_MULTIPLIED_BASE", 1.5, "irons_spellbooks", "magic", "multiplier");
        reward(stats, "intelligence", "irons_spellbooks:cooldown_reduction", 0.005, "ADD_MULTIPLIED_BASE", 1.5, "irons_spellbooks", "magic", "multiplier");
        reward(stats, "perception", "minecraft:generic.luck", 1.0, "ADD_VALUE", null, null, "utility", "number");
        reward(stats, "perception", "apothic_attributes:crit_chance", 0.002, "ADD_VALUE", 0.20, "apothic_attributes", "damage", "percent");
        reward(stats, "perception", "apothic_attributes:arrow_damage", 0.005, "ADD_MULTIPLIED_BASE", 1.50, "apothic_attributes", "utility", "multiplier");
        reward(stats, "perception", "apothic_attributes:draw_speed", 0.005, "ADD_MULTIPLIED_BASE", 1.50, "apothic_attributes", "utility", "multiplier");
        EpicFightNativeRewards.addDefaults(root);
        return root;
    }

    static boolean currentFormat(JsonObject root) {
        try {
            return root.has("format_version")
                && root.get("format_version").getAsInt() == FORMAT_VERSION;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static void reward(JsonObject stats, String stat, String id, double amount, String operation,
                               Double cap, String requiredMod, String category, String formatter) {
        JsonObject value = new JsonObject();
        value.addProperty("enabled", true);
        value.addProperty("amount_per_point", amount);
        value.addProperty("operation", operation);
        value.add("min_final", com.google.gson.JsonNull.INSTANCE);
        if (cap == null) value.add("cap", com.google.gson.JsonNull.INSTANCE); else value.addProperty("cap", cap);
        if (requiredMod == null) value.add("required_mod", com.google.gson.JsonNull.INSTANCE);
        else value.addProperty("required_mod", requiredMod);
        value.addProperty("display", true);
        value.addProperty("display_category", category);
        value.addProperty("formatter", formatter);
        stats.getAsJsonObject(stat).getAsJsonObject("rewards").add(id, value);
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        try { return object.has(key) ? object.get(key).getAsBoolean() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String string(JsonObject object, String key, String fallback) {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static String nullableString(JsonObject object, String key) {
        String value = string(object, key, "").trim();
        return value.isEmpty() ? null : value;
    }

    static boolean nativeRewardEnabled(JsonObject root, JsonObject reward) {
        String nativeIntegration = nullableString(reward, "native_integration");
        if (nativeIntegration == null) return true;
        if (!nativeIntegration.matches("[a-z][a-z0-9_]{1,63}"))
            throw new IllegalArgumentException("invalid native_integration");
        return EpicFightNativeRewards.nativeEnabled(root, nativeIntegration);
    }

    private static double finite(JsonObject object, String key, double fallback) {
        double value = object.has(key) ? object.get(key).getAsDouble() : fallback;
        if (!Double.isFinite(value)) throw new IllegalArgumentException(key + " must be finite");
        return value;
    }

    private static double finiteNonNegative(JsonObject object, String key, double fallback) {
        double value = finite(object, key, fallback);
        return Math.max(0.0, value);
    }

    private static Double nullableFinite(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return null;
        return finite(object, key, 0.0);
    }

    private static String formatter(JsonObject object) {
        String value = string(object, "formatter", "auto").toLowerCase(Locale.ROOT);
        if (value.equals("auto") || value.equals("number") || value.equals("percent") || value.equals("multiplier"))
            return value;
        throw new IllegalArgumentException("invalid formatter");
    }
}
