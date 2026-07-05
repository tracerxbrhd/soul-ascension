package dev.uapi.soulascension.item;

import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;

public final class SoulLensItem extends SpyglassItem {
    public SoulLensItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // The common config is authoritative on the server. Checking the local
        // client copy here would incorrectly disable the lens on servers whose
        // setting differs from the player's local configuration.
        if (!level.isClientSide() && !SoulAscensionConfigManager.current().soulLensEnabled())
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.SPYGLASS; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return USE_DURATION; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.soul_ascension.soul_lens.tooltip.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.soul_ascension.soul_lens.tooltip.2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
