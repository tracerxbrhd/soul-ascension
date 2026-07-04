package dev.uapi.soulascension.item;

import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.progression.SoulAscensionService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class DebugProgressItem extends Item {
    public enum Action { LEVEL_UP, ADD_POINT }
    private final Action action;

    public DebugProgressItem(Action action, Properties properties) {
        super(properties);
        this.action = action;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.sidedSuccess(stack, true);
        if (!(player instanceof ServerPlayer serverPlayer) || !serverPlayer.hasPermissions(2)
            || !SoulAscensionServerConfig.DEBUG_ITEMS_ENABLED.get()) return InteractionResultHolder.fail(stack);
        switch (action) {
            case LEVEL_UP -> {
                PlayerProgress progress = SoulAscensionService.get(serverPlayer);
                SoulAscensionService.addExperience(serverPlayer,
                    Math.max(1.0, SoulAscensionService.requiredDamage(progress.level()) - progress.damageProgress()));
            }
            case ADD_POINT -> SoulAscensionService.addPoints(serverPlayer, 1);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.soul_ascension.admin_item").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("tooltip.soul_ascension.creative_testing").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.soul_ascension.debug." + action.name().toLowerCase(java.util.Locale.ROOT))
            .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
