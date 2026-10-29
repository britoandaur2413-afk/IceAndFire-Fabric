package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/** The mod's buildings made in code, placed in a normal world and photographed outside and inside (IAF_ONLY=world). */
public class IafWorldTests implements FabricClientGameTest {
	private static void log(String message) {
		IafMod.LOGGER.info("[IAFWORLD] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("world")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			settings.setGenerateStructures(true);
			for (WorldCreationUiState.WorldTypeEntry entry : settings.getNormalPresetList()) {
				if (entry.preset() != null && entry.preset().is(WorldPresets.NORMAL)) {
					settings.setWorldType(entry);
				}
			}
		}).create()) {
			world.getServer().runOnServer(server -> log("world seed " + server.overworld().getSeed()));
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				if (!mc.options.hideGui) {
					mc.options.hideGui = !mc.options.hideGui;
				}
			});
			if (place("hall")) {
				int[] found = locate(context, world, "iceandfire_fabric:dragon_hall");
				if (found == null) {
					throw new AssertionError("no dragon hall found by /locate");
				}
				dragonHall(context, world, found[0], found[1]);
			}
			if (place("kingdom")) {
				int[] kingdom = locate(context, world, "iceandfire_fabric:pillager_kingdom");
				if (kingdom == null) {
					throw new AssertionError("no pillager kingdom found by /locate");
				}
				pillagerKingdom(context, world, kingdom[0], kingdom[1]);
			}
			if (place("sanctuary")) {
				int[] sanctuary = locate(context, world, "iceandfire_fabric:pixie_sanctuary");
				if (sanctuary == null) {
					throw new AssertionError("no pixie sanctuary found by /locate");
				}
				pixiePlace(context, world, sanctuary[0], sanctuary[1], false);
			}
			for (String[] place : new String[][]{{"nest", "iceandfire_fabric:dragon_roost"}, {"cave", "iceandfire_fabric:dragon_cave"}, {"spire", "iceandfire_fabric:dread_spire"},
				{"throne", "iceandfire_fabric:dread_throne"}}) {
				if (place(place[0])) {
					int[] found = locate(context, world, place[1]);
					if (found == null) {
						throw new AssertionError("no " + place[1] + " found by /locate");
					}
					switch (place[0]) {
						case "nest" -> nest(context, world, found[0], found[1]);
						case "cave" -> cave(context, world, found[0], found[1]);
						case "spire" -> spire(context, world, found[0], found[1]);
						default -> throne(context, world, found[0], found[1]);
					}
				}
			}
			if (place("cottage")) {
				int[] cottage = locate(context, world, "iceandfire_fabric:witch_cottage");
				if (cottage == null) {
					throw new AssertionError("no witches' cottage found by /locate");
				}
				cottage(context, world, cottage[0], cottage[1]);
			}
			if (place("hydra")) {
				int[] cave = locate(context, world, "iceandfire_fabric:hydra_cave");
				if (cave == null) {
					throw new AssertionError("no hydra's cave found by /locate");
				}
				hydraCave(context, world, cave[0], cave[1]);
			}
			if (place("cavern")) {
				int[] cavern = locate(context, world, "iceandfire_fabric:pixie_cavern");
				if (cavern == null) {
					throw new AssertionError("no pixie cavern found by /locate");
				}
				pixiePlace(context, world, cavern[0], cavern[1], true);
			}
		}
	}

	/** Whether to visit this place (IAF_PLACES=sanctuary,cavern... visits only those; all by default). */
	private static boolean place(String name) {
		String only = System.getenv("IAF_PLACES");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(name);
	}

	/** Where /locate finds the nearest of a structure (x, z), or null. */
	static int[] locate(ClientGameTestContext context, TestSingleplayerContext world, String structure) {
		String result = run(world, "locate structure " + structure);
		log("locate " + structure + ": " + result);
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[(-?\\d+), [^,]+, (-?\\d+)\\]").matcher(result);
		return m.find() ? new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))} : null;
	}

	/** A command's answer, as text. */
	private static String run(TestSingleplayerContext world, String command) {
		String[] result = {""};
		world.getServer().runOnServer(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new net.minecraft.commands.CommandSource() {
			public void sendSystemMessage(net.minecraft.network.chat.Component message) {
				result[0] += message.getString();
			}

			public boolean acceptsSuccess() {
				return true;
			}

			public boolean acceptsFailure() {
				return true;
			}

			public boolean shouldInformAdmins() {
				return false;
			}
		}), command));
		return result[0];
	}

	/** Teleports (flying) and waits for the land around to be made. */
	private static void go(ClientGameTestContext context, TestSingleplayerContext world, double x, double y, double z, float yaw, float pitch, int wait) {
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.runOnClient(mc -> mc.player.getAbilities().flying = true);
		context.waitTicks(wait);
	}

	/** A witches' cottage as the world made it: from its way in, from a corner above, and its room (its chest full of cakes). */
	private static void cottage(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz) {
		go(context, world, cx + 0.5, 200, cz + 0.5, 0, 90, 200);
		int[] at = new int[6];
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			boolean chest = false;
			boolean door = false;
			for (BlockPos pos : BlockPos.betweenClosed(cx - 14, level.getMinBuildHeight() + 1, cz - 14, cx + 14, level.getMaxBuildHeight() - 1, cz + 14)) {
				var state = level.getBlockState(pos);
				if (!chest && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) {
					chest = true;
					at[0] = pos.getX();
					at[1] = pos.getY();
					at[2] = pos.getZ();
				} else if (!door && state.is(net.minecraft.world.level.block.Blocks.DARK_OAK_DOOR)) {
					door = true;
					at[3] = pos.getX();
					at[4] = pos.getY();
					at[5] = pos.getZ();
				}
			}
			log("witches' cottage at " + cx + " " + cz + ": chest " + chest + " at " + at[0] + " " + at[1] + " " + at[2] + ", door " + door + " at " + at[3] + " "
				+ at[4] + " " + at[5] + ", biome " + level.getBiome(new BlockPos(cx, at[1], cz)).getRegisteredName());
			if (!chest || !door) {
				throw new AssertionError("the witches' cottage has no chest or no door");
			}
		});
		world.getServer().runCommand("gamemode spectator @p");
		// the hut's middle is five blocks in from its gate, the door two: out through the door is its front
		double mx = cx + 0.5;
		double mz = cz + 0.5;
		double fx = at[3] + 0.5 - mx;
		double fz = at[5] + 0.5 - mz;
		double f = Math.max(0.1, Math.sqrt(fx * fx + fz * fz));
		fx /= f;
		fz /= f;
		int y = at[4];
		// from over the trees about it: its front, a corner, its back
		look(context, world, mx + fx * 13 + fz * 3, y + 13, mz + fz * 13 - fx * 3, mx, y + 2.5, mz, 60);
		context.takeScreenshot("world_witch_cottage");
		look(context, world, mx + fx * 9 - fz * 10, y + 15, mz + fz * 9 + fx * 10, mx, y + 3, mz, 20);
		context.takeScreenshot("world_witch_cottage_corner");
		look(context, world, mx - fx * 11 + fz * 7, y + 14, mz - fz * 11 - fx * 7, mx, y + 3, mz, 20);
		context.takeScreenshot("world_witch_cottage_back");
		look(context, world, at[3] + 0.5 - fx * 1.2, y + 1.1, at[5] + 0.5 - fz * 1.2, mx - fx * 3, y + 0.6, mz - fz * 3, 20);
		context.takeScreenshot("world_witch_cottage_inside");
		world.getServer().runCommand("gamemode creative @p");
	}

	/**
	 * A hydra's cave as the world makes it now (a structure: the game's search and the exiled pixie's find it): her
	 * search for her hydra quest from the world's spawn finds a cave; its mound of grass in the swamp from above and
	 * before a way in, the hydra and its chests in its hollow.
	 */
	private static void hydraCave(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz) {
		BlockPos found = world.getServer().computeOnServer(server -> {
			var level = server.overworld();
			var structure = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
				.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, IafMod.id("hydra_cave")));
			BlockPos spawn = level.getSharedSpawnPos();
			int within = Math.max(1600, (int) Math.sqrt(spawn.distToLowCornerSqr(cx, spawn.getY(), cz)) + 200);
			var locator = new com.iafabric.pixie.FairyLocator(level, java.util.List.of(structure), spawn, within);
			for (int i = 0; i < 200000 && !locator.step(); i++) {
			}
			return locator.found();
		});
		log("her search for a hydra's cave (her hydra quest) from the spawn: " + found + ", /locate's at " + cx + " " + cz);
		// (how far the swamps are: the caves are only there)
		log("the nearest swamps: " + run(world, "locate biome minecraft:swamp") + " / " + run(world, "locate biome minecraft:mangrove_swamp"));
		if (found == null) {
			throw new AssertionError("her search for her hydra quest found no hydra's cave");
		}
		go(context, world, cx + 0.5, 200, cz + 0.5, 0, 90, 200);
		int[] at = world.getServer().computeOnServer(server -> {
			var level = server.overworld();
			BlockPos center = null;
			for (var start : level.getChunk(cx >> 4, cz >> 4).getAllStarts().values()) {
				for (var piece : start.getPieces()) {
					if (piece instanceof com.iafabric.world.structure.HydraCavePiece cave) {
						center = cave.center();
					}
				}
			}
			if (center == null) {
				throw new AssertionError("no hydra's cave starts at " + cx + " " + cz);
			}
			int hydras = level.getEntitiesOfClass(com.iafabric.entity.EntityHydra.class,
				new AABB(cx - 24, level.getMinBuildHeight(), cz - 24, cx + 24, level.getMaxBuildHeight(), cz + 24)).size();
			int chests = 0;
			int grass = 0;
			for (BlockPos pos : BlockPos.betweenClosed(center.offset(-10, -2, -10), center.offset(10, 8, 10))) {
				if (level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) {
					chests++;
				} else if (level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
					grass++;
				}
			}
			log("hydra's cave at " + center.toShortString() + ": " + hydras + " hydra(s), " + chests + " chest(s), " + grass + " grass blocks in its mound, biome "
				+ level.getBiome(center).getRegisteredName());
			if (hydras == 0 || grass < 200) {
				throw new AssertionError("the hydra's cave was not made whole (hydras " + hydras + ", grass " + grass + ")");
			}
			return new int[]{center.getX(), center.getY(), center.getZ()};
		});
		world.getServer().runCommand("gamemode spectator @p");
		// (a swamp's trees all about it: from where nothing is in the way)
		Vec3 target = new Vec3(at[0] + 0.5, at[1] + 2, at[2] + 0.5);
		Vec3 above = clearView(world, target, 16, 26, 8, 22, 6);
		look(context, world, above.x, above.y, above.z, target.x, target.y, target.z, 60);
		context.takeScreenshot("world_hydra_cave");
		Vec3 inside = new Vec3(at[0] + 0.5, at[1] + 1.2, at[2] + 0.5);
		Vec3 door = clearView(world, inside, 9, 15, 1, 4, 4);
		look(context, world, door.x, door.y, door.z, inside.x, inside.y, inside.z, 20);
		context.takeScreenshot("world_hydra_cave_door");
		world.getServer().runCommand("gamemode creative @p");
	}

	/**
	 * A place to see something from, with nothing in the way (no leaf, vine nor block but its own within a few
	 * blocks of it): around it in that many directions, that far, that high above it; the first that is clear.
	 */
	private static Vec3 clearView(TestSingleplayerContext world, Vec3 target, int nearest, int farthest, int lowest, int highest, int directions) {
		return world.getServer().computeOnServer(server -> {
			var level = server.overworld();
			for (int h = lowest; h <= highest; h += 2) {
				for (int d = nearest; d <= farthest; d += 2) {
					for (int i = 0; i < directions; i++) {
						double angle = Math.PI * 2 * i / directions;
						Vec3 from = target.add(Math.sin(angle) * d, h, Math.cos(angle) * d);
						if (clear(level, from, target, 6)) {
							log("a clear view of " + target + " from " + from);
							return from;
						}
					}
				}
			}
			log("no clear view of " + target + ": from above");
			return target.add(0, highest + 10, 1);
		});
	}

	/** Whether nothing is between these two places (but what is within that many blocks of the second). */
	private static boolean clear(net.minecraft.server.level.ServerLevel level, Vec3 from, Vec3 to, double near) {
		double length = from.distanceTo(to);
		for (double s = 0; s < length - near; s += 0.4) {
			Vec3 p = from.add(to.subtract(from).scale(s / length));
			if (!level.getBlockState(BlockPos.containing(p)).isAir()) {
				return false;
			}
		}
		return true;
	}

	/** Teleports (flying) to a place, looking at another. */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, double x, double y, double z, double tx, double ty, double tz, int wait) {
		double dx = tx - x;
		double dy = ty - y;
		double dz = tz - z;
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		go(context, world, x, y, z, yaw, pitch, wait);
	}

	/** A pixie sanctuary (from above) or cavern (from inside): its altar, its pixie houses and pixies. */
	private static void pixiePlace(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz, boolean cavern) {
		go(context, world, cx, 200, cz, 0, 90, 300);
		int[] altar = new int[3];
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			int houses = 0;
			boolean found = false;
			for (BlockPos pos : BlockPos.betweenClosed(cx - 24, level.getMinBuildHeight() + 1, cz - 24, cx + 24, level.getMaxBuildHeight() - 1, cz + 24)) {
				var block = level.getBlockState(pos).getBlock();
				if (block == com.iafabric.block.IafBlockRegistry.PIXIE_ALTAR && !found) {
					found = true;
					altar[0] = pos.getX();
					altar[1] = pos.getY();
					altar[2] = pos.getZ();
				} else if (block instanceof com.iafabric.block.BlockPixieHouse) {
					houses++;
				}
			}
			int pixies = level.getEntitiesOfClass(com.iafabric.entity.EntityPixie.class, new AABB(cx - 32, level.getMinBuildHeight(), cz - 32, cx + 32, level.getMaxBuildHeight(), cz + 32)).size();
			log((cavern ? "pixie cavern" : "pixie sanctuary") + " at " + cx + " " + cz + ": altar " + found + " at y " + altar[1] + ", " + houses + " houses, " + pixies + " pixies out");
			if (!found || houses == 0) {
				throw new AssertionError("the pixie place has no altar or no houses");
			}
		});
		if (cavern) {
			// which way the tunnel goes: open air far from the altar at the height of a man
			int[] way = new int[2];
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				int best = -1;
				for (int[] dir : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
					int open = 0;
					for (int d = 18; d <= 30; d++) {
						for (int h = 1; h <= 6; h++) {
							if (level.getBlockState(new BlockPos(altar[0] + dir[0] * d, altar[1] + h, altar[2] + dir[1] * d)).isAir()) {
								open++;
							}
						}
					}
					if (open > best) {
						best = open;
						way[0] = dir[0];
						way[1] = dir[1];
					}
				}
				// lava or loose water near the cavern
				int lava = 0;
				int water = 0;
				for (BlockPos pos : BlockPos.betweenClosed(altar[0] - 32, altar[1] - 6, altar[2] - 32, altar[0] + 32, altar[1] + 22, altar[2] + 32)) {
					var fluid = level.getFluidState(pos);
					if (fluid.is(net.minecraft.tags.FluidTags.LAVA)) {
						lava++;
					} else if (fluid.is(net.minecraft.tags.FluidTags.WATER) && !fluid.isSource()) {
						water++;
					}
				}
				log("pixie cavern: tunnel toward " + way[0] + " " + way[1] + ", lava blocks " + lava + ", flowing water " + water);
			});
			world.getServer().runCommand("gamemode spectator @p");
			float yaw = way[0] == 1 ? -90 : way[0] == -1 ? 90 : way[1] == 1 ? 0 : 180;
			// the village from the plaza's edge, as it is lit (no night vision), facing away from the tunnel
			go(context, world, altar[0] + 0.5 + way[0] * 12, altar[1] + 5, altar[2] + 0.5 + way[1] * 12, yaw + 180, 18, 60);
			context.takeScreenshot("world_pixie_cavern");
			// toward the tunnel, from the altar
			go(context, world, altar[0] + 0.5 - way[0] * 6, altar[1] + 3, altar[2] + 0.5 - way[1] * 6, yaw, 0, 30);
			context.takeScreenshot("world_pixie_cavern_tunnel");
			// inside the tunnel, halfway up
			go(context, world, altar[0] + 0.5 + way[0] * 38, altar[1] + 13, altar[2] + 0.5 + way[1] * 38, yaw + 180, 25, 40);
			context.takeScreenshot("world_pixie_cavern_tunnel_inside");
			world.getServer().runCommand("gamemode creative @p");
			// where the tunnel comes out, open to the sky toward its mouth: nothing hangs there from nothing
			var cave = piece(world, "iceandfire_fabric:pixie_cavern", cx, cz, com.iafabric.world.structure.PixieCavernPiece.class);
			int[] hanging = new int[1];
			int[] mouthY = new int[1];
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var box = cave.getBoundingBox();
				for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
					for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
						level.getChunk(x, z);
					}
				}
				BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
				for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
					var state = level.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					below.set(pos.getX(), pos.getY() - 1, pos.getZ());
					if (!state.canSurvive(level, pos) && !level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
						if (hanging[0]++ < 10) {
							log("hanging in the pixie cavern: " + state + " at " + pos.toShortString());
						}
					}
				}
				BlockPos mouth = cave.at(0, cave.end() + 10, 0);
				mouthY[0] = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mouth.getX(), mouth.getZ());
			});
			log("pixie cavern: things hanging in the air " + hanging[0]);
			look(context, world, cave, 0, cave.end() + 12, mouthY[0] + 9, 180, 35, 60);
			context.takeScreenshot("world_pixie_cavern_mouth");
			if (hanging[0] > 0) {
				throw new AssertionError(hanging[0] + " things hang in the air in the pixie cavern");
			}
		} else {
			// the tree and its village from the way in, above
			go(context, world, altar[0] + 0.5, altar[1] + 22, altar[2] - 40.5, 0, 22, 60);
			context.takeScreenshot("world_pixie_sanctuary");
			go(context, world, altar[0] - 30.5, altar[1] + 30, altar[2] + 0.5, -90, 30, 30);
			context.takeScreenshot("world_pixie_sanctuary_side");
			// on the ground by the houses
			go(context, world, altar[0] + 12.5, altar[1] + 2, altar[2] - 14.5, 40, 5, 30);
			context.takeScreenshot("world_pixie_sanctuary_village");
			// the altar in the hollow of the tree
			go(context, world, altar[0] + 0.5, altar[1] + 1.2, altar[2] - 5.5, 0, 10, 30);
			context.takeScreenshot("world_pixie_sanctuary_shrine");
			// the platform in the tree
			go(context, world, altar[0] + 0.5, altar[1] + 13, altar[2] - 7.5, 0, 20, 30);
			context.takeScreenshot("world_pixie_sanctuary_platform");
		}
	}

	/** A young dragon's nest on a hilltop: from above and from its rim. */
	private static void nest(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz) {
		var roost = piece(world, "iceandfire_fabric:dragon_roost", cx, cz, com.iafabric.world.structure.DragonLairPieces.Roost.class);
		BlockPos at = roost.gate();
		look(context, world, roost, 0, 0, at.getY() + 60, 0, 90, 200);
		world.getServer().runOnServer(server -> {
			var dragons = server.overworld().getEntitiesOfClass(EntityDragonBase.class, new AABB(at).inflate(30));
			for (EntityDragonBase dragon : dragons) {
				log(String.format(Locale.ROOT, "nest dragon %s stage %d asleep %s", dragon.getType().toShortString(), dragon.getDragonStage(), dragon.isSleeping()));
			}
			log("dragon nest at " + at + " kind " + roost.kind() + ": " + dragons.size() + " dragons");
			if (dragons.isEmpty()) {
				throw new AssertionError("no dragon at the nest");
			}
		});
		look(context, world, roost, -20, -20, at.getY() + 16, -45, 35, 40);
		context.takeScreenshot("world_dragon_nest");
	}

	/** A dragons' cave: inside, at its hoard. */
	private static void cave(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz) {
		var cave = piece(world, "iceandfire_fabric:dragon_cave", cx, cz, com.iafabric.world.structure.DragonLairPieces.Cave.class);
		BlockPos at = cave.gate();
		look(context, world, cave, 0, 0, at.getY() + 60, 0, 90, 200);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var dragons = level.getEntitiesOfClass(EntityDragonBase.class, new AABB(at).inflate(30));
			int lava = 0;
			for (BlockPos pos : BlockPos.betweenClosed(at.offset(-34, -2, -34), at.offset(34, 18, 34))) {
				if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA)) {
					lava++;
				}
			}
			log("dragon cave at " + at + " kind " + cave.kind() + ": " + dragons.size() + " dragons, lava blocks " + lava);
			if (dragons.size() < 2) {
				throw new AssertionError("the cave has " + dragons.size() + " dragons");
			}
		});
		world.getServer().runCommand("gamemode spectator @p");
		look(context, world, cave, 0, -24, at.getY() + 6, 0, 10, 40);
		context.takeScreenshot("world_dragon_cave");
		world.getServer().runCommand("gamemode creative @p");
	}

	/** A dread knight's spire with its undead dragons. */
	private static void spire(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz) {
		var spire = piece(world, "iceandfire_fabric:dread_spire", cx, cz, com.iafabric.world.structure.DreadPieces.Spire.class);
		BlockPos gate = spire.gate();
		look(context, world, spire, 0, 22, gate.getY() + 60, 0, 90, 200);
		world.getServer().runOnServer(server -> {
			var dragons = server.overworld().getEntitiesOfClass(EntityDragonBase.class, new AABB(spire.at(0, 22, gate.getY())).inflate(30, 40, 30), d -> d.dreadMount);
			int ridden = (int) dragons.stream().filter(d -> d.getVirtualRider() == com.iafabric.entity.DragonRiders.KNIGHT).count();
			for (EntityDragonBase dragon : dragons) {
				log(String.format(Locale.ROOT, "dread dragon stage %d health %.0f/%.0f hurt %d last %s at %s look %d sleeping %s sitting %s in wall %s",
					dragon.getDragonStage(), dragon.getHealth(), dragon.getMaxHealth(), dragon.hurtTime, dragon.getLastDamageSource(), dragon.blockPosition(),
					dragon.getDreadLook(), dragon.isSleeping(), dragon.isOrderedToSit(), dragon.isInWall()));
			}
			log("dread spire at " + gate + ": " + dragons.size() + " dread dragons, " + ridden + " ridden by knights");
			if (dragons.isEmpty()) {
				throw new AssertionError("no dread dragon at the spire");
			}
		});
		look(context, world, spire, 16, -10, gate.getY() + 26, 30, 25, 40);
		context.takeScreenshot("world_dread_spire");
	}

	/**
	 * Cryona's citadel: from high over its gate, its gate, the courtyard, the great hall, the throne room with Black
	 * Frost and Cryona asleep; its stones hold (a pick, a blast, a block set against it); a player comes near the
	 * throne and she wakes and takes them into her realm.
	 */
	private static void throne(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz) {
		var citadel = piece(world, "iceandfire_fabric:dread_throne", cx, cz, com.iafabric.world.structure.CitadelPiece.class);
		BlockPos gate = citadel.gate();
		int g = gate.getY();
		int throneV = com.iafabric.world.structure.CitadelPiece.THRONE_V;
		look(context, world, citadel, 0, 60, g + 90, 0, 90, 260);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var queens = level.getEntitiesOfClass(com.iafabric.entity.EntityCryonaAsleep.class, new AABB(citadel.at(0, throneV, g)).inflate(40));
			var frosts = level.getEntitiesOfClass(com.iafabric.entity.EntityBlackFrostGuard.class, new AABB(citadel.at(0, throneV, g)).inflate(40));
			var dead = level.getEntitiesOfClass(com.iafabric.entity.EntityDreadMob.class, new AABB(citadel.at(0, 66, g)).inflate(70, 30, 80));
			log("dread citadel at " + gate + ": " + queens.size() + " Cryona asleep (throne " + (queens.isEmpty() ? "-" : queens.get(0).throne()) + ", bed "
				+ (queens.isEmpty() ? "-" : queens.get(0).bed()) + "), " + frosts.size() + " Black Frost by her, " + dead.size() + " of the dread in all");
			if (queens.isEmpty() || frosts.isEmpty()) {
				throw new AssertionError("the citadel has no queen or no Black Frost");
			}
		});
		// from the snow before its gate, from high on a side, the gate close
		look(context, world, citadel, 0, -46, g + 34, 0, 22, 60);
		context.takeScreenshot("world_citadel");
		look(context, world, citadel, 90, 66, g + 60, 90, 32, 40);
		context.takeScreenshot("world_citadel_side");
		look(context, world, citadel, 0, -16, g + 3, 0, -5, 20);
		context.takeScreenshot("world_citadel_gate");
		// the courtyard from the wall over the gate, the great hall, the arch, the throne room
		look(context, world, citadel, 0, 8, g + 10, 0, 14, 20);
		context.takeScreenshot("world_citadel_courtyard");
		look(context, world, citadel, 0, 57, g + 5, 0, 5, 20);
		context.takeScreenshot("world_citadel_hall");
		look(context, world, citadel, 0, 80, g + 20, 0, 22, 20);
		context.takeScreenshot("world_citadel_throne_room");
		look(context, world, citadel, 0, throneV - 9, g + 6, 0, 18, 20);
		context.takeScreenshot("world_citadel_queen");
		// through the arch, as who comes in sees it: her throne ahead, Black Frost sitting behind it, nothing in the way
		look(context, world, citadel, 0, 72, g + 1, 0, 4, 40);
		context.takeScreenshot("world_citadel_arch");
		// from a side of the room: all of Black Frost inside it, its wings clear of the walls and lamps
		BlockPos side = citadel.at(-23, throneV + 4, g + 5);
		BlockPos frostAt = citadel.at(0, throneV + 1 + com.iafabric.world.structure.CitadelPiece.GUARD_BEHIND, g + 6);
		look(context, world, side.getX() + 0.5, side.getY(), side.getZ() + 0.5, frostAt.getX() + 0.5, frostAt.getY(), frostAt.getZ() + 0.5, 20);
		context.takeScreenshot("world_citadel_frost_side");
		// an older citadel's throne room (her throne at its back, her chest behind it, a stone left in her aisle): made anew
		// as it is now when she comes in, what her chest held moved to the new one, Black Frost behind her new throne
		float[] facing = {0F};
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			for (var sign : level.getEntitiesOfClass(com.iafabric.entity.EntityCryonaAsleep.class, new AABB(citadel.throneSeat()).inflate(30))) {
				facing[0] = sign.getYRot();
				sign.discard();
			}
			BlockPos oldSeat = citadel.at(0, 122, g + 3);
			BlockPos oldChest = citadel.at(-3, 123, g + 3);
			level.setBlock(oldChest, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
			if (level.getBlockEntity(oldChest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
				chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
			}
			level.setBlock(citadel.at(0, 100, g), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
			var older = com.iafabric.entity.EntityCryonaAsleep.make(level, oldSeat, citadel.at(0, 108, g), oldSeat.getX() + 0.5, oldSeat.getY() + 0.5,
				oldSeat.getZ() + 0.5, facing[0], net.minecraft.world.entity.MobSpawnType.COMMAND);
			level.addFreshEntity(older);
		});
		context.waitTicks(160);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var signs = level.getEntitiesOfClass(com.iafabric.entity.EntityCryonaAsleep.class, new AABB(citadel.throneSeat()).inflate(30));
			BlockPos newChest = citadel.at(-3, throneV + 2, g + 3);
			int diamonds = level.getBlockEntity(newChest) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest ? chest.getItem(0).getCount() : -1;
			boolean oldGone = level.getBlockState(citadel.at(-3, 123, g + 3)).isAir();
			boolean aisle = !level.getBlockState(citadel.at(0, 100, g)).is(net.minecraft.world.level.block.Blocks.STONE);
			var guards = level.getEntitiesOfClass(com.iafabric.entity.EntityBlackFrostGuard.class, new AABB(citadel.throneSeat()).inflate(48));
			log("an older throne room made anew: Cryona on " + (signs.isEmpty() ? "-" : signs.get(0).throne()) + " (her throne " + citadel.throneSeat()
				+ "), chest moved with " + diamonds + " diamonds, old chest gone " + oldGone + ", aisle clear " + aisle + ", " + guards.size() + " Black Frost by her");
			if (signs.size() != 1 || !signs.get(0).throne().equals(citadel.throneSeat()) || diamonds != 7 || !oldGone || !aisle || guards.size() != 1) {
				throw new AssertionError("an older throne room was not made anew as it is now");
			}
		});
		// its stones hold while she reigns: a pick on its wall, a block set against it, a blast at its gate
		world.getServer().runCommand("gamemode survival @p");
		world.getServer().runCommand("effect give @p resistance 120 4 true");
		look(context, world, citadel, 0, -4, g, 0, 0, 10);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var player = server.getPlayerList().getPlayers().get(0);
			BlockPos wall = citadel.at(12, 1, g + 4);
			BlockPos inside = citadel.at(6, 20, g);
			boolean sealedWall = com.iafabric.boss.CastleWard.sealed(level, wall);
			boolean broken = player.gameMode.destroyBlock(wall);
			var stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
			var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(inside.below()), net.minecraft.core.Direction.UP, inside.below(), false);
			var placed = stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
			BlockPos gateWall = citadel.at(5, 1, g + 3);
			var before = level.getBlockState(gateWall);
			level.explode(null, gateWall.getX() + 0.5, gateWall.getY() + 0.5, gateWall.getZ() - 1.5, 4F, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
			boolean blasted = !level.getBlockState(gateWall).equals(before);
			log("citadel ward: sealed " + sealedWall + ", wall broken " + broken + " (" + level.getBlockState(wall) + "), block set " + placed + " ("
				+ level.getBlockState(inside) + "), gate stone blasted " + blasted);
			if (!sealedWall || broken || level.getBlockState(inside).is(net.minecraft.world.level.block.Blocks.COBBLESTONE) || blasted) {
				throw new AssertionError("the citadel's stones did not hold");
			}
			// nor lava from a bucket (looking down at its floor)
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET));
			player.setXRot(90F);
			player.gameMode.useItem(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
			int lava = 0;
			for (BlockPos pos : BlockPos.betweenClosed(player.blockPosition().offset(-3, -2, -3), player.blockPosition().offset(3, 2, 3))) {
				if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA)) {
					lava++;
				}
			}
			// (the same, far from any citadel, does pour it: the ward is what holds it back)
			var away = player.position();
			BlockPos farColumn = player.blockPosition().offset(400, 0, 400);
			level.getChunk(farColumn);
			BlockPos far = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, farColumn).above(2);
			// (a slab of stone to pour it on, whatever the land is there)
			for (BlockPos pos : BlockPos.betweenClosed(far.offset(-2, -1, -2), far.offset(2, -1, 2))) {
				level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
			}
			player.teleportTo(far.getX() + 0.5, far.getY(), far.getZ() + 0.5);
			player.setXRot(90F);
			log("citadel ward: the control pours at " + far.toShortString() + " on " + level.getBlockState(far.below()));
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET));
			player.gameMode.useItem(player, level, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
			int control = 0;
			for (BlockPos pos : BlockPos.betweenClosed(far.offset(-3, -2, -3), far.offset(3, 2, 3))) {
				if (level.getFluidState(pos).is(net.minecraft.tags.FluidTags.LAVA)) {
					control++;
					level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
				}
			}
			player.teleportTo(away.x, away.y, away.z);
			log("citadel ward: lava poured " + lava + " (far from it: " + control + ")");
			if (lava > 0 || control == 0) {
				throw new AssertionError("the ward on lava is wrong: in the citadel " + lava + ", far from it " + control);
			}
			player.setXRot(0F);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
		});
		// a player comes near her throne: she wakes and takes the player into her realm, the Frozen Throne
		look(context, world, citadel, 0, throneV - 3, g + 1, 0, 5, 80);
		context.waitTicks(60);
		context.takeScreenshot("world_citadel_realm");
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			log("woken on her throne, the player is now in " + player.level().dimension().location() + ", fight "
				+ com.iafabric.boss.ThroneFight.arenaOf(player));
		});
		world.getServer().runCommand("gamemode creative @p");
	}

	/** The first piece of the structure that starts in the chunk at (x, z), of that class. */
	private static <P> P piece(TestSingleplayerContext world, String structure, int x, int z, Class<P> type) {
		Object[] found = new Object[1];
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			var registry = level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
			var start = level.getChunk(x >> 4, z >> 4).getStartForStructure(registry.get(net.minecraft.resources.ResourceLocation.parse(structure)));
			if (start != null) {
				for (var p : start.getPieces()) {
					if (type.isInstance(p) && found[0] == null) {
						found[0] = p;
					}
				}
			}
		});
		if (found[0] == null) {
			throw new AssertionError("no " + type.getSimpleName() + " of " + structure + " at " + x + " " + z);
		}
		return type.cast(found[0]);
	}

	/** Goes to a place in a building's frame, looking along its +v (plus a turn). */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, com.iafabric.world.structure.BuiltPiece piece,
		int u, int v, int y, float turn, float pitch, int wait) {
		BlockPos at = piece.at(u, v, y);
		float yaw = switch (piece.facing() & 3) {
			case 0 -> 0F;
			case 1 -> 90F;
			case 2 -> 180F;
			default -> -90F;
		};
		go(context, world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw + turn, pitch, wait);
	}

	private static void pillagerKingdom(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz) {
		var kingdom = piece(world, "iceandfire_fabric:pillager_kingdom", cx, cz, com.iafabric.world.structure.PillagerKingdomPiece.class);
		BlockPos gate = kingdom.gate();
		look(context, world, kingdom, 0, 30, gate.getY() + 60, 0, 90, 300);
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			BlockPos middle = kingdom.at(0, 35, gate.getY());
			AABB box = new AABB(middle).inflate(60, 40, 60);
			int dragons = 0;
			int ridden = 0;
			int sitting = 0;
			for (EntityDragonBase dragon : level.getEntitiesOfClass(EntityDragonBase.class, box, d -> d.pillagerMount)) {
				dragons++;
				if (dragon.getVirtualRider() != com.iafabric.entity.DragonRiders.NONE) {
					ridden++;
				}
				if (dragon.isOrderedToSit()) {
					sitting++;
				}
				log(String.format(Locale.ROOT, "kingdom dragon %s stage %d elite %s armor %s rider %s at %s", dragon.getType().toShortString(), dragon.getDragonStage(),
					dragon.eliteMount, dragon.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD), dragon.getVirtualRider(), dragon.blockPosition()));
			}
			int illagers = level.getEntitiesOfClass(net.minecraft.world.entity.raid.Raider.class, box).size();
			log("pillager kingdom at " + cx + " " + cz + " kind " + kingdom.kind() + ": " + dragons + " dragons (" + ridden + " ridden, " + sitting + " sitting), "
				+ illagers + " illagers");
			if (dragons < 2 || ridden < dragons) {
				throw new AssertionError("the kingdom has " + dragons + " dragons, " + ridden + " ridden");
			}
		});
		look(context, world, kingdom, -8, -30, gate.getY() + 32, 0, 32, 60);
		context.takeScreenshot("world_pillager_kingdom");
		look(context, world, kingdom, 40, 20, gate.getY() + 24, 90, 35, 30);
		context.takeScreenshot("world_pillager_kingdom_side");
		// a roost with its dragon and rider, from the road
		look(context, world, kingdom, -6, 6, gate.getY() + 7, -40, 22, 40);
		context.takeScreenshot("world_pillager_kingdom_roost");
		// the keep's hall
		world.getServer().runCommand("gamemode spectator @p");
		look(context, world, kingdom, 0, 51, gate.getY() + 3, 0, 8, 40);
		context.takeScreenshot("world_pillager_kingdom_keep");
		world.getServer().runCommand("gamemode creative @p");
	}

	/** The hall found at (cx, cz): the castle, the stair, the forge, the bridge over the chasm, the arena and the hoard. */
	private static void dragonHall(ClientGameTestContext context, TestSingleplayerContext world, int cx, int cz) {
		var castle = piece(world, "iceandfire_fabric:dragon_hall", cx, cz, com.iafabric.world.structure.DragonHallPieces.Castle.class);
		BlockPos gate = castle.gate();
		int g = gate.getY();
		int forge = g - com.iafabric.world.structure.DragonHallPieces.FORGE_DROP;
		int arena = g - castle.param();
		log("dragon hall at " + cx + " " + cz + ": gate " + gate + " facing " + castle.facing() + " kind " + castle.kind() + " forge floor " + forge
			+ " arena floor " + arena);
		// wait for the whole building to be made: from above its middle, then over the arena
		look(context, world, castle, 0, 100, g + 60, 0, 90, 300);
		look(context, world, castle, 0, 190, g + 60, 0, 90, 200);
		double[] found = new double[2];
		world.getServer().runOnServer(server -> {
			var level = server.overworld();
			BlockPos hoard = castle.at(0, com.iafabric.world.structure.DragonHallPieces.HOARD_V, arena);
			for (EntityDragonBase dragon : level.getEntitiesOfClass(EntityDragonBase.class, new AABB(hoard).inflate(40))) {
				log(String.format(Locale.ROOT, "dragon %s stage %d at %.0f %.0f %.0f asleep %s", dragon.getType().toShortString(), dragon.getDragonStage(),
					dragon.getX(), dragon.getY(), dragon.getZ(), dragon.isSleeping()));
				if (dragon.getDragonStage() == 5) {
					found[0] = 1;
				}
			}
			// fluids that got out: flowing lava or water in the forge hall
			int loose = 0;
			for (BlockPos pos : BlockPos.betweenClosed(castle.at(-20, 62, forge), castle.at(20, 100, forge + 10))) {
				if (!level.getFluidState(pos).isEmpty() && !level.getFluidState(pos).isSource()) {
					loose++;
				}
			}
			log("flowing fluid blocks in the forge hall: " + loose);
			// no bit of a tree in the castle, its walls and towers (a crown cut by it, or grown into it from a chunk made later)
			int leaves = 0;
			int half = com.iafabric.world.structure.DragonHallPieces.CASTLE_HALF;
			for (BlockPos pos : BlockPos.betweenClosed(castle.at(-half, 0, g), castle.at(half, com.iafabric.world.structure.DragonHallPieces.CASTLE_END, g + 26))) {
				if (level.getBlockState(pos).is(net.minecraft.tags.BlockTags.LEAVES)) {
					if (leaves++ < 5) {
						log("leaves inside the castle at " + pos.toShortString());
					}
				}
			}
			log("leaves in the castle: " + leaves);
			found[1] = leaves;
		});
		if (found[0] == 0) {
			throw new AssertionError("no stage 5 dragon on the hoard");
		}
		if (found[1] > 0) {
			throw new AssertionError(found[1] + " leaves in the castle");
		}
		// the castle from outside, before the gate, high up
		look(context, world, castle, 14, -34, g + 24, -18, 28, 60);
		context.takeScreenshot("world_dragon_hall_castle");
		// the courtyard and the keep
		look(context, world, castle, 0, 8, g + 5, 0, 5, 30);
		context.takeScreenshot("world_dragon_hall_courtyard");
		world.getServer().runCommand("gamemode spectator @p");
		// in the keep: the stairwell going down
		look(context, world, castle, 0, 35, g + 7, 0, 32, 30);
		context.takeScreenshot("world_dragon_hall_keep");
		// the stair, low down
		look(context, world, castle, 0, 54, g - 11, 0, 20, 30);
		context.takeScreenshot("world_dragon_hall_stair");
		// the forge hall
		look(context, world, castle, 0, 64, forge + 5, 0, 10, 40);
		context.takeScreenshot("world_dragon_hall_forge");
		// a side room
		look(context, world, castle, -13, 74, forge + 2, -90, 10, 30);
		context.takeScreenshot("world_dragon_hall_room");
		// the bridge over the chasm
		look(context, world, castle, 0, 102, forge + 5, 0, 18, 40);
		context.takeScreenshot("world_dragon_hall_bridge");
		look(context, world, castle, 8, 116, forge - 4, -30, 25, 30);
		context.takeScreenshot("world_dragon_hall_chasm");
		// the arena: from its gate, from a gallery, and the hoard
		int arenaGate = com.iafabric.world.structure.DragonHallPieces.ARENA_V0;
		int hoardV = com.iafabric.world.structure.DragonHallPieces.HOARD_V;
		int half = com.iafabric.world.structure.DragonHallPieces.ARENA_HALF;
		look(context, world, castle, 0, arenaGate + 2, arena + 6, 0, 6, 40);
		context.takeScreenshot("world_dragon_hall_arena");
		look(context, world, castle, half - 6, arenaGate + 24, arena + 20, -30, 22, 30);
		context.takeScreenshot("world_dragon_hall_gallery");
		look(context, world, castle, 0, hoardV - 34, arena + 8, 0, 8, 30);
		context.takeScreenshot("world_dragon_hall_hoard");
		// how high and wide it is, from the middle of the floor
		look(context, world, castle, 0, arenaGate + 30, arena + 2, 0, -20, 20);
		context.takeScreenshot("world_dragon_hall_vault");
		world.getServer().runCommand("gamemode creative @p");
	}
}
