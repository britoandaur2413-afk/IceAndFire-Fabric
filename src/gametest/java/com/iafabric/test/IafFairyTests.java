package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.client.gui.ExiledPixieHud;
import com.iafabric.client.gui.GuiExiledPixie;
import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessageExiledAction;
import com.iafabric.pixie.ExiledPixieData;
import com.iafabric.pixie.ExiledPixies;
import com.iafabric.pixie.ExiledQuest;
import com.iafabric.pixie.ExiledTalk;
import com.iafabric.pixie.ExiledVariants;
import com.iafabric.world.structure.WitchCottagePiece;
import com.mojang.blaze3d.platform.InputConstants;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;

import java.util.Set;

/**
 * The exiled pixie as a player meets her (IAF_ONLY=fairy): the first tree cut, her coming out angry, her menu the
 * first time and after (in Spanish and English), the witches' cottage and its cakes (a joke about witches on the
 * way), her offer, a quest she leads to (a dragon skeleton she remembers), her friends gone, a pixie bonded, what she
 * says of creatures near, her memory of a hidden chest, and the twenty of them in a row with their names.
 */
public class IafFairyTests implements FabricClientGameTest {
	private static final int GROUND = -60;
	private static final BlockPos TREE = new BlockPos(0, GROUND, 5);

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFFAIRY] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("fairy")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings ->
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL)).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.runOnClient(mc -> {
				// (a scene before may have hidden it)
				if (mc.options.hideGui) {
					mc.options.hideGui = !mc.options.hideGui;
				}
			});
			language(context, "es_es");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				player.teleportTo(server.overworld(), 0.5, GROUND, 0.5, Set.of(), 0, 10);
				ServerLevel level = server.overworld();
				// a tree before the player: five logs, leaves grown about its top
				for (int y = 0; y < 5; y++) {
					level.setBlock(TREE.above(y), Blocks.OAK_LOG.defaultBlockState(), 2);
				}
				for (int x = -2; x <= 2; x++) {
					for (int z = -2; z <= 2; z++) {
						for (int y = 3; y <= 5; y++) {
							BlockPos at = TREE.offset(x, y, z);
							if ((x != 0 || z != 0 || y > 4) && Math.abs(x) + Math.abs(z) < 4) {
								level.setBlock(at, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1), 2);
							}
						}
					}
				}
			});
			context.waitTicks(30);
			meet(context, world);
			menus(context, world);
			cake(context, world);
			quests(context, world);
			friends(context, world);
			memory(context, world);
			vision(context, world);
			dread(context, world);
			lineup(context, world);
			language(context, "en_us");
		}
	}

	private static void language(ClientGameTestContext context, String code) {
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected(code);
			mc.options.languageCode = code;
			mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
		});
		context.waitTicks(3);
	}

	private static ExiledPixieData data(TestSingleplayerContext world) {
		ExiledPixieData[] data = new ExiledPixieData[1];
		world.getServer().runOnServer(server -> data[0] = ExiledPixies.get(server.getPlayerList().getPlayers().get(0)));
		return data[0];
	}

	/** The first tree cut: she comes out of it, angry, and the player must take care of her. */
	private static void meet(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		context.waitTicks(3);
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).gameMode.destroyBlock(TREE));
		context.waitTicks(14);
		context.takeScreenshot("fairy_meet_1");
		ExiledPixieData data = data(world);
		log("the first tree cut: stage " + data.stage + ", she is " + ExiledVariants.name(Math.max(0, data.variant)));
		if (data.stage != ExiledPixieData.ANGRY) {
			throw new AssertionError("cutting the first tree did not free its pixie");
		}
		context.waitTicks(56);
		context.takeScreenshot("fairy_meet_2");
		context.waitTicks(80);
		context.takeScreenshot("fairy_meet_hint");
		// close: her, angry before the player's eyes, glowing
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(10);
		context.takeScreenshot("fairy_meet_close");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		context.waitTicks(2);
	}

	/** Her menu: the first time (her complaint, "I'll take care of you" clicked for real), then its three tabs. */
	private static void menus(ClientGameTestContext context, TestSingleplayerContext world) {
		// J opens it
		context.getInput().pressKey(InputConstants.getKey("key.keyboard.j"));
		context.waitTicks(10);
		boolean open = context.computeOnClient(mc -> mc.screen instanceof GuiExiledPixie);
		log("J opens her menu: " + open);
		if (!open) {
			throw new AssertionError("J did not open the exiled pixie's menu");
		}
		context.takeScreenshot("fairy_menu_intro_es");
		language(context, "en_us");
		context.takeScreenshot("fairy_menu_intro");
		language(context, "es_es");
		double[] at = context.computeOnClient(mc -> {
			double[] gui = ((GuiExiledPixie) mc.screen).button(Component.translatable("gui.iceandfire_fabric.exiled.adopt").getString());
			double scale = mc.getWindow().getGuiScale();
			return gui == null ? null : new double[]{gui[0] * scale, gui[1] * scale};
		});
		if (at == null) {
			throw new AssertionError("no \"I'll take care of you\" button the first time");
		}
		context.getInput().setCursorPos(at[0], at[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(20);
		ExiledPixieData data = data(world);
		log("clicked \"I'll take care of you\": stage " + data.stage);
		if (data.stage != ExiledPixieData.FRIEND) {
			throw new AssertionError("the click did not make her the player's");
		}
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(10);
		context.takeScreenshot("fairy_menu_talk_es");
		context.runOnClient(mc -> ((GuiExiledPixie) mc.screen).show(1, ExiledQuest.BONES.ordinal()));
		context.waitTicks(3);
		context.takeScreenshot("fairy_menu_quests_es");
		context.runOnClient(mc -> ((GuiExiledPixie) mc.screen).show(2, -1));
		context.waitTicks(3);
		context.takeScreenshot("fairy_menu_gifts_es");
		language(context, "en_us");
		context.runOnClient(mc -> ((GuiExiledPixie) mc.screen).show(1, ExiledQuest.CYCLOPS.ordinal()));
		context.waitTicks(3);
		context.takeScreenshot("fairy_menu_quests");
		context.runOnClient(mc -> ((GuiExiledPixie) mc.screen).show(2, -1));
		context.waitTicks(3);
		context.takeScreenshot("fairy_menu_gifts");
		language(context, "es_es");
		context.runOnClient(mc -> mc.setScreen(null));
		context.waitTicks(25);
		context.takeScreenshot("fairy_adopted");
		context.waitTicks(200);
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(10);
		context.takeScreenshot("fairy_shoulder");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		context.waitTicks(2);
		context.takeScreenshot("fairy_first_person");
	}

	/** Her offer, then the dragon bones: she remembers a skeleton, leads the way, waits over it; four bones and it is done. */
	private static void quests(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ExiledPixieData data = ExiledPixies.get(player);
			data.nextOffer = 0;
			player.setAttached(ExiledPixies.DATA, data);
		});
		context.waitTicks(30);
		context.runOnClient(mc -> mc.player.setXRot(0));
		context.waitTicks(20);
		context.takeScreenshot("fairy_offer");
		ExiledPixieData offered = data(world);
		log("her offer: " + offered.offer());
		// (the cake done by her magic, she offers what comes after it: her friends)
		if (offered.offer() != ExiledQuest.FRIENDS) {
			throw new AssertionError("after the cake she does not offer her friends: " + offered.offer());
		}
		context.runOnClient(mc -> ExiledPixieHud.clear());
		world.getServer().runOnServer(server -> ExiledPixies.act(server.getPlayerList().getPlayers().get(0), MessageExiledAction.ACCEPT, ExiledQuest.BONES.ordinal()));
		context.waitTicks(50);
		ExiledPixieData data = data(world);
		log("the bones taken up: the skeleton she remembers is at " + (data.target == null ? "nowhere" : data.target.toShortString()));
		if (data.target == null) {
			throw new AssertionError("she remembered no skeleton for the bones");
		}
		BlockPos target = data.target;
		// toward it: she flies ahead, the screen shows the way
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			float yaw = (float) Math.toDegrees(Math.atan2(-(target.getX() + 0.5 - player.getX()), target.getZ() + 0.5 - player.getZ()));
			player.teleportTo(server.overworld(), player.getX(), GROUND, player.getZ(), Set.of(), yaw + 25, 5);
		});
		context.waitTicks(40);
		context.takeScreenshot("fairy_quest_bones_far");
		// near it: the skeleton is made, she waits over it
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			double dx = target.getX() + 0.5 - player.getX();
			double dz = target.getZ() + 0.5 - player.getZ();
			double d = Math.sqrt(dx * dx + dz * dz);
			double x = target.getX() + 0.5 - dx / d * 14;
			double z = target.getZ() + 0.5 - dz / d * 14;
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			player.teleportTo(server.overworld(), x, GROUND, z, Set.of(), yaw, 12);
		});
		boolean made = false;
		for (int i = 0; i < 20 && !made; i++) {
			context.waitTicks(10);
			boolean[] found = {false};
			world.getServer().runOnServer(server -> found[0] = !server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityDragonBase.class,
				new net.minecraft.world.phys.AABB(target).inflate(30, 60, 30), d -> d.isModelDead()).isEmpty());
			made = found[0];
		}
		log("the skeleton made when the player came near: " + made);
		if (!made) {
			throw new AssertionError("the skeleton she remembered was not made when the player came near");
		}
		context.waitTicks(60);
		BlockPos[] ring = new BlockPos[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityExiledPixie pixie = ExiledPixies.living(player);
			ExiledPixieData now = ExiledPixies.get(player);
			ring[0] = now.checkpoint;
			log("near the skeleton: she is at " + (pixie == null ? "nowhere" : pixie.blockPosition().toShortString()) + ", the quest's place "
				+ (now.target == null ? "none" : now.target.toShortString()) + ", her ring " + (ring[0] == null ? "none" : ring[0].toShortString())
				+ ", the player at " + player.blockPosition().toShortString());
		});
		if (ring[0] == null) {
			throw new AssertionError("she made no ring by the skeleton");
		}
		context.takeScreenshot("fairy_ring");
		context.getInput().pressKey(InputConstants.getKey("key.keyboard.j"));
		context.waitTicks(10);
		context.runOnClient(mc -> {
			if (mc.screen instanceof GuiExiledPixie menu) {
				menu.show(1, ExiledQuest.BONES.ordinal());
			}
		});
		context.waitTicks(3);
		context.takeScreenshot("fairy_menu_active_es");
		context.runOnClient(mc -> mc.setScreen(null));
		context.waitTicks(5);
		// into her ring: the place reached
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			double dx = target.getX() + 0.5 - (ring[0].getX() + 0.5);
			double dz = target.getZ() + 0.5 - (ring[0].getZ() + 0.5);
			double d = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			player.teleportTo(server.overworld(), ring[0].getX() + 0.5 - dx / d * 1.6, ring[0].getY(), ring[0].getZ() + 0.5 - dz / d * 1.6, Set.of(), yaw, 18);
		});
		context.waitTicks(30);
		context.takeScreenshot("fairy_ring_reached");
		ExiledPixieData reached = data(world);
		log("in her ring: the place reached " + ((reached.progress & 1) != 0));
		if ((reached.progress & 1) == 0) {
			throw new AssertionError("standing in her ring did not reach the place");
		}
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).getInventory().add(new ItemStack(IafItemRegistry.DRAGON_BONE, 4)));
		context.waitTicks(30);
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		context.waitTicks(5);
		context.takeScreenshot("fairy_bones_done");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		ExiledPixieData done = data(world);
		boolean[] quiet = {false};
		world.getServer().runOnServer(server -> {
			var state = server.overworld().getBlockState(ring[0]);
			quiet[0] = state.is(com.iafabric.block.IafBlockRegistry.EXILED_LANTERN) && !state.getValue(com.iafabric.block.BlockExiledLantern.LIT);
		});
		log("four bones: the quest done " + done.isDone(ExiledQuest.BONES) + ", her bond " + done.level + " (" + done.xp + "), her ring quiet " + quiet[0]);
		if (!done.isDone(ExiledQuest.BONES)) {
			throw new AssertionError("four dragon bones did not finish her quest");
		}
		if (!quiet[0]) {
			throw new AssertionError("her ring is still lit after its quest was done");
		}
		mineThrough(context, world);
	}

	/**
	 * What is only in the way does not stop a swing: with her hovering before her player (as over her lantern), and with
	 * a dragon's dead body there, the block behind is mined all the same.
	 */
	private static void mineThrough(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> ExiledPixieHud.clear());
		for (String what : new String[]{"her", "a dragon's body"}) {
			BlockPos[] block = new BlockPos[1];
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ServerLevel level = server.overworld();
				// a clear stretch, the player in it looking south
				BlockPos at = new BlockPos(300, GROUND, 300);
				for (BlockPos p : BlockPos.betweenClosed(at.offset(-8, 0, -4), at.offset(8, 8, 14))) {
					level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
				}
				player.teleportTo(level, 300.5, GROUND, 300.5, Set.of(), 0F, 0F);
			});
			// (she comes along to her player's shoulder first)
			context.waitTicks(40);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ServerLevel level = server.overworld();
				if (what.equals("her")) {
					ExiledPixies.living(player).focus(new net.minecraft.world.phys.Vec3(300.5, GROUND + 1.3, 302.3), 400);
				} else {
					ExiledPixies.living(player).focus(null, 0);
					var dragon = IafEntityRegistry.ICE_DRAGON.create(level);
					dragon.growDragon(60);
					dragon.moveTo(300.5, GROUND, 302.6, 90F, 0F);
					dragon.setNoAi(true);
					level.addFreshEntity(dragon);
					dragon.setModelDead(true);
				}
			});
			context.waitTicks(40);
			// the player looks right at it, and a block of dirt is set behind it on that line (within reach)
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ServerLevel level = server.overworld();
				net.minecraft.world.entity.Entity inTheWay = what.equals("her") ? ExiledPixies.living(player)
					: level.getEntitiesOfClass(com.iafabric.entity.EntityDragonBase.class, player.getBoundingBox().inflate(10)).get(0);
				net.minecraft.world.phys.Vec3 eyes = player.getEyePosition();
				net.minecraft.world.phys.Vec3 aim = inTheWay.getBoundingBox().getCenter().subtract(eyes).normalize();
				float yaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
				float pitch = (float) -Math.toDegrees(Math.asin(aim.y));
				player.teleportTo(level, player.getX(), player.getY(), player.getZ(), Set.of(), yaw, pitch);
				// (as far along the line as is still in reach, past it)
				block[0] = BlockPos.containing(eyes.add(aim.scale(net.minecraft.client.Minecraft.getInstance().gameMode.getPickRange() - 0.6)));
				level.setBlock(block[0], Blocks.DIRT.defaultBlockState(), 3);
			});
			context.waitTicks(5);
			BlockPos[] onLine = new BlockPos[1];
			// (aimed again on the client, at where the client has it: this harness does not step the client and the
			// server together as 26.x's did, so the client's may lag a little)
			context.runOnClient(mc -> {
				var near = mc.player.getBoundingBox().inflate(10);
				java.util.List<? extends net.minecraft.world.entity.Entity> seen = what.equals("her")
					? mc.level.getEntitiesOfClass(com.iafabric.entity.EntityExiledPixie.class, near)
					: mc.level.getEntitiesOfClass(com.iafabric.entity.EntityDragonBase.class, near);
				if (!seen.isEmpty()) {
					var aim = seen.get(0).getBoundingBox().getCenter().subtract(mc.player.getEyePosition()).normalize();
					mc.player.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
					mc.player.setXRot((float) -Math.toDegrees(Math.asin(aim.y)));
					// (the block behind on the line as the client aims: she may have moved since the server's)
					onLine[0] = BlockPos.containing(mc.player.getEyePosition().add(aim.scale(mc.gameMode.getPickRange() - 0.6)));
				}
			});
			world.getServer().runOnServer(server -> {
				if (onLine[0] != null && !onLine[0].equals(block[0])) {
					server.overworld().setBlock(block[0], Blocks.AIR.defaultBlockState(), 3);
					block[0] = onLine[0];
					server.overworld().setBlock(block[0], Blocks.DIRT.defaultBlockState(), 3);
				}
			});
			context.waitTicks(2);
			String crosshair = context.computeOnClient(mc -> mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
				? hit.getEntity().getType().toShortString() : String.valueOf(mc.hitResult == null ? null : mc.hitResult.getType()));
			log("crosshair check: " + context.computeOnClient(mc -> {
				var near = mc.player.getBoundingBox().inflate(10);
				var pixies = mc.level.getEntitiesOfClass(com.iafabric.entity.EntityExiledPixie.class, near);
				return pixies.isEmpty() ? "no pixie near on the client (player at " + mc.player.position() + "; all: "
					+ java.util.stream.StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false).filter(e -> e instanceof com.iafabric.entity.EntityPixie)
						.map(e -> e.getClass().getSimpleName() + "#" + e.getId() + "@" + e.position() + " removed " + e.isRemoved()).toList() + ")" : "pixie at " + Math.round(pixies.get(0).getBoundingBox().getCenter().distanceTo(mc.player.getEyePosition()) * 100) / 100.0
					+ " pickable " + pixies.get(0).isPickable() + " box " + pixies.get(0).getBoundingBox() + " entity range " + 3.0
					+ " block range " + mc.gameMode.getPickRange() + " gamemode " + mc.gameMode.getPlayerMode()
					+ ", mouse grabbed " + mc.mouseHandler.isMouseGrabbed() + ", window focused " + mc.isWindowActive()
					+ ", behind " + mc.player.pick(mc.gameMode.getPickRange(), 1.0F, false).getLocation() + " / the block " + block[0];
			}));
			context.takeScreenshot("fairy_mine_through_" + (what.equals("her") ? "her" : "body"));
			context.getInput().holdMouseFor(InputConstants.MOUSE_BUTTON_LEFT, 50);
			context.waitTicks(5);
			boolean[] mined = {false};
			world.getServer().runOnServer(server -> {
				mined[0] = server.overworld().getBlockState(block[0]).isAir();
				for (var dragon : server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityDragonBase.class,
					new net.minecraft.world.phys.AABB(block[0]).inflate(12))) {
					dragon.discard();
				}
				ExiledPixies.living(server.getPlayerList().getPlayers().get(0)).focus(null, 0);
			});
			log("a swing with " + what + " in the way (the crosshair on " + crosshair + "): the block behind mined " + mined[0]);
			if (crosshair.equals("BLOCK") || crosshair.equals("MISS")) {
				throw new AssertionError("the test is wrong: " + what + " was not in the way of the crosshair (" + crosshair + ")");
			}
			if (!mined[0]) {
				throw new AssertionError("with " + what + " in the way the block behind was not mined");
			}
		}
	}

	/**
	 * The cake (the village visited: there are none in this flat world): the witches' cottage, her checkpoint toward
	 * it, a joke about witches on the way, her ring by the hut, the chest she shows, full of cakes; one taken, done.
	 */
	private static void cake(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> ExiledPixieHud.clear());
		BlockPos[] chest = new BlockPos[1];
		BlockPos[] middle = new BlockPos[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = server.overworld();
			// the cottage, its door to the south (facing north from its gate)
			WitchCottagePiece cottage = new WitchCottagePiece(0, GROUND, -40, 2, 77L);
			cottage.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), cottage.getBoundingBox(),
				new net.minecraft.world.level.ChunkPos(0, -3), cottage.gate());
			chest[0] = cottage.chest();
			middle[0] = cottage.at(0, WitchCottagePiece.MIDDLE, GROUND);
			ExiledPixieData data = ExiledPixies.get(player);
			data.done |= ExiledQuest.VILLAGE.bit();
			data.quest = ExiledQuest.CAKE.ordinal();
			data.offered = -1;
			data.nextOffer = Long.MAX_VALUE / 2;
			data.target = new BlockPos(middle[0].getX(), 0, middle[0].getZ());
			data.targetLevel = level.dimension().location().toString();
			player.setAttached(ExiledPixies.DATA, data);
			player.teleportTo(level, 0.5, GROUND, 40.5, Set.of(), 180, 0);
			ExiledPixies.say(player, Component.translatable("exiled.iceandfire_fabric.quest.cake.start"), com.iafabric.message.MessageExiledSpeech.QUEST);
		});
		context.waitTicks(40);
		context.takeScreenshot("fairy_cake_pin");
		// on the way: a joke about witches
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ExiledPixieData data = ExiledPixies.get(player);
			long later = player.level().getGameTime() + 100000;
			ExiledTalk.trip(player, data, later);
			ExiledTalk.trip(player, data, later + 3000);
		});
		context.waitTicks(40);
		context.takeScreenshot("fairy_witch_joke");
		// near the hut: her ring
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(server.overworld(), 0.5, GROUND, -12.5, Set.of(), 180, 8);
		});
		context.waitTicks(60);
		context.takeScreenshot("fairy_cottage");
		BlockPos ring = data(world).checkpoint;
		log("the witches' cottage at " + middle[0].toShortString() + ": her ring " + (ring == null ? "none" : ring.toShortString()));
		if (ring == null) {
			throw new AssertionError("she made no ring at the witches' cottage");
		}
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			double dx = middle[0].getX() + 0.5 - (ring.getX() + 0.5);
			double dz = middle[0].getZ() + 0.5 - (ring.getZ() + 0.5);
			float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
			player.teleportTo(server.overworld(), ring.getX() + 0.5, ring.getY(), ring.getZ() + 1.4, Set.of(), yaw, 10);
		});
		context.waitTicks(50);
		context.takeScreenshot("fairy_cake_here");
		BlockPos shown = context.computeOnClient(mc -> com.iafabric.message.MessageExiledSync.extra.contains("Hint") ? BlockPos.of(com.iafabric.message.MessageExiledSync.extra.getLong("Hint")) : null);
		log("in her ring by the cottage: the chest she shows " + (shown == null ? "none" : shown.toShortString()) + " (the witches' at " + chest[0].toShortString() + ")");
		if (!chest[0].equals(shown)) {
			throw new AssertionError("she did not show the witches' chest");
		}
		// inside: the room, then their chest opened
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(server.overworld(), middle[0].getX() + 0.5, GROUND + 1, middle[0].getZ() + 1.8, Set.of(), 180, 15);
		});
		context.waitTicks(30);
		context.takeScreenshot("fairy_cottage_inside");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			if (server.overworld().getBlockEntity(chest[0]) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity box) {
				player.openMenu(box);
			}
		});
		context.waitTicks(20);
		context.takeScreenshot("fairy_cake_chest");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.closeContainer();
			if (server.overworld().getBlockEntity(chest[0]) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity box) {
				for (int i = 0; i < box.getContainerSize(); i++) {
					if (box.getItem(i).is(net.minecraft.world.item.Items.CAKE)) {
						player.getInventory().add(box.removeItem(i, 1));
						break;
					}
				}
			}
		});
		context.waitTicks(40);
		context.takeScreenshot("fairy_cake_done");
		boolean done = data(world).isDone(ExiledQuest.CAKE);
		log("a cake from the witches' chest: the quest done " + done);
		if (!done) {
			throw new AssertionError("a cake from the witches' chest did not finish the quest");
		}
		context.waitTicks(30);
	}

	/**
	 * Her friends' sanctuary: they are gone (a sad word), the young pixies look at her oddly, a cake makes a friend; a
	 * pixie bonded and her wink; then a word about the creatures near (a few cows, a cyclops).
	 */
	private static void friends(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> ExiledPixieHud.clear());
		int[] wild = new int[3];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = server.overworld();
			for (int i = 0; i < wild.length; i++) {
				com.iafabric.entity.EntityPixie pixie = IafEntityRegistry.PIXIE.create(level);
				pixie.setColor(i);
				pixie.setNoAi(true);
				pixie.moveTo(-60.5 + i * 2, GROUND + 1.5, -70.5, 0, 0);
				pixie.setYHeadRot(0);
				level.addFreshEntity(pixie);
				wild[i] = pixie.getId();
			}
			ExiledPixieData data = ExiledPixies.get(player);
			data.quest = ExiledQuest.FRIENDS.ordinal();
			data.offered = -1;
			data.nextOffer = Long.MAX_VALUE / 2;
			data.progress = 0;
			data.checkpoint = null;
			data.target = new BlockPos(-58, GROUND, -66);
			data.targetLevel = level.dimension().location().toString();
			player.setAttached(ExiledPixies.DATA, data);
			player.teleportTo(level, -58.5, GROUND, -50.5, Set.of(), 180, 5);
		});
		context.waitTicks(50);
		BlockPos ring = data(world).checkpoint;
		log("her friends' place: her ring " + (ring == null ? "none" : ring.toShortString()));
		if (ring == null) {
			throw new AssertionError("she made no ring at her friends' place");
		}
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(server.overworld(), ring.getX() + 0.5, ring.getY(), ring.getZ() + 1.4, Set.of(), 180, 0);
		});
		context.waitTicks(30);
		context.takeScreenshot("fairy_friends_sad");
		context.waitTicks(105);
		context.takeScreenshot("fairy_friends_look");
		context.waitTicks(110);
		context.takeScreenshot("fairy_friends_cake");
		// a cake makes one of them the player's, and the player bonds her: her wink
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			if (server.overworld().getEntity(wild[1]) instanceof com.iafabric.entity.EntityPixie pixie) {
				pixie.tame(player);
			}
		});
		context.waitTicks(40);
		context.takeScreenshot("fairy_friends_done");
		boolean done = data(world).isDone(ExiledQuest.FRIENDS);
		log("a pixie tamed at her friends' place: the quest done " + done);
		if (!done) {
			throw new AssertionError("a pixie of the player's own did not finish her friends' quest");
		}
		context.runOnClient(mc -> ExiledPixieHud.clear());
		boolean[] bonded = {false};
		world.getServer().runOnServer(server -> bonded[0] = com.iafabric.pixie.PixieBonding.bond(server.getPlayerList().getPlayers().get(0), 1));
		context.waitTicks(90);
		context.takeScreenshot("fairy_bond_wink");
		log("a pixie bonded: " + bonded[0]);
		if (!bonded[0]) {
			throw new AssertionError("the pixie tamed could not be bonded");
		}
		// what she says of creatures near: a few cows, then a cyclops
		for (String step : new String[]{"cows", "cyclops"}) {
			context.runOnClient(mc -> ExiledPixieHud.clear());
			boolean[] spoke = {false};
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().get(0);
				ServerLevel level = server.overworld();
				double x = step.equals("cows") ? -100.5 : -140.5;
				player.teleportTo(level, x, GROUND, -100.5, Set.of(), 180, 0);
				for (int i = 0; i < (step.equals("cows") ? 3 : 1); i++) {
					net.minecraft.world.entity.Mob mob = (step.equals("cows") ? net.minecraft.world.entity.EntityType.COW : IafEntityRegistry.CYCLOPS)
						.create(level);
					mob.setNoAi(true);
					mob.moveTo(x - 2 + i * 2.5, GROUND, -108.5 - (step.equals("cows") ? 0 : 4), 0, 0);
					mob.setYHeadRot(0);
					mob.yBodyRot = 0;
					level.addFreshEntity(mob);
				}
			});
			context.waitTicks(10);
			for (int tries = 0; tries < 8 && !spoke[0]; tries++) {
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().get(0);
					spoke[0] = ExiledTalk.sight(player, ExiledPixies.get(player), player.level().getGameTime() + 100000L * (step.equals("cows") ? 1 : 2), true);
				});
			}
			context.waitTicks(40);
			context.takeScreenshot("fairy_sees_" + step);
			log("creatures near (" + step + "): she spoke of them " + spoke[0]);
			if (!spoke[0]) {
				throw new AssertionError("she said nothing of the " + step + " near");
			}
		}
	}

	/**
	 * The right moment: Cryona near with her memories not all back, the pixie warns the player they are not ready (and
	 * why); with all of them back, she calls the player and takes them to her glade; her lines, "close your eyes" (all
	 * goes dark), her gift (the resistance to absolute ice), their eyes opening little by little, and back where they
	 * were. Her last quest, nameless till then, is done. All of it a memory seen as a film, where nothing goes wrong
	 * (memorySafe).
	 */
	/** How many times a quill was heard writing a memory's words. */
	private static final int[] QUILLS = {0};

	private static void vision(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> ExiledPixieHud.clear());
		// (her memories' music heard for real: the test game has its music turned off)
		double music = context.computeOnClient(mc -> mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).get());
		context.runOnClient(mc -> mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(1.0));
		net.minecraft.client.sounds.SoundEventListener quill = (sound, events) -> {
			if (sound.getLocation().toString().equals("iceandfire_fabric:memory_writing")) {
				QUILLS[0]++;
			}
		};
		context.runOnClient(mc -> mc.getSoundManager().addListener(quill));
		int[] queen = {-1};
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = server.overworld();
			player.teleportTo(level, 60.5, GROUND, 60.5, Set.of(), 0, 0);
			// asleep on her throne, a little way off (her call does not reach them there)
			var cryona = com.iafabric.entity.EntityCryonaAsleep.make(level, new net.minecraft.core.BlockPos(60, GROUND, 88), null, 60.5, GROUND, 88.5, 180F,
				MobSpawnType.COMMAND);
			level.addFreshEntity(cryona);
			queen[0] = cryona.getId();
		});
		context.waitTicks(40);
		context.takeScreenshot("fairy_not_ready");
		context.waitTicks(130);
		context.takeScreenshot("fairy_not_ready_why");
		boolean early = context.computeOnClient(mc -> mc.level != null && mc.level.dimension().location().getPath().equals("fairy_glade"));
		log("near Cryona with her memories not all back: taken to her glade " + early);
		if (early) {
			throw new AssertionError("she took the player to her glade before all her memories were back");
		}
		// all her memories back: the right moment (five apples in hand, to throw in it)
		context.runOnClient(mc -> ExiledPixieHud.clear());
		ItemStack[] held = new ItemStack[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			held[0] = player.getMainHandItem().copy();
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.APPLE, 5));
			ExiledPixieData data = ExiledPixies.get(player);
			for (ExiledQuest quest : ExiledQuest.ordered()) {
				if (quest.offered()) {
					data.done |= quest.bit();
				}
			}
			player.setAttached(ExiledPixies.DATA, data);
		});
		// her call: the screen framed as a film, her words its subtitles; their eyes close, to open in her glade
		boolean framed = false;
		for (int i = 0; i < 80 && !framed; i++) {
			context.waitTicks(1);
			framed = context.computeOnClient(mc -> com.iafabric.client.gui.MemoryOverlay.active());
		}
		log("her call: the screen framed as a memory " + framed);
		if (!framed) {
			throw new AssertionError("her call did not frame the screen as a memory");
		}
		context.waitTicks(12);
		context.takeScreenshot("fairy_vision_call");
		boolean inGlade = false;
		for (int i = 0; i < 120 && !inGlade; i++) {
			context.waitTicks(1);
			inGlade = context.computeOnClient(mc -> mc.level != null && mc.level.dimension().location().getPath().equals("fairy_glade"));
		}
		log("her call took the player to her glade: " + inGlade);
		if (!inGlade) {
			throw new AssertionError("her call did not take the player to her glade");
		}
		context.waitTicks(25);
		context.takeScreenshot("fairy_glade_arriving");
		context.waitTicks(50);
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		context.waitTicks(3);
		context.takeScreenshot("fairy_glade");
		context.runOnClient(mc -> mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		memorySafe(context, world);
		context.waitTicks(75);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			EntityExiledPixie pixie = ExiledPixies.living(player);
			log("in her glade: the player at " + player.position() + ", she at " + (pixie == null ? "nowhere" : pixie.position() + " in "
				+ pixie.level().dimension().location()));
		});
		context.takeScreenshot("fairy_glade_moment");
		// a quill is heard as her words are written out
		int quills = context.computeOnClient(mc -> QUILLS[0]);
		log("in her memory a quill heard writing her words " + quills + " times");
		if (quills < 1) {
			throw new AssertionError("no quill is heard writing her memory's words");
		}
		// her memories' music plays through it (and the game's own waits)
		int track = context.computeOnClient(mc -> com.iafabric.client.sound.Soundtrack.audible() ? com.iafabric.client.sound.Soundtrack.playing() : 0);
		log("in her memory the soundtrack playing: " + track + " (2 her memories'): " + context.computeOnClient(mc -> com.iafabric.client.sound.Soundtrack.describe()));
		if (track != com.iafabric.boss.Soundtracks.MEMORIES) {
			throw new AssertionError("her memories' music does not play in her memory: " + track);
		}
		// "close your eyes": all goes dark; her gift; their eyes open little by little
		boolean closed = false;
		for (int i = 0; i < 400 && !closed; i++) {
			context.waitTicks(1);
			closed = context.computeOnClient(mc -> com.iafabric.client.gui.EyesOverlay.closed());
		}
		log("her bidding to close their eyes: all dark " + closed);
		if (!closed) {
			throw new AssertionError("the player's eyes did not close at her bidding");
		}
		context.takeScreenshot("fairy_eyes_closed");
		boolean opening = false;
		for (int i = 0; i < 200 && !opening; i++) {
			context.waitTicks(1);
			opening = context.computeOnClient(mc -> com.iafabric.client.gui.EyesOverlay.opening() >= 0.22F);
		}
		context.takeScreenshot("fairy_eyes_opening");
		for (int i = 0; i < 200 && context.computeOnClient(mc -> com.iafabric.client.gui.EyesOverlay.opening() >= 0F && com.iafabric.client.gui.EyesOverlay.opening() < 0.46F); i++) {
			context.waitTicks(1);
		}
		context.takeScreenshot("fairy_eyes_blink");
		boolean open = false;
		for (int i = 0; i < 200 && !open; i++) {
			context.waitTicks(1);
			open = context.computeOnClient(mc -> com.iafabric.client.gui.EyesOverlay.opening() < 0F && !com.iafabric.client.gui.EyesOverlay.closed());
		}
		log("their eyes opening little by little: seen opening " + opening + ", open again " + open);
		if (!opening || !open) {
			throw new AssertionError("the player's eyes did not open again little by little");
		}
		context.waitTicks(5);
		context.takeScreenshot("fairy_glade_gift");
		// back when her words are over (the server's own time: it may run behind the client's)
		boolean back = false;
		for (int i = 0; i < 60 && !back; i++) {
			context.waitTicks(10);
			back = context.computeOnClient(mc -> mc.level != null && mc.level.dimension() == net.minecraft.world.level.Level.OVERWORLD);
		}
		context.waitTicks(15);
		context.takeScreenshot("fairy_vision_back_eyes");
		// the memory over: the screen theirs again (its bars gone, the HUD back), her word, their apples all there
		boolean over = false;
		for (int i = 0; i < 200 && !over; i++) {
			context.waitTicks(1);
			over = context.computeOnClient(mc -> !com.iafabric.client.gui.MemoryOverlay.active());
		}
		context.waitTicks(25);
		ExiledPixieData data = data(world);
		context.takeScreenshot("fairy_vision_back");
		int after = context.computeOnClient(mc -> com.iafabric.client.sound.Soundtrack.playing());
		context.runOnClient(mc -> mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(music));
		context.runOnClient(mc -> mc.getSoundManager().removeListener(quill));
		if (after != com.iafabric.boss.Soundtracks.NONE) {
			throw new AssertionError("her memories' music goes on after her memory: " + after);
		}
		int apples = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getInventory()
			.countItem(net.minecraft.world.item.Items.APPLE));
		boolean shown = context.computeOnClient(mc -> !com.iafabric.client.gui.MemoryOverlay.showing() && mc.options.getCameraType().isFirstPerson());
		log("back from her glade " + back + ", her gift " + data.hasGift(ExiledPixieData.GIFT_ICE) + ", her last quest done " + data.isDone(ExiledQuest.CRYONA)
			+ ", the memory over " + over + ", the screen theirs again " + shown + ", apples " + apples + " of 5");
		if (!back || !data.hasGift(ExiledPixieData.GIFT_ICE) || !data.isDone(ExiledQuest.CRYONA)) {
			throw new AssertionError("the right moment did not give her gift and bring the player back");
		}
		if (!over || !shown || apples != 5) {
			throw new AssertionError("the memory did not end well: over " + over + ", screen back " + shown + ", apples " + apples);
		}
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held[0]));
		world.getServer().runOnServer(server -> {
			var entity = server.overworld().getEntity(queen[0]);
			if (entity != null) {
				entity.discard();
			}
		});
		// her menu now: her last quest named, her gift among her gifts
		context.getInput().pressKey(InputConstants.getKey("key.keyboard.j"));
		context.waitTicks(10);
		context.runOnClient(mc -> {
			if (mc.screen instanceof GuiExiledPixie menu) {
				menu.show(1, ExiledQuest.CRYONA.ordinal());
			}
		});
		context.waitTicks(3);
		context.takeScreenshot("fairy_menu_moment_es");
		context.runOnClient(mc -> {
			if (mc.screen instanceof GuiExiledPixie menu) {
				menu.show(2, -1);
			}
		});
		context.waitTicks(3);
		context.takeScreenshot("fairy_menu_gift_es");
		context.runOnClient(mc -> mc.setScreen(null));
		context.waitTicks(5);
	}

	/**
	 * Nothing goes wrong in her memory: the player can neither walk, jump nor look away; their pack does not open, what
	 * they throw (by the key, or as a client that does not hold back would) is in their pack again at once; put out
	 * over the void they are back where they stood, and nothing hurts them nor breaks a block of her glade.
	 */
	private static void memorySafe(ClientGameTestContext context, TestSingleplayerContext world) {
		net.minecraft.world.phys.Vec3 at = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).position());
		float yaw = context.computeOnClient(mc -> mc.player.getYRot());
		context.getInput().holdKey(options -> options.keyUp);
		context.getInput().holdKey(options -> options.keyJump);
		for (int i = 0; i < 10; i++) {
			context.getInput().moveCursor(40, 12);
			context.waitTicks(2);
		}
		context.getInput().releaseKey(options -> options.keyUp);
		context.getInput().releaseKey(options -> options.keyJump);
		context.waitTicks(4);
		net.minecraft.world.phys.Vec3 walked = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).position());
		float looked = context.computeOnClient(mc -> mc.player.getYRot());
		context.getInput().pressKey(options -> options.keyInventory);
		context.getInput().pressKey(options -> options.keyDrop);
		context.waitTicks(4);
		boolean packOpen = context.computeOnClient(mc -> mc.screen != null);
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).drop(true));
		context.waitTicks(4);
		int[] kept = world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			int lying = player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, player.getBoundingBox().inflate(64)).size();
			return new int[]{player.getInventory().countItem(net.minecraft.world.item.Items.APPLE), lying};
		});
		float health = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().get(0).getHealth());
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo((ServerLevel) player.level(), at.x + 30, 50, at.z + 24, Set.of(), player.getYRot(), player.getXRot());
		});
		context.waitTicks(4);
		Object[] after = world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = (ServerLevel) player.level();
			boolean hurt = player.hurt(player.damageSources().fellOutOfWorld(), 50F);
			BlockPos ground = BlockPos.containing(at).below();
			boolean broke = player.gameMode.destroyBlock(ground) || !level.getBlockState(ground).is(Blocks.GRASS_BLOCK);
			return new Object[]{player.position(), hurt, broke, player.getHealth()};
		});
		net.minecraft.world.phys.Vec3 held = (net.minecraft.world.phys.Vec3) after[0];
		log("in her memory: walked " + String.format("%.2f", walked.distanceTo(at)) + " blocks, looked away " + String.format("%.1f", Math.abs(looked - yaw))
			+ " degrees, pack open " + packOpen + ", apples " + kept[0] + " of 5 (" + kept[1] + " lying about), put over the void and back "
			+ String.format("%.2f", held.distanceTo(at)) + " blocks from where they stood, hurt " + after[1] + " (health " + health + " -> " + after[3]
			+ "), a block broken " + after[2]);
		if (walked.distanceTo(at) > 0.3 || Math.abs(looked - yaw) > 0.5F || packOpen) {
			throw new AssertionError("the player moved, looked away or opened their pack in her memory");
		}
		if (kept[0] != 5 || kept[1] != 0) {
			throw new AssertionError("what the player threw in her memory was not in their pack again: " + kept[0] + " apples, " + kept[1] + " lying");
		}
		if (held.distanceTo(at) > 0.3 || (boolean) after[1] || (boolean) after[2] || (float) after[3] < health) {
			throw new AssertionError("her memory let the player fall, be hurt or break a block");
		}
		context.takeScreenshot("fairy_glade_safe");
	}

	/** The dread's undead dragons (bare bones, green soul fire) beside Black Frost: nothing alike. */
	private static void dread(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> ExiledPixieHud.clear());
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			for (int i = 0; i < 2; i++) {
				com.iafabric.entity.EntityDragonBase dragon = IafEntityRegistry.ICE_DRAGON.create(level);
				dragon.setAgeInDays(0);
				dragon.growDragon(90);
				dragon.setDreadLook(i == 0 ? 1 : 2);
				dragon.setNoAi(true);
				dragon.moveTo(-160 + i * 16, GROUND, 40, 200, 0);
				dragon.setYHeadRot(200);
				dragon.yBodyRot = 200;
				level.addFreshEntity(dragon);
			}
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(level, -152, GROUND + 4, 18, Set.of(), 0, 10);
		});
		context.waitTicks(40);
		context.runOnClient(mc -> {
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		context.waitTicks(5);
		context.takeScreenshot("dread_dragon_vs_black_frost");
		context.runOnClient(mc -> mc.options.hideGui = !mc.options.hideGui);
	}

	/** Her memory (K): a chest never opened, under the ground nearby, shown by a light; she flies to it. */
	private static void memory(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> ExiledPixieHud.clear());
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			ServerLevel level = server.overworld();
			player.teleportTo(level, 40.5, GROUND, 40.5, Set.of(), 0, 15);
			BlockPos chest = new BlockPos(41, GROUND - 3, 49);
			level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
			if (level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {
				container.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON, 7L);
			}
			ExiledPixieData data = ExiledPixies.get(player);
			data.abilityReady = 0;
			data.nextOffer = Long.MAX_VALUE / 2;
			player.setAttached(ExiledPixies.DATA, data);
		});
		context.waitTicks(30);
		context.getInput().pressKey(InputConstants.getKey("key.keyboard.k"));
		context.waitTicks(40);
		context.takeScreenshot("fairy_memory");
	}

	/** The twenty, in two rows, with their names. */
	private static void lineup(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> ExiledPixieHud.clear());
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			for (int i = 0; i < ExiledVariants.COUNT; i++) {
				EntityExiledPixie pixie = IafEntityRegistry.EXILED_PIXIE.create(level);
				int row = i / 10;
				int col = i % 10;
				pixie.setVariant(i);
				pixie.setNoAi(true);
				pixie.setCustomName(Component.literal(ExiledVariants.name(i)));
				pixie.setCustomNameVisible(true);
				pixie.moveTo(-120 + col * 1.15 - 5.2, GROUND + 1.2 + row * 1.6, 130.5, 180, 0);
				pixie.setYHeadRot(180);
				pixie.yBodyRot = 180;
				level.addFreshEntity(pixie);
			}
			ServerPlayer player = server.getPlayerList().getPlayers().get(0);
			player.teleportTo(level, -120.15, GROUND, 124.4, Set.of(), 0, -3);
		});
		context.waitTicks(30);
		context.runOnClient(mc -> {
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		context.waitTicks(5);
		context.takeScreenshot("fairy_lineup");
		context.runOnClient(mc -> mc.options.hideGui = !mc.options.hideGui);
	}
}
