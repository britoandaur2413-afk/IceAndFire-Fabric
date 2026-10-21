package com.iafabric.entity;


import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionResult;
import com.iafabric.util.EntityUtil;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.util.profiling.Profiler;
import com.iafabric.util.ItemNbt;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.iafabric.network.IafNetwork;
import com.iafabric.util.BlockMaterial;
import com.iafabric.citadel.animation.Animation;
import com.iafabric.citadel.animation.AnimationHandler;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.api.FoodUtils;
import com.iafabric.block.IDragonProof;
import com.iafabric.entity.util.IFChainBuffer;
import com.iafabric.entity.util.LegSolverQuadruped;
import com.iafabric.entity.ai.*;
import com.iafabric.entity.props.ChainProperties;
import com.iafabric.entity.props.MiscProperties;
import com.iafabric.entity.tile.TileEntityDragonforgeInput;
import com.iafabric.entity.util.*;
import com.iafabric.enums.EnumDragonEgg;
import com.iafabric.inventory.ContainerDragon;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemDragonArmor;
import com.iafabric.item.ItemSummoningCrystal;
import com.iafabric.message.MessageDragonSetBurnBlock;
import com.iafabric.message.MessageStartRidingMob;
import com.iafabric.misc.IafSoundRegistry;
import com.iafabric.pathfinding.raycoms.AdvancedPathNavigate;
import com.iafabric.pathfinding.raycoms.IPassabilityNavigator;
import com.iafabric.pathfinding.raycoms.PathingStuckHandler;
import com.iafabric.pathfinding.raycoms.pathjobs.ICustomSizeNavigator;
import com.iafabric.world.DragonPosWorldData;
import com.google.common.base.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import java.util.List;
import net.minecraft.util.RandomSource;
import java.util.UUID;

public abstract class EntityDragonBase extends TamableAnimal implements IPassabilityNavigator, ISyncMount, IFlyingMount, IMultipartEntity, IAnimatedEntity, IDragonFlute, IDeadMob, IVillagerFear, IAnimalFear, IDropArmor, IHasCustomizableAttributes, ICustomSizeNavigator, ICustomMoveController {

