package dev.uapi.soulascension.progression;

import dev.uapi.accessory.AccessoryIntegrationService;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.ProfilePrivacyData;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import net.minecraft.server.level.ServerPlayer;

public final class ProfilePrivacyService {
    public enum InspectionChannel { SOUL_BADGE, SOUL_LENS }

    private ProfilePrivacyService() {}

    public static ProfilePrivacyData get(ServerPlayer player) {
        ensureInitialized(player);
        return player.getData(SoulAscensionAttachments.PROFILE_PRIVACY);
    }

    public static void ensureInitialized(ServerPlayer player) {
        ProfilePrivacyData current = player.getData(SoulAscensionAttachments.PROFILE_PRIVACY);
        if (!current.initialized()) {
            player.setData(SoulAscensionAttachments.PROFILE_PRIVACY,
                current.initialized(SoulAscensionServerConfig.PROFILE_DEFAULT_HIDDEN.get()));
        }
    }

    public static boolean hasEquippedEmblem(ServerPlayer player) {
        if (!SoulAscensionServerConfig.ACCESSORIES_ENABLED.get()
            || !SoulAscensionServerConfig.PREFER_UAPI_ACCESSORY_SERVICE.get()) return false;
        return AccessoryIntegrationService.isItemEquipped(player,
            stack -> stack.is(SoulAscensionMod.CONCEALMENT_EMBLEM.get()));
    }

    public static boolean isEffectivelyHidden(ServerPlayer player) {
        if (!SoulAscensionServerConfig.PROFILE_PRIVACY_ENABLED.get()) return false;
        return get(player).profileHidden() || hasEquippedEmblem(player);
    }

    public static boolean isProfileHidden(ServerPlayer player) {
        return isEffectivelyHidden(player);
    }

    public static boolean getManualProfileHidden(ServerPlayer player) {
        return get(player).profileHidden();
    }

    public static boolean hasConcealmentEffect(ServerPlayer player) {
        return hasEquippedEmblem(player);
    }

    public static boolean canToggle(ServerPlayer player) {
        if (!SoulAscensionServerConfig.PROFILE_PRIVACY_ENABLED.get()
            || !SoulAscensionServerConfig.ALTAR_ALLOW_PROFILE_TOGGLE.get()) return false;
        return true;
    }

    public static boolean setHidden(ServerPlayer player, boolean hidden) {
        if (!canToggle(player)) return false;
        player.setData(SoulAscensionAttachments.PROFILE_PRIVACY, get(player).withHidden(hidden));
        return true;
    }

    public static boolean setProfileHidden(ServerPlayer player, boolean hidden) {
        return setHidden(player, hidden);
    }

    public static void activateEmblem(ServerPlayer player, boolean unlockToggle, boolean setHidden) {
        player.setData(SoulAscensionAttachments.PROFILE_PRIVACY,
            get(player).activateEmblem(unlockToggle, setHidden));
    }

    public static boolean mayInspect(ServerPlayer viewer, ServerPlayer target) {
        return canInspectProfile(viewer, target, InspectionChannel.SOUL_BADGE);
    }

    public static boolean mayInspectWithLens(ServerPlayer viewer, ServerPlayer target) {
        return canInspectProfile(viewer, target, InspectionChannel.SOUL_LENS);
    }

    public static boolean canInspectProfile(ServerPlayer viewer, ServerPlayer target, InspectionChannel channel) {
        if (viewer == target) return true;
        if (!SoulAscensionServerConfig.PROFILE_PRIVACY_ENABLED.get()) return true;
        boolean privacyApplies = switch (channel) {
            case SOUL_BADGE -> SoulAscensionServerConfig.HIDE_FROM_BADGE_INSPECTION.get();
            case SOUL_LENS -> SoulAscensionServerConfig.SOUL_LENS_RESPECT_HIDDEN.get()
                && SoulAscensionServerConfig.HIDE_FROM_SOUL_LENS.get();
        };
        if (!privacyApplies || !isProfileHidden(target)) return true;
        return SoulAscensionServerConfig.OPERATORS_BYPASS_HIDDEN.get() && viewer.hasPermissions(2);
    }
}
