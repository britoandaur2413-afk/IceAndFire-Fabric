package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.DragonBones;
import com.iafabric.entity.DragonSaddle;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.props.MiscProperties;
import com.iafabric.event.ServerEvents;
import com.iafabric.inventory.ContainerDragon;
import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Locale;

/**
 * The double saddle (DragonSaddle): only on a grown fire, ice or lightning dragon and only by its owner; a friend gets
 * on behind while the owner rides it, sits behind the owner on its back (not in its jaws as prey), steers nothing,
 * gets off with the owner; the saddle stays on while someone sits behind, is kept in its save and falls off when it
 * dies; its riders neither hurt it nor shoot into it.
 */
public class IafSaddleTests implements FabricGameTest {
	private static void log(String message) {
		IafMod.LOGGER.info("[IAFTEST] {}", message);
	}

	/** A test player in survival, joined as a player joins (the test's own mock player is always in creative). */
	private static ServerPlayer player(GameTestHelper helper, String name, double x, double z) {
		var server = helper.getLevel().getServer();
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name), false);
		var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, 2, z));
		player.moveTo(at.x, at.y, at.z, 30F, 0F);
		return player;
	}

	private static void leave(GameTestHelper helper, ServerPlayer... players) {
		for (ServerPlayer player : players) {
			player.stopRiding();
			helper.getLevel().getServer().getPlayerList().remove(player);
		}
	}

	private static EntityDragonBase dragon(GameTestHelper helper, EntityType<? extends EntityDragonBase> type, int days, ServerPlayer owner, double x, double z) {
		var level = helper.getLevel();
		EntityDragonBase dragon = type.create(level);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(x, 2, z));
		dragon.moveTo(at.x, at.y, at.z, 30F, 0F);
		dragon.yBodyRot = 30F;
		dragon.setAgeInDays(0);
		dragon.growDragon(days);
		dragon.setHealth(dragon.getMaxHealth());
		dragon.tame(owner);
		dragon.setCommand(0);
		level.addFreshEntity(dragon);
		return dragon;
	}

	private static void saddle(EntityDragonBase dragon) {
		dragon.dragonInventory.setItem(DragonSaddle.SLOT, new ItemStack(IafItemRegistry.DOUBLE_DRAGON_SADDLE));
	}

	private static void click(EntityDragonBase dragon, ServerPlayer player, ItemStack held) {
		player.setItemInHand(InteractionHand.MAIN_HAND, held);
		dragon.mobInteract(player, InteractionHand.MAIN_HAND);
	}

	/** Only its owner saddles it, only a grown fire, ice or lightning dragon (stage 4 on), not one of the dread. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void doubleSaddleOnlyFits(GameTestHelper helper) {
		var owner = player(helper, "saddle-owner", 2, 2);
		var stranger = player(helper, "saddle-stranger", 3, 2);
		var grown = dragon(helper, IafEntityRegistry.FIRE_DRAGON, 80, owner, 6, 6);
		var young = dragon(helper, IafEntityRegistry.ICE_DRAGON, 60, owner, 14, 6);
		var dread = dragon(helper, IafEntityRegistry.LIGHTNING_DRAGON, 90, owner, 6, 14);
		dread.setDreadLook(1);
		var other = dragon(helper, IafEntityRegistry.LIGHTNING_DRAGON, 110, owner, 14, 14);
		ItemStack held = new ItemStack(IafItemRegistry.DOUBLE_DRAGON_SADDLE, 1);
		click(grown, owner, held);
		boolean grownOn = grown.hasDoubleSaddle() && DragonSaddle.worn(grown) && held.isEmpty();
		ItemStack second = new ItemStack(IafItemRegistry.DOUBLE_DRAGON_SADDLE, 1);
		click(young, owner, second);
		click(dread, owner, second);
		ItemStack hers = new ItemStack(IafItemRegistry.DOUBLE_DRAGON_SADDLE, 1);
		click(other, stranger, hers);
		log(String.format(Locale.ROOT, "double saddle: on the grown fire dragon %s; on the young ice dragon %s, the dread one %s (saddle still in hand: %d); put by a stranger %s",
			grownOn, young.hasDoubleSaddle(), dread.hasDoubleSaddle(), second.getCount(), other.hasDoubleSaddle()));
		boolean youngOn = young.hasDoubleSaddle(), dreadOn = dread.hasDoubleSaddle(), otherOn = other.hasDoubleSaddle();
		int left = second.getCount(), hersLeft = hers.getCount();
		// its place in its menu: there on a grown dragon, not on a young one
		boolean slotGrown = new ContainerDragon(0, grown.dragonInventory, owner.getInventory(), grown).getSlot(DragonSaddle.SLOT).isActive();
		boolean slotYoung = new ContainerDragon(0, young.dragonInventory, owner.getInventory(), young).getSlot(DragonSaddle.SLOT).isActive();
		for (EntityDragonBase d : new EntityDragonBase[]{grown, young, dread, other}) {
			d.discard();
		}
		leave(helper, owner, stranger);
		helper.assertTrue(grownOn, "its owner did not put the double saddle on a grown fire dragon");
		helper.assertTrue(!youngOn && !dreadOn && left == 1, "the double saddle went on a young (" + youngOn + ") or dread (" + dreadOn + ") dragon, " + left + " left");
		helper.assertTrue(!otherOn && hersLeft == 1, "someone else put the double saddle on another's dragon");
		helper.assertTrue(slotGrown && !slotYoung, "its place in the menu: on a grown dragon " + slotGrown + ", on a young one " + slotYoung);
		helper.succeed();
	}

	/**
	 * A friend gets on behind only in a double saddle and only while its owner rides it; sits behind the owner along its
	 * back (not in its jaws: no prey shake, no harm), steers nothing; a third has no seat; and gets off with the owner,
	 * without a fall's harm.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void doubleSaddleFriendRides(GameTestHelper helper) {
		var owner = player(helper, "saddle-owner", 2, 2);
		var friend = player(helper, "saddle-friend", 3, 2);
		var third = player(helper, "saddle-third", 4, 2);
		var bare = dragon(helper, IafEntityRegistry.ICE_DRAGON, 90, owner, 14, 14);
		var dragon = dragon(helper, IafEntityRegistry.FIRE_DRAGON, 90, owner, 6, 6);
		saddle(dragon);
		// before its owner is on it: no
		click(dragon, friend, ItemStack.EMPTY);
		boolean beforeOwner = friend.getVehicle() == dragon;
		// without the saddle: no
		click(bare, owner, ItemStack.EMPTY);
		click(bare, friend, ItemStack.EMPTY);
		boolean onBare = friend.getVehicle() == bare;
		owner.stopRiding();
		click(dragon, owner, ItemStack.EMPTY);
		click(dragon, friend, ItemStack.EMPTY);
		click(dragon, third, ItemStack.EMPTY);
		helper.assertTrue(!beforeOwner, "a friend got on before its owner");
		helper.assertTrue(!onBare, "a friend got on a dragon with no double saddle");
		helper.assertTrue(owner.getVehicle() == dragon && friend.getVehicle() == dragon, "owner on " + owner.getVehicle() + ", friend on " + friend.getVehicle());
		helper.assertTrue(third.getVehicle() == null, "a third got on");
		helper.assertTrue(dragon.getControllingPassenger() == owner, "the one steering it is " + dragon.getControllingPassenger());
		helper.assertTrue(DragonSaddle.inBackSeat(dragon, friend) && dragon.getBackRider() == friend.getId(), "the friend is not in the seat behind");
		float health = friend.getHealth();
		int[] ticks = {0};
		helper.onEachTick(() -> {
			if (ticks[0]++ > 80 || friend.getVehicle() != dragon) {
				return;
			}
			// (these players do not tick: their seats are given them here, as their own ticks would)
			dragon.positionRider(owner);
			dragon.positionRider(friend);
		});
		helper.runAfterDelay(60, () -> {
			// along its back: the friend behind the owner, about a block, at about the same height, not to a side
			float yaw = dragon.yBodyRot * Mth.DEG_TO_RAD;
			double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
			double dx = owner.getX() - friend.getX(), dz = owner.getZ() - friend.getZ();
			double ahead = dx * fx + dz * fz, aside = Math.abs(dx * fz - dz * fx), up = owner.getY() - friend.getY();
			double overDragon = friend.getY() - dragon.getY();
			boolean shaken = dragon.getAnimation() == EntityDragonBase.ANIMATION_SHAKEPREY;
			log(String.format(Locale.ROOT, "double saddle: the friend %.2f blocks behind the owner along its back, %.2f to a side, %.2f lower; %.2f over its feet; prey shake %s, health %.1f -> %.1f",
				ahead, aside, up, overDragon, shaken, health, friend.getHealth()));
			helper.assertTrue(ahead > 0.6 && ahead < 2.5, "the friend is not behind the owner: " + ahead);
			helper.assertTrue(aside < 0.15, "the friend is to a side: " + aside);
			helper.assertTrue(Math.abs(up) < 0.5, "the friend is not on its back with the owner: " + up);
			helper.assertTrue(overDragon > 1.0, "the friend is not up on its back: " + overDragon);
			helper.assertTrue(!shaken && friend.getHealth() >= health, "the friend was taken for prey");
			// the saddle stays on while someone sits behind
			var menu = new ContainerDragon(0, dragon.dragonInventory, owner.getInventory(), dragon);
			helper.assertTrue(!menu.getSlot(DragonSaddle.SLOT).mayPickup(owner), "the saddle could be taken off with a friend on it");
			// the owner gets off on the ground: the friend too, next tick, and no fall hurts them
			owner.stopRiding();
		});
		helper.runAfterDelay(66, () -> {
			log("double saddle: owner off; friend on " + friend.getVehicle() + ", seat behind " + dragon.getBackRider() + ", no fall harm " + MiscProperties.hasDismounted(friend));
			helper.assertTrue(friend.getVehicle() == null, "the friend stayed on with its owner off");
			helper.assertTrue(dragon.getBackRider() == DragonSaddle.NOBODY, "the seat behind is still taken");
			helper.assertTrue(MiscProperties.hasDismounted(friend), "the friend's drop from its back is not spared");
			var menu = new ContainerDragon(0, dragon.dragonInventory, owner.getInventory(), dragon);
			helper.assertTrue(menu.getSlot(DragonSaddle.SLOT).mayPickup(owner), "the saddle cannot be taken off with no one behind");
			dragon.discard();
			bare.discard();
			leave(helper, owner, friend, third);
			helper.succeed();
		});
	}

	/**
	 * Its riders neither hurt it (a blow from the friend behind) nor shoot into it (an arrow of theirs passes through its
	 * body's parts); someone else's arrow strikes them. The saddle is kept in its save, and falls off when it dies.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
	public void doubleSaddleKeptAndDropped(GameTestHelper helper) {
		var level = helper.getLevel();
		var owner = player(helper, "saddle-owner", 2, 2);
		var friend = player(helper, "saddle-friend", 3, 2);
		var stranger = player(helper, "saddle-stranger", 4, 2);
		var dragon = dragon(helper, IafEntityRegistry.LIGHTNING_DRAGON, 100, owner, 6, 6);
		saddle(dragon);
		click(dragon, owner, ItemStack.EMPTY);
		click(dragon, friend, ItemStack.EMPTY);
		dragon.updateParts();
		float health = dragon.getHealth();
		boolean hurt = dragon.hurt(level.damageSources().playerAttack(friend), 6F);
		var head = dragon.getParts().get(DragonBones.HEAD);
		var arrow = EntityType.ARROW.create(level);
		arrow.setOwner(friend);
		boolean through = ServerEvents.cancelProjectileHit(arrow, new EntityHitResult(head));
		arrow.setOwner(stranger);
		boolean strangerThrough = ServerEvents.cancelProjectileHit(arrow, new EntityHitResult(head));
		arrow.discard();
		log(String.format(Locale.ROOT, "double saddle: the friend's blow hurt it %s (%.1f -> %.1f); the riders' arrow passes its head %s, a stranger's %s",
			hurt, health, dragon.getHealth(), through, strangerThrough));
		helper.assertTrue(!hurt && dragon.getHealth() >= health, "the friend behind hurt its dragon");
		helper.assertTrue(through && !strangerThrough, "arrows: a rider's passes " + through + ", a stranger's " + strangerThrough);
		owner.stopRiding();
		friend.stopRiding();
		// kept in its save
		CompoundTag saved = new CompoundTag();
		dragon.saveWithoutId(saved);
		var loaded = IafEntityRegistry.LIGHTNING_DRAGON.create(level);
		loaded.load(saved);
		boolean kept = loaded.hasDoubleSaddle() && loaded.dragonInventory.getItem(DragonSaddle.SLOT).is(IafItemRegistry.DOUBLE_DRAGON_SADDLE);
		loaded.discard();
		// dies: it falls off
		dragon.die(level.damageSources().generic());
		var dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(dragon.blockPosition()).inflate(8), e -> e.getItem().is(IafItemRegistry.DOUBLE_DRAGON_SADDLE));
		boolean emptied = dragon.dragonInventory.getItem(DragonSaddle.SLOT).isEmpty() && !dragon.hasDoubleSaddle();
		log("double saddle: kept in its save " + kept + "; dropped at its death " + dropped.size() + ", off it " + emptied);
		dropped.forEach(ItemEntity::discard);
		dragon.discard();
		leave(helper, owner, friend, stranger);
		helper.assertTrue(kept, "the double saddle was not kept in its save");
		helper.assertTrue(dropped.size() == 1 && emptied, "at its death the saddle fell " + dropped.size() + " times, off it " + emptied);
		helper.succeed();
	}

	/**
	 * Its owner's shift and right click opens its menu whatever is in hand (nothing, food: not eaten, a sword); the
	 * dragon staff keeps its own (its home); someone else's opens nothing; without shift its owner gets on.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void dragonMenuWithShift(GameTestHelper helper) {
		var owner = player(helper, "menu-owner", 2, 2);
		var stranger = player(helper, "menu-stranger", 3, 2);
		var dragon = dragon(helper, IafEntityRegistry.FIRE_DRAGON, 80, owner, 6, 6);
		owner.setShiftKeyDown(true);
		String[] held = {"nothing", "beef", "sword"};
		ItemStack[] stacks = {ItemStack.EMPTY, new ItemStack(net.minecraft.world.item.Items.BEEF, 4), new ItemStack(net.minecraft.world.item.Items.IRON_SWORD)};
		boolean[] opened = new boolean[3];
		int hunger = dragon.getHunger();
		for (int i = 0; i < 3; i++) {
			click(dragon, owner, stacks[i]);
			opened[i] = owner.containerMenu instanceof ContainerDragon;
			owner.closeContainer();
		}
		boolean fed = dragon.getHunger() != hunger || stacks[1].getCount() != 4 || dragon.isInLove();
		click(dragon, owner, new ItemStack(IafItemRegistry.DRAGON_STAFF));
		boolean staffMenu = owner.containerMenu instanceof ContainerDragon;
		boolean home = dragon.hasHomePosition;
		owner.closeContainer();
		stranger.setShiftKeyDown(true);
		click(dragon, stranger, ItemStack.EMPTY);
		boolean strangerMenu = stranger.containerMenu instanceof ContainerDragon;
		owner.setShiftKeyDown(false);
		click(dragon, owner, ItemStack.EMPTY);
		boolean rides = owner.getVehicle() == dragon;
		log(String.format(Locale.ROOT, "dragon menu with shift: %s %s, %s %s, %s %s; fed %s; staff opened it %s, home %s; a stranger's %s; without shift on it %s",
			held[0], opened[0], held[1], opened[1], held[2], opened[2], fed, staffMenu, home, strangerMenu, rides));
		dragon.discard();
		leave(helper, owner, stranger);
		helper.assertTrue(opened[0] && opened[1] && opened[2], "its owner's shift click did not open its menu with " + held[0] + " " + opened[0]
			+ ", " + held[1] + " " + opened[1] + ", " + held[2] + " " + opened[2]);
		helper.assertTrue(!fed, "shift and right click with food fed it instead of opening its menu");
		helper.assertTrue(!staffMenu && home, "the dragon staff's shift click no longer sets its home");
		helper.assertTrue(!strangerMenu, "someone else's shift click opened its menu");
		helper.assertTrue(rides, "without shift its owner did not get on");
		helper.succeed();
	}
}
