package dev.uapi.soulascension.item;

import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.progression.SoulAscensionService;
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

/** Server-authoritative consumable that directly increases one configured stat. */
public final class BlackBookItem extends Item {
    private final Stat stat;

    public BlackBookItem(Stat stat, Properties properties) {
        super(properties);
        this.stat = stat;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.fail(stack);

        if (!SoulAscensionService.increaseStatFromBook(serverPlayer, stat))
            return InteractionResultHolder.fail(stack);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
            SoundSource.PLAYERS, 0.8F, 0.75F);
        serverPlayer.displayClientMessage(Component.translatable("message.soul_ascension.black_book."
                + stat.name().toLowerCase(Locale.ROOT))
            .withStyle(ChatFormatting.DARK_PURPLE), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.soul_ascension.black_book."
            + stat.name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.soul_ascension.black_book.no_recipe").withStyle(ChatFormatting.DARK_GRAY));
    }
}
