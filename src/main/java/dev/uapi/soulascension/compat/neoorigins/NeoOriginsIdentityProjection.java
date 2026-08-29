package dev.uapi.soulascension.compat.neoorigins;

import com.cyberday1.neoorigins.api.PowerLayers;
import com.cyberday1.neoorigins.api.origin.Origin;
import dev.uapi.api.profile.ProfileFacet;
import dev.uapi.api.profile.ProfileFacetAudience;
import dev.uapi.api.profile.ProfileFacetEntry;
import dev.uapi.api.profile.ProfileFacetField;
import dev.uapi.api.profile.ProfileFacetIcon;
import dev.uapi.api.profile.ProfileFacetIconType;
import dev.uapi.api.profile.ProfileFacetText;
import dev.uapi.soulascension.SoulAscensionMod;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Pure public-API projection for the future server-side NeoOrigins provider.
 *
 * <p>This class is isolated so it is never loaded when NeoOrigins is absent. It intentionally
 * accepts already-resolved {@link Origin} values: resolving the selections is impossible through
 * NeoOrigins 2.2.21's published API without reaching into implementation internals.</p>
 */
final class NeoOriginsIdentityProjection {
    static final ResourceLocation ORIGIN_LAYER =
        ResourceLocation.fromNamespaceAndPath("neoorigins", "origin");
    static final ResourceLocation CLASS_LAYER = PowerLayers.CLASS_LAYER;
    private static final ResourceLocation FACET_ID = SoulAscensionMod.id("neoorigins_identity");
    private static final ProfileFacetIcon FACET_ICON = new ProfileFacetIcon(
        ProfileFacetIconType.TEXTURE, SoulAscensionMod.id("textures/gui/icons/origins.png"));

    private NeoOriginsIdentityProjection() {
    }

    static ProfileFacet create(
        Optional<Origin> origin,
        Optional<Origin> selectedClass,
        ProfileFacetAudience audience
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(selectedClass, "selectedClass");
        return ProfileFacet.entries(
            FACET_ID,
            ProfileFacetText.translatable("profile_facet.soul_ascension.neoorigins.title"),
            Optional.of(FACET_ICON),
            audience,
            100,
            List.of(
                entry(ORIGIN_LAYER, "origin", origin, true),
                entry(CLASS_LAYER, "class", selectedClass, false)));
    }

    private static ProfileFacetEntry entry(
        ResourceLocation type,
        String translationSuffix,
        Optional<Origin> selected,
        boolean includeImpact
    ) {
        ProfileFacetText typeLabel = ProfileFacetText.translatable(
            "profile_facet.soul_ascension.neoorigins." + translationSuffix);
        if (selected.isEmpty()) {
            return new ProfileFacetEntry(
                type,
                Optional.empty(),
                typeLabel,
                Component.translatable(
                    "profile_facet.soul_ascension.neoorigins." + translationSuffix + "_not_selected"),
                Component.translatable(
                    "profile_facet.soul_ascension.neoorigins.selection_pending"),
                ItemStack.EMPTY,
                List.of());
        }

        Origin value = selected.orElseThrow();
        List<ProfileFacetField> metadata = includeImpact
            ? List.of(new ProfileFacetField(
                ProfileFacetText.translatable("profile_facet.soul_ascension.neoorigins.impact"),
                ProfileFacetText.translatable("profile_facet.soul_ascension.neoorigins.impact."
                    + value.impact().name().toLowerCase(Locale.ROOT)),
                false))
            : List.of();
        return new ProfileFacetEntry(
            type,
            Optional.of(value.id()),
            typeLabel,
            value.name(),
            value.description(),
            value.icon(),
            metadata);
    }
}
