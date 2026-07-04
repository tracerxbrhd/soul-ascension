package dev.uapi.soulascension;

import dev.uapi.reward.RewardRegistry;
import dev.uapi.creative.UApiCreativeTabs;
import dev.uapi.command.UApiCommandRegistry;
import dev.uapi.soulascension.command.SoulAscensionCommands;
import dev.uapi.soulascension.config.SoulAscensionClientConfig;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.network.SoulAscensionNetwork;
import dev.uapi.soulascension.progression.SoulAscensionEvents;
import dev.uapi.soulascension.progression.SoulAscensionService;
import dev.uapi.soulascension.title.TitleReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import dev.uapi.soulascension.item.DebugProgressItem;
import dev.uapi.soulascension.item.AmnesiaScrollItem;
import dev.uapi.soulascension.progression.AttributeService;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@Mod(SoulAscensionMod.MOD_ID)
public final class SoulAscensionMod {
    public static final String MOD_ID = "soul_ascension";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<Item> AMNESIA_SCROLL = ITEMS.register("amnesia_scroll",
        () -> new AmnesiaScrollItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<Item> DEBUG_LEVEL_UP = ITEMS.register("debug_level_up",
        () -> new DebugProgressItem(DebugProgressItem.Action.LEVEL_UP, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DEBUG_ADD_POINT = ITEMS.register("debug_add_point",
        () -> new DebugProgressItem(DebugProgressItem.Action.ADD_POINT, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DEBUG_RESET = ITEMS.register("debug_reset",
        () -> new DebugProgressItem(DebugProgressItem.Action.RESET, new Item.Properties().stacksTo(1)));
    public static final UApiCreativeTabs.Registrar CREATIVE_TAB = UApiCreativeTabs.create(
        MOD_ID, "main", "itemGroup.soul_ascension", () -> AMNESIA_SCROLL.get().getDefaultInstance());

    public SoulAscensionMod(IEventBus modBus, ModContainer container) {
        // Vanilla 1.21.1 does not synchronize attack damage to clients. The
        // character screen needs the authoritative value without optional
        // attribute mods being installed.
        Attributes.ATTACK_DAMAGE.value().setSyncable(true);

        UApiCommandRegistry.registerSection("soulascension", SoulAscensionCommands::create);
        ITEMS.register(modBus);
        CREATIVE_TAB.add(id("amnesia_scroll"), 0);
        CREATIVE_TAB.add(id("debug_level_up"), 200,
            () -> SoulAscensionServerConfig.DEBUG_ITEMS_ENABLED.get());
        CREATIVE_TAB.add(id("debug_add_point"), 201,
            () -> SoulAscensionServerConfig.DEBUG_ITEMS_ENABLED.get());
        CREATIVE_TAB.add(id("debug_reset"), 202,
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
        });
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

}
