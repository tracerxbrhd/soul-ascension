package dev.uapi.soulascension.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.uapi.soulascension.block.entity.SoulAltarBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public final class SoulAltarRenderer implements BlockEntityRenderer<SoulAltarBlockEntity> {
    private final ItemRenderer itemRenderer;

    public SoulAltarRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SoulAltarBlockEntity altar, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        long gameTime = altar.getLevel() == null ? 0L : altar.getLevel().getGameTime();
        float time = gameTime + partialTick;
        pose.pushPose();
        pose.translate(0.5, 1.22 + Math.sin(time * 0.08) * 0.07, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 2.25F));
        pose.mulPose(Axis.XP.rotationDegrees(12.0F));
        pose.scale(0.62F, 0.62F, 0.62F);
        itemRenderer.renderStatic(Items.ENDER_EYE.getDefaultInstance(), ItemDisplayContext.GROUND,
            packedLight, packedOverlay, pose, buffers, altar.getLevel(), 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SoulAltarBlockEntity altar) {
        return new AABB(altar.getBlockPos()).inflate(0.5, 1.0, 0.5);
    }
}
