package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.client.gui.GuiLectern;
import com.iafabric.entity.EntityChainTie;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityHippogryph;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.props.ChainProperties;
import com.iafabric.entity.tile.TileEntityLectern;
import com.iafabric.inventory.ContainerLectern;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.TestSingleplayerContext;
import com.iafabric.util.ItemNbt;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * IAF_ONLY=reports: what a player reported on CurseForge (2026-10-28), each done as they would do it. A baby dragon put
 * on its owner's shoulder stays there as they walk; a hippogryph brought down with shift lets its rider off on both
 * sides at once (no "ghost": off on the screen while the server still has them riding); a dragon chained to the player
 * is tied to a wall with a right click; the bestiary takes the page clicked at the lectern for manuscripts.
 */
public class IafPlayerReportsScene implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFREPORTS] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (IafTestOnly.skip("reports")) {
			return;
		}
		List<String> problems = new ArrayList<>();
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
			settings.setAllowCheats(true);
		}).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			context.runOnClient(mc -> mc.options.hideGui = true);
			shoulder(context, world, problems);
			hippogryph(context, world, problems);
			bestiary(context, world, problems);
			chain(context, world, problems);
			horn(context, world, problems);
			context.runOnClient(mc -> {
				mc.options.hideGui = false;
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
		}
		if (!problems.isEmpty()) {
			throw new AssertionError(String.join(" | ", problems));
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
	}

	/** A baby dragon of its owner's: a right click with an empty hand puts it on their shoulder, and it stays there as they walk. */
	private static void shoulder(ClientGameTestContext context, TestSingleplayerContext world, List<String> problems) {
		world.getServer().runCommand("tp @p 0.5 " + GROUND + " 0.5 0 33");
		int[] id = {0};
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			EntityDragonBase dragon = IafEntityRegistry.FIRE_DRAGON.create(level);
			dragon.setAgeInDays(5);
			dragon.moveTo(0.5, GROUND, 2.5, 180, 0);
			dragon.tame(player(server));
			dragon.setCommand(1);
			level.addFreshEntity(dragon);
			id[0] = dragon.getId();
			log("shoulder: a stage " + dragon.getDragonStage() + " dragon, " + dragon.getBbWidth() + " wide, its owner's");
		});
		context.waitTicks(30);
		String aimed = context.computeOnClient(mc -> mc.hitResult instanceof EntityHitResult hit ? hit.getEntity().getType().toShortString()
			: String.valueOf(mc.hitResult == null ? null : mc.hitResult.getType()));
		log("shoulder: the crosshair on " + aimed);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
		context.waitTicks(10);
		riding(context, world, id[0], "after the click");
		context.waitTicks(40);
		riding(context, world, id[0], "2 s later");
		context.getInput().holdKeyFor(InputConstants.getKey("key.keyboard.w"), 30);
		context.waitTicks(5);
		boolean[] on = riding(context, world, id[0], "after walking");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(10);
		context.takeScreenshot("reports_shoulder");
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
		context.waitTicks(10);
		context.takeScreenshot("reports_shoulder_back");
		String drawn = context.computeOnClient(mc -> {
			Entity dragon = mc.level.getEntity(id[0]);
			return !(dragon instanceof EntityDragonBase baby) ? "none" : "at " + dragon.position().subtract(mc.player.position()) + ", invisible " + dragon.isInvisible()
				+ ", flying " + baby.isFlying() + " (" + baby.flyProgress + "), hovering " + baby.isHovering() + " (" + baby.hoverProgress + "), riding pose "
				+ baby.ridingProgress + ", sitting " + baby.sitProgress + ", render size " + baby.getRenderSize();
		});
		log("shoulder: the dragon drawn " + drawn);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		if (!on[0] || !on[1]) {
			problems.add("the baby dragon is not on its owner's shoulder (server " + on[0] + ", client " + on[1] + ")");
		}
		world.getServer().runOnServer(server -> {
			Entity dragon = server.overworld().getEntity(id[0]);
			if (dragon != null) {
				dragon.stopRiding();
				dragon.discard();
			}
		});
		context.waitTicks(5);
	}

	/** Whether the dragon rides the player on the server and on the client, and how far from the player's head it is. */
	private static boolean[] riding(ClientGameTestContext context, TestSingleplayerContext world, int id, String when) {
		String server = world.getServer().computeOnServer(s -> {
			Entity dragon = s.overworld().getEntity(id);
			ServerPlayer player = player(s);
			return dragon == null ? "none" : (dragon.getVehicle() == player) + " " + String.format("%.2f", dragon.position().distanceTo(player.getEyePosition()));
		});
		String client = context.computeOnClient(mc -> {
			Entity dragon = mc.level.getEntity(id);
			return dragon == null ? "none" : (dragon.getVehicle() == mc.player) + " " + String.format("%.2f", dragon.position().distanceTo(mc.player.getEyePosition()));
		});
		log("shoulder " + when + ": on it (server, blocks from the head) " + server + "; (client) " + client);
		return new boolean[]{server.startsWith("true"), client.startsWith("true")};
	}

	/**
	 * A hippogryph in the air, its rider holding shift: it goes down, lands, and its rider gets off. On the screen and on
	 * the server alike: the player never stands off it on the screen while the server still has them riding.
	 */
	private static void hippogryph(ClientGameTestContext context, TestSingleplayerContext world, List<String> problems) {
		world.getServer().runCommand("tp @p 30.5 " + GROUND + " 0.5 0 0");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			ServerPlayer player = player(server);
			EntityHippogryph hippogryph = IafEntityRegistry.HIPPOGRYPH.create(level);
			hippogryph.moveTo(30.5, GROUND + 8, 0.5, 0, 0);
			hippogryph.tame(player);
			hippogryph.setSaddled(true);
			level.addFreshEntity(hippogryph);
			hippogryph.setFlying(true);
			player.startRiding(hippogryph, true);
		});
		context.waitTicks(30);
		InputConstants.Key shift = InputConstants.getKey("key.keyboard.left.shift");
		context.getInput().holdKey(shift);
		int ghost = 0;
		int offAt = -1;
		for (int tick = 0; tick < 160 && offAt < 0; tick++) {
			context.waitTick();
			boolean client = context.computeOnClient(mc -> mc.player.isPassenger());
			boolean server = world.getServer().computeOnServer(s -> player(s).isPassenger());
			if (!client && server) {
				ghost++;
			}
			if (!client && !server) {
				offAt = tick;
			}
		}
		context.getInput().releaseKey(shift);
		context.waitTicks(20);
		boolean[] after = {context.computeOnClient(mc -> mc.player.isPassenger()), world.getServer().computeOnServer(s -> player(s).isPassenger())};
		log("hippogryph: shift held from the air, off on both sides at tick " + offAt + "; ticks off on the screen while the server had them riding: "
			+ ghost + "; riding 1 s after letting go (client, server): " + Arrays.toString(after));
		if (ghost > 0 || after[0] != after[1]) {
			problems.add("getting off a landed hippogryph: off on the screen while the server still had the player riding (" + ghost + " ticks)");
		}
		if (offAt < 0) {
			problems.add("the hippogryph never let its rider off");
		}
		world.getServer().runOnServer(server -> {
			player(server).stopRiding();
			for (EntityHippogryph hippogryph : server.overworld().getEntitiesOfClass(EntityHippogryph.class, new AABB(10, GROUND - 5, -20, 50, GROUND + 30, 20))) {
				hippogryph.discard();
			}
		});
		context.waitTicks(5);
	}

	/** The bestiary and manuscripts in the lectern: a click on one of the pages it offers puts that page in the book. */
	private static void bestiary(ClientGameTestContext context, TestSingleplayerContext world, List<String> problems) {
		BlockPos at = new BlockPos(-4, GROUND, 3);
		world.getServer().runCommand("tp @p -3.5 " + GROUND + " 0.5 0 0");
		world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(at, IafBlockRegistry.LECTERN.defaultBlockState()));
		context.waitTicks(10);
		context.getInput().lookAt(at);
		context.waitTicks(5);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
		context.waitTicks(10);
		// the book and the manuscripts put in, as through its screen
		world.getServer().runOnServer(server -> {
			TileEntityLectern lectern = (TileEntityLectern) server.overworld().getBlockEntity(at);
			ItemStack book = new ItemStack(IafItemRegistry.BESTIARY);
			ItemNbt.update(book, tag -> tag.putIntArray("Pages", new int[]{0}));
			lectern.setItem(1, new ItemStack(IafItemRegistry.MANUSCRIPT, 9));
			lectern.setItem(0, book);
		});
		context.waitTicks(15);
		boolean open = context.computeOnClient(mc -> mc.screen instanceof GuiLectern);
		String offered = context.computeOnClient(mc -> mc.screen instanceof GuiLectern lectern
			? Arrays.toString(((ContainerLectern) lectern.getMenu()).getPossiblePages()) : "no lectern screen");
		log("bestiary: the lectern open " + open + ", its pages to choose: " + offered);
		context.takeScreenshot("reports_lectern");
		double[] button = context.computeOnClient(mc -> {
			var screen = mc.screen;
			int left = (screen.width - 176) / 2;
			int top = (screen.height - 166) / 2;
			double scale = (double) mc.getWindow().getScreenWidth() / mc.getWindow().getGuiScaledWidth();
			return new double[]{(left + 60 + 54) * scale, (top + 14 + 9) * scale};
		});
		context.getInput().setCursorPos(button[0], button[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(15);
		// (a click the screen missed while it was still being laid out: once more, and said)
		int took = world.getServer().computeOnServer(server -> ItemNbt.getOrEmpty(((TileEntityLectern) server.overworld().getBlockEntity(at)).getItem(0)).getIntArray("Pages").length);
		if (took < 2) {
			log("bestiary: the first click took no page, clicking again; on the client: " + context.computeOnClient(mc -> mc.screen instanceof GuiLectern lectern ? "manuscripts " + ((ContainerLectern) lectern.getMenu()).getManuscriptAmount() + ", pages " + Arrays.toString(((ContainerLectern) lectern.getMenu()).getPossiblePages()) + ", lectern known " + com.iafabric.IafMod.PROXY.getRefrencedTE() + ", button 0 takes it " + ((ContainerLectern) lectern.getMenu()).clickMenuButton(mc.player, 0) : "screen " + mc.screen));
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitTicks(15);
		}
		int[] pages = world.getServer().computeOnServer(server -> {
			TileEntityLectern lectern = (TileEntityLectern) server.overworld().getBlockEntity(at);
			return ItemNbt.getOrEmpty(lectern.getItem(0)).getIntArray("Pages");
		});
		int manuscripts = world.getServer().computeOnServer(server -> ((TileEntityLectern) server.overworld().getBlockEntity(at)).getItem(1).getCount());
		log("bestiary: after a click on the first page, the book's pages " + Arrays.toString(pages) + ", manuscripts left " + manuscripts);
		if (pages.length < 2) {
			problems.add("the bestiary took no page at the lectern (pages " + Arrays.toString(pages) + ")");
		}
		context.runOnClient(mc -> mc.setScreen(null));
		context.waitTicks(5);
	}

	/** The dragon horn used on the part of a dragon farthest from its body's middle (a wing tip, the tail's end): the dragon goes into it. */
	private static void horn(ClientGameTestContext context, TestSingleplayerContext world, List<String> problems) {
		world.getServer().runCommand("tp @p 20.5 " + GROUND + " -14.5 0 0");
		context.waitTicks(5);
		int[] id = {0};
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			EntityDragonBase dragon = IafEntityRegistry.LIGHTNING_DRAGON.create(level);
			dragon.setAgeInDays(74);
			dragon.moveTo(20.5, GROUND, -8.5, 0, 0);
			dragon.tame(player(server));
			dragon.setCommand(1);
			level.addFreshEntity(dragon);
			id[0] = dragon.getId();
		});
		context.waitTicks(40);
		String result = world.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			ServerPlayer player = player(server);
			Entity dragon = level.getEntity(id[0]);
			com.iafabric.entity.EntityMutlipartPart far = null;
			double most = -1;
			for (com.iafabric.entity.EntityMutlipartPart part : level.getEntitiesOfClass(com.iafabric.entity.EntityMutlipartPart.class,
				dragon.getBoundingBox().inflate(16), part -> part.getParent() == dragon)) {
				double away = part.position().distanceTo(dragon.position());
				if (away > most) {
					most = away;
					far = part;
				}
			}
			if (far == null) {
				return "no part";
			}
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(IafItemRegistry.DRAGON_HORN));
			var used = player.interactOn(far, InteractionHand.MAIN_HAND);
			String in = ItemNbt.getOrEmpty(player.getMainHandItem()).getString("DragonHornEntityID");
			return String.format(java.util.Locale.ROOT, "a part %.1f blocks from its middle, %s, the horn holds '%s', the dragon %s", most, used, in,
				dragon.isRemoved() ? "gone into it" : "still out");
		});
		log("horn: " + result);
		if (!result.contains("gone into it")) {
			problems.add("the dragon horn did nothing on a part of the dragon (" + result + ")");
		}
	}

	/** A dragon chained to the player, then a right click with the chain on a wall: the dragon is tied to the wall. */
	private static void chain(ClientGameTestContext context, TestSingleplayerContext world, List<String> problems) {
		BlockPos wall = new BlockPos(-10, GROUND, 3);
		world.getServer().runCommand("tp @p -9.5 " + GROUND + " 0.5 0 0");
		context.waitTicks(5);
		int[] id = {0};
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			ServerPlayer player = player(server);
			level.setBlockAndUpdate(wall, Blocks.COBBLESTONE_WALL.defaultBlockState());
			EntityDragonBase dragon = IafEntityRegistry.ICE_DRAGON.create(level);
			dragon.setAgeInDays(55);
			dragon.moveTo(-15.5, GROUND, 6.5, 0, 0);
			dragon.tame(player);
			dragon.setCommand(1);
			level.addFreshEntity(dragon);
			id[0] = dragon.getId();
			ItemStack chain = new ItemStack(IafItemRegistry.CHAIN, 4);
			player.setItemInHand(InteractionHand.MAIN_HAND, chain);
			chain.getItem().interactLivingEntity(chain, player, dragon, InteractionHand.MAIN_HAND);
			log("chain: the dragon chained to the player " + ChainProperties.isChainedTo(dragon, player));
		});
		context.waitTicks(10);
		context.getInput().lookAt(wall);
		context.waitTicks(5);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
		context.waitTicks(40);
		String tied = world.getServer().computeOnServer(server -> {
			ServerLevel level = server.overworld();
			List<EntityChainTie> ties = level.getEntitiesOfClass(EntityChainTie.class, new AABB(wall).inflate(2));
			Entity dragon = level.getEntity(id[0]);
			boolean held = dragon instanceof EntityDragonBase chained && !ties.isEmpty() && ChainProperties.isChainedTo(chained, ties.get(0));
			return ties.size() + " ties at the wall, the dragon held by it " + held;
		});
		log("chain: after a right click on the wall, " + tied);
		if (!tied.endsWith("true")) {
			problems.add("the chained dragon was not tied to the wall (" + tied + ")");
		}
		context.getInput().lookAt(new BlockPos(-14, GROUND + 1, 5));
		context.waitTicks(10);
		context.takeScreenshot("reports_chain_wall");
	}
}
