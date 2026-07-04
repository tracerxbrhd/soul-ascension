package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.network.SoulAltarActionPayload;
import dev.uapi.soulascension.network.SoulAltarOpenPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SoulAltarScreen extends Screen {
    private static final ResourceLocation PANEL = SoulAscensionMod.id("character/panel");
    private static final ResourceLocation INSET = SoulAscensionMod.id("character/inset");
    private static final int TEXT = 0xFFF1E9FF;
    private static final int MUTED = 0xFF9B91AA;
    private static final int ACCENT = 0xFFD66BFF;
    private static final int DIVIDER = 0xFF8E4BC4;
    private final SoulAltarOpenPayload data;
    private boolean confirmingRespec;

    public SoulAltarScreen(SoulAltarOpenPayload data) {
        super(Component.translatable("screen.soul_ascension.soul_altar.title"));
        this.data = data;
    }

    @Override
    protected void init() {
        int left = left(), top = top(), innerX = left + 24, innerWidth = panelWidth() - 48;
        if (confirmingRespec) {
            int half = (innerWidth - 8) / 2;
            addRenderableWidget(new SoulTextButton(innerX, top + 119, half, 22,
                Component.translatable("screen.soul_ascension.soul_altar.confirm_respec"), () ->
                    PacketDistributor.sendToServer(new SoulAltarActionPayload(
                        data.altarPos(), SoulAltarActionPayload.RESPEC, false))));
            addRenderableWidget(new SoulTextButton(innerX + half + 8, top + 119, half, 22,
                Component.translatable("gui.cancel"), () -> {
                    confirmingRespec = false;
                    rebuildWidgets();
                }));
            return;
        }

        SoulTextButton respec = new SoulTextButton(innerX, top + 91, innerWidth, 22,
            Component.translatable("screen.soul_ascension.soul_altar.respec"), () -> {
                if (data.requireConfirmation()) {
                    confirmingRespec = true;
                    rebuildWidgets();
                } else PacketDistributor.sendToServer(new SoulAltarActionPayload(
                    data.altarPos(), SoulAltarActionPayload.RESPEC, false));
            });
        respec.active = data.allowRespec();
        addRenderableWidget(respec);

        SoulCheckbox visibility = new SoulCheckbox(innerX + 3, top + 126, innerWidth - 6,
            Component.translatable("screen.soul_ascension.soul_altar.hide_profile"), data::profileHidden, hidden ->
                PacketDistributor.sendToServer(new SoulAltarActionPayload(
                    data.altarPos(), SoulAltarActionPayload.SET_VISIBILITY, hidden)));
        visibility.active = data.canToggleVisibility();
        addRenderableWidget(visibility);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xB0080612);
        int left = left(), top = top(), panelWidth = panelWidth(), panelHeight = panelHeight();
        graphics.blitSprite(PANEL, left, top, panelWidth, panelHeight);
        graphics.drawString(font, title, left + 14, top + 13, TEXT, false);
        graphics.fill(left + 10, top + 32, left + panelWidth - 10, top + 33, DIVIDER);
        graphics.blitSprite(INSET, left + 12, top + 42, panelWidth - 24, panelHeight - 54);

        if (confirmingRespec) {
            graphics.drawCenteredString(font, Component.translatable("screen.soul_ascension.soul_altar.confirm_description"),
                left + panelWidth / 2, top + 68, ACCENT);
            graphics.drawCenteredString(font, Component.translatable("screen.soul_ascension.soul_altar.respec_description"),
                left + panelWidth / 2, top + 87, MUTED);
        } else {
            graphics.drawCenteredString(font, Component.translatable("screen.soul_ascension.soul_altar.respec_description"),
                left + panelWidth / 2, top + 57, MUTED);
            Component cost = Component.translatable("screen.soul_ascension.soul_altar.cost." + data.costType(), data.costAmount());
            graphics.drawCenteredString(font, cost, left + panelWidth / 2, top + 116, MUTED);
            graphics.drawCenteredString(font, Component.translatable("screen.soul_ascension.soul_altar.privacy_description"),
                left + panelWidth / 2, top + 153, MUTED);
            if (data.effectiveHidden() && !data.profileHidden()) {
                graphics.drawCenteredString(font, Component.translatable("screen.soul_ascension.soul_altar.concealment_forced"),
                    left + panelWidth / 2, top + 170, ACCENT);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private int panelWidth() { return Math.min(500, Math.max(330, width - 24)); }
    private int panelHeight() { return Math.min(220, Math.max(190, height - 28)); }
    private int left() { return (width - panelWidth()) / 2; }
    private int top() { return (height - panelHeight()) / 2; }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
}
