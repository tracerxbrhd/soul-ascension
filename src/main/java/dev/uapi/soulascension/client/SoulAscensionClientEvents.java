package dev.uapi.soulascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.client.UApiScreenTabs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;

public final class SoulAscensionClientEvents {
    private static final ModelResourceLocation SOUL_LENS_MODEL =
        ModelResourceLocation.inventory(SoulAscensionMod.id("soul_lens"));
    private static final ModelResourceLocation SOUL_LENS_IN_HAND_MODEL =
        ModelResourceLocation.standalone(SoulAscensionMod.id("item/soul_lens_in_hand"));
    public static final KeyMapping OPEN = new KeyMapping("key.soul_ascension.open", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P, "key.categories.soul_ascension");
    private SoulAscensionClientEvents() {}

    @EventBusSubscriber(modid = SoulAscensionMod.MOD_ID, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
            event.register(SOUL_LENS_IN_HAND_MODEL);
        }

        @SubscribeEvent public static void modifyModels(ModelEvent.ModifyBakingResult event) {
            BakedModel inventoryModel = event.getModels().get(SOUL_LENS_MODEL);
            BakedModel inHandModel = event.getModels().get(SOUL_LENS_IN_HAND_MODEL);
            if (inventoryModel != null && inHandModel != null) {
                event.getModels().put(SOUL_LENS_MODEL, new SoulLensBakedModel(inventoryModel, inHandModel));
                SoulAscensionMod.LOGGER.debug("Installed context-aware Soul Lens models");
            } else {
                SoulAscensionMod.LOGGER.error("Could not install Soul Lens models: inventory={}, inHand={}",
                    inventoryModel != null, inHandModel != null);
            }
        }

        @SubscribeEvent public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
            SoulLensOverlay.registerHud();
            UApiScreenTabs.register(SoulAscensionMod.id("character"), 100,
                Component.translatable("button.soul_ascension.character"),
                SoulAscensionMod.id("textures/gui/icons/character_tab.png"), minecraft -> new CharacterScreen());
        }
    }

    @EventBusSubscriber(modid = SoulAscensionMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            while (OPEN.consumeClick()) Minecraft.getInstance().setScreen(new CharacterScreen());
        }

        @SubscribeEvent public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
            if (SoulLensOverlay.handleScroll(event.getScrollDeltaY())) event.setCanceled(true);
        }
    }
}
