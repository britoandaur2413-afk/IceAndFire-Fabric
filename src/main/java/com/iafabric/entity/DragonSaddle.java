package com.iafabric.entity;

import com.iafabric.entity.props.MiscProperties;
import com.iafabric.entity.util.IFlyingMount;
import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/**
 * The double saddle: on a grown fire, ice or lightning dragon (stage 4 on) a friend rides behind its owner. Its owner
 * puts it on (a right click with it, or its place in the dragon's menu); a friend climbs on behind with a right click
 * while the owner rides it. The friend steers nothing and gets off on the ground: when they will, or with the owner
 * (in the air the dragon first comes down, as it does at the flute). Its two seats are on its back as its model is
 * posed: the front one on BodyUpper (DragonSeatPose, a little higher on the saddle), the one behind on BodyLower; the
 * dragon draws its riders there (RenderDragonBase), and the server puts them there (here, from DragonBones).
 */
public final class DragonSaddle {
	/** Its place in the dragon's inventory (after the banner and the four armor pieces). */
	public static final int SLOT = 5;
	/** No one in the seat behind (the seat holds the rider's entity id). */
	public static final int NOBODY = -1;
	/** The tops of the bones under the seats (model units, y down: BodyUpper's and BodyLower's boxes). */
	public static final float UPPER_TOP = -1.8F, LOWER_TOP = -1.5F;
	/** How high the seats' cushions are over those tops: the riders sit on them. */
	public static final float CUSHION = 0.5F;
	/** Where BodyLower hangs from BodyUpper, along it (the same in the three kinds). */
	private static final float LOWER_AT = 6.7F;

	private DragonSaddle() {
	}

