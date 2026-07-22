package dev.uapi.soulascension.network;

import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClientTitleCatalog {
    private record Snapshot(List<ClientTitleDefinition> titles,
                            Map<Identifier, ClientTitleDefinition> byId,
                            Map<Identifier, Component> names,
                            Map<Identifier, Component> descriptions,
                            long revision) {}
    private static volatile Snapshot snapshot = new Snapshot(List.of(), Map.of(), Map.of(), Map.of(), 0);
    private ClientTitleCatalog() {}

    public static void replace(List<ClientTitleDefinition> values) {
        List<ClientTitleDefinition> titles = values.stream().sorted(Comparator.comparingInt(ClientTitleDefinition::order)
            .thenComparing(value -> value.id().toString())).toList();
        Map<Identifier, ClientTitleDefinition> byId = new LinkedHashMap<>();
        Map<Identifier, Component> names = new LinkedHashMap<>();
        Map<Identifier, Component> descriptions = new LinkedHashMap<>();
        for (ClientTitleDefinition title : titles) {
            byId.put(title.id(), title);
            names.put(title.id(), Component.translatable(title.nameKey()));
            descriptions.put(title.id(), Component.translatable(title.descriptionKey()));
        }
        Snapshot previous = snapshot;
        snapshot = new Snapshot(titles, Map.copyOf(byId), Map.copyOf(names),
            Map.copyOf(descriptions), previous.revision() + 1);
    }

    public static List<ClientTitleDefinition> all() { return snapshot.titles(); }
    public static Optional<ClientTitleDefinition> get(Identifier id) {
        return Optional.ofNullable(snapshot.byId().get(id));
    }
    public static Component name(Identifier id) {
        return snapshot.names().getOrDefault(id, Component.empty());
    }
    public static Component description(Identifier id) {
        return snapshot.descriptions().getOrDefault(id, Component.empty());
    }
    public static long revision() { return snapshot.revision(); }
}
