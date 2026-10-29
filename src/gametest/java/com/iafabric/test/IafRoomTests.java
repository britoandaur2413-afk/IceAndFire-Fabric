package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.world.structure.BuiltPiece;
import com.iafabric.world.structure.DragonHallPieces;
import com.iafabric.world.structure.StructureRoom;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Where the player's world ("Mundo nuevo", its seed) had a dragon hall made over a snowy village and a trial chamber,
 * both left cut in half, and a brazier of its arena floating over coins: now those give way to the hall, nothing of
 * another structure reaches into it, and nothing hangs in the air in its castle or its arena (IAF_ONLY=room).
 */
public class IafRoomTests implements FabricClientGameTest {
	private static final long SEED = -4662578358899072206L;
	/** The hall's start chunk in that world, and those of the trial chamber and the village it cut. */
	private static final ChunkPos HALL = new ChunkPos(13707, 2110);
	private static final ChunkPos TRIAL = new ChunkPos(13708, 2109);
	private static final ChunkPos VILLAGE = new ChunkPos(13711, 2115);
	/** Its pixie cavern in the snow, whose tunnel comes out in a long open cut (vines and a chain hung there from air). */
	private static final ChunkPos CAVERN = new ChunkPos(13680, 2126);

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFROOM] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("room")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			settings.setGenerateStructures(true);
			settings.setSeed(Long.toString(SEED));
			for (WorldCreationUiState.WorldTypeEntry entry : settings.getNormalPresetList()) {
				if (entry.preset() != null && entry.preset().is(WorldPresets.NORMAL)) {
					settings.setWorldType(entry);
				}
			}
		}).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				if (!mc.options.hideGui) {
					mc.options.hideGui = !mc.options.hideGui;
				}
			});
			Object[] found = new Object[2];
			String[] problem = new String[1];
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
				StructureStart hall = null;
				List<StructureStart> others = new ArrayList<>();
				int r = 12;
				for (int dx = -r; dx <= r; dx++) {
					for (int dz = -r; dz <= r; dz++) {
						var chunk = level.getChunk(HALL.x + dx, HALL.z + dz, ChunkStatus.STRUCTURE_STARTS, true);
						for (var entry : chunk.getAllStarts().entrySet()) {
							StructureStart start = entry.getValue();
							if (!start.isValid()) {
								continue;
							}
							String id = String.valueOf(registry.getKey(entry.getKey()));
							if (id.equals("iceandfire_fabric:dragon_hall") && start.getChunkPos().equals(HALL)) {
								hall = start;
							} else {
								others.add(start);
							}
							log("start " + id + " at chunk " + start.getChunkPos().x + " " + start.getChunkPos().z);
						}
					}
				}
				if (hall == null) {
					problem[0] = "the dragon hall of that world is not made at its chunk any more";
					return;
				}
				List<String> into = new ArrayList<>();
				for (StructureStart other : others) {
					String id = String.valueOf(registry.getKey(other.getStructure()));
					StructureType<?> type = other.getStructure().type();
					if (type == StructureType.MINESHAFT || type == StructureType.STRONGHOLD || type == StructureType.BURIED_TREASURE) {
						continue;
					}
					if (id.equals("minecraft:trial_chambers") && other.getChunkPos().equals(TRIAL) || id.startsWith("minecraft:village") && other.getChunkPos().equals(VILLAGE)) {
						into.add(id + " (the one that was cut) at chunk " + other.getChunkPos().x + " " + other.getChunkPos().z);
						continue;
					}
					if (StructureRoom.ours(other.getStructure())) {
						continue;
					}
					for (StructurePiece piece : other.getPieces()) {
						for (StructurePiece part : hall.getPieces()) {
							if (piece.getBoundingBox().intersects(part.getBoundingBox().inflatedBy(3))) {
								into.add(id + " at chunk " + other.getChunkPos().x + " " + other.getChunkPos().z + " reaches into the hall's "
									+ part.getClass().getSimpleName());
							}
						}
					}
				}
				if (!into.isEmpty()) {
					problem[0] = "structures still in the hall's place: " + into;
					return;
				}
				for (StructurePiece part : hall.getPieces()) {
					if (part instanceof DragonHallPieces.Castle castle) {
						found[0] = castle;
					} else if (part instanceof DragonHallPieces.Arena arena) {
						found[1] = arena;
					}
				}
				log("the hall stands alone: the trial chamber and the snowy village that it cut gave way, no other structure reaches into it");
			});
			if (problem[0] != null) {
				throw new AssertionError(problem[0]);
			}
			// /locate does not lead to the trial chamber that gave way
			String located = run(world, "execute positioned " + TRIAL.getMiddleBlockX() + " 0 " + TRIAL.getMiddleBlockZ() + " run locate structure minecraft:trial_chambers");
			log("locate trial chambers there: " + located);
			if (located.contains("[" + TRIAL.getMiddleBlockX() + ",") && located.contains(", " + TRIAL.getMiddleBlockZ() + "]")) {
				throw new AssertionError("/locate still leads to the trial chamber that gave way: " + located);
			}
			BuiltPiece castle = (BuiltPiece) found[0];
			BuiltPiece arena = (BuiltPiece) found[1];
			int g = castle.gate().getY();
			int floor = g - castle.param();
			// the castle and the arena made whole (their chunks), then nothing hanging in the air in them
			int[] counts = new int[3];
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				for (BuiltPiece piece : new BuiltPiece[]{castle, arena}) {
					BoundingBox box = piece.getBoundingBox();
					for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
						for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
							level.getChunk(cx, cz);
						}
					}
				}
				BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
				for (BuiltPiece piece : new BuiltPiece[]{castle, arena}) {
					for (BlockPos pos : BlockPos.betweenClosed(piece.getBoundingBox().minX(), piece.getBoundingBox().minY(), piece.getBoundingBox().minZ(),
						piece.getBoundingBox().maxX(), piece.getBoundingBox().maxY(), piece.getBoundingBox().maxZ())) {
						var state = level.getBlockState(pos);
						if (state.isAir()) {
							continue;
						}
						below.set(pos.getX(), pos.getY() - 1, pos.getZ());
						boolean firm = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
						if (state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.CAMPFIRE)) {
							counts[0]++;
							if (!firm) {
								counts[1]++;
								log("brazier floating at " + pos.toShortString() + " over " + level.getBlockState(below));
							}
						} else if (!firm && !state.canSurvive(level, pos)) {
							counts[2]++;
							if (counts[2] <= 10) {
								log("hanging " + state + " at " + pos.toShortString() + " over " + level.getBlockState(below));
							}
						}
					}
				}
			});
			log("braziers " + counts[0] + ", floating " + counts[1] + "; things hanging in the air " + counts[2]);
			if (counts[1] > 0 || counts[2] > 0) {
				throw new AssertionError("in the hall " + counts[1] + " braziers float and " + counts[2] + " things hang in the air");
			}
			// the castle from the side the village stood on, from high up; the arena's braziers round the hoard
			BlockPos village = new BlockPos(VILLAGE.getMiddleBlockX(), g, VILLAGE.getMiddleBlockZ());
			BlockPos middle = castle.at(0, DragonHallPieces.CASTLE_END / 2, g);
			double dx = middle.getX() - village.getX();
			double dz = middle.getZ() - village.getZ();
			float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
			go(context, world, village.getX() + 0.5 - dx * 0.4, g + 34, village.getZ() + 0.5 - dz * 0.4, yaw, 30, 200);
			context.takeScreenshot("room_hall_where_the_village_was");
			world.getServer().runCommand("gamemode spectator @p");
			look(context, world, castle, 0, DragonHallPieces.HOARD_V - 34, floor + 8, 0, 8, 120);
			context.takeScreenshot("room_hall_arena_braziers");
			world.getServer().runCommand("gamemode creative @p");
			// the pixie cavern in the snow: nothing hangs from the air where its tunnel is open to the sky
			Object[] cave = new Object[1];
			int[] hanging = new int[2];
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
				var start = level.getChunk(CAVERN.x, CAVERN.z, ChunkStatus.STRUCTURE_STARTS, true)
					.getStartForStructure(registry.get(net.minecraft.resources.ResourceLocation.parse("iceandfire_fabric:pixie_cavern")));
				if (start == null || !start.isValid()) {
					return;
				}
				var piece = (com.iafabric.world.structure.PixieCavernPiece) start.getPieces().getFirst();
				cave[0] = piece;
				BoundingBox box = piece.getBoundingBox();
				for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
					for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
						level.getChunk(cx, cz);
					}
				}
				BlockPos.MutableBlockPos below = new BlockPos.MutableBlockPos();
				for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
					var state = level.getBlockState(pos);
					if (state.isAir()) {
						continue;
					}
					below.set(pos.getX(), pos.getY() - 1, pos.getZ());
					if (!state.canSurvive(level, pos) && !level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
						if (hanging[0]++ < 10) {
							log("hanging in the pixie cavern: " + state + " at " + pos.toShortString());
						}
					}
				}
				BlockPos mouth = piece.at(0, piece.end() - 8, 0);
				hanging[1] = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mouth.getX(), mouth.getZ());
			});
			if (cave[0] == null) {
				throw new AssertionError("the pixie cavern of that world is not made at its chunk any more");
			}
			log("pixie cavern in the snow: things hanging in the air " + hanging[0]);
			var cavern = (com.iafabric.world.structure.PixieCavernPiece) cave[0];
			look(context, world, cavern, 0, cavern.end() + 6, hanging[1] + 10, 180, 40, 80);
			context.takeScreenshot("room_pixie_cavern_mouth");
			if (hanging[0] > 0) {
				throw new AssertionError(hanging[0] + " things hang in the air in the pixie cavern");
			}
		}
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

	/** Goes to a place in a building's frame, looking along its +v (plus a turn). */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, BuiltPiece piece, int u, int v, int y, float turn, float pitch, int wait) {
		BlockPos at = piece.at(u, v, y);
		float yaw = switch (piece.facing() & 3) {
			case 0 -> 0F;
			case 1 -> 90F;
			case 2 -> 180F;
			default -> -90F;
		};
		go(context, world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw + turn, pitch, wait);
	}
}
