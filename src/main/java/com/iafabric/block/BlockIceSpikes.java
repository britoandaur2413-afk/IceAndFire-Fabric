package com.iafabric.block;


import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.MapColor;
import com.iafabric.entity.EntityIceDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class BlockIceSpikes extends Block {
    protected static final VoxelShape AABB = Block.box(1, 0, 1, 15, 8, 15);
    public Item itemBlock;

    public BlockIceSpikes() {
        super(
            IafBlockRegistry.props().mapColor(MapColor.ICE)
                .noOcclusion()
                .dynamicShape()
                .randomTicks()
                .sound(SoundType.GLASS)
                .strength(2.5F)
                .requiresCorrectToolForDrops()
        );
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        return !stateIn.canSurvive(worldIn, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader worldIn, BlockPos pos) {
        BlockPos blockpos = pos.below();
        return this.isValidGround(worldIn.getBlockState(blockpos), worldIn, blockpos);
    }

    @Override
    public boolean propagatesSkylightDown(@NotNull BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return true;
    }

    private boolean isValidGround(BlockState blockState, LevelReader worldIn, BlockPos blockpos) {
        return blockState.canOcclude();
    }

    @Override
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return AABB;
    }

    @Override
    public VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return AABB;
    }

    @Override
    public void stepOn(Level worldIn, BlockPos pos, BlockState pState, Entity entityIn) {
        if (!(entityIn instanceof EntityIceDragon)) {
            entityIn.hurt(entityIn.damageSources().cactus(), 1);
            if (entityIn instanceof LivingEntity && entityIn.getDeltaMovement().x != 0 && entityIn.getDeltaMovement().z != 0) {
                ((LivingEntity) entityIn).knockback(0.5F, entityIn.getDeltaMovement().x, entityIn.getDeltaMovement().z);
            }
        }
    }

    @Override
    public boolean useShapeForLightOcclusion(@NotNull BlockState state) {
        return true;
    }

}
