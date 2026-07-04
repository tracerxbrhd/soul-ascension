package dev.uapi.soulascension.client;

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
    private static final int TEXT = 0xFFF1E9FF;
    private static final int MUTED = 0xFF9B91AA;
    private static final int VALUE = 0xFFD79BFF;
    private static final int ACCENT = 0xFFD66BFF;
    private static final int DIVIDER = 0xFF8E4BC4;
    private static final int NO_TARGET = -2;
    private static final int LOADING = -1;
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
        if (profile != null) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int)Math.signum(delta) * 18));
        }
        return true;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isUsing(minecraft.player) || minecraft.options.hideGui) return;
        int width = Math.min(270, Math.max(190, graphics.guiWidth() / 3));
        int height = Math.min(320, Math.max(150, graphics.guiHeight() - 52));
        int x = 12, y = (graphics.guiHeight() - height) / 2;
        graphics.blitSprite(PANEL, x, y, width, height);
        graphics.drawString(minecraft.font, Component.translatable("overlay.soul_ascension.soul_lens.title"),
            x + 11, y + 11, TEXT, false);
        graphics.fill(x + 9, y + 29, x + width - 9, y + 30, DIVIDER);
        graphics.blitSprite(INSET, x + 8, y + 37, width - 16, height - 45);

        Component state = stateMessage();
        if (state != null) {
            graphics.drawCenteredString(minecraft.font, state, x + width / 2, y + height / 2 - 4,
                status == SoulLensProfilePayload.HIDDEN ? ACCENT : MUTED);
            return;
        }
        renderProfile(graphics, minecraft, x + 13, y + 43, width - 26, height - 53);
    }

    private static void renderProfile(GuiGraphics graphics, Minecraft minecraft, int x, int y, int width, int height) {
        Component title = ClientTitleCatalog.get(profile.activeTitle())
            .<Component>map(value -> Component.translatable(value.nameKey())).orElse(Component.empty());
        if (!title.getString().isEmpty())
            graphics.drawCenteredString(minecraft.font, title, x + width / 2, y + 3, ACCENT);
        graphics.drawCenteredString(minecraft.font, Component.literal(profile.playerName()), x + width / 2, y + 15, TEXT);
        graphics.drawCenteredString(minecraft.font, Component.translatable("screen.soul_ascension.level", profile.level()),
            x + width / 2, y + 27, MUTED);
        graphics.fill(x + 5, y + 40, x + width - 5, y + 41, DIVIDER);

        String[] names = {"strength", "endurance", "agility", "intelligence", "perception"};
        int[] values = {profile.strength(), profile.endurance(), profile.agility(), profile.intelligence(), profile.perception()};
        int statY = y + 46;
        for (int index = 0; index < names.length; index++) {
            int column = index % 2;
            int row = index / 2;
            int cellX = x + column * width / 2;
            int cellWidth = index == 4 ? width : width / 2 - 2;
            graphics.blitSprite(SECTION, cellX, statY + row * 17, cellWidth, 15);
            Component name = Component.translatable("stat.soul_ascension.short." + names[index]);
            graphics.drawString(minecraft.font, name, cellX + 4, statY + row * 17 + 3, TEXT, false);
            graphics.drawString(minecraft.font, Integer.toString(values[index]),
                cellX + cellWidth - 5 - minecraft.font.width(Integer.toString(values[index])),
                statY + row * 17 + 3, VALUE, false);
        }

        int attributesTop = statY + 56;
        graphics.drawString(minecraft.font, Component.translatable("screen.soul_ascension.public_attributes"),
            x + 3, attributesTop, ACCENT, false);
        int clipTop = attributesTop + 14;
        graphics.enableScissor(x, clipTop, x + width, y + height);
        int rowY = clipTop - scroll;
        for (PublicProfileData.PublicAttribute attribute : profile.attributes()) {
            var holder = BuiltInRegistries.ATTRIBUTE.getHolder(attribute.id()).orElse(null);
            if (holder == null) continue;
            graphics.blitSprite(SECTION, x, rowY, width, 15);
            Component name = Component.translatable(holder.value().getDescriptionId());
            Component value = DynamicAttributeView.formatValue(attribute.id(), holder.value(), attribute.value());
            graphics.drawString(minecraft.font, trim(minecraft, name, width - 72), x + 4, rowY + 3, TEXT, false);
            graphics.drawString(minecraft.font, value, x + width - 4 - minecraft.font.width(value), rowY + 3, VALUE, false);
            rowY += 17;
        }
        graphics.disableScissor();
    }

    private static Component stateMessage() {
        return switch (status) {
            case NO_TARGET -> Component.translatable("overlay.soul_ascension.soul_lens.no_target");
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

    private static int maxScroll() {
        if (profile == null) return 0;
        Minecraft minecraft = Minecraft.getInstance();
        int visible = Math.min(320, Math.max(150, minecraft.getWindow().getGuiScaledHeight() - 52)) - 166;
        return Math.max(0, profile.attributes().size() * 17 - Math.max(20, visible));
    }

    private static void clampScroll() { scroll = Math.max(0, Math.min(scroll, maxScroll())); }

    private static net.minecraft.util.FormattedCharSequence trim(Minecraft minecraft, Component value, int width) {
        List<net.minecraft.util.FormattedCharSequence> lines = minecraft.font.split(value, Math.max(12, width));
        return lines.isEmpty() ? net.minecraft.util.FormattedCharSequence.EMPTY : lines.getFirst();
    }
}
