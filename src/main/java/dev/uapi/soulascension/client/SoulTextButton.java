package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Resource-pack-friendly text button shared by Soul Ascension screens. */
public final class SoulTextButton extends AbstractButton {
    private static final Identifier NORMAL = SoulAscensionMod.id("character/tab_normal");
    private static final Identifier HOVERED = SoulAscensionMod.id("character/tab_hovered");
    private static final Identifier DISABLED = SoulAscensionMod.id("character/tab_disabled");
    private final Runnable action;

    public SoulTextButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override public void onPress(InputWithModifiers input) {
        setFocused(false);
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Identifier sprite = !active ? DISABLED : isMouseOver(mouseX, mouseY) ? HOVERED : NORMAL;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), getWidth(), getHeight());
        int color = active ? isMouseOver(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFF1E9FF : 0xFF716879;
        graphics.centeredText(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
            getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
