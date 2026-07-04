package dev.uapi.soulascension;

import com.mojang.serialization.MapCodec;
import dev.uapi.reward.RewardRegistry;
import dev.uapi.creative.UApiCreativeTabs;
import dev.uapi.command.UApiCommandRegistry;
import dev.uapi.soulascension.command.SoulAscensionCommands;
import dev.uapi.soulascension.condition.EmblemCraftableCondition;
import dev.uapi.soulascension.condition.SoulLensEnabledCondition;
import dev.uapi.soulascension.block.SoulAltarBlock;
import dev.uapi.soulascension.block.entity.SoulAltarBlockEntity;
import dev.uapi.soulascension.config.SoulAscensionClientConfig;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.item.AmnesiaScrollItem;
import dev.uapi.soulascension.item.DebugProgressItem;
import dev.uapi.soulascension.item.SoulBadgeItem;
import dev.uapi.soulascension.item.WitheredMemoryPotionItem;
import dev.uapi.soulascension.item.ConcealmentEmblemItem;
import dev.uapi.soulascension.item.SoulLensItem;
import dev.uapi.soulascension.network.SoulAscensionNetwork;
import dev.uapi.soulascension.progression.AttributeService;
import dev.uapi.soulascension.progression.SoulAscensionEvents;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.title.TitleReloadListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.alchemy.Potion;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredBlock;
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
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, MOD_ID);
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
        DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, MOD_ID);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<? extends ICondition>> EMBLEM_CRAFTABLE_CONDITION =
        CONDITIONS.register("concealment_emblem_craftable", () -> EmblemCraftableCondition.CODEC);
    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<? extends ICondition>> SOUL_LENS_ENABLED_CONDITION =
        CONDITIONS.register("soul_lens_enabled", () -> SoulLensEnabledCondition.CODEC);
    public static final DeferredHolder<Potion, Potion> WITHERED_MEMORY = POTIONS.register("withered_memory",
        () -> new Potion("withered_memory",
            new MobEffectInstance(MobEffects.WEAKNESS, 48 * 20, 0),
            new MobEffectInstance(MobEffects.CONFUSION, 48 * 20, 1),
            new MobEffectInstance(MobEffects.POISON, 3 * 20, 1)));
    public static final DeferredItem<Item> AMNESIA_SCROLL = ITEMS.register("amnesia_scroll",
        () -> new AmnesiaScrollItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> SOUL_BADGE = ITEMS.register("soul_badge",
        () -> new SoulBadgeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> WITHERED_MEMORY_POTION = ITEMS.register("withered_memory_potion",
        () -> new WitheredMemoryPotionItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CONCEALMENT_EMBLEM = ITEMS.register("concealment_emblem",
        () -> new ConcealmentEmblemItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<Item> SOUL_LENS = ITEMS.register("soul_lens",
        () -> new SoulLensItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredBlock<SoulAltarBlock> SOUL_ALTAR = BLOCKS.registerBlock("soul_altar", SoulAltarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0F, 1200.0F)
            .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredItem<BlockItem> SOUL_ALTAR_ITEM = ITEMS.registerSimpleBlockItem(SOUL_ALTAR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SoulAltarBlockEntity>> SOUL_ALTAR_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("soul_altar", () -> BlockEntityType.Builder.of(SoulAltarBlockEntity::new, SOUL_ALTAR.get()).build(null));
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
        BLOCKS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        POTIONS.register(modBus);
        CONDITIONS.register(modBus);
        CREATIVE_TAB.add(id("amnesia_scroll"), 0);
        CREATIVE_TAB.add(id("soul_badge"), 10);
        CREATIVE_TAB.add(id("withered_memory_potion"), 20);
        CREATIVE_TAB.add(id("concealment_emblem"), 30,
            () -> SoulAscensionServerConfig.CONCEALMENT_EMBLEM_ENABLED.get());
        CREATIVE_TAB.add(id("soul_lens"), 35,
            () -> SoulAscensionServerConfig.SOUL_LENS_ENABLED.get());
        CREATIVE_TAB.add(id("soul_altar"), 40);
        CREATIVE_TAB.add(id("debug_level_up"), 200,
            () -> SoulAscensionServerConfig.DEBUG_ITEMS_ENABLED.get());
        CREATIVE_TAB.add(id("debug_add_point"), 201,
            () -> SoulAscensionServerConfig.DEBUG_ITEMS_ENABLED.get());
        CREATIVE_TAB.registerBus(modBus);
        SoulAscensionAttachments.register(modBus);
        modBus.addListener(SoulAscensionNetwork::register);
        modBus.addListener(this::onConfigReload);
        container.registerConfig(ModConfig.Type.COMMON, SoulAscensionServerConfig.SPEC, "uapi/soul-ascension/server.toml");
        container.registerConfig(ModConfig.Type.CLIENT, SoulAscensionClientConfig.SPEC, "uapi/soul-ascension/client.toml");
        NeoForge.EVENT_BUS.register(SoulAscensionEvents.class);
        NeoForge.EVENT_BUS.register(TitleReloadListener.class);
        RewardRegistry.registerProvider(ResourceLocation.fromNamespaceAndPath(MOD_ID, "experience"),
            SoulAscensionService::grantRewardExperience);
    }

    private void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SoulAscensionServerConfig.SPEC) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.getPlayerList().getPlayers().forEach(player -> {
            SoulAscensionService.refreshRules(player);
            AttributeService.apply(player, SoulAscensionService.get(player));
            dev.uapi.soulascension.title.TitleService.evaluate(player);
            SoulAscensionNetwork.syncRules(player);
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

}
