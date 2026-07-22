package dev.uapi.soulascension.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.client.UApiScreenTabs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;

public final class SoulAscensionClientEvents {
    private static final KeyMapping.Category CATEGORY =
        new KeyMapping.Category(SoulAscensionMod.id("controls"));
    public static final KeyMapping OPEN = new KeyMapping("key.soul_ascension.open", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P, CATEGORY);
    private SoulAscensionClientEvents() {}

    @EventBusSubscriber(modid = SoulAscensionMod.MOD_ID, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.registerCategory(CATEGORY);
            event.register(OPEN);
            UApiScreenTabs.register(SoulAscensionMod.id("character"), 100,
                Component.translatable("button.soul_ascension.character"),
                SoulAscensionMod.id("textures/gui/icons/character_tab.png"), minecraft -> new CharacterScreen());
        }
    }

    @EventBusSubscriber(modid = SoulAscensionMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            SoulLensOverlay.registerHud();
            while (OPEN.consumeClick()) Minecraft.getInstance().setScreenAndShow(new CharacterScreen());
        }

        @SubscribeEvent public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
            if (SoulLensOverlay.handleScroll(event.getScrollDeltaY())) event.setCanceled(true);
        }
    }
}
