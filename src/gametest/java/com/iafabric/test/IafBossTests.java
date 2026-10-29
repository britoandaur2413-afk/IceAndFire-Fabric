package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.boss.BossCold;
import com.iafabric.boss.CryonaFight;
import com.iafabric.effect.IafEffects;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityDreadQueen;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.IafVillagerRegistry;
import com.iafabric.entity.props.FrozenProperties;
import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

/** Cryona's fight as one boss in two parts, the Frost Ward, the scribe's sure trades, her minions for a while. */
public class IafBossTests {
	private static void log(String message) {
		IafMod.LOGGER.info("[gametest] " + message);
	}

	/** The Frost Ward holds off the bosses' cold and the ice dragons' freeze; the Glacial Prison freezes through it. */
	@GameTest(maxTicks = 40)
	public void frostWard(GameTestHelper helper) {
		var level = helper.getLevel();
		var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1), EntitySpawnReason.COMMAND);
		zombie.setNoAi(true);
		zombie.addEffect(new MobEffectInstance(IafEffects.FROST_WARD, 400));
		BossCold.freeze(level, zombie, 20);
		helper.assertTrue(!FrozenProperties.isFrozen(zombie), "the ward did not hold off the bosses' cold");
		EntityDragonBase dragon = IafEntityRegistry.ICE_DRAGON.create(level, EntitySpawnReason.COMMAND);
		dragon.growDragon(80);
		BossCold.breathFreeze(zombie, dragon, 200);
		helper.assertTrue(!FrozenProperties.isFrozen(zombie), "the ward did not hold off an ice dragon's breath");
		float hurt = BossCold.breathDamage(zombie, dragon, 10F);
		helper.assertTrue(hurt < 10F, "the ward did not soften an ice breath: " + hurt);
		BossCold.prison(level, zombie, 30);
		helper.assertTrue(FrozenProperties.isFrozen(zombie), "the Glacial Prison did not freeze through the ward");
		// the potions: a short one sold by the scribes, a long one brewed with ice dragon blood
		int shortTicks = IafEffects.FROST_WARD_POTION.value().getEffects().get(0).getDuration();
		int longTicks = IafEffects.LONG_FROST_WARD_POTION.value().getEffects().get(0).getDuration();
		helper.assertTrue(shortTicks == 400 && longTicks == 1200, "the wards last " + shortTicks + " and " + longTicks + " ticks");
		var recipes = level.getServer().getRecipeManager();
		for (String kind : new String[]{"potion", "splash_potion", "lingering_potion"}) {
			var key = ResourceKey.create(Registries.RECIPE, IafMod.id("brewing/" + kind + "_awkward_ice_dragon_blood"));
			helper.assertTrue(recipes.byKey(key).isPresent(), "no brewing of the long ward: " + key.identifier());
		}
		log("frost ward: no freeze from the bosses nor an ice breath, breath " + hurt + " of 10, the prison freezes through; potions "
			+ shortTicks + " and " + longTicks + " ticks, brewed with ice dragon blood");
		zombie.discard();
		helper.succeed();
	}

	/** A scribe always sells the short Frost Ward (novice) and the long one for ice dragon blood (journeyman). */
	@GameTest(maxTicks = 40)
	public void scribeSellsTheWard(GameTestHelper helper) {
		var level = helper.getLevel();
		Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
		villager.setNoAi(true);
		var scribe = BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(IafVillagerRegistry.SCRIBE);
		villager.setVillagerData(villager.getVillagerData().withProfession(scribe).withLevel(1));
		helper.assertTrue(sells(villager, IafEffects.FROST_WARD_POTION), "a novice scribe does not sell the Frost Ward: " + offers(villager));
		villager.setVillagerData(villager.getVillagerData().withLevel(3));
		IafVillagerRegistry.addSureTrades(villager, level);
		boolean longWard = false;
		for (MerchantOffer offer : villager.getOffers()) {
			if (potion(offer.getResult(), IafEffects.LONG_FROST_WARD_POTION) && offer.getCostB().is(IafItemRegistry.ICE_DRAGON_BLOOD)) {
				longWard = true;
			}
		}
		helper.assertTrue(longWard, "a journeyman scribe does not sell the long Frost Ward for ice dragon blood: " + offers(villager));
		log("scribe: sells the Frost Ward at novice and the long one for ice dragon blood at journeyman (" + villager.getOffers().size() + " offers)");
		villager.discard();
		helper.succeed();
	}

	private static boolean sells(Villager villager, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
		for (MerchantOffer offer : villager.getOffers()) {
			if (potion(offer.getResult(), potion)) {
				return true;
			}
		}
		return false;
	}

	private static boolean potion(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		return contents != null && contents.is(potion);
	}

	private static String offers(Villager villager) {
		StringBuilder text = new StringBuilder();
		for (MerchantOffer offer : villager.getOffers()) {
			text.append(offer.getResult()).append("; ");
		}
		return text.toString();
	}

	/** The dead Cryona raises go back to the cold when their time is up, and leave nothing behind. */
	@GameTest(maxTicks = 60)
	public void minionsForAWhile(GameTestHelper helper) {
		var thrall = helper.spawn(IafEntityRegistry.DREAD_THRALL, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
		thrall.setNoAi(true);
		thrall.summonFor(20);
		helper.assertTrue(thrall.isSummoned(), "the thrall is not called for a while");
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(thrall.isRemoved(), "the called thrall stayed after its time");
			log("minions: a called thrall went back to the cold after its time");
			helper.succeed();
		});
	}

	/** Cryona's loot table loads (it was written in the old form once and was dropped whole). */
	@GameTest(maxTicks = 20)
	public void cryonaLoot(GameTestHelper helper) {
		var tables = helper.getLevel().getServer().reloadableRegistries();
		for (String name : new String[]{"entities/dread_queen", "chest/mausoleum_chest"}) {
			LootTable table = tables.getLootTable(ResourceKey.create(Registries.LOOT_TABLE, IafMod.id(name)));
			helper.assertTrue(table != LootTable.EMPTY, "the loot table " + name + " did not load");
		}
		log("loot: Cryona's table and her chest's load");
		helper.succeed();
	}

	/**
	 * One boss in two parts (where she wakes outside her realm, as in the gametest server): on Black Frost her blows go
	 * to it; when it falls she mourns it, its body goes and its remains stay, she rises alone with her own life; at her
	 * end she kneels, her weapons go to the player and she dies. (In a batch of its own: the dread tests clear the dread
	 * near them. Each step waits for the one before.)
	 */
	@GameTest(maxTicks = 1000, environment = "iceandfire_fabric:boss")
	public void cryonaOneBossTwoParts(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos bed = helper.absolutePos(new BlockPos(6, 2, 6));
		// (the test's chunks kept ticking: its fake player does not hold them, and a big dragon spans several)
		var forced = new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
		var middle = net.minecraft.world.level.ChunkPos.containing(bed);
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				var chunk = new net.minecraft.world.level.ChunkPos(middle.x() + dx, middle.z() + dz);
				if (level.setChunkForced(chunk.x(), chunk.z(), true)) {
					forced.add(chunk);
				}
			}
		}
		EntityDragonBase frost = com.iafabric.world.structure.DreadPieces.dreadDragon(level, level.getRandom(), bed, 125, true);
		level.addFreshEntity(frost);
		EntityDreadQueen queen = helper.spawn(IafEntityRegistry.DREAD_QUEEN, new BlockPos(6, 2, 6), EntitySpawnReason.COMMAND);
		queen.startRiding(frost, true, true);
		ServerPlayer player = survivalPlayer(helper);
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 4000, 4));
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 4000, 4));
		var near = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1));
		player.snapTo(near.x, near.y, near.z, 0, 0);
		float[] at = new float[2];
		long[] fell = new long[2];
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(queen.getPhase() == CryonaFight.MOUNTED, "she is not in her first part on Black Frost: " + queen.getPhase()))
			.thenExecute(() -> {
				at[0] = frost.getHealth();
				at[1] = queen.getHealth();
				queen.hurtServer(level, level.damageSources().playerAttack(player), 30F);
				helper.assertTrue(queen.getHealth() == at[1], "on Black Frost she took the blow herself");
				helper.assertTrue(frost.getHealth() < at[0], "on Black Frost the blow did not go to it: " + frost.getHealth() + " / " + at[0]);
				// Black Frost falls (a dragon's kill command takes it away whole, and no player's blow kills it at a stroke: the
				// blow of an admin's /kill, for its body)
				frost.hurtServer(level, level.damageSources().genericKill(), 100000F);
			})
			.thenWaitUntil(() -> helper.assertTrue(queen.getPhase() == CryonaFight.FALLING, "she does not mourn Black Frost: " + queen.getPhase()))
			.thenExecute(() -> {
				fell[0] = level.getGameTime();
				helper.assertTrue(!queen.isPassenger(), "she still rides Black Frost's body");
				float before = queen.getHealth();
				queen.hurtServer(level, level.damageSources().playerAttack(player), 50F);
				helper.assertTrue(queen.getHealth() == before, "she was hurt while she mourned");
			})
			// (her mourning lasts the pixie's memories, almost two minutes: once its body has gone to her, hurried to her rising)
			.thenWaitUntil(() -> helper.assertTrue(CryonaFight.of(queen).scene() > 110, "its body did not go to her: " + CryonaFight.of(queen).scene()))
			.thenExecute(() -> {
				helper.assertTrue(CryonaFight.mourning(queen) > 0.9F, "her mourning's bar is not shown full: " + CryonaFight.mourning(queen));
				CryonaFight.of(queen).hurry(30);
			})
			.thenWaitUntil(() -> helper.assertTrue(queen.getPhase() == CryonaFight.ALONE, "she did not rise alone: " + queen.getPhase()))
			.thenExecute(() -> {
				fell[1] = level.getGameTime();
				helper.assertTrue(frost.isRemoved(), "Black Frost's body did not go");
				var remains = level.getEntitiesOfClass(ItemEntity.class, new AABB(bed).inflate(24), item -> item.getItem().is(IafItemRegistry.ICE_DRAGON_BLOOD));
				helper.assertTrue(!remains.isEmpty(), "Black Frost left no blood on the ice");
				helper.assertTrue(queen.getShield() >= CryonaFight.SHIELD_MAX - 1, "she rose alone without her ice shield: " + queen.getShield());
				// her end: the killing blow (an admin's, past her shield and her limit) leaves her a breath of life for it
				queen.hurtServer(level, level.damageSources().genericKill(), 5000F);
				helper.assertTrue(queen.isAlive() && queen.getPhase() == CryonaFight.DYING, "her end was not a scene: alive " + queen.isAlive() + ", " + queen.getPhase());
			})
			.thenWaitUntil(() -> helper.assertTrue(!queen.isAlive(), "she did not fade"))
			.thenExecute(() -> {
				boolean sword = player.getInventory().contains(new ItemStack(IafItemRegistry.DREAD_QUEEN_SWORD));
				boolean staff = player.getInventory().contains(new ItemStack(IafItemRegistry.DREAD_QUEEN_STAFF));
				var dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(bed).inflate(32),
					item -> item.getItem().is(IafItemRegistry.DREAD_QUEEN_SWORD) || item.getItem().is(IafItemRegistry.DREAD_QUEEN_STAFF));
				helper.assertTrue(sword && staff || dropped.size() >= 2, "her weapons did not come to the player: sword " + sword + ", staff " + staff + ", dropped " + dropped.size());
				log("cryona: on Black Frost her blows went to it; it fell, she mourned " + (fell[1] - fell[0]) + " ticks, its body went and its blood stayed; "
					+ "alone she rose; at her end her weapons came to the player (" + (sword && staff ? "in hand" : "at their feet") + ")");
				level.getServer().getPlayerList().remove(player);
				for (var item : level.getEntitiesOfClass(ItemEntity.class, new AABB(bed).inflate(40))) {
					item.discard();
				}
				for (var minion : level.getEntitiesOfClass(com.iafabric.entity.EntityDreadMob.class, new AABB(bed).inflate(60))) {
					minion.discard();
				}
				for (var chunk : forced) {
					level.setChunkForced(chunk.x(), chunk.z(), false);
				}
			})
			.thenSucceed();
	}

	/**
	 * The bosses' limits: Black Frost has ten thousand of life and loses at most a hundredth of it a second, whatever
	 * strikes it; no blow moves it. Cryona has three thousand and an ice shield of a thousand that takes blows first.
	 */
	@GameTest(maxTicks = 120, environment = "iceandfire_fabric:boss")
	public void bossLimits(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos at = helper.absolutePos(new BlockPos(6, 2, 6));
		EntityDragonBase frost = com.iafabric.world.structure.DreadPieces.dreadDragon(level, level.getRandom(), at, 125, true);
		frost.setNoAi(true);
		level.addFreshEntity(frost);
		EntityDreadQueen queen = helper.spawn(IafEntityRegistry.DREAD_QUEEN, new BlockPos(1, 2, 1), EntitySpawnReason.COMMAND);
		queen.setNoAi(true);
		var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(2, 2, 6), EntitySpawnReason.COMMAND);
		zombie.setNoAi(true);
		float[] marks = new float[4];
		helper.startSequence()
			.thenExecute(() -> {
				helper.assertTrue(Math.abs(frost.getMaxHealth() - 10000) < 1, "Black Frost has " + frost.getMaxHealth() + " life");
				helper.assertTrue(Math.abs(queen.getMaxHealth() - 3000) < 1, "Cryona has " + queen.getMaxHealth() + " life");
				marks[0] = frost.getHealth();
				frost.hurtServer(level, level.damageSources().mobAttack(zombie), 5000F);
				float lost = marks[0] - frost.getHealth();
				helper.assertTrue(lost > 0 && lost <= 100.5F, "a blow of 5000 took " + lost + " of Black Frost's life (at most 100)");
				marks[0] = frost.getHealth();
				var before = frost.getDeltaMovement();
				frost.knockback(4, 1, 0, level.damageSources().mobAttack(zombie), 1F);
				helper.assertTrue(frost.getDeltaMovement().distanceTo(before) < 0.001 && !frost.isPushable(), "Black Frost was moved by a knockback");
			})
			.thenIdle(21)
			.thenExecute(() -> {
				frost.hurtServer(level, level.damageSources().mobAttack(zombie), 5000F);
				float lost = marks[0] - frost.getHealth();
				helper.assertTrue(lost > 0 && lost <= 100.5F, "a second blow a second later took " + lost);
			})
			.thenWaitUntil(() -> helper.assertTrue(queen.getPhase() == CryonaFight.ALONE && queen.getShield() >= CryonaFight.SHIELD_MAX - 1,
				"Cryona alone has no whole shield: phase " + queen.getPhase() + ", shield " + queen.getShield()))
			.thenExecute(() -> {
				marks[1] = queen.getHealth();
				queen.hurtServer(level, level.damageSources().mobAttack(zombie), 300F);
				helper.assertTrue(queen.getHealth() == marks[1] && Math.abs(queen.getShield() - 700) < 1, "her shield did not take the blow: life "
					+ queen.getHealth() + ", shield " + queen.getShield());
			})
			.thenIdle(12)
			.thenExecute(() -> {
				queen.hurtServer(level, level.damageSources().mobAttack(zombie), 5000F);
				float lost = marks[1] - queen.getHealth();
				helper.assertTrue(queen.getShield() <= 0 && lost > 0 && lost <= 30.5F, "after her shield broke, a blow of 5000 took " + lost
					+ " of her life (at most 30), shield " + queen.getShield());
				log("limits: Black Frost 10000 life, a blow of 5000 takes at most 100 a second and moves it not; Cryona 3000 and a shield of 1000 "
					+ "that takes blows first, then at most 30 a second");
				frost.discard();
				queen.discard();
				zombie.discard();
			})
			.thenSucceed();
	}

	/**
	 * The cold of the Frozen Throne takes a share of life through any armor or potion; the longest calls take nine
	 * tenths of it, always (golden hearts kept): they kill only the hurt, and a totem of undying in hand saves.
	 */
	@GameTest(maxTicks = 40, environment = "iceandfire_fabric:boss")
	public void frostDoom(GameTestHelper helper) {
		var level = helper.getLevel();
		var boss = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1), EntitySpawnReason.COMMAND);
		boss.setNoAi(true);
		var armored = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 2, 3), EntitySpawnReason.COMMAND);
		armored.setNoAi(true);
		for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
			net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
			armored.setItemSlot(slot, new ItemStack(switch (slot) {
				case HEAD -> net.minecraft.world.item.Items.NETHERITE_HELMET;
				case CHEST -> net.minecraft.world.item.Items.NETHERITE_CHESTPLATE;
				case LEGS -> net.minecraft.world.item.Items.NETHERITE_LEGGINGS;
				default -> net.minecraft.world.item.Items.NETHERITE_BOOTS;
			}));
		}
		armored.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 4));
		float full = armored.getHealth();
		com.iafabric.boss.BossLimits.strike(level, armored, boss, 0.25F);
		float lost = full - armored.getHealth();
		helper.assertTrue(Math.abs(lost - full * 0.25F) < 0.6F, "a share of a quarter took " + lost + " of " + full + " through netherite and Resistance V");
		// the longest calls: nine tenths of its life, always, through Resistance V; its golden hearts stay as they were
		var whole = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(5, 2, 5), EntitySpawnReason.COMMAND);
		whole.setNoAi(true);
		whole.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 4));
		whole.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
		float golden = whole.getAbsorptionAmount();
		float max = whole.getMaxHealth();
		com.iafabric.boss.BossLimits.doom(level, whole, boss);
		helper.assertTrue(whole.isAlive() && Math.abs(whole.getHealth() - max * 0.1F) < 0.6F, "a whole one was left with " + whole.getHealth() + " of " + max);
		helper.assertTrue(golden > 0 && Math.abs(whole.getAbsorptionAmount() - golden) < 0.01F, "its golden hearts took the blow: " + whole.getAbsorptionAmount()
			+ " of " + golden);
		// a moment after another blow it is the same
		com.iafabric.boss.BossLimits.strike(level, whole, boss, 0.05F);
		whole.setHealth(max);
		com.iafabric.boss.BossLimits.doom(level, whole, boss);
		helper.assertTrue(Math.abs(whole.getHealth() - max * 0.1F) < 0.6F, "right after another blow it took less: " + whole.getHealth());
		// it kills who was hurt already; a totem of undying saves them
		var saved = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(6, 2, 2), EntitySpawnReason.COMMAND);
		saved.setNoAi(true);
		saved.setHealth(max * 0.5F);
		saved.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
		com.iafabric.boss.BossLimits.doom(level, saved, boss);
		var doomed = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(7, 2, 7), EntitySpawnReason.COMMAND);
		doomed.setNoAi(true);
		doomed.setHealth(max * 0.5F);
		com.iafabric.boss.BossLimits.doom(level, doomed, boss);
		helper.assertTrue(saved.isAlive(), "the totem did not save from the cold");
		helper.assertTrue(!doomed.isAlive(), "a half-hurt one lived through nine tenths of its life");
		log("frost doom: a quarter of life through netherite and Resistance V; the longest calls take nine tenths, golden hearts kept; "
			+ "they kill only the hurt, and a totem saves");
		boss.discard();
		armored.discard();
		whole.discard();
		saved.discard();
		helper.succeed();
	}

	/**
	 * A villager without work takes the Bestiary Lectern near it and becomes a scribe (its job site is in the tag). By
	 * day: at night a villager rests and goes to no work.
	 */
	@GameTest(maxTicks = 2400, environment = "iceandfire_fabric:day")
	public void scribeTakesTheLectern(GameTestHelper helper) {
		var level = helper.getLevel();
		Villager villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(2, 2, 2), EntitySpawnReason.COMMAND);
		BlockPos[] lectern = new BlockPos[1];
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(villager.onGround(), "the villager did not land"))
			.thenExecute(() -> {
				// on the ground beside it (where it can stand next to it)
				lectern[0] = villager.blockPosition().offset(2, 0, 2);
				level.setBlock(lectern[0], com.iafabric.block.IafBlockRegistry.LECTERN.defaultBlockState(), 3);
			})
			.thenWaitUntil(() -> {
				if (!villager.getVillagerData().profession().is(IafVillagerRegistry.SCRIBE_KEY)) {
					var poi = level.getPoiManager().getType(lectern[0]);
					var brain = villager.getBrain();
					helper.assertTrue(false, "the villager did not take the lectern: " + villager.getVillagerData().profession().unwrapKey()
						+ ", poi there " + poi.map(h -> h.unwrapKey() + " tagged " + h.is(net.minecraft.tags.PoiTypeTags.ACQUIRABLE_JOB_SITE)).orElse("none")
						+ ", potential job site " + brain.getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.POTENTIAL_JOB_SITE)
						+ ", job site " + brain.getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE) + ", baby " + villager.isBaby()
						+ ", at " + villager.blockPosition().subtract(helper.absolutePos(BlockPos.ZERO)).toShortString());
				}
			})
			.thenExecute(() -> {
				log("scribe: a villager took the Bestiary Lectern and became a scribe");
				villager.discard();
			})
			.thenSucceed();
	}

	/** A pixie at its last level takes no more dust (the slot keeps it). */
	@GameTest(maxTicks = 40, environment = "iceandfire_fabric:normal")
	public void pixieDustAtMax(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(3, 2, 3));
		player.snapTo(at.x, at.y, at.z, 0, 0);
		com.iafabric.entity.EntityPixie pixie = helper.spawn(IafEntityRegistry.PIXIE, new BlockPos(4, 3, 4), EntitySpawnReason.COMMAND);
		pixie.setColor(2);
		pixie.tame(player);
		helper.assertTrue(com.iafabric.pixie.PixieBonding.bond(player, 2), "the pixie could not be bonded");
		com.iafabric.pixie.PixieBonding.addXp(player, 2, 15990);
		var slot = new net.minecraft.world.SimpleContainer(1);
		slot.setItem(0, new ItemStack(IafItemRegistry.PIXIE_DUST, 64));
		com.iafabric.pixie.PixieBonding.feed(player, 2, slot);
		var bond = com.iafabric.pixie.PixieBonding.get(player).get(2);
		helper.assertTrue(bond.level == 5 && slot.getItem(0).getCount() == 63, "a pixie 10 xp short of its last level took "
			+ (64 - slot.getItem(0).getCount()) + " dust (level " + bond.level + ")");
		com.iafabric.pixie.PixieBonding.feed(player, 2, slot);
		helper.assertTrue(slot.getItem(0).getCount() == 63, "a pixie at its last level took dust: " + slot.getItem(0).getCount() + " left");
		log("pixie dust: one dust to its last level, then none taken");
		player.level().getServer().getPlayerList().remove(player);
		pixie.discard();
		helper.succeed();
	}

	/**
	 * A dragon's body parts are in the world (head, neck, wings, tail): a blow on one reaches the dragon, a wall or a
	 * fall does not.
	 */
	@GameTest(maxTicks = 60, environment = "iceandfire_fabric:normal")
	public void dragonParts(GameTestHelper helper) {
		var level = helper.getLevel();
		EntityDragonBase dragon = IafEntityRegistry.ICE_DRAGON.create(level, EntitySpawnReason.COMMAND);
		BlockPos at = helper.absolutePos(new BlockPos(5, 2, 5));
		dragon.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
		dragon.setAgeInDays(0);
		dragon.growDragon(75);
		dragon.setNoAi(true);
		level.addFreshEntity(dragon);
		var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1), EntitySpawnReason.COMMAND);
		zombie.setNoAi(true);
		helper.runAfterDelay(10, () -> {
			var parts = level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, dragon.getBoundingBox().inflate(30),
				part -> dragon.getUUID().equals(part.getParentId()));
			helper.assertTrue(parts.size() >= 10, "the dragon has " + parts.size() + " body parts in the world");
			float full = dragon.getHealth();
			var wing = parts.get(0);
			helper.assertTrue(!wing.hurtServer(level, level.damageSources().inWall(), 5F), "a part took a wall's damage");
			wing.hurtServer(level, level.damageSources().mobAttack(zombie), 10F);
			helper.assertTrue(dragon.getHealth() < full, "a blow on a part did not reach the dragon");
			log("dragon parts: " + parts.size() + " in the world, a blow on one reaches the dragon, a wall does not");
			dragon.discard();
			zombie.discard();
			helper.succeed();
		});
	}

	/** An ice dragon whose owner holds one of Cryona's weapons freezes solid, Frost Ward or not. */
	@GameTest(maxTicks = 40, environment = "iceandfire_fabric:normal")
	public void cryonasWeaponsFreeze(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer player = survivalPlayer(helper);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2));
		player.snapTo(at.x, at.y, at.z, 0, 0);
		EntityDragonBase dragon = IafEntityRegistry.ICE_DRAGON.create(level, EntitySpawnReason.COMMAND);
		dragon.snapTo(at.x + 3, at.y, at.z + 3, 0F, 0F);
		dragon.setNoAi(true);
		level.addFreshEntity(dragon);
		dragon.tame(player);
		var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(6, 2, 6), EntitySpawnReason.COMMAND);
		zombie.setNoAi(true);
		zombie.addEffect(new MobEffectInstance(IafEffects.FROST_WARD, 400));
		BossCold.breathFreeze(zombie, dragon, 100);
		helper.assertTrue(!FrozenProperties.isFrozen(zombie), "the ward did not hold without Cryona's weapons");
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(IafItemRegistry.DREAD_QUEEN_SWORD));
		BossCold.breathFreeze(zombie, dragon, 100);
		helper.assertTrue(FrozenProperties.isFrozen(zombie), "Cryona's sword in its owner's hand did not make the ice freeze solid");
		log("Cryona's weapons: with her sword in hand, your ice dragon's ice freezes through the Frost Ward");
		player.level().getServer().getPlayerList().remove(player);
		dragon.discard();
		zombie.discard();
		helper.succeed();
	}

	/**
	 * A dead dragon's body stays to be harvested but is no foe: what went for it lets it be, and a bonded pixie does not
	 * strike it (it struck it while it lived).
	 */
	@GameTest(maxTicks = 100, environment = "iceandfire_fabric:normal")
	public void deadDragonIsNoFoe(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer player = survivalPlayer(helper);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
		player.snapTo(at.x, at.y, at.z, 0, 0);
		EntityDragonBase dragon = IafEntityRegistry.ICE_DRAGON.create(level, EntitySpawnReason.COMMAND);
		BlockPos spot = helper.absolutePos(new BlockPos(6, 2, 6));
		dragon.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0F, 0F);
		dragon.setAgeInDays(0);
		dragon.growDragon(30);
		dragon.setNoAi(true);
		level.addFreshEntity(dragon);
		com.iafabric.entity.EntityPixie pixie = helper.spawn(IafEntityRegistry.PIXIE, new BlockPos(3, 3, 3), EntitySpawnReason.COMMAND);
		pixie.setColor(0);
		pixie.tame(player);
		pixie.setNoAi(true);
		var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 2, 7), EntitySpawnReason.COMMAND);
		zombie.setNoAi(true);
		zombie.setTarget(dragon);
		player.setLastHurtMob(dragon);
		helper.assertTrue(com.iafabric.pixie.PixieCombat.foe(level, player, pixie) == dragon, "the pixie did not see the living dragon its player hit as a foe");
		dragon.hurtServer(level, level.damageSources().mobAttack(zombie), 100000F);
		helper.startSequence()
			.thenWaitUntil(() -> helper.assertTrue(dragon.isModelDead(), "the dragon did not die to a body"))
			.thenWaitUntil(() -> helper.assertTrue(zombie.getTarget() == null, "a mob still goes for the dragon's body"))
			.thenExecute(() -> {
				helper.assertTrue(dragon.isAlive() && !dragon.canBeSeenAsEnemy(), "the body is gone or still a foe: alive " + dragon.isAlive()
					+ ", foe " + dragon.canBeSeenAsEnemy());
				player.setLastHurtMob(dragon);
				helper.assertTrue(com.iafabric.pixie.PixieCombat.foe(level, player, pixie) == null, "the pixie still strikes the dragon's body");
				log("dead dragon: its body stays to be harvested, no mob goes for it and the pixie leaves it be");
				player.level().getServer().getPlayerList().remove(player);
				dragon.discard();
				pixie.discard();
				zombie.discard();
			})
			.thenSucceed();
	}

	/** The mod's copper is the game's now: an old world's iceandfire_fabric copper is read as the game's, not lost. */
	@GameTest(maxTicks = 20)
	public void oldCopperIsVanilla(GameTestHelper helper) {
		var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
		helper.assertTrue(items.getValue(net.minecraft.resources.Identifier.parse("iceandfire_fabric:copper_ingot")) == net.minecraft.world.item.Items.COPPER_INGOT,
			"an old copper ingot is not the game's");
		helper.assertTrue(items.getValue(net.minecraft.resources.Identifier.parse("iceandfire_fabric:armor_copper_metal_helmet")) == net.minecraft.world.item.Items.COPPER_HELMET,
			"an old copper helmet is not the game's");
		helper.assertTrue(blocks.getValue(net.minecraft.resources.Identifier.parse("iceandfire_fabric:copper_ore")) == net.minecraft.world.level.block.Blocks.COPPER_ORE,
			"old copper ore is not the game's");
		helper.assertTrue(items.get(net.minecraft.resources.Identifier.parse("iceandfire_fabric:copper_nugget")).isPresent(), "an old copper nugget is lost");
		helper.assertTrue(items.getValue(net.minecraft.resources.Identifier.parse("iceandfire_fabric:nothing_like_this")) == net.minecraft.world.item.Items.AIR,
			"an unknown id is not air");
		log("old copper: the mod's copper ingot, nugget, ore and armor are read as the game's");
		helper.succeed();
	}

	/**
	 * The mod's first id was iafabric: an older world's blocks, items, creatures and structures under it are read as the
	 * mod's own (its copper as the game's), and what worlds keep (saved data, players' data) is still kept under it.
	 */
	@GameTest(maxTicks = 20)
	public void oldIdsAreRead(GameTestHelper helper) {
		var items = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		var blocks = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
		var entities = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
		java.util.function.Function<String, net.minecraft.resources.Identifier> old = path -> net.minecraft.resources.Identifier.fromNamespaceAndPath("iafabric", path);
		helper.assertTrue(items.getValue(old.apply("dragonbone")) == com.iafabric.item.IafItemRegistry.DRAGON_BONE, "an old dragon bone is lost");
		helper.assertTrue(items.get(old.apply("dragonbone")).isPresent(), "an old dragon bone is not found");
		helper.assertTrue(blocks.getValue(old.apply("dragon_bone_block")) == com.iafabric.block.IafBlockRegistry.DRAGON_BONE_BLOCK, "an old bone block is lost");
		helper.assertTrue(entities.getValue(old.apply("pixie")) == com.iafabric.entity.IafEntityRegistry.PIXIE, "an old pixie is lost");
		helper.assertTrue(items.getValue(old.apply("copper_ingot")) == net.minecraft.world.item.Items.COPPER_INGOT, "an old copper ingot is not the game's");
		var structures = helper.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
		helper.assertTrue(structures.get(old.apply("dragon_roost")).isPresent(), "an old dragon roost is lost");
		helper.assertTrue(com.iafabric.IafMod.stored("exiled_pixie").getNamespace().equals("iafabric"), "players' data is not kept under the first id");
		helper.assertTrue(com.iafabric.IafMod.MOD_ID.equals("iceandfire_fabric"), "the mod's id is not iceandfire_fabric");
		log("old ids: iafabric dragon bone, bone block, pixie, roost read as the mod's own; old copper as the game's");
		helper.succeed();
	}

	/** The dragonbone bow draws and shoots the game's arrows too (it took only dragonbone arrows: with others it did not even draw). */
	@GameTest(maxTicks = 20, environment = "iceandfire_fabric:normal")
	public void dragonBowShootsArrows(GameTestHelper helper) {
		var level = helper.getLevel();
		ServerPlayer player = survivalPlayer(helper);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2));
		player.snapTo(at.x, at.y, at.z, 0, 0);
		ItemStack bow = new ItemStack(IafItemRegistry.DRAGON_BOW);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bow);
		player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.ARROW, 4));
		var drawn = bow.use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
		helper.assertTrue(drawn.consumesAction(), "the dragonbone bow does not draw with the game's arrows: " + drawn);
		bow.releaseUsing(level, player, bow.getUseDuration(player) - 20);
		helper.runAfterDelay(2, () -> {
			var shot = level.getEntitiesOfClass(net.minecraft.world.entity.projectile.arrow.AbstractArrow.class, player.getBoundingBox().inflate(24));
			helper.assertTrue(!shot.isEmpty(), "the dragonbone bow shot no arrow");
			log("dragonbone bow: drawn and shot with the game's arrows (" + shot.size() + " in the air)");
			shot.forEach(net.minecraft.world.entity.Entity::discard);
			player.level().getServer().getPlayerList().remove(player);
			helper.succeed();
		});
	}

	/**
	 * A boss's throw reaches who it throws: a player's own game is sent their new speed (26.3 sends it to them only with
	 * syncVelocity, needsSync tells the others), and it is set, not added to what the server last kept for them (it
	 * piled up: a body pushing a player every tick would have flung them); a creature's is added to its own.
	 */
	@GameTest(maxTicks = 20, environment = "iceandfire_fabric:normal")
	public void bossThrowsReachPlayers(GameTestHelper helper) {
		ServerPlayer player = survivalPlayer(helper);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 2));
		player.snapTo(at.x, at.y, at.z, 0, 0);
		player.setDeltaMovement(3, 0, 0);
		player.syncVelocity = false;
		com.iafabric.boss.BossLimits.shove(player, 0.4, 0.2, 0);
		helper.assertTrue(player.syncVelocity, "a player's throw is not sent to their game");
		helper.assertTrue(player.getDeltaMovement().distanceTo(new net.minecraft.world.phys.Vec3(0.4, 0.2, 0)) < 1.0E-6,
			"a player's throw piled on what the server kept: " + player.getDeltaMovement());
		var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(4, 2, 4), EntitySpawnReason.COMMAND);
		zombie.setNoAi(true);
		zombie.setDeltaMovement(0.1, 0, 0);
		com.iafabric.boss.BossLimits.shove(zombie, 0.4, 0.2, 0);
		helper.assertTrue(zombie.getDeltaMovement().distanceTo(new net.minecraft.world.phys.Vec3(0.5, 0.2, 0)) < 1.0E-6,
			"a creature's throw was not added to its own speed: " + zombie.getDeltaMovement());
		log("boss throws: a player's speed is set and sent to their game, a creature's is added to");
		player.level().getServer().getPlayerList().remove(player);
		zombie.discard();
		helper.succeed();
	}

	/** A test player in survival, joined like the test's own mock player (which is always in creative). */
	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		var server = helper.getLevel().getServer();
		var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-hero"), false);
		var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
		var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
		new io.netty.channel.embedded.EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(connection, player, cookie);
		player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
		return player;
	}

	/**
	 * A dragon's parts hold to its bones as its model poses them (DragonBones), in its three kinds: standing, sitting (the
	 * lightning dragon raises its neck high) and hovering as its wings beat. Its head part holds its snout, each piece of
	 * its neck its own middle, its last tail part its club, its wings' parts the tips of their fingers; and its wings'
	 * parts are no bigger than its wings need (they were tall columns over its back, the same for the three kinds).
	 */
	@GameTest(maxTicks = 420, environment = "iceandfire_fabric:normal")
	public void dragonPartsFit(GameTestHelper helper) {
		var level = helper.getLevel();
		var types = new net.minecraft.world.entity.EntityType<?>[]{IafEntityRegistry.FIRE_DRAGON, IafEntityRegistry.ICE_DRAGON, IafEntityRegistry.LIGHTNING_DRAGON};
		String[] kinds = {"fire", "ice", "lightning"};
		EntityDragonBase[] dragon = new EntityDragonBase[1];
		String[] state = {"standing"};
		// (a player by them: with none near, a dragon's parts keep their last pose)
		var watcher = survivalPlayer(helper);
		var by = helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, -8));
		watcher.snapTo(by.x, by.y, by.z, 0F, 0F);
		helper.onEachTick(() -> {
			// (no mind of its own: what it is doing is kept up)
			if (dragon[0] != null && state[0].startsWith("hovering")) {
				dragon[0].setNoGravity(true);
				dragon[0].setHovering(true);
			}
		});
		for (int k = 0; k < 3; k++) {
			int which = k;
			int start = k * 130;
			helper.runAfterDelay(start + 1, () -> {
				EntityDragonBase made = (EntityDragonBase) types[which].create(level, EntitySpawnReason.COMMAND);
				BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
				made.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
				made.setAgeInDays(0);
				made.growDragon(40);
				made.setNoAi(true);
				level.addFreshEntity(made);
				dragon[0] = made;
				state[0] = "standing";
			});
			helper.runAfterDelay(start + 20, () -> {
				fit(helper, dragon[0], kinds[which], state[0], 14);
				// (told to sit: it sits only so)
				dragon[0].setCommand(1);
				state[0] = "sitting";
			});
			helper.runAfterDelay(start + 64, () -> {
				fit(helper, dragon[0], kinds[which], state[0], 14);
				dragon[0].setCommand(0);
				state[0] = "hovering";
			});
			helper.runAfterDelay(start + 112, () -> fit(helper, dragon[0], kinds[which], state[0] + " (one beat)", 28));
			helper.runAfterDelay(start + 119, () -> fit(helper, dragon[0], kinds[which], state[0] + " (another beat)", 28));
			helper.runAfterDelay(start + 125, () -> {
				dragon[0].discard();
				dragon[0] = null;
			});
		}
		helper.runAfterDelay(400, () -> {
			level.getServer().getPlayerList().remove(watcher);
			helper.succeed();
		});
	}

	/** Its parts around its bones now: a few points of its bones in their parts, and its wings' boxes not too big. */
	private static void fit(GameTestHelper helper, EntityDragonBase dragon, String kind, String state, double most) {
		var level = helper.getLevel();
		var parts = level.getEntitiesOfClass(com.iafabric.entity.EntityDragonPart.class, dragon.getBoundingBox().inflate(60),
			p -> dragon.getUUID().equals(p.getParentId()) && p.part() >= 0);
		helper.assertTrue(parts.size() == com.iafabric.entity.DragonBones.PARTS, "the " + kind + " dragon " + state + " has " + parts.size() + " parts");
		AABB[] box = new AABB[com.iafabric.entity.DragonBones.PARTS];
		for (var part : parts) {
			box[part.part()] = part.getBoundingBox().inflate(0.02);
		}
		var at = dragon.position();
		holds(helper, box, com.iafabric.entity.DragonBones.HEAD, com.iafabric.entity.DragonBones.HEAD, at,
			com.iafabric.entity.DragonBones.middle(dragon, "HeadFront"), kind + " " + state + ": its snout");
		for (int k = 0; k < 3; k++) {
			holds(helper, box, com.iafabric.entity.DragonBones.NECK + k, com.iafabric.entity.DragonBones.NECK + k, at,
				com.iafabric.entity.DragonBones.middle(dragon, "Neck" + (k + 1)), kind + " " + state + ": its neck's piece " + (k + 1));
		}
		holds(helper, box, com.iafabric.entity.DragonBones.TAIL + 5, com.iafabric.entity.DragonBones.TAIL + 5, at,
			com.iafabric.entity.DragonBones.middle(dragon, "Club"), kind + " " + state + ": its club");
		for (String side : new String[]{"R", "L"}) {
			int first = side.equals("R") ? com.iafabric.entity.DragonBones.WING_RIGHT : com.iafabric.entity.DragonBones.WING_LEFT;
			// the tips of its first and its last finger (the first's end is named apart in the fire dragon's right wing)
			var tip = com.iafabric.entity.DragonBones.point(dragon, "finger" + side + "1.2", 0, 16.5F, 0);
			if (tip == null) {
				tip = com.iafabric.entity.DragonBones.point(dragon, "finger" + side + ".2", 0, 16.5F, 0);
			}
			holds(helper, box, first, first + 5, at, tip, kind + " " + state + ": the tip of its " + side + " wing's first finger");
			holds(helper, box, first, first + 5, at, com.iafabric.entity.DragonBones.point(dragon, "finger" + side + "4.2", 0, 14.5F, 0),
				kind + " " + state + ": the tip of its " + side + " wing's last finger");
		}
		double k = dragon.getRenderSize() / 3.0;
		double wings = 0;
		for (int i = com.iafabric.entity.DragonBones.WING_RIGHT; i < com.iafabric.entity.DragonBones.PARTS; i++) {
			wings += box[i].getXsize() * box[i].getYsize() * box[i].getZsize();
		}
		double head = box[com.iafabric.entity.DragonBones.HEAD].getYsize() + box[com.iafabric.entity.DragonBones.HEAD].minY - at.y;
		log(String.format(java.util.Locale.ROOT, "dragon parts %s %s: its wings' boxes %.1f of its size cubed (at most %.0f), its head's top %.2f of its size up",
			kind, state, wings / (k * k * k), most, head / k));
		helper.assertTrue(wings / (k * k * k) <= most, "the " + kind + " dragon's wings' boxes are too big " + state + ": " + wings / (k * k * k));
	}

	private static void holds(GameTestHelper helper, AABB[] box, int from, int to, net.minecraft.world.phys.Vec3 at, org.joml.Vector3f point, String what) {
		helper.assertTrue(point != null, what + ": no such bone");
		var p = at.add(point.x(), point.y(), point.z());
		for (int i = from; i <= to; i++) {
			if (box[i].contains(p)) {
				return;
			}
		}
		helper.assertTrue(false, what + " at " + String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", point.x(), point.y(), point.z()) + " is in none of its parts");
	}

	/**
	 * An arrow in a cyclops' eye blinds it (it roars, sees little and is slow). One through the back of its head, where
	 * its eye's box reaches, strikes its head and does not: only a shot at its face does. Its eye's box stays on its face
	 * as it turns its head.
	 */
	@GameTest(maxTicks = 120, environment = "iceandfire_fabric:normal")
	public void cyclopsEye(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, 4.5));
		var cyclops = IafEntityRegistry.CYCLOPS.create(level, EntitySpawnReason.COMMAND);
		cyclops.snapTo(pos.x, pos.y, pos.z, 0F, 0F);
		cyclops.yBodyRot = 0F;
		cyclops.yHeadRot = 0F;
		cyclops.setNoAi(true);
		level.addFreshEntity(cyclops);
		// (its eye part takes its place on its face from its 11th tick)
		helper.runAfterDelay(25, () -> {
			var eye = cyclops.eyeEntity;
			helper.assertTrue(eye != null, "the cyclops has no eye part");
			var at = eye.getBoundingBox().getCenter();
			// from behind it, at the top of its eye's box (over its body's: an arrow finds what is a little around its way),
			// the arrow strikes its eye's box first, through the back of its head
			var top = new net.minecraft.world.phys.Vec3(at.x, eye.getBoundingBox().maxY - 0.05, at.z);
			var from = top.add(0, 0, -8);
			var to = top.add(0, 0, 2);
			var behind = new net.minecraft.world.entity.projectile.arrow.Arrow(level, from.x, from.y, from.z, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
			behind.setDeltaMovement(0, 0, 3);
			var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, behind, from, to, new AABB(from, to).inflate(1),
				e -> e == eye || e == cyclops);
			helper.assertTrue(hit != null && hit.getEntity() == eye, "an arrow at the back of its head strikes " + (hit == null ? "nothing" : hit.getEntity()));
			float life = cyclops.getHealth();
			eye.hurtServer(level, level.damageSources().arrow(behind, null), 4F);
			helper.assertTrue(!cyclops.isBlinded(), "an arrow through the back of its head blinded the cyclops");
			helper.assertTrue(cyclops.getHealth() < life, "an arrow through the back of its head did not hurt the cyclops");
			// from ahead, straight at its eye
			from = at.add(0, 0, 6);
			to = at.add(0, 0, -2);
			var ahead = new net.minecraft.world.entity.projectile.arrow.Arrow(level, from.x, from.y, from.z, new ItemStack(net.minecraft.world.item.Items.ARROW), null);
			ahead.setDeltaMovement(0, 0, -3);
			hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, ahead, from, to, new AABB(from, to).inflate(1),
				e -> e == eye || e == cyclops);
			helper.assertTrue(hit != null && hit.getEntity() == eye, "an arrow at its eye strikes " + (hit == null ? "nothing" : hit.getEntity()));
			eye.hurtServer(level, level.damageSources().arrow(ahead, null), 4F);
			helper.assertTrue(cyclops.isBlinded(), "an arrow in its eye did not blind the cyclops");
			log("cyclops eye: an arrow through the back of its head hurts it (" + life + " -> " + cyclops.getHealth() + ") but does not blind it; one in its eye does");
			cyclops.yHeadRot = 60F;
		});
		helper.runAfterDelay(30, () -> {
			// its head turned: its eye's box is on its face there (where it looks, a block from its middle)
			var eye = cyclops.eyeEntity.getBoundingBox().getCenter().subtract(cyclops.position());
			var look = net.minecraft.world.entity.Entity.calculateViewVector(0, cyclops.yHeadRot);
			double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, new net.minecraft.world.phys.Vec3(eye.x, 0, eye.z).normalize().dot(look)))));
			log("cyclops eye: its head turned to " + Math.round(cyclops.yHeadRot) + ", its eye's box " + Math.round(angle) + " degrees from where it looks");
			helper.assertTrue(angle < 5, "its eye's box is " + angle + " degrees from where its head looks");
			cyclops.discard();
			helper.succeed();
		});
	}

	/**
	 * A dragon's parts go into the world where the dragon is: they are made with it, before it is set where it is, and
	 * went into the world there, at the world's middle (far out, as in a world played far from it, that land is not
	 * loaded and they never moved: no head, neck, wing or tail to strike). One left far behind comes back to it.
	 */
	@GameTest(maxTicks = 40, environment = "iceandfire_fabric:normal")
	public void dragonPartsFarAway(GameTestHelper helper) {
		var level = helper.getLevel();
		EntityDragonBase dragon = IafEntityRegistry.FIRE_DRAGON.create(level, EntitySpawnReason.COMMAND);
		BlockPos at = helper.absolutePos(new BlockPos(4, 2, 4));
		dragon.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0F, 0F);
		dragon.setAgeInDays(0);
		dragon.growDragon(40);
		dragon.setNoAi(true);
		level.addFreshEntity(dragon);
		var parts = dragon.getParts();
		// (as made: where the dragon was made, not where it is)
		double before = parts.stream().mapToDouble(p -> p.distanceTo(dragon)).max().orElse(-1);
		// with its first tick they go into the world
		dragon.updateParts();
		double after = parts.stream().mapToDouble(p -> p.distanceTo(dragon)).max().orElse(-1);
		// one left far behind (in land not loaded) comes back with the next
		var part = parts.get(0);
		part.setPos(dragon.getX() + 3000, part.getY(), dragon.getZ());
		dragon.updateParts();
		double back = part.distanceTo(dragon);
		log(String.format(java.util.Locale.ROOT, "dragon parts far away: %d parts, %.1f blocks from the dragon as made, %.2f as they went into the world; one left 3000 blocks behind came back to %.2f",
			parts.size(), before, after, back));
		dragon.discard();
		helper.assertTrue(parts.size() == com.iafabric.entity.DragonBones.PARTS, "the dragon has " + parts.size() + " parts");
		helper.assertTrue(after < 0.01, "its parts went into the world " + after + " blocks from the dragon");
		helper.assertTrue(back < 0.01, "a part left far behind did not come back to its dragon: " + back);
		helper.succeed();
	}
}
