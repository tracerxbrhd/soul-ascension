package dev.uapi.soulascension.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.uapi.client.ui.components.UILabel;
import dev.uapi.client.ui.components.UIProgressBar;
import dev.uapi.client.ui.components.UIProfileFacetPanel;
import dev.uapi.client.ui.cache.PlayerHeadCache;
import dev.uapi.client.ui.core.UIContainer;
import dev.uapi.client.ui.core.UIScreen;
import dev.uapi.client.ui.theme.UIThemes;
import dev.uapi.client.ui.theme.UITheme.ColorToken;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.client.UApiTabHost;
import dev.uapi.soulascension.config.SoulAscensionClientConfigManager;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.PlayerProgress;
import dev.uapi.soulascension.data.Stat;
import dev.uapi.soulascension.data.TitleProgress;
import dev.uapi.soulascension.network.ClientTitleCatalog;
import dev.uapi.soulascension.network.ClientTitleDefinition;
import dev.uapi.soulascension.network.ClientProgressionRules;
import dev.uapi.soulascension.network.ApplyStatAllocationPayload;
import dev.uapi.soulascension.network.PublicProfileData;
import dev.uapi.soulascension.network.SelectTitlePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

public class CharacterScreen extends UIScreen implements UApiTabHost {
    private enum Page { ATTRIBUTES, TITLES, INTEGRATION }
    private record UiTheme(int text, int mutedText, int valueText,
                           int positiveText, int accent, int divider) {}
    private static final class TitleHitbox {
        private int x, y, width, height;
        private Identifier id;
        private boolean unlocked;

