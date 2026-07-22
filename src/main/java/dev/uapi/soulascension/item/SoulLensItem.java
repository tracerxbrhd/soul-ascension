package dev.uapi.soulascension.item;

import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public final class SoulLensItem extends SpyglassItem {
    public SoulLensItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // The common config is authoritative on the server. Checking the local
        // client copy here would incorrectly disable the lens on servers whose
        // setting differs from the player's local configuration.
        if (!level.isClientSide() && !SoulAscensionConfigManager.current().soulLensEnabled())
            return InteractionResult.FAIL;
        return super.use(level, player, hand);
    }

    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.SPYGLASS; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return USE_DURATION; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.soul_ascension.soul_lens.tooltip.1").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.soul_ascension.soul_lens.tooltip.2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
