package dev.uapi.soulascension.item;

import dev.uapi.accessory.AccessoryIntegrationService;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.progression.ProfilePrivacyService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public final class ConcealmentEmblemItem extends Item {
    public ConcealmentEmblemItem(Properties properties) { super(properties); }

    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!SoulAscensionServerConfig.CONCEALMENT_EMBLEM_ENABLED.get())
            return InteractionResultHolder.fail(stack);
        if (AccessoryIntegrationService.isLoaded()
            && !SoulAscensionServerConfig.EMBLEM_DIRECT_USE_WITH_ACCESSORY.get()) {
            if (!level.isClientSide()) player.displayClientMessage(
                Component.translatable("message.soul_ascension.concealment.wear_as_charm"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer player) {
            ProfilePrivacyService.activateEmblem(player,
                SoulAscensionServerConfig.EMBLEM_USE_UNLOCKS_TOGGLE.get(),
                SoulAscensionServerConfig.EMBLEM_USE_SETS_HIDDEN.get());
            if (SoulAscensionServerConfig.EMBLEM_USE_CONSUMES_ITEM.get() && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.soul_ascension.concealment_emblem.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.soul_ascension.concealment_emblem.2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
