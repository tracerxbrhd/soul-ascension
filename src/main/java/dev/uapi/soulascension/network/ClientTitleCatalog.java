package dev.uapi.soulascension.network;

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ClientTitleCatalog {
    private static volatile List<ClientTitleDefinition> titles = List.of();
    private ClientTitleCatalog() {}

    public static void replace(List<ClientTitleDefinition> values) {
        titles = values.stream().sorted(Comparator.comparingInt(ClientTitleDefinition::order)
            .thenComparing(value -> value.id().toString())).toList();
    }

    public static List<ClientTitleDefinition> all() { return titles; }
    public static Optional<ClientTitleDefinition> get(ResourceLocation id) {
        return titles.stream().filter(value -> value.id().equals(id)).findFirst();
    }
}
