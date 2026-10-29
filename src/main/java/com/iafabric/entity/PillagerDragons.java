package com.iafabric.entity;

import com.iafabric.IafMod;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The dragons of the pillager kingdoms: young (the age a dragon is first ridden, kept there), in iron armor, each
 * with a pillager on its back (DragonRiders: its own hitbox and health; killed, the dragon goes to another pillager
 * to ride it). They rest sitting on their platforms and stand up when an enemy comes near the kingdom, fight, and sit
 * again when it is over; they take off only to fight, break no blocks, never fight illagers nor each other, and their
 * fire does not hurt illagers. Now and then a kingdom has an elite: an older ice dragon in netherite, ridden by the
 * warlord, who uses its frost (PillagerFrost). They cannot be tamed: killed, they leave their armor and, from the body, only
 * scales. Also the rider's seal: whoever carries it is not hurt by their own dragons.
 */
public final class PillagerDragons {
	/** How far from the kingdom's middle a pillager dragon goes without a fight. */
	private static final int HOME_RANGE = 24;
	/** How near an enemy has to come for the dragons to stand up. */
	private static final int ALERT = 22;
	/** How long a dragon waits, nothing near, before it sits again (ticks). */
	private static final int CALM = 100;
	public static final String WARLORD = "iceandfire_fabric_warlord";
	private static final ResourceLocation AGILITY = IafMod.id("pillager_dragon_agility");

	private PillagerDragons() {
	}

	/** Every tick on the server for a pillager dragon (from DragonSmarts). */
	public static void tick(EntityDragonBase dragon, ServerLevel level) {
		if (!dragon.pillagerMount) {
			return;
		}
		LivingEntity target = dragon.getTarget();
		// a kingdom dragon does not hunt: it fights who threatens the kingdom or who hurt it
		if (target != null && !(target instanceof Player) && !(target instanceof IronGolem) && target != dragon.getLastHurtByMob()) {
			dragon.setTarget(null);
			target = null;
		}
		if (target instanceof Raider || target != null && dragon.alliedWith(target)) {
			dragon.setTarget(null);
			target = null;
		}
		// a far enemy is not its business (unless it hurt it): it keeps to its kingdom
		if (target != null && dragon.distanceToSqr(target) > ALERT * ALERT * 4 && dragon.getLastHurtByMob() != target) {
			dragon.setTarget(null);
			target = null;
		}
		if (dragon.riderCandidate != null) {
			// it goes for a new rider (DragonRiders): no sitting down on the way
			dragon.calmTicks = 0;
			agile(dragon);
			return;
		}
		if (target == null && dragon.tickCount % 10 == 0) {
			LivingEntity threat = threat(dragon, level);
			if (threat != null) {
				dragon.setTarget(threat);
				target = threat;
			}
		}
		if (target == null) {
			// it rests sitting on its platform while all is quiet
			if (dragon.isFlying() || dragon.isHovering()) {
				dragon.setFlying(false);
				dragon.setHovering(false);
			}
			if (++dragon.calmTicks >= CALM && dragon.onGround() && !dragon.isOrderedToSit()) {
				dragon.setCommand(1);
			}
		} else {
			dragon.calmTicks = 0;
			if (dragon.isOrderedToSit()) {
				// up at once, with a roar
				dragon.setCommand(0);
				dragon.roar();
			}
			if (dragon.isSleeping()) {
				dragon.setInSittingPose(false);
			}
			if (DragonRiders.warlordRides(dragon)) {
				PillagerFrost.tick(dragon, target, level);
			}
		}
		agile(dragon);
		// back to the kingdom (only a real home, and a near one: a path to a far place is huge to work out)
		if (target == null && dragon.hasHomePosition && dragon.homePos != null && dragon.tickCount % 20 == 0) {
			BlockPos home = dragon.getRestrictCenter();
			double d2 = dragon.distanceToSqr(Vec3.atCenterOf(home));
			if (d2 > HOME_RANGE * HOME_RANGE && d2 < 96 * 96) {
				dragon.setCommand(0);
				dragon.calmTicks = 0;
				dragon.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.0);
			}
		}
	}

	/** An enemy of the kingdom near the dragon: a player (not in creative), an iron golem. */
	private static LivingEntity threat(EntityDragonBase dragon, ServerLevel level) {
		LivingEntity nearest = null;
		double best = ALERT * ALERT;
		List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, dragon.getBoundingBox().inflate(ALERT),
			e -> e.isAlive() && (e instanceof Player player && !player.isCreative() && !player.isSpectator() || e instanceof IronGolem));
		for (LivingEntity entity : near) {
			double d = entity.distanceToSqr(dragon);
			// seen, or so near it is heard and smelt
			if (d < best && (d < 64 || dragon.hasLineOfSight(entity))) {
				best = d;
				nearest = entity;
			}
		}
		return nearest;
	}

	/** A quarter faster, on the ground and in the air. */
	private static void agile(EntityDragonBase dragon) {
		for (var attribute : List.of(Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED)) {
			var instance = dragon.getAttribute(attribute);
			if (instance != null && instance.getModifier(com.iafabric.compat.Modifiers.uuid(AGILITY)) == null) {
				instance.addTransientModifier(com.iafabric.compat.Modifiers.of(AGILITY, 0.25, AttributeModifier.Operation.MULTIPLY_BASE));
			}
		}
	}

	/** Only when fighting does a pillager dragon take off. */
	public static boolean mayTakeOff(EntityDragonBase dragon) {
		return !dragon.pillagerMount || dragon.getTarget() != null;
	}

	/**
	 * False for damage that must not happen: a pillager dragon's to an illager, a wild dragon's to an allied dragon
	 * (no fights by mistake between the dragons of a cave, of a kingdom or of the dread), a dread dragon's to the
	 * dread, and a dragon's to its owner when the owner carries a rider's seal.
	 */
	public static boolean allowDamage(LivingEntity victim, DamageSource source) {
		EntityDragonBase dragon = dragonBehind(source);
		if (dragon == null) {
			return true;
		}
		if (dragon.pillagerMount && victim instanceof Raider) {
			return false;
		}
		if (victim instanceof EntityDragonBase other && other != dragon && dragon.alliedWith(other)) {
			return false;
		}
		if (dragon.dreadMount && victim instanceof EntityDreadMob) {
			return false;
		}
		if (victim instanceof Player player && dragon.isTame() && dragon.isOwnedBy(player) && hasSeal(player)) {
			return false;
		}
		return true;
	}

	private static EntityDragonBase dragonBehind(DamageSource source) {
		if (source.getEntity() instanceof EntityDragonBase dragon) {
			return dragon;
		}
		if (source.getDirectEntity() instanceof EntityDragonBase dragon) {
			return dragon;
		}
		if (source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() instanceof EntityDragonBase dragon) {
			return dragon;
		}
		return null;
	}

	public static boolean hasSeal(Player player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).is(IafItemRegistry.RIDER_SEAL)) {
				return true;
			}
		}
		return false;
	}

	/** A pillager dragon killed leaves its armor. */
	public static void dropArmor(EntityDragonBase dragon, ServerLevel level) {
		for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack armor = dragon.getItemBySlot(slot);
			if (!armor.isEmpty()) {
				dragon.spawnAtLocation(armor.copy(), 1);
				dragon.setItemSlot(slot, ItemStack.EMPTY);
			}
		}
	}

	/** Whether this is a kingdom's warlord (he rides the elite and stays on it until he dies). */
	public static boolean isWarlord(Entity entity) {
		return entity instanceof AbstractIllager && entity.getTags().contains(WARLORD);
	}
}
