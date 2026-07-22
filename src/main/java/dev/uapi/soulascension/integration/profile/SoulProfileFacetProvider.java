package dev.uapi.soulascension.integration.profile;

import dev.uapi.api.profile.ProfileFacet;
import dev.uapi.api.profile.ProfileFacetAudience;
import dev.uapi.api.profile.ProfileFacetField;
import dev.uapi.api.profile.ProfileFacetIcon;
import dev.uapi.api.profile.ProfileFacetIconType;
import dev.uapi.api.profile.ProfileFacetProvider;
import dev.uapi.api.profile.ProfileFacetQuery;
import dev.uapi.api.profile.ProfileFacetText;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.TitleProgress;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.title.TitleRegistry;
import dev.uapi.soulascension.title.TitleService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Publishes only the same deliberately public progression subset as the Soul Badge/Lens. */
public final class SoulProfileFacetProvider implements ProfileFacetProvider {
    private static final Identifier PROVIDER_ID = SoulAscensionMod.id("profile_facets");
    private static final Identifier FACET_ID = SoulAscensionMod.id("public_progression");

    @Override
    public Identifier providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProfileFacet> provide(ProfileFacetQuery query) {
        ServerPlayer subject = query.server().getPlayerList().getPlayer(query.subjectId());
        if (subject == null) return List.of();
        boolean publicProfile = subject.getData(SoulAscensionAttachments.PROFILE_SETTINGS).publicProfile();
        if (!publicProfile && !query.selfView()) return List.of();
        PlayerProgress progress = SoulAscensionService.get(subject);
        if (!progress.isUsable()) return List.of();

        List<ProfileFacetField> fields = new ArrayList<>();
        fields.add(ProfileFacetField.prominent("profile_facet.soul_ascension.level",
            Integer.toString(progress.level())));
        Identifier activeTitle = TitleService.get(subject).activeTitle();
        if (!TitleProgress.NONE.equals(activeTitle)) {
            ProfileFacetText title = TitleRegistry.get(activeTitle)
                .map(definition -> ProfileFacetText.translatable(definition.nameKey()))
                .orElseGet(() -> ProfileFacetText.literal(activeTitle.toString()));
            fields.add(new ProfileFacetField(
                ProfileFacetText.translatable("profile_facet.soul_ascension.active_title"), title, true));
        }
        fields.add(ProfileFacetField.literal("profile_facet.soul_ascension.strength",
            Integer.toString(progress.strength())));
        fields.add(ProfileFacetField.literal("profile_facet.soul_ascension.endurance",
            Integer.toString(progress.endurance())));
        fields.add(ProfileFacetField.literal("profile_facet.soul_ascension.agility",
            Integer.toString(progress.agility())));
        fields.add(ProfileFacetField.literal("profile_facet.soul_ascension.intelligence",
            Integer.toString(progress.intelligence())));
        fields.add(ProfileFacetField.literal("profile_facet.soul_ascension.perception",
            Integer.toString(progress.perception())));
        return List.of(new ProfileFacet(FACET_ID,
            ProfileFacetText.translatable("profile_facet.soul_ascension.title"),
            Optional.of(new ProfileFacetIcon(ProfileFacetIconType.ITEM,
                SoulAscensionMod.id("soul_badge"))),
            publicProfile ? ProfileFacetAudience.PUBLIC : ProfileFacetAudience.SUBJECT_ONLY, 200, fields));
    }
}
