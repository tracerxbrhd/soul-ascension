package dev.uapi.soulascension.progression;

import dev.uapi.accessory.AccessoryIntegrationService;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.ProfilePrivacyData;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import net.minecraft.server.level.ServerPlayer;

public final class ProfilePrivacyService {
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

    public static boolean canToggle(ServerPlayer player) {
        if (!SoulAscensionServerConfig.PROFILE_PRIVACY_ENABLED.get()
            || !SoulAscensionServerConfig.ALTAR_ALLOW_PROFILE_TOGGLE.get()) return false;
        ProfilePrivacyData privacy = get(player);
        // A hidden player must always be able to make the profile visible again,
        // even when the original concealment item is no longer present.
        return privacy.profileHidden() || privacy.concealmentUnlocked() || hasEquippedEmblem(player);
    }

    public static boolean setHidden(ServerPlayer player, boolean hidden) {
        if (!canToggle(player)) return false;
        player.setData(SoulAscensionAttachments.PROFILE_PRIVACY, get(player).withHidden(hidden));
        return true;
    }

    public static void activateEmblem(ServerPlayer player, boolean unlockToggle, boolean setHidden) {
        player.setData(SoulAscensionAttachments.PROFILE_PRIVACY,
            get(player).activateEmblem(unlockToggle, setHidden));
    }

    public static boolean mayInspect(ServerPlayer viewer, ServerPlayer target) {
        if (viewer == target) return true;
        if (!SoulAscensionServerConfig.PROFILE_PRIVACY_ENABLED.get()
            || !SoulAscensionServerConfig.HIDE_FROM_BADGE_INSPECTION.get()) return true;
        if (!isEffectivelyHidden(target)) return true;
        return SoulAscensionServerConfig.OPERATORS_BYPASS_HIDDEN.get() && viewer.hasPermissions(2);
    }

    public static boolean mayInspectWithLens(ServerPlayer viewer, ServerPlayer target) {
        if (viewer == target) return true;
        if (!SoulAscensionServerConfig.PROFILE_PRIVACY_ENABLED.get()
            || !SoulAscensionServerConfig.SOUL_LENS_RESPECT_HIDDEN.get()
            || !SoulAscensionServerConfig.HIDE_FROM_SOUL_LENS.get()) return true;
        if (!isEffectivelyHidden(target)) return true;
        return SoulAscensionServerConfig.SOUL_LENS_OPERATOR_BYPASS.get() && viewer.hasPermissions(2);
    }
}
