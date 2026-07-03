package dev.uapi.soulascension.client;

import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.client.UApiTabHost;
import dev.uapi.soulascension.config.SoulAscensionClientConfig;
import dev.uapi.soulascension.config.SoulAscensionServerConfig;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.data.TitleProgress;
import dev.uapi.soulascension.network.ClientTitleCatalog;
import dev.uapi.soulascension.network.ClientTitleDefinition;
import dev.uapi.soulascension.network.SelectTitlePayload;
import dev.uapi.soulascension.network.SpendStatPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CharacterScreen extends Screen implements UApiTabHost {
    private enum Page { ATTRIBUTES, TITLES, INTEGRATION }
    private record UiTheme(int backgroundOverlay, int text, int mutedText, int valueText,
                           int positiveText, int accent, int divider) {}
    private record TitleHitbox(int x, int y, int width, int height, ResourceLocation id, boolean unlocked) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }
    private record AttributeHitbox(int x, int y, int width, int height, ResourceLocation id) {
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
    }

    private static final ResourceLocation ATTRIBUTES_ICON = SoulAscensionMod.id("textures/gui/icons/attributes.png");
    private static final ResourceLocation TITLES_ICON = SoulAscensionMod.id("textures/gui/icons/title.png");
    private static final ResourceLocation PANEL_SPRITE = SoulAscensionMod.id("character/panel");
    private static final ResourceLocation INSET_SPRITE = SoulAscensionMod.id("character/inset");
    private static final ResourceLocation SECTION_SPRITE = SoulAscensionMod.id("character/section");
    private static final ResourceLocation PROGRESS_BACKGROUND_SPRITE = SoulAscensionMod.id("character/progress_background");
    private static final ResourceLocation PROGRESS_FILL_SPRITE = SoulAscensionMod.id("character/progress_fill");
    private static final ResourceLocation STAT_PLUS_ICON = SoulAscensionMod.id("character/stat_plus");
    private static final ResourceLocation STAT_MINUS_ICON = SoulAscensionMod.id("character/stat_minus");
    private static final ResourceLocation[] STAT_ICONS = {
        SoulAscensionMod.id("textures/gui/stats/strength.png"),
        SoulAscensionMod.id("textures/gui/stats/endurance.png"),
        SoulAscensionMod.id("textures/gui/stats/agility.png"),
        SoulAscensionMod.id("textures/gui/stats/intelligence.png"),
        SoulAscensionMod.id("textures/gui/stats/perception.png")
    };
    private static final UiTheme THEME = new UiTheme(0xA8080612, 0xFFF1E9FF, 0xFF9B91AA,
        0xFFD79BFF, 0xFFB9FFDB, 0xFFD66BFF, 0xFF8E4BC4);
    private static final int STAT_ROW_INSET = 6;
    private static final int STAT_ICON_INSET = 6;
    private static final int STAT_BUTTON_RIGHT_INSET = 8;
    private static final int STAT_CONTROL_GAP = 3;

    private PlayerProgress progress = PlayerProgress.initial();
    private TitleProgress titles = TitleProgress.initial();
    private Page page = Page.ATTRIBUTES;
    private ResourceLocation integrationId;
    private int scroll;
    private int attributeListScroll;
    private int attributeDetailScroll;
    private final List<TitleHitbox> titleHitboxes = new ArrayList<>();
    private final List<AttributeHitbox> attributeHitboxes = new ArrayList<>();
    private final AttributeViewModel attributeViewModel = new AttributeViewModel();
    private final FlatStatButton[] increaseButtons = new FlatStatButton[Stat.values().length];
    private final FlatStatButton[] decreaseButtons = new FlatStatButton[Stat.values().length];

    public CharacterScreen() { super(Component.translatable("screen.soul_ascension.title")); }

    @Override
    protected void init() {
        refresh();
        if (minecraft != null && minecraft.player != null) {
            attributeViewModel.forceRefresh();
            attributeViewModel.tick(minecraft.player, progress);
        }
        int left = panelLeft(), top = panelTop(), panelWidth = panelWidth(), panelHeight = panelHeight();
        int characterWidth = characterWidth();
        int railSize = railButtonSize();
        int railStep = railSize + 4;
        int railX = left + panelWidth - railSize - 7;
        int railY = top + 40;
        addRenderableWidget(new TextureIconButton(railX, railY, railSize, railSize, ATTRIBUTES_ICON,
            Component.translatable("screen.soul_ascension.attributes"), () -> page == Page.ATTRIBUTES,
            () -> switchPage(Page.ATTRIBUTES, null)));
        addRenderableWidget(new TextureIconButton(railX, railY + railStep, railSize, railSize, TITLES_ICON,
            Component.translatable("screen.soul_ascension.titles"), () -> page == Page.TITLES,
            () -> switchPage(Page.TITLES, null)));
        List<CharacterIntegrationRegistry.Tab> integrations = CharacterIntegrationRegistry.visibleTabs();
        for (int i = 0; i < integrations.size(); i++) {
            CharacterIntegrationRegistry.Tab tab = integrations.get(i);
            addRenderableWidget(new TextureIconButton(railX, railY + railStep * (2 + i), railSize, railSize,
                tab.icon(), tab.title(), () -> page == Page.INTEGRATION && tab.id().equals(integrationId),
                () -> switchPage(Page.INTEGRATION, tab.id())));
        }

        int rowHeight = statRowHeight();
        int statY = statStartY();
        int buttonSize = Math.max(10, Math.min(14, rowHeight - 3));
        if ((buttonSize & 1) != 0) buttonSize--;
        int rowRight = left + characterWidth - 10;
        int buttonsRight = rowRight - STAT_BUTTON_RIGHT_INSET;
        int plusX = buttonsRight - buttonSize;
        int minusX = plusX - buttonSize - STAT_CONTROL_GAP;
        for (Stat stat : Stat.values()) {
            int y = statY + stat.ordinal() * rowHeight;
            int buttonY = y + 4 - buttonSize / 2;
            FlatStatButton decrease = new FlatStatButton(minusX, buttonY, buttonSize, buttonSize,
                Component.literal("−"), STAT_MINUS_ICON, () -> {
                    PacketDistributor.sendToServer(new SpendStatPayload(stat.ordinal(), -1));
                });
            FlatStatButton increase = new FlatStatButton(plusX, buttonY, buttonSize, buttonSize,
                Component.literal("+"), STAT_PLUS_ICON, () -> {
                    PacketDistributor.sendToServer(new SpendStatPayload(stat.ordinal(), 1));
                });
            decreaseButtons[stat.ordinal()] = addRenderableWidget(decrease);
            increaseButtons[stat.ordinal()] = addRenderableWidget(increase);
        }
        updateStatButtons();
    }

    private void switchPage(Page value, ResourceLocation integration) {
        page = value; integrationId = integration; scroll = 0; attributeListScroll = 0;
        attributeDetailScroll = 0; titleHitboxes.clear(); attributeHitboxes.clear();
        setFocused(null);
    }

    private void refresh() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            progress = player.getData(SoulAscensionAttachments.PROGRESS);
            titles = player.getData(SoulAscensionAttachments.TITLES);
            updateStatButtons();
        }
    }

    @Override public void tick() {
        refresh();
        if (minecraft != null && minecraft.player != null) attributeViewModel.tick(minecraft.player, progress);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refresh();
        UiTheme theme = theme();
        graphics.fill(0, 0, width, height, theme.backgroundOverlay());
        int left = panelLeft(), top = panelTop(), panelWidth = panelWidth(), panelHeight = panelHeight();
        int characterWidth = characterWidth();
        drawPanel(graphics, left, top, panelWidth, panelHeight);
        graphics.drawString(font, title, left + 12, top + 12, theme.text(), false);

        renderCharacter(graphics, mouseX, mouseY, left + 7, top + 37, characterWidth - 11, panelHeight - 44);
        int contentX = left + characterWidth + 3;
        int contentY = top + 37;
        int contentWidth = panelWidth - characterWidth - railReserve();
        int contentHeight = panelHeight - 44;
        drawSurface(graphics, INSET_SPRITE, contentX, contentY, contentWidth, contentHeight);
        switch (page) {
            case ATTRIBUTES -> renderAttributes(graphics, contentX, contentY, contentWidth, contentHeight);
            case TITLES -> renderTitles(graphics, contentX, contentY, contentWidth, contentHeight);
            case INTEGRATION -> renderIntegration(graphics, contentX, contentY, contentWidth, contentHeight);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderCharacter(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        drawSurface(graphics, INSET_SPRITE, x, y, width, height);
        UiTheme theme = theme();
        Component playerName = minecraft == null || minecraft.player == null ? Component.empty()
            : Component.literal(minecraft.player.getGameProfile().getName());
        Component selectedTitle = ClientTitleCatalog.get(titles.activeTitle())
            .<Component>map(value -> Component.translatable(value.nameKey())).orElse(Component.empty());
        boolean hasTitle = !selectedTitle.getString().isEmpty();
        int titleY = y + 8;
        int nameY = hasTitle ? y + 20 : y + 10;
        int levelY = nameY + 12;
        int dividerY = levelY + 12;
        if (hasTitle) graphics.drawCenteredString(font, trim(selectedTitle, width - 16), x + width / 2, titleY, theme.accent());
        graphics.drawCenteredString(font, trim(playerName, width - 16), x + width / 2, nameY, theme.text());
        graphics.drawCenteredString(font, trim(Component.translatable("screen.soul_ascension.level", progress.level()), width - 16),
            x + width / 2, levelY, theme.text());
        graphics.fill(x + 8, dividerY, x + width - 8, dividerY + 1, theme.divider());

        int statY = statStartY();
        int modelTop = dividerY + 4;
        int availableModelHeight = Math.max(20, Math.min(125, statY - modelTop - 21));
        int modelWidth = Math.max(28, Math.min(88, (availableModelHeight * 3) / 4));
        int modelBottom = modelTop + availableModelHeight;
        if (minecraft != null && minecraft.player != null) {
            int modelScale = Math.max(12, Math.min(42, 12 + availableModelHeight / 4));
            int modelLeft = x + (width - modelWidth) / 2;
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, modelLeft, modelTop,
                modelLeft + modelWidth, modelBottom, modelScale, 0.0625F, mouseX, mouseY, minecraft.player);
        }

        int rowHeight = statRowHeight();
        graphics.drawString(font, trim(Component.translatable("screen.soul_ascension.points", progress.unspentPoints()), width - 18),
            x + 9, statY - Math.min(17, rowHeight), progress.unspentPoints() > 0 ? theme.accent() : theme.mutedText(), false);
        for (Stat stat : Stat.values()) {
            int rowY = statY + stat.ordinal() * rowHeight;
            int rowX = x + STAT_ROW_INSET;
            int rowVisualHeight = Math.min(20, Math.max(11, rowHeight - 2));
            int rowTop = rowY + 4 - rowVisualHeight / 2;
            drawSurface(graphics, SECTION_SPRITE, rowX, rowTop, width - STAT_ROW_INSET * 2, rowVisualHeight);
            Component name = Component.translatable("stat.soul_ascension.short." + stat.name().toLowerCase(Locale.ROOT));
            int iconSize = Math.min(16, Math.max(9, rowVisualHeight - 4));
            int iconX = rowX + STAT_ICON_INSET;
            int iconY = rowTop + (rowVisualHeight - iconSize) / 2;
            graphics.blit(STAT_ICONS[stat.ordinal()], iconX, iconY, 0, 0, iconSize, iconSize, 16, 16);
            int labelX = iconX + iconSize + 5;
            graphics.drawString(font, trim(name, Math.max(28, width - 105)), labelX, rowY, theme.text(), false);
            graphics.drawString(font, Integer.toString(progress.stat(stat)), x + width - 70, rowY, theme.positiveText(), false);
        }
        renderProgressBar(graphics, x + 8, y + height - 19, width - 16);
    }

    private void renderAttributes(GuiGraphics graphics, int x, int y, int width, int height) {
        UiTheme theme = theme();
        graphics.drawString(font, Component.translatable("screen.soul_ascension.attributes"), x + 10, y + 9, theme.text(), false);
        graphics.fill(x + 10, y + 22, x + width - 10, y + 23, theme.divider());
        if (minecraft == null || minecraft.player == null) return;
        int paneTop = y + 29;
        int paneHeight = height - 35;
        int gap = 7;
        int listWidth = attributeListWidth(width);
        int listX = x + 9;
        int detailX = listX + listWidth + gap;
        int detailWidth = x + width - 9 - detailX;
        drawSurface(graphics, SECTION_SPRITE, listX, paneTop, listWidth, paneHeight);
        drawSurface(graphics, SECTION_SPRITE, detailX, paneTop, detailWidth, paneHeight);
        renderAttributeList(graphics, listX, paneTop, listWidth, paneHeight, theme);
        renderAttributeDetail(graphics, detailX, paneTop, detailWidth, paneHeight, theme);
    }

    private void renderAttributeList(GuiGraphics graphics, int x, int y, int width, int height, UiTheme theme) {
        attributeHitboxes.clear();
        int lineY = y + 5 - attributeListScroll;
        graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
        for (AttributeViewModel.Group group : attributeViewModel.groups()) {
            int headingY = lineY;
            graphics.fill(x + 5, headingY + 5, x + width / 2 - 18, headingY + 6, theme.divider());
            graphics.fill(x + width / 2 + 18, headingY + 5, x + width - 5, headingY + 6, theme.divider());
            graphics.drawCenteredString(font, trim(group.category().title(), Math.max(20, width - 42)),
                x + width / 2, headingY + 1, theme.accent());
            lineY += 15;
            for (AttributeViewModel.DisplayEntry entry : group.entries()) {
                boolean selected = entry.id().equals(attributeViewModel.selectedId());
                if (selected) graphics.blitSprite(SoulAscensionMod.id("character/attribute_selected"),
                    x + 4, lineY, width - 8, 15);
                int valueWidth = font.width(entry.formattedCurrent());
                int valueX = x + width - 7 - valueWidth;
                graphics.drawString(font, trim(entry.name(), Math.max(18, valueX - x - 12)),
                    x + 7, lineY + 3, selected ? theme.accent() : theme.text(), false);
                graphics.drawString(font, entry.formattedCurrent(), valueX, lineY + 3, theme.valueText(), false);
                attributeHitboxes.add(new AttributeHitbox(x + 4, lineY, width - 8, 15, entry.id()));
                lineY += 16;
            }
            lineY += 3;
        }
        graphics.disableScissor();
    }

    private void renderAttributeDetail(GuiGraphics graphics, int x, int y, int width, int height, UiTheme theme) {
        AttributeViewModel.DisplayEntry entry = attributeViewModel.selected().orElse(null);
        if (entry == null) {
            graphics.drawCenteredString(font, Component.translatable("screen.soul_ascension.attribute.none"),
                x + width / 2, y + height / 2, theme.mutedText());
            return;
        }
        int lineY = y + 7 - attributeDetailScroll;
        int textX = x + 8;
        int textWidth = Math.max(20, width - 16);
        graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
        graphics.drawString(font, trim(entry.name(), textWidth), textX, lineY, theme.accent(), false);
        lineY += 14;
        boolean showId = SoulAscensionClientConfig.SHOW_ATTRIBUTE_NAMESPACES.get()
            || minecraft.options.advancedItemTooltips;
        if (showId) {
            graphics.drawString(font, trim(Component.literal(entry.id().toString()), textWidth),
                textX, lineY, theme.mutedText(), false);
            lineY += 13;
        }
        lineY = drawDetailValue(graphics, textX, lineY, textWidth,
            Component.translatable("screen.soul_ascension.attribute.current"), entry.formattedCurrent(), theme);
        lineY = drawDetailValue(graphics, textX, lineY, textWidth,
            Component.translatable("screen.soul_ascension.attribute.base"), entry.formattedBase(), theme);
        lineY = drawDetailValue(graphics, textX, lineY, textWidth,
            Component.translatable("screen.soul_ascension.attribute.category"), entry.category().title(), theme);
        lineY += 3;
        for (net.minecraft.util.FormattedCharSequence part : font.split(entry.description(), textWidth)) {
            graphics.drawString(font, part, textX, lineY, theme.text(), false);
            lineY += 11;
        }
        lineY += 7;
        graphics.drawString(font, Component.translatable("screen.soul_ascension.attribute.sources"),
            textX, lineY, theme.accent(), false);
        lineY += 14;
        graphics.drawString(font, Component.translatable("screen.soul_ascension.source.base"),
            textX, lineY, theme.text(), false);
        graphics.drawString(font, entry.formattedBase(), x + width - 8 - font.width(entry.formattedBase()),
            lineY, theme.valueText(), false);
        lineY += 16;
        for (AttributeViewModel.SourceEntry source : entry.sources()) {
            int sourceX = textX;
            if (!source.icon().isEmpty()) {
                graphics.renderItem(source.icon(), sourceX, lineY - 2);
                sourceX += 19;
            }
            Component amount = Component.empty().append(source.formattedAmount());
            int amountX = x + width - 8 - font.width(amount);
            graphics.drawString(font, trim(source.name(), Math.max(20, amountX - sourceX - 4)),
                sourceX, lineY, theme.text(), false);
            graphics.drawString(font, amount, amountX, lineY,
                source.amount() >= 0 ? theme.positiveText() : 0xFFFF7777, false);
            if (showId) graphics.drawString(font, trim(Component.literal(source.modifierId().toString()), textWidth - 7),
                sourceX, lineY + 11, theme.mutedText(), false);
            lineY += showId || !source.icon().isEmpty() ? 24 : 16;
        }
        graphics.disableScissor();
    }

    private int drawDetailValue(GuiGraphics graphics, int x, int y, int width, Component label,
                                Component value, UiTheme theme) {
        graphics.drawString(font, label, x, y, theme.mutedText(), false);
        graphics.drawString(font, trim(value, Math.max(16, width / 2)),
            x + width - font.width(trim(value, Math.max(16, width / 2))), y, theme.valueText(), false);
        return y + 13;
    }

    private void renderTitles(GuiGraphics graphics, int x, int y, int width, int height) {
        UiTheme theme = theme();
        graphics.drawString(font, Component.translatable("screen.soul_ascension.titles"), x + 10, y + 9, theme.text(), false);
        graphics.fill(x + 10, y + 22, x + width - 10, y + 23, theme.divider());
        titleHitboxes.clear();
        int cardY = y + 30 - scroll;
        graphics.enableScissor(x + 4, y + 25, x + width - 4, y + height - 4);
        for (ClientTitleDefinition definition : ClientTitleCatalog.all()) {
            boolean unlocked = titles.unlocked().contains(definition.id());
            if (definition.hidden() && !unlocked) continue;
            boolean selected = titles.activeTitle().equals(definition.id());
            int cardHeight = 39;
            drawSurface(graphics, SECTION_SPRITE, x + 9, cardY, width - 18, cardHeight);
            if (selected) graphics.renderOutline(x + 10, cardY + 1, width - 20, cardHeight - 2, theme.accent());
            graphics.blit(definition.icon(), x + 14, cardY + 4, 0, 0, 30, 30, 32, 32);
            Component name = Component.translatable(definition.nameKey());
            graphics.drawString(font, name, x + 49, cardY + 6, unlocked ? theme.accent() : theme.mutedText(), false);
            Component description = Component.translatable(definition.descriptionKey());
            graphics.drawString(font, trim(description, Math.max(30, width - 65)),
                x + 49, cardY + 20, unlocked ? theme.text() : theme.mutedText(), false);
            if (!unlocked) graphics.drawString(font, Component.translatable("screen.soul_ascension.locked"),
                x + width - 55, cardY + 6, 0xFFFF7777, false);
            titleHitboxes.add(new TitleHitbox(x + 9, cardY, width - 18, cardHeight, definition.id(), unlocked));
            cardY += cardHeight + 5;
        }
        graphics.disableScissor();
    }

    private void renderIntegration(GuiGraphics graphics, int x, int y, int width, int height) {
        UiTheme theme = theme();
        CharacterIntegrationRegistry.Tab selected = CharacterIntegrationRegistry.visibleTabs().stream()
            .filter(tab -> tab.id().equals(integrationId)).findFirst().orElse(null);
        Component heading = selected == null ? Component.translatable("screen.soul_ascension.integration") : selected.title();
        graphics.drawString(font, heading, x + 10, y + 9, theme.text(), false);
        graphics.fill(x + 10, y + 22, x + width - 10, y + 23, theme.divider());
        if (selected == null || minecraft == null || minecraft.player == null) return;
        int lineY = y + 34 - scroll;
        graphics.enableScissor(x + 4, y + 25, x + width - 4, y + height - 4);
        for (Component line : selected.lines().apply(minecraft.player)) {
            for (net.minecraft.util.FormattedCharSequence part : font.split(line, width - 24)) {
                graphics.drawString(font, part, x + 12, lineY, theme.text(), false); lineY += 13;
            }
            lineY += 3;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int contentX = panelLeft() + characterWidth() + 3;
        int contentY = panelTop() + 37;
        int contentWidth = panelWidth() - characterWidth() - 39;
        int contentHeight = panelHeight() - 44;
        if (page == Page.ATTRIBUTES && button == 0) {
            for (AttributeHitbox hitbox : attributeHitboxes) if (hitbox.contains(mouseX, mouseY)) {
                attributeViewModel.select(hitbox.id());
                attributeDetailScroll = 0;
                return true;
            }
        }
        if (page == Page.TITLES && button == 0 && mouseX >= contentX && mouseX < contentX + contentWidth
            && mouseY >= contentY + 25 && mouseY < contentY + contentHeight) {
            for (TitleHitbox hitbox : titleHitboxes) if (hitbox.unlocked() && hitbox.contains(mouseX, mouseY)) {
                ResourceLocation selected = titles.activeTitle().equals(hitbox.id()) ? TitleProgress.NONE : hitbox.id();
                PacketDistributor.sendToServer(new SelectTitlePayload(selected));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == Page.ATTRIBUTES) {
            int contentX = panelLeft() + characterWidth() + 3;
            int contentWidth = panelWidth() - characterWidth() - railReserve();
            int listWidth = attributeListWidth(contentWidth);
            int amount = (int) Math.signum(scrollY) * 18;
            if (mouseX < contentX + 9 + listWidth + 4)
                attributeListScroll = Math.max(0, Math.min(maxAttributeListScroll(), attributeListScroll - amount));
            else attributeDetailScroll = Math.max(0, Math.min(maxAttributeDetailScroll(), attributeDetailScroll - amount));
            return true;
        }
        if (page == Page.TITLES || page == Page.INTEGRATION) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY) * 18));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blitSprite(PANEL_SPRITE, x, y, width, height);
        UiTheme theme = theme();
        graphics.fill(x + 7, y + 31, x + width - 7, y + 32, theme.divider());
    }

    private int panelWidth() { return Math.max(280, Math.min(760, width - 12)); }
    private int panelHeight() { return Math.max(145, Math.min(420, height - 30)); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelTop() { return Math.max(24, (height - panelHeight()) / 2 + 6); }
    private int characterWidth() { return Math.max(140, Math.min(210, panelWidth() / 3)); }
    private int statRowHeight() {
        return panelHeight() < 180 ? 13 : panelHeight() < 230 ? 15 : panelHeight() < 320 ? 18 : 22;
    }
    private int statStartY() {
        return panelTop() + panelHeight() - 28 - Stat.values().length * statRowHeight();
    }
    private int railButtonSize() { return panelHeight() < 240 ? 18 : 24; }
    private int railReserve() { return railButtonSize() + 15; }
    private int attributeListWidth(int contentWidth) {
        int available = Math.max(40, contentWidth - 25);
        return Math.max(40, Math.min(190, available * 2 / 5));
    }

    private UiTheme theme() {
        return THEME;
    }

    private void drawSurface(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
        graphics.blitSprite(sprite, x, y, width, height);
    }

    private void renderProgressBar(GuiGraphics graphics, int x, int y, int width) {
        double required = progress.requiredDamage() > 0 ? progress.requiredDamage() : 1.0;
        double ratio = Math.max(0.0, Math.min(1.0, progress.damageProgress() / required));
        UiTheme theme = theme();
        graphics.blitSprite(PROGRESS_BACKGROUND_SPRITE, x, y, width, 12);
        int filledWidth = (int) ((width - 4) * ratio);
        if (filledWidth > 0) graphics.blitSprite(PROGRESS_FILL_SPRITE, x + 2, y + 2, filledWidth, 8);
        Component text = Component.translatable("screen.soul_ascension.progress_values",
            formatProgress(progress.damageProgress()), formatProgress(required));
        graphics.drawCenteredString(font, trim(text, width - 4), x + width / 2, y + 2, theme.text());
    }

    private String formatProgress(double value) {
        return value >= 100 || Math.rint(value) == value ? String.format(Locale.ROOT, "%.0f", value)
            : String.format(Locale.ROOT, "%.1f", value);
    }

    private void updateStatButtons() {
        if (minecraft == null || minecraft.player == null) return;
        for (Stat stat : Stat.values()) {
            FlatStatButton increase = increaseButtons[stat.ordinal()];
            FlatStatButton decrease = decreaseButtons[stat.ordinal()];
            if (increase != null) {
                int maximum = SoulAscensionServerConfig.MAX_POINTS_PER_STAT.get();
                boolean limited = SoulAscensionServerConfig.LIMIT_STAT_POINTS.get() && maximum > 0;
                increase.active = progress.unspentPoints() > 0 && (!limited || progress.stat(stat) < maximum);
                increase.setTooltip(Tooltip.create(adjustmentTooltip(stat, 1)));
            }
            if (decrease != null) {
                decrease.active = SoulAscensionServerConfig.ALLOW_STAT_DECREASE.get() && progress.stat(stat) > 0;
                decrease.setTooltip(Tooltip.create(adjustmentTooltip(stat, -1)));
            }
        }
    }

    private Component adjustmentTooltip(Stat stat, int delta) {
        if (delta > 0 && progress.unspentPoints() <= 0) return Component.translatable("screen.soul_ascension.no_free_points");
        if (delta < 0 && progress.stat(stat) <= 0) return Component.translatable("screen.soul_ascension.no_allocated_points");
        int maximum = SoulAscensionServerConfig.MAX_POINTS_PER_STAT.get();
        if (delta > 0 && SoulAscensionServerConfig.LIMIT_STAT_POINTS.get()
            && maximum > 0 && progress.stat(stat) >= maximum)
            return Component.translatable("screen.soul_ascension.stat_at_max");
        List<StatAttributePreview.Change> changes = StatAttributePreview.change(minecraft.player, progress, stat, delta);
        if (changes.isEmpty()) return Component.translatable("screen.soul_ascension.no_attribute_changes");
        var result = Component.empty();
        for (int index = 0; index < changes.size(); index++) {
            StatAttributePreview.Change change = changes.get(index);
            if (index > 0) result.append("\n");
            ChatFormatting color = change.afterValue() > change.beforeValue() ? ChatFormatting.GREEN
                : change.afterValue() < change.beforeValue() ? ChatFormatting.RED : ChatFormatting.GRAY;
            result.append(change.name()).append(": ")
                .append(change.before().copy().withStyle(ChatFormatting.GRAY)).append(" ")
                .append(Component.literal("→").withStyle(color)).append(" ")
                .append(change.after().copy().withStyle(color));
            if (change.capped()) result.append(" ").append(
                Component.translatable("screen.soul_ascension.capped").withStyle(ChatFormatting.YELLOW));
        }
        return result;
    }

    private int maxScroll() {
        int contentWidth = panelWidth() - characterWidth() - railReserve();
        int contentHeight = panelHeight() - 44;
        if (page == Page.TITLES) {
            long count = ClientTitleCatalog.all().stream()
                .filter(value -> !value.hidden() || titles.unlocked().contains(value.id())).count();
            return Math.max(0, (int) count * 44 - Math.max(1, contentHeight - 32));
        }
        if (page == Page.INTEGRATION && minecraft != null && minecraft.player != null) {
            int lines = CharacterIntegrationRegistry.visibleTabs().stream().filter(tab -> tab.id().equals(integrationId))
                .findFirst().map(tab -> tab.lines().apply(minecraft.player).size()).orElse(0);
            return Math.max(0, lines * 16 - Math.max(1, contentHeight - 34));
        }
        return 0;
    }

    private int maxAttributeListScroll() {
        int rows = attributeViewModel.groups().stream().mapToInt(group -> 18 + group.entries().size() * 16).sum();
        return Math.max(0, rows - Math.max(1, panelHeight() - 85));
    }

    private int maxAttributeDetailScroll() {
        AttributeViewModel.DisplayEntry entry = attributeViewModel.selected().orElse(null);
        if (entry == null) return 0;
        int estimated = 105 + entry.sources().size() * 24;
        return Math.max(0, estimated - Math.max(1, panelHeight() - 85));
    }

    private net.minecraft.util.FormattedCharSequence trim(Component value, int maxWidth) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(value, maxWidth);
        return lines.isEmpty() ? net.minecraft.util.FormattedCharSequence.EMPTY : lines.getFirst();
    }

    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    @Override public boolean isPauseScreen() { return false; }
    @Override public ResourceLocation uApiTabId() { return SoulAscensionMod.id("character"); }
    @Override public int uApiTabLeft() { return panelLeft(); }
    @Override public int uApiTabTop() { return Math.max(1, panelTop() - 24); }
    @Override public dev.uapi.client.UApiTabSprites uApiTabSprites() {
        return new dev.uapi.client.UApiTabSprites(
            SoulAscensionMod.id("character/tab_normal"), SoulAscensionMod.id("character/tab_hovered"),
            SoulAscensionMod.id("character/tab_pressed"), SoulAscensionMod.id("character/tab_selected"),
            SoulAscensionMod.id("character/tab_disabled"));
    }
}
