package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessageExiledAction;
import com.iafabric.pixie.ExiledPixieData;
import com.iafabric.pixie.ExiledPixies;
import com.iafabric.pixie.FairyVision;
import com.iafabric.pixie.ExiledQuest;
import com.iafabric.pixie.ExiledQuests;
import com.iafabric.pixie.ExiledTalk;
import com.iafabric.pixie.ExiledVariants;
import com.iafabric.world.structure.WitchCottagePiece;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The exiled pixie on the server: the first real tree frees her, her twenty skins go round, her quests and gifts. Log lines start with [IAFFAIRY]. */
public class IafFairyGameTests implements FabricGameTest {
	private static void log(String message) {
		IafMod.LOGGER.info("[IAFFAIRY] {}", message);
	}

	/** A player in survival, joined as a real one (with a connection that takes what is sent to it). */
	private static ServerPlayer survivalPlayer(GameTestHelper helper, String name) {
		var server = helper.getLevel().getServer();
		var player = new ServerPlayer(server, helper.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		return player;
	}

	private static void leave(GameTestHelper helper, ServerPlayer player) {
		ExiledPixies.reset(player);
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	/**
	 * The first tree a player cuts (a log with grown leaves about it) frees her, angry; a log they put down with no
	 * leaves does nothing. A word with her and she is theirs; out by them, she gives a share of all the pixies'
	 * gifts by her bond (a tenth of their best at her first level, half at her last).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void firstTreeFreesHer(GameTestHelper helper) {
		// a tree: four logs, leaves grown about the top
		for (int y = 2; y <= 5; y++) {
			helper.setBlock(new BlockPos(3, y, 3), Blocks.OAK_LOG.defaultBlockState());
		}
		for (int x = 1; x <= 5; x++) {
			for (int z = 1; z <= 5; z++) {
				for (int y = 4; y <= 6; y++) {
					BlockPos at = new BlockPos(x, y, z);
					if (!(x == 3 && z == 3 && y <= 5)) {
						helper.setBlock(at, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false).setValue(LeavesBlock.DISTANCE, 1));
					}
				}
			}
		}
		// a lone log put down, far from leaves
		helper.setBlock(new BlockPos(12, 2, 12), Blocks.BIRCH_LOG.defaultBlockState());
		ServerPlayer woodcutter = survivalPlayer(helper, "test-woodcutter");
		ServerPlayer builder = survivalPlayer(helper, "test-builder");
		BlockPos log = helper.absolutePos(new BlockPos(3, 2, 3));
		BlockPos lone = helper.absolutePos(new BlockPos(12, 2, 12));
		woodcutter.moveTo(log.getX() + 0.5, log.getY(), log.getZ() - 2.5, 0, 0);
		builder.moveTo(lone.getX() + 0.5, lone.getY(), lone.getZ() - 2.5, 0, 0);
		woodcutter.gameMode.destroyBlock(log);
		builder.gameMode.destroyBlock(lone);
		ExiledPixieData data = ExiledPixies.get(woodcutter);
		log("the woodcutter's pixie: stage " + data.stage + ", " + ExiledVariants.name(Math.max(0, data.variant)) + "; the builder's: stage "
			+ ExiledPixies.get(builder).stage);
		helper.assertTrue(data.stage == ExiledPixieData.ANGRY, "cutting the first tree did not free its pixie");
		helper.assertTrue(data.variant >= 0 && data.variant < ExiledVariants.COUNT, "her skin is not one of the twenty: " + data.variant);
		helper.assertTrue(!ExiledPixies.get(builder).met(), "a log put down with no leaves freed a pixie");
		helper.assertTrue(helper.getLevel().getBlockState(log).isAir(), "the log was not cut");
		int variant = data.variant;
		helper.runAfterDelay(25, () -> {
			EntityExiledPixie pixie = ExiledPixies.living(woodcutter);
			helper.assertTrue(pixie != null && pixie.isAngry() && pixie.isOwnedBy(woodcutter), "she is not out, angry, with her player");
			helper.assertTrue(pixie.getVariant() == variant, "she is not drawn as the one met");
			helper.assertTrue(pixie.distanceTo(woodcutter) < 8, "she is not by her player: " + pixie.distanceTo(woodcutter));
			// nothing hurts her
			pixie.hurt(helper.getLevel().damageSources().generic(), 100F);
			helper.assertTrue(pixie.isAlive() && pixie.getHealth() == pixie.getMaxHealth(), "she was hurt");
			// a word: she is theirs
			ExiledPixies.act(woodcutter, MessageExiledAction.ADOPT, 0);
			helper.assertTrue(ExiledPixies.get(woodcutter).stage == ExiledPixieData.FRIEND, "a word with her did not make her theirs");
			helper.assertTrue(!pixie.isAngry(), "she is still angry");
		});
		helper.runAfterDelay(50, () -> {
			var attack = woodcutter.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(com.iafabric.compat.Modifiers.uuid(IafMod.id("exiled_pixie_attack_damage")));
			log("her gift at bond 1: attack +" + (attack == null ? 0 : attack.getAmount()));
			helper.assertTrue(attack != null && Math.abs(attack.getAmount() - 0.9) < 1e-6, "her gift at her first level is not a tenth of the strength pixie's best (0.9)");
			ExiledPixieData friend = ExiledPixies.get(woodcutter);
			friend.level = 5;
			woodcutter.setAttached(ExiledPixies.DATA, friend);
		});
		helper.runAfterDelay(80, () -> {
			var attack = woodcutter.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(com.iafabric.compat.Modifiers.uuid(IafMod.id("exiled_pixie_attack_damage")));
			var speed = woodcutter.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(com.iafabric.compat.Modifiers.uuid(IafMod.id("exiled_pixie_movement_speed")));
			log("her gifts at bond 5: attack +" + (attack == null ? 0 : attack.getAmount()) + ", speed +" + (speed == null ? 0 : speed.getAmount()));
			helper.assertTrue(attack != null && Math.abs(attack.getAmount() - 4.5) < 1e-6, "her gift at her last level is not half the strength pixie's best (4.5)");
			helper.assertTrue(speed != null && Math.abs(speed.getAmount() - 0.25) < 1e-6, "her speed gift at her last level is not half the speed pixie's");
			// resting, she gives nothing
			ExiledPixies.act(woodcutter, MessageExiledAction.REST, 0);
			helper.assertTrue(woodcutter.getAttribute(Attributes.ATTACK_DAMAGE).getModifier(com.iafabric.compat.Modifiers.uuid(IafMod.id("exiled_pixie_attack_damage"))) == null,
				"resting, she still gives her gifts");
			helper.assertTrue(ExiledPixies.living(woodcutter) == null, "resting, she is still out");
			leave(helper, woodcutter);
			leave(helper, builder);
			helper.succeed();
		});
	}

	/** The twenty go round: each new world gets one not met in this round until all twenty came; the next round does not begin with the last one. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void twentyGoRound(GameTestHelper helper) {
		try {
			// a fresh round (this is the test game's own file, not a player's)
			java.nio.file.Files.deleteIfExists(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("iceandfire_fabric").resolve("exiled_pixies.json"));
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		Set<Integer> round = new HashSet<>();
		int last = -1;
		for (int i = 0; i < ExiledVariants.COUNT; i++) {
			last = ExiledVariants.next(helper.getLevel().getRandom());
			helper.assertTrue(round.add(last), "one came twice in a round: " + last);
		}
		int next = ExiledVariants.next(helper.getLevel().getRandom());
		log("a whole round: " + round.size() + " different, ending with " + ExiledVariants.name(last) + "; the next round begins with " + ExiledVariants.name(next));
		helper.assertTrue(round.size() == ExiledVariants.COUNT, "a round did not bring all twenty");
		helper.assertTrue(next != last, "a round began with the last one of the round before");
		helper.succeed();
	}

	/**
	 * Her quests: she offers the first open one (the village); the cake waits for the village; what the player already
	 * has is done at once (a cake in the bag); a weapon past dragon bones passes their quest; a cyclops' eye in the bag
	 * is that quest done.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void questsSkipWhatIsPast(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper, "test-adventurer");
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		ExiledPixies.meet(player, at.above(2));
		ExiledPixies.act(player, MessageExiledAction.ADOPT, 0);
		ExiledPixieData data = ExiledPixies.get(player);
		data.nextOffer = 0;
		player.setAttached(ExiledPixies.DATA, data);
		helper.runAfterDelay(25, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			log("her first offer: " + d.offer());
			helper.assertTrue(d.offer() == ExiledQuest.VILLAGE, "her first offer is not the village: " + d.offer());
			// a diamond sword: dragon bones are past
			player.getInventory().add(new ItemStack(Items.DIAMOND_SWORD));
			helper.assertTrue(ExiledQuests.pastBones(player), "a diamond sword does not pass the dragon bones");
		});
		helper.runAfterDelay(50, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			log("passed after a diamond sword: bones " + d.isSkipped(ExiledQuest.BONES));
			helper.assertTrue(d.isSkipped(ExiledQuest.BONES), "the bones quest is still open with a diamond sword");
			// a cake in the bag: the cake waits for the village; then accepting it is done at once, with its experience
			player.getInventory().add(new ItemStack(Items.CAKE));
			ExiledPixies.act(player, MessageExiledAction.ACCEPT, ExiledQuest.CAKE.ordinal());
			ExiledPixieData before = ExiledPixies.get(player);
			helper.assertTrue(before.current() == null && !before.isDone(ExiledQuest.CAKE), "the cake was taken up before the village");
			before.done |= ExiledQuest.VILLAGE.bit();
			player.setAttached(ExiledPixies.DATA, before);
			int xp = before.xp;
			ExiledPixies.act(player, MessageExiledAction.ACCEPT, ExiledQuest.CAKE.ordinal());
			ExiledPixieData after = ExiledPixies.get(player);
			log("the cake accepted with one in the bag: done " + after.isDone(ExiledQuest.CAKE) + ", her experience " + xp + " -> " + after.xp);
			helper.assertTrue(after.isDone(ExiledQuest.CAKE) && after.current() == null, "accepting the cake with one in the bag did not finish it");
			helper.assertTrue(after.xp == xp + ExiledQuest.CAKE.xp, "the cake quest gave no experience");
			// a cyclops' eye: the quest is done the moment it is taken up
			player.getInventory().add(new ItemStack(IafItemRegistry.CYCLOPS_EYE));
			ExiledPixies.act(player, MessageExiledAction.ACCEPT, ExiledQuest.CYCLOPS.ordinal());
			helper.assertTrue(ExiledPixies.get(player).isDone(ExiledQuest.CYCLOPS), "the cyclops' quest with his eye in the bag is not done");
			// a quest done is never offered again
			ExiledPixies.act(player, MessageExiledAction.ACCEPT, ExiledQuest.CAKE.ordinal());
			helper.assertTrue(ExiledPixies.get(player).current() == null, "a quest done was taken up again");
			leave(helper, player);
			helper.succeed();
		});
	}

	/**
	 * Her ring marks a quest's place: made by it once her player is near (her lantern, lit, of her color, in a ring of
	 * lit candles of her color); standing in it is being there (a visit is done), and it grows quiet then.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
	public void ringMarksThePlace(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer player = survivalPlayer(helper, "test-pilgrim");
		BlockPos start = helper.absolutePos(new BlockPos(1, 2, 1));
		player.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
		ExiledPixies.meet(player, start.above(2));
		ExiledPixies.act(player, MessageExiledAction.ADOPT, 0);
		ExiledPixieData data = ExiledPixies.get(player);
		data.quest = ExiledQuest.GRAVEYARD.ordinal();
		data.offered = -1;
		data.nextOffer = Long.MAX_VALUE / 2;
		data.target = helper.absolutePos(new BlockPos(6, 2, 6));
		data.targetLevel = level.dimension().location().toString();
		player.setAttached(ExiledPixies.DATA, data);
		BlockPos[] ring = new BlockPos[1];
		helper.runAfterDelay(50, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			ring[0] = d.checkpoint;
			helper.assertTrue(ring[0] != null, "she made no ring at the quest's place");
			var lantern = level.getBlockState(ring[0]);
			helper.assertTrue(lantern.is(com.iafabric.block.IafBlockRegistry.EXILED_LANTERN), "no lantern where her ring is");
			helper.assertTrue(lantern.getValue(com.iafabric.block.BlockExiledLantern.LIT), "her lantern is not lit");
			helper.assertTrue(lantern.getValue(com.iafabric.block.BlockExiledLantern.VARIANT) == d.variant, "her lantern is not of her color");
			int candles = candles(level, ring[0], d.variant, true);
			log("her ring at " + ring[0].toShortString() + " (" + (int) Math.sqrt(ring[0].distSqr(d.target)) + " blocks from the place): " + candles + " lit candles");
			helper.assertTrue(candles >= 5, "too few lit candles of her color about her lantern: " + candles);
			helper.assertTrue(!d.isDone(ExiledQuest.GRAVEYARD), "the visit was done before reaching her ring");
			player.moveTo(ring[0].getX() + 0.5, ring[0].getY(), ring[0].getZ() + 1.6, 180, 0);
		});
		helper.runAfterDelay(100, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			var lantern = level.getBlockState(ring[0]);
			log("in her ring: the visit done " + d.isDone(ExiledQuest.GRAVEYARD) + ", her lantern lit " + lantern.getValue(com.iafabric.block.BlockExiledLantern.LIT)
				+ ", candles lit " + candles(level, ring[0], d.variant, true));
			helper.assertTrue(d.isDone(ExiledQuest.GRAVEYARD), "standing in her ring did not finish the visit");
			helper.assertTrue(!lantern.getValue(com.iafabric.block.BlockExiledLantern.LIT), "her lantern is still lit after the visit");
			helper.assertTrue(candles(level, ring[0], d.variant, true) == 0, "candles still lit after the visit");
			com.iafabric.pixie.FairyCheckpoints.remove(level, ring[0]);
			helper.assertTrue(level.getBlockState(ring[0]).isAir() && candles(level, ring[0], d.variant, false) == 0, "her ring was not taken away");
			leave(helper, player);
			helper.succeed();
		});
	}

	/** Her candles about a lantern (lit only, or all). */
	private static int candles(net.minecraft.server.level.ServerLevel level, BlockPos lantern, int variant, boolean litOnly) {
		var candle = com.iafabric.pixie.FairyCheckpoints.candle(variant);
		int n = 0;
		for (BlockPos at : BlockPos.betweenClosed(lantern.offset(-3, -1, -3), lantern.offset(3, 1, 3))) {
			var state = level.getBlockState(at);
			if (state.is(candle) && (!litOnly || state.getValue(net.minecraft.world.level.block.CandleBlock.LIT))) {
				n++;
			}
		}
		return n;
	}

	/** A witches' cottage built that many blocks east of the test, facing south (its land loaded and kept). */
	private static WitchCottagePiece cottage(GameTestHelper helper, int east) {
		var level = helper.getLevel();
		BlockPos base = helper.absolutePos(new BlockPos(0, 1, 0)).offset(east, 0, 0);
		for (int cx = (base.getX() - 12) >> 4; cx <= (base.getX() + 12) >> 4; cx++) {
			for (int cz = (base.getZ() - 8) >> 4; cz <= (base.getZ() + 20) >> 4; cz++) {
				level.setChunkForced(cx, cz, true);
				level.getChunk(cx, cz);
			}
		}
		int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
		WitchCottagePiece piece = new WitchCottagePiece(base.getX(), y, base.getZ(), 0, 1234L);
		BoundingBox box = piece.getBoundingBox();
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), box,
			new net.minecraft.world.level.ChunkPos(base.getX() >> 4, base.getZ() >> 4), base);
		return piece;
	}

