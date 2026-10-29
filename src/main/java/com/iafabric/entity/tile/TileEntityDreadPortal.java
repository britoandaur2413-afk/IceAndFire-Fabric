package com.iafabric.entity.tile;

import net.minecraft.core.HolderLookup;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class TileEntityDreadPortal extends BlockEntity {
    private long age;
    private BlockPos exitPortal;
    private boolean exactTeleport;

    public TileEntityDreadPortal(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.DREAD_PORTAL, pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
		ValueOutput compound = new ValueOutput(compoundTag);
        super.saveAdditional(compoundTag);
        compound.putLong("Age", this.age);

        if (this.exitPortal != null) {
            //   compound.setTag("ExitPortal", NBTUtil.createPosTag(this.exitPortal));
        }

        if (this.exactTeleport) {
            compound.putBoolean("ExactTeleport", this.exactTeleport);
        }
    }

    @Override
    public void load(CompoundTag compoundTag) {
		ValueInput compound = new ValueInput(compoundTag);
        super.load(compoundTag);
        this.age = compound.getLongOr("Age", 0L);

        if (compound.contains("ExitPortal")) {
            this.exitPortal = BlockPos.ZERO;
        }

        this.exactTeleport = compound.getBooleanOr("ExactTeleport", false);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityDreadPortal dreadPortal) {
        ++dreadPortal.age;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean shouldRenderFace(Direction face) {
        return true;
    }
}
