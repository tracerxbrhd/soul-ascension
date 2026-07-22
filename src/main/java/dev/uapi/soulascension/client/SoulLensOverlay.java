package dev.uapi.soulascension.client;

import dev.uapi.client.hud.HudAnchor;
import dev.uapi.client.hud.HudElement;
import dev.uapi.client.hud.HudElementRegistration;
import dev.uapi.client.hud.HudPlacement;
import dev.uapi.client.hud.HudRenderContext;
import dev.uapi.client.hud.HudTickContext;
import dev.uapi.client.hud.UApiHud;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.network.ClientProgressionRules;
import dev.uapi.soulascension.network.ClientTitleCatalog;
import dev.uapi.soulascension.network.PublicProfileData;
import dev.uapi.soulascension.network.SoulLensProfileData;
import dev.uapi.soulascension.network.SoulLensProfilePayload;
import dev.uapi.soulascension.network.SoulLensRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.UUID;

public final class SoulLensOverlay {
    private record CachedAttributeRow(Component name, Component value) {}
    private static final Identifier PANEL = SoulAscensionMod.id("character/panel");
    private static final Identifier INSET = SoulAscensionMod.id("character/inset");
    private static final int NO_TARGET = -2;
    private static final int LOADING = -1;
    private static final int STAT_ROW_HEIGHT = 17;
    private static final int STAT_COUNT = 5;
    private static final int ACTIVE_CONTENT_TOP = 43;
    private static final int ATTRIBUTE_CLIP_OFFSET = 151;
    private static final Component OVERLAY_TITLE =
        Component.translatable("overlay.soul_ascension.soul_lens.title");
    private static final Component AIM_AT_PLAYER =
        Component.translatable("overlay.soul_ascension.soul_lens.aim_at_player");
    private static final Component LOADING_MESSAGE =
        Component.translatable("overlay.soul_ascension.soul_lens.loading");
    private static final Component OUT_OF_RANGE_MESSAGE =
        Component.translatable("overlay.soul_ascension.soul_lens.out_of_range");
    private static final Component PRIVATE_MESSAGE =
        Component.translatable("overlay.soul_ascension.soul_lens.private");
    private static final Component PUBLIC_ATTRIBUTES =
        Component.translatable("overlay.soul_ascension.soul_lens.public_attributes");
    private static final Component[] STAT_NAMES = {
        Component.translatable("stat.soul_ascension.short.strength"),
        Component.translatable("stat.soul_ascension.short.endurance"),
        Component.translatable("stat.soul_ascension.short.agility"),
        Component.translatable("stat.soul_ascension.short.intelligence"),
        Component.translatable("stat.soul_ascension.short.perception")
    };
    private static UUID targetId;
    private static int status = NO_TARGET;
    private static SoulLensProfileData profile;
    private static List<CachedAttributeRow> cachedAttributeRows = List.of();
    private static Component cachedTitle = Component.empty();
    private static Component cachedPlayerName = Component.empty();
    private static Component cachedLevel = Component.empty();
    private static final String[] cachedStatValues = new String[STAT_COUNT];
    private static int ticksSinceRequest;
    private static int targetScanTicks;
    private static int scroll;
    private static long observationId;
    private static long titleCatalogRevision = -1;
    private static HudElementRegistration hudRegistration;
    private static final HudElement HUD_ELEMENT = new HudElement() {
        @Override public Identifier id() { return SoulAscensionMod.id("soul_lens_overlay"); }
        @Override public int width() { return overlayWidth(Minecraft.getInstance().getWindow().getGuiScaledWidth()); }
        @Override public int height() { return overlayHeight(Minecraft.getInstance().getWindow().getGuiScaledHeight()); }
        @Override public HudPlacement defaultPlacement() {
            return new HudPlacement(HudAnchor.CENTER_LEFT, 6, 0, 1.0F, true, 100);
        }
        @Override public boolean visible(HudTickContext context) { return shouldRender(context.minecraft()); }
        @Override public void tick(HudTickContext context) { SoulLensOverlay.tick(context.minecraft()); }
        @Override public void render(HudRenderContext context) { SoulLensOverlay.render(context); }
    };