    public static final int FLIGHT_CHANCE_PER_TICK = 1500;
    protected static final EntityDataAccessor<Boolean> SWIMMING = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE_TICKS = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GENDER = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FIREBREATHING = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> HOVERING = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MODEL_DEAD = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DEATH_STAGE = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> CONTROL_STATE = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> TACKLE = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> AGINGDISABLED = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> COMMAND = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DRAGON_PITCH = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> CRYSTAL_BOUND = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> CUSTOM_POSE = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.STRING);
    /** The look of a dragon of the dread: 0 none, 1 a dread rider's undead dragon, 2 Black Frost, the Dread Queen's own. */
    private static final EntityDataAccessor<Byte> DREAD_LOOK = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BYTE);
    /** The rider drawn on its back (DragonRiders): 0 none, 1 a pillager, 2 the warlord, 3 a dread knight. */
    private static final EntityDataAccessor<Byte> VIRTUAL_RIDER = SynchedEntityData.defineId(EntityDragonBase.class, EntityDataSerializers.BYTE);
    public static Animation ANIMATION_FIRECHARGE;
    public static Animation ANIMATION_EAT;
    public static Animation ANIMATION_SPEAK;
    public static Animation ANIMATION_BITE;
    public static Animation ANIMATION_SHAKEPREY;
    public static Animation ANIMATION_WINGBLAST;
    public static Animation ANIMATION_ROAR;
    public static Animation ANIMATION_EPIC_ROAR;
    public static Animation ANIMATION_TAILWHACK;
    public DragonType dragonType;
    public double minimumDamage;
    public double maximumDamage;
    public double minimumHealth;
    public double maximumHealth;
    public double minimumSpeed;
    public double maximumSpeed;
    public double minimumArmor;
    public double maximumArmor;
    public float sitProgress;
    public float sleepProgress;
    public float hoverProgress;
    public float flyProgress;
    public float fireBreathProgress;
    public float diveProgress;
    public float prevDiveProgress;
    public float prevFireBreathProgress;
    public int fireStopTicks;
    public int flyTicks;
    public float modelDeadProgress;
    public float prevModelDeadProgress;
    public float ridingProgress;
    public float tackleProgress;
    /*
    0 = sit
    1 = sleep
    2 = hover
    3 = fly
    4 = fireBreath
    5 = riding
    6 = tackle
     */
    public boolean isSwimming;
    public float prevSwimProgress;
    public float swimProgress;
    public int ticksSwiming;
    public int swimCycle;
    public float[] prevAnimationProgresses = new float[10];
    public boolean isDaytime;
    public int flightCycle;
    public HomePosition homePos;
    public boolean hasHomePosition = false;
    /** The player who hurt it (DragonSmarts), its target until revengeUntil. */
    public java.util.UUID revengePlayer;
    public long revengeUntil;
    public int hopCooldown;
    public int stuckInBlocksTicks;
    /** A young dragon of the open sky (DragonSkies): it only fights who hurts it. */
    public boolean skyWanderer;
    /** A dragon of a wave sent after a player who killed sky dragons. */
    public boolean waveDragon;
    /** When a sky or wave dragon may leave (game time), 0 for never. */
    public long despawnAt;
    /** A dragon of a pillager kingdom (PillagerDragons): ridden by pillagers, never tamed, breaks no blocks. */
    public boolean pillagerMount;
    /** A dragon of the dread (Black Frost, the dread riders' dragons): undead, ridden by the dread. */
    public boolean dreadMount;
    /**
     * Its boss fight leads it (Black Frost in its realm, BlackFrostFight): its own mind takes no part (no goals, no
     * steps, no foe of its own: MobFightLedMixin, IafDragonLogic), yet it is no "NoAi" mob, whose model stands stiff (no
     * look, no breath, no beat of its wings).
     */
    public boolean fightLed;
    /** Black Frost asleep at Cryona's throne in an older world (itself, lying still): her guard takes its place. */
    boolean oldSleeper;
    /** The elite of a pillager kingdom: bigger, in netherite, with frost powers its rider uses. */
    public boolean eliteMount;
    /** Ticks a flying dragon has made no way (DragonSmarts gives it another way, then lands it). */
    public int flightStuckTicks;
    /** Ticks a kingdom dragon has had nothing to fight (it sits down after a while). */
    public int calmTicks;
    /** Ticks it has walked without getting anywhere, and where from (DragonSmarts). */
    public int walkStuckTicks;
    public Vec3 walkStuckFrom;
    /** The health its rider has left (DragonRiders). */
    public float riderHealth;
    /** Its rider's own hitbox, while it has a rider. */
    public EntityDragonPart riderPart;
    /** The one it goes to for a new rider, since when, and until when it gives up looking (DragonRiders). */
    public java.util.UUID riderCandidate;
    public long riderSearchSince;
    public long riderGiveUpUntil;
    /** It goes straight to the one who will ride it (no path found to them). */
    public boolean walkingToRider;
    /** How near it has come to the one who will ride it, and when it last came nearer (DragonRiders). */
    public double riderBestDistance;
    public long riderProgressAt;
    /** No rider and nobody to ride it near: it fights with its breath (DragonRiders). */
    public boolean berserk;
    /** Ticks until it may call its element down again (DragonSpecials). */
    public int specialCooldown = 200;
    /** Client: ticks its rider shows red after a hit. */
    public int riderHurtTime;
    public IFChainBuffer roll_buffer;
    public IFChainBuffer pitch_buffer;
    public IFChainBuffer pitch_buffer_body;
    public ReversedBuffer turn_buffer;
    public ChainBuffer tail_buffer;
    public int spacebarTicks;
    public float[][] growth_stages;
    public LegSolverQuadruped legSolver;
    public int walkCycle;
    public BlockPos burningTarget;
    public int burnProgress;
    public double burnParticleX;
    public double burnParticleY;
    public double burnParticleZ;
    public float prevDragonPitch;
    public IafDragonAttacks.Air airAttack;
    public IafDragonAttacks.Ground groundAttack;
    public boolean usingGroundAttack = true;
    public IafDragonLogic logic;
    public int hoverTicks;
    public int tacklingTicks;
    public int ticksStill;
    /*
        0 = ground/walking
        1 = ai flight
        2 = controlled flight
     */
    public int navigatorType;
    public SimpleContainer dragonInventory;
    public String prevArmorResLoc = "0|0|0|0";
    public String armorResLoc = "0|0|0|0";
    public IafDragonFlightManager flightManager;
    public boolean lookingForRoostAIFlag = false;
    protected int flyHovering;
    protected boolean hasHadHornUse = false;
    protected int fireTicks;
    protected int blockBreakCounter;
    private int prevFlightCycle;
    private boolean isModelDead;
    private int animationTick;
    private Animation currentAnimation;
    private float lastScale;

    private EntityDragonPart headPart;
    private EntityDragonPart neckPart;
    private EntityDragonPart rightWingUpperPart;
    private EntityDragonPart rightWingLowerPart;
    private EntityDragonPart leftWingUpperPart;
    private EntityDragonPart leftWingLowerPart;
    private EntityDragonPart tail1Part;
    private EntityDragonPart tail2Part;
    private EntityDragonPart tail3Part;
    private EntityDragonPart tail4Part;
    /** Its wings' tips and its tail's fin: the wings spread far past the old parts in the air (their tips had none). */
    private EntityDragonPart rightWingTipPart;
    private EntityDragonPart leftWingTipPart;
    private EntityDragonPart tailFinPart;
    /** The size its parts were made for (its render size / 3: a model unit of 16 is one of these). */
    private float partScale = 1;
    private boolean isOverAir;

    /** Air speed used by the riding code (the old LivingEntity#flyingSpeed field). */
    protected float flyingSpeed = 0.02F;

    public EntityDragonBase(EntityType t, Level world, DragonType type, double minimumDamage, double maximumDamage, double minimumHealth, double maximumHealth, double minimumSpeed, double maximumSpeed) {
        super(t, world);
        IHasCustomizableAttributes.applyAttributesForEntity(t, this);

        this.dragonType = type;
        this.minimumDamage = minimumDamage;
        this.maximumDamage = maximumDamage;
        this.minimumHealth = minimumHealth;
        this.maximumHealth = maximumHealth;
        this.minimumSpeed = minimumSpeed;
        this.maximumSpeed = maximumSpeed;
        this.minimumArmor = 1D;
        this.maximumArmor = 20D;
        ANIMATION_EAT = Animation.create(20);
        this.createInventory();
        if (world.isClientSide()) {
            roll_buffer = new IFChainBuffer();
            pitch_buffer = new IFChainBuffer();
            pitch_buffer_body = new IFChainBuffer();
            turn_buffer = new ReversedBuffer();
            tail_buffer = new ChainBuffer();
        }
        legSolver = new LegSolverQuadruped(0.3F, 0.35F, 0.2F, 1.45F, 1.0F);
        this.flightManager = new IafDragonFlightManager(this);
        this.logic = createDragonLogic();
        switchNavigator(0);
        randomizeAttacks();
        resetParts(1);
    }

    public static AttributeSupplier.Builder bakeAttributes() {
        return Mob.createMobAttributes()
            //HEALTH
            .add(Attributes.MAX_HEALTH, 20.0D)
            //SPEED
            .add(Attributes.MOVEMENT_SPEED, 0.3D)
            //ATTACK
            .add(Attributes.ATTACK_DAMAGE, 1)
            //FOLLOW RANGE
            .add(Attributes.FOLLOW_RANGE, Math.min(2048, IafConfig.dragonTargetSearchLength))
            //ARMOR
            .add(Attributes.ARMOR, 4);
    }

    @Override
    public AttributeSupplier.Builder getConfigurableAttributes() {
        return bakeAttributes();
    }

    @Override
    public BlockPos getHomePosition() {
        return this.homePos == null ? super.getHomePosition() : homePos.getPosition();
    }

    @Override
    public int getHomeRadius() {
        return IafConfig.dragonWanderFromHomeDistance;
    }

    @Override
    protected float getFlyingSpeed() {
        return flyingSpeed;
    }

    public String getHomeDimensionName() {
        return this.homePos == null ? "" : homePos.getDimension();
    }

    @Override
    public boolean hasHome() {
        return this.hasHomePosition &&
            getHomeDimensionName().equals(DragonUtils.getDimensionName(this.level()))
            || super.hasHome();
    }

    @Override
    protected void registerGoals() {
//        this.goalSelector.addGoal(0, new DragonAIRide<>(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new DragonAIMate(this, 1.0D));
        this.goalSelector.addGoal(3, new DragonAIReturnToRoost(this, 1.0D));
        this.goalSelector.addGoal(4, new DragonAIEscort(this, 1.0D));
        this.goalSelector.addGoal(5, new DragonAIAttackMelee(this, 1.5D, false));
        this.goalSelector.addGoal(6, new AquaticAITempt(this, 1.0D, IafItemRegistry.FIRE_STEW, false));
        this.goalSelector.addGoal(7, new DragonAIWander(this, 1.0D));
        this.goalSelector.addGoal(8, new DragonAIWatchClosest(this, LivingEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new DragonAILookIdle(this));
        this.targetSelector.addGoal(1, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new DragonAITargetNonTamed<>(this, LivingEntity.class, false, (LivingEntity entity) -> !EntityDragonBase.this.skyWanderer && !EntityDragonBase.this.alliedWith(entity) && (!(entity instanceof Player) || !((Player) entity).isCreative()) && DragonUtils.canHostilesTarget(entity) && entity.getType() != EntityDragonBase.this.getType() && EntityDragonBase.this.shouldTarget(entity) && DragonUtils.isAlive(entity)));
        this.targetSelector.addGoal(5, new DragonAITarget<>(this, LivingEntity.class, true, (LivingEntity entity) -> !EntityDragonBase.this.skyWanderer && !EntityDragonBase.this.alliedWith(entity) && DragonUtils.canHostilesTarget(entity) && entity.getType() != EntityDragonBase.this.getType() && EntityDragonBase.this.shouldTarget(entity) && DragonUtils.isAlive(entity)));
        this.targetSelector.addGoal(6, new DragonAITargetItems(this, false));
    }

    protected abstract boolean shouldTarget(Entity entity);

    public void resetParts(float scale) {
        removeParts();
        headPart = new EntityDragonPart(this, 1.55F * scale, 0, 0.6F * scale, 0.5F * scale, 0.35F * scale, 1.5F);
        headPart.copyPosition(this);
        headPart.setParent(this);
        neckPart = new EntityDragonPart(this, 0.85F * scale, 0, 0.7F * scale, 0.5F * scale, 0.2F * scale, 1);
        neckPart.copyPosition(this);
        neckPart.setParent(this);
        rightWingUpperPart = new EntityDragonPart(this, scale, 90, 0.5F * scale, 0.85F * scale, 0.3F * scale, 0.5F);
        rightWingUpperPart.copyPosition(this);
        rightWingUpperPart.setParent(this);
        rightWingLowerPart = new EntityDragonPart(this, 1.4F * scale, 100, 0.3F * scale, 0.85F * scale, 0.2F * scale, 0.5F);
        rightWingLowerPart.copyPosition(this);
        rightWingLowerPart.setParent(this);
        leftWingUpperPart = new EntityDragonPart(this, scale, -90, 0.5F * scale, 0.85F * scale, 0.3F * scale, 0.5F);
        leftWingUpperPart.copyPosition(this);
        leftWingUpperPart.setParent(this);
        leftWingLowerPart = new EntityDragonPart(this, 1.4F * scale, -100, 0.3F * scale, 0.85F * scale, 0.2F * scale, 0.5F);
        leftWingLowerPart.copyPosition(this);
        leftWingLowerPart.setParent(this);
        tail1Part = new EntityDragonPart(this, -0.75F * scale, 0, 0.6F * scale, 0.35F * scale, 0.35F * scale, 1);
        tail1Part.copyPosition(this);
        tail1Part.setParent(this);
        tail2Part = new EntityDragonPart(this, -1.15F * scale, 0, 0.45F * scale, 0.35F * scale, 0.35F * scale, 1);
        tail2Part.copyPosition(this);
        tail2Part.setParent(this);
        tail3Part = new EntityDragonPart(this, -1.5F * scale, 0, 0.35F * scale, 0.35F * scale, 0.35F * scale, 1);
        tail3Part.copyPosition(this);
        tail3Part.setParent(this);
        tail4Part = new EntityDragonPart(this, -1.95F * scale, 0, 0.25F * scale, 0.45F * scale, 0.3F * scale, 1.5F);
        tail4Part.copyPosition(this);
        tail4Part.setParent(this);
        rightWingTipPart = new EntityDragonPart(this, 1.8F * scale, 105, 0.3F * scale, 0.65F * scale, 0.5F * scale, 0.5F);
        rightWingTipPart.copyPosition(this);
        rightWingTipPart.setParent(this);
        leftWingTipPart = new EntityDragonPart(this, 1.8F * scale, -105, 0.3F * scale, 0.65F * scale, 0.5F * scale, 0.5F);
        leftWingTipPart.copyPosition(this);
        leftWingTipPart.setParent(this);
        tailFinPart = new EntityDragonPart(this, -2.75F * scale, 0, 0.42F * scale, 0.9F * scale, 0.3F * scale, 1.5F);
        tailFinPart.copyPosition(this);
        tailFinPart.setParent(this);
        this.partScale = scale;
        if (this.tickCount > 0 && this.level() instanceof ServerLevel server && this.getVirtualRider() != DragonRiders.NONE && DragonRiders.hasRiders(this)) {
            // its rider's hitbox again at once
            DragonRiders.keepPart(this, server);
        }
    }

    public void removeParts() {
        if (riderPart != null) {
            riderPart.remove(RemovalReason.DISCARDED);
            riderPart = null;
        }
        if (headPart != null) {
            headPart.remove(RemovalReason.DISCARDED);
            headPart = null;
        }
        if (neckPart != null) {
            neckPart.remove(RemovalReason.DISCARDED);
            neckPart = null;
        }
        if (rightWingUpperPart != null) {
            rightWingUpperPart.remove(RemovalReason.DISCARDED);
            rightWingUpperPart = null;
        }
        if (rightWingLowerPart != null) {
            rightWingLowerPart.remove(RemovalReason.DISCARDED);
            rightWingLowerPart = null;
        }
        if (leftWingUpperPart != null) {
            leftWingUpperPart.remove(RemovalReason.DISCARDED);
            leftWingUpperPart = null;
        }
        if (leftWingLowerPart != null) {
            leftWingLowerPart.remove(RemovalReason.DISCARDED);
            leftWingLowerPart = null;
        }
        if (tail1Part != null) {
            tail1Part.remove(RemovalReason.DISCARDED);
            tail1Part = null;
        }
        if (tail2Part != null) {
            tail2Part.remove(RemovalReason.DISCARDED);
            tail2Part = null;
        }
        if (tail3Part != null) {
            tail3Part.remove(RemovalReason.DISCARDED);
            tail3Part = null;
        }
        if (tail4Part != null) {
            tail4Part.remove(RemovalReason.DISCARDED);
            tail4Part = null;
        }
        if (rightWingTipPart != null) {
            rightWingTipPart.remove(RemovalReason.DISCARDED);
            rightWingTipPart = null;
        }
        if (leftWingTipPart != null) {
            leftWingTipPart.remove(RemovalReason.DISCARDED);
            leftWingTipPart = null;
        }
        if (tailFinPart != null) {
            tailFinPart.remove(RemovalReason.DISCARDED);
            tailFinPart = null;
        }
    }

    public void updateParts() {
        if (this.level().isClientSide()) {
            // (the server's parts are sent to the client like any entity)
            return;
        }
        if (headPart != null) {
            if (!headPart.shouldContinuePersisting()) {
                level().addFreshEntity(headPart);
            }
            headPart.setParent(this);
        }
        if (neckPart != null) {
            if (!neckPart.shouldContinuePersisting()) {
                level().addFreshEntity(neckPart);
            }
            neckPart.setParent(this);
        }
        if (rightWingUpperPart != null) {
            if (!rightWingUpperPart.shouldContinuePersisting()) {
                level().addFreshEntity(rightWingUpperPart);
            }
            rightWingUpperPart.setParent(this);
        }
        if (rightWingLowerPart != null) {
            if (!rightWingLowerPart.shouldContinuePersisting()) {
                level().addFreshEntity(rightWingLowerPart);
            }
            rightWingLowerPart.setParent(this);
        }
        if (leftWingUpperPart != null) {
            if (!leftWingUpperPart.shouldContinuePersisting()) {
                level().addFreshEntity(leftWingUpperPart);
            }
            leftWingUpperPart.setParent(this);
        }
        if (leftWingLowerPart != null) {
            if (!leftWingLowerPart.shouldContinuePersisting()) {
                level().addFreshEntity(leftWingLowerPart);
            }
            leftWingLowerPart.setParent(this);
        }
        if (tail1Part != null) {
            if (!tail1Part.shouldContinuePersisting()) {
                level().addFreshEntity(tail1Part);
            }
            tail1Part.setParent(this);
        }
        if (tail2Part != null) {
            if (!tail2Part.shouldContinuePersisting()) {
                level().addFreshEntity(tail2Part);
            }
            tail2Part.setParent(this);
        }
        if (tail3Part != null) {
            if (!tail3Part.shouldContinuePersisting()) {
                level().addFreshEntity(tail3Part);
            }
            tail3Part.setParent(this);
        }
        if (tail4Part != null) {
            if (!tail4Part.shouldContinuePersisting()) {
                level().addFreshEntity(tail4Part);
            }
            tail4Part.setParent(this);
        }
        for (EntityDragonPart part : new EntityDragonPart[]{rightWingTipPart, leftWingTipPart, tailFinPart}) {
            if (part != null) {
                if (!part.shouldContinuePersisting()) {
                    level().addFreshEntity(part);
                }
                part.setParent(this);
            }
        }
        layParts();
    }

    /**
     * Its parts where its body is drawn, by its pose (measured on its model, the same for the three kinds): on the
     * ground its wings fold along its sides up to its back; hovering or flying they spread three and a half times its
     * size out and rise; hovering its tail hangs under it. Between the two as it takes off or lands.
     * Each: its side (right), forward, bottom, width and height, in its size (render size / 3).
     */
    private void layParts() {
        if (headPart == null || rightWingTipPart == null) {
            return;
        }
        float air = Mth.clamp(Math.max(this.flyProgress, this.hoverProgress) / 20F, 0F, 1F);
        float hang = Mth.clamp(this.hoverProgress / 20F, 0F, 1F) * (1F - Mth.clamp(this.flyProgress / 20F, 0F, 1F));
        lay(headPart, air, 0, 1.65F, 0.42F, 0.55F, 0.45F, 0, 1.55F, 0.70F, 0.55F, 0.5F);
        lay(neckPart, air, 0, 1.05F, 0.52F, 0.5F, 0.42F, 0, 1.0F, 0.6F, 0.5F, 0.6F);
        for (int side = -1; side <= 1; side += 2) {
            EntityDragonPart upper = side > 0 ? rightWingUpperPart : leftWingUpperPart;
            EntityDragonPart lower = side > 0 ? rightWingLowerPart : leftWingLowerPart;
            EntityDragonPart tip = side > 0 ? rightWingTipPart : leftWingTipPart;
            lay(upper, air, side * 1.0F, 0.3F, 0.1F, 0.9F, 1.5F, side * 1.3F, 0.45F, 0.95F, 1.15F, 1.0F);
            lay(lower, air, side * 1.45F, -0.4F, 0.05F, 0.75F, 1.5F, side * 2.4F, 0.25F, 1.3F, 1.2F, 1.1F);
            lay(tip, air, side * 1.35F, -1.05F, 0.25F, 0.65F, 1.05F, side * 3.25F, 0.55F, 1.85F, 0.9F, 0.8F);
        }
        lay(tail1Part, hang, 0, -0.4F, 0.5F, 0.4F, 0.45F, 0, -0.3F, 0.25F, 0.45F, 0.45F);
        lay(tail2Part, hang, 0, -0.8F, 0.45F, 0.4F, 0.42F, 0, -0.67F, 0.03F, 0.45F, 0.47F);
        lay(tail3Part, hang, 0, -1.3F, 0.42F, 0.4F, 0.35F, 0, -1.07F, -0.3F, 0.45F, 0.52F);
        lay(tail4Part, hang, 0, -1.8F, 0.42F, 0.45F, 0.25F, 0, -1.4F, -0.7F, 0.4F, 0.5F);
        lay(tailFinPart, hang, 0, -2.75F, 0.42F, 0.9F, 0.3F, 0, -1.75F, -1.6F, 0.7F, 0.95F);
    }

    /** One part between two poses (t: 0 the first, 1 the second), in its size. */
    private void lay(EntityDragonPart part, float t, float side0, float fwd0, float y0, float w0, float h0,
        float side1, float fwd1, float y1, float w1, float h1) {
        if (part == null) {
            return;
        }
        float k = this.partScale;
        part.place(Mth.lerp(t, side0, side1) * k, Mth.lerp(t, fwd0, fwd1) * k, Mth.lerp(t, y0, y1) * k, Mth.lerp(t, w0, w1) * k,
            Mth.lerp(t, h0, h1) * k);
    }

    /**
     * An assembled forge of this dragon's kind the rider is looking toward (within 30 degrees), close enough
     * and in sight: the ridden dragon's breath goes into it.
     */
    @Nullable
    protected BlockPos forgeInFrontOf(Entity rider) {
        double reach = 10.0D + this.getBbWidth();
        Vec3 eye = rider.getEyePosition();
        Vec3 look = rider.getViewVector(1.0F);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        int chunks = (int) Math.ceil(reach / 16.0D) + 1;
        net.minecraft.world.level.ChunkPos center = net.minecraft.world.level.ChunkPos.containing(this.blockPosition());
        for (int cx = -chunks; cx <= chunks; cx++) {
            for (int cz = -chunks; cz <= chunks; cz++) {
                if (!level().hasChunk(center.x() + cx, center.z() + cz)) {
                    continue;
                }
                for (net.minecraft.world.level.block.entity.BlockEntity blockEntity : level().getChunk(center.x() + cx, center.z() + cz).getBlockEntities().values()) {
                    if (!(blockEntity instanceof TileEntityDragonforgeInput input) || !input.isAssembled()
                        || input.getDragonType() != DragonType.getIntFromType(this.dragonType)) {
                        continue;
                    }
                    Vec3 target = Vec3.atCenterOf(input.getBlockPos());
                    Vec3 toTarget = target.subtract(eye);
                    double distance = toTarget.length();
                    if (distance > reach + 4 || distance < 0.5D || toTarget.normalize().dot(look) < 0.866D) {
                        continue;
                    }
                    if (distance < bestDistance && this.canPositionBeSeen(target.x, target.y, target.z)) {
                        best = input.getBlockPos();
                        bestDistance = distance;
                    }
                }
            }
        }
        return best;
    }

    protected void updateBurnTarget() {
        // A ridden dragon breathes into a forge only while its rider makes it breathe.
        if (burningTarget != null && this.getControllingPassenger() instanceof Player && !this.isStriking()) {
            burningTarget = null;
            return;
        }
        if (burningTarget != null && !this.isSleeping() && !this.isModelDead() && !this.isBaby()) {
            float maxDist = 115 * this.getDragonStage();
            boolean flag = false;
            if (level().getBlockEntity(burningTarget) instanceof TileEntityDragonforgeInput && ((TileEntityDragonforgeInput) level().getBlockEntity(burningTarget)).isAssembled()
                && this.distanceToSqr(burningTarget.getX() + 0.5D, burningTarget.getY() + 0.5D, burningTarget.getZ() + 0.5D) < maxDist && canPositionBeSeen(burningTarget.getX() + 0.5D, burningTarget.getY() + 0.5D, burningTarget.getZ() + 0.5D)) {
                this.getLookControl().setLookAt(burningTarget.getX() + 0.5D, burningTarget.getY() + 0.5D, burningTarget.getZ() + 0.5D, 180F, 180F);
                this.breathFireAtPos(burningTarget);
                this.setBreathingFire(true);
            } else {
                if (!level().isClientSide()) {
                    IafNetwork.sendToAll(new MessageDragonSetBurnBlock(this.getId(), true, burningTarget));
                }
                burningTarget = null;
            }
        }
    }

    protected abstract void breathFireAtPos(BlockPos burningTarget);

    protected PathingStuckHandler createStuckHandler() {
        return PathingStuckHandler.createStuckHandler();
    }

    @Override
    protected PathNavigation createNavigation(@NotNull Level worldIn) {
        return createNavigator(worldIn, AdvancedPathNavigate.MovementType.WALKING);
    }


    protected PathNavigation createNavigator(Level worldIn, AdvancedPathNavigate.MovementType type) {
        return createNavigator(worldIn, type, createStuckHandler());
    }

    protected PathNavigation createNavigator(Level worldIn, AdvancedPathNavigate.MovementType type, PathingStuckHandler stuckHandler) {
        return createNavigator(worldIn, type, stuckHandler, 4f, 4f);
    }

    protected PathNavigation createNavigator(Level worldIn, AdvancedPathNavigate.MovementType type, PathingStuckHandler stuckHandler, float width, float height) {
        AdvancedPathNavigate newNavigator = new AdvancedPathNavigate(this, level(), type, width, height);
        this.navigation = newNavigator;
        newNavigator.setCanFloat(true);
        newNavigator.getNodeEvaluator().setCanOpenDoors(true);
        return newNavigator;
    }

    protected void switchNavigator(int navigatorType) {
        if (navigatorType == 0) {
            this.moveControl = new IafDragonFlightManager.GroundMoveHelper(this);
            this.navigation = createNavigator(level(), AdvancedPathNavigate.MovementType.WALKING, createStuckHandler().withTeleportSteps(5));
            this.navigatorType = 0;
            this.setFlying(false);
            this.setHovering(false);
        } else if (navigatorType == 1) {
            this.moveControl = new IafDragonFlightManager.FlightMoveHelper(this);
            this.navigation = createNavigator(level(), AdvancedPathNavigate.MovementType.FLYING);
            this.navigatorType = 1;
        } else {
            this.moveControl = new IafDragonFlightManager.PlayerFlightMoveHelper(this);
            this.navigation = createNavigator(level(), AdvancedPathNavigate.MovementType.FLYING);
            this.navigatorType = 2;
        }
    }


    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        breakBlock();
    }

    public boolean canDestroyBlock(BlockPos pos, BlockState state) {
        return !(state.getBlock() instanceof IDragonProof) && state.getDestroySpeed(level(), pos) >= 0.0F;
    }

    @Override
    public boolean isMobDead() {
        return this.isModelDead();
    }

    @Override
    public int getMaxHeadYRot() {
        return 30 * this.getDragonStage() / 5;
    }

    public void openInventory(Player player) {
        if (!this.level().isClientSide())
            player.openMenu(getMenuProvider());
        IafMod.PROXY.setReferencedMob(this);
    }

    public MenuProvider getMenuProvider() {
        return new SimpleMenuProvider((containerId, playerInventory, player) -> new ContainerDragon(containerId, dragonInventory, playerInventory, this), this.getDisplayName());
    }

    @Override
    public int getAmbientSoundInterval() {
        return 90;
    }

    @Override
    protected void tickDeath() {
        this.deathTime = 0;
        this.setModelDead(true);
        this.ejectPassengers();
        if (this.level() instanceof ServerLevel server && this.tickCount % 10 == 0) {
            // its body is no foe: whatever still went for it (another dragon, a pet, a summoned minion) lets it be
            for (Mob mob : server.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(48), m -> m.getTarget() == this)) {
                mob.setTarget(null);
            }
        }
        if (this.getDeathStage() >= this.getAgeInDays() / 5) {
            this.remove(RemovalReason.KILLED);
            for (int k = 0; k < 40; ++k) {
                double d2 = this.random.nextGaussian() * 0.02D;
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                if (level().isClientSide()) {
                    this.level().addParticle(ParticleTypes.CLOUD, this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(), this.getY() + this.random.nextFloat() * this.getBbHeight(), this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(), d2, d0, d1);
                }
            }
            spawnDeathParticles();
        }
    }

    protected void spawnDeathParticles() {
    }

    protected void spawnBabyParticles() {
    }

    @Override
    public void remove(Entity.@NotNull RemovalReason reason) {
        removeParts();
        super.remove(reason);
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        switch (this.getDragonStage()) {
            case 2:
                return 20;
            case 3:
                return 150;
            case 4:
                return 300;
            case 5:
                return 650;
            default:
                return 5;
        }
    }

    public int getArmorOrdinal(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof ItemDragonArmor armorItem) {
            return armorItem.type.ordinal() + 1;
        }
        return 0;
    }

    @Override
    public boolean isNoAi() {
        return this.isModelDead() || super.isNoAi();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SWIMMING, Boolean.FALSE);
        builder.define(DREAD_LOOK, (byte) 0);
        builder.define(VIRTUAL_RIDER, (byte) 0);
        builder.define(HUNGER, 0);
        builder.define(AGE_TICKS, 0);
        builder.define(GENDER, false);
        builder.define(VARIANT, 0);
        builder.define(SLEEPING, false);
        builder.define(FIREBREATHING, false);
        builder.define(HOVERING, false);
        builder.define(FLYING, false);
        builder.define(DEATH_STAGE, 0);
        builder.define(MODEL_DEAD, false);
        builder.define(CONTROL_STATE, (byte) 0);
        builder.define(TACKLE, false);
        builder.define(AGINGDISABLED, false);
        builder.define(COMMAND, 0);
        builder.define(DRAGON_PITCH, 0F);
        builder.define(CRYSTAL_BOUND, false);
        builder.define(CUSTOM_POSE, "");
    }

    @Override
    public boolean isGoingUp() {
        return (entityData.get(CONTROL_STATE).byteValue() & 1) == 1;
    }

    @Override
    public boolean isGoingDown() {
        return (entityData.get(CONTROL_STATE).byteValue() >> 1 & 1) == 1;
    }

    public boolean isAttacking() {
        return (entityData.get(CONTROL_STATE).byteValue() >> 2 & 1) == 1;
    }

    public boolean isStriking() {
        return (entityData.get(CONTROL_STATE).byteValue() >> 3 & 1) == 1;
    }

    public boolean isDismounting() {
        return (entityData.get(CONTROL_STATE).byteValue() >> 4 & 1) == 1;
    }

    @Override
    public void up(boolean up) {
        setStateField(0, up);
    }

    @Override
    public void down(boolean down) {
        setStateField(1, down);
    }

    @Override
    public void attack(boolean attack) {
        setStateField(2, attack);
    }

    @Override
    public void strike(boolean strike) {
        setStateField(3, strike);
    }

    @Override
    public void dismount(boolean dismount) {
        setStateField(4, dismount);
    }

    private void setStateField(int i, boolean newState) {
        byte prevState = entityData.get(CONTROL_STATE);
        if (newState) {
            entityData.set(CONTROL_STATE, (byte) (prevState | (1 << i)));
        } else {
            entityData.set(CONTROL_STATE, (byte) (prevState & ~(1 << i)));
        }
    }

    @Override
    public byte getControlState() {
        return entityData.get(CONTROL_STATE);
    }

    @Override
    public void setControlState(byte state) {
        entityData.set(CONTROL_STATE, state);
    }

    public int getCommand() {
        return this.entityData.get(COMMAND);
    }

    public void setCommand(int command) {
        this.entityData.set(COMMAND, command);
        this.setOrderedToSit(command == 1);
    }

    public float getDragonPitch() {
        return entityData.get(DRAGON_PITCH);
    }

    public void setDragonPitch(float pitch) {
        entityData.set(DRAGON_PITCH, pitch);
    }

    public void incrementDragonPitch(float pitch) {
        entityData.set(DRAGON_PITCH, getDragonPitch() + pitch);
    }

    public void decrementDragonPitch(float pitch) {
        entityData.set(DRAGON_PITCH, getDragonPitch() - pitch);
    }

    @Override
    public void addAdditionalSaveData(ValueOutput compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Hunger", this.getHunger());
        compound.putInt("AgeTicks", this.getAgeInTicks());
        compound.putBoolean("Gender", this.isMale());
        compound.putInt("Variant", this.getVariant());
        compound.putBoolean("Sleeping", this.isSleeping());
        compound.putBoolean("TamedDragon", this.isTame());
        compound.putBoolean("FireBreathing", this.isBreathingFire());
        compound.putBoolean("AttackDecision", usingGroundAttack);
        compound.putBoolean("SkyWanderer", skyWanderer);
        compound.putBoolean("WaveDragon", waveDragon);
        compound.putLong("DespawnAt", despawnAt);
        compound.putBoolean("PillagerMount", pillagerMount);
        compound.putBoolean("DreadMount", dreadMount);
        compound.putInt("DreadLook", this.getDreadLook());
        compound.putBoolean("EliteMount", eliteMount);
        compound.putInt("VirtualRider", this.getVirtualRider());
        compound.putFloat("RiderHealth", this.riderHealth);
        compound.putBoolean("Hovering", this.isHovering());
        compound.putBoolean("Flying", this.isFlying());
        compound.putInt("DeathStage", this.getDeathStage());
        compound.putBoolean("ModelDead", this.isModelDead());
        compound.putFloat("DeadProg", this.modelDeadProgress);
        compound.putBoolean("Tackle", this.isTackling());
        compound.putBoolean("HasHomePosition", this.hasHomePosition);
        compound.putString("CustomPose", this.getCustomPose());
        if (homePos != null && this.hasHomePosition) {
            homePos.write(compound);
        }
        compound.putBoolean("AgingDisabled", this.isAgingDisabled());
        compound.putInt("Command", this.getCommand());
        if (dragonInventory != null) {
            ContainerHelper.saveAllItems(compound, dragonInventory.getItems());
        }
        compound.putBoolean("CrystalBound", this.isBoundToCrystal());
    }

    @Override
    public void readAdditionalSaveData(ValueInput compound) {
        super.readAdditionalSaveData(compound);
        this.setHunger(compound.getIntOr("Hunger", 0));
        this.setAgeInTicks(compound.getIntOr("AgeTicks", 0));
        this.setGender(compound.getBooleanOr("Gender", false));
        this.setVariant(compound.getIntOr("Variant", 0));
        this.setInSittingPose(compound.getBooleanOr("Sleeping", false));
        this.setTame(compound.getBooleanOr("TamedDragon", false), true);
        this.setBreathingFire(compound.getBooleanOr("FireBreathing", false));
        this.usingGroundAttack = compound.getBooleanOr("AttackDecision", false);
        this.skyWanderer = compound.getBooleanOr("SkyWanderer", false);
        this.waveDragon = compound.getBooleanOr("WaveDragon", false);
        this.despawnAt = compound.getLongOr("DespawnAt", 0L);
        this.pillagerMount = compound.getBooleanOr("PillagerMount", false);
        this.dreadMount = compound.getBooleanOr("DreadMount", false);
        this.setDreadLook(compound.getIntOr("DreadLook", 0));
        this.oldSleeper = this.isBlackFrost() && this.isNoAi() && !(this instanceof EntityBlackFrostGuard);
        this.eliteMount = compound.getBooleanOr("EliteMount", false);
        this.setVirtualRider(compound.getIntOr("VirtualRider", 0));
        this.riderHealth = compound.getFloatOr("RiderHealth", 0F);
        this.setHovering(compound.getBooleanOr("Hovering", false));
        this.setFlying(compound.getBooleanOr("Flying", false));
        this.setDeathStage(compound.getIntOr("DeathStage", 0));
        this.setModelDead(compound.getBooleanOr("ModelDead", false));
        this.modelDeadProgress = compound.getFloatOr("DeadProg", 0.0F);
        this.setCustomPose(compound.getStringOr("CustomPose", ""));
        this.hasHomePosition = compound.getBooleanOr("HasHomePosition", false);
        if (hasHomePosition && compound.getIntOr("HomeAreaX", 0) != 0 && compound.getIntOr("HomeAreaY", 0) != 0 && compound.getIntOr("HomeAreaZ", 0) != 0) {
            homePos = new HomePosition(compound, this.level());
        }
        this.setTackling(compound.getBooleanOr("Tackle", false));
        this.setAgingDisabled(compound.getBooleanOr("AgingDisabled", false));
        this.setCommand(compound.getIntOr("Command", 0));
        this.createInventory();
        ContainerHelper.loadAllItems(compound, dragonInventory.getItems());
        this.updateContainerEquipment();
        this.setCrystalBound(compound.getBooleanOr("CrystalBound", false));
    }

    public int getContainerSize() {
        return 5;
    }

    protected void createInventory() {
        SimpleContainer tempInventory = this.dragonInventory;
        this.dragonInventory = new SimpleContainer(this.getContainerSize()) {
            @Override
            public void setChanged() {
                super.setChanged();
                containerChanged(this);
            }
        };
        if (tempInventory != null) {
            int i = Math.min(tempInventory.getContainerSize(), this.dragonInventory.getContainerSize());

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = tempInventory.getItem(j);
                if (!itemstack.isEmpty()) {
                    this.dragonInventory.setItem(j, itemstack.copy());
                }
            }
        }

        this.updateContainerEquipment();
    }

    protected void updateContainerEquipment() {
        if (!this.level().isClientSide()) {
            updateAttributes();
        }
    }



    public boolean hasInventoryChanged(Container pInventory) {
        return this.dragonInventory != pInventory;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof Player player && this.getTarget() != passenger) {
                if (this.isTame() && EntityUtil.getOwnerUUID(this) != null && EntityUtil.getOwnerUUID(this).equals(player.getUUID())) {
                    return player;
                }
            }
        }
        return null;
    }

    public boolean isRidingPlayer(Player player) {
        return getRidingPlayer() != null && player != null && getRidingPlayer().getUUID().equals(player.getUUID());
    }

    @Override
    @Nullable
    public Player getRidingPlayer() {
        if (this.getControllingPassenger() instanceof Player) {
            return (Player) this.getControllingPassenger();
        }
        return null;
    }

    protected void updateAttributes() {
        prevArmorResLoc = armorResLoc;
        final int armorHead = this.getArmorOrdinal(this.getItemBySlot(EquipmentSlot.HEAD));
        final int armorNeck = this.getArmorOrdinal(this.getItemBySlot(EquipmentSlot.CHEST));
        final int armorLegs = this.getArmorOrdinal(this.getItemBySlot(EquipmentSlot.LEGS));
        final int armorFeet = this.getArmorOrdinal(this.getItemBySlot(EquipmentSlot.FEET));
        armorResLoc = dragonType.getName() + "|" + armorHead + "|" + armorNeck + "|" + armorLegs + "|" + armorFeet;
        IafMod.PROXY.updateDragonArmorRender(armorResLoc);

        double age = 125F;
        if (this.getAgeInDays() <= 125) age = this.getAgeInDays();
        final double healthStep = (maximumHealth - minimumHealth) / 125F;
        final double attackStep = (maximumDamage - minimumDamage) / 125F;
        final double speedStep = (maximumSpeed - minimumSpeed) / 125F;
        final double armorStep = (maximumArmor - minimumArmor) / 125F;

        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.round(minimumHealth + (healthStep * age)));
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.round(minimumDamage + (attackStep * age)));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(minimumSpeed + (speedStep * age));
        final double baseValue = minimumArmor + (armorStep * this.getAgeInDays());
        this.getAttribute(Attributes.ARMOR).setBaseValue(baseValue);
        if (!this.level().isClientSide()) {
            this.getAttribute(Attributes.ARMOR).removeModifier(IafMod.id("armor_modifier_uuid"));
            this.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(IafMod.id("armor_modifier_uuid"), calculateArmorModifier(), AttributeModifier.Operation.ADD_VALUE));
        }
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(Math.min(2048, IafConfig.dragonTargetSearchLength));
    }

    public int getHunger() {
        return this.entityData.get(HUNGER);
    }

    public void setHunger(int hunger) {
        this.entityData.set(HUNGER, Mth.clamp(hunger, 0, 100));
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    public int getAgeInDays() {
        return this.entityData.get(AGE_TICKS) / 24000;
    }

    public void setAgeInDays(int age) {
        this.entityData.set(AGE_TICKS, age * 24000);
    }

    public int getAgeInTicks() {
        return this.entityData.get(AGE_TICKS);
    }

    public void setAgeInTicks(int age) {
        this.entityData.set(AGE_TICKS, age);
    }

    public int getDeathStage() {
        return this.entityData.get(DEATH_STAGE);
    }

    public void setDeathStage(int stage) {
        this.entityData.set(DEATH_STAGE, stage);
    }

    public boolean isMale() {
        return this.entityData.get(GENDER);
    }

    public boolean isModelDead() {
        if (level().isClientSide()) {
            return this.isModelDead = this.entityData.get(MODEL_DEAD);
        }
        return isModelDead;
    }

    public void setModelDead(boolean modeldead) {
        this.entityData.set(MODEL_DEAD, modeldead);
        if (!level().isClientSide()) {
            this.isModelDead = modeldead;
        }
    }

    @Override
    public boolean isHovering() {
        return this.entityData.get(HOVERING);
    }

    public void setHovering(boolean hovering) {
        this.entityData.set(HOVERING, hovering);
    }

    @Override
    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(FLYING, flying);
    }

    public boolean useFlyingPathFinder() {
        return isFlying() && this.getControllingPassenger() == null;
    }

    public void setGender(boolean male) {
        this.entityData.set(GENDER, male);
    }

    @Override
    public boolean isSleeping() {
        return this.entityData.get(SLEEPING);
    }

    public boolean isBlinking() {
        return this.tickCount % 50 > 43;
    }

    public boolean isBreathingFire() {
        return this.entityData.get(FIREBREATHING);
    }

    public void setBreathingFire(boolean breathing) {
        this.entityData.set(FIREBREATHING, breathing);
    }

    @Override
    protected boolean canAddPassenger(@NotNull Entity passenger) {
        return this.getPassengers().size() < 2;
    }

    @Override
    public boolean isOrderedToSit() {
        return (this.entityData.get(DATA_FLAGS_ID).byteValue() & 1) != 0;
    }

    @Override
    public void setInSittingPose(boolean sleeping) {
        this.entityData.set(SLEEPING, sleeping);
        if (sleeping)
            this.getNavigation().stop();
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        byte b0 = this.entityData.get(DATA_FLAGS_ID);
        if (sitting) {
            this.entityData.set(DATA_FLAGS_ID, (byte) (b0 | 1));
            this.getNavigation().stop();
        } else {
            this.entityData.set(DATA_FLAGS_ID, (byte) (b0 & -2));
        }
    }

    public String getCustomPose() {
        return this.entityData.get(CUSTOM_POSE);
    }

    public void setCustomPose(String customPose) {
        this.entityData.set(CUSTOM_POSE, customPose);
        modelDeadProgress = 20f;
    }

    public void riderShootFire(Entity controller) {
    }

    @Override
    public boolean killedEntity(ServerLevel world, LivingEntity entity, DamageSource damageSource) {
        this.setHunger(this.getHunger() + FoodUtils.getFoodPoints(entity));
        return true;
    }

    private double calculateArmorModifier() {
        double val = 1D;
        final EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot slot : slots) {
            switch (getArmorOrdinal(getItemBySlot(slot))) {
                case 1:
                    val += 2D;
                    break;
                case 2:
                case 4:
                    val += 3D;
                    break;
                case 3:
                    val += 5D;
                    break;
                case 5:
                case 6:
                case 8:
                    val += 10D;
                    break;
                case 7:
                    val += 1.5D;
                    break;
                case 9:
                    val += 12D;
                    break;
            }
        }
        return val;
    }

    public boolean canMove() {
        return !this.isOrderedToSit() && !this.isSleeping() &&
                this.getControllingPassenger() == null && !this.isPassenger() &&
                !this.isModelDead() && sleepProgress == 0 &&
                this.getAnimation() != ANIMATION_SHAKEPREY;
    }

    public boolean isFuelingForge() {
        return burningTarget != null && level().getBlockEntity(burningTarget) instanceof TileEntityDragonforgeInput;
    }

    @Override
    public boolean isAlive() {
        if (this.isModelDead())
            return !this.isRemoved();
        return super.isAlive();
    }

    /** A dead dragon's body stays (to be harvested) but is no foe: no mob, pet or pixie goes for it. */
    @Override
    public boolean canBeSeenAsEnemy() {
        return !this.isModelDead() && super.canBeSeenAsEnemy();
    }

    /** The tick of the last harvest: a click reaches the dragon through its parts and the multipart message too. */
    private int lastHarvestTick = -1;

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        InteractionResult harvest = this.harvestDead(player, hand);
        if (harvest != InteractionResult.PASS) {
            return harvest;
        }
        return super.interact(player, hand, location);
    }

    /** A dead dragon (or a skeleton) gives, one right click at a time: blood in bottles, its scales and bones, its heart, last its skull. */
    public InteractionResult harvestDead(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int lastDeathStage = Math.min(this.getAgeInDays() / 5, 25);
        if (stack.getItem() == IafItemRegistry.DRAGON_DEBUG_STICK) {
            logic.debug();
            return InteractionResult.SUCCESS;
        }
        if (this.isModelDead() && this.getDeathStage() < lastDeathStage && player.mayBuild()) {
            if (!level().isClientSide()) {
                if (this.lastHarvestTick == this.tickCount) {
                    return InteractionResult.SUCCESS;
                }
                this.lastHarvestTick = this.tickCount;
            }
            if (this.pillagerMount) {
                // a pillager's dragon gives only its scales
                if (!level().isClientSide() && stack.isEmpty()) {
                    this.setDeathStage(this.getDeathStage() + 1);
                    this.spawnAtLocation((ServerLevel) this.level(), new ItemStack(this.getVariantScale(this.getVariant()), 1 + this.random.nextInt(3)), 1);
                    if (this.getDeathStage() >= lastDeathStage - 1) {
                        this.remove(RemovalReason.DISCARDED);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (!level().isClientSide() && !stack.isEmpty() && stack.getItem() != null && stack.getItem() == Items.GLASS_BOTTLE && this.getDeathStage() < lastDeathStage / 2 && IafConfig.dragonDropBlood) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                this.setDeathStage(this.getDeathStage() + 1);
                player.getInventory().add(new ItemStack(this.getBloodItem(), 1));
                return InteractionResult.SUCCESS;
            } else if (!level().isClientSide() && stack.isEmpty() && IafConfig.dragonDropSkull) {
                if (this.getDeathStage() >= lastDeathStage - 1) {
                    ItemStack skull = getSkull().copy();
                    CompoundTag skullTag = new CompoundTag();
                    skullTag.putInt("Stage", this.getDragonStage());
                    skullTag.putInt("DragonType", 0);
                    skullTag.putInt("DragonAge", this.getAgeInDays());
                    ItemNbt.setTag(skull, skullTag);
                    this.setDeathStage(this.getDeathStage() + 1);
                    if (!level().isClientSide()) {
                        this.spawnAtLocation((ServerLevel) this.level(), skull, 1);
                    }
                    this.remove(RemovalReason.DISCARDED);
                } else if (this.getDeathStage() == (lastDeathStage / 2) - 1 && IafConfig.dragonDropHeart) {
                    ItemStack heart = new ItemStack(this.getHeartItem(), 1);
                    ItemStack egg = new ItemStack(this.getVariantEgg(this.random.nextInt(4)), 1);
                    if (!level().isClientSide()) {
                        this.spawnAtLocation((ServerLevel) this.level(), heart, 1);
                        if (!this.isMale() && this.getDragonStage() > 3) {
                            this.spawnAtLocation((ServerLevel) this.level(), egg, 1);
                        }
                    }
                    this.setDeathStage(this.getDeathStage() + 1);
                } else {
                    this.setDeathStage(this.getDeathStage() + 1);
                    ItemStack drop = getRandomDrop();
                    if (!drop.isEmpty() && !level().isClientSide()) {
                        this.spawnAtLocation((ServerLevel) this.level(), drop, 1);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        // Interaction usually means right-click but the relevant item is often in the main hand
        ItemStack stack = player.getMainHandItem();

        if (stack == ItemStack.EMPTY) {
            stack = player.getItemInHand(hand);
        }

        if (stack.getItem() == IafItemRegistry.DRAGON_DEBUG_STICK) {
            logic.debug();
            return InteractionResult.SUCCESS;
        }
        if (!this.isModelDead()) {
            if (stack.getItem() == IafItemRegistry.CREATIVE_DRAGON_MEAL) {
                this.setTame(true, true);
                this.tame(player);
                this.setHunger(this.getHunger() + 20);
                this.heal(Math.min(this.getHealth(), (int) (this.getMaxHealth() / 2)));
                this.playSound(SoundEvents.GENERIC_EAT.value(), this.getSoundVolume(), this.getVoicePitch());
                this.spawnItemCrackParticles(stack.getItem());
                this.spawnItemCrackParticles(Items.BONE);
                this.spawnItemCrackParticles(Items.BONE_MEAL);
                this.eatFoodBonus(stack);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
            if (this.isFood(stack) && this.shouldDropLoot()) {
                this.setAge(0);
                this.usePlayerItem(player, InteractionHand.MAIN_HAND, stack);
                this.setInLove(player);
                return InteractionResult.SUCCESS;
            }
            if (this.isOwnedBy(player)) {
                if (stack.getItem() == getSummoningCrystal() && !ItemSummoningCrystal.hasDragon(stack)) {
                    this.setCrystalBound(true);
                    CompoundTag dragonTag = new CompoundTag();
                    dragonTag.store("DragonUUID", UUIDUtil.CODEC, this.getUUID());
                    if (this.getCustomName() != null) {
                        dragonTag.putString("CustomName", this.getCustomName().getString());
                    }
                    ItemNbt.update(stack, compound -> compound.put("Dragon", dragonTag));
                    this.playSound(SoundEvents.BOTTLE_FILL_DRAGONBREATH, 1, 1);
                    player.swing(hand, SwingAnimation.DEFAULT, true);
                    return InteractionResult.SUCCESS;
                }
                this.tame(player);
                if (stack.getItem() == IafItemRegistry.DRAGON_HORN) {
                    return super.mobInteract(player, hand);
                }
                if (stack.isEmpty() && !player.isShiftKeyDown()) {
                    if (!level().isClientSide()) {
                        final int dragonStage = this.getDragonStage();
                        if (dragonStage < 2) {
                            if (player.getPassengers().size() >= 3)
                                return InteractionResult.FAIL;
                            this.startRiding(player, true, true);
                            IafNetwork.sendToAll(new MessageStartRidingMob(this.getId(), true, true));
                        } else if (dragonStage > 2 && !player.isPassenger()) {
                            player.setShiftKeyDown(false);
                            player.startRiding(this, true, true);
                            IafNetwork.sendToAll(new MessageStartRidingMob(this.getId(), true, false));
                            this.setInSittingPose(false);
                        }
                        this.getNavigation().stop();
                    }
                    return InteractionResult.SUCCESS;
                } else if (stack.isEmpty() && player.isShiftKeyDown()) {
                    this.openInventory(player);
                    return InteractionResult.SUCCESS;
                } else {
                    int itemFoodAmount = FoodUtils.getFoodPoints(stack, true, dragonType.isPiscivore());
                    if (itemFoodAmount > 0 && (this.getHunger() < 100 || this.getHealth() < this.getMaxHealth())) {
                        this.setHunger(this.getHunger() + itemFoodAmount);
                        this.setHealth(Math.min(this.getMaxHealth(), (int) (this.getHealth() + (itemFoodAmount / 10))));
                        this.playSound(SoundEvents.GENERIC_EAT.value(), this.getSoundVolume(), this.getVoicePitch());
                        this.spawnItemCrackParticles(stack.getItem());
                        this.eatFoodBonus(stack);
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        return InteractionResult.SUCCESS;
                    }
                    final Item stackItem = stack.getItem();
                    if (stackItem == IafItemRegistry.DRAGON_MEAL) {
                        this.growDragon(1);
                        this.setHunger(this.getHunger() + 20);
                        this.heal(Math.min(this.getHealth(), (int) (this.getMaxHealth() / 2)));
                        this.playSound(SoundEvents.GENERIC_EAT.value(), this.getSoundVolume(), this.getVoicePitch());
                        this.spawnItemCrackParticles(stackItem);
                        this.spawnItemCrackParticles(Items.BONE);
                        this.spawnItemCrackParticles(Items.BONE_MEAL);
                        this.eatFoodBonus(stack);
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        return InteractionResult.SUCCESS;
                    } else if (stackItem == IafItemRegistry.SICKLY_DRAGON_MEAL && !this.isAgingDisabled()) {
                        this.setHunger(this.getHunger() + 20);
                        this.heal(this.getMaxHealth());
                        this.playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, this.getSoundVolume(), this.getVoicePitch());
                        this.spawnItemCrackParticles(stackItem);
                        this.spawnItemCrackParticles(Items.BONE);
                        this.spawnItemCrackParticles(Items.BONE_MEAL);
                        this.spawnItemCrackParticles(Items.POISONOUS_POTATO);
                        this.spawnItemCrackParticles(Items.POISONOUS_POTATO);
                        this.setAgingDisabled(true);
                        this.eatFoodBonus(stack);
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        return InteractionResult.SUCCESS;
                    } else if (stackItem == IafItemRegistry.DRAGON_STAFF) {
                        if (player.isShiftKeyDown()) {
                            if (this.hasHomePosition) {
                                this.hasHomePosition = false;
                                player.sendOverlayMessage(Component.translatable("dragon.command.remove_home"));
                                return InteractionResult.SUCCESS;
                            } else {
                                BlockPos pos = this.blockPosition();
                                this.homePos = new HomePosition(pos, this.level());
                                this.hasHomePosition = true;
                                player.sendOverlayMessage(Component.translatable("dragon.command.new_home", pos.getX(), pos.getY(), pos.getZ(), homePos.getDimension()));
                                return InteractionResult.SUCCESS;
                            }
                        } else {
                            this.playSound(SoundEvents.ZOMBIE_INFECT, this.getSoundVolume(), this.getVoicePitch());
                            if (!level().isClientSide()) {
                                this.setCommand(this.getCommand() + 1);
                                if (this.getCommand() > 2) {
                                    this.setCommand(0);
                                }
                            }
                            String commandText = "stand";
                            if (this.getCommand() == 1) {
                                commandText = "sit";
                            } else if (this.getCommand() == 2) {
                                commandText = "escort";
                            }
                            player.sendOverlayMessage(Component.translatable("dragon.command." + commandText));
                            return InteractionResult.SUCCESS;
                        }
                    }
                }
            }
        }
        return super.mobInteract(player, hand);

    }

    protected abstract ItemLike getHeartItem();

    protected abstract Item getBloodItem();

    protected ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    private ItemStack getRandomDrop() {
        ItemStack stack = getItemFromLootTable();
        if (stack.getItem() == IafItemRegistry.DRAGON_BONE) {
            this.playSound(SoundEvents.SKELETON_AMBIENT, 1, 1);
        } else {
            this.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 1, 1);
        }
        return stack;
    }

    public boolean canPositionBeSeen(final double x, final double y, final double z) {
        final HitResult result = this.level().clip(new ClipContext(new Vec3(this.getX(), this.getY() + (double) this.getEyeHeight(), this.getZ()), new Vec3(x, y, z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        final double dist = result.getLocation().distanceToSqr(x, y, z);
        return dist <= 1.0D || result.getType() == HitResult.Type.MISS;
    }

    public abstract Identifier getDeadLootTable();

    public ItemStack getItemFromLootTable() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return ItemStack.EMPTY;
        }
        LootTable loottable = serverLevel.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, getDeadLootTable()));
        LootParams params = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .withParameter(LootContextParams.ORIGIN, this.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, this.damageSources().generic())
            .create(LootContextParamSets.ENTITY);
        for (ItemStack itemstack : loottable.getRandomItems(params)) {
            return itemstack;
        }
        return ItemStack.EMPTY;
    }

    public void eatFoodBonus(ItemStack stack) {

    }


    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    public void growDragon(final int ageInDays) {
        if (this.isAgingDisabled()) {
            return;
        }
        this.setAgeInDays(this.getAgeInDays() + ageInDays);
        //TODO: Probably brakes bounding boxes
        this.setBoundingBox(this.getBoundingBox());
        if (level().isClientSide()) {
            if (this.getAgeInDays() % 25 == 0) {
                for (int i = 0; i < this.getRenderSize() * 4; i++) {
                    final float f = (float) (getRandom().nextFloat() * (this.getBoundingBox().maxX - this.getBoundingBox().minX) + this.getBoundingBox().minX);
                    final float f1 = (float) (getRandom().nextFloat() * (this.getBoundingBox().maxY - this.getBoundingBox().minY) + this.getBoundingBox().minY);
                    final float f2 = (float) (getRandom().nextFloat() * (this.getBoundingBox().maxZ - this.getBoundingBox().minZ) + this.getBoundingBox().minZ);
                    final double motionX = getRandom().nextGaussian() * 0.07D;
                    final double motionY = getRandom().nextGaussian() * 0.07D;
                    final double motionZ = getRandom().nextGaussian() * 0.07D;

                    this.level().addParticle(ParticleTypes.HAPPY_VILLAGER, f, f1, f2, motionX, motionY, motionZ);
                }
            }
        }
        if (this.getDragonStage() >= 2)
            this.removeVehicle();
        this.updateAttributes();
    }

    public void spawnItemCrackParticles(Item item) {
        for (int i = 0; i < 15; i++) {
            final double motionX = getRandom().nextGaussian() * 0.07D;
            final double motionY = getRandom().nextGaussian() * 0.07D;
            final double motionZ = getRandom().nextGaussian() * 0.07D;
            final Vec3 headVec = this.getHeadPosition();
            if (!level().isClientSide()) {
                ((ServerLevel) this.level()).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item), headVec.x, headVec.y, headVec.z, 1, motionX, motionY, motionZ, 0.1);
            } else {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, item), headVec.x, headVec.y, headVec.z, motionX, motionY, motionZ);
            }
        }
    }

    public boolean isTimeToWake() {
        return this.level().isBrightOutside() || this.getCommand() == 2;
    }

    private boolean isStuck() {
        return !this.isChained() && !this.isTame() && (!this.getNavigation().isDone() && (this.getNavigation().getPath() == null || this.getNavigation().getPath().getEndNode() != null && this.distanceToSqr(this.getNavigation().getPath().getEndNode().x, this.getNavigation().getPath().getEndNode().y, this.getNavigation().getPath().getEndNode().z) > 15)) && ticksStill > 80 && !this.isHovering() && canMove();
    }

    protected boolean isOverAir() {
        return isOverAir;
    }

    private boolean isOverAirLogic() {
        return level().isEmptyBlock(BlockPos.containing(this.getX(), this.getBoundingBox().minY - 1, this.getZ()));
    }

    public boolean isDiving() {
        return false;//isFlying() && motionY < -0.2;
    }

    public boolean isBeyondHeight() {
        if (this.getY() > this.level().getMaxY()) {
            return true;
        }
        return this.getY() > IafConfig.maxDragonFlight;
    }

    private int calculateDownY() {
        if (this.getNavigation().getPath() != null) {
            Path path = this.getNavigation().getPath();
            Vec3 p = path.getEntityPosAtNode(this, Math.min(path.getNodeCount() - 1, path.getNextNodeIndex() + 1));
            if (p.y < this.getY() - 1) {
                return -1;
            }
        }
        return 1;
    }

    public void breakBlock() {
        if (this.blockBreakCounter > 0 || IafConfig.dragonBreakBlockCooldown == 0) {
            --this.blockBreakCounter;
            if (!this.isIceInWater() && (this.blockBreakCounter == 0 || IafConfig.dragonBreakBlockCooldown == 0) && this.level() instanceof ServerLevel griefLevel && EntityUtil.rule(griefLevel, GameRules.MOB_GRIEFING)) {
                if (IafConfig.dragonGriefing != 2 && !this.keepsBlocks() && (!this.isTame() || IafConfig.tamedDragonGriefing)) {
                    if (!isModelDead() && this.getDragonStage() >= 3 && (this.canMove() || this.getControllingPassenger() != null)) {
                        final int bounds = 1;//(int)Math.ceil(this.getRenderSize() * 0.1);
                        final int flightModifier = isFlying() && this.getTarget() != null ? -1 : 1;
                        final int yMinus = calculateDownY();
                        BlockPos.betweenClosedStream(
                            (int) Math.floor(this.getBoundingBox().minX) - bounds,
                            (int) Math.floor(this.getBoundingBox().minY) + yMinus,
                            (int) Math.floor(this.getBoundingBox().minZ) - bounds,
                            (int) Math.floor(this.getBoundingBox().maxX) + bounds,
                            (int) Math.floor(this.getBoundingBox().maxY) + bounds + flightModifier,
                            (int) Math.floor(this.getBoundingBox().maxZ) + bounds
                        ).forEach(pos -> {

                            final BlockState state = level().getBlockState(pos);
                            final float hardness = IafConfig.dragonGriefing == 1 || this.getDragonStage() <= 3 ? 2.0F : 5.0F;
                            if (isBreakable(pos, state, hardness)) {
                                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6F, 1, 0.6F));
                                if (!level().isClientSide()) {
                                    if (random.nextFloat() <= IafConfig.dragonBlockBreakingDropChance && DragonUtils.canDropFromDragonBlockBreak(state)) {
                                        level().destroyBlock(pos, true);
                                    } else {
                                        level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    protected boolean isBreakable(BlockPos pos, BlockState state, float hardness) {
        return !state.getCollisionShape(level(), pos).isEmpty() && !state.isAir() && state.getFluidState().isEmpty() && !state.getShape(level(), pos).isEmpty() && state.getDestroySpeed(level(), pos) >= 0F && state.getDestroySpeed(level(), pos) <= hardness && DragonUtils.canDragonBreak(state.getBlock()) && this.canDestroyBlock(pos, state)
            && !com.iafabric.boss.CastleWard.sealed(level(), pos);
    }

    @Override
    public boolean isBlockExplicitlyPassable(BlockState state, BlockPos pos, BlockPos entityPos) {
        if (!isModelDead() && this.getDragonStage() >= 3) {
            if (IafConfig.dragonGriefing != 2 && !this.keepsBlocks() && (!this.isTame() || IafConfig.tamedDragonGriefing) && pos.getY() >= this.getY()) {
                return isBreakable(pos, state, IafConfig.dragonGriefing == 1 || this.getDragonStage() <= 3 ? 2.0F : 5.0F);
            }
        }
        return false;
    }

    @Override
    public boolean isBlockExplicitlyNotPassable(BlockState state, BlockPos pos, BlockPos entityPos) {
        return false;
    }

    protected boolean isIceInWater() {
        return false;
    }

    public void spawnGroundEffects() {
        if (level().isClientSide()) {
            for (int i = 0; i < this.getRenderSize(); i++) {
                for (int i1 = 0; i1 < 20; i1++) {
                    final float radius = 0.75F * (0.7F * getRenderSize() / 3) * -3;
                    final float angle = (0.01745329251F * this.yBodyRot) + i1 * 1F;
                    final double extraX = radius * Mth.sin((float) (Math.PI + angle));
                    final double extraY = 0.8F;
                    final double extraZ = radius * Mth.cos(angle);
                    final BlockPos ground = getGround(new BlockPos(Mth.floor(this.getX() + extraX), Mth.floor(this.getY() + extraY) - 1, Mth.floor(this.getZ() + extraZ)));
                    final BlockState BlockState = this.level().getBlockState(ground);
                    if (!BlockMaterial.AIR.is(BlockState)) {
                        final double motionX = getRandom().nextGaussian() * 0.07D;
                        final double motionY = getRandom().nextGaussian() * 0.07D;
                        final double motionZ = getRandom().nextGaussian() * 0.07D;

                        level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, BlockState), true, false, this.getX() + extraX, ground.getY() + extraY, this.getZ() + extraZ, motionX, motionY, motionZ);
                    }
                }
            }
        }
    }

    private BlockPos getGround(BlockPos blockPos) {
        while (level().isEmptyBlock(blockPos) && blockPos.getY() > 1) {
            blockPos = blockPos.below();
        }
        return blockPos;
    }

    public void fall(float distance, float damageMultiplier) {

    }

    public boolean isActuallyBreathingFire() {
        return this.fireTicks > 20 && this.isBreathingFire();
    }

    public boolean doesWantToLand() {
        return this.flyTicks > 6000 || isGoingDown() || flyTicks > 40 && this.flyProgress == 0 || this.isChained() && flyTicks > 100;
    }

    public abstract String getVariantName(int variant);


    public int getDreadLook() {
        return this.entityData.get(DREAD_LOOK);
    }

    public void setDreadLook(int look) {
        this.entityData.set(DREAD_LOOK, (byte) look);
    }

    /** The rider drawn on its back (DragonRiders): 0 none, 1 a pillager, 2 the warlord, 3 a dread knight. */
    public int getVirtualRider() {
        return this.entityData.get(VIRTUAL_RIDER);
    }

    public void setVirtualRider(int kind) {
        this.entityData.set(VIRTUAL_RIDER, (byte) kind);
    }

    /** A dragon of the pillagers or of the dread: led by its riders, it breaks no blocks. */
    public boolean keepsBlocks() {
        return this.pillagerMount || this.dreadMount;
    }

    /** Black Frost, the Dread Queen's dragon. */
    public boolean isBlackFrost() {
        return this.getDreadLook() == 2;
    }

    /**
     * A rider it carries on its back without being tamed: a pillager on a kingdom dragon, a dread rider on a dread dragon
     * (Cryona on Black Frost). The client knows a dread dragon by its look (its dreadMount is the server's alone): without
     * it Cryona was drawn held in Black Frost's jaws, on the ground before it, instead of on its back.
     */
    public boolean carriesRider(Entity passenger) {
        return this.pillagerMount && passenger instanceof net.minecraft.world.entity.monster.illager.AbstractIllager
            || (this.dreadMount || this.getDreadLook() > 0) && passenger instanceof EntityDreadMob;
    }

    @Override
    public void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        super.positionRider(passenger, moveFunction);
        if (this.hasPassenger(passenger)) {
            if (this.carriesRider(passenger)) {
                // its rider (a pillager of the kingdom, the Dread Queen...) sits on its back, not in its jaws
                Vec3 riderPos = this.getRiderPosition();
                passenger.setPos(riderPos.x, this.carriedSeatY(riderPos.y), riderPos.z);
                if (passenger instanceof LivingEntity living) {
                    living.yBodyRot = this.yBodyRot;
                }
                return;
            }
            if (this.getControllingPassenger() == null || !this.getControllingPassenger().getUUID().equals(passenger.getUUID())) {
                updatePreyInMouth(passenger);
            } else {
                if (this.isModelDead()) {
                    passenger.stopRiding();
                }

                this.setYRot(passenger.getYRot());
                this.setYHeadRot(passenger.getYHeadRot());
                this.setXRot(passenger.getXRot());

                Vec3 riderPos = this.getRiderPosition();
                passenger.setPos(riderPos.x, riderPos.y + passenger.getBbHeight(), riderPos.z);
            }
        }
    }

    private float bob(float speed, float degree, boolean bounce, float f, float f1) {
        final double a = Mth.sin(f * speed) * f1 * degree;
        float bob = (float) (a - f1 * degree);
        if (bounce) {
            bob = (float) -Math.abs(a);
        }
        return bob * this.getRenderSize() / 3;
    }

    protected void updatePreyInMouth(Entity prey) {
        if (this.getAnimation() != ANIMATION_SHAKEPREY) {
            this.setAnimation(ANIMATION_SHAKEPREY);
        }
        if (this.getAnimation() == ANIMATION_SHAKEPREY && this.getAnimationTick() > 55 && prey != null) {
            prey.hurtOrSimulate(prey.damageSources().mobAttack(this), prey instanceof Player ? 17F : (float) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue() * 4);
            prey.stopRiding();
        }
        yBodyRot = getYRot();
        final float modTick_0 = this.getAnimationTick() - 25;
        final float modTick_1 = this.getAnimationTick() > 25 && this.getAnimationTick() < 55 ? 8 * Mth.clamp(Mth.sin((float) (Math.PI + modTick_0 * 0.25)), -0.8F, 0.8F) : 0;
        final float modTick_2 = this.getAnimationTick() > 30 ? 10 : Math.max(0, this.getAnimationTick() - 20);
        final float radius = 0.75F * (0.6F * getRenderSize() / 3) * -3;
        final float angle = (0.01745329251F * this.yBodyRot) + 3.15F + (modTick_1 * 2F) * 0.015F;
        final double extraX = radius * Mth.sin((float) (Math.PI + angle));
        final double extraZ = radius * Mth.cos(angle);
        final double extraY = modTick_2 == 0 ? 0 : 0.035F * ((getRenderSize() / 3) + (modTick_2 * 0.5 * (getRenderSize() / 3)));
        prey.setPos(this.getX() + extraX, this.getY() + extraY, this.getZ() + extraZ);
    }

    public int getDragonStage() {
        final int age = this.getAgeInDays();
        if (age >= 100) {
            return 5;
        } else if (age >= 75) {
            return 4;
        } else if (age >= 50) {
            return 3;
        } else if (age >= 25) {
            return 2;
        } else {
            return 1;
        }
    }

    public boolean isTeen() {
        return getDragonStage() < 4 && getDragonStage() > 2;
    }

    public boolean shouldDropLoot() {
        return getDragonStage() >= 4;
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return this.shouldDropLoot() && super.shouldDropLoot(level);
    }

    /** Dragons are babies by stage, not by the vanilla age timer (which still counts the breeding cooldown). */
    @Override
    public int getAge() {
        int age = super.getAge();
        return getDragonStage() < 2 ? Math.min(age, -1) : Math.max(age, 0);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, EntitySpawnReason reason, @Nullable SpawnGroupData spawnDataIn) {
        spawnDataIn = super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn);
        this.setGender(this.getRandom().nextBoolean());
        final int age = this.getRandom().nextInt(80) + 1;
        this.growDragon(age);
        this.setVariant(RandomSource.create().nextInt(4));
        this.setInSittingPose(false);
        this.updateAttributes();
        final double healthStep = (maximumHealth - minimumHealth) / 125;
        this.heal((Math.round(minimumHealth + (healthStep * age))));
        this.usingGroundAttack = true;
        this.setHunger(50);
        return spawnDataIn;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource dmg, float i) {
        if (this.isModelDead() && dmg != damageSources().fellOutOfWorld()) {
            return false;
        }
        if (dmg.getEntity() == this && dmg.getDirectEntity() != this) {
            // its own rider's bolt or its own fire coming back on it
            return false;
        }
        if (this.isBlackFrost()) {
            if (com.iafabric.boss.ThroneFight.waiting(this)) {
                // (before the duel is accepted nothing touches it)
                return false;
            }
            // stunned in its fight it takes half again; shielded while Cryona on its back calls Absolute Zero
            i = com.iafabric.boss.BlackFrostFight.damageTaken(this, i);
        }
        if (this.isVehicle() && dmg.getEntity() != null && this.getControllingPassenger() != null && dmg.getEntity() == this.getControllingPassenger()) {
            return false;
        }

        if ((dmg.getMsgId().contains("arrow") || getVehicle() != null && dmg.getEntity() != null && dmg.getEntity().is(this.getVehicle())) && this.isPassenger()) {
            return false;
        }

        if (dmg.is(DamageTypes.IN_WALL) || dmg.is(DamageTypes.FALLING_BLOCK) || dmg.is(DamageTypes.CRAMMING)) {
            return false;
        }
        if (!level().isClientSide() && dmg.getEntity() != null && this.getRandom().nextInt(4) == 0) {
            this.roar();
        }
        if (i > 0) {
            if (this.isSleeping()) {
                this.setInSittingPose(false);
                if (!this.isTame()) {
                    if (dmg.getEntity() instanceof Player) {
                        this.setTarget((Player) dmg.getEntity());
                    }
                }
            }
        }
        return super.hurtServer(level, dmg, i);

    }


    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        final float scale = Math.min(this.getRenderSize() * 0.35F, 7F);
//        double prevX = getPosX();
//        double prevY = getPosY();
//        double prevZ = getPosZ();
//        float localWidth = this.getWidth();
//        if (this.getWidth() > localWidth && !this.firstUpdate && !this.world.isRemote) {
//            this.setPosition(prevX, prevY, prevZ);
//        }
        if (scale != lastScale) {
            resetParts(this.getRenderSize() / 3);
        }
        lastScale = scale;
    }

    public float getStepHeight() {
        return Math.max(1.2F, 1.2F + (Math.min(this.getAgeInDays(), 125) - 25) * 1.8F / 100F);
    }

    @Override
    public void tick() {
        super.tick();
        refreshDimensions();
        updateParts();
        if (this.riderHurtTime > 0) {
            this.riderHurtTime--;
        }
        this.prevDragonPitch = getDragonPitch();
        Profiler.get().push("dragonLogic");
        this.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(getStepHeight());
        isOverAir = isOverAirLogic();
        logic.updateDragonCommon();
        if (this.isModelDead()) {
            if (!level().isClientSide() && level().isEmptyBlock(BlockPos.containing(this.getX(), this.getBoundingBox().minY, this.getZ())) && this.getY() > -1) {
                this.move(MoverType.SELF, new Vec3(0, -0.2F, 0));
            }
            this.setBreathingFire(false);

            float dragonPitch = this.getDragonPitch();
            if (dragonPitch > 0) {
                dragonPitch = Math.min(0, dragonPitch - 5);
                this.setDragonPitch(dragonPitch);
            }
            if (dragonPitch < 0) {
                this.setDragonPitch(Math.max(0, dragonPitch + 5));
            }
        } else {
            if (level().isClientSide()) {
                logic.updateDragonClient();
            } else {
                logic.updateDragonServer();
                logic.updateDragonAttack();
            }
        }
        Profiler.get().pop();
        Profiler.get().push("dragonFlight");
        if (useFlyingPathFinder() && !level().isClientSide() && isLocalInstanceAuthoritative()) {
            this.flightManager.update();
        }
        Profiler.get().pop();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.prevModelDeadProgress = this.modelDeadProgress;
        this.prevDiveProgress = this.diveProgress;
        prevAnimationProgresses[0] = this.sitProgress;
        prevAnimationProgresses[1] = this.sleepProgress;
        prevAnimationProgresses[2] = this.hoverProgress;
        prevAnimationProgresses[3] = this.flyProgress;
        prevAnimationProgresses[4] = this.fireBreathProgress;
        prevAnimationProgresses[5] = this.ridingProgress;
        prevAnimationProgresses[6] = this.tackleProgress;
        if (level().getDifficulty() == Difficulty.PEACEFUL && this.getTarget() instanceof Player) {
            this.setTarget(null);
        }
        if (this.isModelDead()) {
            if (this.isVehicle()) {
                this.ejectPassengers();
            }

            this.setHovering(false);
            this.setFlying(false);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
        if (animationTick > this.getAnimation().getDuration() && !level().isClientSide()) {
            animationTick = 0;
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(@NotNull Pose poseIn) {
        return this.getType().getDimensions().scale(this.getDragonScale());
    }

    /** The size factor of the hitbox (the old getScale, now final in LivingEntity). */
    public float getDragonScale() {
        return Math.min(this.getRenderSize() * 0.35F, 7F);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGroundIn, @NotNull BlockState state, @NotNull BlockPos pos) {
    }

    public float getRenderSize() {
        final int stage = this.getDragonStage() - 1;
        final float step = (growth_stages[stage][1] - growth_stages[stage][0]) / 25;
        if (this.getAgeInDays() > 125) {
            return growth_stages[stage][0] + (step * 25);
        }
        return growth_stages[stage][0] + (step * this.getAgeFactor());
    }

    private int getAgeFactor() {
        return (this.getDragonStage() > 1 ? this.getAgeInDays() - (25 * (this.getDragonStage() - 1)) : this.getAgeInDays());
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity entityIn) {
        this.getLookControl().setLookAt(entityIn, 30.0F, 30.0F);
        if (this.isTackling() || this.isModelDead()) {
            return false;
        }

        final boolean flag = entityIn.hurtOrSimulate(entityIn.damageSources().mobAttack(this), ((int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue()));

        if (flag) {
            EnchantmentHelper.doPostAttackEffects(level, entityIn, this.damageSources().mobAttack(this));
        }

        return flag;
    }

    @Override
    public void rideTick() {
        Entity entity = this.getVehicle();
        if (this.isPassenger() && !entity.isAlive()) {
            this.stopRiding();
        } else {
            this.setDeltaMovement(0, 0, 0);
            this.tick();
            if (this.isPassenger()) {
                this.updateRiding(entity);
            }
        }
    }

    public void updateRiding(Entity riding) {
        if (riding != null && riding.hasPassenger(this) && riding instanceof Player) {
            final int i = riding.getPassengers().indexOf(this);
            final float radius = (i == 2 ? -0.2F : 0.5F) + (((Player) riding).isFallFlying() ? 2 : 0);
            final float angle = (0.01745329251F * ((Player) riding).yBodyRot) + (i == 1 ? 90 : i == 0 ? -90 : 0);
            final double extraX = radius * Mth.sin((float) (Math.PI + angle));
            final double extraZ = radius * Mth.cos(angle);
            final double extraY = (riding.isShiftKeyDown() ? 1.2D : 1.4D) + (i == 2 ? 0.4D : 0D);
            this.yHeadRot = ((Player) riding).yHeadRot;
            this.setYRot(((Player) riding).yHeadRot);
            // TODO: consider removing this, up and down is handled in travel()
            this.setPos(riding.getX() + extraX, riding.getY() + extraY, riding.getZ() + extraZ);
            if ((this.getControlState() == 1 << 4 || ((Player) riding).isFallFlying()) && !riding.isPassenger()) {
                this.stopRiding();
                if (level().isClientSide()) {
                    IafNetwork.sendToServer(new MessageStartRidingMob(this.getId(), false, true));
                }

            }

        }
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public Animation getAnimation() {
        if (this.isModelDead()) {
            return NO_ANIMATION;
        }
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        if (this.isModelDead()) {
            return;
        }
        currentAnimation = animation;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isSleeping() && !this.isModelDead() && !this.level().isClientSide()) {
            if (this.getAnimation() == this.NO_ANIMATION) {
                this.setAnimation(ANIMATION_SPEAK);
            }
            super.playAmbientSound();
        }
    }

    @Override
    protected void playHurtSound(@NotNull DamageSource source) {
        if (!this.isModelDead()) {
            if (this.getAnimation() == this.NO_ANIMATION && !this.level().isClientSide()) {
                this.setAnimation(ANIMATION_SPEAK);
            }
            super.playHurtSound(source);
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{IAnimatedEntity.NO_ANIMATION, EntityDragonBase.ANIMATION_EAT};
    }

    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel serverWorld, @NotNull AgeableMob ageable) {
        return null;
    }

    /** A male and a female of the same kind, both grown (stage 4 or 5): only grown females lay eggs. */
    @Override
    public boolean canMate(@NotNull Animal otherAnimal) {
        if (otherAnimal instanceof EntityDragonBase dragon && otherAnimal != this && otherAnimal.getClass() == this.getClass()
            && this.getDragonStage() >= 4 && dragon.getDragonStage() >= 4) {
            return this.isMale() && !dragon.isMale() || !this.isMale() && dragon.isMale();
        }
        return false;
    }

    /** A female old enough to lay eggs (stage 4 or 5): only her body gives one. */
    public boolean laysEggs() {
        return !this.isMale() && this.getDragonStage() >= 4;
    }

    public EntityDragonEgg createEgg(EntityDragonBase ageable) {
        EntityDragonEgg dragon = new EntityDragonEgg(IafEntityRegistry.DRAGON_EGG, this.level());
        dragon.setEggType(EnumDragonEgg.byMetadata(RandomSource.create().nextInt(4) + getStartMetaForType()));
        dragon.setPos(Mth.floor(this.getX()) + 0.5, Mth.floor(this.getY()) + 1, Mth.floor(this.getZ()) + 0.5);
        return dragon;
    }

    public int getStartMetaForType() {
        return 0;
    }

    public boolean isTargetBlocked(Vec3 target) {
        if (target != null) {
            final BlockHitResult rayTrace = this.level().clip(new ClipContext(this.position().add(0, this.getEyeHeight(), 0), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            final BlockPos sidePos = rayTrace.getBlockPos();
            if (!level().isEmptyBlock(sidePos)) {
                return true;
            } else if (!level().isEmptyBlock(BlockPos.containing(rayTrace.getLocation()))) {
                return true;
            }
            return rayTrace.getType() == HitResult.Type.BLOCK;
        }
        return false;
    }

    private double getFlySpeed() {
        return (2 + (this.getAgeInDays() / 125) * 2) * (this.isTackling() ? 2 : 1);
    }

    public boolean isTackling() {
        return this.entityData.get(TACKLE);
    }

    public void setTackling(boolean tackling) {
        this.entityData.set(TACKLE, tackling);
    }

    public boolean isAgingDisabled() {
        return this.entityData.get(AGINGDISABLED);
    }

    public void setAgingDisabled(boolean isAgingDisabled) {
        this.entityData.set(AGINGDISABLED, isAgingDisabled);
    }

    public boolean isBoundToCrystal() {
        return this.entityData.get(CRYSTAL_BOUND);
    }

    public void setCrystalBound(boolean crystalBound) {
        this.entityData.set(CRYSTAL_BOUND, crystalBound);
    }


    public float getDistanceSquared(Vec3 Vector3d) {
        final float f = (float) (this.getX() - Vector3d.x);
        final float f1 = (float) (this.getY() - Vector3d.y);
        final float f2 = (float) (this.getZ() - Vector3d.z);
        return f * f + f1 * f1 + f2 * f2;
    }

    public abstract Item getVariantScale(int variant);

    public abstract Item getVariantEgg(int variant);

    public abstract Item getSummoningCrystal();



    @Override
    public boolean isImmobile() {
        return this.getHealth() <= 0.0F || isOrderedToSit() && !this.isVehicle() || this.isModelDead() || this.isPassenger();
    }

    @Override
    public boolean isInWater() {
        return super.isInWater() && this.getFluidHeight(FluidTags.WATER) > Mth.floor(this.getDragonStage() / 2.0f);
    }

    public final boolean IGNORE_PHYSICS_ON_SERVER = false;
    public boolean allowLocalMotionControl = true;
    public boolean allowMousePitchControl = true;
    protected boolean gliding = false;
    protected float glidingSpeedBonus = 0;

    @Override
    public void travel(@NotNull Vec3 pTravelVector) {
        if (this.getAnimation() == ANIMATION_SHAKEPREY || !this.canMove() && !this.isVehicle() || this.isOrderedToSit()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            pTravelVector = new Vec3(0, 0, 0);
        }
        // Player riding controls
        // Note: when motion is handled by the client no server side setDeltaMovement() should be called
        // otherwise the movement will halt
        // Todo: move wrongly fix
        if (allowLocalMotionControl && this.getControllingPassenger() != null) {
            LivingEntity rider = (LivingEntity) this.getControllingPassenger();
            if (rider == null) {
                super.travel(pTravelVector);
                return;
            }

            // Flying control, include flying through waterfalls
            if (isHovering() || isFlying()) {
                double forward = rider.zza;
                double strafing = rider.xxa;
                double vertical = 0;
                float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);
                // Bigger difference in speed for young and elder dragons
//                float airSpeedModifier = (float) (5.2f + 1.0f * Mth.map(Math.min(this.getAgeInDays(), 125), 0, 125, 0f, 1.5f));
                float airSpeedModifier = (float) (5.2f + 1.0f * Mth.map(speed, this.minimumSpeed, this.maximumSpeed, 0f, 1.5f));
                // Apply speed mod
                speed *= airSpeedModifier;
                // Set flag for logic and animation
                if (forward > 0) {
                    this.setFlying(true);
                    this.setHovering(false);
                }
                // Rider controlled tackling
                if (this.isAttacking() && this.getXRot() > -5 && this.getDeltaMovement().length() > 1.0d) {
                    this.setTackling(true);
//                } else if (this.getXRot() > 10 && this.getDeltaMovement().length() > 1.0d) {
//                    this.setDiving(true);
                    // Todo: diving animation here
                } else {
                    this.setTackling(false);
                }

                gliding = allowMousePitchControl && rider.isSprinting();
                if (!gliding) {
                    // Mouse controlled yaw
                    speed += glidingSpeedBonus;
                    // Slower on going astern
                    forward *= rider.zza > 0 ? 1.0f : 0.5f;
                    // Slower on going sideways
                    strafing *= 0.4f;
                    if (isGoingUp() && !isGoingDown()) {
                        vertical = 1f;
                    } else if (isGoingDown() && !isGoingUp()) {
                        vertical = -1f;
                    }
                    // Damp the vertical motion so the dragon's head is more responsive to the control
                    else if (this.isLocalInstanceAuthoritative()) {
//                        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0f, 0.8f, 1.0f));
                    }
                } else {
                    // Mouse controlled yaw and pitch
                    speed *= 1.5f;
                    strafing *= 0.1f;
                    // Diving is faster
                    // Todo: a new and better algorithm much like elytra flying
                    glidingSpeedBonus = (float) Mth.clamp(glidingSpeedBonus + this.getDeltaMovement().y * -0.05d, -0.8d, 1.5d);
                    speed += glidingSpeedBonus;
                    // Try to match the moving vector to the rider's look vector
                    forward = Mth.abs(Mth.cos(this.getXRot() * ((float) Math.PI / 180F)));
                    vertical = Mth.abs(Mth.sin(this.getXRot() * ((float) Math.PI / 180F)));
                    // Pitch is still responsive to spacebar and x key
                    if (isGoingUp() && !isGoingDown()) {
                        vertical = Math.max(vertical, 0.5);
                    } else if (isGoingDown() && !isGoingUp()) {
                        vertical = Math.min(vertical, -0.5);
                    } else if (isGoingUp() && isGoingDown()) {
                        vertical = 0;
                    }
                    // X rotation takes minus on looking upward
                    else if (this.getXRot() < 0) {
                        vertical *= 1;
                    } else if (this.getXRot() > 0) {
                        vertical *= -1;
                    } else if (this.isLocalInstanceAuthoritative()) {
//                        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0f, 0.8f, 1.0f));
                    }
                }
                // Speed bonus damping
                glidingSpeedBonus -= glidingSpeedBonus * 0.01d;

                if (this.isLocalInstanceAuthoritative()) {
                    // Vanilla friction on Y axis is smaller, which will influence terminal speed for climbing and diving
                    // use same friction coefficient on all axis simplifies how travel vector is computed
                    this.flyingSpeed = speed * 0.1F;
                    this.setSpeed(speed);

                    this.moveRelative(flyingSpeed, new Vec3(strafing, vertical, forward));
                    this.move(MoverType.SELF, this.getDeltaMovement());
                    this.setDeltaMovement(this.getDeltaMovement().multiply(new Vec3(0.9, 0.9, 0.9)));

                    Vec3 currentMotion = this.getDeltaMovement();
                    if (this.horizontalCollision) {
                        currentMotion = new Vec3(currentMotion.x, 0.1D, currentMotion.z);
                    }
                    this.setDeltaMovement(currentMotion);

                    this.calculateEntityAnimation(false);
                } else {
                    this.setDeltaMovement(Vec3.ZERO);
                }
                this.applyEffectsFromBlocks();
                this.updatePitch(this.yOld - this.getY());
                return;
            }
            // In water move control, for those that can't swim
            else if (isInWater() || isInLava()) {
                double forward = rider.zza;
                double strafing = rider.xxa;
                double vertical = 0;
                float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);

                if (isGoingUp() && !isGoingDown()) {
                    vertical = 0.5f;
                } else if (isGoingDown() && !isGoingUp()) {
                    vertical = -0.5f;
                }

                this.flyingSpeed = speed;
                // Float in water for those can't swim is done in LivingEntity#aiStep on server side
                // Leave this handled by both side before we have a better solution
                this.setSpeed(speed);
                // Overwrite the zza in setSpeed
                this.setZza((float) forward);
                // Vanilla in water behavior includes float on water and moving very slow
                // in lava behavior includes moving slow and sink
                super.travel(pTravelVector.add(strafing, vertical, forward));

                return;
            }
            // Walking control
            else {
                double forward = rider.zza;
                double strafing = rider.xxa;
                // Inherit y motion for dropping
                double vertical = pTravelVector.y;
                float speed = (float) this.getAttributeValue(Attributes.MOVEMENT_SPEED);

                float groundSpeedModifier = (float) (1.8F * this.getFlightSpeedModifier());
                speed *= groundSpeedModifier;
                // Try to match the original riding speed
                forward *= speed;
                // Faster sprint
                forward *= rider.isSprinting() ? 1.2f : 1.0f;
                // Slower going back
                forward *= rider.zza > 0 ? 1.0f : 0.2f;
                // Slower going sideway
                strafing *= 0.05f;

                if (this.isLocalInstanceAuthoritative()) {
                    this.flyingSpeed = speed * 0.1F;
                    this.setSpeed(speed);

                    // Vanilla walking behavior includes going up steps
                    super.travel(new Vec3(strafing, vertical, forward));
                } else {
                    this.setDeltaMovement(Vec3.ZERO);
                }
                this.applyEffectsFromBlocks();
                this.updatePitch(this.yOld - this.getY());
                return;
            }
        }
        // No rider move control
        else {
            super.travel(pTravelVector);
        }
    }

    /**
     * Update dragon pitch for the server on {@link IafDragonLogic#updateDragonServer()} <br>
     * For some reason the {@link LivingEntity#yo} failed to update the pitch properly when the movement is handled by client.
     * Use {@link LivingEntity#yOld} instead will properly update the pitch on server.
     * @param verticalDelta vertical distance from last update
     */
    protected void updatePitch(final double verticalDelta) {
        if (this.isOverAir() && !this.isPassenger()) {
            // Update the pitch when in air, and stepping up many blocks
            if (!this.isHovering()) {
                this.incrementDragonPitch((float) (verticalDelta) * 10);
            }
            this.setDragonPitch(Mth.clamp(this.getDragonPitch(), -60, 40));
            final float plateau = 2;
            final float planeDist = (float) ((Math.abs(this.getDeltaMovement().x) + Math.abs(this.getDeltaMovement().z)) * 6F);
            if (this.getDragonPitch() > plateau) {
                //down
                //this.motionY -= 0.2D;
                this.decrementDragonPitch(planeDist * Math.abs(this.getDragonPitch()) / 90);
            }
            if (this.getDragonPitch() < -plateau) {//-2
                //up
                this.incrementDragonPitch(planeDist * Math.abs(this.getDragonPitch()) / 90);
            }
            if (this.getDragonPitch() > 2F) {
                this.decrementDragonPitch(1);
            } else if (this.getDragonPitch() < -2F) {
                this.incrementDragonPitch(1);
            }
            if (this.getControllingPassenger() == null && this.getDragonPitch() < -45 && planeDist < 3) {
                if (this.isFlying() && !this.isHovering()) {
                    this.setHovering(true);
                }
            }
        } else {
            // Damp the pitch once on ground
            if (Mth.abs(this.getDragonPitch()) < 1) {
                this.setDragonPitch(0);
            } else {
                this.setDragonPitch(this.getDragonPitch() / 1.5f);
            }
        }
    }

    /**
     * Rider logic from {@link IafDragonLogic#updateDragonServer()} <br>
     * Updates when rider is onboard
     */
    public void updateRider() {
        Entity controllingPassenger = this.getControllingPassenger();

        if (controllingPassenger instanceof Player rider) {
            this.ticksStill = 0;
            this.hoverTicks = 0;
            this.flyTicks = 0;

            if (this.isGoingUp()) {
                if (!this.isFlying() && !this.isHovering()) {
                    // Update spacebar tick for take off
                    this.spacebarTicks += 2;
                }
            } else if (this.isDismounting()) {
                if (this.isFlying() || this.isHovering()) {
                    // If the rider decided to dismount in air, try to follow
                    this.setCommand(2);
                }
            }
            // Update spacebar ticks and take off
            if (this.spacebarTicks > 0) {
                this.spacebarTicks--;
            }
            // Hold spacebar 1 sec to take off
            if (this.spacebarTicks > 20 && this.getOwner() != null && this.getPassengers().contains(this.getOwner()) && !this.isFlying() && !this.isHovering()) {
                if (!this.isInWater()) {
                    this.setHovering(true);
                    this.spacebarTicks = 0;

                    this.glidingSpeedBonus = 0;
                }
            }
            if (isFlying() || isHovering()) {
                if (rider.zza > 0) {
                    this.setFlying(true);
                    this.setHovering(false);
                } else {
                    this.setFlying(false);
                    this.setHovering(true);
                }
                // Hitting terrain with big angle of attack
                if (!this.isOverAir() && this.isFlying() && rider.getXRot() > 10 && !this.isInWater()) {
                    this.setHovering(false);
                    this.setFlying(false);
                }
                // Dragon landing
                if (!this.isOverAir() && this.isGoingDown() && !this.isInWater()) {
                    this.setFlying(false);
                    this.setHovering(false);
                }
            }

            // Dragon tackle attack
            if (this.isTackling()) {
                // Todo: tackling too low will cause animation to disappear
                this.tacklingTicks++;
                if (this.tacklingTicks == 40) {
                    this.tacklingTicks = 0;
                }
                if (!this.isFlying() && this.onGround()) {
                    this.tacklingTicks = 0;
                    this.setTackling(false);
                }
                // Todo: problem with friendly fire to tamed horses
                List<Entity> victims = this.level().getEntities(this, this.getBoundingBox().expandTowards(2.0D, 2.0D, 2.0D), potentialVictim -> (
                        potentialVictim != rider
                                && potentialVictim instanceof LivingEntity
                ));
                victims.forEach(victim -> {
                    logic.attackTarget(victim, rider, this.getDragonStage() * 3);
                });
            }
            // Dragon breathe attack; a dragon forge in front of the rider takes the breath
            if (this.isStriking() && this.getControllingPassenger() != null && this.getDragonStage() > 1) {
                BlockPos forge = this.forgeInFrontOf(rider);
                if (forge != null) {
                    this.burningTarget = forge;
                } else {
                    this.riderShootFire(this.getControllingPassenger());
                }
                this.setBreathingFire(true);
                this.fireStopTicks = 10;
            }
            // Dragon bite attack
            if (this.isAttacking() && this.getControllingPassenger() != null && this.getControllingPassenger() instanceof Player) {
                LivingEntity target = DragonUtils.riderLookingAtEntity(this, (Player) this.getControllingPassenger(), this.getDragonStage() + (this.getBoundingBox().maxX - this.getBoundingBox().minX));
                if (this.getAnimation() != EntityDragonBase.ANIMATION_BITE) {
                    this.setAnimation(EntityDragonBase.ANIMATION_BITE);
                }
                if (target != null && !DragonUtils.hasSameOwner(this, target)) {
                    logic.attackTarget(target, rider instanceof Player ? (Player) rider : null, (int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue());
                }
            }
            // Shift key to dismount, on the ground (in the air it brings the dragon down)
            if (this.getControllingPassenger() != null && this.getControllingPassenger().isShiftKeyDown() && IFlyingMount.canDismount(this)) {
                if (this.getControllingPassenger() instanceof LivingEntity)
                    MiscProperties.setDismountedDragon((LivingEntity) this.getControllingPassenger(), true);
                this.getControllingPassenger().stopRiding();
            }
            // Reset attack target when being ridden
            if (this.getTarget() != null && !this.getPassengers().isEmpty() && this.getOwner() != null && this.getPassengers().contains(this.getOwner())) {
                this.setTarget(null);
            }
            // Stop flying when hit the water, but waterfalls do not block flying
            if (this.getInBlockState().getFluidState().isSource() && this.isInWater() && !this.isGoingUp()) {
                this.setFlying(false);
                this.setHovering(false);
            }
        } else if (controllingPassenger instanceof EntityDreadQueen) {
            // Original logic involves riding
            Player ridingPlayer = this.getRidingPlayer();
            if (ridingPlayer != null) {
                if (this.isGoingUp()) {
                    if (!this.isFlying() && !this.isHovering()) {
                        this.spacebarTicks += 2;
                    }
                } else if (this.isDismounting()) {
                    if (this.isFlying() || this.isHovering()) {
                        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
                        this.setFlying(false);
                        this.setHovering(false);
                    }
                }
            }
            if (!this.isDismounting() && (this.isFlying() || this.isHovering())) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.01, 0));
            }
            if (this.isStriking() && this.getControllingPassenger() != null && this.getDragonStage() > 1) {
                this.setBreathingFire(true);
                this.riderShootFire(this.getControllingPassenger());
                this.fireStopTicks = 10;
            }
            if (this.isAttacking() && this.getControllingPassenger() != null && this.getControllingPassenger() instanceof Player) {
                LivingEntity target = DragonUtils.riderLookingAtEntity(this, (Player) this.getControllingPassenger(), this.getDragonStage() + (this.getBoundingBox().maxX - this.getBoundingBox().minX));
                if (this.getAnimation() != EntityDragonBase.ANIMATION_BITE) {
                    this.setAnimation(EntityDragonBase.ANIMATION_BITE);
                }
                if (target != null && !DragonUtils.hasSameOwner(this, target)) {
                    logic.attackTarget(target, ridingPlayer, (int) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue());
                }
            }
            if (this.getControllingPassenger() != null && this.getControllingPassenger().isShiftKeyDown()) {
                if (this.getControllingPassenger() instanceof LivingEntity)
                    MiscProperties.setDismountedDragon((LivingEntity) this.getControllingPassenger(), true);
                this.getControllingPassenger().stopRiding();
            }
            if (this.isFlying()) {
                if (!this.isHovering() && this.getControllingPassenger() != null && !this.onGround() && Math.max(Math.abs(this.getDeltaMovement().x()), Math.abs(this.getDeltaMovement().z())) < 0.1F) {
                    this.setHovering(true);
                    this.setFlying(false);
                }
            } else {
                if (this.isHovering() && this.getControllingPassenger() != null && !this.onGround() && Math.max(Math.abs(this.getDeltaMovement().x()), Math.abs(this.getDeltaMovement().z())) > 0.1F) {
                    this.setFlying(true);
                    this.usingGroundAttack = false;
                    this.setHovering(false);
                }
            }
            if (this.spacebarTicks > 0) {
                this.spacebarTicks--;
            }
            if (this.spacebarTicks > 20 && this.getOwner() != null && this.getPassengers().contains(this.getOwner()) && !this.isFlying() && !this.isHovering()) {
                this.setHovering(true);
            }

            if (this.isVehicle() && !this.isOverAir() && this.isFlying() && !this.isHovering() && this.flyTicks > 40) {
                this.setFlying(false);
            }
        }
    }

    @Override
    public void setDeltaMovement(Vec3 pMotion) {
        super.setDeltaMovement(pMotion);
    }

    @Override
    public void move(@NotNull MoverType pType, @NotNull Vec3 pPos) {
        if (this.isOrderedToSit() && !this.isVehicle()) {
            pPos = new Vec3(0, pPos.y(), 0);
        }

        if (this.isVehicle()) {
            // When riding, the server side movement check is performed in ServerGamePacketListenerImpl#handleMoveVehicle
            // verticalCollide tag might get inconsistent due to dragon's large bounding box and causes move wrongly msg
            if (this.isLocalInstanceAuthoritative()) {
                // This is how EntityDragonBase#breakBlock handles movement when breaking blocks
                // it's done by server, however client does not fire server side events, so breakBlock() here won't work
                if (horizontalCollision) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(0.6F, 1, 0.6F));
                }
                super.move(pType, pPos);
            } else {
                // Use noPhysics tag to disable server side collision check
                this.noPhysics = IGNORE_PHYSICS_ON_SERVER;
                super.move(pType, pPos);
            }
            // Set no gravity flag to prevent getting kicked by flight disabled servers
            if (this.isHovering() || this.isFlying()) {
                this.setNoGravity(true);
            } else {
                this.setNoGravity(false);
            }

        } else {
            this.noPhysics = false;
            // The flight mgr is not ready for noGravity
            this.setNoGravity(false);
            super.move(pType, pPos);
        }

    }


    public void updateCheckPlayer() {
        final double checkLength = this.getBoundingBox().getSize() * 3;
        final Player player = level().getNearestPlayer(this, checkLength);
        if (this.isSleeping()) {
            if (player != null && !this.isOwnedBy(player) && !player.isCreative()) {
                this.setInSittingPose(false);
                this.setOrderedToSit(false);
                this.setTarget(player);
            }
        }
    }

    public boolean shouldDismountInWater(Entity rider) {
        return false;
    }

    public boolean isDirectPathBetweenPoints(Vec3 vec1, Vec3 vec2) {
        final BlockHitResult rayTrace = this.level().clip(new ClipContext(vec1, new Vec3(vec2.x, vec2.y + (double) this.getBbHeight() * 0.5D, vec2.z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return rayTrace.getType() != HitResult.Type.BLOCK;
    }

    /** Black Frost loses at most a hundredth of its life in a second, whatever strikes it (BossLimits). */
    @Override
    protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
        float before = this.getHealth();
        super.actuallyHurt(level, source, amount);
        if (this.isBlackFrost() && !com.iafabric.boss.BossLimits.unlimited(source)) {
            float lost = before - this.getHealth();
            float allowed = com.iafabric.boss.BossLimits.allow(this, lost, level.getGameTime());
            if (allowed < lost) {
                this.setHealth(before - allowed);
                // the cold holds the rest of the blow
                level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + this.getBbHeight() * 0.6, this.getZ(), 8,
                    this.getBbWidth() * 0.3, this.getBbHeight() * 0.3, this.getBbWidth() * 0.3, 0.02);
            }
            if (source.getDirectEntity() instanceof net.minecraft.server.level.ServerPlayer player && source.getEntity() == player) {
                // struck by a player's own hand, close by: its claws for them
                com.iafabric.boss.BlackFrostFight.of(this).struckBy(level, player);
            }
        }
    }

    /** Black Frost is a wall: no blow nor push moves it. */
    @Override
    public void knockback(double strength, double x, double z, DamageSource source, float amount, boolean b) {
        if (!com.iafabric.boss.BossLimits.immovable(this)) {
            super.knockback(strength, x, z, source, amount, b);
        }
    }

    @Override
    public void knockback(double strength, double x, double z, DamageSource source, float amount) {
        if (!com.iafabric.boss.BossLimits.immovable(this)) {
            super.knockback(strength, x, z, source, amount);
        }
    }

    @Override
    public boolean isPushable() {
        return !com.iafabric.boss.BossLimits.immovable(this) && super.isPushable();
    }

    @Override
    public void push(double x, double y, double z) {
        if (!com.iafabric.boss.BossLimits.immovable(this)) {
            super.push(x, y, z);
        }
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        super.die(cause);
        if (this.pillagerMount && this.level() instanceof ServerLevel server) {
            PillagerDragons.dropArmor(this, server);
        }
        if (this.level() instanceof ServerLevel server) {
            DragonRiders.onDragonDeath(this, server);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == DragonRiders.EVENT_RIDER_HURT) {
            this.riderHurtTime = 10;
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void onHearFlute(Player player) {
        if (this.isTame() && this.isOwnedBy(player)) {
            if (this.isFlying() || this.isHovering()) {
                this.setFlying(false);
                this.setHovering(false);
            }
        }
    }

    public abstract SoundEvent getRoarSound();

    public void roar() {
        if (EntityGorgon.isStoneMob(this) || this.isModelDead()) {
            return;
        }
        if (random.nextBoolean()) {
            if (this.getAnimation() != ANIMATION_EPIC_ROAR) {
                this.setAnimation(ANIMATION_EPIC_ROAR);
                this.playSound(this.getRoarSound(), this.getSoundVolume() + 3 + Math.max(0, this.getDragonStage() - 2), this.getVoicePitch() * 0.7F);
            }
            if (this.getDragonStage() > 3) {
                final int size = (this.getDragonStage() - 3) * 30;
                final List<Entity> entities = level().getEntities(this, this.getBoundingBox().expandTowards(size, size, size));
                for (final Entity entity : entities) {
                    final boolean isStrongerDragon = entity instanceof EntityDragonBase && ((EntityDragonBase) entity).getDragonStage() >= this.getDragonStage();
                    if (entity instanceof LivingEntity living && !isStrongerDragon) {
                        if (this.isOwnedBy(living) || this.isOwnersPet(living)) {
                            living.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 50 * size));
                        } else {
                            if (living.getItemBySlot(EquipmentSlot.HEAD).getItem() != IafItemRegistry.EARPLUGS) {
                                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50 * size));
                            }
                        }
                    }
                }
            }
        } else {
            if (this.getAnimation() != ANIMATION_ROAR) {
                this.setAnimation(ANIMATION_ROAR);
                this.playSound(this.getRoarSound(), this.getSoundVolume() + 2 + Math.max(0, this.getDragonStage() - 3), this.getVoicePitch());
            }
            if (this.getDragonStage() > 3) {
                final int size = (this.getDragonStage() - 3) * 30;
                final List<Entity> entities = level().getEntities(this, this.getBoundingBox().expandTowards(size, size, size));
                for (final Entity entity : entities) {
                    final boolean isStrongerDragon = entity instanceof EntityDragonBase && ((EntityDragonBase) entity).getDragonStage() >= this.getDragonStage();
                    if (entity instanceof LivingEntity living && !isStrongerDragon) {
                        if (this.isOwnedBy(living) || this.isOwnersPet(living)) {
                            living.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30 * size));
                        } else {
                            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30 * size));
                        }
                    }
                }
            }
        }
    }

    private boolean isOwnersPet(LivingEntity living) {
        return this.isTame() && this.getOwner() != null && living instanceof TamableAnimal && ((TamableAnimal) living).getOwner() != null && this.getOwner().is(((TamableAnimal) living).getOwner());
    }

    public boolean isDirectPathBetweenPoints(Entity entity, Vec3 vec1, Vec3 vec2) {

        HitResult movingobjectposition = this.level().clip(new ClipContext(vec1, vec2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return movingobjectposition.getType() != HitResult.Type.BLOCK;
    }

    public boolean shouldRenderEyes() {
        return !this.isSleeping() && !this.isModelDead() && !this.isBlinking() && !EntityGorgon.isStoneMob(this);
    }

    @Override
    public boolean shouldAnimalsFear(Entity entity) {
        return DragonUtils.canTameDragonAttack(this, entity);
    }

    @Override
    public void dropArmor() {

    }

    public boolean isChained() {
        return ChainProperties.hasChainData(this);
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource damageSourceIn, boolean attackedRecently) {
    }

    public HitResult rayTraceRider(Entity rider, double blockReachDistance, float partialTicks) {
        Vec3 Vector3d = rider.getEyePosition(partialTicks);
        Vec3 Vector3d1 = rider.getViewVector(partialTicks);
        Vec3 Vector3d2 = Vector3d.add(Vector3d1.x * blockReachDistance, Vector3d1.y * blockReachDistance, Vector3d1.z * blockReachDistance);
        return this.level().clip(new ClipContext(Vector3d, Vector3d2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    /**
     * Provide rider position deltas for a dragon of certain size <br>
     * These methods use quadratic regression on data below to provide a smooth transition on the position <br>
     * Stage 3 / 50 Days / 1200000 Ticks / Render size 7.0 / Scale 2.45      | XZ 1.5    Y 0.0 <br>
     * Stage 4 / 75 Days / 1800000 Ticks / Render size 12.5 / Scale 4.375    | XZ 2.9    Y 1.6 <br>
     * Stage 5 / 100 Days / 2400000 Ticks / Render size 20.0 / Scale 7.0     | XZ 5.2    Y 3.8 <br>
     * Stage 5 / 125 Days / 3000000 Ticks / Render size 30.0 / Scale 7.0     | XZ 8.8    Y 7.4 <br>
     * @return rider's vertical position delta, in blocks
     * @see EntityDragonBase#getRideHorizontalBase()
     */
    protected float getRideHeightBase() {
        return 0.002237889819f*Mth.square(getRenderSize()) + 0.233137174f*getRenderSize() + -1.717904311f;
    }

    /**
     * Provide a rough horizontal distance of rider's hitbox
     * @return rider's horizontal position delta, in blocks
     * @see EntityDragonBase#getRideHeightBase()
     */
    protected float getRideHorizontalBase() {
        return 0.00336283f*Mth.square(getRenderSize()) + 0.1934242516f*getRenderSize() + -0.02622133882f;
    }

    /**
     * Carrying a rider of its own (Cryona, a kingdom's pillager) its body turns with it: the game keeps a mob's body
     * still under a mob rider, so the dragon was drawn turned one way while its rider sat where its turn said (beside
     * its back, on its neck).
     */
    @Override
    protected void tickHeadTurn(float yBodyRot) {
        if (this.getFirstPassenger() instanceof Mob && !(this.getControllingPassenger() instanceof Player)) {
            this.yBodyRot = this.getYRot();
            if (this.isBlackFrost()) {
                // (Black Frost with Cryona on its back, in her duel: its head always with its body, as its fight turns it;
                // on a game the head's turn came a tick or two before the body's, bending its neck aside)
                this.yHeadRot = this.getYRot();
            }
            return;
        }
        super.tickHeadTurn(yBodyRot);
    }

    /**
     * Where a rider it carries on its back (Cryona, a pillager) is drawn this frame: its seat turned with its body as
     * drawn, tilted with it, and rising and sinking with its body's beat in the air as its model does
     * (DragonTabulaModelAnimator: the body bobs up to 5 model units while it hovers or flies).
     */
    public Vec3 drawnSeat(float partialTick, float bodyRot) {
        double x = Mth.lerp(partialTick, this.xo, this.getX());
        double y = Mth.lerp(partialTick, this.yo, this.getY());
        double z = Mth.lerp(partialTick, this.zo, this.getZ());
        float pitch = this.prevDragonPitch + (this.getDragonPitch() - this.prevDragonPitch) * partialTick;
        Vec3 seat = this.riderPosition(x, y, z, bodyRot, pitch);
        seat = new Vec3(seat.x, this.carriedSeatY(seat.y), seat.z);
        boolean inAir = this.isHovering() || this.isFlying() || this.hoverProgress > 0 || this.flyProgress > 0;
        if (inAir && !this.isNoAi()) {
            float age = this.tickCount + partialTick;
            float bob = (float) (Math.sin(age * -0.2F) * 2.5F - 2.5F);
            seat = seat.add(0, -bob / 16F * this.getRenderSize() / 3F, 0);
        }
        return seat;
    }

    /**
     * Where a rider of its own has its feet, for a seat's height (riderPosition, made for a player's view): its hips on
     * the top of its back there, measured on its model (0.92 of its size on the ground, 0.97 in the air: the same for the
     * three kinds). The player's seat, a body's height over the seat, floated a rider of its own over its back.
     */
    private double carriedSeatY(double seatY) {
        float air = Mth.clamp(Math.max(this.flyProgress, this.hoverProgress) / 20F, 0F, 1F);
        float back = (0.92F + 0.05F * air) * this.getRenderSize() / 3F;
        return seatY - this.getRideHeightBase() + back - 0.8F;
    }

    // For slowly raise rider position
    protected float riderWalkingExtraY = 0;
    public Vec3 getRiderPosition() {
        return this.riderPosition(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getDragonPitch());
    }

    /** Its rider's seat for a place, a turn and a tilt of its body. */
    protected Vec3 riderPosition(double x, double y, double z, float yaw, float dragonPitch) {
        // The old position is seems to be given by a series compute of magic numbers
        // So I replace the number with an even more magical yet better one I tuned
        // The rider's hitbox and pov is now closer to its model, and less model clipping in first person
        // Todo: a better way of computing rider position, and a more dynamic one that changes according to dragon's animation

        float extraXZ = 0;
        float extraY = 0;

        // Extra delta when going up and down
        float pitchXZ = 0F;
        float pitchY = 0F;
        if (dragonPitch > 0) {
            pitchXZ = Math.min(dragonPitch / 90, 0.2F);
            pitchY = -(dragonPitch / 90) * 0.6F;
        } else if (dragonPitch < 0) {//going up
            pitchXZ = Math.max(dragonPitch / 90, -0.5F);
            pitchY = (dragonPitch / 90) * 0.03F;
        }
//        float extraY = (pitchY + sitProg + hoverProg + deadProg + sleepProg + flyProg) * getRenderSize();
        extraXZ += pitchXZ * getRenderSize();
        extraY += pitchY * getRenderSize();

        // Extra delta when moving
        // The linear part of the tuning
        final float linearFactor = Mth.map(Math.max(Math.min(this.getAgeInDays(), 125) - 50, 0), 0, 75, 0, 1);
        // (a player's seat rises in flight and as they ride on; a rider of its own sits on its back as it is drawn)
        if (this.getControllingPassenger() instanceof Player rider) {
            // Extra height when rider and the dragon look upwards, this will reduce model clipping
            if (rider.getXRot() < 0) {
                extraY += Mth.map(rider.getXRot(), 60, -40, -0.1, 0.1);
            }
            if (this.isHovering() || this.isFlying()) {
                // Extra height when flying, reduces model clipping since dragon has a bigger amplitude when flying/hovering
                extraY += 1.1f * linearFactor;
                extraY += getRideHeightBase() * 0.6f;
            } else {
                // Extra height when walking, reduces model clipping
                if (rider.zza > 0) {
                    final float MAX_RAISE_HEIGHT = 1.1f * linearFactor + getRideHeightBase() * 0.1f;
                    riderWalkingExtraY = Math.min(MAX_RAISE_HEIGHT, riderWalkingExtraY + 0.1f);
                } else {
                    riderWalkingExtraY = Math.max(0, riderWalkingExtraY - 0.15f);
                }
                extraY += riderWalkingExtraY;
            }
        }

        final float xzMod = getRideHorizontalBase() + extraXZ;
//        final float xzMod = (0.15F + pitchXZ) * getRenderSize() + extraAgeScale;
        final float yMod = getRideHeightBase() + extraY;
        final double headPosX = x + xzMod * Mth.cos((float) ((yaw + 90) * Math.PI / 180));
//        final float headPosY = (float) (getY() + (0.7F + sitProg + hoverProg + deadProg + sleepProg + flyProg + pitchY) * getRenderSize() * 0.3F + this.getScale() * 0.2F);
        final double headPosY = y + yMod;
        final double headPosZ = z + xzMod * Mth.sin((float) ((yaw + 90) * Math.PI / 180));
        return new Vec3(headPosX, headPosY, headPosZ);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pPassenger) {
        return this.getRiderPosition().add(0, pPassenger.getBbHeight(), 0);
    }

    @Override
    public void kill(ServerLevel level) {
        this.remove(RemovalReason.KILLED);
        this.setDeathStage(this.getAgeInDays() / 5);
        this.setModelDead(false);
    }

    @Override
    protected boolean considersEntityAsAlly(@NotNull Entity entityIn) {
        // Workaround to make sure dragons won't be attacked when dead
        if (this.isModelDead())
            return true;
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
        if (this.dreadMount && (entityIn instanceof com.iafabric.entity.util.IDreadMob || entityIn instanceof EntityDragonBase other && other.dreadMount)) {
            return true;
        }
        if (this.pillagerMount && (entityIn instanceof net.minecraft.world.entity.raid.Raider || entityIn instanceof EntityDragonBase other && other.pillagerMount)) {
            return true;
        }

        return super.considersEntityAsAlly(entityIn);
    }

    public Vec3 getHeadPosition() {
        final float sitProg = this.sitProgress * 0.015F;
        final float deadProg = this.modelDeadProgress * -0.02F;
        final float hoverProg = this.hoverProgress * 0.03F;
        final float flyProg = this.flyProgress * 0.01F;
        int tick = 0;
        if (this.getAnimationTick() < 10) {
            tick = this.getAnimationTick();
        } else if (this.getAnimationTick() > 50) {
            tick = 60 - this.getAnimationTick();
        } else {
            tick = 10;
        }
        final float epicRoarProg = this.getAnimation() == ANIMATION_EPIC_ROAR ? tick * 0.1F : 0;
        final float sleepProg = this.sleepProgress * -0.025F;
        float pitchMulti = 0F;
        float pitchAdjustment = 0F;
        float pitchMinus = 0F;
        final float dragonPitch = -getDragonPitch();
        if (this.isFlying() || this.isHovering()) {
            pitchMulti = Mth.sin((float) Math.toRadians(dragonPitch));
            pitchAdjustment = 1.2F;
            pitchMulti *= 2.1F * Math.abs(dragonPitch) / 90;
            if (pitchMulti > 0) {
                pitchMulti *= 1.5F - pitchMulti * 0.5F;
            }
            if (pitchMulti < 0) {
                pitchMulti *= 1.3F - pitchMulti * 0.1F;
            }
            pitchMinus = 0.3F * Math.abs(dragonPitch / 90);
            if (dragonPitch >= 0) {
                pitchAdjustment = 0.6F * Math.abs(dragonPitch / 90);
                pitchMinus = 0.95F * Math.abs(dragonPitch / 90);
            }
        }
        final float flightXz = 1.0F + flyProg + hoverProg;
        final float xzMod = (1.7F * getRenderSize() * 0.3F * flightXz) + getRenderSize() * (0.3F * Mth.sin((float) ((dragonPitch + 90) * Math.PI / 180)) * pitchAdjustment - pitchMinus - hoverProg * 0.45F);
        final float headPosX = (float) (getX() + (xzMod) * Mth.cos((float) ((getYRot() + 90) * Math.PI / 180)));
        final float headPosY = (float) (getY() + (0.7F + sitProg + hoverProg + deadProg + epicRoarProg + sleepProg + flyProg + pitchMulti) * getRenderSize() * 0.3F);
        final float headPosZ = (float) (getZ() + (xzMod) * Mth.sin((float) ((getYRot() + 90) * Math.PI / 180)));
        return new Vec3(headPosX, headPosY, headPosZ);
    }

    public abstract void stimulateFire(double burnX, double burnY, double burnZ, int syncType);

    public void randomizeAttacks() {
        this.airAttack = IafDragonAttacks.Air.values()[getRandom().nextInt(IafDragonAttacks.Air.values().length)];
        this.groundAttack = IafDragonAttacks.Ground.values()[getRandom().nextInt(IafDragonAttacks.Ground.values().length)];
        DragonRiders.shapeAttacks(this);
    }

    @Override
    public boolean shouldBlockExplode(@NotNull Explosion explosionIn, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, BlockState blockStateIn, float explosionPower) {
        return !(blockStateIn.getBlock() instanceof IDragonProof) && DragonUtils.canDragonBreak(blockStateIn.getBlock());
    }

    public void tryScorchTarget() {
        LivingEntity entity = this.getTarget();
        if (entity != null) {
            final float distX = (float) (entity.getX() - this.getX());
            final float distZ = (float) (entity.getZ() - this.getZ());
            if (this.isBreathingFire()) {
                if (this.isActuallyBreathingFire()) {
                    setYRot(yBodyRot);
                    if (this.tickCount % 5 == 0) {
                        this.playSound(IafSoundRegistry.FIREDRAGON_BREATH, 4, 1);
                    }
                    stimulateFire(this.getX() + distX * this.fireTicks / 40, entity.getY(), this.getZ() + distZ * this.fireTicks / 40, 1);
                }
            } else {
                this.setBreathingFire(true);
            }
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity LivingEntityIn) {
        if (LivingEntityIn != null && this.riderCandidate != null) {
            // it goes for a new rider first (DragonRiders)
            return;
        }
        super.setTarget(LivingEntityIn);
        this.flightManager.onSetAttackTarget(LivingEntityIn);
    }

    @Override
    public boolean wantsToAttack(@NotNull LivingEntity target, @NotNull LivingEntity owner) {
        if (this.isTame() && target instanceof TamableAnimal tamableTarget) {
            UUID targetOwner = EntityUtil.getOwnerUUID(tamableTarget);
            if (targetOwner != null && targetOwner.equals(EntityUtil.getOwnerUUID(this))) {
                return false;
            }
        }
        return super.wantsToAttack(target, owner);
    }

    @Override
    public boolean canAttack(@NotNull LivingEntity target) {
        return super.canAttack(target) && DragonUtils.isAlive(target);
    }

    public boolean isPart(Entity entityHit) {
        return headPart != null && headPart.is(entityHit) || neckPart != null && neckPart.is(entityHit) ||
            leftWingLowerPart != null && leftWingLowerPart.is(entityHit) || rightWingLowerPart != null && rightWingLowerPart.is(entityHit) ||
            leftWingUpperPart != null && leftWingUpperPart.is(entityHit) || rightWingUpperPart != null && rightWingUpperPart.is(entityHit) ||
            tail1Part != null && tail1Part.is(entityHit) || tail2Part != null && tail2Part.is(entityHit) ||
            tail3Part != null && tail3Part.is(entityHit) || tail4Part != null && tail4Part.is(entityHit) ||
            rightWingTipPart != null && rightWingTipPart.is(entityHit) || leftWingTipPart != null && leftWingTipPart.is(entityHit) ||
            tailFinPart != null && tailFinPart.is(entityHit);
    }

    @Override
    public double getFlightSpeedModifier() {
        return IafConfig.dragonFlightSpeedMod;
    }

    public boolean isAllowedToTriggerFlight() {
        // under a roof (a cave, a hall) it stays on its feet: taking off there only bumps it into the rock
        return (this.hasFlightClearance() && this.onGround() && DragonSmarts.roomToFly(this) && PillagerDragons.mayTakeOff(this) || this.isInWater()) && !this.isOrderedToSit() && this.getPassengers().stream().allMatch(this::carriesRider) && !this.isBaby() && !this.isSleeping() && this.canMove();
    }

    public BlockPos getEscortPosition() {
        return this.getOwner() != null ? BlockPos.containing(this.getOwner().position()) : this.blockPosition();
    }

    public boolean shouldTPtoOwner() {
        return this.getOwner() != null && this.distanceTo(this.getOwner()) > 10;
    }

    public boolean isSkeletal() {
        return this.getDeathStage() >= (this.getAgeInDays() / 5) / 2;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean save(ValueOutput compound) {
        return this.saveAsPassenger(compound);
    }

    @Override
    public void playSound(@NotNull SoundEvent soundIn, float volume, float pitch) {
        if (soundIn == SoundEvents.GENERIC_EAT.value() || soundIn == this.getAmbientSound() || soundIn == this.getHurtSound(null) || soundIn == this.getDeathSound() || soundIn == this.getRoarSound()) {
            if (!this.isSilent() && this.headPart != null) {
                this.level().playSound(null, this.headPart.getX(), this.headPart.getY(), this.headPart.getZ(), soundIn, this.getSoundSource(), volume, pitch);
            }
        } else {
            super.playSound(soundIn, volume, pitch);
        }
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    /**
     * Room to take off: the whole body free of blocks (leaves too) for a few blocks up, not only the middle of its
     * back (a dragon under a branch or an overhang took off into it and got stuck).
     */
    public boolean hasFlightClearance() {
        BlockPos topOfBB = BlockPos.containing(this.getX(), this.getBoundingBox().maxY, this.getZ());
        for (int i = 1; i < 4; i++) {
            if (!level().isEmptyBlock(topOfBB.above(i))) {
                return false;
            }
        }
        net.minecraft.world.phys.AABB box = this.getBoundingBox().deflate(0.1);
        double up = Math.max(4, this.getBbHeight());
        return this.level().noCollision(this, box.move(0, up, 0)) && this.level().noCollision(this, box.expandTowards(0, up, 0))
            && !this.level().getBlockStates(box.move(0, 2, 0).expandTowards(0, up - 2, 0)).anyMatch(state -> state.is(net.minecraft.tags.BlockTags.LEAVES));
    }

    /**
     * Dragons of the wild that do not fight each other: wild dragons of the land, the dragons of a pillager kingdom,
     * the dragons of the dread (each among their own).
     */
    public boolean alliedWith(Entity entity) {
        if (!(entity instanceof EntityDragonBase other) || this.isTame() || other.isTame()) {
            return false;
        }
        if (this.pillagerMount || other.pillagerMount) {
            return this.pillagerMount && other.pillagerMount;
        }
        if (this.dreadMount || other.dreadMount) {
            return this.dreadMount && other.dreadMount;
        }
        return true;
    }

    @Override
    public ItemStack getItemBySlot(final EquipmentSlot slotIn) {
        switch (slotIn) {
            case OFFHAND:
                return dragonInventory.getItem(0);
            case HEAD:
                return dragonInventory.getItem(1);
            case CHEST:
                return dragonInventory.getItem(2);
            case LEGS:
                return dragonInventory.getItem(3);
            case FEET:
                return dragonInventory.getItem(4);
            default:
                return super.getItemBySlot(slotIn);
        }
    }

    @Override
    public void setItemSlot(final EquipmentSlot slotIn, final @NotNull ItemStack stack) {
        switch (slotIn) {
            case OFFHAND:
                dragonInventory.setItem(0, stack);
                break;
            case HEAD:
                dragonInventory.setItem(1, stack);
                break;
            case CHEST:
                dragonInventory.setItem(2, stack);
                break;
            case LEGS:
                dragonInventory.setItem(3, stack);
                break;
            case FEET:
                dragonInventory.setItem(4, stack);
                break;
            default:
                super.getItemBySlot(slotIn);
        }
    }

    public SoundEvent getBabyFireSound() {
        return SoundEvents.FIRE_EXTINGUISH;
    }

    protected boolean isPlayingAttackAnimation() {
        return this.getAnimation() == ANIMATION_BITE || this.getAnimation() == ANIMATION_SHAKEPREY || this.getAnimation() == ANIMATION_WINGBLAST ||
                this.getAnimation() == ANIMATION_TAILWHACK;
    }

    protected IafDragonLogic createDragonLogic() {
        return new IafDragonLogic(this);
    }

    protected int getFlightChancePerTick() {
        return FLIGHT_CHANCE_PER_TICK;
    }

    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        if (IafConfig.chunkLoadSummonCrystal) {
            if (this.isBoundToCrystal()) {
                DragonPosWorldData data = DragonPosWorldData.get(level());
                if (data != null) {
                    data.addDragon(this.getUUID(), this.blockPosition());
                }
            }
        }
        super.onRemoval(reason);
    }

    @Override
    public int maxSearchNodes() {
        return (int) this.getAttribute(Attributes.FOLLOW_RANGE).getValue();
    }

    @Override
    public boolean isSmallerThanBlock() {
        return false;
    }

    @Override
    public float getXZNavSize() {
        return Math.max(1.4F, this.getBbWidth() / 2.0F);
    }

    @Override
    public int getYNavSize() {
        return Mth.ceil(this.getBbHeight());
    }

    public void containerChanged(@NotNull Container invBasic) {
        if (!this.level().isClientSide()) {
            updateAttributes();
        }
    }


}
