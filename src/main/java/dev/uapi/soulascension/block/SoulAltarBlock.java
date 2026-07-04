package dev.uapi.soulascension.block;

import com.mojang.serialization.MapCodec;
import dev.uapi.soulascension.block.entity.SoulAltarBlockEntity;
import dev.uapi.soulascension.progression.SoulAltarService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class SoulAltarBlock extends BaseEntityBlock {
    public static final MapCodec<SoulAltarBlock> CODEC = simpleCodec(SoulAltarBlock::new);

    public SoulAltarBlock(BlockBehaviour.Properties properties) { super(properties); }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer serverPlayer) SoulAltarService.open(serverPlayer, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.35 + random.nextDouble() * 0.3,
            pos.getY() + 1.05 + random.nextDouble() * 0.4, pos.getZ() + 0.35 + random.nextDouble() * 0.3,
            0.0, 0.015, 0.0);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoulAltarBlockEntity(pos, state);
    }
}
