package com.iafabric.entity.tile;

import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.storage.ValueInput;
import com.iafabric.entity.EntityGhost;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class TileEntityGhostChest extends ChestBlockEntity {

    public TileEntityGhostChest(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.GHOST_CHEST, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput nbt) {
        super.loadAdditional(nbt);
    }

    @Override
    public void startOpen(ContainerUser player) {
        super.startOpen(player);
        if (this.level.getDifficulty() != Difficulty.PEACEFUL) {
            EntityGhost ghost = IafEntityRegistry.GHOST.create(level, EntitySpawnReason.TRIGGERED);
            ghost.absSnapTo(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.5F, this.worldPosition.getZ() + 0.5F,
                ThreadLocalRandom.current().nextFloat() * 360F, 0);
            if (!this.level.isClientSide()) {
                ghost.finalizeSpawn((ServerLevel) level, ((ServerLevel) level).getCurrentDifficultyAt(this.worldPosition), EntitySpawnReason.SPAWNER, null);
                if (player instanceof Player realPlayer && !realPlayer.isCreative()) {
                    ghost.setTarget(realPlayer);
                }
                ghost.setPersistenceRequired();
                level.addFreshEntity(ghost);
            }
            ghost.setAnimation(EntityGhost.ANIMATION_SCARE);
            ghost.setHomeTo(this.worldPosition, 4);
            ghost.setFromChest(true);
        }
    }

    @Override
    protected void signalOpenCount(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, int p_155336_, int p_155337_) {
        super.signalOpenCount(level, pos, state, p_155336_, p_155337_);
        level.updateNeighborsAt(pos.below(), state.getBlock());
    }
}
