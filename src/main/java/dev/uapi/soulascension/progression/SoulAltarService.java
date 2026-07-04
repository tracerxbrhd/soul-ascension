package dev.uapi.soulascension.progression;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.ProfilePrivacyData;
import dev.uapi.soulascension.network.SoulAltarActionPayload;
import dev.uapi.soulascension.network.SoulAltarOpenPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

public final class SoulAltarService {
    private static final double MAX_DISTANCE_SQUARED = 64.0;

    private SoulAltarService() {}

    public static boolean isValidAccess(ServerPlayer player, BlockPos pos) {
        return player.level().getBlockState(pos).is(SoulAscensionMod.SOUL_ALTAR.get())
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MAX_DISTANCE_SQUARED;
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        if (!SoulAscensionServerConfig.ALTAR_ENABLED.get() || !isValidAccess(player, pos)) {
            player.displayClientMessage(Component.translatable("message.soul_ascension.soul_altar.disabled"), true);
            return;
        }
        ProfilePrivacyData privacy = ProfilePrivacyService.get(player);
        String costType = SoulAscensionServerConfig.ALTAR_RESPEC_COST_TYPE.get().toLowerCase(Locale.ROOT);
        PacketDistributor.sendToPlayer(player, new SoulAltarOpenPayload(pos,
            SoulAscensionServerConfig.ALTAR_ALLOW_RESPEC.get() && !costType.equals("disabled"),
            SoulAscensionServerConfig.ALTAR_RESPEC_CONFIRMATION.get(), ProfilePrivacyService.canToggle(player),
            privacy.profileHidden(), ProfilePrivacyService.isEffectivelyHidden(player), costType,
            SoulAscensionServerConfig.ALTAR_RESPEC_COST_AMOUNT.get()));
    }

    public static void handleAction(ServerPlayer player, BlockPos pos, int action, boolean value) {
        if (!isValidAccess(player, pos) || !SoulAscensionServerConfig.ALTAR_ENABLED.get()) return;
        if (action == SoulAltarActionPayload.RESPEC) {
            respec(player);
        } else if (action == SoulAltarActionPayload.SET_VISIBILITY) {
            if (!ProfilePrivacyService.setHidden(player, value))
                player.displayClientMessage(Component.translatable("tooltip.soul_ascension.requires_concealment_emblem"), true);
        } else return;
        open(player, pos);
    }

    private static void respec(ServerPlayer player) {
        if (!SoulAscensionServerConfig.ALTAR_ALLOW_RESPEC.get()) return;
        String type = SoulAscensionServerConfig.ALTAR_RESPEC_COST_TYPE.get().toLowerCase(Locale.ROOT);
        int amount = SoulAscensionServerConfig.ALTAR_RESPEC_COST_AMOUNT.get();
        if (type.equals("disabled") || !pay(player, type, amount)) {
            player.displayClientMessage(Component.translatable("message.soul_ascension.soul_altar.cannot_pay"), true);
            return;
        }
        SoulAscensionService.respecWithoutLoss(player);
        player.displayClientMessage(Component.translatable("message.soul_ascension.soul_altar.respec_complete"), true);
    }

    private static boolean pay(ServerPlayer player, String type, int amount) {
        if (amount <= 0 || type.equals("none") || player.getAbilities().instabuild) return true;
        return switch (type) {
            case "xp_levels" -> {
                if (player.experienceLevel < amount) yield false;
                player.giveExperienceLevels(-amount);
                yield true;
            }
            case "experience_points" -> {
                if (player.totalExperience < amount) yield false;
                player.giveExperiencePoints(-amount);
                yield true;
            }
            case "item" -> payItem(player, amount);
            default -> false;
        };
    }

    private static boolean payItem(ServerPlayer player, int amount) {
        ResourceLocation id = ResourceLocation.tryParse(SoulAscensionServerConfig.ALTAR_RESPEC_COST_ITEM.get());
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR || player.getInventory().countItem(item) < amount) return false;
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int removed = Math.min(stack.getCount(), remaining);
            stack.shrink(removed);
            remaining -= removed;
        }
        player.getInventory().setChanged();
        return remaining == 0;
    }
}
