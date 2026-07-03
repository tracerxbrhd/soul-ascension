package dev.uapi.soulascension.title;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class TitleRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String ROOT = "level_titles";
    private static volatile Map<ResourceLocation, TitleDefinition> definitions = Map.of();
    private static volatile Set<ResourceLocation> trackedEntities = Set.of();
    private static volatile Set<ResourceLocation> trackedItems = Set.of();
    private static volatile Set<ResourceLocation> trackedBlocks = Set.of();
    private static volatile boolean hasTimeBasedTitles;
    private TitleRegistry() {}

    public static void reload(ResourceManager manager) {
        Map<ResourceLocation, TitleDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(ROOT,
            id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation file = entry.getKey();
            String path = file.getPath().substring(ROOT.length() + 1, file.getPath().length() - 5);
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), path);
            try (Reader reader = entry.getValue().openAsReader()) {
                loaded.put(id, TitleDefinition.parse(id, JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception exception) {
                LOGGER.error("Skipping invalid level title {}: {}", id, rootCause(exception));
            }
        }
        loaded.entrySet().removeIf(entry -> !valid(entry.getValue(), loaded));
        definitions = Map.copyOf(loaded);
        trackedEntities = tracked(loaded, value -> value.conditions().entityKills().keySet());
        trackedItems = tracked(loaded, value -> value.conditions().itemsCollected().keySet());
        trackedBlocks = tracked(loaded, value -> value.conditions().blocksMined().keySet());
        hasTimeBasedTitles = loaded.values().stream().anyMatch(value -> value.available() && value.conditions().playTimeTicks() > 0);
        LOGGER.info("Loaded {} level title definitions", loaded.size());
    }

    public static Optional<TitleDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static List<TitleDefinition> all() {
        List<TitleDefinition> result = new ArrayList<>(definitions.values());
        result.sort(Comparator.comparingInt(TitleDefinition::order).thenComparing(value -> value.id().toString()));
        return List.copyOf(result);
    }

    public static boolean tracksEntity(ResourceLocation id) { return trackedEntities.contains(id); }
    public static boolean tracksItem(ResourceLocation id) { return trackedItems.contains(id); }
    public static boolean tracksBlock(ResourceLocation id) { return trackedBlocks.contains(id); }
    public static boolean hasTimeBasedTitles() { return hasTimeBasedTitles; }
    public static Set<ResourceLocation> ids() { return definitions.keySet(); }

    private static String rootCause(Throwable throwable) {
        while (throwable.getCause() != null) throwable = throwable.getCause();
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private static boolean valid(TitleDefinition definition, Map<ResourceLocation, TitleDefinition> loaded) {
        if (!definition.available()) return true;
        boolean valid = true;
        for (ResourceLocation id : definition.conditions().entityKills().keySet()) if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
            LOGGER.error("Skipping title {}: unknown entity type {}", definition.id(), id); valid = false;
        }
        for (ResourceLocation id : definition.conditions().itemsCollected().keySet()) if (!BuiltInRegistries.ITEM.containsKey(id)) {
            LOGGER.error("Skipping title {}: unknown item {}", definition.id(), id); valid = false;
        }
        for (ResourceLocation id : definition.conditions().blocksMined().keySet()) if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            LOGGER.error("Skipping title {}: unknown block {}", definition.id(), id); valid = false;
        }
        for (ResourceLocation id : definition.conditions().requiredTitles()) if (!loaded.containsKey(id)) {
            LOGGER.error("Skipping title {}: unknown required title {}", definition.id(), id); valid = false;
        }
        return valid;
    }

    private static Set<ResourceLocation> tracked(Map<ResourceLocation, TitleDefinition> values,
                                                 java.util.function.Function<TitleDefinition, Set<ResourceLocation>> ids) {
        return values.values().stream().filter(TitleDefinition::available).flatMap(value -> ids.apply(value).stream())
            .collect(Collectors.toUnmodifiableSet());
    }
}
