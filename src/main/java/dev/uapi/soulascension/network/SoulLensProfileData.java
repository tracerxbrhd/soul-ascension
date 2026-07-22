package dev.uapi.soulascension.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lightweight, frequently refreshed Soul Lens projection.
 *
 * <p>Unlike {@link PublicProfileData}, this DTO deliberately contains no skin property. A signed
 * skin value is commonly several kilobytes and the overlay never renders it, so including it in
 * every lens refresh only wastes bandwidth and repeatedly touches the game-profile property map.</p>
 */
public record SoulLensProfileData(UUID playerId, String playerName, int level,
                                  Identifier activeTitle,
                                  int strength, int endurance, int agility,
                                  int intelligence, int perception,
                                  List<PublicProfileData.PublicAttribute> attributes) {
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_ATTRIBUTES = 32;

    public SoulLensProfileData {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
        playerName = playerName.substring(0, Math.min(playerName.length(), MAX_NAME_LENGTH));
        Objects.requireNonNull(activeTitle, "activeTitle");
        attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
        if (attributes.size() > MAX_ATTRIBUTES)
            throw new IllegalArgumentException("Too many Soul Lens attributes");
    }

    public static SoulLensProfileData decode(RegistryFriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        String playerName = buffer.readUtf(MAX_NAME_LENGTH);
        int level = buffer.readVarInt();
        Identifier activeTitle = buffer.readIdentifier();
        int strength = buffer.readVarInt();
        int endurance = buffer.readVarInt();
        int agility = buffer.readVarInt();
        int intelligence = buffer.readVarInt();
        int perception = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ATTRIBUTES)
            throw new IllegalArgumentException("Invalid Soul Lens attribute count: " + size);
        List<PublicProfileData.PublicAttribute> attributes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            attributes.add(new PublicProfileData.PublicAttribute(
                buffer.readIdentifier(), buffer.readDouble()));
        }
        return new SoulLensProfileData(playerId, playerName, level, activeTitle, strength, endurance,
            agility, intelligence, perception, attributes);
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeUtf(playerName, MAX_NAME_LENGTH);
        buffer.writeVarInt(level);
        buffer.writeIdentifier(activeTitle);
        buffer.writeVarInt(strength);
        buffer.writeVarInt(endurance);
        buffer.writeVarInt(agility);
        buffer.writeVarInt(intelligence);
        buffer.writeVarInt(perception);
        buffer.writeVarInt(attributes.size());
        for (PublicProfileData.PublicAttribute attribute : attributes) {
            buffer.writeIdentifier(attribute.id());
            buffer.writeDouble(attribute.value());
        }
    }
}
