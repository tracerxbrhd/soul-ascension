package dev.uapi.soulascension.progression;

import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import dev.uapi.soulascension.config.SoulAscensionRuntimeConfig;
import dev.uapi.soulascension.config.AttributeRewardsConfig;
import dev.uapi.soulascension.item.WitheredMemoryPotionItem;
import dev.uapi.soulascension.integration.OptionalIntegrations;
import dev.uapi.soulascension.network.SoulAscensionNetwork;
import dev.uapi.soulascension.data.DamageLedger;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.registry.SoulAscensionTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Ingredient;
import dev.uapi.soulascension.title.TitleRegistry;
import dev.uapi.soulascension.title.TitleService;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulAscensionEvents {
    private static final Map<UUID, FarmMemory> FARM_MEMORY = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> VANILLA_XP_REMAINDER = new ConcurrentHashMap<>();
    private static int titleTimer;
    private SoulAscensionEvents() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || target == player || target instanceof ServerPlayer) return;
        if (target.getType().is(SoulAscensionTags.NO_EXPERIENCE)) return;
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        if (config.ignoreTamed() && target instanceof TamableAnimal animal && animal.isTame()) return;
        if (config.ignorePlayerCreated()
            && target.getTags().contains("soul_ascension:player_created")) return;
        double actualDamage = Math.max(0, event.getNewDamage());
        if (actualDamage <= 0) return;

        DamageLedger ledger = target.getData(SoulAscensionAttachments.DAMAGE_LEDGER);
        double previous = ledger.credited(player.getUUID());
        double credit = Math.min(actualDamage, Math.max(0, target.getMaxHealth() - previous));
        if (credit <= 0) return;
        target.setData(SoulAscensionAttachments.DAMAGE_LEDGER, ledger.withCredit(player.getUUID(), previous + credit));
        double intelligenceMultiplier = AttributeRewardsConfig.affectsSoulProgression()
            ? AttributeRewardsConfig.experienceMultiplier(SoulAscensionService.get(player).intelligence()) : 1.0;
        double creditedProgress = credit * farmMultiplier(player.getUUID(), target.getType()) * intelligenceMultiplier;
        SoulAscensionService.addExperience(player, creditedProgress);
    }

    @SubscribeEvent
    public static void onVanillaExperience(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0
            || !AttributeRewardsConfig.affectsVanillaExperience()) return;
        double multiplier = AttributeRewardsConfig.experienceMultiplier(
            SoulAscensionService.get(player).intelligence());
        double exactBonus = event.getAmount() * Math.max(0.0, multiplier - 1.0)
            + VANILLA_XP_REMAINDER.getOrDefault(player.getUUID(), 0.0);
        long wholeBonus = (long) Math.floor(exactBonus);
        VANILLA_XP_REMAINDER.put(player.getUUID(), exactBonus - wholeBonus);
        event.setAmount((int) Math.min(Integer.MAX_VALUE, (long) event.getAmount() + wholeBonus));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulAscensionService.refreshRules(player);
            AttributeService.apply(player, SoulAscensionService.get(player));
            TitleService.evaluate(player);
            SoulAscensionNetwork.syncRules(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SoulAscensionService.refreshRules(player);
        AttributeService.apply(player, SoulAscensionService.get(player));
        if (SoulAscensionConfigManager.current().fullHealthAfterRespawn()) player.setHealth(player.getMaxHealth());
        else if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AttributeService.apply(player, SoulAscensionService.get(player));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        FARM_MEMORY.remove(playerId);
        VANILLA_XP_REMAINDER.remove(playerId);
        SoulLensService.forget(playerId);
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player && event.getEntity() != player)
            TitleService.addEntityKill(player, BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()));
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        int amount = Math.max(0, event.getOriginalStack().getCount() - event.getCurrentStack().getCount());
        if (amount > 0) TitleService.addCollectedItem(player,
            BuiltInRegistries.ITEM.getKey(event.getOriginalStack().getItem()), amount);
    }

    @SubscribeEvent
    public static void onBlockMined(BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getPlayer() instanceof ServerPlayer player)
            TitleService.addMinedBlock(player, BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()));
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) AttributeRewardsConfig.reload();
        event.getRelevantPlayers().forEach(player -> {
            AttributeService.apply(player, SoulAscensionService.get(player));
            TitleService.evaluate(player);
            TitleService.syncDefinitions(player);
            SoulAscensionNetwork.syncRules(player);
        });
    }

    @SubscribeEvent
    public static void registerBrewingRecipe(RegisterBrewingRecipesEvent event) {
        var longWeakness = PotionContents.createItemStack(Items.POTION, Potions.LONG_WEAKNESS);
        Ingredient input = DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS,
            longWeakness.get(DataComponents.POTION_CONTENTS), Items.POTION);
        event.getBuilder().addRecipe(input, Ingredient.of(Items.WITHER_ROSE),
            WitheredMemoryPotionItem.createStack());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        OptionalIntegrations.flushAttributeRefreshes(event.getServer());
        if (!TitleRegistry.hasTimeBasedTitles() || ++titleTimer < 200) return;
        titleTimer = 0;
        event.getServer().getPlayerList().getPlayers().forEach(TitleService::evaluate);
    }

    private static double farmMultiplier(UUID player, EntityType<?> type) {
        long now = System.currentTimeMillis();
        FarmMemory previous = FARM_MEMORY.get(player);
        int streak = previous != null && previous.type == type && now - previous.lastHit < 300_000L
            ? previous.streak + 1 : 1;
        FARM_MEMORY.put(player, new FarmMemory(type, streak, now));
        SoulAscensionRuntimeConfig config = SoulAscensionConfigManager.current();
        int excess = Math.max(0, streak - config.repeatThreshold());
        return Math.max(config.minimumFarmMultiplier(),
            1.0 / (1.0 + excess * config.repeatPenalty()));
    }

    private record FarmMemory(EntityType<?> type, int streak, long lastHit) {}
}
