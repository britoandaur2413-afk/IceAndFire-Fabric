package com.iafabric.entity.tile;

import com.iafabric.compat.ValueOutput;
import net.minecraft.core.HolderLookup;
import com.iafabric.compat.ValueInput;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

/** The dread spawner: a mob spawner of its own block entity type (the vanilla one only accepts the vanilla spawner block). */
public class TileEntityDreadSpawner extends BlockEntity {
    private final DreadSpawnerBaseLogic spawner = new DreadSpawnerBaseLogic() {
        @Override
        public void broadcastEvent(Level p_155767_, @NotNull BlockPos p_155768_, int p_155769_) {
            p_155767_.blockEvent(p_155768_, Blocks.SPAWNER, p_155769_, 0);
        }

        @Override
        public void setNextSpawnData(@Nullable Level p_155771_, @NotNull BlockPos p_155772_, @NotNull SpawnData p_155773_) {
            super.setNextSpawnData(p_155771_, p_155772_, p_155773_);
            if (p_155771_ != null) {
                BlockState blockstate = p_155771_.getBlockState(p_155772_);
                p_155771_.sendBlockUpdated(p_155772_, blockstate, blockstate, 4);
            }

        }

    };

    public TileEntityDreadSpawner(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.DREAD_SPAWNER, pos, state);
    }

    public void setEntityId(EntityType<?> entityType, RandomSource random) {
        this.spawner.setEntityId(entityType, this.level, random, this.worldPosition);
        this.setChanged();
    }

    @Override
    public void load(CompoundTag p_155760_Tag) {
		ValueInput p_155760_ = new ValueInput(p_155760_Tag);
        super.load(p_155760_Tag);
        this.spawner.load(this.level, this.worldPosition, p_155760_Tag);
    }

    @Override
    protected void saveAdditional(CompoundTag outputTag) {
		ValueOutput output = new ValueOutput(outputTag);
        super.saveAdditional(outputTag);
        this.spawner.save(outputTag);
    }

    public static void clientTick(Level p_155755_, BlockPos p_155756_, BlockState p_155757_, TileEntityDreadSpawner p_155758_) {
        p_155758_.spawner.clientTick(p_155755_, p_155756_);
    }

    public static void serverTick(Level p_155762_, BlockPos p_155763_, BlockState p_155764_, TileEntityDreadSpawner p_155765_) {
        p_155765_.spawner.serverTick((ServerLevel) p_155762_, p_155763_);
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag compoundtag = this.saveWithoutMetadata();
        compoundtag.remove("SpawnPotentials");
        return compoundtag;
    }

    @Override
    public boolean triggerEvent(int p_59797_, int p_59798_) {
        return this.spawner.onEventTriggered(this.level, p_59797_) || super.triggerEvent(p_59797_, p_59798_);
    }


    public BaseSpawner getSpawner() {
        return this.spawner;
    }

}