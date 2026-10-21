package com.iafabric.block;


import net.minecraft.world.entity.InsideBlockEffectApplier;
import com.iafabric.IafConfig;
import net.minecraft.world.level.material.MapColor;
import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityDreadPortal;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.enums.EnumParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import net.minecraft.util.RandomSource;

import static com.iafabric.entity.tile.IafTileEntityRegistry.DREAD_PORTAL;

public class BlockDreadPortal extends BaseEntityBlock implements IDreadBlock {

    public BlockDreadPortal() {
        super(
            IafBlockRegistry.props().mapColor(MapColor.NONE)
                .dynamicShape()
                .strength(-1, 100000)
                .lightLevel((state) -> {
                    return 1;
                })
                .randomTicks()
		);
    }

    @Override
    public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean intersects) {
     /* if(entity.dimension != IafConfig.dreadlandsDimensionId){
            MiscEntityProperties properties = EntityPropertiesHandler.INSTANCE.getProperties(entity, MiscEntityProperties.class);
            if (properties != null) {
                properties.lastEnteredDreadPortalX = pos.getX();
                properties.lastEnteredDreadPortalY = pos.getY();
                properties.lastEnteredDreadPortalZ = pos.getZ();
            }
        }
        if ((!entity.isBeingRidden()) && (entity.getPassengers().isEmpty()) && (entity instanceof PlayerEntityMP)) {
            CriteriaTriggers.ENTER_BLOCK.trigger((PlayerEntityMP) entity, world.getBlockState(pos));
            PlayerEntityMP thePlayer = (PlayerEntityMP) entity;
            if (thePlayer.timeUntilPortal > 0) {
                thePlayer.timeUntilPortal = 10;
            } else if (thePlayer.dimension != IafConfig.dreadlandsDimensionId) {
                thePlayer.timeUntilPortal = 10;
                thePlayer.changeDimension(IafConfig.dreadlandsDimensionId, new TeleporterDreadLands(thePlayer.server.getWorld(IafConfig.dreadlandsDimensionId), false));
            } else {
                MiscEntityProperties properties = EntityPropertiesHandler.INSTANCE.getProperties(thePlayer, MiscEntityProperties.class);
                BlockPos setPos = BlockPos.ORIGIN;
                if (properties != null) {
                    setPos = new BlockPos(properties.lastEnteredDreadPortalX, properties.lastEnteredDreadPortalY, properties.lastEnteredDreadPortalZ);
                }
                thePlayer.timeUntilPortal = 10;
                thePlayer.changeDimension( 0, new TeleporterDreadLands(thePlayer.server.getWorld(0), true));
                thePlayer.setPositionAndRotation(setPos.getX(), setPos.getY() + 0.5D, setPos.getZ(), 0, 0);

            }
        }*/
    }


    public void updateTick(Level worldIn, BlockPos pos, BlockState state, RandomSource rand) {
        if (!this.canSurviveAt(worldIn, pos)) {
            worldIn.destroyBlock(pos, true);
        }
    }

    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!this.canSurviveAt(worldIn, pos)) {
            worldIn.destroyBlock(pos, true);
        }
    }

    public boolean canSurviveAt(Level world, BlockPos pos) {
        return DragonUtils.isDreadBlock(world.getBlockState(pos.above())) && DragonUtils.isDreadBlock(world.getBlockState(pos.below()));
    }

    public int quantityDropped(RandomSource random) {
        return 0;
    }

    @Override
    public void animateTick(@NotNull BlockState stateIn, Level worldIn, @NotNull BlockPos pos, @NotNull RandomSource rand) {
        BlockEntity tileentity = worldIn.getBlockEntity(pos);

        if (tileentity instanceof TileEntityDreadPortal) {
            int i = 3;
            for (int j = 0; j < i; ++j) {
                double d0 = (float) pos.getX() + rand.nextFloat();
                double d1 = (float) pos.getY() + rand.nextFloat();
                double d2 = (float) pos.getZ() + rand.nextFloat();
                double d3 = ((double) rand.nextFloat() - 0.5D) * 0.25D;
                double d4 = ((double) rand.nextFloat()) * -0.25D;
                double d5 = ((double) rand.nextFloat() - 0.5D) * 0.25D;
                int k = rand.nextInt(2) * 2 - 1;
                IafMod.PROXY.spawnParticle(EnumParticles.Dread_Portal, d0, d1, d2, d3, d4, d5);
                //worldIn.spawnParticle(ParticleTypes.END_ROD, d0, d1, d2, d3, d4, d5);
            }
        }
    }

    public boolean isOpaqueCube(BlockState state) {
        return false;
    }

    public boolean isFullCube(BlockState state) {
        return false;
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> entityType) {
        return createTickerHelper(entityType, DREAD_PORTAL, TileEntityDreadPortal::tick);
    }


    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new TileEntityDreadPortal(pos, state);
    }
}