package dev.uapi.soulascension.item;

import dev.uapi.soulascension.progression.PublicProfileService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Server-authoritative entry point for opening character profiles. */
public final class SoulBadgeItem extends Item {
    public SoulBadgeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PublicProfileService.open(serverPlayer, serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget,
                                                   InteractionHand usedHand) {
        if (!player.level().isClientSide() && player instanceof ServerPlayer viewer) {
            ServerPlayer target = !player.isShiftKeyDown() && interactionTarget instanceof ServerPlayer other
                ? other : viewer;
            PublicProfileService.open(viewer, target);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.soul_ascension.soul_badge").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.soul_ascension.concealment_emblem.tooltip.crafting")
            .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
