package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.integration.IntegrationService;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** Client API: optional compatibility mods may replace or add character-screen tabs. */
public final class CharacterIntegrationRegistry {
    public record Tab(ResourceLocation id, ResourceLocation icon, Component title, int order,
                      BooleanSupplier visible, Function<LocalPlayer, List<Component>> lines) {}
    private static final Map<ResourceLocation, Tab> TABS = new LinkedHashMap<>();
    private static boolean defaultsRegistered;
    private CharacterIntegrationRegistry() {}

    public static synchronized void register(Tab tab) { TABS.put(tab.id(), tab); }

    public static synchronized List<Tab> visibleTabs() {
        bootstrapDefaults();
        return TABS.values().stream().filter(tab -> tab.visible().getAsBoolean())
            .sorted(Comparator.comparingInt(Tab::order).thenComparing(tab -> tab.id().toString())).toList();
    }

    private static void bootstrapDefaults() {
        if (defaultsRegistered) return;
        defaultsRegistered = true;
        ResourceLocation originsIcon = SoulAscensionMod.id("textures/gui/icons/origins.png");
        registerDefault(new Tab(SoulAscensionMod.id("origins"), originsIcon, Component.translatable("integration.soul_ascension.origins"), 100,
            () -> loadedAny("origins", "origins_neoforge", "neoorigins"), player -> {
                List<Component> lines = new ArrayList<>();
                lines.add(Component.translatable("integration.soul_ascension.detected"));
                player.getTags().stream().filter(tag -> tag.contains("origin")).sorted()
                    .forEach(tag -> lines.add(Component.literal(tag)));
                if (lines.size() == 1) lines.add(Component.translatable("integration.soul_ascension.provider_hint"));
                return lines;
            }));
    }

    private static void registerDefault(Tab tab) { TABS.putIfAbsent(tab.id(), tab); }

    private static boolean loadedAny(String... ids) {
        for (String id : ids) if (IntegrationService.isLoaded(id)) return true;
        return false;
    }
}