    private SoulLensOverlay() {}

    public static synchronized void registerHud() {
        if (hudRegistration == null || !hudRegistration.isActive())
            hudRegistration = UApiHud.register(HUD_ELEMENT);
    }

    private static void tick(Minecraft minecraft) {
        if (titleCatalogRevision != ClientTitleCatalog.revision()) rebuildCache();
        LocalPlayer player = minecraft.player;
        if (!isUsing(player) || minecraft.level == null || minecraft.gui.screen() != null) {
            clear();
            return;
        }
        if (++targetScanTicks >= 2) {
            targetScanTicks = 0;
            Player target = findTarget(minecraft, ClientProgressionRules.soulLensRange());
            if (target == null) {
                if (targetId != null) observationId++;
                targetId = null;
                status = NO_TARGET;
                profile = null;
                cachedAttributeRows = List.of();
                cachedTitle = Component.empty();
                cachedPlayerName = Component.empty();
                cachedLevel = Component.empty();
                scroll = 0;
                return;
            }
            boolean changed = !target.getUUID().equals(targetId);
            if (changed) {
                observationId++;
                targetId = target.getUUID();
                status = LOADING;
                profile = null;
                cachedAttributeRows = List.of();
                cachedTitle = Component.empty();
                scroll = 0;
                ticksSinceRequest = ClientProgressionRules.soulLensUpdateInterval();
            }
        }
        if (targetId != null && ++ticksSinceRequest >= ClientProgressionRules.soulLensUpdateInterval()) {
            ticksSinceRequest = 0;
            ClientPacketDistributor.sendToServer(new SoulLensRequestPayload(targetId, observationId));
        }
    }

    public static void receive(SoulLensProfilePayload payload) {
        if (!payload.targetId().equals(targetId) || payload.observationId() != observationId) return;
        status = payload.status();
        profile = payload.profile();
        rebuildCache();
        clampScroll();
    }

    private static void rebuildCache() {
        titleCatalogRevision = ClientTitleCatalog.revision();
        if (profile == null) {
            cachedAttributeRows = List.of();
            cachedTitle = Component.empty();
            cachedPlayerName = Component.empty();
            cachedLevel = Component.empty();
            return;
        }
        cachedTitle = ClientTitleCatalog.name(profile.activeTitle());
        cachedPlayerName = Component.literal(profile.playerName());
        cachedLevel = Component.translatable("screen.soul_ascension.level", profile.level());
        for (int index = 0; index < cachedStatValues.length; index++)
            cachedStatValues[index] = Integer.toString(statValue(index));
        java.util.ArrayList<CachedAttributeRow> rows = new java.util.ArrayList<>();
        for (PublicProfileData.PublicAttribute attribute : profile.attributes()) {
            var holder = BuiltInRegistries.ATTRIBUTE.get(attribute.id()).orElse(null);
            if (holder == null) continue;
            rows.add(new CachedAttributeRow(Component.translatable(holder.value().getDescriptionId()),
                DynamicAttributeView.formatValue(attribute.id(), holder.value(), attribute.value())));
        }
        cachedAttributeRows = List.copyOf(rows);
    }

