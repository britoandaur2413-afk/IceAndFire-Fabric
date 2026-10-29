package com.iafabric.entity.tile;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import com.iafabric.util.EntityUtil;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.network.IafNetwork;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.enums.EnumParticles;
import com.iafabric.message.MessageUpdatePixieHouse;
import com.iafabric.message.MessageUpdatePixieHouseModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import net.minecraft.util.RandomSource;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TileEntityPixieHouse extends BlockEntity {

    private static final float PARTICLE_WIDTH = 0.3F;
    private static final float PARTICLE_HEIGHT = 0.6F;
    private final RandomSource rand;
    public int houseType;
    public boolean hasPixie;
    public boolean tamedPixie;
    public UUID pixieOwnerUUID;
    public int pixieType;
    public int ticksExisted;
    public NonNullList<ItemStack> pixieItems = NonNullList.withSize(1, ItemStack.EMPTY);

    public TileEntityPixieHouse(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.PIXIE_HOUSE, pos, state);
        this.rand = RandomSource.create();
    }

    public static int getHouseTypeFromBlock(Block block) {
        if (block == IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED) return 1;
        if (block == IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN) return 0;
        if (block == IafBlockRegistry.PIXIE_HOUSE_OAK) return 3;
        if (block == IafBlockRegistry.PIXIE_HOUSE_BIRCH) return 2;
        if (block == IafBlockRegistry.PIXIE_HOUSE_SPRUCE) return 5;
        if (block == IafBlockRegistry.PIXIE_HOUSE_DARK_OAK) return 4;
        else return 0;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityPixieHouse entityPixieHouse) {
        entityPixieHouse.ticksExisted++;
        if (!level.isClientSide && entityPixieHouse.hasPixie && ThreadLocalRandom.current().nextInt(100) == 0) {
            entityPixieHouse.releasePixie();
        }
        if (level.isClientSide && entityPixieHouse.hasPixie) {
            IafMod.PROXY.spawnParticle(EnumParticles.If_Pixie,
                pos.getX() + 0.5F + (double) (entityPixieHouse.rand.nextFloat() * PARTICLE_WIDTH * 2F) - PARTICLE_WIDTH,
                pos.getY() + (double) (entityPixieHouse.rand.nextFloat() * PARTICLE_HEIGHT),
                pos.getZ() + 0.5F + (double) (entityPixieHouse.rand.nextFloat() * PARTICLE_WIDTH * 2F) - PARTICLE_WIDTH,
                EntityPixie.PARTICLE_RGB[entityPixieHouse.pixieType][0], EntityPixie.PARTICLE_RGB[entityPixieHouse.pixieType][1],
                EntityPixie.PARTICLE_RGB[entityPixieHouse.pixieType][2]);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
		ValueOutput compound = new ValueOutput(compoundTag);
        compound.putInt("HouseType", houseType);
        compound.putBoolean("HasPixie", hasPixie);
        compound.putInt("PixieType", pixieType);
        compound.putBoolean("TamedPixie", tamedPixie);
        if (pixieOwnerUUID != null) {
            compound.store("PixieOwnerUUID", UUIDUtil.CODEC, pixieOwnerUUID);
        }
        ContainerHelper.saveAllItems(compound.tag(), this.pixieItems);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void load(CompoundTag compoundTag) {
		ValueInput compound = new ValueInput(compoundTag);
        houseType = compound.getIntOr("HouseType", 0);
        hasPixie = compound.getBooleanOr("HasPixie", false);
        pixieType = compound.getIntOr("PixieType", 0);
        tamedPixie = compound.getBooleanOr("TamedPixie", false);
        if (compound.read("PixieOwnerUUID", UUIDUtil.CODEC).isPresent()) {
            pixieOwnerUUID = compound.read("PixieOwnerUUID", UUIDUtil.CODEC).orElse(null);
        }
        this.pixieItems = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compound.tag(), pixieItems);
        super.load(compoundTag);
    }

    public void releasePixie() {
        EntityPixie pixie = new EntityPixie(IafEntityRegistry.PIXIE, this.level);
        pixie.absMoveTo(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 1F, this.worldPosition.getZ() + 0.5F,
            ThreadLocalRandom.current().nextInt(360), 0);
        pixie.setItemInHand(InteractionHand.MAIN_HAND, pixieItems.get(0));
        pixie.setColor(this.pixieType);
        if (!level.isClientSide) {
            level.addFreshEntity(pixie);
        }
        this.hasPixie = false;
        this.pixieType = 0;
        pixie.ticksUntilHouseAI = 500;
        pixie.setTame(this.tamedPixie);
        EntityUtil.setOwnerUUID(pixie, this.pixieOwnerUUID);
        if (!level.isClientSide) {
            IafNetwork.sendToAll(new MessageUpdatePixieHouse(worldPosition.asLong(), false, 0));
        }
    }

    /** The pixie inside flies out when the block is broken. */
    public void onBroken() {
        if (this.hasPixie && this.level != null && !this.level.isClientSide) {
            this.releasePixie();
        }
    }
}
