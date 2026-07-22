package dev.uapi.soulascension.network;

import dev.uapi.api.profile.ProfileFacet;
import dev.uapi.api.profile.ProfileFacetRegistry;
import dev.uapi.api.profile.ProfileFacetWireCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Deliberately limited DTO: no free points, progress, modifier sources, or configuration data. */
public record PublicProfileData(UUID playerId, String playerName, String skinValue, String skinSignature,
                                int level, Identifier activeTitle,
                                int strength, int endurance, int agility, int intelligence, int perception,
                                List<PublicAttribute> attributes, List<ProfileFacet> facets) {
    private static final int MAX_NAME_LENGTH = 64;
    private static final int MAX_SKIN_VALUE_LENGTH = 16384;
    private static final int MAX_SKIN_SIGNATURE_LENGTH = 2048;
    private static final int MAX_ATTRIBUTES = 32;

    public record PublicAttribute(Identifier id, double value) {
        public PublicAttribute {
            Objects.requireNonNull(id, "id");
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite public attribute value");
        }
    }

    public PublicProfileData {
        Objects.requireNonNull(playerId, "playerId");
        playerName = Objects.requireNonNull(playerName, "playerName");
        playerName = playerName.length() > MAX_NAME_LENGTH ? playerName.substring(0, MAX_NAME_LENGTH) : playerName;
        skinValue = skinValue == null ? "" : skinValue.substring(0, Math.min(skinValue.length(), MAX_SKIN_VALUE_LENGTH));
        skinSignature = skinSignature == null ? "" : skinSignature.substring(0, Math.min(skinSignature.length(), MAX_SKIN_SIGNATURE_LENGTH));
        Objects.requireNonNull(activeTitle, "activeTitle");
        attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes"));
        if (attributes.size() > MAX_ATTRIBUTES) throw new IllegalArgumentException("Too many public attributes");
        Set<Identifier> attributeIds = new HashSet<>();
        for (PublicAttribute attribute : attributes) {
            Objects.requireNonNull(attribute, "attributes must not contain null");
            if (!attributeIds.add(attribute.id()))
                throw new IllegalArgumentException("Duplicate public attribute: " + attribute.id());
        }
        facets = List.copyOf(Objects.requireNonNull(facets, "facets"));
        if (facets.size() > ProfileFacetRegistry.MAXIMUM_FACETS)
            throw new IllegalArgumentException("Too many public profile facets");
    }

    public PublicProfileData(UUID playerId, String playerName, String skinValue, String skinSignature,
                             int level, Identifier activeTitle, int strength, int endurance,
                             int agility, int intelligence, int perception, List<PublicAttribute> attributes) {
        this(playerId, playerName, skinValue, skinSignature, level, activeTitle, strength, endurance,
            agility, intelligence, perception, attributes, List.of());
    }

    public static PublicProfileData decode(RegistryFriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        String playerName = buffer.readUtf(MAX_NAME_LENGTH);
        String skinValue = buffer.readUtf(MAX_SKIN_VALUE_LENGTH);
        String skinSignature = buffer.readUtf(MAX_SKIN_SIGNATURE_LENGTH);
        int level = buffer.readVarInt();
        Identifier activeTitle = buffer.readIdentifier();
        int strength = buffer.readVarInt();
        int endurance = buffer.readVarInt();
        int agility = buffer.readVarInt();
        int intelligence = buffer.readVarInt();
        int perception = buffer.readVarInt();
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_ATTRIBUTES) throw new IllegalArgumentException("Invalid public attribute count: " + size);
        List<PublicAttribute> attributes = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            attributes.add(new PublicAttribute(buffer.readIdentifier(), buffer.readDouble()));
        }
        List<ProfileFacet> facets = ProfileFacetWireCodec.decodeList(buffer);
        return new PublicProfileData(playerId, playerName, skinValue, skinSignature, level, activeTitle, strength,
            endurance, agility, intelligence, perception, attributes, facets);
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(playerId);
        buffer.writeUtf(playerName, MAX_NAME_LENGTH);
        buffer.writeUtf(skinValue, MAX_SKIN_VALUE_LENGTH);
        buffer.writeUtf(skinSignature, MAX_SKIN_SIGNATURE_LENGTH);
        buffer.writeVarInt(level);
        buffer.writeIdentifier(activeTitle);
        buffer.writeVarInt(strength);
        buffer.writeVarInt(endurance);
        buffer.writeVarInt(agility);
        buffer.writeVarInt(intelligence);
        buffer.writeVarInt(perception);
        buffer.writeVarInt(attributes.size());
        for (PublicAttribute attribute : attributes) {
            buffer.writeIdentifier(attribute.id());
            buffer.writeDouble(attribute.value());
        }
        ProfileFacetWireCodec.encodeList(buffer, facets);
    }
}