	private static void unforce(GameTestHelper helper, WitchCottagePiece cottage) {
		BlockPos base = cottage.gate();
		for (int cx = (base.getX() - 12) >> 4; cx <= (base.getX() + 12) >> 4; cx++) {
			for (int cz = (base.getZ() - 8) >> 4; cz <= (base.getZ() + 20) >> 4; cz++) {
				helper.getLevel().setChunkForced(cx, cz, false);
			}
		}
	}

	/** The witches' cottage: its hut (a cauldron, a brewing stand, a bitten cake on a barrel, a hat of a roof) and its chest full of cakes. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void witchCottageHasCakes(GameTestHelper helper) {
		var level = helper.getLevel();
		WitchCottagePiece cottage = cottage(helper, 400);
		int g = cottage.gate().getY();
		ServerPlayer player = survivalPlayer(helper, "test-witch-guest");
		helper.assertTrue(level.getBlockEntity(cottage.chest()) instanceof ChestBlockEntity, "no chest in the witches' cottage");
		ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(cottage.chest());
		chest.unpackLootTable(player);
		int cakes = 0;
		int other = 0;
		for (int i = 0; i < chest.getContainerSize(); i++) {
			ItemStack stack = chest.getItem(i);
			if (stack.is(Items.CAKE)) {
				cakes += stack.getCount();
			} else if (!stack.isEmpty()) {
				other++;
			}
		}
		log("the witches' chest: " + cakes + " cakes and " + other + " other things; cauldron " + level.getBlockState(cottage.at(-2, 7, g + 1)).getBlock()
			+ ", brewing stand " + level.getBlockState(cottage.at(-2, 6, g + 1)).getBlock() + ", on the barrel " + level.getBlockState(cottage.at(2, 6, g + 2)).getBlock()
			+ ", on the hat " + level.getBlockState(cottage.at(0, WitchCottagePiece.MIDDLE, g + 10)).getBlock());
		helper.assertTrue(cakes >= 5, "the witches' chest is not full of cakes: " + cakes);
		helper.assertTrue(level.getBlockState(cottage.at(-2, 7, g + 1)).is(Blocks.WATER_CAULDRON), "no cauldron in the cottage");
		helper.assertTrue(level.getBlockState(cottage.at(-2, 6, g + 1)).is(Blocks.BREWING_STAND), "no brewing stand in the cottage");
		helper.assertTrue(level.getBlockState(cottage.at(2, 6, g + 2)).is(Blocks.CAKE), "no bitten cake on the barrel");
		helper.assertTrue(level.getBlockState(cottage.at(0, WitchCottagePiece.MIDDLE, g + 10)).is(Blocks.SOUL_LANTERN), "no soul lantern at the top of its hat");
		helper.assertTrue(level.getBlockState(cottage.at(0, 2, g + 1)).is(Blocks.DARK_OAK_DOOR), "no door");
		unforce(helper, cottage);
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	/** The cake: her ring at the witches' cottage; there she shows their chest; a cake from it and the quest is done. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void cakeFromTheWitches(GameTestHelper helper) {
		var level = helper.getLevel();
		WitchCottagePiece cottage = cottage(helper, 460);
		int g = cottage.gate().getY();
		BlockPos middle = cottage.at(0, WitchCottagePiece.MIDDLE, g);
		BlockPos start = cottage.at(0, -14, g);
		ServerPlayer player = survivalPlayer(helper, "test-cake-thief");
		player.teleportTo(level, start.getX() + 0.5, start.getY(), start.getZ() + 0.5, Set.of(), 0, 0);
		ExiledPixies.meet(player, start.above(2));
		ExiledPixies.act(player, MessageExiledAction.ADOPT, 0);
		ExiledPixieData data = ExiledPixies.get(player);
		data.done |= ExiledQuest.VILLAGE.bit();
		data.quest = ExiledQuest.CAKE.ordinal();
		data.offered = -1;
		data.nextOffer = Long.MAX_VALUE / 2;
		// where the search finds a structure: its chunk's middle (the hut's middle), at the bottom of the world
		data.target = new BlockPos(middle.getX(), 0, middle.getZ());
		data.targetLevel = level.dimension().location().toString();
		player.setAttached(ExiledPixies.DATA, data);
		BlockPos[] ring = new BlockPos[1];
		helper.runAfterDelay(50, () -> {
			ring[0] = ExiledPixies.get(player).checkpoint;
			log("her ring at the witches' cottage: " + (ring[0] == null ? "none" : ring[0].toShortString() + ", "
				+ (int) Math.sqrt(ring[0].distSqr(middle.atY(ring[0].getY()))) + " blocks from the hut's middle"));
			helper.assertTrue(ring[0] != null, "she made no ring at the witches' cottage");
			player.teleportTo(level, ring[0].getX() + 0.5, ring[0].getY(), ring[0].getZ() + 1.6, Set.of(), 180, 0);
		});
		helper.runAfterDelay(90, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			var extra = ExiledQuests.extra(player, d);
			BlockPos shown = extra.contains("Hint") ? BlockPos.of(extra.getLong("Hint")) : null;
			log("in her ring: there " + ((d.progress & 1) != 0) + ", the chest she shows " + (shown == null ? "none" : shown.toShortString())
				+ " (the witches' chest at " + cottage.chest().toShortString() + ")");
			helper.assertTrue((d.progress & 1) != 0, "her ring at the cottage was not reached");
			helper.assertTrue(cottage.chest().equals(shown), "she did not show the witches' chest");
			ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(cottage.chest());
			chest.unpackLootTable(player);
			for (int i = 0; i < chest.getContainerSize(); i++) {
				if (chest.getItem(i).is(Items.CAKE)) {
					player.getInventory().add(chest.removeItem(i, 1));
					break;
				}
			}
		});
		helper.runAfterDelay(130, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			log("a cake from the witches' chest: the quest done " + d.isDone(ExiledQuest.CAKE));
			helper.assertTrue(d.isDone(ExiledQuest.CAKE), "a cake from the witches' chest did not finish the quest");
			if (ring[0] != null) {
				com.iafabric.pixie.FairyCheckpoints.remove(level, ring[0]);
			}
			unforce(helper, cottage);
			leave(helper, player);
			helper.succeed();
		});
	}

	/**
	 * What catches her eye: the kinds of creatures near (three cows near more than a zombie far; a cyclops, a dread
	 * thrall), the kind of each creature (a bone dragon, a dragon's skeleton, never Black Frost nor herself), and her
	 * lines going round: fifty anecdotes of villagers, none twice before all fifty were told.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void talkFollowsWhatIsNear(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer player = survivalPlayer(helper, "test-gossip");
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			made.add(spawn(level, net.minecraft.world.entity.EntityType.COW, at.offset(2 + i, 0, 3)));
		}
		made.add(spawn(level, net.minecraft.world.entity.EntityType.ZOMBIE, at.offset(18, 0, 6)));
		made.add(spawn(level, com.iafabric.entity.IafEntityRegistry.CYCLOPS, at.offset(9, 0, 9)));
		made.add(spawn(level, com.iafabric.entity.IafEntityRegistry.DREAD_THRALL, at.offset(5, 0, -6)));
		Map<String, Double> near = ExiledTalk.near(player);
		log("what catches her eye: " + near);
		helper.assertTrue(near.getOrDefault("cow", 0.0) > near.getOrDefault("zombie", 0.0) && near.containsKey("zombie"),
			"three cows near do not catch her eye more than a zombie far");
		helper.assertTrue(near.containsKey("cyclops") && near.containsKey("dread"), "a cyclops or a dread thrall near did not catch her eye");
		var villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
		var witch = net.minecraft.world.entity.EntityType.WITCH.create(level);
		var bone = com.iafabric.entity.IafEntityRegistry.ICE_DRAGON.create(level);
		var frost = com.iafabric.entity.IafEntityRegistry.ICE_DRAGON.create(level);
		var exiled = com.iafabric.entity.IafEntityRegistry.EXILED_PIXIE.create(level);
		bone.setDreadLook(1);
		frost.setDreadLook(2);
		log("their kinds: villager " + ExiledTalk.kind(villager) + ", witch " + ExiledTalk.kind(witch) + ", a dread knight's dragon " + ExiledTalk.kind(bone)
			+ ", Black Frost " + ExiledTalk.kind(frost) + ", herself " + ExiledTalk.kind(exiled));
		helper.assertTrue("villager".equals(ExiledTalk.kind(villager)) && "witch".equals(ExiledTalk.kind(witch)), "a villager or a witch is not of its kind");
		helper.assertTrue("bone_dragon".equals(ExiledTalk.kind(bone)), "a dread knight's dragon is not a bone dragon");
		helper.assertTrue(ExiledTalk.kind(frost) == null && ExiledTalk.kind(exiled) == null, "Black Frost or herself has lines of a kind");
		ExiledPixieData data = ExiledPixies.get(player);
		Set<String> told = new HashSet<>();
		for (int i = 0; i < 50; i++) {
			told.add(((TranslatableContents) ExiledTalk.line(player, data, "sees.villager", 50).getContents()).getKey());
		}
		String next = ((TranslatableContents) ExiledTalk.line(player, data, "sees.villager", 50).getContents()).getKey();
		log("fifty anecdotes told: " + told.size() + " different; then again: " + next);
		helper.assertTrue(told.size() == 50, "an anecdote came twice before all fifty were told: " + told.size());
		made.forEach(net.minecraft.world.entity.Entity::discard);
		helper.getLevel().getServer().getPlayerList().remove(player);
		helper.succeed();
	}

	private static net.minecraft.world.entity.Entity spawn(net.minecraft.server.level.ServerLevel level,
		net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type, BlockPos at) {
		net.minecraft.world.entity.Mob mob = type.create(level);
		mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		mob.setNoAi(true);
		level.addFreshEntity(mob);
		return mob;
	}

	/**
	 * Black Frost's ice on who lacks the pixie's gift goes straight to the heart, through netherite, resistance, golden
	 * hearts and even a mod's invulnerability, and it cauterizes: nothing heals them (a potion's heal, a mod's
	 * setHealth); milk takes the effect away, not the wound. A deadly bite kills, unless a totem of undying in hand saves
	 * them (spent). With the gift its blow is a blow like any other (the invulnerability holds it), and no wound.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void frostBitesToTheHeart(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer plain = survivalPlayer(helper, "test-unready");
		ServerPlayer gifted = survivalPlayer(helper, "test-gifted");
		ServerPlayer doomed = survivalPlayer(helper, "test-doomed");
		ExiledPixieData gift = ExiledPixies.get(gifted);
		gift.gifts |= ExiledPixieData.GIFT_ICE;
		gifted.setAttached(ExiledPixies.DATA, gift);
		var frost = com.iafabric.entity.IafEntityRegistry.ICE_DRAGON.create(level);
		frost.setDreadLook(2);
		for (ServerPlayer player : List.of(plain, gifted)) {
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 600, 4));
			// golden hearts (an absorption: the game keeps none without it)
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 600, 2));
			// as a mod's god mode would have it
			player.getAbilities().invulnerable = true;
		}
		float goldenBefore = plain.getAbsorptionAmount();
		com.iafabric.boss.BossLimits.strike(level, plain, frost, 0.25F);
		com.iafabric.boss.BossLimits.strike(level, gifted, frost, 0.25F);
		float bitten = plain.getHealth();
		float golden = plain.getAbsorptionAmount();
		plain.heal(10F);
		plain.setHealth(20F);
		float healed = plain.getHealth();
		// as milk does
		plain.removeAllEffects();
		boolean still = com.iafabric.boss.BossCold.wounded(plain);
		plain.heal(4F);
		log("Black Frost's bite through netherite, resistance V, golden hearts and invulnerability: life " + bitten + " (golden hearts " + golden
			+ "), healed to " + healed + ", after milk wounded " + still + " (life " + plain.getHealth() + "); with the gift: life " + gifted.getHealth()
			+ ", wounded " + com.iafabric.boss.BossCold.wounded(gifted));
		helper.assertTrue(bitten == 15F, "Black Frost's ice did not go straight to the heart: " + bitten);
		helper.assertTrue(goldenBefore > 0F && golden == goldenBefore, "the golden hearts took the bite: " + goldenBefore + " -> " + golden);
		helper.assertTrue(healed == 15F, "a wounded player was healed");
		helper.assertTrue(still && plain.getHealth() == 15F, "milk took the wound away");
		helper.assertTrue(gifted.getHealth() == 20F && !com.iafabric.boss.BossCold.wounded(gifted), "with the gift its blow bit through the invulnerability, or wounded");
		doomed.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
		doomed.setHealth(4F);
		com.iafabric.boss.BossLimits.strike(level, doomed, frost, 0.25F);
		boolean saved = doomed.isAlive();
		float left = doomed.getHealth();
		boolean spent = doomed.getOffhandItem().isEmpty();
		com.iafabric.boss.BossLimits.strike(level, doomed, frost, 0.25F);
		log("a deadly bite with a totem in hand: saved " + saved + " (life " + left + ", the totem spent " + spent + "); bitten again with none: alive "
			+ doomed.isAlive());
		helper.assertTrue(saved && left == 1F && spent, "a totem of undying in hand did not save from a deadly bite of its ice");
		helper.assertTrue(!doomed.isAlive(), "a deadly bite with no totem left did not kill");
		leave(helper, plain);
		leave(helper, gifted);
		helper.getLevel().getServer().getPlayerList().remove(doomed);
		helper.succeed();
	}

	/**
	 * Her memories come back on their own too: a troll beaten and a horse ridden (as the game counts them) are their
	 * quests done without her asking; all of them over, the player is ready for Cryona.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void memoriesComeOnTheirOwn(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper, "test-wanderer");
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		ExiledPixies.meet(player, at.above(2));
		ExiledPixies.act(player, MessageExiledAction.ADOPT, 0);
		ExiledPixieData data = ExiledPixies.get(player);
		data.nextOffer = Long.MAX_VALUE / 2;
		player.setAttached(ExiledPixies.DATA, data);
		player.awardStat(net.minecraft.stats.Stats.ENTITY_KILLED.get(com.iafabric.entity.IafEntityRegistry.TROLL));
		player.awardStat(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.HORSE_ONE_CM), 250);
		helper.runAfterDelay(60, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			log("on their own: a troll beaten " + d.isDone(ExiledQuest.TROLL) + ", a horse ridden " + d.isDone(ExiledQuest.HORSE) + "; memories left "
				+ ExiledQuest.left(d) + ", ready " + ExiledQuest.ready(d));
			helper.assertTrue(d.isDone(ExiledQuest.TROLL) && d.isDone(ExiledQuest.HORSE), "what the player did on their own did not count");
			helper.assertTrue(!ExiledQuest.ready(d) && ExiledQuest.left(d) == 12, "ready, or the memories left are not twelve: " + ExiledQuest.left(d));
			for (ExiledQuest quest : ExiledQuest.ordered()) {
				if (quest.offered()) {
					d.done |= quest.bit();
				}
			}
			player.setAttached(ExiledPixies.DATA, d);
			helper.assertTrue(ExiledQuest.ready(d) && ExiledQuest.left(d) == 0, "all of them over is not ready");
		});
		helper.runAfterDelay(100, () -> {
			leave(helper, player);
			helper.succeed();
		});
	}

	/**
	 * Near Cryona not ready: no glade (a warning instead); all her memories back, her call comes, but only to one
	 * standing on the ground (her memory holds them where they stand: not in the air).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 140)
	public void notReadyNoMoment(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer player = survivalPlayer(helper, "test-hasty");
		// (in creative: she does not wake for them, nor fight them)
		player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		player.setOnGround(true);
		ExiledPixies.meet(player, at.above(2));
		ExiledPixies.act(player, MessageExiledAction.ADOPT, 0);
		ExiledPixieData data = ExiledPixies.get(player);
		data.nextOffer = Long.MAX_VALUE / 2;
		player.setAttached(ExiledPixies.DATA, data);
		// asleep on her throne (her sign)
		var cryona = com.iafabric.entity.EntityCryonaAsleep.make(level, at.offset(8, 0, 0), null, at.getX() + 8.5, at.getY(), at.getZ() + 0.5, 0F,
			net.minecraft.world.entity.MobSpawnType.COMMAND);
		level.addFreshEntity(cryona);
		helper.runAfterDelay(50, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			log("near Cryona (" + (cryona.isAlive() && cryona.level() == level) + "), " + ExiledQuest.left(d) + " memories left: her call " + (d.visionFrom != null));
			helper.assertTrue(d.visionFrom == null, "her call came with her memories not back");
			for (ExiledQuest quest : ExiledQuest.ordered()) {
				if (quest.offered()) {
					d.done |= quest.bit();
				}
			}
			player.setAttached(ExiledPixies.DATA, d);
			player.setOnGround(false);
		});
		helper.runAfterDelay(90, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			log("all her memories back, the player in the air: her call " + (d.visionFrom != null));
			helper.assertTrue(d.visionFrom == null, "her call came to a player in the air");
			player.setOnGround(true);
		});
		helper.runAfterDelay(120, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			log("all her memories back, the player standing: her call " + (d.visionFrom != null) + ", in her memory " + FairyVision.inMemory(player));
			helper.assertTrue(d.visionFrom != null && FairyVision.inMemory(player), "her call did not come with all her memories back");
			cryona.discard();
			leave(helper, player);
			helper.succeed();
		});
	}

	/**
	 * /iceandfire fairy complete: a player who never met her has her as a friend, out, every quest of hers done (ready for
	 * the right moment: her last quest and her gift still to come), her bond at level 5; and the strength pixie (by
	 * default) bonded at level 5 with its three spells, out with them. With a color, that pixie is out instead.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
	public void completeCommand(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		ServerPlayer player = survivalPlayer(helper, "test-tester");
		BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
		player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
		var source = server.createCommandSourceStack().withEntity(player).withPosition(player.position()).withSuppressedOutput();
		server.getCommands().performPrefixedCommand(source, "iceandfire fairy complete");
		ExiledPixieData data = ExiledPixies.get(player);
		var bonds = com.iafabric.pixie.PixieBonding.get(player);
		var strength = bonds.get(0);
		boolean spells = true;
		for (var spell : com.iafabric.pixie.PixieAbility.spellsOf(0)) {
			spells &= strength.knows(spell);
		}
		log("fairy complete: her stage " + data.stage + ", out " + data.out + ", " + ExiledQuest.left(data) + " quests left, ready " + ExiledQuest.ready(data)
			+ ", level " + data.level + ", her last quest done " + data.isDone(ExiledQuest.CRYONA) + ", gift " + data.hasGift(ExiledPixieData.GIFT_ICE)
			+ "; strength pixie bonded " + strength.bonded + " level " + strength.level + " spells " + spells + ", out " + bonds.active);
		helper.assertTrue(data.stage == ExiledPixieData.FRIEND && data.out && ExiledQuest.ready(data) && ExiledQuest.left(data) == 0 && data.level == 5,
			"her quests were not all done, or she is not a friend out at level 5");
		helper.assertTrue(!data.isDone(ExiledQuest.CRYONA) && !data.hasGift(ExiledPixieData.GIFT_ICE) && data.current() == null,
			"the right moment is not left to come");
		helper.assertTrue(strength.bonded && strength.level == 5 && spells && bonds.active == 0, "the strength pixie is not bonded at level 5 and out");
		server.getCommands().performPrefixedCommand(source, "iceandfire fairy complete speed");
		var after = com.iafabric.pixie.PixieBonding.get(player);
		log("fairy complete speed: out " + after.active + ", speed level " + after.get(2).level + ", strength still bonded " + after.get(0).bonded);
		helper.assertTrue(after.active == 2 && after.get(2).bonded && after.get(2).level == 5 && after.get(0).bonded, "the speed pixie is not the one out");
		helper.runAfterDelay(30, () -> {
			boolean hers = ExiledPixies.living(player) != null;
			boolean out = com.iafabric.pixie.PixieBonding.activePixie(player) != null;
			log("a moment later: her pixie out " + hers + ", the speed pixie out " + out);
			helper.assertTrue(hers && out, "the pixies are not out with the player");
			leave(helper, player);
			helper.succeed();
		});
	}

	/** Only grown females lay eggs: a pair of stage 4 may breed, a young pair not; a young female's body holds no egg. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void onlyGrownFemalesLayEggs(GameTestHelper helper) {
		var level = helper.getLevel();
		com.iafabric.entity.EntityDragonBase male = com.iafabric.entity.IafEntityRegistry.FIRE_DRAGON.create(level);
		com.iafabric.entity.EntityDragonBase female = com.iafabric.entity.IafEntityRegistry.FIRE_DRAGON.create(level);
		male.setGender(true);
		female.setGender(false);
		male.growDragon(60);
		female.growDragon(60);
		boolean young = male.canMate(female) || female.laysEggs();
		male.growDragon(85);
		female.growDragon(85);
		boolean grown = male.canMate(female) && female.canMate(male) && female.laysEggs() && !male.laysEggs();
		log("a pair of stage " + female.getDragonStage() + ": breeds " + grown + "; at stage 3: " + young);
		helper.assertTrue(!young, "young dragons (stage 3) may breed or lay eggs");
		helper.assertTrue(grown, "a grown pair (stage 4) may not breed, or the female lays no eggs");
		helper.succeed();
	}

	/**
	 * Her gift, the resistance to absolute ice: Black Frost's breath freezes a player without it (each time it
	 * catches them, a moment free after); with it, its first touch only chills, then it freezes for half as long.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void iceGiftHoldsLess(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer plain = survivalPlayer(helper, "test-cold");
		ServerPlayer gifted = survivalPlayer(helper, "test-warm");
		ExiledPixieData data = ExiledPixies.get(gifted);
		data.gifts |= ExiledPixieData.GIFT_ICE;
		gifted.setAttached(ExiledPixies.DATA, data);
		com.iafabric.entity.EntityDragonBase frost = com.iafabric.entity.IafEntityRegistry.ICE_DRAGON.create(level);
		frost.setDreadLook(2);
		com.iafabric.boss.BossCold.breathFreeze(plain, frost, 60);
		com.iafabric.boss.BossCold.breathFreeze(gifted, frost, 60);
		int plainHeld = com.iafabric.entity.props.FrozenProperties.ticksUntilUnfrozen(plain);
		boolean giftedFirst = com.iafabric.entity.props.FrozenProperties.isFrozen(gifted);
		boolean chilled = gifted.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
		helper.runAfterDelay(5, () -> {
			com.iafabric.boss.BossCold.breathFreeze(gifted, frost, 60);
			int giftedHeld = com.iafabric.entity.props.FrozenProperties.ticksUntilUnfrozen(gifted);
			log("Black Frost's breath: held without her gift " + plainHeld + " ticks; with it, the first touch froze " + giftedFirst + " (chilled "
				+ chilled + "), the second held " + giftedHeld);
			helper.assertTrue(plainHeld == 30, "Black Frost's breath did not freeze a player without the gift for its time: " + plainHeld);
			helper.assertTrue(!giftedFirst && chilled, "with the gift, the breath's first touch froze (or did not chill)");
			helper.assertTrue(giftedHeld == 15, "with the gift, the second touch did not freeze for half the time: " + giftedHeld);
			// a Frost Ward stops the breath's ice
			ServerPlayer warded = survivalPlayer(helper, "test-warded");
			warded.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.iafabric.effect.IafEffects.FROST_WARD, 200));
			com.iafabric.boss.BossCold.breathFreeze(warded, frost, 60);
			helper.assertTrue(!com.iafabric.entity.props.FrozenProperties.isFrozen(warded), "a Frost Ward did not stop Black Frost's breath");
			helper.getLevel().getServer().getPlayerList().remove(warded);
			leave(helper, plain);
			leave(helper, gifted);
			frost.discard();
			helper.succeed();
		});
	}

	/** The egg quest leads to a den where a grown female keeps her eggs (a nest noted), and that nest counts while she or an egg is there. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 120)
	public void eggQuestLeadsToANest(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer player = survivalPlayer(helper, "test-egg-thief");
		BlockPos start = helper.absolutePos(new BlockPos(1, 2, 1));
		player.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
		ExiledPixies.meet(player, start.above(2));
		ExiledPixies.act(player, MessageExiledAction.ADOPT, 0);
		BlockPos den = helper.absolutePos(new BlockPos(6, 2, 6));
		com.iafabric.entity.EntityDragonBase mother = com.iafabric.entity.IafEntityRegistry.FIRE_DRAGON.create(level);
		mother.setGender(false);
		mother.growDragon(90);
		mother.setNoAi(true);
		mother.moveTo(den.getX() + 0.5, den.getY(), den.getZ() + 0.5, 0, 0);
		level.addFreshEntity(mother);
		com.iafabric.world.FairyLandmarks.note(level, com.iafabric.world.FairyLandmarks.Kind.NEST, den);
		ExiledPixieData data = ExiledPixies.get(player);
		data.done |= ExiledQuest.CYCLOPS.bit() | ExiledQuest.BONES.bit();
		data.nextOffer = Long.MAX_VALUE / 2;
		player.setAttached(ExiledPixies.DATA, data);
		ExiledPixies.act(player, MessageExiledAction.ACCEPT, ExiledQuest.EGG.ordinal());
		helper.runAfterDelay(50, () -> {
			ExiledPixieData d = ExiledPixies.get(player);
			log("the egg quest leads to " + (d.target == null ? "nowhere" : d.target.toShortString()) + " (the den at " + den.toShortString() + "), a nest: "
				+ com.iafabric.world.FairyLandmarks.stillThere(level, com.iafabric.world.FairyLandmarks.Kind.NEST, den));
			helper.assertTrue(d.current() == ExiledQuest.EGG, "the egg quest was not taken up");
			helper.assertTrue(d.target != null && d.target.distSqr(den) < 9, "the egg quest does not lead to the den with a grown female");
			helper.assertTrue(com.iafabric.world.FairyLandmarks.stillThere(level, com.iafabric.world.FairyLandmarks.Kind.NEST, den), "a grown female's den is no nest");
			com.iafabric.world.FairyLandmarks.forget(level, com.iafabric.world.FairyLandmarks.Kind.NEST, den);
			if (d.checkpoint != null) {
				com.iafabric.pixie.FairyCheckpoints.remove(level, d.checkpoint);
			}
			mother.discard();
			leave(helper, player);
			helper.succeed();
		});
	}

	/** A dragon skeleton she remembers is made when her player comes near: on dry ground, old enough for four bones. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
	public void rememberedSkeletonIsMade(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos from = helper.absolutePos(new BlockPos(2, 2, 2));
		BlockPos at = com.iafabric.world.FairyLandmarks.remember(level, com.iafabric.world.FairyLandmarks.Kind.SKELETON, from, level.getRandom());
		helper.assertTrue(at != null, "she remembered no place for a skeleton");
		double distance = Math.sqrt(at.distSqr(new BlockPos(from.getX(), at.getY(), from.getZ())));
		log("a skeleton remembered " + (int) distance + " blocks off, waiting: " + com.iafabric.world.FairyLandmarks.waiting(level, at));
		helper.assertTrue(distance >= 110 && distance <= 270, "the place is not a little way off: " + distance);
		helper.assertTrue(com.iafabric.world.FairyLandmarks.waiting(level, at), "the place was made before anyone came near");
		ServerPlayer player = survivalPlayer(helper, "test-explorer");
		player.teleportTo(level, at.getX() + 20.5, level.getSeaLevel() + 40, at.getZ() + 0.5, Set.of(), 0, 0);
		player.getAbilities().flying = true;
		// (a test player's connection is not ticked: its land is loaded by hand, as a real player's would be)
		level.setChunkForced(at.getX() >> 4, at.getZ() >> 4, true);
		level.getChunk(at.getX() >> 4, at.getZ() >> 4);
		helper.succeedWhen(() -> {
			var skeletons = level.getEntitiesOfClass(com.iafabric.entity.EntityDragonBase.class, new AABB(at).inflate(40, 200, 40),
				d -> d.isModelDead());
			helper.assertTrue(!skeletons.isEmpty(), "no skeleton made yet");
			var skeleton = skeletons.get(0);
			int stages = Math.min(skeleton.getAgeInDays() / 5, 25) - skeleton.getDeathStage();
			log("made: a " + skeleton.getType().toShortString() + " skeleton at " + skeleton.blockPosition().toShortString() + ", " + stages + " harvests left");
			helper.assertTrue(stages >= 6, "too few harvests left for four bones: " + stages);
			helper.assertTrue(level.getBlockState(skeleton.blockPosition().below()).getFluidState().isEmpty(), "it lies in water");
			skeletons.forEach(net.minecraft.world.entity.Entity::discard);
			com.iafabric.world.FairyLandmarks.forget(level, com.iafabric.world.FairyLandmarks.Kind.SKELETON, skeleton.blockPosition());
			level.setChunkForced(at.getX() >> 4, at.getZ() >> 4, false);
			helper.getLevel().getServer().getPlayerList().remove(player);
		});
	}
}