    public static boolean handleScroll(double delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null || !isUsing(minecraft.player)
            || !ClientProgressionRules.soulLensBlockHotbarScroll()) return false;
        if (profile != null)
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * 18));
        return true;
    }

    private static void render(HudRenderContext context) {
        Minecraft minecraft = context.minecraft();
        GuiGraphicsExtractor graphics = context.graphics();
        if (!shouldRender(minecraft)) return;

        boolean active = status == SoulLensProfilePayload.VISIBLE && profile != null;
        double opacity = active ? ClientProgressionRules.soulLensActiveOverlayOpacity()
            : ClientProgressionRules.soulLensIdleOverlayOpacity();
        int width = context.bounds().width();
        int height = context.bounds().height();
        int x = 0;
        int y = 0;

        blit(graphics, PANEL, x, y, width, height, opacity);
        graphics.text(minecraft.font, OVERLAY_TITLE,
            x + 11, y + 11, color(SoulUiTheme.TEXT, opacity), false);
        graphics.fill(x + 9, y + 29, x + width - 9, y + 30, color(SoulUiTheme.DIVIDER, opacity));
        blit(graphics, INSET, x + 8, y + 37, width - 16, height - 45, opacity);

        Component state = stateMessage();
        if (state != null) {
            int messageColor = SoulUiTheme.MUTED;
            List<net.minecraft.util.FormattedCharSequence> lines = minecraft.font.split(state, width - 28);
            int lineY = y + 44 + Math.max(0, (height - 51 - lines.size() * 10) / 2);
            for (var line : lines) {
                graphics.centeredText(minecraft.font, line, x + width / 2, lineY, color(messageColor, opacity));
                lineY += 10;
            }
            return;
        }
        renderProfile(graphics, minecraft, x + 13, y + ACTIVE_CONTENT_TOP, width - 26,
            height - ACTIVE_CONTENT_TOP - 10, opacity);
    }

    private static void renderProfile(GuiGraphicsExtractor graphics, Minecraft minecraft, int x, int y, int width,
                                      int height, double opacity) {
        if (!cachedTitle.getString().isEmpty())
            graphics.centeredText(minecraft.font, cachedTitle, x + width / 2, y + 3,
                color(SoulUiTheme.ACCENT, opacity));
        graphics.centeredText(minecraft.font, cachedPlayerName, x + width / 2, y + 15,
            color(SoulUiTheme.TEXT, opacity));
        graphics.centeredText(minecraft.font, cachedLevel,
            x + width / 2, y + 27, color(SoulUiTheme.MUTED, opacity));
        graphics.fill(x + 5, y + 40, x + width - 5, y + 41, color(SoulUiTheme.DIVIDER, opacity));

        int statY = y + 46;
        for (int index = 0; index < STAT_NAMES.length; index++) {
            int rowY = statY + index * STAT_ROW_HEIGHT;
            drawRow(graphics, x, rowY, width, 15, opacity);
            Component name = STAT_NAMES[index];
            String valueText = cachedStatValues[index];
            int valueX = x + width - 5 - minecraft.font.width(valueText);
            graphics.text(minecraft.font, trim(minecraft, name, valueX - x - 9), x + 4, rowY + 3,
                color(SoulUiTheme.TEXT, opacity), false);
            graphics.text(minecraft.font, valueText, valueX, rowY + 3,
                color(SoulUiTheme.VALUE, opacity), false);
        }

        int attributesTop = statY + STAT_COUNT * STAT_ROW_HEIGHT + 6;
        graphics.text(minecraft.font, PUBLIC_ATTRIBUTES,
            x + 3, attributesTop, color(SoulUiTheme.ACCENT, opacity), false);
        int clipTop = attributesTop + 14;
        int clipBottom = y + height;
        graphics.enableScissor(x, clipTop, x + width, clipBottom);
        int rowY = clipTop - scroll;
        for (CachedAttributeRow attribute : cachedAttributeRows) {
            if (rowY + 15 < clipTop) {
                rowY += STAT_ROW_HEIGHT;
                continue;
            }
            if (rowY > clipBottom) break;
            drawRow(graphics, x, rowY, width, 15, opacity);
            int valueX = x + width - 4 - minecraft.font.width(attribute.value());
            graphics.text(minecraft.font, trim(minecraft, attribute.name(), valueX - x - 8), x + 4, rowY + 3,
                color(SoulUiTheme.TEXT, opacity), false);
            graphics.text(minecraft.font, attribute.value(), valueX, rowY + 3,
                color(SoulUiTheme.VALUE, opacity), false);
            rowY += STAT_ROW_HEIGHT;
        }
        graphics.disableScissor();
    }

    private static Component stateMessage() {
        return switch (status) {
            case NO_TARGET -> AIM_AT_PLAYER;
            case LOADING -> LOADING_MESSAGE;
            case SoulLensProfilePayload.OUT_OF_RANGE -> OUT_OF_RANGE_MESSAGE;
            case SoulLensProfilePayload.PRIVATE -> PRIVATE_MESSAGE;
            default -> profile == null ? LOADING_MESSAGE : null;
        };
    }

    private static Player findTarget(Minecraft minecraft, double range) {
        Entity camera = minecraft.getCameraEntity();
        if (camera == null || minecraft.level == null) return null;
        Vec3 start = camera.getEyePosition();
        Vec3 end = start.add(camera.getViewVector(1.0F).scale(range));
        HitResult blockHit = minecraft.level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE, camera));
        if (blockHit.getType() == HitResult.Type.BLOCK) end = blockHit.getLocation();
        Vec3 delta = end.subtract(start);
        AABB search = camera.getBoundingBox().expandTowards(delta).inflate(1.0);
        EntityHitResult result = ProjectileUtil.getEntityHitResult(camera, start, end, search,
            entity -> entity instanceof Player && entity != camera && entity.isPickable(), delta.lengthSqr());
        return result != null && result.getEntity() instanceof Player player ? player : null;
    }

    private static boolean isUsing(LocalPlayer player) {
        return player != null && ClientProgressionRules.soulLensEnabled() && player.isUsingItem()
            && player.getUseItem().is(SoulAscensionMod.SOUL_LENS.get());
    }

    private static boolean shouldRender(Minecraft minecraft) {
        return isUsing(minecraft.player) && !minecraft.gui.hud.isHidden()
            && (status != NO_TARGET || ClientProgressionRules.soulLensShowIdleHint());
    }

    private static void clear() {
        if (targetId != null) observationId++;
        targetId = null;
        profile = null;
        cachedAttributeRows = List.of();
        cachedTitle = Component.empty();
        cachedPlayerName = Component.empty();
        cachedLevel = Component.empty();
        status = NO_TARGET;
        ticksSinceRequest = 0;
        targetScanTicks = 0;
        scroll = 0;
    }

    private static int activeHeight(int guiHeight) {
        return Math.min(340, Math.max(230, guiHeight - 52));
    }

    private static int overlayWidth(int guiWidth) {
        boolean active = status == SoulLensProfilePayload.VISIBLE && profile != null;
        return active ? Math.min(270, Math.max(210, guiWidth / 3))
            : Math.min(250, Math.max(180, guiWidth / 4));
    }

    private static int overlayHeight(int guiHeight) {
        boolean active = status == SoulLensProfilePayload.VISIBLE && profile != null;
        return active ? activeHeight(guiHeight) : status == NO_TARGET ? 64 : 78;
    }

    private static int maxScroll() {
        if (profile == null) return 0;
        int visible = Math.max(20, activeHeight(Minecraft.getInstance().getWindow().getGuiScaledHeight())
            - ATTRIBUTE_CLIP_OFFSET - ACTIVE_CONTENT_TOP);
        return Math.max(0, cachedAttributeRows.size() * STAT_ROW_HEIGHT - visible);
    }

    private static int statValue(int index) {
        return switch (index) {
            case 0 -> profile.strength();
            case 1 -> profile.endurance();
            case 2 -> profile.agility();
            case 3 -> profile.intelligence();
            case 4 -> profile.perception();
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    private static void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private static net.minecraft.util.FormattedCharSequence trim(Minecraft minecraft, Component value, int width) {
        List<net.minecraft.util.FormattedCharSequence> lines = minecraft.font.split(value, Math.max(12, width));
        return lines.isEmpty() ? net.minecraft.util.FormattedCharSequence.EMPTY : lines.getFirst();
    }

    private static int color(int base, double opacity) {
        return SoulUiTheme.withOpacity(base, opacity);
    }

    private static void blit(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height,
                             double opacity) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height,
            (float) Math.max(0.0, Math.min(1.0, opacity)));
    }

    private static void drawRow(GuiGraphicsExtractor graphics, int x, int y, int width, int height, double opacity) {
        graphics.fill(x, y, x + width, y + height, color(0xE8140F1D, opacity));
        graphics.fill(x, y, x + width, y + 1, color(SoulUiTheme.DIVIDER, opacity));
        graphics.fill(x, y + height - 1, x + width, y + height, color(0xFF2D2438, opacity));
    }
}
