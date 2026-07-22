package dev.uapi.soulascension.progression;

import dev.uapi.api.profile.ProfileFacetContext;
import dev.uapi.api.profile.ProfileFacetRegistry;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.network.PublicProfileData;
import dev.uapi.soulascension.network.PublicProfilePayload;
import dev.uapi.soulascension.network.SoulLensProfileData;
import dev.uapi.soulascension.title.TitleService;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import com.mojang.authlib.properties.Property;

public final class PublicProfileService {
    private static final List<Holder<Attribute>> PUBLIC_ATTRIBUTES = List.of(
        Attributes.ATTACK_DAMAGE,
        Attributes.ATTACK_SPEED,
        Attributes.MAX_HEALTH,
        Attributes.ARMOR,
        Attributes.ARMOR_TOUGHNESS,
        Attributes.KNOCKBACK_RESISTANCE,
        Attributes.MOVEMENT_SPEED,
        Attributes.LUCK
    );

    private PublicProfileService() {}

    public static boolean open(ServerPlayer viewer, ServerPlayer target) {
        if (!viewer.getUUID().equals(target.getUUID())
            && !target.getData(SoulAscensionAttachments.PROFILE_SETTINGS).publicProfile()) {
            viewer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.soul_ascension.public_profile.private", target.getGameProfile().name()));
            return false;
        }
        PacketDistributor.sendToPlayer(viewer, new PublicProfilePayload(snapshot(viewer, target)));
        return true;
    }

    public static PublicProfileData snapshot(ServerPlayer target) {
        return snapshot(target, target);
    }

    public static PublicProfileData snapshot(ServerPlayer viewer, ServerPlayer target) {
        PlayerProgress progress = SoulAscensionService.get(target);
        Property texture = target.getGameProfile().properties().get("textures").stream().findFirst().orElse(null);
        String skinValue = texture == null ? "" : texture.value();
        String skinSignature = texture == null || !texture.hasSignature() ? "" : texture.signature();
        return new PublicProfileData(target.getUUID(), target.getGameProfile().name(),
            skinValue, skinSignature,
            progress.level(), TitleService.get(target).activeTitle(), progress.strength(), progress.endurance(),
            progress.agility(), progress.intelligence(), progress.perception(), snapshotAttributes(target),
            ProfileFacetRegistry.query(target.level().getServer(), viewer.getUUID(), target.getUUID(),
                ProfileFacetContext.PUBLIC_PROFILE).stream()
                // Base Soul progression is already represented by the fields above. Keep this
                // extension list for other optional mods instead of rendering it twice.
                .filter(facet -> !facet.id().getNamespace().equals(SoulAscensionMod.MOD_ID))
                .toList());
    }

    /** Builds the high-frequency Lens projection without reading or transmitting signed skin data. */
    public static SoulLensProfileData soulLensSnapshot(ServerPlayer target) {
        PlayerProgress progress = SoulAscensionService.get(target);
        return new SoulLensProfileData(target.getUUID(), target.getGameProfile().name(), progress.level(),
            TitleService.get(target).activeTitle(), progress.strength(), progress.endurance(), progress.agility(),
            progress.intelligence(), progress.perception(), snapshotAttributes(target));
    }

    private static List<PublicProfileData.PublicAttribute> snapshotAttributes(ServerPlayer target) {
        List<PublicProfileData.PublicAttribute> attributes = new ArrayList<>();
        for (Holder<Attribute> holder : PUBLIC_ATTRIBUTES) {
            AttributeInstance instance = target.getAttribute(holder);
            Identifier id = holder.unwrapKey().orElseThrow().identifier();
            if (instance != null) attributes.add(new PublicProfileData.PublicAttribute(id, instance.getValue()));
        }
        return List.copyOf(attributes);
    }
}
