package com.iafabric.entity.tile;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import com.iafabric.util.EntityUtil;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.network.IafNetwork;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.enums.EnumParticles;
import com.iafabric.message.MessageUpdatePixieHouse;
import com.iafabric.message.MessageUpdatePixieHouseModel;
import com.iafabric.message.MessageUpdatePixieJar;
import com.iafabric.misc.IafSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import net.minecraft.util.RandomSource;
import java.util.UUID;

public class TileEntityJar extends BlockEntity {

    private static final float PARTICLE_WIDTH = 0.3F;
    private static final float PARTICLE_HEIGHT = 0.6F;
    public boolean hasPixie;
    public boolean prevHasProduced;
    public boolean hasProduced;
    public boolean tamedPixie;
    public UUID pixieOwnerUUID;
    public int pixieType;
    public int ticksExisted;
    public NonNullList<ItemStack> pixieItems = NonNullList.withSize(1, ItemStack.EMPTY);
    public float rotationYaw;
    public float prevRotationYaw;
    private final RandomSource rand;

    public TileEntityJar(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.PIXIE_JAR, pos, state);
        this.rand = RandomSource.create();
        this.hasPixie = true;
    }

    public TileEntityJar(BlockPos pos, BlockState state, boolean empty) {
        super(IafTileEntityRegistry.PIXIE_JAR, pos, state);
        this.rand = RandomSource.create();
        this.hasPixie = !empty;
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
		ValueOutput compound = new ValueOutput(compoundTag, registries);
        compound.putBoolean("HasPixie", hasPixie);
        compound.putInt("PixieType", pixieType);
        compound.putBoolean("HasProduced", hasProduced);
        compound.putBoolean("TamedPixie", tamedPixie);
        if (pixieOwnerUUID != null) {
            compound.store("PixieOwnerUUID", UUIDUtil.CODEC, pixieOwnerUUID);
        }
        compound.putInt("TicksExisted", ticksExisted);
        ContainerHelper.saveAllItems(compound.tag(), this.pixieItems, compound.registries());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** What the client gets of the jar (its pixie): without it a placed or reloaded jar looked empty. */
    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }


    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider registries) {
		ValueInput compound = new ValueInput(compoundTag, registries);
        hasPixie = compound.getBooleanOr("HasPixie", false);
        pixieType = compound.getIntOr("PixieType", 0);
        hasProduced = compound.getBooleanOr("HasProduced", false);
        ticksExisted = compound.getIntOr("TicksExisted", 0);
        tamedPixie = compound.getBooleanOr("TamedPixie", false);
        if (compound.read("PixieOwnerUUID", UUIDUtil.CODEC).isPresent()) {
            pixieOwnerUUID = compound.read("PixieOwnerUUID", UUIDUtil.CODEC).orElse(null);
        }
        this.pixieItems = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compound.tag(), pixieItems, compound.registries());
        super.loadAdditional(compoundTag, registries);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityJar entityJar) {
        entityJar.ticksExisted++;
        if (level.isClientSide && entityJar.hasPixie) {
            IafMod.PROXY.spawnParticle(EnumParticles.If_Pixie,
                pos.getX() + 0.5F + (double) (entityJar.rand.nextFloat() * PARTICLE_WIDTH * 2F) - PARTICLE_WIDTH,
                pos.getY() + (double) (entityJar.rand.nextFloat() * PARTICLE_HEIGHT),
                pos.getZ() + 0.5F + (double) (entityJar.rand.nextFloat() * PARTICLE_WIDTH * 2F) - PARTICLE_WIDTH, EntityPixie.PARTICLE_RGB[entityJar.pixieType][0], EntityPixie.PARTICLE_RGB[entityJar.pixieType][1], EntityPixie.PARTICLE_RGB[entityJar.pixieType][2]);
        }
        if (entityJar.ticksExisted % 24000 == 0 && !entityJar.hasProduced && entityJar.hasPixie) {
            entityJar.hasProduced = true;
            if (!level.isClientSide) {
                IafNetwork.sendToAll(new MessageUpdatePixieJar(pos.asLong(), entityJar.hasProduced));
            }
        }
        if (entityJar.hasPixie && entityJar.hasProduced != entityJar.prevHasProduced && entityJar.ticksExisted > 5) {
            if (!level.isClientSide) {
                IafNetwork.sendToAll(new MessageUpdatePixieJar(pos.asLong(), entityJar.hasProduced));
            } else {
                level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5, IafSoundRegistry.PIXIE_HURT, SoundSource.BLOCKS, 1, 1, false);
            }
        }
        entityJar.prevRotationYaw = entityJar.rotationYaw;
        if (entityJar.rand.nextInt(30) == 0) {
            entityJar.rotationYaw = (entityJar.rand.nextFloat() * 360F) - 180F;
        }
        if (entityJar.hasPixie && entityJar.ticksExisted % 40 == 0 && entityJar.rand.nextInt(6) == 0 && level.isClientSide) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5, IafSoundRegistry.PIXIE_IDLE, SoundSource.BLOCKS, 1, 1, false);
        }
        entityJar.prevHasProduced = entityJar.hasProduced;
    }

    public void releasePixie() {
        EntityPixie pixie = new EntityPixie(IafEntityRegistry.PIXIE, this.level);
        pixie.absMoveTo(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 1F, this.worldPosition.getZ() + 0.5F, RandomSource.create().nextInt(360), 0);
        pixie.setItemInHand(InteractionHand.MAIN_HAND, pixieItems.get(0));
        pixie.setColor(this.pixieType);
        level.addFreshEntity(pixie);
        this.hasPixie = false;
        this.pixieType = 0;
        pixie.ticksUntilHouseAI = 500;
        pixie.setTame(this.tamedPixie, true);
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
