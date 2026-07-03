package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

final class FlatStatButton extends AbstractButton {
    private static final ResourceLocation NORMAL = SoulAscensionMod.id("character/stat_button");
    private static final ResourceLocation HIGHLIGHTED = SoulAscensionMod.id("character/stat_button_highlighted");
    private static final ResourceLocation DISABLED = SoulAscensionMod.id("character/stat_button_disabled");
    private final ResourceLocation icon;
    private final Runnable action;

    FlatStatButton(int x, int y, int width, int height, Component narration,
                   ResourceLocation icon, Runnable action) {
        super(x, y, width, height, narration);
        this.icon = icon;
        this.action = action;
    }

    @Override
    public void onPress() {
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        ResourceLocation sprite = !active ? DISABLED : hovered ? HIGHLIGHTED : NORMAL;
        graphics.blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
        int iconSize = Math.min(8, Math.min(getWidth(), getHeight()) - 4);
        if ((iconSize & 1) != (getWidth() & 1)) iconSize--;
        graphics.blitSprite(icon, getX() + (getWidth() - iconSize) / 2,
            getY() + (getHeight() - iconSize) / 2, iconSize, iconSize);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
