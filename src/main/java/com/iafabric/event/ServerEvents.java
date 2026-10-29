package com.iafabric.event;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityAmphithere;
import com.iafabric.entity.EntityCockatrice;
import com.iafabric.entity.EntityCyclops;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityGhost;
import com.iafabric.entity.EntityHydra;
import com.iafabric.entity.EntityHydraHead;
import com.iafabric.entity.EntityMutlipartPart;
import com.iafabric.entity.EntityStoneStatue;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.ai.EntitySheepAIFollowCyclops;
import com.iafabric.entity.ai.VillagerAIFearUntamed;
import com.iafabric.entity.props.ChainProperties;
import com.iafabric.entity.props.ChickenProperties;
import com.iafabric.entity.props.FrozenProperties;
import com.iafabric.entity.props.MiscProperties;
import com.iafabric.entity.props.SirenProperties;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.IAnimalFear;
import com.iafabric.entity.util.IHearsSiren;
import com.iafabric.entity.util.IVillagerFear;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemChain;
import com.iafabric.item.ItemDragonsteelArmor;
import com.iafabric.item.ItemGhostSword;
import com.iafabric.item.ItemScaleArmor;
import com.iafabric.item.ItemTrollArmor;
import com.iafabric.message.MessagePlayerHitMultipart;
import com.iafabric.message.MessageSyncPath;
import com.iafabric.misc.IafDamageRegistry;
import com.iafabric.misc.IafTagRegistry;
import com.iafabric.network.IafNetwork;
import com.iafabric.pathfinding.raycoms.Pathfinding;
import com.iafabric.pathfinding.raycoms.pathjobs.AbstractPathJob;
import com.iafabric.util.ItemNbt;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Gameplay hooks on vanilla things (the old Forge event handlers): chained mobs, frozen mobs,
 * charmed players, dragons guarding gold, armor resistances and more. Fired by Fabric events and by
 * the mixins in com.iafabric.mixin.
 */
public final class ServerEvents {

    public static final UUID ALEX_UUID = UUID.fromString("71363abe-fd03-49c9-940d-aae8b8209b7c");
    public static final String BOLT_DONT_DESTROY_LOOT = "iceandfire_fabric.bolt_skip_loot";
    private static final Predicate<LivingEntity> VILLAGER_FEAR = entity -> entity instanceof IVillagerFear;
    private static final RandomSource RAND = RandomSource.create();

