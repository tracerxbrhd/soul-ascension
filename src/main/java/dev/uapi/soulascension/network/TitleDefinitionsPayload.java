package dev.uapi.soulascension.network;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public record TitleDefinitionsPayload(List<ClientTitleDefinition> titles) implements CustomPacketPayload {
    public static final Type<TitleDefinitionsPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(SoulAscensionMod.MOD_ID, "title_definitions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TitleDefinitionsPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public TitleDefinitionsPayload decode(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size < 0 || size > 4096) throw new IllegalArgumentException("Invalid title definition count: " + size);
            List<ClientTitleDefinition> values = new ArrayList<>(size);
            for (int i = 0; i < size; i++) values.add(ClientTitleDefinition.decode(buffer));
            return new TitleDefinitionsPayload(values);
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, TitleDefinitionsPayload value) {
            buffer.writeVarInt(value.titles().size()); value.titles().forEach(title -> title.encode(buffer));
        }
    };

    public TitleDefinitionsPayload { titles = List.copyOf(titles); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
