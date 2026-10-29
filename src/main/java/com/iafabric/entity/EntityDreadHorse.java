package com.iafabric.entity;

import net.minecraft.core.UUIDUtil;
import com.iafabric.entity.util.IafDataSerializers;
import net.minecraft.world.entity.MobSpawnType;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.entity.util.IDreadMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

public class EntityDreadHorse extends SkeletonHorse implements IDreadMob {

    protected static final EntityDataAccessor<Optional<UUID>> COMMANDER_UNIQUE_ID = SynchedEntityData.defineId(EntityDreadHorse.class, IafDataSerializers.OPTIONAL_UUID);

    public EntityDreadHorse(EntityType type, Level worldIn) {
        super(type, worldIn);
    }


    public static AttributeSupplier.Builder bakeAttributes() {
        return createBaseHorseAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 25.0D)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            //ARMOR
            .add(Attributes.ARMOR, 4.0D);
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COMMANDER_UNIQUE_ID, Optional.empty());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
		ValueOutput compound = new ValueOutput(compoundTag, this.level().registryAccess());
        super.addAdditionalSaveData(compoundTag);
        if (this.getCommanderId() != null) {
            compound.store("CommanderUUID", UUIDUtil.CODEC, this.getCommanderId());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
		ValueInput compound = new ValueInput(compoundTag, this.level().registryAccess());
        super.readAdditionalSaveData(compoundTag);
        UUID uuid;
        if (compound.read("CommanderUUID", UUIDUtil.CODEC).isPresent()) {
            uuid = compound.read("CommanderUUID", UUIDUtil.CODEC).orElse(null);
        } else {
            String s = compound.getStringOr("CommanderUUID", "");
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.level().getServer(), s);
        }

        if (uuid != null) {
            try {
                this.setCommanderId(uuid);
            } catch (Throwable throwable) {
            }
        }

    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag) {
        SpawnGroupData data = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
        this.setAge(24000);
        return data;
    }

    @Override
    public boolean isAlliedTo(Entity entityIn) {
        return entityIn instanceof IDreadMob || super.isAlliedTo(entityIn);
    }

    @Nullable
    public UUID getCommanderId() {
        return this.entityData.get(COMMANDER_UNIQUE_ID).orElse(null);
    }

    public void setCommanderId(@Nullable UUID uuid) {
        this.entityData.set(COMMANDER_UNIQUE_ID, Optional.ofNullable(uuid));
    }

    @Override
    public Entity getCommander() {
        try {
            UUID uuid = this.getCommanderId();
            return uuid == null ? null : this.level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException var2) {
            return null;
        }
    }

    }
