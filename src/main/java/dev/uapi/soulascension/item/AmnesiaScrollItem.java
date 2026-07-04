package dev.uapi.soulascension.item;

import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.network.ClientProgressionRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;

public final class AmnesiaScrollItem extends Item {
    public AmnesiaScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);
        SoulAscensionService.ResetResult result = SoulAscensionService.resetWithAmnesia(serverPlayer);
        if (!result.changed()) {
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
            SoundSource.PLAYERS, 1.0F, 0.75F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.soul_ascension.amnesia_scroll").withStyle(ChatFormatting.GRAY));
        if (ClientProgressionRules.amnesiaPointLossEnabled()) {
            tooltip.add(Component.translatable("tooltip.soul_ascension.amnesia_scroll.loss",
                formatPercent(ClientProgressionRules.amnesiaPointLossPercent())).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private static String formatPercent(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-9) return String.format(Locale.ROOT, "%.0f", value);
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