        void set(int x, int y, int width, int height, Identifier id, boolean unlocked) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.id = id; this.unlocked = unlocked;
        }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
        Identifier id() { return id; }
        boolean unlocked() { return unlocked; }
    }
    private static final class AttributeHitbox {
        private int x, y, width, height;
        private Identifier id;

        void set(int x, int y, int width, int height, Identifier id) {
            this.x = x; this.y = y; this.width = width; this.height = height; this.id = id;
        }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
        Identifier id() { return id; }
    }
    private record PublicAttributeRow(Component name, Component value) {}
    private record SnapshotSkinKey(UUID playerId, String skinValue, String skinSignature) {}

    private static final Identifier ATTRIBUTES_ICON = SoulAscensionMod.id("textures/gui/icons/attributes.png");
    private static final Identifier TITLES_ICON = SoulAscensionMod.id("textures/gui/icons/title.png");
    private static final Identifier PANEL_SPRITE = SoulAscensionMod.id("character/panel");
    private static final Identifier INSET_SPRITE = SoulAscensionMod.id("character/inset");
    private static final Identifier SECTION_SPRITE = SoulAscensionMod.id("character/section");
    private static final Identifier ATTRIBUTE_SELECTED_SPRITE =
        SoulAscensionMod.id("character/attribute_selected");
    private static final Identifier STAT_PLUS_ICON = SoulAscensionMod.id("character/stat_plus");
    private static final Identifier STAT_MINUS_ICON = SoulAscensionMod.id("character/stat_minus");
    private static final Identifier[] STAT_ICONS = {
        SoulAscensionMod.id("textures/gui/stats/strength.png"),
        SoulAscensionMod.id("textures/gui/stats/endurance.png"),
        SoulAscensionMod.id("textures/gui/stats/agility.png"),
        SoulAscensionMod.id("textures/gui/stats/intelligence.png"),
        SoulAscensionMod.id("textures/gui/stats/perception.png")
    };
    private static final Component[] STAT_LABELS = java.util.Arrays.stream(Stat.values())
        .map(stat -> Component.translatable("stat.soul_ascension.short."
            + stat.name().toLowerCase(Locale.ROOT))).toArray(Component[]::new);
    private static final Component READ_ONLY_LABEL =
        Component.translatable("screen.soul_ascension.read_only");
    private static final Component INCOMPATIBLE_DATA_LABEL =
        Component.translatable("screen.soul_ascension.incompatible_data");
    private static final PlayerHeadCache<SnapshotSkinKey, Supplier<PlayerSkin>> SNAPSHOT_SKINS =
        new PlayerHeadCache<>(64);
    private static final UiTheme THEME = new UiTheme(SoulUiTheme.TEXT, SoulUiTheme.MUTED,
        SoulUiTheme.VALUE, SoulUiTheme.POSITIVE, SoulUiTheme.ACCENT, SoulUiTheme.DIVIDER);
    private static final int STAT_ROW_INSET = 6;
    private static final int STAT_ICON_INSET = 6;
    private static final int STAT_BUTTON_RIGHT_INSET = 8;
    private static final int STAT_CONTROL_GAP = 3;

    private PlayerProgress progress = PlayerProgress.initial();
    private TitleProgress titles = TitleProgress.initial();
    private final PublicProfileData publicProfile;
    private final List<PublicAttributeRow> publicAttributeRows;
    private final List<UIProfileFacetPanel> publicFacetPanels = new ArrayList<>();
    private final CharacterScreenMode mode;
    private final Supplier<PlayerSkin> snapshotSkin;
    private Page page = Page.ATTRIBUTES;
    private Identifier integrationId;
    private int scroll;
    private int attributeListScroll;
    private int attributeDetailScroll;
    private final List<TitleHitbox> titleHitboxes = new ArrayList<>();
    private final List<AttributeHitbox> attributeHitboxes = new ArrayList<>();
    private int titleHitboxCount;
    private int attributeHitboxCount;
    private final AttributeViewModel attributeViewModel = new AttributeViewModel();
    private final FlatStatButton[] increaseButtons = new FlatStatButton[Stat.values().length];
    private final FlatStatButton[] decreaseButtons = new FlatStatButton[Stat.values().length];
    private final int[] pending = new int[Stat.values().length];
    private SoulTextButton confirmButton;
    private SoulTextButton cancelButton;
    private UILabel screenTitleLabel;
    private UIProgressBar progressionBar;
    private boolean applyingAllocation;
    private Component cachedPlayerName = Component.empty();
    private Component cachedSelectedTitle = Component.empty();
    private Component cachedLevel = Component.empty();
    private Component cachedPoints = Component.empty();
    private final String[] cachedStatValues = new String[Stat.values().length];
    private long titleCatalogRevision = -1;
    private long progressionRulesRevision = -1;
    private List<CharacterIntegrationRegistry.Tab> visibleIntegrations = List.of();
    private CharacterIntegrationRegistry.Tab activeIntegration;
    private List<Component> integrationLines = List.of();
    private int integrationRefreshTicks;

    public CharacterScreen() {
        this(CharacterScreenMode.NORMAL, null);
    }

    public CharacterScreen(PublicProfileData publicProfile) {
        this(publicProfile == null ? CharacterScreenMode.NORMAL : CharacterScreenMode.PUBLIC_VIEW, publicProfile);
    }

    private CharacterScreen(CharacterScreenMode mode, PublicProfileData publicProfile) {
        super(Component.translatable(publicProfile == null
            ? "screen.soul_ascension.title" : "screen.soul_ascension.public_profile"));
        this.mode = mode;
        this.publicProfile = publicProfile;
        if (publicProfile != null) {
            GameProfile snapshotProfile = new GameProfile(publicProfile.playerId(), publicProfile.playerName());
            if (!publicProfile.skinValue().isEmpty()) {
                Property texture = publicProfile.skinSignature().isEmpty()
                    ? new Property("textures", publicProfile.skinValue())
                    : new Property("textures", publicProfile.skinValue(), publicProfile.skinSignature());
                snapshotProfile.properties().put("textures", texture);
            }
            SnapshotSkinKey skinKey = new SnapshotSkinKey(publicProfile.playerId(),
                publicProfile.skinValue(), publicProfile.skinSignature());
            snapshotSkin = SNAPSHOT_SKINS.getOrLoad(skinKey,
                ignored -> Minecraft.getInstance().getSkinManager().createLookup(snapshotProfile, false));
            progress = new PlayerProgress(publicProfile.level(), 0, 0, 0, publicProfile.strength(),
                publicProfile.endurance(), publicProfile.agility(), publicProfile.intelligence(),
                publicProfile.perception());
            publicAttributeRows = buildPublicAttributeRows(publicProfile);
        } else {
            snapshotSkin = null;
            publicAttributeRows = List.of();
        }
        rebuildCharacterPresentation();
    }

    @Override
    protected void buildUi(UIContainer root) {
        root.setTheme(UIThemes.ARCANE);
        screenTitleLabel = root.add(new UILabel(title, ColorToken.TEXT_PRIMARY));
        progressionBar = root.add(new UIProgressBar(0, Component.empty()));
        progressionBar.setVisible(!isPublicProfile());
        if (publicProfile != null) {
            for (var facet : publicProfile.facets())
                publicFacetPanels.add(root.add(new UIProfileFacetPanel(facet)));
        }
    }

    @Override
    protected void layoutUi(UIContainer root) {
        screenTitleLabel.setBounds(panelLeft() + 12, panelTop() + 12,
            Math.max(0, panelWidth() - 24), font.lineHeight);
        progressionBar.setBounds(panelLeft() + 15, panelTop() + panelHeight() - 26,
            Math.max(0, characterWidth() - 27), 12);
        layoutPublicFacetPanels();
    }

    @Override
    protected void initScreen() {
        refresh();
        visibleIntegrations = isPublicProfile() ? List.of() : CharacterIntegrationRegistry.visibleTabs();
        rebuildCharacterPresentation();
        updateProgressComponent();
        if (!isPublicProfile() && minecraft != null && minecraft.player != null) {
            attributeViewModel.forceRefresh();
            attributeViewModel.tick(minecraft.player, previewProgress());
        }
        int left = panelLeft(), top = panelTop(), panelWidth = panelWidth(), panelHeight = panelHeight();
        int characterWidth = characterWidth();
        int railSize = railButtonSize();
        int railStep = railSize + 4;
        int railX = left + panelWidth - railSize - 7;
        int railY = top + 40;
        if (!isPublicProfile()) {
            addRenderableWidget(new TextureIconButton(railX, railY, railSize, railSize, ATTRIBUTES_ICON,
                Component.translatable("screen.soul_ascension.attributes"), () -> page == Page.ATTRIBUTES,
                () -> switchPage(Page.ATTRIBUTES, null)));
            addRenderableWidget(new TextureIconButton(railX, railY + railStep, railSize, railSize, TITLES_ICON,
                Component.translatable("screen.soul_ascension.titles"), () -> page == Page.TITLES,
                () -> switchPage(Page.TITLES, null)));
            for (int i = 0; i < visibleIntegrations.size(); i++) {
                CharacterIntegrationRegistry.Tab tab = visibleIntegrations.get(i);
                addRenderableWidget(new TextureIconButton(railX, railY + railStep * (2 + i), railSize, railSize,
                    tab.icon(), tab.title(), () -> page == Page.INTEGRATION && tab.id().equals(integrationId),
                    () -> switchPage(Page.INTEGRATION, tab.id())));
            }
        }

        int rowHeight = statRowHeight();
        int statY = statStartY();
        int buttonSize = Math.max(10, Math.min(14, rowHeight - 3));
        if ((buttonSize & 1) != 0) buttonSize--;
        int rowRight = left + characterWidth - 10;
        int buttonsRight = rowRight - STAT_BUTTON_RIGHT_INSET;
        int plusX = buttonsRight - buttonSize;
        int minusX = plusX - buttonSize - STAT_CONTROL_GAP;
        for (Stat stat : isPublicProfile() ? new Stat[0] : Stat.values()) {
            int y = statY + stat.ordinal() * rowHeight;
            int buttonY = y + 4 - buttonSize / 2;
            FlatStatButton decrease = new FlatStatButton(minusX, buttonY, buttonSize, buttonSize,
                Component.literal("−"), STAT_MINUS_ICON, () -> adjustPending(stat, -1));
            FlatStatButton increase = new FlatStatButton(plusX, buttonY, buttonSize, buttonSize,
                Component.literal("+"), STAT_PLUS_ICON, () -> adjustPending(stat, 1));
            decreaseButtons[stat.ordinal()] = addRenderableWidget(decrease);
            increaseButtons[stat.ordinal()] = addRenderableWidget(increase);
        }
        if (!isPublicProfile()) initAllocationControls(left, characterWidth);
        updateStatButtons();
    }

    private void initAllocationControls(int left, int characterWidth) {
        int x = left + 13;
        int y = panelTop() + panelHeight() - 45;
        int width = characterWidth - 23;
        int gap = 8;
        int half = (width - gap) / 2;
        confirmButton = new SoulTextButton(x, y, half, 16,
            Component.translatable("screen.soul_ascension.allocation.confirm"), this::confirmPending);
        confirmButton.setTooltip(Tooltip.create(Component.translatable("screen.soul_ascension.allocation.confirm.tooltip")));
        cancelButton = new SoulTextButton(x + half + gap, y, width - half - gap, 16,
            Component.translatable("screen.soul_ascension.allocation.cancel"), this::cancelPending);
        cancelButton.setTooltip(Tooltip.create(Component.translatable("screen.soul_ascension.allocation.cancel.tooltip")));
        addRenderableWidget(confirmButton);
        addRenderableWidget(cancelButton);
    }

    private void adjustPending(Stat stat, int delta) {
        if (applyingAllocation || !progressionCompatible()) return;
        int index = stat.ordinal();
        if (delta < 0) pending[index] = Math.max(0, pending[index] - 1);
        else if (remainingPreviewPoints() > 0 && !previewAtCap(stat)) pending[index]++;
        attributeViewModel.forceRefresh();
        refreshPreviewModel();
        rebuildCharacterPresentation();
        updateStatButtons();
    }

    private void confirmPending() {
        if (!progressionCompatible() || !hasPending() || applyingAllocation) return;
        applyingAllocation = true;
        updateStatButtons();
        ClientPacketDistributor.sendToServer(new ApplyStatAllocationPayload(pending[0], pending[1], pending[2], pending[3], pending[4]));
    }

    private void cancelPending() {
        java.util.Arrays.fill(pending, 0);
        applyingAllocation = false;
        attributeViewModel.forceRefresh();
        refreshPreviewModel();
        rebuildCharacterPresentation();
        updateStatButtons();
    }

    public static void receiveAllocationResult(boolean accepted) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof CharacterScreen screen && !screen.isPublicProfile()) {
            screen.applyingAllocation = false;
            java.util.Arrays.fill(screen.pending, 0);
            screen.attributeViewModel.forceRefresh();
            screen.refreshPreviewModel();
            screen.rebuildCharacterPresentation();
            screen.updateStatButtons();
            if (minecraft.player != null) minecraft.player.sendOverlayMessage(Component.translatable(accepted
                ? "message.soul_ascension.allocation.applied" : "message.soul_ascension.allocation.rejected"));
        }
    }

    private void switchPage(Page value, Identifier integration) {
        page = value; integrationId = integration; scroll = 0; attributeListScroll = 0;
        attributeDetailScroll = 0; titleHitboxCount = 0; attributeHitboxCount = 0;
        activeIntegration = visibleIntegrations.stream()
            .filter(tab -> tab.id().equals(integrationId)).findFirst().orElse(null);
        rebuildIntegrationLines();
        setFocused(null);
        if (!isPublicProfile() && page == Page.ATTRIBUTES && minecraft != null && minecraft.player != null) {
            attributeViewModel.forceRefresh();
            attributeViewModel.tick(minecraft.player, previewProgress());
        }
    }

    private void refresh() {
        if (isPublicProfile()) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            PlayerProgress nextProgress = player.getData(SoulAscensionAttachments.PROGRESS);
            TitleProgress nextTitles = player.getData(SoulAscensionAttachments.TITLES);
            boolean progressChanged = !nextProgress.equals(progress);
            boolean titlesChanged = !nextTitles.equals(titles);
            progress = nextProgress;
            titles = nextTitles;
            if (progressChanged && !progressionCompatible()) {
                java.util.Arrays.fill(pending, 0);
                applyingAllocation = false;
            }
            if (progressChanged || titlesChanged || titleCatalogRevision != ClientTitleCatalog.revision()) {
                rebuildCharacterPresentation();
            }
            if (progressChanged) {
                attributeViewModel.forceRefresh();
                updateStatButtons();
            }
        }
    }

    private void refreshPreviewModel() {
        if (!isPublicProfile() && page == Page.ATTRIBUTES && minecraft != null && minecraft.player != null)
            attributeViewModel.tick(minecraft.player, previewProgress());
    }

    @Override protected void tickScreen() {
        refresh();
        if (titleCatalogRevision != ClientTitleCatalog.revision()) rebuildCharacterPresentation();
        if (progressionRulesRevision != ClientProgressionRules.revision()) {
            updateProgressComponent();
            updateStatButtons();
            attributeViewModel.forceRefresh();
        }
        if (page == Page.INTEGRATION && ++integrationRefreshTicks >= 20) rebuildIntegrationLines();
        if (!isPublicProfile() && page == Page.ATTRIBUTES && minecraft != null && minecraft.player != null)
            attributeViewModel.tick(minecraft.player, previewProgress());
    }

    @Override
    protected void renderScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        UiTheme theme = theme();
        int left = panelLeft(), top = panelTop(), panelWidth = panelWidth(), panelHeight = panelHeight();
        int characterWidth = characterWidth();
        drawPanel(graphics, left, top, panelWidth, panelHeight);

        renderCharacter(graphics, mouseX, mouseY, left + 7, top + 37, characterWidth - 11, panelHeight - 44);
        int contentX = contentX();
        int contentY = contentY();
        int contentWidth = contentWidth();
        int contentHeight = contentHeight();
        drawSurface(graphics, INSET_SPRITE, contentX, contentY, contentWidth, contentHeight);
        if (isPublicProfile()) {
            renderPublicAttributes(graphics, contentX, contentY, contentWidth, contentHeight);
        } else {
            switch (page) {
                case ATTRIBUTES -> renderAttributes(graphics, contentX, contentY, contentWidth, contentHeight);
                case TITLES -> renderTitles(graphics, contentX, contentY, contentWidth, contentHeight);
                case INTEGRATION -> renderIntegration(graphics, contentX, contentY, contentWidth, contentHeight);
            }
        }
    }

    private void renderCharacter(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, int height) {
        drawSurface(graphics, INSET_SPRITE, x, y, width, height);
        UiTheme theme = theme();
        int statY = statStartY();
        boolean hasTitle = !cachedSelectedTitle.getString().isEmpty();
        int headerTop = y + 7;
        int headerBottom = Math.max(headerTop + 34, statY - 22);
        int modelColumnWidth = Math.max(58, Math.min(width - 76, width * 46 / 100));
        int dividerX = x + modelColumnWidth;
        graphics.fill(dividerX, headerTop + 3, dividerX + 1, headerBottom - 3, theme.divider());
        graphics.fill(x + 8, headerBottom, x + width - 8, headerBottom + 1, theme.divider());

        int modelTop = headerTop + 3;
        int modelBottom = headerBottom - 3;
        int availableModelHeight = Math.max(20, Math.min(125, modelBottom - modelTop));
        int modelWidth = Math.max(28, Math.min(modelColumnWidth - 12, (availableModelHeight * 3) / 4));
        boolean showPlayerPreview = SoulAscensionClientConfigManager.current().showPlayerPreview();
        LivingEntity displayedPlayer = showPlayerPreview ? displayedPlayer() : null;
        if (displayedPlayer != null) {
            int modelScale = Math.max(12, Math.min(42, 12 + availableModelHeight / 4));
            int modelLeft = x + Math.max(5, (modelColumnWidth - modelWidth) / 2);
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, modelLeft, modelTop,
                modelLeft + modelWidth, modelBottom, modelScale, 0.0625F, mouseX, mouseY, displayedPlayer);
        } else if (!isPublicProfile() && minecraft != null && minecraft.player != null) {
            int faceSize = Math.max(24, Math.min(48, availableModelHeight / 2));
            PlayerFaceExtractor.extractRenderState(graphics, minecraft.player.getSkin(),
                x + (modelColumnWidth - faceSize) / 2,
                modelTop + Math.max(1, (availableModelHeight - faceSize) / 2 - 6), faceSize);
        } else if (isPublicProfile() && snapshotSkin != null) {
            int faceSize = Math.max(24, Math.min(48, availableModelHeight / 2));
            PlayerFaceExtractor.extractRenderState(graphics, snapshotSkin.get(),
                x + (modelColumnWidth - faceSize) / 2,
                modelTop + Math.max(1, (availableModelHeight - faceSize) / 2 - 6), faceSize);
            boolean online = minecraft != null && minecraft.getConnection() != null
                && minecraft.getConnection().getPlayerInfo(publicProfile.playerId()) != null;
            Component status = Component.translatable(online
                ? "screen.soul_ascension.public_profile.out_of_range"
                : "screen.soul_ascension.public_profile.offline");
            graphics.centeredText(font, trim(status, modelColumnWidth - 12), x + modelColumnWidth / 2,
                modelBottom - 10, theme.mutedText());
        }

        int infoX = dividerX + 7;
        int infoWidth = Math.max(36, x + width - 8 - infoX);
        int lineCount = hasTitle ? 3 : 2;
        int infoY = headerTop + Math.max(4, (headerBottom - headerTop - lineCount * 13) / 2);
        graphics.centeredText(font, trim(cachedPlayerName, infoWidth), infoX + infoWidth / 2, infoY, theme.text());
        graphics.centeredText(font, trim(cachedLevel, infoWidth),
            infoX + infoWidth / 2, infoY + 13, theme.text());
        if (hasTitle) graphics.centeredText(font, trim(cachedSelectedTitle, infoWidth),
            infoX + infoWidth / 2, infoY + 26, theme.accent());

        int rowHeight = statRowHeight();
        if (!isPublicProfile()) {
            graphics.text(font,
                trim(cachedPoints, width - 18),
                x + 9, statY - Math.min(17, rowHeight),
                !progressionCompatible() ? 0xFFFF7777
                    : remainingPreviewPoints() > 0 || hasPending() ? theme.accent() : theme.mutedText(), false);
        }
        for (Stat stat : Stat.values()) {
            int rowY = statY + stat.ordinal() * rowHeight;
            int rowX = x + STAT_ROW_INSET;
            int rowVisualHeight = Math.min(20, Math.max(11, rowHeight - 2));
            int rowTop = rowY + 4 - rowVisualHeight / 2;
            drawSurface(graphics, SECTION_SPRITE, rowX, rowTop, width - STAT_ROW_INSET * 2, rowVisualHeight);
            Component name = STAT_LABELS[stat.ordinal()];
            int iconSize = Math.min(16, Math.max(9, rowVisualHeight - 4));
            int iconX = rowX + STAT_ICON_INSET;
            int iconY = rowTop + (rowVisualHeight - iconSize) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, STAT_ICONS[stat.ordinal()], iconX, iconY, 0, 0, iconSize, iconSize, 16, 16);
            int labelX = iconX + iconSize + 5;
            graphics.text(font, trim(name, Math.max(28, width - 105)), labelX, rowY, theme.text(), false);
            graphics.text(font, cachedStatValues[stat.ordinal()], x + width - 70, rowY,
                !isPublicProfile() && pending[stat.ordinal()] > 0 ? theme.accent() : theme.positiveText(), false);
            if (!isPublicProfile() && stat == Stat.INTELLIGENCE && mouseX >= rowX
                && mouseX < rowX + width - STAT_ROW_INSET * 2 && mouseY >= rowTop
                && mouseY < rowTop + rowVisualHeight) {
                graphics.setTooltipForNextFrame(intelligenceTooltip(), mouseX, mouseY);
            }
        }
        if (isPublicProfile()) {
            graphics.centeredText(font, READ_ONLY_LABEL,
                x + width / 2, y + height - 15, theme.mutedText());
        }
    }

    private void renderPublicAttributes(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        UiTheme theme = theme();
        graphics.text(font, Component.translatable("screen.soul_ascension.public_attributes"),
            x + 10, y + 9, theme.text(), false);
        graphics.fill(x + 10, y + 22, x + width - 10, y + 23, theme.divider());
        int rowY = y + 30 - scroll;
        int clipTop = y + 25;
        int clipBottom = publicFacetPanels.isEmpty() ? y + height - 4 : publicFacetRegionTop() - 4;
        if (clipBottom <= clipTop) return;
        graphics.enableScissor(x + 4, clipTop, x + width - 4, clipBottom);
        for (PublicAttributeRow attribute : publicAttributeRows) {
            if (rowY + 20 < clipTop) {
                rowY += 23;
                continue;
            }
            if (rowY > clipBottom) break;
            drawSurface(graphics, SECTION_SPRITE, x + 9, rowY, width - 18, 20);
            graphics.text(font, trim(attribute.name(), Math.max(30, width - 110)), x + 15, rowY + 6,
                theme.text(), false);
            graphics.text(font, attribute.value(), x + width - 15 - font.width(attribute.value()), rowY + 6,
                theme.valueText(), false);
            rowY += 23;
        }
        graphics.disableScissor();
    }

    private static List<PublicAttributeRow> buildPublicAttributeRows(PublicProfileData profile) {
        List<PublicAttributeRow> rows = new ArrayList<>();
        for (PublicProfileData.PublicAttribute attribute : profile.attributes()) {
            var holder = BuiltInRegistries.ATTRIBUTE.get(attribute.id()).orElse(null);
            if (holder == null) continue;
            rows.add(new PublicAttributeRow(Component.translatable(holder.value().getDescriptionId()),
                DynamicAttributeView.formatValue(attribute.id(), holder.value(), attribute.value())));
        }
        return List.copyOf(rows);
    }

    private void renderAttributes(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        UiTheme theme = theme();
        graphics.text(font, Component.translatable("screen.soul_ascension.attributes"), x + 10, y + 9, theme.text(), false);
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

    private void renderAttributeList(GuiGraphicsExtractor graphics, int x, int y, int width, int height, UiTheme theme) {
        attributeHitboxCount = 0;
        int lineY = y + 5 - attributeListScroll;
        int clipTop = y + 2;
        int clipBottom = y + height - 2;
        graphics.enableScissor(x + 2, clipTop, x + width - 2, clipBottom);
        for (AttributeViewModel.Group group : attributeViewModel.groups()) {
            int headingY = lineY;
            if (headingY + 14 >= clipTop && headingY <= clipBottom) {
                graphics.fill(x + 5, headingY + 5, x + width / 2 - 18, headingY + 6, theme.divider());
                graphics.fill(x + width / 2 + 18, headingY + 5, x + width - 5, headingY + 6, theme.divider());
                graphics.centeredText(font, trim(group.category().title(), Math.max(20, width - 42)),
                    x + width / 2, headingY + 1, theme.accent());
            }
            lineY += 15;
            for (AttributeViewModel.DisplayEntry entry : group.entries()) {
                if (lineY + 15 < clipTop) {
                    lineY += 16;
                    continue;
                }
                if (lineY > clipBottom) break;
                boolean selected = entry.id().equals(attributeViewModel.selectedId());
                if (selected) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ATTRIBUTE_SELECTED_SPRITE,
                    x + 4, lineY, width - 8, 15);
                Component displayedValue = entry.hasPreview()
                    ? Component.empty().append(entry.formattedCurrent()).append(" → ").append(entry.formattedPreview())
                    : entry.formattedCurrent();
                int valueWidth = font.width(displayedValue);
                int valueX = x + width - 7 - valueWidth;
                graphics.text(font, trim(entry.name(), Math.max(18, valueX - x - 12)),
                    x + 7, lineY + 3, selected ? theme.accent() : theme.text(), false);
                graphics.text(font, displayedValue, valueX, lineY + 3,
                    entry.hasPreview() ? theme.accent() : theme.valueText(), false);
                recordAttributeHitbox(x + 4, lineY, width - 8, 15, entry.id());
                lineY += 16;
            }
            lineY += 3;
        }
        graphics.disableScissor();
    }

    private void renderAttributeDetail(GuiGraphicsExtractor graphics, int x, int y, int width, int height, UiTheme theme) {
        AttributeViewModel.DisplayEntry entry = attributeViewModel.selected().orElse(null);
        if (entry == null) {
            graphics.centeredText(font, Component.translatable("screen.soul_ascension.attribute.none"),
                x + width / 2, y + height / 2, theme.mutedText());
            return;
        }
        int lineY = y + 7 - attributeDetailScroll;
        int textX = x + 8;
        int textWidth = Math.max(20, width - 16);
        int clipTop = y + 2;
        int clipBottom = y + height - 2;
        graphics.enableScissor(x + 2, clipTop, x + width - 2, clipBottom);
        graphics.text(font, trim(entry.name(), textWidth), textX, lineY, theme.accent(), false);
        lineY += 14;
        boolean showId = SoulAscensionClientConfigManager.current().showAttributeNamespaces()
            || minecraft.options.advancedItemTooltips;
        if (showId) {
            graphics.text(font, trim(Component.literal(entry.id().toString()), textWidth),
                textX, lineY, theme.mutedText(), false);
            lineY += 13;
        }
        lineY = drawDetailValue(graphics, textX, lineY, textWidth,
            Component.translatable("screen.soul_ascension.attribute.current"), entry.formattedCurrent(), theme);
        if (entry.hasPreview()) lineY = drawDetailValue(graphics, textX, lineY, textWidth,
            Component.translatable("screen.soul_ascension.attribute.preview"), entry.formattedPreview(), theme, true);
        lineY = drawDetailValue(graphics, textX, lineY, textWidth,
            Component.translatable("screen.soul_ascension.attribute.base"), entry.formattedBase(), theme);
        lineY = drawDetailValue(graphics, textX, lineY, textWidth,
            Component.translatable("screen.soul_ascension.attribute.category"), entry.category().title(), theme);
        lineY += 3;
        for (net.minecraft.util.FormattedCharSequence part : font.split(entry.description(), textWidth)) {
            if (lineY + 10 >= clipTop && lineY <= clipBottom)
                graphics.text(font, part, textX, lineY, theme.text(), false);
            lineY += 11;
        }
        lineY += 7;
        graphics.text(font, Component.translatable("screen.soul_ascension.attribute.sources"),
            textX, lineY, theme.accent(), false);
        lineY += 14;
        graphics.text(font, Component.translatable("screen.soul_ascension.source.base"),
            textX, lineY, theme.text(), false);
        graphics.text(font, entry.formattedBase(), x + width - 8 - font.width(entry.formattedBase()),
            lineY, theme.valueText(), false);
        lineY += 16;
        for (AttributeViewModel.SourceEntry source : entry.sources()) {
            int rowHeight = showId || !source.icon().isEmpty() ? 24 : 16;
            if (lineY + rowHeight < clipTop) {
                lineY += rowHeight;
                continue;
            }
            if (lineY > clipBottom) break;
            int sourceX = textX;
            if (!source.icon().isEmpty()) {
                graphics.item(source.icon(), sourceX, lineY - 2);
                sourceX += 19;
            }
            Component amount = Component.empty().append(source.formattedAmount());
            int amountX = x + width - 8 - font.width(amount);
            graphics.text(font, trim(source.name(), Math.max(20, amountX - sourceX - 4)),
                sourceX, lineY, theme.text(), false);
            graphics.text(font, amount, amountX, lineY,
                source.amount() >= 0 ? theme.positiveText() : 0xFFFF7777, false);
            if (showId) graphics.text(font, trim(Component.literal(source.modifierId().toString()), textWidth - 7),
                sourceX, lineY + 11, theme.mutedText(), false);
            lineY += rowHeight;
        }
        graphics.disableScissor();
    }

    private int drawDetailValue(GuiGraphicsExtractor graphics, int x, int y, int width, Component label,
                                 Component value, UiTheme theme) {
        return drawDetailValue(graphics, x, y, width, label, value, theme, false);
    }

    private int drawDetailValue(GuiGraphicsExtractor graphics, int x, int y, int width, Component label,
                                Component value, UiTheme theme, boolean preview) {
        graphics.text(font, label, x, y, theme.mutedText(), false);
        net.minecraft.util.FormattedCharSequence trimmed = trim(value, Math.max(16, width / 2));
        graphics.text(font, trimmed, x + width - font.width(trimmed), y,
            preview ? theme.accent() : theme.valueText(), false);
        return y + 13;
    }

    private void renderTitles(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        UiTheme theme = theme();
        graphics.text(font, Component.translatable("screen.soul_ascension.titles"), x + 10, y + 9, theme.text(), false);
        graphics.fill(x + 10, y + 22, x + width - 10, y + 23, theme.divider());
        titleHitboxCount = 0;
        int cardY = y + 30 - scroll;
        int clipTop = y + 25;
        int clipBottom = y + height - 4;
        graphics.enableScissor(x + 4, clipTop, x + width - 4, clipBottom);
        for (ClientTitleDefinition definition : ClientTitleCatalog.all()) {
            boolean unlocked = titles.unlocked().contains(definition.id());
            if (definition.hidden() && !unlocked) continue;
            boolean selected = titles.activeTitle().equals(definition.id());
            int cardHeight = 39;
            if (cardY + cardHeight < clipTop) {
                cardY += cardHeight + 5;
                continue;
            }
            if (cardY > clipBottom) break;
            drawSurface(graphics, SECTION_SPRITE, x + 9, cardY, width - 18, cardHeight);
            if (selected) graphics.outline(x + 10, cardY + 1, width - 20, cardHeight - 2, theme.accent());
            graphics.blit(RenderPipelines.GUI_TEXTURED, definition.icon(), x + 14, cardY + 4, 0, 0, 30, 30, 32, 32);
            Component name = ClientTitleCatalog.name(definition.id());
            graphics.text(font, name, x + 49, cardY + 6, unlocked ? theme.accent() : theme.mutedText(), false);
            Component description = ClientTitleCatalog.description(definition.id());
            graphics.text(font, trim(description, Math.max(30, width - 65)),
                x + 49, cardY + 20, unlocked ? theme.text() : theme.mutedText(), false);
            if (!unlocked) graphics.text(font, Component.translatable("screen.soul_ascension.locked"),
                x + width - 55, cardY + 6, 0xFFFF7777, false);
            recordTitleHitbox(x + 9, cardY, width - 18, cardHeight, definition.id(), unlocked);
            cardY += cardHeight + 5;
        }
        graphics.disableScissor();
    }

    private void renderIntegration(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        UiTheme theme = theme();
        Component heading = activeIntegration == null
            ? Component.translatable("screen.soul_ascension.integration") : activeIntegration.title();
        graphics.text(font, heading, x + 10, y + 9, theme.text(), false);
        graphics.fill(x + 10, y + 22, x + width - 10, y + 23, theme.divider());
        if (activeIntegration == null || minecraft == null || minecraft.player == null) return;
        int lineY = y + 34 - scroll;
        int clipTop = y + 25;
        int clipBottom = y + height - 4;
        graphics.enableScissor(x + 4, clipTop, x + width - 4, clipBottom);
        for (Component line : integrationLines) {
            for (net.minecraft.util.FormattedCharSequence part : font.split(line, width - 24)) {
                if (lineY + 10 >= clipTop && lineY <= clipBottom)
                    graphics.text(font, part, x + 12, lineY, theme.text(), false);
                lineY += 13;
                if (lineY > clipBottom + 16) break;
            }
            lineY += 3;
            if (lineY > clipBottom + 16) break;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubledClick) {
        if (super.mouseClicked(event, doubledClick)) return true;
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (isPublicProfile()) return false;
        int contentX = contentX();
        int contentY = contentY();
        int contentWidth = contentWidth();
        int contentHeight = contentHeight();
        if (page == Page.ATTRIBUTES && button == 0) {
            for (int index = 0; index < attributeHitboxCount; index++) {
                AttributeHitbox hitbox = attributeHitboxes.get(index);
                if (!hitbox.contains(mouseX, mouseY)) continue;
                attributeViewModel.select(hitbox.id());
                attributeDetailScroll = 0;
                return true;
            }
        }
        if (page == Page.TITLES && button == 0 && mouseX >= contentX && mouseX < contentX + contentWidth
            && mouseY >= contentY + 25 && mouseY < contentY + contentHeight) {
            for (int index = 0; index < titleHitboxCount; index++) {
                TitleHitbox hitbox = titleHitboxes.get(index);
                if (hitbox.unlocked() && hitbox.contains(mouseX, mouseY)) {
                    Identifier selected = titles.activeTitle().equals(hitbox.id())
                        ? TitleProgress.NONE : hitbox.id();
                    ClientPacketDistributor.sendToServer(new SelectTitlePayload(selected));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isPublicProfile()) {
            scroll = Math.max(0, Math.min(maxPublicAttributeScroll(),
                scroll - (int) Math.signum(scrollY) * 18));
            return true;
        }
        if (page == Page.ATTRIBUTES) {
            int contentX = contentX();
            int contentWidth = contentWidth();
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

    private void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL_SPRITE, x, y, width, height);
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
        int footer = isPublicProfile() ? 28 : 51;
        return panelTop() + panelHeight() - footer - Stat.values().length * statRowHeight();
    }
    private int railButtonSize() { return panelHeight() < 240 ? 18 : 24; }
    private int railReserve() { return railButtonSize() + 15; }
    private int contentReserve() { return isPublicProfile() ? 11 : railReserve(); }
    private int contentX() { return panelLeft() + characterWidth() + 3; }
    private int contentY() { return panelTop() + 37; }
    private int contentWidth() { return panelWidth() - characterWidth() - contentReserve(); }
    private int contentHeight() {
        return panelHeight() - 44;
    }
    private int attributeListWidth(int contentWidth) {
        int available = Math.max(40, contentWidth - 25);
        return Math.max(40, Math.min(190, available * 2 / 5));
    }

    private UiTheme theme() {
        return THEME;
    }

    private void drawSurface(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    private void updateProgressComponent() {
        if (progressionBar == null) return;
        progressionRulesRevision = ClientProgressionRules.revision();
        if (isPublicProfile()) return;
        boolean maximumReached = ClientProgressionRules.maxLevel() > 0
            && progress.level() >= ClientProgressionRules.maxLevel();
        double required = progress.requiredDamage() > 0 ? progress.requiredDamage() : 1.0;
        double ratio = maximumReached ? 1.0
            : Math.max(0.0, Math.min(1.0, progress.damageProgress() / required));
        Component text = maximumReached ? Component.translatable("screen.soul_ascension.max_level")
            : Component.translatable("screen.soul_ascension.progress_values",
                formatProgress(progress.damageProgress()), formatProgress(required));
        progressionBar.setProgress(ratio);
        progressionBar.setLabel(text);
    }

    private String formatProgress(double value) {
        return value >= 100 || Math.rint(value) == value ? String.format(Locale.ROOT, "%.0f", value)
            : String.format(Locale.ROOT, "%.1f", value);
    }

    private void updateStatButtons() {
        if (isPublicProfile() || minecraft == null || minecraft.player == null) return;
        boolean compatible = progressionCompatible();
        for (Stat stat : Stat.values()) {
            FlatStatButton increase = increaseButtons[stat.ordinal()];
            FlatStatButton decrease = decreaseButtons[stat.ordinal()];
            if (increase != null) {
                increase.active = compatible && !applyingAllocation
                    && remainingPreviewPoints() > 0 && !previewAtCap(stat);
                increase.setTooltip(Tooltip.create(compatible
                    ? adjustmentTooltip(stat, 1) : INCOMPATIBLE_DATA_LABEL));
            }
            if (decrease != null) {
                decrease.active = compatible && !applyingAllocation && pending[stat.ordinal()] > 0;
                decrease.setTooltip(Tooltip.create(compatible ? Component.translatable(
                    "screen.soul_ascension.allocation.decrease_preview") : INCOMPATIBLE_DATA_LABEL));
            }
        }
        boolean enabled = compatible && hasPending() && !applyingAllocation;
        if (confirmButton != null) confirmButton.active = enabled;
        if (cancelButton != null) cancelButton.active = enabled;
    }

    private Component adjustmentTooltip(Stat stat, int delta) {
        if (delta > 0 && remainingPreviewPoints() <= 0)
            return Component.translatable("screen.soul_ascension.no_free_points");
        int maximum = ClientProgressionRules.maxPointsPerStat();
        if (delta > 0 && ClientProgressionRules.limitStatPoints()
            && maximum > 0 && previewProgress().stat(stat) >= maximum)
            return Component.translatable("screen.soul_ascension.stat_at_max");
        PlayerProgress preview = previewProgress();
        List<StatAttributePreview.Change> changes = StatAttributePreview.change(minecraft.player, preview, stat, delta);
        if (changes.isEmpty() && stat != Stat.INTELLIGENCE)
            return Component.translatable("screen.soul_ascension.no_attribute_changes");
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
        if (stat == Stat.INTELLIGENCE) {
            if (!changes.isEmpty()) result.append("\n");
            result.append(intelligenceTooltip().copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return result;
    }

    private boolean hasPending() {
        return pendingTotal() > 0;
    }

    private Component intelligenceTooltip() {
        Component experience = Component.translatable("tooltip.soul_ascension.intelligence.experience",
            formatPercent(ClientProgressionRules.intelligenceExperienceBonusPerPoint() * 100.0));
        if (ClientProgressionRules.intelligenceAffectsVanillaExperience()
            && ClientProgressionRules.intelligenceAffectsSoulProgression()) {
            return Component.empty().append(experience).append("\n")
                .append(Component.translatable("tooltip.soul_ascension.intelligence.soul_progress"));
        }
        if (ClientProgressionRules.intelligenceAffectsVanillaExperience()) return experience;
        if (ClientProgressionRules.intelligenceAffectsSoulProgression()) {
            return Component.empty().append(experience).append("\n")
                .append(Component.translatable("tooltip.soul_ascension.intelligence.soul_progress"));
        }
        return Component.translatable("tooltip.soul_ascension.intelligence.disabled");
    }

    private static String formatPercent(double value) {
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        return formatted.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private int pendingTotal() {
        int total = 0;
        for (int value : pending) total += value;
        return total;
    }

    private int remainingPreviewPoints() {
        return Math.max(0, progress.unspentPoints() - pendingTotal());
    }

    private boolean previewAtCap(Stat stat) {
        int maximum = ClientProgressionRules.maxPointsPerStat();
        return ClientProgressionRules.limitStatPoints() && maximum > 0
            && progress.stat(stat) + pending[stat.ordinal()] >= maximum;
    }

    private PlayerProgress previewProgress() {
        if (isPublicProfile() || !hasPending()) return progress;
        return new PlayerProgress(progress.level(), progress.damageProgress(), progress.requiredDamage(),
            remainingPreviewPoints(), progress.strength() + pending[0], progress.endurance() + pending[1],
            progress.agility() + pending[2], progress.intelligence() + pending[3],
            progress.perception() + pending[4]);
    }

    private int maxScroll() {
        int contentHeight = contentHeight();
        if (page == Page.TITLES) {
            long count = ClientTitleCatalog.all().stream()
                .filter(value -> !value.hidden() || titles.unlocked().contains(value.id())).count();
            return Math.max(0, (int) count * 44 - Math.max(1, contentHeight - 32));
        }
        if (page == Page.INTEGRATION && minecraft != null && minecraft.player != null) {
            return Math.max(0, integrationLines.size() * 16 - Math.max(1, contentHeight - 34));
        }
        return 0;
    }

    private int maxPublicAttributeScroll() {
        int clipBottom = publicFacetPanels.isEmpty()
            ? contentY() + contentHeight() - 4 : publicFacetRegionTop() - 4;
        return Math.max(0, publicAttributeRows.size() * 23
            - Math.max(1, clipBottom - (contentY() + 30)));
    }

    private int publicFacetRegionTop() {
        if (publicFacetPanels.isEmpty()) return contentY() + contentHeight();
        int desired = publicFacetPanels.stream().mapToInt(UIProfileFacetPanel::suggestedHeight).sum()
            + Math.max(0, publicFacetPanels.size() - 1) * 4 + 8;
        int maximum = Math.max(40, contentHeight() * 3 / 5);
        int regionHeight = Math.min(desired, Math.min(contentHeight() - 28, maximum));
        return contentY() + contentHeight() - Math.max(20, regionHeight);
    }

    private void layoutPublicFacetPanels() {
        if (publicFacetPanels.isEmpty()) return;
        int x = contentX() + 9;
        int width = Math.max(0, contentWidth() - 18);
        int top = publicFacetRegionTop() + 4;
        int bottom = contentY() + contentHeight() - 4;
        int available = Math.max(0, bottom - top);
        int gap = 4;
        int maximumVisible = Math.max(1, (available + gap) / (18 + gap));
        int visibleCount = Math.min(publicFacetPanels.size(), maximumVisible);
        int desired = publicFacetPanels.subList(0, visibleCount).stream()
            .mapToInt(UIProfileFacetPanel::suggestedHeight).sum() + Math.max(0, visibleCount - 1) * gap;
        int compactHeight = visibleCount == 0 ? 0
            : Math.max(18, (available - Math.max(0, visibleCount - 1) * gap) / visibleCount);
        int y = top;
        for (int index = 0; index < publicFacetPanels.size(); index++) {
            UIProfileFacetPanel panel = publicFacetPanels.get(index);
            boolean visible = index < visibleCount;
            panel.setVisible(visible);
            if (!visible) continue;
            int height = desired <= available ? panel.suggestedHeight() : compactHeight;
            height = Math.min(height, Math.max(0, bottom - y));
            panel.setBounds(x, y, width, height);
            y += height + gap;
        }
    }

    private int maxAttributeListScroll() {
        int rows = attributeViewModel.groups().stream().mapToInt(group -> 18 + group.entries().size() * 16).sum();
        return Math.max(0, rows - Math.max(1, contentHeight() - 41));
    }

    private int maxAttributeDetailScroll() {
        AttributeViewModel.DisplayEntry entry = attributeViewModel.selected().orElse(null);
        if (entry == null) return 0;
        int estimated = 105 + (entry.hasPreview() ? 13 : 0) + entry.sources().size() * 24;
        return Math.max(0, estimated - Math.max(1, contentHeight() - 41));
    }

    private net.minecraft.util.FormattedCharSequence trim(Component value, int maxWidth) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(value, maxWidth);
        return lines.isEmpty() ? net.minecraft.util.FormattedCharSequence.EMPTY : lines.getFirst();
    }

    private boolean isPublicProfile() {
        return mode == CharacterScreenMode.PUBLIC_VIEW;
    }

    private boolean progressionCompatible() {
        return isPublicProfile() || progress.isUsable();
    }

    private void recordTitleHitbox(int x, int y, int width, int height,
                                   Identifier id, boolean unlocked) {
        TitleHitbox hitbox;
        if (titleHitboxCount < titleHitboxes.size()) hitbox = titleHitboxes.get(titleHitboxCount);
        else {
            hitbox = new TitleHitbox();
            titleHitboxes.add(hitbox);
        }
        titleHitboxCount++;
        hitbox.set(x, y, width, height, id, unlocked);
    }

    private void recordAttributeHitbox(int x, int y, int width, int height, Identifier id) {
        AttributeHitbox hitbox;
        if (attributeHitboxCount < attributeHitboxes.size()) hitbox = attributeHitboxes.get(attributeHitboxCount);
        else {
            hitbox = new AttributeHitbox();
            attributeHitboxes.add(hitbox);
        }
        attributeHitboxCount++;
        hitbox.set(x, y, width, height, id);
    }

    private void rebuildCharacterPresentation() {
        Minecraft client = Minecraft.getInstance();
        cachedPlayerName = isPublicProfile() ? Component.literal(publicProfile.playerName())
            : client.player == null ? Component.empty()
            : Component.literal(client.player.getGameProfile().name());
        Identifier titleId = isPublicProfile() ? publicProfile.activeTitle() : titles.activeTitle();
        cachedSelectedTitle = ClientTitleCatalog.name(titleId);
        cachedLevel = Component.translatable("screen.soul_ascension.level", progress.level());
        cachedPoints = isPublicProfile() ? Component.empty() : !progressionCompatible()
            ? INCOMPATIBLE_DATA_LABEL : hasPending()
            ? Component.translatable("screen.soul_ascension.allocation.points_preview",
                remainingPreviewPoints(), pendingTotal())
            : Component.translatable("screen.soul_ascension.points", progress.unspentPoints());
        for (Stat stat : Stat.values()) {
            int shownValue = progress.stat(stat) + (isPublicProfile() ? 0 : pending[stat.ordinal()]);
            cachedStatValues[stat.ordinal()] = Integer.toString(shownValue);
        }
        titleCatalogRevision = ClientTitleCatalog.revision();
        updateProgressComponent();
    }

    private void rebuildIntegrationLines() {
        integrationRefreshTicks = 0;
        if (activeIntegration == null || minecraft == null || minecraft.player == null) {
            integrationLines = List.of();
            return;
        }
        integrationLines = List.copyOf(activeIntegration.lines().apply(minecraft.player));
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private LivingEntity displayedPlayer() {
        if (minecraft == null) return null;
        if (!isPublicProfile()) return minecraft.player;
        return minecraft.level == null ? null : minecraft.level.getPlayerByUUID(publicProfile.playerId());
    }

    @Override public Identifier uApiTabId() { return SoulAscensionMod.id("character"); }
    @Override public int uApiTabLeft() { return panelLeft(); }
    @Override public int uApiTabTop() { return Math.max(1, panelTop() - 24); }
    @Override public dev.uapi.client.UApiTabSprites uApiTabSprites() {
        return new dev.uapi.client.UApiTabSprites(
            SoulAscensionMod.id("character/tab_normal"), SoulAscensionMod.id("character/tab_hovered"),
            SoulAscensionMod.id("character/tab_pressed"), SoulAscensionMod.id("character/tab_selected"),
            SoulAscensionMod.id("character/tab_disabled"));
    }
}
