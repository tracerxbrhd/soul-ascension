package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Resource-pack-friendly text button shared by Soul Ascension screens. */
public final class SoulTextButton extends AbstractButton {
    private static final ResourceLocation NORMAL = SoulAscensionMod.id("character/tab_normal");
    private static final ResourceLocation HOVERED = SoulAscensionMod.id("character/tab_hovered");
    private static final ResourceLocation DISABLED = SoulAscensionMod.id("character/tab_disabled");
    private final Runnable action;

    public SoulTextButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    @Override public void onPress() {
        setFocused(false);
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation sprite = !active ? DISABLED : isMouseOver(mouseX, mouseY) ? HOVERED : NORMAL;
        graphics.blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
        int color = active ? isMouseOver(mouseX, mouseY) ? 0xFFFFFFFF : 0xFFF1E9FF : 0xFF716879;
        graphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, getMessage(),
            getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
