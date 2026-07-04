package dev.uapi.soulascension.item;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.progression.SoulAscensionService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class WitheredMemoryPotionItem extends PotionItem {
    public static final int POTION_COLOR = 0x4A315C;

    public WitheredMemoryPotionItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createStack() {
        ItemStack stack = new ItemStack(SoulAscensionMod.WITHERED_MEMORY_POTION.get());
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
            Optional.of(SoulAscensionMod.WITHERED_MEMORY), Optional.of(POTION_COLOR), List.of()));
        return stack;
    }

    @Override
    public ItemStack getDefaultInstance() {
        return createStack();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer player) {
            SoulAscensionService.resetWithAmnesia(player);
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return "item.soul_ascension.withered_memory_potion";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.soul_ascension.withered_memory_potion")
            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
