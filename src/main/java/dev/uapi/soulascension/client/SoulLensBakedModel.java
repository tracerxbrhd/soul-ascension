package dev.uapi.soulascension.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

/** Selects the 3D lens only for contexts that render it on the player. */
final class SoulLensBakedModel extends BakedModelWrapper<BakedModel> {
    private final BakedModel inHandModel;

    SoulLensBakedModel(BakedModel inventoryModel, BakedModel inHandModel) {
        super(inventoryModel);
        this.inHandModel = inHandModel;
    }

    @Override
    public BakedModel applyTransform(ItemDisplayContext context, PoseStack poseStack, boolean leftHand) {
        return switch (context) {
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND,
                 THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND,
                 HEAD -> inHandModel.applyTransform(context, poseStack, leftHand);
            default -> super.applyTransform(context, poseStack, leftHand);
        };
    }
}
