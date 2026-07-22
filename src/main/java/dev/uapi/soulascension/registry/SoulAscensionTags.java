package dev.uapi.soulascension.registry;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class SoulAscensionTags {
    public static final TagKey<EntityType<?>> NO_EXPERIENCE = TagKey.create(Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(SoulAscensionMod.MOD_ID, "no_experience"));
    private SoulAscensionTags() {}
}
