package com.iafabric.entity;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.UUIDUtil;
import com.iafabric.entity.util.IafDataSerializers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.entity.util.IDreadMob;
import com.iafabric.entity.util.IHumanoid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.UUID;

public class EntityDreadMob extends Monster implements IDreadMob {
    protected static final EntityDataAccessor<Optional<UUID>> COMMANDER_UNIQUE_ID = SynchedEntityData.defineId(EntityDreadMob.class, IafDataSerializers.OPTIONAL_UUID);

    public EntityDreadMob(EntityType<? extends Monster> t, Level worldIn) {
        super(t, worldIn);
    }

    public static Entity necromancyEntity(LivingEntity entity) {
        Entity lichSummoned = null;
        if (entity.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD) {
            lichSummoned = new EntityDreadScuttler(IafEntityRegistry.DREAD_SCUTTLER, entity.level());
            float readInScale = (entity.getBbWidth() / 1.5F);
            if (entity.level() instanceof ServerLevelAccessor) {
                ((EntityDreadScuttler) lichSummoned).finalizeSpawn((ServerLevelAccessor) entity.level(), ((ServerLevelAccessor) entity.level()).getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            }
            ((EntityDreadScuttler) lichSummoned).setSize(readInScale);
            return lichSummoned;
        }
        if (entity instanceof Zombie || entity instanceof IHumanoid) {
            lichSummoned = new EntityDreadGhoul(IafEntityRegistry.DREAD_GHOUL, entity.level());
            float readInScale = (entity.getBbWidth() / 0.6F);
            if (entity.level() instanceof ServerLevelAccessor) {
                ((EntityDreadGhoul) lichSummoned).finalizeSpawn((ServerLevelAccessor) entity.level(), ((ServerLevelAccessor) entity.level()).getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            }
            ((EntityDreadGhoul) lichSummoned).setSize(readInScale);
            return lichSummoned;
        }
        if (entity.getMobType() == net.minecraft.world.entity.MobType.UNDEAD || entity instanceof AbstractSkeleton || entity instanceof Player) {
            lichSummoned = new EntityDreadThrall(IafEntityRegistry.DREAD_THRALL, entity.level());
            EntityDreadThrall thrall = (EntityDreadThrall) lichSummoned;
            if (entity.level() instanceof ServerLevelAccessor) {
                thrall.finalizeSpawn((ServerLevelAccessor) entity.level(), ((ServerLevelAccessor) entity.level()).getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            }
            thrall.setCustomArmorHead(false);
            thrall.setCustomArmorChest(false);
            thrall.setCustomArmorLegs(false);
            thrall.setCustomArmorFeet(false);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                thrall.setItemSlot(slot, entity.getItemBySlot(slot));
            }
            return thrall;
        }
        if (entity instanceof AbstractHorse) {
            lichSummoned = new EntityDreadHorse(IafEntityRegistry.DREAD_HORSE, entity.level());
            return lichSummoned;
        }
        if (entity instanceof Animal) {
            lichSummoned = new EntityDreadBeast(IafEntityRegistry.DREAD_BEAST, entity.level());
            float readInScale = (entity.getBbWidth() / 1.2F);
            if (entity.level() instanceof ServerLevelAccessor) {
                ((EntityDreadBeast) lichSummoned).finalizeSpawn((ServerLevelAccessor) entity.level(), ((ServerLevelAccessor) entity.level()).getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            }
            ((EntityDreadBeast) lichSummoned).setSize(readInScale);
            return lichSummoned;
        }
        return lichSummoned;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COMMANDER_UNIQUE_ID, Optional.empty());
    }

    /** A dead one called for a while (by Cryona): gone in a puff of cold when its time is up (0: for good). */
    private long summonedUntil;

    public void summonFor(int ticks) {
        this.summonedUntil = this.level().getGameTime() + ticks;
    }

    public boolean isSummoned() {
        return this.summonedUntil > 0;
    }

    @Override
    protected boolean shouldDropLoot() {
        return !this.isSummoned() && super.shouldDropLoot();
    }

    @Override
    public int getExperienceReward() {
        return this.isSummoned() ? 0 : super.getExperienceReward();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
		ValueOutput compound = new ValueOutput(compoundTag, this.level().registryAccess());
        super.addAdditionalSaveData(compoundTag);
        if (this.getCommanderId() != null) {
            compound.store("CommanderUUID", UUIDUtil.CODEC, this.getCommanderId());
        }
        if (this.summonedUntil > 0) {
            compound.putLong("SummonedUntil", this.summonedUntil);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
		ValueInput compound = new ValueInput(compoundTag, this.level().registryAccess());
        super.readAdditionalSaveData(compoundTag);
        this.summonedUntil = compound.getLongOr("SummonedUntil", 0L);
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
    public boolean isAlliedTo(Entity entityIn) {
        return entityIn instanceof IDreadMob || super.isAlliedTo(entityIn);
    }

    @Nullable
    public UUID getCommanderId() {
        return (UUID) ((Optional) this.entityData.get(COMMANDER_UNIQUE_ID)).orElse(null);
    }

    public void setCommanderId(@Nullable UUID uuid) {
        this.entityData.set(COMMANDER_UNIQUE_ID, Optional.ofNullable(uuid));
    }

    @Override
    public void aiStep() {
        if (this.summonedUntil > 0 && this.level() instanceof net.minecraft.server.level.ServerLevel server && server.getGameTime() >= this.summonedUntil) {
            // its time is up: back to the cold
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 1, this.getZ(), 20, 0.3, 0.6, 0.3, 0.04);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, this.getX(), this.getY() + 0.5, this.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
            this.discard();
            return;
        }
        super.aiStep();
        // a lich's minions, or the Dread Queen's, fight what their master fights
        if (!level().isClientSide && (this.getCommander() instanceof EntityDreadLich || this.getCommander() instanceof EntityDreadQueen)) {
            Mob master = (Mob) this.getCommander();
            if (master.getTarget() != null && master.getTarget().isAlive()) {
                this.setTarget(master.getTarget());
            }
        }
    }

    @Override
    public Entity getCommander() {
        try {
            UUID uuid = this.getCommanderId();
            LivingEntity player = uuid == null ? null : this.level().getPlayerByUUID(uuid);
            if (player != null) {
                return player;
            } else {
                if (!level().isClientSide) {
                    Entity entity = level().getServer().getLevel(this.level().dimension()).getEntity(uuid);
                    if (entity instanceof LivingEntity) {
                        return entity;
                    }
                }
            }
        } catch (IllegalArgumentException var2) {
            return null;
        }
        return null;
    }

    public void onKillEntity(LivingEntity LivingEntityIn) {
        Entity commander = this instanceof EntityDreadLich ? this : this.getCommander();
        if (commander != null && !(LivingEntityIn instanceof EntityDragonBase)) {// zombie dragons!!!!
            Entity summoned = necromancyEntity(LivingEntityIn);
            if (summoned != null) {
                summoned.copyPosition(LivingEntityIn);
                if (!level().isClientSide) {
                    level().addFreshEntity(summoned);
                }
                if (commander instanceof EntityDreadLich) {
                    ((EntityDreadLich) commander).setMinionCount(((EntityDreadLich) commander).getMinionCount() + 1);
                }
                if (summoned instanceof EntityDreadMob) {
                    ((EntityDreadMob) summoned).setCommanderId(commander.getUUID());
                }
            }
        }

    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!isRemoved() && this.getCommander() != null && this.getCommander() instanceof EntityDreadLich) {
            EntityDreadLich lich = (EntityDreadLich) this.getCommander();
            lich.setMinionCount(lich.getMinionCount() - 1);
        }
        super.remove(reason);
    }

    }
