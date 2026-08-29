package dev.uapi.soulascension.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** Client API: optional compatibility mods may add character-screen tabs backed by real data. */
public final class CharacterIntegrationRegistry {
    public record Tab(ResourceLocation id, ResourceLocation icon, Component title, int order,
                      BooleanSupplier visible, Function<LocalPlayer, List<Component>> lines) {}
    private static final Map<ResourceLocation, Tab> TABS = new LinkedHashMap<>();
    private CharacterIntegrationRegistry() {}

    public static synchronized void register(Tab tab) { TABS.put(tab.id(), tab); }

    public static synchronized List<Tab> visibleTabs() {
        return TABS.values().stream().filter(tab -> tab.visible().getAsBoolean())
            .sorted(Comparator.comparingInt(Tab::order).thenComparing(tab -> tab.id().toString())).toList();
    }
}
