package com.iafabric.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import com.iafabric.util.EntityUtil;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.iafabric.network.IafNetwork;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.ai.*;
import com.iafabric.entity.tile.TileEntityPixieHouse;
import com.iafabric.enums.EnumParticles;
import com.iafabric.message.MessageUpdatePixieHouse;
import com.iafabric.misc.IafSoundRegistry;
import com.google.common.base.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import net.minecraft.util.RandomSource;

public class EntityPixie extends TamableAnimal {

    public static final float[][] PARTICLE_RGB = new float[][]{new float[]{1F, 0.752F, 0.792F}, new float[]{0.831F, 0.662F, 1F}, new float[]{0.513F, 0.843F, 1F}, new float[]{0.654F, 0.909F, 0.615F}, new float[]{0.996F, 0.788F, 0.407F}};
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(EntityPixie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMMAND = SynchedEntityData.defineId(EntityPixie.class, EntityDataSerializers.INT);
    /** Bonded to its player (com.iafabric.pixie.PixieBonding). */
    private static final EntityDataAccessor<Boolean> BONDED = SynchedEntityData.defineId(EntityPixie.class, EntityDataSerializers.BOOLEAN);

    public static final int STEAL_COOLDOWN = 3000;

    public MobEffect[] positivePotions = new MobEffect[]{MobEffects.STRENGTH.value(), MobEffects.JUMP_BOOST.value(), MobEffects.SPEED.value(), MobEffects.LUCK.value(), MobEffects.HASTE.value()};
    public MobEffect[] negativePotions = new MobEffect[]{MobEffects.WEAKNESS.value(), MobEffects.NAUSEA.value(), MobEffects.SLOWNESS.value(), MobEffects.UNLUCK.value(), MobEffects.MINING_FATIGUE.value()};
    public boolean slowSpeed = false;
    /** An oracle pixie (DragonSkies): how long it has been here, -1 for a normal pixie. It stays still and goes after ten seconds. */
    public int oracleTicks = -1;
    /** The token of its player's bonds when it came out: a pixie with an old token is a copy and goes. */
    public long bondToken;

    public boolean isBonded() {
        return this.entityData.get(BONDED);
    }

    public void setBonded(boolean bonded) {
        this.entityData.set(BONDED, bonded);
    }
    public int ticksUntilHouseAI;
    public int ticksHeldItemFor;
    private BlockPos housePos;
    public int stealCooldown = 0;
    private boolean isSitting;

    public EntityPixie(EntityType type, Level worldIn) {
        super(type, worldIn);
        this.moveControl = new EntityPixie.AIMoveControl(this);
        this.xpReward = 3;
        this.setDropChance(EquipmentSlot.MAINHAND, 0F);
    }

    public static BlockPos getPositionRelativetoGround(Entity entity, Level world, double x, double z, RandomSource rand) {
        BlockPos pos = BlockPos.containing(x, entity.getY(), z);
        for (int yDown = 0; yDown < 3; yDown++) {
            if (!world.isEmptyBlock(pos.below(yDown))) {
                return pos.above(yDown);
            }
        }
        return pos;
    }

    public static BlockPos findAHouse(Entity entity, Level world) {
        for (int xSearch = -10; xSearch < 10; xSearch++) {
            for (int ySearch = -10; ySearch < 10; ySearch++) {
                for (int zSearch = -10; zSearch < 10; zSearch++) {
                    if (world.getBlockEntity(entity.blockPosition().offset(xSearch, ySearch, zSearch)) != null && world.getBlockEntity(entity.blockPosition().offset(xSearch, ySearch, zSearch)) instanceof TileEntityPixieHouse) {
                        TileEntityPixieHouse house = (TileEntityPixieHouse) world.getBlockEntity(entity.blockPosition().offset(xSearch, ySearch, zSearch));
                        if (!house.hasPixie) {
                            return entity.blockPosition().offset(xSearch, ySearch, zSearch);
                        }
                    }
                }
            }
        }
        return entity.blockPosition();
    }

    public boolean isPixieSitting() {
        if (level().isClientSide()) {
            boolean isSitting = (this.entityData.get(DATA_FLAGS_ID).byteValue() & 1) != 0;
            this.isSitting = isSitting;
            this.setOrderedToSit(isSitting);
            return isSitting;
        }
        return this.isSitting;
    }

    public void setPixieSitting(boolean sitting) {
        if (!level().isClientSide()) {
            this.isSitting = sitting;
            this.setInSittingPose(sitting);
        }
        byte b0 = this.entityData.get(DATA_FLAGS_ID).byteValue();
        if (sitting) {
            this.entityData.set(DATA_FLAGS_ID, (byte) (b0 | 1));
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte) (b0 & -2));
        }
    }

    @Override
    public boolean isOrderedToSit() {
        return this.isPixieSitting();
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 3;
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 10D)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isBonded()) {
            // a bonded pixie is not hurt: its player takes a part of it
            com.iafabric.pixie.PixieBonding.onBondedPixieHurt(this, level, source, amount);
            return false;
        }
        if (!this.level().isClientSide() && this.getRandom().nextInt(3) == 0 && !this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            this.spawnAtLocation((ServerLevel) this.level(), this.getItemInHand(InteractionHand.MAIN_HAND), 0);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.stealCooldown = STEAL_COOLDOWN;
            return true;
        }
        if (this.isOwnerClose() && (source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.IN_WALL) || this.getOwner() != null && source.getEntity() == this.getOwner())) {
            return false;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        boolean invulnerable = super.isInvulnerableTo(level, source);
        if (!invulnerable) {
            Entity owner = this.getOwner();
            if (owner != null && source.getEntity() == owner) {
                return true;
            }
        }
        return invulnerable;
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        if (!this.level().isClientSide() && !this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
            this.spawnAtLocation((ServerLevel) this.level(), this.getItemInHand(InteractionHand.MAIN_HAND), 0);
            this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        super.die(cause);
        //if (cause.getTrueSource() instanceof PlayerEntity) {
        //	((PlayerEntity) cause.getTrueSource()).addStat(ModAchievements.killPixie);
        //}
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COLOR, 0);
        builder.define(COMMAND, 0);
        builder.define(BONDED, false);
    }

    @Override
    protected void doPush(@NotNull Entity entityIn) {
        if (this.getOwner() != entityIn) {
            entityIn.push(this);
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGroundIn, @NotNull BlockState state, @NotNull BlockPos pos) {
    }

    @Override
    public InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (this.isOwnedBy(player)) {

            if (player.getItemInHand(hand).getItem() == Items.SUGAR && this.getHealth() < this.getMaxHealth()) {
                this.heal(5);
                player.getItemInHand(hand).shrink(1);
                this.playSound(IafSoundRegistry.PIXIE_TAUNT, 1F, 1F);
                return InteractionResult.SUCCESS;
            } else {

                // make pixie sit via a check in livingTick() like Hippogryphs work
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() > 1) {
                    this.setCommand(0);
                }

                return InteractionResult.SUCCESS;
            }
        } else if (player.getItemInHand(hand).getItem() == IafBlockRegistry.JAR_EMPTY.asItem() && !this.isTame()) {
            if (!player.isCreative()) {
                player.getItemInHand(hand).shrink(1);
            }
            Block jar = IafBlockRegistry.JAR_PIXIE_0;
            switch (this.getColor()) {
                case 0:
                    jar = IafBlockRegistry.JAR_PIXIE_0;
                    break;
                case 1:
                    jar = IafBlockRegistry.JAR_PIXIE_1;
                    break;
                case 2:
                    jar = IafBlockRegistry.JAR_PIXIE_2;
                    break;
                case 3:
                    jar = IafBlockRegistry.JAR_PIXIE_3;
                    break;
                case 4:
                    jar = IafBlockRegistry.JAR_PIXIE_4;
                    break;
            }
            ItemStack stack = new ItemStack(jar, 1);
            if (!level().isClientSide()) {
                if (!this.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                    this.spawnAtLocation((ServerLevel) this.level(), this.getItemInHand(InteractionHand.MAIN_HAND), 0.0F);
                    this.stealCooldown = STEAL_COOLDOWN;
                }

                this.spawnAtLocation((ServerLevel) this.level(), stack, 0.0F);
            }
            //player.addStat(ModAchievements.jarPixie);
            this.remove(RemovalReason.DISCARDED);
        }
        return super.mobInteract(player, hand);
    }

    public void flipAI(boolean flee) {
    }

    public void fall(float distance, float damageMultiplier) {
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PixieAIFollowOwner(this, 1.0D, 2.0F, 4.0F));
        this.goalSelector.addGoal(2, new PixieAIPickupItem(this, false));
        this.goalSelector.addGoal(2, new PixieAIFlee(this, Player.class, 10, entity -> true));
        this.goalSelector.addGoal(2, new PixieAISteal(this, 1.0D));
        this.goalSelector.addGoal(3, new PixieAIMoveRandom(this));
        this.goalSelector.addGoal(4, new PixieAIEnterHouse(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, EntitySpawnReason reason, @Nullable SpawnGroupData spawnDataIn) {
        spawnDataIn = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn);
        this.setColor(this.random.nextInt(5));
        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        return spawnDataIn;
    }

    /** Going for an item on the ground (cake, sugar): it stops floating up meanwhile. */
    public boolean fetchingItem;

    private boolean isBeyondHeight() {
        if (this.getY() > this.level().getMaxY()) {
            return true;
        }
        BlockPos height = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.blockPosition());
        int maxY = 20 + height.getY();
        return this.getY() > maxY;
    }

    /** Up and following its owner (not sitting, whatever it was told before). */
    public void standUp() {
        this.setCommand(0);
        this.setPixieSitting(false);
        this.setOrderedToSit(false);
        this.setInSittingPose(false);
    }

    public int getCommand() {
        return this.entityData.get(COMMAND).intValue();
    }

    public void setCommand(int command) {
        this.entityData.set(COMMAND, command);
        this.setPixieSitting(command == 1);
    }


    @Override
    public void aiStep() {
        if (this.oracleTicks >= 0) {
            this.setDeltaMovement(0, Math.sin(this.tickCount * 0.15) * 0.02, 0);
            if (!this.level().isClientSide() && ++this.oracleTicks > 200) {
                if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, this.getX(), this.getY() + 0.3, this.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
                }
                this.discard();
            }
            super.aiStep();
            return;
        }
        super.aiStep();

        if (!this.level().isClientSide()) {

            // NOTE: This code was taken from EntityHippogryph basically same idea
            if (this.isPixieSitting() && this.getCommand() != 1) {
                this.setPixieSitting(false);
            }
            if (!this.isPixieSitting() && this.getCommand() == 1) {
                this.setPixieSitting(true);
            }
            if (this.isPixieSitting()) {
                this.getNavigation().stop();
            }
        }

        if (stealCooldown > 0) {
            stealCooldown--;
        }
        if (!this.getMainHandItem().isEmpty() && !this.isTame()) {
            ticksHeldItemFor++;
        } else {
            ticksHeldItemFor = 0;
        }


        if (!this.isPixieSitting() && !this.isBeyondHeight() && !this.fetchingItem) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.08, 0));
        }
        // A little dust (one tick in three, it hid the pixie when it came every tick)
        if (level().isClientSide() && this.random.nextInt(3) == 0) {
            float[] dust = this.dustColor();
            IafMod.PROXY.spawnParticle(EnumParticles.If_Pixie, this.getX() + (double) (this.random.nextFloat() * this.getBbWidth() * 2F) - (double) this.getBbWidth(), this.getY() + (double) (this.random.nextFloat() * this.getBbHeight()), this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth() * 2F) - (double) this.getBbWidth(), dust[0], dust[1], dust[2]);
        }
        if (ticksUntilHouseAI > 0) {
            ticksUntilHouseAI--;
        }
        if (!level().isClientSide()) {
            if (housePos != null && this.distanceToSqr(Vec3.atCenterOf(housePos)) < 1.5F && level().getBlockEntity(housePos) != null && level().getBlockEntity(housePos) instanceof TileEntityPixieHouse) {
                TileEntityPixieHouse house = (TileEntityPixieHouse) level().getBlockEntity(housePos);
                if (house.hasPixie) {
                    this.housePos = null;
                } else {
                    house.hasPixie = true;
                    house.pixieType = this.getColor();
                    house.pixieItems.set(0, this.getItemInHand(InteractionHand.MAIN_HAND));
                    house.tamedPixie = this.isTame();
                    house.pixieOwnerUUID = EntityUtil.getOwnerUUID(this);
                    IafNetwork.sendToAll(new MessageUpdatePixieHouse(housePos.asLong(), true, this.getColor()));
                    this.remove(RemovalReason.DISCARDED);
                }
            }
        }
        if (this.isBonded() && this.level() instanceof ServerLevel server) {
            // only the one its player has out lives (an older copy goes); out of the world it comes back to its player
            if (!(this.getOwner() instanceof net.minecraft.server.level.ServerPlayer owner)
                || com.iafabric.pixie.PixieBonding.get(owner).active != this.getColor() || com.iafabric.pixie.PixieBonding.get(owner).token != this.bondToken) {
                this.discard();
                return;
            }
            if (this.getY() < server.getMinY() + 2) {
                this.teleportTo(owner.getX(), owner.getY() + 1.5, owner.getZ());
            }
        }
        if (this.getOwner() != null && this.isOwnerClose() && this.tickCount % 80 == 0 && !this.isBonded() && this.givesOwnerEffect()) {
            this.getOwner().addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(positivePotions[this.getColor()]), 100, 0, false, false));
        }
        //PlayerEntity player = world.getClosestPlayerToEntity(this, 25);
        //if (player != null) {
        //	player.addStat(ModAchievements.findPixie);
        //}
    }

    public int getColor() {
        return Mth.clamp(this.getEntityData().get(COLOR).intValue(), 0, 4);
    }

    /** The color of its dust (red, green, blue from 0 to 1). */
    protected float[] dustColor() {
        return PARTICLE_RGB[this.getColor()];
    }

    /** A tamed pixie near its owner gives them its color's effect now and then. */
    protected boolean givesOwnerEffect() {
        return true;
    }

    public void setColor(int color) {
        this.getEntityData().set(COLOR, color);
    }


    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        // the tamable part first: it sets the sitting pose from its own flag, which must not undo the pixie's
        super.readAdditionalSaveData(compound);
        this.setColor(compound.getIntOr("Color", 0));

        this.stealCooldown = compound.getIntOr("StealCooldown", 0);
        this.ticksHeldItemFor = compound.getIntOr("HoldingTicks", 0);

        this.setPixieSitting(compound.getBooleanOr("PixieSitting", false));
        this.setCommand(compound.getIntOr("Command", 0));
        this.oracleTicks = compound.getIntOr("OracleTicks", -1);
        this.setBonded(compound.getBooleanOr("Bonded", false));
        this.bondToken = compound.getLongOr("BondToken", 0L);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput compound) {
        compound.putInt("Color", this.getColor());
        compound.putInt("Command", this.getCommand());
        compound.putInt("StealCooldown", this.stealCooldown);
        compound.putInt("HoldingTicks", this.ticksHeldItemFor);
        compound.putBoolean("PixieSitting", this.isPixieSitting());
        compound.putInt("OracleTicks", this.oracleTicks);
        compound.putBoolean("Bonded", this.isBonded());
        compound.putLong("BondToken", this.bondToken);
        super.addAdditionalSaveData(compound);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel serverWorld, @NotNull AgeableMob ageable) {
        return null;
    }

    public void setHousePosition(BlockPos blockPos) {
        this.housePos = blockPos;
    }

    public BlockPos getHousePos() {
        return housePos;
    }

    public boolean isOwnerClose() {
        return this.isTame() && this.getOwner() != null && this.distanceToSqr(this.getOwner()) < 100;
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return IafSoundRegistry.PIXIE_IDLE;
    }

    @Override
    @Nullable
    protected SoundEvent getHurtSound(@NotNull DamageSource damageSourceIn) {
        return IafSoundRegistry.PIXIE_HURT;
    }

    @Override
    @Nullable
    protected SoundEvent getDeathSound() {
        return IafSoundRegistry.PIXIE_DIE;
    }

    @Override
    protected boolean considersEntityAsAlly(Entity entityIn) {
        if (this.isTame()) {
            LivingEntity livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }
            if (entityIn instanceof TamableAnimal) {
                return ((TamableAnimal) entityIn).isOwnedBy(livingentity);
            }
            if (livingentity != null) {
                return livingentity.isAlliedTo(entityIn);
            }
        }

        return super.considersEntityAsAlly(entityIn);
    }

    class AIMoveControl extends MoveControl {
        public AIMoveControl(EntityPixie pixie) {
            super(pixie);
        }

        @Override
        public void tick() {
            float speedMod = 1;
            if (EntityPixie.this.slowSpeed) {
                speedMod = 2F;
            }
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                if (EntityPixie.this.horizontalCollision) {
                    EntityPixie.this.setYRot(this.mob.getYRot() + 180.0F);
                    speedMod = 0.1F;
                    BlockPos target = EntityPixie.getPositionRelativetoGround(EntityPixie.this, EntityPixie.this.level(), EntityPixie.this.getX() + EntityPixie.this.random.nextInt(15) - 7, EntityPixie.this.getZ() + EntityPixie.this.random.nextInt(15) - 7, EntityPixie.this.random);
                    this.wantedX = target.getX();
                    this.wantedY = target.getY();
                    this.wantedZ = target.getZ();
                }
                double d0 = this.wantedX - EntityPixie.this.getX();
                double d1 = this.wantedY - EntityPixie.this.getY();
                double d2 = this.wantedZ - EntityPixie.this.getZ();
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                d3 = Math.sqrt(d3);

                if (d3 < EntityPixie.this.getBoundingBox().getSize()) {
                    this.operation = MoveControl.Operation.WAIT;
                    EntityPixie.this.setDeltaMovement(EntityPixie.this.getDeltaMovement().multiply(0.5D, 0.5D, 0.5D));
                } else {
                    EntityPixie.this.setDeltaMovement(EntityPixie.this.getDeltaMovement().add(d0 / d3 * 0.05D * this.speedModifier * speedMod, d1 / d3 * 0.05D * this.speedModifier * speedMod, d2 / d3 * 0.05D * this.speedModifier * speedMod));

                    if (EntityPixie.this.getTarget() == null) {
                        EntityPixie.this.setYRot(-((float) Mth.atan2(EntityPixie.this.getDeltaMovement().x, EntityPixie.this.getDeltaMovement().z)) * (180F / (float) Math.PI));
                        EntityPixie.this.yBodyRot = EntityPixie.this.getYRot();
                    } else {
                        double d4 = EntityPixie.this.getTarget().getX() - EntityPixie.this.getX();
                        double d5 = EntityPixie.this.getTarget().getZ() - EntityPixie.this.getZ();
                        EntityPixie.this.setYRot(-((float) Mth.atan2(d4, d5)) * (180F / (float) Math.PI));
                        EntityPixie.this.yBodyRot = EntityPixie.this.getYRot();
                    }
                }
            }
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT);
    }
}