    private ServerEvents() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register(ServerEvents::onPlayerAttack);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(ServerEvents::allowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(ServerEvents::onEntityDie);
        UseItemCallback.EVENT.register(ServerEvents::onEntityUseItem);
        UseEntityCallback.EVENT.register(ServerEvents::onEntityInteract);
        UseBlockCallback.EVENT.register(ServerEvents::onPlayerRightClick);
        PlayerBlockBreakEvents.BEFORE.register(ServerEvents::onBreakBlock);
        // on the server thread, before the player is saved (a connection closes on the network thread)
        ServerPlayerEvents.LEAVE.register(player -> {
            for (Entity entity : player.getPassengers()) {
                entity.stopRiding();
            }
        });
        EntityTrackingEvents.START_TRACKING.register(ServerEvents::onPlayerStartTracking);
        ServerEntityEvents.ENTITY_LOAD.register(ServerEvents::onEntityJoinWorld);
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (key.identifier().equals(Identifier.withDefaultNamespace("entities/wither_skeleton"))) {
                // a wither skeleton drops a witherbone half of the time
                builder.withPool(LootPool.lootPool().add(LootItem.lootTableItem(IafItemRegistry.WITHERBONE))
                    .when(net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition.randomChance(0.5F)));
            }
        });
    }

    private static void signalChickenAlarm(LivingEntity chicken, LivingEntity attacker) {
        final float d0 = IafConfig.cockatriceChickenSearchLength;
        final List<EntityCockatrice> list = chicken.level().getEntitiesOfClass(EntityCockatrice.class, (new AABB(chicken.getX(), chicken.getY(), chicken.getZ(), chicken.getX() + 1.0D, chicken.getY() + 1.0D, chicken.getZ() + 1.0D)).inflate(d0, 10.0D, d0));
        for (final EntityCockatrice cockatrice : list) {
            if (!(attacker instanceof EntityCockatrice) && !DragonUtils.hasSameOwner(cockatrice, attacker)) {
                if (attacker instanceof Player player) {
                    if (!player.isCreative() && !cockatrice.isOwnedBy(player)) {
                        cockatrice.setTarget(player);
                    }
                } else {
                    cockatrice.setTarget(attacker);
                }
            }
        }
    }

    private static void signalAmphithereAlarm(LivingEntity villager, LivingEntity attacker) {
        final float d0 = IafConfig.amphithereVillagerSearchLength;
        final List<EntityAmphithere> list = villager.level().getEntitiesOfClass(EntityAmphithere.class, (new AABB(villager.getX() - 1.0D, villager.getY() - 1.0D, villager.getZ() - 1.0D, villager.getX() + 1.0D, villager.getY() + 1.0D, villager.getZ() + 1.0D)).inflate(d0, d0, d0));
        for (final EntityAmphithere amphithere : list) {
            if (!(attacker instanceof EntityAmphithere) && !DragonUtils.hasSameOwner(amphithere, attacker)) {
                if (attacker instanceof Player player) {
                    if (!player.isCreative() && !amphithere.isOwnedBy(player)) {
                        amphithere.setTarget(player);
                    }
                } else {
                    amphithere.setTarget(attacker);
                }
            }
        }
    }

    private static boolean isInEntityTag(Identifier loc, EntityType<?> type) {
        return type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, loc));
    }

    public static boolean isLivestock(Entity entity) {
        return entity != null && isInEntityTag(IafTagRegistry.FEAR_DRAGONS, entity.getType());
    }

    public static boolean isVillager(Entity entity) {
        return entity != null && isInEntityTag(IafTagRegistry.VILLAGERS, entity.getType());
    }

    public static boolean isSheep(Entity entity) {
        return entity != null && isInEntityTag(IafTagRegistry.SHEEP, entity.getType());
    }

    public static boolean isChicken(Entity entity) {
        return entity != null && isInEntityTag(IafTagRegistry.CHICKENS, entity.getType());
    }

    public static boolean isCockatriceTarget(Entity entity) {
        return entity != null && isInEntityTag(IafTagRegistry.COCKATRICE_TARGETS, entity.getType());
    }

    public static boolean doesScareCockatrice(Entity entity) {
        return entity != null && isInEntityTag(IafTagRegistry.SCARES_COCKATRICES, entity.getType());
    }

    public static boolean isBlindMob(Entity entity) {
        return entity != null && isInEntityTag(IafTagRegistry.BLINDED, entity.getType());
    }

    public static boolean isRidingOrBeingRiddenBy(final Entity first, final Entity entityIn) {
        for (final Entity entity : first.getPassengers()) {
            if (entity.equals(entityIn) || isRidingOrBeingRiddenBy(entity, entityIn)) {
                return true;
            }
        }
        return false;
    }

    /** From the projectile mixin: true stops the hit (ghosts, and the rider's own tamed mount). */
    public static boolean cancelProjectileHit(Projectile projectile, HitResult result) {
        if (!(result instanceof EntityHitResult entityResult)) {
            return false;
        }
        Entity shotEntity = entityResult.getEntity();
        if (shotEntity instanceof EntityGhost) {
            return true;
        }
        // a dragon's riders (its owner, a friend behind in a double saddle) shoot past it: through its body and its parts
        if (projectile.getOwner() instanceof Player shooter && shooter.getVehicle() instanceof EntityDragonBase ridden
            && (shotEntity == ridden || shotEntity instanceof EntityMutlipartPart part && part.getParent() == ridden)) {
            return true;
        }
        if (projectile instanceof AbstractArrow && projectile.getOwner() instanceof LivingEntity shootingEntity) {
            if (isRidingOrBeingRiddenBy(shootingEntity, shotEntity) && shotEntity instanceof TamableAnimal tamable && tamable.isTame()
                && shotEntity.isAlliedTo(shootingEntity)) {
                return true;
            }
        }
        return false;
    }

    /** From the fall mixin: no fall damage right after jumping off a dragon. */
    public static boolean cancelFallDamage(LivingEntity entity) {
        if (entity instanceof Player && MiscProperties.hasDismounted(entity)) {
            MiscProperties.setDismountedDragon(entity, false);
            return true;
        }
        return false;
    }

    /** From the hurt mixin: troll leather softens arrows, dragon scales soften dragon breath. */
    public static float modifyDamage(LivingEntity entity, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            float multi = 1;
            if (entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ItemTrollArmor) multi -= 0.1F;
            if (entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ItemTrollArmor) multi -= 0.3F;
            if (entity.getItemBySlot(EquipmentSlot.LEGS).getItem() instanceof ItemTrollArmor) multi -= 0.2F;
            if (entity.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof ItemTrollArmor) multi -= 0.1F;
            amount *= multi;
        }
        if (source.is(IafDamageRegistry.DRAGON_FIRE) || source.is(IafDamageRegistry.DRAGON_ICE) || source.is(IafDamageRegistry.DRAGON_LIGHTNING)) {
            float multi = 1;
            if (isDragonArmor(entity.getItemBySlot(EquipmentSlot.HEAD))) multi -= 0.1F;
            if (isDragonArmor(entity.getItemBySlot(EquipmentSlot.CHEST))) multi -= 0.3F;
            if (isDragonArmor(entity.getItemBySlot(EquipmentSlot.LEGS))) multi -= 0.2F;
            if (isDragonArmor(entity.getItemBySlot(EquipmentSlot.FEET))) multi -= 0.1F;
            amount *= multi;
        }
        return amount;
    }

    private static boolean isDragonArmor(ItemStack stack) {
        return stack.getItem() instanceof ItemScaleArmor || stack.getItem() instanceof ItemDragonsteelArmor;
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        if (!com.iafabric.entity.PillagerDragons.allowDamage(entity, source)) {
            return false;
        }
        if (source.getEntity() instanceof LivingEntity attacker) {
            if (MiscProperties.getLoveTicks(attacker) > 0) {
                return false;
            }
            if (isChicken(entity)) {
                signalChickenAlarm(entity, attacker);
            } else if (DragonUtils.isVillager(entity)) {
                signalAmphithereAlarm(entity, attacker);
            }
        }
        return true;
    }

    /** From the mob mixin when a mob picks a target. */
    public static void onLivingSetTarget(LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            if (isChicken(target)) {
                signalChickenAlarm(target, attacker);
            } else if (DragonUtils.isVillager(target)) {
                signalAmphithereAlarm(target, attacker);
            }
        }
    }

    private static InteractionResult onPlayerAttack(Player player, Level level, InteractionHand hand, Entity target, EntityHitResult hitResult) {
        if (target instanceof EntityMutlipartPart part) {
            Entity parent = part.getParent();
            if (parent != null && parent.hasIndirectPassenger(player)) {
                // (its rider swinging past its head does not strike their own mount)
                return InteractionResult.FAIL;
            }
            if (parent != null) {
                player.attack(parent);
            }
            int extraData = 0;
            if (target instanceof EntityHydraHead head && parent instanceof EntityHydra hydra) {
                extraData = head.headIndex;
                hydra.triggerHeadFlags(extraData);
            }
            if (level.isClientSide() && parent != null) {
                IafNetwork.sendToServer(new MessagePlayerHitMultipart(parent.getId(), extraData));
            }
            return InteractionResult.FAIL;
        }
        if (isSheep(target)) {
            float dist = IafConfig.cyclopesSheepSearchLength;
            for (Entity entity : level.getEntities(player, player.getBoundingBox().expandTowards(dist, dist, dist))) {
                if (entity instanceof EntityCyclops cyclops && !cyclops.isBlinded() && !player.isCreative()) {
                    cyclops.setTarget(player);
                }
            }
        }
        if (target instanceof EntityStoneStatue statue) {
            statue.setHealth(statue.getMaxHealth());
            ItemStack stack = player.getMainHandItem();
            statue.playSound(SoundEvents.STONE_BREAK, 2, 0.5F + (RAND.nextFloat() - RAND.nextFloat()) * 0.2F + 0.5F);
            if (stack.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()) || stack.getItem().getDescriptionId().contains("pickaxe")) {
                statue.setCrackAmount(statue.getCrackAmount() + 1);
                if (statue.getCrackAmount() > 9 && level instanceof ServerLevel serverLevel) {
                    statue.playSound(SoundEvents.STONE_BREAK, 2, (RAND.nextFloat() - RAND.nextFloat()) * 0.2F + 0.5F);
                    boolean silkTouch = EnchantmentHelper.getItemEnchantmentLevel(
                        serverLevel.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), stack) > 0;
                    if (silkTouch) {
                        ItemStack statuette = new ItemStack(IafItemRegistry.STONE_STATUE);
                        CompoundTag tag = new CompoundTag();
                        tag.putBoolean("IAFStoneStatuePlayerEntity", statue.getTrappedEntityTypeString().equalsIgnoreCase("minecraft:player"));
                        tag.putString("IAFStoneStatueEntityID", statue.getTrappedEntityTypeString());
                        tag.put("IAFStoneStatueNBT", statue.saveTrappedData());
                        ItemNbt.setTag(statuette, tag);
                        statue.spawnAtLocation(serverLevel, statuette, 1);
                    } else {
                        statue.spawnAtLocation(serverLevel, new ItemStack(Blocks.COBBLESTONE.asItem(), 2 + statue.getRandom().nextInt(4)), 1);
                    }
                    statue.remove(Entity.RemovalReason.KILLED);
                }
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    private static void onEntityDie(LivingEntity entity, DamageSource source) {
        Level world = entity.level();
        if (world.isClientSide() || !(world instanceof ServerLevel serverLevel)) {
            return;
        }
        if (ChainProperties.hasChainData(entity)) {
            ItemEntity entityitem = new ItemEntity(world, entity.getX(), entity.getY() + 1, entity.getZ(),
                new ItemStack(IafItemRegistry.CHAIN, ChainProperties.getChainedTo(entity).size()));
            entityitem.setDefaultPickUpDelay();
            world.addFreshEntity(entityitem);
            ChainProperties.clearChainData(entity);
        }
        if (entity.getUUID().equals(ALEX_UUID)) {
            entity.spawnAtLocation(serverLevel, new ItemStack(IafItemRegistry.WEEZER_BLUE_ALBUM), 1);
        }
        if (entity instanceof Player && IafConfig.ghostsFromPlayerDeaths) {
            Entity attacker = entity.getLastHurtByMob();
            if (attacker instanceof Player && entity.getRandom().nextInt(3) == 0) {
                CombatEntry entry = entity.getCombatTracker().getMostSignificantFall();
                boolean flag = entry != null && (entry.source().is(DamageTypes.FALL) || entry.source().is(DamageTypes.DROWN) || entry.source().is(DamageTypes.LAVA));
                if (entity.hasEffect(MobEffects.POISON)) {
                    flag = true;
                }
                if (flag) {
                    EntityGhost ghost = IafEntityRegistry.GHOST.create(world, EntitySpawnReason.TRIGGERED);
                    if (ghost != null) {
                        ghost.copyPosition(entity);
                        ghost.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(entity.blockPosition()), EntitySpawnReason.SPAWNER, null);
                        world.addFreshEntity(ghost);
                        ghost.setDaytimeMode(true);
                    }
                }
            }
        }
    }

    private static InteractionResult onEntityUseItem(Player player, Level level, InteractionHand hand) {
        if (player.getXRot() > 87 && player.getVehicle() instanceof EntityDragonBase dragon) {
            dragon.mobInteract(player, hand);
        }
        return InteractionResult.PASS;
    }

    /** From the living tick mixin, on both sides. */
    public static void onLivingTick(LivingEntity entity) {
        if (ChainProperties.hasChainData(entity)) {
            ChainProperties.tickChain(entity);
        }
        if (IafConfig.chickensLayRottenEggs && !entity.level().isClientSide() && isChicken(entity) && !entity.isBaby() && entity instanceof Animal) {
            ChickenProperties.tickChicken(entity);
        }
        if (FrozenProperties.isFrozen(entity)) {
            FrozenProperties.tickFrozenEntity(entity);
            if (!(entity instanceof Player player && player.isCreative())) {
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.25F, 1, 0.25F));
                if (!(entity instanceof EnderDragon) && !entity.onGround()) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(0, -0.2, 0));
                }
            }
        }
        if (entity instanceof Player || entity instanceof AbstractVillager || entity instanceof IHearsSiren) {
            SirenProperties.tickCharmedEntity(entity);
        }
        if (MiscProperties.getLoveTicks(entity) > 0) {
            MiscProperties.tickLove(entity);
        }
    }

    private static InteractionResult onEntityInteract(Player player, Level level, InteractionHand hand, Entity target, EntityHitResult hitResult) {
        if (target instanceof LivingEntity living && ChainProperties.isChainedTo(living, player)) {
            ChainProperties.removeChain(living, player);
            if (level instanceof ServerLevel serverLevel) {
                target.spawnAtLocation(serverLevel, new ItemStack(IafItemRegistry.CHAIN), 1);
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && target instanceof Mob && player.getItemInHand(hand).getItem() == Items.STICK && Pathfinding.isDebug()) {
            if (AbstractPathJob.trackingMap.getOrDefault(player, UUID.randomUUID()).equals(target.getUUID())) {
                AbstractPathJob.trackingMap.remove(player);
                IafNetwork.sendToPlayer(new MessageSyncPath(new HashSet<>(), new HashSet<>(), new HashSet<>()), (ServerPlayer) player);
            } else {
                AbstractPathJob.trackingMap.put(player, target.getUUID());
            }
        }
        return InteractionResult.PASS;
    }

    /** Swinging in the air (sent by the client): the ghost sword throws its blade. */
    public static void onLeftClick(final Player playerEntity, final ItemStack stack) {
        if (stack.getItem() == IafItemRegistry.GHOST_SWORD) {
            ItemGhostSword.spawnGhostSwordEntity(stack, playerEntity);
        }
    }

    private static InteractionResult onPlayerRightClick(Player player, Level level, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractChestBlock && !player.isCreative()) {
            angerDragonsNear(player, level);
        }
        // (on the server only: a knot made on the client has no id, and the game crashed reading it)
        if (state.getBlock() instanceof WallBlock && !level.isClientSide()) {
            ItemChain.attachToFence(player, level, pos);
        }
        return InteractionResult.PASS;
    }

    private static boolean onBreakBlock(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (state.getBlock() instanceof AbstractChestBlock || state.getBlock() == IafBlockRegistry.GOLD_PILE
            || state.getBlock() == IafBlockRegistry.SILVER_PILE || state.getBlock() == IafBlockRegistry.COPPER_PILE) {
            if (!player.isCreative()) {
                angerDragonsNear(player, level);
            }
        }
        return true;
    }

    private static void angerDragonsNear(Player player, Level level) {
        final float dist = IafConfig.dragonGoldSearchLength;
        for (Entity entity : level.getEntities(player, player.getBoundingBox().inflate(dist, dist, dist))) {
            if (entity instanceof EntityDragonBase dragon && !dragon.isTame() && !dragon.isModelDead() && !dragon.isOwnedBy(player)) {
                dragon.setInSittingPose(false);
                dragon.setOrderedToSit(false);
                dragon.setTarget(player);
            }
        }
    }

    private static void onPlayerStartTracking(Entity target, ServerPlayer player) {
        if (target instanceof LivingEntity living) {
            if (ChainProperties.hasChainData(living))
                ChainProperties.updateData(living);
            if (FrozenProperties.isFrozen(living))
                FrozenProperties.updateData(living);
            if (MiscProperties.getLoveTicks(living) > 0)
                MiscProperties.updateData(living);
            if (SirenProperties.isCharmed(living))
                SirenProperties.updateData(living);
        }
    }

    private static void onEntityJoinWorld(Entity entity, ServerLevel level) {
        try {
            if (isSheep(entity) && entity instanceof Animal animal) {
                animal.goalSelector.addGoal(8, new EntitySheepAIFollowCyclops(animal, 1.2D));
            }
            if (isVillager(entity) && entity instanceof PathfinderMob villager && IafConfig.villagersFearDragons) {
                villager.goalSelector.addGoal(1, new VillagerAIFearUntamed(villager, LivingEntity.class, 8.0F, 0.8D, 0.8D, VILLAGER_FEAR));
            }
            if (isLivestock(entity) && entity instanceof PathfinderMob animal && IafConfig.animalsFearDragons) {
                animal.goalSelector.addGoal(1, new VillagerAIFearUntamed(animal, LivingEntity.class, 30, 1.0D, 0.5D,
                    other -> other instanceof IAnimalFear fear && fear.shouldAnimalsFear(animal)));
            }
        } catch (Exception e) {
            IafMod.LOGGER.warn("Tried to add unique behaviors to vanilla mobs and encountered an error", e);
        }
    }

    /** From the thunder mixin: bolts of dragonsteel weapons leave the loot and the wielder alone. */
    public static boolean cancelLightningHit(Entity entity, LightningBolt lightning) {
        if ((entity instanceof ItemEntity || entity instanceof ExperienceOrb) && lightning.entityTags().contains(BOLT_DONT_DESTROY_LOOT)) {
            return true;
        }
        return lightning.entityTags().contains(entity.getStringUUID());
    }
}