	public static void register() {
		// a rider who leaves the game with someone else on it is not saved with the dragon (the game keeps a vehicle
		// with its player only when it carries one): they come back where they sat, and do not die of the fall
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			if (handler.player.getVehicle() instanceof EntityDragonBase dragon && behind(dragon) != null) {
				MiscProperties.setDismountedDragon(handler.player, true);
			}
		});
	}

	/** A fire, ice or lightning dragon of the wild's own kind (not of the dread, not Black Frost). */
	public static boolean rightKind(EntityDragonBase dragon) {
		EntityType<?> type = dragon.getType();
		return (type == IafEntityRegistry.FIRE_DRAGON || type == IafEntityRegistry.ICE_DRAGON || type == IafEntityRegistry.LIGHTNING_DRAGON)
			&& dragon.getDreadLook() == 0;
	}

	/** A dragon that can wear it: of the right kind, and grown (stage 4 on). */
	public static boolean fits(EntityDragonBase dragon) {
		return rightKind(dragon) && dragon.getDragonStage() >= 4;
	}

	/** It wears one (known on both sides). */
	public static boolean worn(EntityDragonBase dragon) {
		return dragon.hasDoubleSaddle() && fits(dragon);
	}

	/** Who sits in the seat behind, or null. */
	public static @Nullable Entity behind(EntityDragonBase dragon) {
		int id = dragon.getBackRider();
		if (id == NOBODY) {
			return null;
		}
		for (Entity passenger : dragon.getPassengers()) {
			if (passenger.getId() == id) {
				return passenger;
			}
		}
		return null;
	}

	public static boolean inBackSeat(EntityDragonBase dragon, @Nullable Entity rider) {
		return rider != null && dragon.getBackRider() != NOBODY && rider.getId() == dragon.getBackRider() && rider.getVehicle() == dragon;
	}

	/**
	 * How far behind the front seat the one behind is, along the back (model units): a block at least (a rider's legs
	 * reach forward that far), and the length of a seat at least on the biggest dragons.
	 */
	public static float gap(EntityDragonBase dragon) {
		float size = dragon.getRenderSize() / 3F;
		return Math.max(3.0F, 16F / Math.max(size, 0.01F));
	}

	/** The seat behind along BodyLower, in its own frame (model units). */
	public static float backZ(EntityDragonBase dragon) {
		return DragonSeatPose.SEAT_Z + gap(dragon) - LOWER_AT;
	}

	/** The seat behind on the server, in the world: on BodyLower as its bones are posed now. */
	public static Vec3 backSeat(EntityDragonBase dragon) {
		Vector3f at = DragonBones.point(dragon, "BodyLower", 0F, LOWER_TOP - CUSHION, backZ(dragon));
		if (at != null) {
			return dragon.position().add(at.x(), at.y(), at.z());
		}
		// (no bones: along its back from the front seat)
		Vec3 front = DragonSeatPose.world(dragon);
		float yaw = dragon.yBodyRot * Mth.DEG_TO_RAD;
		double gap = gap(dragon) / 16.0 * dragon.getRenderSize() / 3.0;
		return front.add(Mth.sin(yaw) * gap, 0, -Mth.cos(yaw) * gap);
	}

	/** Its owner, with the saddle in hand: on it, if it is of the right kind and grown. */
	public static InteractionResult putOn(EntityDragonBase dragon, Player owner, ItemStack stack) {
		if (!dragon.level().isClientSide) {
			if (!rightKind(dragon)) {
				owner.displayClientMessage(Component.translatable("dragon.saddle.wrong_kind"), true);
			} else if (dragon.getDragonStage() < 4) {
				owner.displayClientMessage(Component.translatable("dragon.saddle.too_young"), true);
			} else {
				dragon.dragonInventory.setItem(SLOT, stack.copyWithCount(1));
				if (!owner.isCreative()) {
					stack.shrink(1);
				}
				dragon.playSound(SoundEvents.HORSE_SADDLE, 1F, 1F);
				owner.displayClientMessage(Component.translatable("dragon.saddle.on"), true);
			}
		}
		return InteractionResult.SUCCESS;
	}

	/** Someone else's right click: the saddle in hand (only its owner puts it on), or getting on behind. */
	public static InteractionResult byOther(EntityDragonBase dragon, Player player, ItemStack stack) {
		if (!dragon.isTame() || player.isPassenger() || dragon.isModelDead()) {
			return InteractionResult.PASS;
		}
		if (stack.is(IafItemRegistry.DOUBLE_DRAGON_SADDLE)) {
			tell(dragon, player, "dragon.saddle.owner_only");
			return InteractionResult.SUCCESS;
		}
		if (!fits(dragon)) {
			return InteractionResult.PASS;
		}
		boolean ownerOn = dragon.getControllingPassenger() != null;
		if (!worn(dragon)) {
			if (!ownerOn) {
				return InteractionResult.PASS;
			}
			tell(dragon, player, "dragon.saddle.needed");
		} else if (!ownerOn) {
			tell(dragon, player, "dragon.saddle.owner_first");
		} else if (behind(dragon) != null) {
			tell(dragon, player, "dragon.saddle.taken");
		} else if (!dragon.level().isClientSide) {
			dragon.setBackRider(player.getId());
			player.setShiftKeyDown(false);
			player.startRiding(dragon, true);
		}
		return InteractionResult.SUCCESS;
	}

	private static void tell(EntityDragonBase dragon, Player player, String key) {
		if (!dragon.level().isClientSide) {
			player.displayClientMessage(Component.translatable(key), true);
		}
	}

	/**
	 * Every tick on the server: with no one steering it (its owner got off, or left) or no saddle, the one behind gets
	 * off, on the ground; in the air it comes down first.
	 */
	public static void tick(EntityDragonBase dragon) {
		Entity friend = behind(dragon);
		if (friend == null) {
			if (dragon.getBackRider() != NOBODY) {
				dragon.setBackRider(NOBODY);
			}
			return;
		}
		if (dragon.getControllingPassenger() != null && worn(dragon)) {
			return;
		}
		if (IFlyingMount.canDismount(dragon)) {
			friend.stopRiding();
		} else {
			dragon.setFlying(false);
			dragon.setHovering(false);
		}
	}
}
