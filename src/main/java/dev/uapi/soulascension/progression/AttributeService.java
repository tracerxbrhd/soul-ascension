package dev.uapi.soulascension.progression;

import com.mojang.logging.LogUtils;
import dev.uapi.integration.IntegrationService;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class AttributeService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_CONFIGURED_MODIFIERS_PER_STAT = 64;
    private static final Set<String> WARNED = new HashSet<>();

    private AttributeService() {}

    public record ConfiguredModifier(ResourceLocation attributeId, double amountPerPoint,
                                     AttributeModifier.Operation operation, Double minimumFinal,
                                     Double maximumFinal, String requiredMod, boolean displayInUi,
                                     String category, String formatter, boolean enabled, int index) {}

    public static void apply(ServerPlayer player, PlayerProgress progress) {
        removeManagedModifiers(player);
        for (Stat stat : Stat.values()) applyStat(player, stat, progress.stat(stat));
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void applyStat(ServerPlayer player, Stat stat, int points) {
        if (points <= 0) return;
        for (ConfiguredModifier definition : definitions(stat)) {
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(definition.attributeId()).orElse(null);
            if (attribute == null) continue;
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            ResourceLocation modifierId = modifierId(stat, definition.index());
            double amount = effectiveAmount(instance, modifierId, definition, points);
            if (Math.abs(amount) < 1.0E-12) continue;
            instance.addTransientModifier(new AttributeModifier(modifierId, amount, definition.operation()));
        }
    }

    public static List<ConfiguredModifier> definitions(Stat stat) {
        String configured = SoulAscensionServerConfig.modifiers(stat);
        if (configured == null || configured.isBlank()) return List.of();
        List<ConfiguredModifier> parsed = new ArrayList<>();
        String[] entries = configured.split(";");
        for (int index = 0; index < entries.length && index < MAX_CONFIGURED_MODIFIERS_PER_STAT; index++) {
            String entry = entries[index].trim();
            if (entry.isEmpty()) continue;
            Optional<ConfiguredModifier> definition = parse(entry, index);
            definition.ifPresent(parsed::add);
        }
        return List.copyOf(parsed);
    }

    public static Optional<ConfiguredModifier> displayDefinition(ResourceLocation attributeId) {
        for (Stat stat : Stat.values()) for (ConfiguredModifier definition : definitions(stat))
            if (definition.attributeId().equals(attributeId)) return Optional.of(definition);
        return Optional.empty();
    }

    private static Optional<ConfiguredModifier> parse(String entry, int index) {
        String[] parts = entry.split("\\|", -1);
        if (parts.length != 3 && parts.length != 9 && parts.length != 10) {
            warnOnce("format:" + entry,
                "Ignoring invalid SOUL ASCENSION reward '{}': expected 3, 9, or 10 fields", entry);
            return Optional.empty();
        }
        try {
            ResourceLocation attributeId = ResourceLocation.parse(parts[0].trim());
            double perPoint = Double.parseDouble(parts[1].trim());
            if (!Double.isFinite(perPoint)) throw new IllegalArgumentException("amount must be finite");
            AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(
                parts[2].trim().toUpperCase(Locale.ROOT));
            Double minimum = parts.length >= 9 ? optionalNumber(parts[3]) : null;
            Double maximum = parts.length >= 9 ? optionalNumber(parts[4]) : null;
            if (minimum != null && maximum != null && minimum > maximum)
                throw new IllegalArgumentException("min_final must not exceed max_final");
            String requiredMod = parts.length >= 9 ? optionalText(parts[5]) : "";
            if (!requiredMod.isEmpty() && !requiredMod.matches("[a-z][a-z0-9_]{1,63}"))
                throw new IllegalArgumentException("required_mod is not a valid mod id");
            boolean display = parts.length < 9 || booleanValue(parts[6], "display");
            String category = parts.length >= 9 ? normalizeCategory(parts[7]) : "other";
            String formatter = parts.length == 10 ? normalizeFormatter(parts[8]) : "auto";
            boolean enabled = parts.length < 9 || booleanValue(parts[parts.length - 1], "enabled");
            if (!enabled) return Optional.empty();
            if (!requiredMod.isEmpty() && !IntegrationService.isLoaded(requiredMod)) return Optional.empty();
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(attributeId).orElse(null);
            if (attribute == null) {
                warnOnce("attribute:" + attributeId,
                    "Skipping unknown SOUL ASCENSION attribute reward '{}'", attributeId);
                return Optional.empty();
            }
            return Optional.of(new ConfiguredModifier(attributeId, perPoint, operation, minimum, maximum,
                requiredMod, display, category, formatter, true, index));
        } catch (RuntimeException exception) {
            warnOnce("invalid:" + entry, "Ignoring invalid SOUL ASCENSION attribute reward '{}': {}",
                entry, rootCause(exception));
            return Optional.empty();
        }
    }

    /** Calculates the modifier amount after applying optional final-value bounds. */
    public static double effectiveAmount(AttributeInstance instance, ResourceLocation modifierId,
                                         ConfiguredModifier definition, int points) {
        double requested = definition.amountPerPoint() * Math.max(0, points);
        if (requested == 0 || (definition.minimumFinal() == null && definition.maximumFinal() == null)) return requested;
        double candidate = valueWithReplacement(instance, modifierId, requested, definition.operation());
        if (inside(candidate, definition.minimumFinal(), definition.maximumFinal())) return requested;

        double baseline = valueWithReplacement(instance, modifierId, 0, definition.operation());
        if (!inside(baseline, definition.minimumFinal(), definition.maximumFinal())) {
            boolean improvesMinimum = definition.minimumFinal() != null && baseline < definition.minimumFinal()
                && candidate > baseline;
            boolean improvesMaximum = definition.maximumFinal() != null && baseline > definition.maximumFinal()
                && candidate < baseline;
            if (!improvesMinimum && !improvesMaximum) return 0;
            return requested;
        }

        double low = 0;
        double high = 1;
        for (int i = 0; i < 48; i++) {
            double middle = (low + high) * 0.5;
            double value = valueWithReplacement(instance, modifierId, requested * middle, definition.operation());
            if (inside(value, definition.minimumFinal(), definition.maximumFinal())) low = middle;
            else high = middle;
        }
        return requested * low;
    }

    public static double valueWithReplacement(AttributeInstance instance, ResourceLocation modifierId,
                                              double amount, AttributeModifier.Operation operation) {
        List<AttributeModifier> modifiers = new ArrayList<>();
        for (AttributeModifier modifier : instance.getModifiers())
            if (!modifier.id().equals(modifierId)) modifiers.add(modifier);
        if (Math.abs(amount) >= 1.0E-12) modifiers.add(new AttributeModifier(modifierId, amount, operation));

        double value = instance.getBaseValue();
        for (AttributeModifier modifier : modifiers)
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) value += modifier.amount();
        double afterBase = value;
        for (AttributeModifier modifier : modifiers)
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                afterBase += value * modifier.amount();
        for (AttributeModifier modifier : modifiers)
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                afterBase *= 1.0 + modifier.amount();
        return instance.getAttribute().value().sanitizeValue(afterBase);
    }

    private static boolean inside(double value, Double minimum, Double maximum) {
        return (minimum == null || value >= minimum - 1.0E-9)
            && (maximum == null || value <= maximum + 1.0E-9);
    }

    private static void removeManagedModifiers(ServerPlayer player) {
        BuiltInRegistries.ATTRIBUTE.holders().forEach(attribute -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;
            for (Stat stat : Stat.values()) for (int index = 0; index < MAX_CONFIGURED_MODIFIERS_PER_STAT; index++)
                instance.removeModifier(modifierId(stat, index));
        });
    }

    public static ResourceLocation modifierId(Stat stat, int index) {
        return ResourceLocation.fromNamespaceAndPath(SoulAscensionMod.MOD_ID,
            "stat_" + stat.name().toLowerCase(Locale.ROOT) + "_" + index);
    }

    public static Optional<Stat> sourceStat(ResourceLocation modifierId) {
        for (Stat stat : Stat.values()) {
            for (ConfiguredModifier definition : definitions(stat)) {
                if (modifierId(stat, definition.index()).equals(modifierId)) return Optional.of(stat);
            }
        }
        return Optional.empty();
    }

    private static Double optionalNumber(String value) {
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.equals("-")) return null;
        double parsed = Double.parseDouble(normalized);
        if (!Double.isFinite(parsed)) throw new IllegalArgumentException("bounds must be finite");
        return parsed;
    }

    private static String optionalText(String value) {
        String normalized = value.trim();
        return normalized.equals("-") ? "" : normalized;
    }

    private static String normalizeCategory(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || normalized.equals("-") ? "other" : normalized;
    }

    private static String normalizeFormatter(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty() || normalized.equals("-")) return "auto";
        if (normalized.equals("auto") || normalized.equals("number") || normalized.equals("percent")
            || normalized.equals("multiplier")) return normalized;
        throw new IllegalArgumentException("formatter must be auto, number, percent, or multiplier");
    }

    private static boolean booleanValue(String value, String field) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("true")) return true;
        if (normalized.equals("false")) return false;
        throw new IllegalArgumentException(field + " must be true or false");
    }

    private static String rootCause(Throwable throwable) {
        while (throwable.getCause() != null) throwable = throwable.getCause();
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private static synchronized void warnOnce(String key, String message, Object... arguments) {
        if (WARNED.add(key)) LOGGER.warn(message, arguments);
    }
}
