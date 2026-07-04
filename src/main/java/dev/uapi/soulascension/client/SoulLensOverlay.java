package dev.uapi.soulascension.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.network.ClientProgressionRules;
import dev.uapi.soulascension.network.ClientTitleCatalog;
import dev.uapi.soulascension.network.PublicProfileData;
import dev.uapi.soulascension.network.SoulLensProfilePayload;
import dev.uapi.soulascension.network.SoulLensRequestPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public final class SoulLensOverlay {
    private static final ResourceLocation PANEL = SoulAscensionMod.id("character/panel");
    private static final ResourceLocation INSET = SoulAscensionMod.id("character/inset");
    private static final ResourceLocation SECTION = SoulAscensionMod.id("character/section");
    private static final int NO_TARGET = -2;
    private static final int LOADING = -1;
    private static final int STAT_ROW_HEIGHT = 17;
    private static final int STAT_COUNT = 5;
    private static final int ACTIVE_CONTENT_TOP = 43;
    private static final int ATTRIBUTE_CLIP_OFFSET = 151;
    private static UUID targetId;
    private static int status = NO_TARGET;
    private static PublicProfileData profile;
    private static int ticksSinceRequest;
    private static int scroll;

    private SoulLensOverlay() {}

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!isUsing(player) || minecraft.level == null || minecraft.screen != null) {
            clear();
            return;
        }
        Player target = findTarget(minecraft, ClientProgressionRules.soulLensRange());
        if (target == null) {
            targetId = null;
            status = NO_TARGET;
            profile = null;
            scroll = 0;
            return;
        }
        boolean changed = !target.getUUID().equals(targetId);
        if (changed) {
            targetId = target.getUUID();
            status = LOADING;
            profile = null;
            scroll = 0;
            ticksSinceRequest = ClientProgressionRules.soulLensUpdateInterval();
        }
        if (++ticksSinceRequest >= ClientProgressionRules.soulLensUpdateInterval()) {
            ticksSinceRequest = 0;
            PacketDistributor.sendToServer(new SoulLensRequestPayload(target.getUUID()));
        }
    }

    public static void receive(SoulLensProfilePayload payload) {
        if (!payload.targetId().equals(targetId)) return;
        status = payload.status();
        profile = payload.profile();
        clampScroll();
    }

    public static boolean handleScroll(double delta) {
        if (!isUsing(Minecraft.getInstance().player) || !ClientProgressionRules.soulLensBlockHotbarScroll()) return false;
        if (profile != null)
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * 18));
        return true;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isUsing(minecraft.player) || minecraft.options.hideGui) return;
        if (status == NO_TARGET && !ClientProgressionRules.soulLensShowIdleHint()) return;

        boolean active = status == SoulLensProfilePayload.VISIBLE && profile != null;
        double opacity = active ? ClientProgressionRules.soulLensActiveOverlayOpacity()
            : status == SoulLensProfilePayload.HIDDEN ? ClientProgressionRules.soulLensHiddenOverlayOpacity()
            : ClientProgressionRules.soulLensIdleOverlayOpacity();
        int width = active ? Math.min(270, Math.max(210, graphics.guiWidth() / 3))
            : Math.min(250, Math.max(180, graphics.guiWidth() / 4));
        int height = active ? activeHeight(graphics.guiHeight()) : status == NO_TARGET ? 64 : 78;
        int x = 12;
        int y = Math.max(8, (graphics.guiHeight() - height) / 2);

        blit(graphics, PANEL, x, y, width, height, opacity);
        graphics.drawString(minecraft.font, Component.translatable("overlay.soul_ascension.soul_lens.title"),
            x + 11, y + 11, color(SoulUiTheme.TEXT, opacity), false);
        graphics.fill(x + 9, y + 29, x + width - 9, y + 30, color(SoulUiTheme.DIVIDER, opacity));
        blit(graphics, INSET, x + 8, y + 37, width - 16, height - 45, opacity);

        Component state = stateMessage();
        if (state != null) {
            int messageColor = status == SoulLensProfilePayload.HIDDEN ? SoulUiTheme.ACCENT : SoulUiTheme.MUTED;
            List<net.minecraft.util.FormattedCharSequence> lines = minecraft.font.split(state, width - 28);
            int lineY = y + 44 + Math.max(0, (height - 51 - lines.size() * 10) / 2);
            for (var line : lines) {
                graphics.drawCenteredString(minecraft.font, line, x + width / 2, lineY, color(messageColor, opacity));
                lineY += 10;
            }
            return;
        }
        renderProfile(graphics, minecraft, x + 13, y + ACTIVE_CONTENT_TOP, width - 26,
            height - ACTIVE_CONTENT_TOP - 10, opacity);
    }

    private static void renderProfile(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width,
                                      int height, double opacity) {
        Component title = ClientTitleCatalog.get(profile.activeTitle())
            .<Component>map(value -> Component.translatable(value.nameKey())).orElse(Component.empty());
        if (!title.getString().isEmpty())
            graphics.drawCenteredString(minecraft.font, title, x + width / 2, y + 3,
                color(SoulUiTheme.ACCENT, opacity));
        graphics.drawCenteredString(minecraft.font, Component.literal(profile.playerName()), x + width / 2, y + 15,
            color(SoulUiTheme.TEXT, opacity));
        graphics.drawCenteredString(minecraft.font, Component.translatable("screen.soul_ascension.level", profile.level()),
            x + width / 2, y + 27, color(SoulUiTheme.MUTED, opacity));
        graphics.fill(x + 5, y + 40, x + width - 5, y + 41, color(SoulUiTheme.DIVIDER, opacity));

        String[] names = {"strength", "endurance", "agility", "intelligence", "perception"};
        int[] values = {profile.strength(), profile.endurance(), profile.agility(), profile.intelligence(),
            profile.perception()};
        int statY = y + 46;
        for (int index = 0; index < names.length; index++) {
            int rowY = statY + index * STAT_ROW_HEIGHT;
            blit(graphics, SECTION, x, rowY, width, 15, opacity);
            Component name = Component.translatable("stat.soul_ascension.short." + names[index]);
            String valueText = Integer.toString(values[index]);
            int valueX = x + width - 5 - minecraft.font.width(valueText);
            graphics.drawString(minecraft.font, trim(minecraft, name, valueX - x - 9), x + 4, rowY + 3,
                color(SoulUiTheme.TEXT, opacity), false);
            graphics.drawString(minecraft.font, valueText, valueX, rowY + 3,
                color(SoulUiTheme.VALUE, opacity), false);
        }

        int attributesTop = statY + STAT_COUNT * STAT_ROW_HEIGHT + 6;
        graphics.drawString(minecraft.font, Component.translatable("overlay.soul_ascension.soul_lens.public_attributes"),
            x + 3, attributesTop, color(SoulUiTheme.ACCENT, opacity), false);
        int clipTop = attributesTop + 14;
        graphics.enableScissor(x, clipTop, x + width, y + height);
        int rowY = clipTop - scroll;
        for (PublicProfileData.PublicAttribute attribute : profile.attributes()) {
            var holder = BuiltInRegistries.ATTRIBUTE.getHolder(attribute.id()).orElse(null);
            if (holder == null) continue;
            blit(graphics, SECTION, x, rowY, width, 15, opacity);
            Component name = Component.translatable(holder.value().getDescriptionId());
            Component value = DynamicAttributeView.formatValue(attribute.id(), holder.value(), attribute.value());
            int valueX = x + width - 4 - minecraft.font.width(value);
            graphics.drawString(minecraft.font, trim(minecraft, name, valueX - x - 8), x + 4, rowY + 3,
                color(SoulUiTheme.TEXT, opacity), false);
            graphics.drawString(minecraft.font, value, valueX, rowY + 3, color(SoulUiTheme.VALUE, opacity), false);
            rowY += STAT_ROW_HEIGHT;
        }
        graphics.disableScissor();
    }

    private static Component stateMessage() {
        return switch (status) {
            case NO_TARGET -> Component.translatable("overlay.soul_ascension.soul_lens.aim_at_player");
            case LOADING -> Component.translatable("overlay.soul_ascension.soul_lens.loading");
            case SoulLensProfilePayload.HIDDEN -> Component.translatable("overlay.soul_ascension.soul_lens.hidden_profile");
            case SoulLensProfilePayload.OUT_OF_RANGE -> Component.translatable("overlay.soul_ascension.soul_lens.out_of_range");
            default -> profile == null ? Component.translatable("overlay.soul_ascension.soul_lens.loading") : null;
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

    private static void clear() {
        targetId = null;
        profile = null;
        status = NO_TARGET;
        ticksSinceRequest = 0;
        scroll = 0;
    }

    private static int activeHeight(int guiHeight) {
        return Math.min(340, Math.max(230, guiHeight - 52));
    }

    private static int maxScroll() {
        if (profile == null) return 0;
        int visible = Math.max(20, activeHeight(Minecraft.getInstance().getWindow().getGuiScaledHeight())
            - ATTRIBUTE_CLIP_OFFSET - ACTIVE_CONTENT_TOP);
        return Math.max(0, profile.attributes().size() * STAT_ROW_HEIGHT - visible);
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

    private static void blit(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height,
                             double opacity) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float) Math.max(0.0, Math.min(1.0, opacity)));
        graphics.blitSprite(sprite, x, y, width, height);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
