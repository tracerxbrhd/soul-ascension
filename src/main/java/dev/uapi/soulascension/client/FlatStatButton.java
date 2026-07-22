package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class FlatStatButton extends AbstractButton {
    private static final Identifier NORMAL = SoulAscensionMod.id("character/stat_button");
    private static final Identifier HIGHLIGHTED = SoulAscensionMod.id("character/stat_button_highlighted");
    private static final Identifier DISABLED = SoulAscensionMod.id("character/stat_button_disabled");
    private final Identifier icon;
    private final Runnable action;

    FlatStatButton(int x, int y, int width, int height, Component narration,
                   Identifier icon, Runnable action) {
        super(x, y, width, height, narration);
        this.icon = icon;
        this.action = action;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        Identifier sprite = !active ? DISABLED : hovered ? HIGHLIGHTED : NORMAL;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), getWidth(), getHeight());
        int iconSize = Math.min(8, Math.min(getWidth(), getHeight()) - 4);
        if ((iconSize & 1) != (getWidth() & 1)) iconSize--;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, icon, getX() + (getWidth() - iconSize) / 2,
            getY() + (getHeight() - iconSize) / 2, iconSize, iconSize);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
