package dev.uapi.soulascension;

import com.mojang.serialization.MapCodec;
import dev.uapi.reward.RewardRegistry;
import dev.uapi.creative.UApiCreativeTabs;
import dev.uapi.command.UApiCommandRegistry;
import dev.uapi.soulascension.command.SoulAscensionCommands;
import dev.uapi.soulascension.condition.SoulLensEnabledCondition;
import dev.uapi.soulascension.condition.StatBookLootEnabledCondition;
import dev.uapi.soulascension.config.SoulAscensionClientConfig;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.config.SoulAscensionConfigManager;
import dev.uapi.soulascension.config.SoulAscensionClientConfigManager;
import dev.uapi.soulascension.config.AttributeRewardsConfig;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.item.AmnesiaScrollItem;
import dev.uapi.soulascension.item.BlackBookItem;
import dev.uapi.soulascension.item.DebugProgressItem;
import dev.uapi.soulascension.item.SoulBadgeItem;
import dev.uapi.soulascension.item.WitheredMemoryPotionItem;
import dev.uapi.soulascension.item.SoulLensItem;
import dev.uapi.soulascension.integration.OptionalIntegrations;
import dev.uapi.soulascension.network.SoulAscensionNetwork;
import dev.uapi.soulascension.progression.AttributeService;
import dev.uapi.soulascension.progression.SoulAscensionEvents;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.title.TitleReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(SoulAscensionMod.MOD_ID)
public final class SoulAscensionMod {
    public static final String MOD_ID = "soul_ascension";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
        DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, MOD_ID);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<? extends ICondition>> SOUL_LENS_ENABLED_CONDITION =
        CONDITIONS.register("soul_lens_enabled", () -> SoulLensEnabledCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<? extends ICondition>> STAT_BOOK_LOOT_ENABLED_CONDITION =
        CONDITIONS.register("stat_book_loot_enabled", () -> StatBookLootEnabledCondition.CODEC);
    public static final DeferredItem<Item> AMNESIA_SCROLL = ITEMS.register("amnesia_scroll",
        () -> new AmnesiaScrollItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SOUL_BADGE = ITEMS.register("soul_badge",
        () -> new SoulBadgeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WITHERED_MEMORY_POTION = ITEMS.register("withered_memory_potion",
        () -> new WitheredMemoryPotionItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> SOUL_LENS = ITEMS.register("soul_lens",
        () -> new SoulLensItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BLACK_BOOK_STRENGTH = ITEMS.register("black_book_strength",
        () -> new BlackBookItem(Stat.STRENGTH, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BLACK_BOOK_ENDURANCE = ITEMS.register("black_book_endurance",
        () -> new BlackBookItem(Stat.ENDURANCE, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BLACK_BOOK_AGILITY = ITEMS.register("black_book_agility",
        () -> new BlackBookItem(Stat.AGILITY, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BLACK_BOOK_INTELLIGENCE = ITEMS.register("black_book_intelligence",
        () -> new BlackBookItem(Stat.INTELLIGENCE, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> BLACK_BOOK_PERCEPTION = ITEMS.register("black_book_perception",
        () -> new BlackBookItem(Stat.PERCEPTION, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> DEBUG_LEVEL_UP = ITEMS.register("debug_level_up",
        () -> new DebugProgressItem(DebugProgressItem.Action.LEVEL_UP, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DEBUG_ADD_POINT = ITEMS.register("debug_add_point",
        () -> new DebugProgressItem(DebugProgressItem.Action.ADD_POINT, new Item.Properties().stacksTo(1)));
    public static final UApiCreativeTabs.Registrar CREATIVE_TAB = UApiCreativeTabs.create(
        MOD_ID, "main", "itemGroup.soul_ascension", () -> AMNESIA_SCROLL.get().getDefaultInstance());

    public SoulAscensionMod(IEventBus modBus, ModContainer container) {
        // Vanilla 1.21.1 does not synchronize attack damage to clients. The
        // character screen needs the authoritative value without optional
        // attribute mods being installed.
        Attributes.ATTACK_DAMAGE.value().setSyncable(true);

        UApiCommandRegistry.registerSection("soulascension", SoulAscensionCommands::create);
        ITEMS.register(modBus);
        CONDITIONS.register(modBus);
        CREATIVE_TAB.add(id("amnesia_scroll"), 0);
        CREATIVE_TAB.add(id("soul_badge"), 10);
        CREATIVE_TAB.add(id("withered_memory_potion"), 20);
        CREATIVE_TAB.add(id("soul_lens"), 35,
            () -> SoulAscensionConfigManager.current().soulLensEnabled());
        CREATIVE_TAB.add(id("black_book_strength"), 50);
        CREATIVE_TAB.add(id("black_book_endurance"), 51);
        CREATIVE_TAB.add(id("black_book_agility"), 52);
        CREATIVE_TAB.add(id("black_book_intelligence"), 53);
        CREATIVE_TAB.add(id("black_book_perception"), 54);
        CREATIVE_TAB.add(id("debug_level_up"), 200,
            () -> SoulAscensionConfigManager.current().debugItemsEnabled());
        CREATIVE_TAB.add(id("debug_add_point"), 201,
            () -> SoulAscensionConfigManager.current().debugItemsEnabled());
        CREATIVE_TAB.registerBus(modBus);
        SoulAscensionAttachments.register(modBus);
        modBus.addListener(SoulAscensionNetwork::register);
        modBus.addListener(this::onConfigLoading);
        modBus.addListener(this::onConfigReload);
        modBus.addListener(this::commonSetup);
        AttributeRewardsConfig.bootstrapDefaults();
        container.registerConfig(ModConfig.Type.SERVER, SoulAscensionServerConfig.SPEC, "uapi/soul-ascension/server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, SoulAscensionClientConfig.SPEC, "uapi/soul-ascension/client.toml");
        NeoForge.EVENT_BUS.register(SoulAscensionEvents.class);
        NeoForge.EVENT_BUS.register(TitleReloadListener.class);
        RewardRegistry.registerProvider(ResourceLocation.fromNamespaceAndPath(MOD_ID, "experience"),
            SoulAscensionService::grantRewardExperience);
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SoulAscensionServerConfig.SPEC)
            SoulAscensionConfigManager.reloadFromSpec();
        else if (event.getConfig().getSpec() == SoulAscensionClientConfig.SPEC)
            SoulAscensionClientConfigManager.reloadFromSpec();
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SoulAscensionClientConfig.SPEC) {
            SoulAscensionClientConfigManager.reloadFromSpec();
            return;
        }
        if (event.getConfig().getSpec() != SoulAscensionServerConfig.SPEC) return;
        SoulAscensionConfigManager.reloadFromSpec();
        AttributeRewardsConfig.reload();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.getPlayerList().getPlayers().forEach(player -> {
            SoulAscensionService.refreshRules(player);
            AttributeService.apply(player, SoulAscensionService.get(player));
            dev.uapi.soulascension.title.TitleService.evaluate(player);
            SoulAscensionNetwork.syncRules(player);
        });
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Optional-mod attribute registries are complete by the time queued setup work runs.
        event.enqueueWork(() -> {
            OptionalIntegrations.bootstrap();
            AttributeRewardsConfig.reload();
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

}
