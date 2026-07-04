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
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.network.chat.Component;

public final class SoulAscensionClientEvents {
    public static final KeyMapping OPEN = new KeyMapping("key.soul_ascension.open", InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_P, "key.categories.soul_ascension");
    private SoulAscensionClientEvents() {}

    @EventBusSubscriber(modid = SoulAscensionMod.MOD_ID, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN);
            UApiScreenTabs.register(SoulAscensionMod.id("character"), 100,
                Component.translatable("button.soul_ascension.character"),
                SoulAscensionMod.id("textures/gui/icons/character_tab.png"), minecraft -> new CharacterScreen());
        }

        @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(SoulAscensionMod.SOUL_ALTAR_BLOCK_ENTITY.get(), SoulAltarRenderer::new);
        }
    }

    @EventBusSubscriber(modid = SoulAscensionMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            while (OPEN.consumeClick()) Minecraft.getInstance().setScreen(new CharacterScreen());
            SoulLensOverlay.tick();
        }

        @SubscribeEvent public static void renderGui(RenderGuiEvent.Post event) {
            SoulLensOverlay.render(event.getGuiGraphics());
        }

        @SubscribeEvent public static void mouseScroll(InputEvent.MouseScrollingEvent event) {
            if (SoulLensOverlay.handleScroll(event.getScrollDeltaY())) event.setCanceled(true);
        }
    }
}
