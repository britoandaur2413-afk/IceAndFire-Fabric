package com.iafabric.entity.tile;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import com.iafabric.util.EntityUtil;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.IafConfig;
import com.iafabric.entity.EntityDragonEgg;
import com.iafabric.entity.EntityIceDragon;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.enums.EnumDragonEgg;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TileEntityEggInIce extends BlockEntity {
    public EnumDragonEgg type;
    public int age;
    public int ticksExisted;
    @Nullable
    public UUID ownerUUID;
    // boolean to prevent time in a bottle shenanigans
    private boolean spawned;

    public TileEntityEggInIce(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.EGG_IN_ICE, pos, state);
    }

    public static void tickEgg(Level level, BlockPos pos, BlockState state, TileEntityEggInIce entityEggInIce) {
        entityEggInIce.age++;
        if (entityEggInIce.age >= IafConfig.dragonEggTime && entityEggInIce.type != null && !entityEggInIce.spawned) {
            if (!level.isClientSide) {
                EntityIceDragon dragon = new EntityIceDragon(level);
                dragon.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                dragon.setVariant(entityEggInIce.type.ordinal() - 4);
                dragon.setGender(ThreadLocalRandom.current().nextBoolean());
                dragon.setTame(true, true);
                dragon.setHunger(50);
                EntityUtil.setOwnerUUID(dragon, entityEggInIce.ownerUUID);
                level.addFreshEntity(dragon);
                entityEggInIce.spawned = true;
                level.destroyBlock(pos, false);
                level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            }

        }
        entityEggInIce.ticksExisted++;
    }

    @Override
    protected void saveAdditional(CompoundTag tagTag, HolderLookup.Provider registries) {
		ValueOutput tag = new ValueOutput(tagTag, registries);
        if (type != null) {
            tag.putByte("Color", (byte) type.ordinal());
        } else {
            tag.putByte("Color", (byte) 0);
        }
        tag.putInt("Age", age);
        if (ownerUUID != null) {
            tag.store("OwnerUUID", UUIDUtil.CODEC, ownerUUID);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tagTag, HolderLookup.Provider registries) {
		ValueInput tag = new ValueInput(tagTag, registries);
        super.loadAdditional(tagTag, registries);
        type = EnumDragonEgg.values()[tag.getByteOr("Color", (byte) 0)];
        age = tag.getIntOr("Age", 0);
        UUID s = null;

        s = tag.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
        if (s != null) {
            ownerUUID = s;
        }
    }


    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    public void spawnEgg() {
        if (type != null) {
            EntityDragonEgg egg = new EntityDragonEgg(IafEntityRegistry.DRAGON_EGG, level);
            egg.setEggType(type);
            egg.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
            egg.setOwnerId(this.ownerUUID);
            if (!level.isClientSide) {
                level.addFreshEntity(egg);
            }
        }
    }
}
