package dev.uapi.soulascension.progression;

import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.DamageLedger;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.registry.SoulAscensionTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.uapi.soulascension.title.TitleRegistry;
import dev.uapi.soulascension.title.TitleService;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulAscensionEvents {
    private static final Map<UUID, FarmMemory> FARM_MEMORY = new ConcurrentHashMap<>();
    private static int titleTimer;
    private SoulAscensionEvents() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer player) || target == player || target instanceof ServerPlayer) return;
        if (target.getType().is(SoulAscensionTags.NO_EXPERIENCE)) return;
        if (SoulAscensionServerConfig.IGNORE_TAMED.get() && target instanceof TamableAnimal animal && animal.isTame()) return;
        if (SoulAscensionServerConfig.IGNORE_PLAYER_CREATED.get()
            && target.getTags().contains("soul_ascension:player_created")) return;
        double actualDamage = Math.max(0, event.getNewDamage());
        if (actualDamage <= 0) return;

        DamageLedger ledger = target.getData(SoulAscensionAttachments.DAMAGE_LEDGER);
        double previous = ledger.credited(player.getUUID());
        double credit = Math.min(actualDamage, Math.max(0, target.getMaxHealth() - previous));
        if (credit <= 0) return;
        target.setData(SoulAscensionAttachments.DAMAGE_LEDGER, ledger.withCredit(player.getUUID(), previous + credit));
        SoulAscensionService.addExperience(player, credit * farmMultiplier(player.getUUID(), target.getType()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SoulAscensionService.refreshRules(player);
            AttributeService.apply(player, SoulAscensionService.get(player));
            TitleService.evaluate(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SoulAscensionService.refreshRules(player);
        AttributeService.apply(player, SoulAscensionService.get(player));
        if (SoulAscensionServerConfig.FULL_HEALTH_AFTER_RESPAWN.get()) player.setHealth(player.getMaxHealth());
        else if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            AttributeService.apply(player, SoulAscensionService.get(player));
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
        event.getRelevantPlayers().forEach(player -> {
            TitleService.evaluate(player); TitleService.syncDefinitions(player);
        });
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
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
        int excess = Math.max(0, streak - SoulAscensionServerConfig.REPEAT_THRESHOLD.get());
        return Math.max(SoulAscensionServerConfig.MINIMUM_FARM_MULTIPLIER.get(),
            1.0 / (1.0 + excess * SoulAscensionServerConfig.REPEAT_PENALTY.get()));
    }

    private record FarmMemory(EntityType<?> type, int streak, long lastHit) {}
}
