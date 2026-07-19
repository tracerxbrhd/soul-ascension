package dev.uapi.soulascension.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.uapi.soulascension.SoulAscensionMod;
import dev.uapi.soulascension.data.SoulAscensionAttachments;
import dev.uapi.soulascension.data.TitleProgress;
import dev.uapi.soulascension.network.ClientTitleCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = SoulAscensionMod.MOD_ID, value = Dist.CLIENT)
public final class SoulAscensionNameTagRenderer {
    private SoulAscensionNameTagRenderer() {}

    @SubscribeEvent
    public static void renderTitle(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer viewer = minecraft.player;
        if (viewer == null || player == viewer || !shouldRender(event, player, viewer, minecraft)) return;
        var titleId = player.getData(SoulAscensionAttachments.ACTIVE_TITLE).titleId();
        if (titleId.equals(TitleProgress.NONE)) return;
        Component title = ClientTitleCatalog.get(titleId)
            .<Component>map(value -> Component.translatable(value.nameKey())).orElse(Component.empty());
        if (title.getString().isEmpty()) return;

        Vec3 anchor = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0,
            player.getViewYRot(event.getPartialTick()));
        if (anchor == null) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(anchor.x, anchor.y + 0.5, anchor.z);
        pose.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(0.025F, -0.025F, 0.025F);
        Matrix4f matrix = pose.last().pose();
        Font font = minecraft.font;
        float x = -font.width(title) / 2.0F;
        float y = -10.0F;
        boolean seeThrough = !player.isDiscrete();
        int background = (int) (minecraft.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(title, x, y, 0x55FFD47A, false, matrix, event.getMultiBufferSource(),
            seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, background, event.getPackedLight());
        if (seeThrough) font.drawInBatch(title, x, y, 0xFFFFD47A, false, matrix,
            event.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, event.getPackedLight());
        pose.popPose();
    }

    private static boolean shouldRender(RenderNameTagEvent event, Player player, LocalPlayer viewer,
                                        Minecraft minecraft) {
        if (event.canRender() == TriState.FALSE) return false;
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        double distance = dispatcher.distanceToSqr(player);
        if (!ClientHooks.isNameplateInRenderDistance(player, distance)) return false;
        if (event.canRender() == TriState.TRUE) return true;
        float range = player.isDiscrete() ? 32.0F : 64.0F;
        if (distance >= range * range) return false;
        boolean visible = !player.isInvisibleTo(viewer);
        Team team = player.getTeam();
        Team viewerTeam = viewer.getTeam();
        if (team != null) {
            return switch (team.getNameTagVisibility()) {
                case ALWAYS -> visible;
                case NEVER -> false;
                case HIDE_FOR_OTHER_TEAMS -> viewerTeam == null ? visible
                    : team.isAlliedTo(viewerTeam) && (team.canSeeFriendlyInvisibles() || visible);
                case HIDE_FOR_OWN_TEAM -> viewerTeam == null ? visible : !team.isAlliedTo(viewerTeam) && visible;
            };
        }
        return Minecraft.renderNames() && player != minecraft.getCameraEntity() && visible && !player.isVehicle();
    }
}
