package dev.uapi.soulascension.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.function.BooleanSupplier;

public final class TextureIconButton extends AbstractButton {
    private static final Identifier NORMAL = dev.uapi.soulascension.SoulAscensionMod.id("character/icon_button_normal");
    private static final Identifier HOVERED = dev.uapi.soulascension.SoulAscensionMod.id("character/icon_button_hovered");
    private static final Identifier SELECTED = dev.uapi.soulascension.SoulAscensionMod.id("character/icon_button_selected");
    private static final Identifier DISABLED = dev.uapi.soulascension.SoulAscensionMod.id("character/icon_button_disabled");
    private final Identifier icon;
    private final Runnable action;
    private final BooleanSupplier selected;

    public TextureIconButton(int x, int y, int width, int height, Identifier icon,
                             Component narration, Runnable action) {
        this(x, y, width, height, icon, narration, () -> false, action);
    }

    public TextureIconButton(int x, int y, int width, int height, Identifier icon,
                             Component narration, BooleanSupplier selected, Runnable action) {
        super(x, y, width, height, narration);
        this.icon = icon;
        this.action = action;
        this.selected = selected;
        setTooltip(Tooltip.create(narration));
    }

    @Override public void onPress(InputWithModifiers input) {
        if (selected.getAsBoolean()) return;
        setFocused(false);
        action.run();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        boolean isSelected = selected.getAsBoolean();
        Identifier sprite = !active ? DISABLED : isSelected ? SELECTED : hovered ? HOVERED : NORMAL;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, getX(), getY(), getWidth(), getHeight());
        int size = Math.min(20, Math.min(getWidth() - 6, getHeight() - 6));
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon,
            getX() + (getWidth() - size) / 2, getY() + (getHeight() - size) / 2,
            0, 0, size, size, 16, 16);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
