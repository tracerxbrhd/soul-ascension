package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class SoulCheckbox extends AbstractButton {
    private static final ResourceLocation NORMAL = SoulAscensionMod.id("character/icon_button_normal");
    private static final ResourceLocation HOVERED = SoulAscensionMod.id("character/icon_button_hovered");
    private static final ResourceLocation SELECTED = SoulAscensionMod.id("character/icon_button_selected");
    private static final ResourceLocation DISABLED = SoulAscensionMod.id("character/icon_button_disabled");
    private final BooleanSupplier checked;
    private final Consumer<Boolean> action;

    public SoulCheckbox(int x, int y, int width, Component message, BooleanSupplier checked, Consumer<Boolean> action) {
        super(x, y, width, 18, message);
        this.checked = checked;
        this.action = action;
    }

    @Override public void onPress() {
        setFocused(false);
        action.accept(!checked.getAsBoolean());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = checked.getAsBoolean();
        ResourceLocation sprite = !active ? DISABLED : selected ? SELECTED : isMouseOver(mouseX, mouseY) ? HOVERED : NORMAL;
        graphics.blitSprite(sprite, getX(), getY() + 1, 16, 16);
        if (selected) graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal("✓"),
            getX() + 8, getY() + 5, active ? 0xFFFFFFFF : 0xFF716879);
        graphics.drawString(Minecraft.getInstance().font, getMessage(), getX() + 22, getY() + 5,
            active ? 0xFFF1E9FF : 0xFF716879, false);
    }

    @Override protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
