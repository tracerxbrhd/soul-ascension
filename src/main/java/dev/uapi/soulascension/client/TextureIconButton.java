package dev.uapi.soulascension.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;

public final class TextureIconButton extends AbstractButton {
    private static final ResourceLocation NORMAL = dev.uapi.soulascension.SoulAscensionMod.id("character/icon_button_normal");
    private static final ResourceLocation HOVERED = dev.uapi.soulascension.SoulAscensionMod.id("character/icon_button_hovered");
    private static final ResourceLocation SELECTED = dev.uapi.soulascension.SoulAscensionMod.id("character/icon_button_selected");
    private static final ResourceLocation DISABLED = dev.uapi.soulascension.SoulAscensionMod.id("character/icon_button_disabled");
    private final ResourceLocation icon;
    private final Runnable action;
    private final BooleanSupplier selected;

    public TextureIconButton(int x, int y, int width, int height, ResourceLocation icon,
                             Component narration, Runnable action) {
        this(x, y, width, height, icon, narration, () -> false, action);
    }

    public TextureIconButton(int x, int y, int width, int height, ResourceLocation icon,
                             Component narration, BooleanSupplier selected, Runnable action) {
        super(x, y, width, height, narration);
        this.icon = icon;
        this.action = action;
        this.selected = selected;
        setTooltip(Tooltip.create(narration));
    }

    @Override public void onPress() {
        if (selected.getAsBoolean()) return;
        setFocused(false);
        action.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        boolean isSelected = selected.getAsBoolean();
        ResourceLocation sprite = !active ? DISABLED : isSelected ? SELECTED : hovered ? HOVERED : NORMAL;
        graphics.blitSprite(sprite, getX(), getY(), getWidth(), getHeight());
        int size = Math.min(20, Math.min(getWidth() - 6, getHeight() - 6));
        graphics.blit(icon, getX() + (getWidth() - size) / 2, getY() + (getHeight() - size) / 2,
            0, 0, size, size, 16, 16);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
