package dev.uapi.soulascension.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record ClientTitleDefinition(ResourceLocation id, String nameKey, String descriptionKey,
                                    ResourceLocation icon, int order, boolean hidden) {
    public static ClientTitleDefinition decode(RegistryFriendlyByteBuf buffer) {
        return new ClientTitleDefinition(buffer.readResourceLocation(), buffer.readUtf(256), buffer.readUtf(256),
            buffer.readResourceLocation(), buffer.readVarInt(), buffer.readBoolean());
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeResourceLocation(id); buffer.writeUtf(nameKey, 256); buffer.writeUtf(descriptionKey, 256);
        buffer.writeResourceLocation(icon); buffer.writeVarInt(order); buffer.writeBoolean(hidden);
    }
}
