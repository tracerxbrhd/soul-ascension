package dev.uapi.soulascension.block.entity;

import dev.uapi.soulascension.SoulAscensionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SoulAltarBlockEntity extends BlockEntity {
    public SoulAltarBlockEntity(BlockPos pos, BlockState state) {
        super(SoulAscensionMod.SOUL_ALTAR_BLOCK_ENTITY.get(), pos, state);
    }
}
