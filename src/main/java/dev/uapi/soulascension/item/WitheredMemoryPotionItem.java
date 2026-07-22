package dev.uapi.soulascension.item;

import dev.uapi.soulascension.progression.SoulAscensionService;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Consumer;

/** Standalone drink item: deliberately not a vanilla Potion, so no splash/lingering/arrow variants exist. */
public final class WitheredMemoryPotionItem extends Item {
    public static final int LONG_EFFECT_DURATION_TICKS = 45 * 20;
    public static final int STRONG_EFFECT_DURATION_TICKS = 9 * 20;

    public WitheredMemoryPotionItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createStack() {
        return new ItemStack(dev.uapi.soulascension.SoulAscensionMod.WITHERED_MEMORY_POTION.get());
    }

    @Override
    public ItemStack getDefaultInstance() {
        return createStack();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        level.playSound(null, livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
            SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        livingEntity.gameEvent(GameEvent.DRINK);
        if (!level.isClientSide()) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, LONG_EFFECT_DURATION_TICKS, 0));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, LONG_EFFECT_DURATION_TICKS, 0));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, STRONG_EFFECT_DURATION_TICKS, 1));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, STRONG_EFFECT_DURATION_TICKS, 1));
            if (livingEntity instanceof ServerPlayer player) {
                CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
                SoulAscensionService.resetWithAmnesia(player);
            }
        }
        if (livingEntity instanceof Player player)
            return ItemUtils.createFilledResult(stack, player, Items.GLASS_BOTTLE.getDefaultInstance());
        stack.shrink(1);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.soul_ascension.withered_memory_potion.weakness")
            .withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip.soul_ascension.withered_memory_potion.nausea")
            .withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip.soul_ascension.withered_memory_potion.poison")
            .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.accept(Component.translatable("tooltip.soul_ascension.withered_memory_potion.wither")
            .withStyle(ChatFormatting.DARK_RED));
        tooltip.accept(Component.empty());
        tooltip.accept(Component.translatable("tooltip.soul_ascension.withered_memory_potion")
            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
