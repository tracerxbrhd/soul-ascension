package dev.uapi.soulascension.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

public record ClientTitleDefinition(Identifier id, String nameKey, String descriptionKey,
                                    Identifier icon, int order, boolean hidden) {
    public static ClientTitleDefinition decode(RegistryFriendlyByteBuf buffer) {
        return new ClientTitleDefinition(buffer.readIdentifier(), buffer.readUtf(256), buffer.readUtf(256),
            buffer.readIdentifier(), buffer.readVarInt(), buffer.readBoolean());
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeIdentifier(id); buffer.writeUtf(nameKey, 256); buffer.writeUtf(descriptionKey, 256);
        buffer.writeIdentifier(icon); buffer.writeVarInt(order); buffer.writeBoolean(hidden);
    }
}
