package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/** Server checks: the mod's content registered, every creature can be made and lives a few ticks. Log lines start with [IAFTEST]. */
public class IafGameTests implements FabricGameTest {
	@BeforeBatch(batch = "rain")
	public void rainBatch(net.minecraft.server.level.ServerLevel level) {
		level.setWeatherParameters(0, 24000, true, false);
	}


	/** A right click on it, as the server takes one: interactAt first, then interact if that did nothing. */
	private static net.minecraft.world.InteractionResult click(Entity target, net.minecraft.world.entity.player.Player player) {
		var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
		var result = target.interactAt(player, target.position(), hand);
		return result.consumesAction() ? result : player.interactOn(target, hand);
	}

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFTEST] {}", message);
	}

	private static <T> long count(Iterable<T> registry, java.util.function.Function<T, String> namespace) {
		long n = 0;
		for (T t : registry) {
			if (IafMod.MOD_ID.equals(namespace.apply(t))) {
				n++;
			}
		}
		return n;
	}

	/**
	 * A right click on any of a grown dragon's parts (its body, head, neck, a wing, its tail) reaches the dragon as the
	 * server takes a click (interactAt, then interact when that did nothing): its owner gets on through each of them.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void rideThroughParts(GameTestHelper helper) {
		var level = helper.getLevel();
		var player = mockPlayer(helper);
		var dragon = com.iafabric.entity.IafEntityRegistry.FIRE_DRAGON.create(level);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 6.5));
		dragon.moveTo(at.x, at.y, at.z, 0, 0);
		level.addFreshEntity(dragon);
		dragon.growDragon(80);
		dragon.setTame(true, true);
		dragon.tame(player);
		dragon.updateParts();
		var parts = dragon.getParts();
		int[] which = {com.iafabric.entity.DragonBones.BODY, com.iafabric.entity.DragonBones.HEAD, com.iafabric.entity.DragonBones.NECK,
			com.iafabric.entity.DragonBones.WING_LEFT, com.iafabric.entity.DragonBones.TAIL};
		String[] names = {"body", "head", "neck", "wing", "tail"};
		StringBuilder got = new StringBuilder();
		boolean all = true;
		for (int i = 0; i < which.length; i++) {
			click(parts.get(which[i]), player);
			boolean on = player.getVehicle() == dragon;
			got.append(names[i]).append(' ').append(on).append(i < which.length - 1 ? ", " : "");
			all &= on;
			player.stopRiding();
		}
		log("a click on its parts lets its owner on: " + got);
		dragon.discard();
		helper.assertTrue(all, "a click on its parts did not let its owner on: " + got);
		helper.succeed();
	}

	/** Its owner gets on a grown dragon with a sword in hand (anything with no use on it), as with an empty hand. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void rideWithSword(GameTestHelper helper) {
		var level = helper.getLevel();
		var player = mockPlayer(helper);
		var dragon = com.iafabric.entity.IafEntityRegistry.FIRE_DRAGON.create(level);
		var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 4.5));
		dragon.moveTo(at.x, at.y, at.z, 0, 0);
		level.addFreshEntity(dragon);
		dragon.growDragon(80);
		dragon.setTame(true, true);
		dragon.tame(player);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
		dragon.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
		boolean on = player.getVehicle() == dragon;
		player.stopRiding();
		dragon.discard();
		if (!on) {
			throw new IllegalStateException("its owner did not get on it with a sword in hand");
		}
		helper.succeed();
	}

	/** Black Frost and Cryona are never turned to stone (a gorgon's gaze, her head, a cockatrice); other dragons can be. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void notStone(GameTestHelper helper) {
		var level = helper.getLevel();
		var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
		var frost = com.iafabric.world.structure.DreadPieces.dreadDragon(level, level.getRandom(), pos, 125, true);
		var dragon = com.iafabric.entity.IafEntityRegistry.ICE_DRAGON.create(level);
		var cryona = com.iafabric.entity.IafEntityRegistry.DREAD_QUEEN.create(level);
		boolean ok = frost.isBlackFrost() && !frost.canBeTurnedToStone() && dragon.canBeTurnedToStone()
			&& cryona instanceof com.iafabric.entity.util.IBlacklistedFromStatues q && !q.canBeTurnedToStone();
		frost.discard();
		if (!ok) {
			throw new IllegalStateException("Black Frost or Cryona can be turned to stone");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void registries(GameTestHelper helper) {
		long blocks = count(BuiltInRegistries.BLOCK, b -> BuiltInRegistries.BLOCK.getKey(b).getNamespace());
		long items = count(BuiltInRegistries.ITEM, i -> BuiltInRegistries.ITEM.getKey(i).getNamespace());
		long entities = count(BuiltInRegistries.ENTITY_TYPE, e -> BuiltInRegistries.ENTITY_TYPE.getKey(e).getNamespace());
		log("blocks=" + blocks + " items=" + items + " entities=" + entities);
		// the ids, for the asset tools (tools/gen_assets.py reads build/run/gameTest/iaf_registry.txt)
		StringBuilder dump = new StringBuilder();
		BuiltInRegistries.ITEM.keySet().stream().filter(id -> id.getNamespace().equals(IafMod.MOD_ID)).sorted()
			.forEach(id -> dump.append("item ").append(id.getPath()).append('\n'));
		BuiltInRegistries.BLOCK.keySet().stream().filter(id -> id.getNamespace().equals(IafMod.MOD_ID)).sorted()
			.forEach(id -> dump.append("block ").append(id.getPath()).append('\n'));
		BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(id -> id.getNamespace().equals(IafMod.MOD_ID)).sorted()
			.forEach(id -> dump.append("entity ").append(id.getPath()).append('\n'));
		try {
			java.nio.file.Files.writeString(java.nio.file.Path.of("iaf_registry.txt"), dump);
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
		helper.assertTrue(blocks > 100, "too few blocks: " + blocks);
		helper.assertTrue(items > 300, "too few items: " + items);
		helper.assertTrue(entities > 40, "too few entity types: " + entities);
		helper.succeed();
	}

	/**
	 * Every name the game shows for the mod's content is in every language the mod has: items, blocks, creatures,
	 * effects, professions, creative tabs, banner patterns, advancements and the death messages.
	 * (Texts the code and the sounds ask for: tools/check_lang.py.)
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void translations(GameTestHelper helper) {
		java.util.Map<String, java.util.Map<String, String>> langs = new java.util.LinkedHashMap<>();
		for (String code : List.of("en_us", "es_es", "es_ar", "es_cl", "es_ec", "es_mx", "de_de", "fr_fr", "ja_jp", "ko_kr",
			"pl_pl", "pt_br", "ru_ru", "uk_ua", "vi_vn", "zh_cn")) {
			langs.put(code, readLang("/assets/iceandfire_fabric/lang/" + code + ".json"));
		}
		java.util.Map<String, String> vanilla = readLang("/assets/minecraft/lang/en_us.json");
		java.util.Map<String, String> keys = new java.util.TreeMap<>();
		for (var item : BuiltInRegistries.ITEM) {
			var id = BuiltInRegistries.ITEM.getKey(item);
			if (IafMod.MOD_ID.equals(id.getNamespace())) {
				textKeys(keys, new net.minecraft.world.item.ItemStack(item).getHoverName(), "item " + id.getPath());
			}
		}
		for (var block : BuiltInRegistries.BLOCK) {
			var id = BuiltInRegistries.BLOCK.getKey(block);
			if (IafMod.MOD_ID.equals(id.getNamespace())) {
				textKeys(keys, block.getName(), "block " + id.getPath());
			}
		}
		for (var type : BuiltInRegistries.ENTITY_TYPE) {
			var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (IafMod.MOD_ID.equals(id.getNamespace())) {
				textKeys(keys, type.getDescription(), "entity " + id.getPath());
			}
		}
		for (var effect : BuiltInRegistries.MOB_EFFECT) {
			if (IafMod.MOD_ID.equals(BuiltInRegistries.MOB_EFFECT.getKey(effect).getNamespace())) {
				textKeys(keys, effect.getDisplayName(), "effect");
			}
		}
		for (var profession : BuiltInRegistries.VILLAGER_PROFESSION) {
			if (IafMod.MOD_ID.equals(BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).getNamespace())) {
				textKeys(keys, net.minecraft.network.chat.Component.translatable("entity.minecraft.villager." + profession.name()), "profession");
			}
		}
		for (var tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
			if (IafMod.MOD_ID.equals(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getNamespace())) {
				textKeys(keys, tab.getDisplayName(), "creative tab");
			}
		}
		var access = helper.getLevel().registryAccess();
		access.lookupOrThrow(net.minecraft.core.registries.Registries.BANNER_PATTERN).listElements()
			.filter(holder -> IafMod.MOD_ID.equals(holder.key().location().getNamespace()))
			.forEach(holder -> keys.putIfAbsent(holder.value().translationKey() + ".white", "banner pattern " + holder.key().location().getPath()));
		// the mod's damage picks one of three messages, with and without a killer (DamageSourceMixin)
		access.lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).listElements()
			.filter(holder -> IafMod.MOD_ID.equals(holder.key().location().getNamespace()))
			.forEach(holder -> {
				for (int i = 0; i < 3; i++) {
					keys.putIfAbsent("death.attack." + holder.value().msgId() + "." + i, "damage " + holder.key().location().getPath());
					keys.putIfAbsent("death.attack." + holder.value().msgId() + ".attacker_" + i, "damage " + holder.key().location().getPath());
				}
			});
		for (var advancement : helper.getLevel().getServer().getAdvancements().getAllAdvancements()) {
			if (IafMod.MOD_ID.equals(advancement.id().getNamespace())) {
				advancement.value().display().ifPresent(display -> {
					textKeys(keys, display.getTitle(), "advancement " + advancement.id().getPath());
					textKeys(keys, display.getDescription(), "advancement " + advancement.id().getPath());
				});
			}
		}
		List<String> missing = new ArrayList<>();
		keys.forEach((key, source) -> {
			if (vanilla.containsKey(key)) {
				return;
			}
			langs.forEach((code, lang) -> {
				if (!lang.containsKey(key)) {
					missing.add(code + " " + key + " (" + source + ")");
				}
			});
		});
		missing.forEach(line -> log("no text: " + line));
		log("texts checked: " + keys.size() + ", missing: " + missing.size());
		helper.assertTrue(missing.isEmpty(), missing.size() + " texts without translation, first: " + (missing.isEmpty() ? "" : missing.get(0)));
		helper.succeed();
	}

	private static void textKeys(java.util.Map<String, String> keys, net.minecraft.network.chat.Component text, String source) {
		if (text.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable) {
			keys.putIfAbsent(translatable.getKey(), source);
			for (Object arg : translatable.getArgs()) {
				if (arg instanceof net.minecraft.network.chat.Component component) {
					textKeys(keys, component, source);
				}
			}
		}
		for (var sibling : text.getSiblings()) {
			textKeys(keys, sibling, source);
		}
	}

	private static java.util.Map<String, String> readLang(String path) {
		java.util.Map<String, String> lang = new java.util.HashMap<>();
		try (var in = IafGameTests.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("no " + path);
			}
			var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
			json.entrySet().forEach(entry -> lang.put(entry.getKey(), entry.getValue().getAsString()));
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
		return lang;
	}

	/**
	 * Every creature and projectile of the mod is saved and loaded back as its chunk does, without a problem
	 * (a dread lich's skull could not be saved, a myrmex queen without a name not loaded: found by the Mod Tester).
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
	public void entitiesSaveAndLoad(GameTestHelper helper) {
		List<String> failed = new ArrayList<>();
		var access = helper.getLevel().registryAccess();
		int tried = 0;
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
			if (!IafMod.MOD_ID.equals(id.getNamespace())) {
				continue;
			}
			try {
				Entity entity = helper.spawn(type, new BlockPos(1, 2, 1));
				var output = entity.saveWithoutId(new net.minecraft.nbt.CompoundTag());
				Entity copy = type.create(helper.getLevel());
				copy.load(output);
				entity.discard();
				tried++;
			} catch (Throwable t) {
				failed.add(id + ": " + t);
				IafMod.LOGGER.error("[IAFTEST] save/load failed for {}", id, t);
			}
		}
		log("saved and loaded " + tried + " entity types, failed " + failed);
		helper.assertTrue(failed.isEmpty(), "entities that do not save or load: " + failed);
		helper.succeed();
	}

	/** Every entity type of the mod is created, added and ticked for 20 ticks without an exception. */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void everyEntityLives(GameTestHelper helper) {
		List<Entity> spawned = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (!IafMod.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
				continue;
			}
			try {
				Entity entity = helper.spawn(type, new BlockPos(1, 2, 1));
				spawned.add(entity);
			} catch (Throwable t) {
				failed.add(BuiltInRegistries.ENTITY_TYPE.getKey(type) + ": " + t);
				IafMod.LOGGER.error("[IAFTEST] spawn failed for {}", BuiltInRegistries.ENTITY_TYPE.getKey(type), t);
			}
		}
		log("spawned " + spawned.size() + " entity types, failed " + failed.size());
		helper.runAfterDelay(20, () -> {
			for (Entity entity : spawned) {
				if (entity.isRemoved() && entity instanceof LivingEntity living && living.isAlive()) {
					failed.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + " removed");
				}
			}
			for (Entity entity : spawned) {
				entity.discard();
			}
			helper.assertTrue(failed.isEmpty(), "entities failed: " + failed);
			helper.succeed();
		});
	}

	/** A dragon of each kind grows to stage 3 or more: more health, and a bigger hitbox after a tick. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
    public void dragonsGrow(GameTestHelper helper) {
        List<EntityDragonBase> dragons = new ArrayList<>();
        for (EntityType<? extends EntityDragonBase> type : List.of(IafEntityRegistry.FIRE_DRAGON, IafEntityRegistry.ICE_DRAGON, IafEntityRegistry.LIGHTNING_DRAGON)) {
            EntityDragonBase dragon = helper.spawn(type, new BlockPos(2, 2, 2));
            dragon.setNoAi(true);
            // (from no age: it is born with an age by chance, and at 51 days a dragon is still under 2 blocks wide)
            dragon.setAgeInDays(0);
            dragon.growDragon(60);
            dragons.add(dragon);
        }
        helper.runAfterDelay(3, () -> {
            for (EntityDragonBase dragon : dragons) {
                log(BuiltInRegistries.ENTITY_TYPE.getKey(dragon.getType()) + " stage=" + dragon.getDragonStage() + " age=" + dragon.getAgeInDays()
                    + " health=" + dragon.getMaxHealth() + " width=" + dragon.getBbWidth() + " height=" + dragon.getBbHeight());
                helper.assertTrue(dragon.getDragonStage() >= 3, "dragon did not grow: " + dragon.getDragonStage());
                helper.assertTrue(dragon.getMaxHealth() > 50, "dragon health too low: " + dragon.getMaxHealth());
                helper.assertTrue(dragon.getBbWidth() > 2.0F, "dragon hitbox did not grow: " + dragon.getBbWidth());
                dragon.discard();
            }
            helper.succeed();
        });
    }

    /**
     * A dead dragon is harvested one right click at a time (blood with a bottle, then scales, bones, flesh,
     * its heart, last its skull); one click through two paths in the same tick counts once.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void deadDragonHarvest(GameTestHelper helper) {
        EntityDragonBase dragon = helper.spawn(IafEntityRegistry.FIRE_DRAGON, new BlockPos(2, 2, 2));
        dragon.growDragon(60);
        dragon.setModelDead(true);
        dragon.setDeathStage(0);
        net.minecraft.world.entity.player.Player player = mockPlayer(helper);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE, 4));
        int lastStage = Math.min(dragon.getAgeInDays() / 5, 25);
        List<String> got = new ArrayList<>();
        int[] clicks = {0};
        helper.onEachTick(() -> {
            if (dragon.isRemoved() || clicks[0] > lastStage + 5) {
                return;
            }
            clicks[0]++;
            if (clicks[0] == 3) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            }
            int before = dragon.getDeathStage();
            click(dragon, player);
            click(dragon, player);
            helper.assertTrue(dragon.isRemoved() || dragon.getDeathStage() == before + 1, "one click must harvest once: " + before + " -> " + dragon.getDeathStage());
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(dragon.isRemoved(), "dragon not harvested to the end yet, stage " + dragon.getDeathStage() + "/" + lastStage);
            for (net.minecraft.world.entity.item.ItemEntity item : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, dragon.getBoundingBox().inflate(16))) {
                got.add(BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath() + "x" + item.getItem().getCount());
            }
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (!player.getInventory().getItem(i).isEmpty()) {
                    got.add("inv:" + BuiltInRegistries.ITEM.getKey(player.getInventory().getItem(i).getItem()).getPath());
                }
            }
            log("harvested a stage " + dragon.getDragonStage() + " dead dragon in " + clicks[0] + " clicks: " + got);
            helper.assertTrue(got.stream().anyMatch(n -> n.contains("fire_dragon_blood")), "no blood from the bottle");
            helper.assertTrue(got.stream().anyMatch(n -> n.contains("dragon_skull")), "no skull at the end");
            helper.assertTrue(got.stream().anyMatch(n -> n.contains("dragonscales") || n.contains("dragonbone") || n.contains("flesh")), "no scales, bones or flesh");
        });
    }

    /** Every damage kind of the mod exists (the breaths crashed the game when one was missing) and hurts. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
    public void damageTypes(GameTestHelper helper) {
        List<String> kinds = List.of("dragon_fire", "dragon_ice", "dragon_lightning", "gorgon");
        for (int i = 0; i < kinds.size(); i++) {
            // One sheep each: a sheep just hurt ignores the next hit for a moment.
            net.minecraft.world.entity.animal.Sheep sheep = helper.spawn(net.minecraft.world.entity.EntityType.SHEEP, new BlockPos(1 + i, 2, 2));
            var source = switch (i) {
                case 0 -> com.iafabric.misc.IafDamageRegistry.causeDragonFireDamage(sheep);
                case 1 -> com.iafabric.misc.IafDamageRegistry.causeDragonIceDamage(sheep);
                case 2 -> com.iafabric.misc.IafDamageRegistry.causeDragonLightningDamage(sheep);
                default -> com.iafabric.misc.IafDamageRegistry.causeGorgonDamage(sheep);
            };
            float before = sheep.getHealth();
            sheep.hurt(source, 1.0F);
            helper.assertTrue(sheep.getHealth() < before, kinds.get(i) + " did not hurt");
            helper.assertTrue(source.getMsgId().equals(kinds.get(i)), "wrong damage kind " + source.getMsgId());
            log(source.getMsgId() + ": " + source.getLocalizedDeathMessage(sheep).getString());
            if (i == 3) {
                helper.assertTrue(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR), "the gorgon's gaze must ignore armor");
            }
            sheep.discard();
        }
        helper.succeed();
    }

    /** Every spawn egg of the mod used on the ground makes its creature (the myrmex queen's egg threw). */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
    public void spawnEggs(GameTestHelper helper) {
        var player = mockPlayer(helper);
        // A queen already living (with her hive), like after the battle tests, where her egg threw.
        helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(),
            "summon iceandfire_fabric:myrmex_queen " + helper.absolutePos(new BlockPos(3, 2, 8)).getX() + " " + helper.absolutePos(new BlockPos(3, 2, 8)).getY() + " " + helper.absolutePos(new BlockPos(3, 2, 8)).getZ());
        List<String> failed = new ArrayList<>();
        int i = 0;
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            var id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(IafMod.MOD_ID) || !(item instanceof net.minecraft.world.item.SpawnEggItem)) {
                continue;
            }
            BlockPos floor = helper.absolutePos(new BlockPos(1 + (i % 6), 0, 1 + (i / 6) % 6));
            i++;
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(item));
            try {
                player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false)));
            } catch (Throwable error) {
                failed.add(id.getPath());
                IafMod.LOGGER.error("[IAFTEST] spawn egg {} failed", id, error);
            }
        }
        log("used " + i + " spawn eggs, failed: " + failed);
        // Only this test's own area (the tests run side by side).
        for (Entity entity : helper.getLevel().getEntities((Entity) null, helper.getBounds(), e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
            entity.discard();
        }
        helper.assertTrue(failed.isEmpty(), "spawn eggs failed: " + failed);
        helper.succeed();
    }

    /** A cake a player threw near a pixie tames it (the pixie flies to it and takes it). */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
    public void pixieCake(GameTestHelper helper) {
        var player = mockPlayer(helper);
        com.iafabric.entity.EntityPixie pixie = helper.spawn(IafEntityRegistry.PIXIE, new BlockPos(2, 3, 2));
        var cake = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 4)).getX() + 0.5,
            helper.absolutePos(new BlockPos(4, 2, 4)).getY(), helper.absolutePos(new BlockPos(4, 2, 4)).getZ() + 0.5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CAKE));
        cake.setThrower(player);
        helper.getLevel().addFreshEntity(cake);
        int[] ticks = {0};
        helper.onEachTick(() -> {
            if (++ticks[0] % 40 == 0) {
                log("pixie at " + pixie.blockPosition() + " cake " + (cake.isAlive() ? cake.blockPosition() : "taken") + " tame=" + pixie.isTame()
                    + " goals=" + pixie.goalSelector.getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList());
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(pixie.isTame(), "pixie not tamed by the cake");
            helper.assertTrue(pixie.isOwnedBy(player), "pixie tamed but not the thrower's");
            pixie.discard();
        });
    }

    /** Rabbit feet thrown near a hippogryph tame it after it ate four to eight; kelp tames a hippocampus (one in three). */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 1200)
    public void tamingWithItems(GameTestHelper helper) {
        var player = mockPlayer(helper);
        com.iafabric.entity.EntityHippogryph hippogryph = helper.spawn(IafEntityRegistry.HIPPOGRYPH, new BlockPos(2, 2, 2));
        var foot = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), hippogryph.getX() + 2, hippogryph.getY(), hippogryph.getZ(),
            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RABBIT_FOOT, 10));
        foot.setThrower(player);
        helper.getLevel().addFreshEntity(foot);
        com.iafabric.entity.EntityHippocampus hippocampus = helper.spawn(IafEntityRegistry.HIPPOCAMPUS, new BlockPos(6, 2, 6));
        hippocampus.setNoAi(true);
        for (int i = 0; i < 30 && !hippocampus.isTame(); i++) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.KELP));
            click(hippocampus, player);
        }
        helper.assertTrue(hippocampus.isOwnedBy(player), "hippocampus not tamed by kelp");
        helper.succeedWhen(() -> {
            helper.assertTrue(hippogryph.isOwnedBy(player), "hippogryph not tamed by the thrown rabbit foot: fed " + hippogryph.feedings
                + ", feet left " + foot.getItem().getCount() + " at " + Math.round(hippogryph.distanceTo(foot) * 10) / 10.0 + ", foot owner " + foot.getOwner()
                + ", moves " + hippogryph.canMove() + ", flying " + hippogryph.isFlying() + " hovering " + hippogryph.isHovering() + " up " + Math.round((hippogryph.getY() - foot.getY()) * 10) / 10.0 + ", goals " + hippogryph.targetSelector.getAvailableGoals().stream().filter(g -> g.isRunning()).map(g -> g.getGoal().getClass().getSimpleName()).toList());
            log("tamed: hippogryph by a rabbit foot, hippocampus by kelp");
            hippogryph.discard();
            hippocampus.discard();
        });
    }

    /** A fire dragon egg placed in fire stays there and hatches a tamed dragon of its owner (egg time shortened). */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void fireEggHatches(GameTestHelper helper) {
        int saved = com.iafabric.IafConfig.dragonEggTime;
        com.iafabric.IafConfig.dragonEggTime = 40;
        var player = mockPlayer(helper);
        BlockPos at = new BlockPos(3, 2, 3);
        helper.setBlock(at.below(), net.minecraft.world.level.block.Blocks.NETHERRACK);
        helper.setBlock(at, net.minecraft.world.level.block.Blocks.FIRE);
        com.iafabric.entity.EntityDragonEgg egg = helper.spawn(IafEntityRegistry.DRAGON_EGG, at);
        egg.setEggType(com.iafabric.enums.EnumDragonEgg.RED);
        egg.setOwnerId(player.getUUID());
        helper.succeedWhen(() -> {
            var dragons = helper.getLevel().getEntitiesOfClass(com.iafabric.entity.EntityFireDragon.class, new net.minecraft.world.phys.AABB(helper.absolutePos(at)).inflate(3));
            helper.assertTrue(!dragons.isEmpty(), "no dragon hatched yet; egg alive=" + egg.isAlive() + " age=" + egg.getDragonAge());
            helper.assertTrue(dragons.get(0).isTame() && player.getUUID().equals(com.iafabric.util.EntityUtil.getOwnerUUID(dragons.get(0))), "hatched dragon is not its owner's");
            com.iafabric.IafConfig.dragonEggTime = saved;
            log("fire egg hatched in fire: " + dragons.get(0));
            dragons.forEach(net.minecraft.world.entity.Entity::discard);
        });
    }

    /** The ghost sword's swing throws a blade that flies (it is a living projectile for a moment). */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
    public void ghostSwordThrows(GameTestHelper helper) {
        var player = mockPlayer(helper);
        // (the mock player starts at the world's 0 0 0, a chunk whose entities may not be ticking: a blade made there
        // is not seen; in the test's own space it is)
        var inside = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5));
        player.moveTo(inside.x, inside.y, inside.z, 0, 0);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.GHOST_SWORD));
        var blades = new java.util.ArrayList<com.iafabric.entity.EntityGhostSword>();
        // (a throw now and then makes a blade the level does not show yet: tried up to three times)
        for (int tries = 0; tries < 3 && blades.isEmpty(); tries++) {
            player.getCooldowns().removeCooldown(player.getMainHandItem().getItem());
            com.iafabric.event.ServerEvents.onLeftClick(player, player.getMainHandItem());
            // looked for at once: a blade that hits the test's wall is gone a tick later
            blades.addAll(helper.getLevel().getEntitiesOfClass(com.iafabric.entity.EntityGhostSword.class, player.getBoundingBox().inflate(30)));
            if (blades.isEmpty()) {
                // (a blade may be out of the box already: any of the level's, thrown by this player)
                for (var blade : helper.getLevel().getEntities(IafEntityRegistry.GHOST_SWORD, b -> b.getOwner() == player)) {
                    blades.add(blade);
                }
            }
        }
        log("ghost sword blades after the swing: " + blades.size() + " cooldown=" + player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem()));
        if (blades.isEmpty()) {
            // what became of a blade made here by hand, to see why
            var level = helper.getLevel();
            var probe = new com.iafabric.entity.EntityGhostSword(IafEntityRegistry.GHOST_SWORD, level, player, 4.5D);
            probe.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1, 0.5f);
            boolean added = level.addFreshEntity(probe);
            helper.assertTrue(false, "the ghost sword threw no blade: player at " + player.position() + " in " + level.dimension().location()
                + " alive " + player.isAlive() + " removed " + player.isRemoved() + "; a blade by hand at " + probe.position() + " added " + added
                + " removed " + probe.isRemoved() + " found " + (level.getEntity(probe.getUUID()) != null) + " loaded " + level.isLoaded(probe.blockPosition())
                + " ticking " + level.isPositionEntityTicking(probe.blockPosition()));
        }
        blades.forEach(net.minecraft.world.entity.Entity::discard);
        helper.succeed();
    }

    /**
     * An ice egg in water freezes into a block of ice and its dragon hatches out of it; a lightning egg hatches
     * under the rain. In a test place of their own where it rains all along (the fire egg's fire would go out).
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400, batch = "rain", skyAccess = true)
    public void iceAndLightningEggsHatch(GameTestHelper helper) {
        int saved = com.iafabric.IafConfig.dragonEggTime;
        com.iafabric.IafConfig.dragonEggTime = 40;
        var player = mockPlayer(helper);
        BlockPos water = new BlockPos(2, 2, 2);
        helper.setBlock(water, net.minecraft.world.level.block.Blocks.WATER);
        com.iafabric.entity.EntityDragonEgg iceEgg = helper.spawn(IafEntityRegistry.DRAGON_EGG, water);
        iceEgg.setEggType(com.iafabric.enums.EnumDragonEgg.BLUE);
        iceEgg.setOwnerId(player.getUUID());
        // in water an ice egg freezes one tick in 500: tried here until it does
        for (int i = 0; i < 100000 && !iceEgg.isRemoved(); i++) {
            com.iafabric.entity.DragonType.ICE.updateEggCondition(iceEgg);
        }
        helper.assertBlockPresent(com.iafabric.block.IafBlockRegistry.EGG_IN_ICE, water);
        BlockPos open = new BlockPos(5, 2, 5);
        // it only rains where the land lets it: the test place may be a desert
        BlockPos from = helper.absolutePos(BlockPos.ZERO);
        BlockPos to = helper.absolutePos(new BlockPos(8, 8, 8));
        helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(),
            "fillbiome " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " minecraft:plains");
        com.iafabric.entity.EntityDragonEgg lightningEgg = helper.spawn(IafEntityRegistry.DRAGON_EGG, open);
        lightningEgg.setEggType(com.iafabric.enums.EnumDragonEgg.ELECTRIC);
        lightningEgg.setOwnerId(player.getUUID());
        helper.succeedWhen(() -> {
            var area = new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2);
            var ice = helper.getLevel().getEntitiesOfClass(com.iafabric.entity.EntityIceDragon.class, area);
            var lightning = helper.getLevel().getEntitiesOfClass(com.iafabric.entity.EntityLightningDragon.class, area);
            var frozen = helper.getLevel().getBlockEntity(helper.absolutePos(water)) instanceof com.iafabric.entity.tile.TileEntityEggInIce egg ? egg : null;
            String state = "ice: block " + helper.getBlockState(water).getBlock() + (frozen == null ? "" : " age " + frozen.age + " type " + frozen.type)
                + "; lightning: raining " + helper.getLevel().isRaining() + " there " + helper.getLevel().isRainingAt(lightningEgg.blockPosition())
                + ", sky " + helper.getLevel().canSeeSky(lightningEgg.blockPosition().above())
                + ", biome " + helper.getLevel().getBiome(lightningEgg.blockPosition()).getRegisteredName()
                + ", egg age " + lightningEgg.getDragonAge() + ", alive " + lightningEgg.isAlive()
                + "; ice dragons near: " + helper.getLevel().getEntitiesOfClass(com.iafabric.entity.EntityIceDragon.class, area.inflate(48)).stream().map(d -> d.blockPosition().toShortString()).toList()
                + " (test at " + helper.absolutePos(BlockPos.ZERO).toShortString() + ")";
            helper.assertTrue(!ice.isEmpty() && !lightning.isEmpty(), "not hatched yet: " + state);
            helper.assertTrue(player.getUUID().equals(com.iafabric.util.EntityUtil.getOwnerUUID(ice.get(0))), "the ice dragon is not its owner's");
            helper.assertTrue(player.getUUID().equals(com.iafabric.util.EntityUtil.getOwnerUUID(lightning.get(0))), "the lightning dragon is not its owner's");
            com.iafabric.IafConfig.dragonEggTime = saved;
            log("ice egg hatched from the ice: " + ice.get(0) + "; lightning egg in the rain: " + lightning.get(0));
            ice.forEach(net.minecraft.world.entity.Entity::discard);
            lightning.forEach(net.minecraft.world.entity.Entity::discard);
        });
    }

    /** An amphithere is tamed by staying on its back long enough (right click with an empty hand to get on). */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void amphithereTamedByRiding(GameTestHelper helper) {
        int saved = com.iafabric.IafConfig.amphithereTameTime;
        com.iafabric.IafConfig.amphithereTameTime = 60;
        var player = mockPlayer(helper);
        var amphithere = helper.spawn(IafEntityRegistry.AMPHITHERE, new BlockPos(4, 2, 4));
        player.moveTo(amphithere.getX() + 1.5, amphithere.getY(), amphithere.getZ(), 90, 0);
        click(amphithere, player);
        helper.assertTrue(player.getVehicle() == amphithere, "the player did not get on the amphithere");
        helper.succeedWhen(() -> {
            player.setHealth(player.getMaxHealth());
            helper.assertTrue(amphithere.isTame(), "not tame yet; riding it " + (player.getVehicle() == amphithere));
            helper.assertTrue(amphithere.isOwnedBy(player), "tamed for someone else");
            com.iafabric.IafConfig.amphithereTameTime = saved;
            log("amphithere tamed by riding it");
            player.stopRiding();
            amphithere.discard();
        });
    }

    /**
     * A cockatrice is tamed by holding its stare (it and the player looking at each other, 4 blocks apart or more)
     * until its taming reaches 1000; here it starts at 950.
     */
    // (a batch of its own: in 1.21.1's tighter grid the players and creatures of the tests beside it catch its eye)
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300, batch = "cockatrice")
    public void cockatriceTamedByStaring(GameTestHelper helper) {
        // a cockatrice leaves players in creative alone (as the test's mock player is): a player in survival
        var player = survivalPlayer(helper);
        var start = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 4.5));
        var cockatrice = helper.spawn(IafEntityRegistry.COCKATRICE, new BlockPos(7, 2, 4));
        cockatrice.setTamingLevel(950);
        helper.succeedWhen(() -> {
            player.moveTo(start.x, start.y, start.z, player.getYRot(), player.getXRot());
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, cockatrice.getEyePosition());
            player.setHealth(player.getMaxHealth());
            if (!cockatrice.isTame()) {
                cockatrice.setTarget(player);
            }
            helper.assertTrue(cockatrice.isTame(), "not tame yet: taming " + cockatrice.getTamingLevel() + ", staring " + cockatrice.isStaring()
                + ", target " + cockatrice.getTarget() + ", player mode " + player.gameMode.getGameModeForPlayer() + ", difficulty " + helper.getLevel().getDifficulty() + ", it looks at the player " + com.iafabric.entity.EntityGorgon.isEntityLookingAt(cockatrice, player, com.iafabric.entity.EntityCockatrice.VIEW_RADIUS)
                + ", the player at it " + com.iafabric.entity.EntityGorgon.isEntityLookingAt(player, cockatrice, com.iafabric.entity.EntityCockatrice.VIEW_RADIUS)
                + ", apart " + cockatrice.distanceTo(player));
            helper.assertTrue(cockatrice.isOwnedBy(player), "tamed for someone else");
            log("cockatrice tamed by its stare");
            cockatrice.discard();
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    /** A thrown egg of a death worm, a cockatrice (a rotten egg, one in four hatches) or a hippogryph gives its thrower a tame young one. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void thrownEggsHatchTame(GameTestHelper helper) {
        var player = mockPlayer(helper);
        var above = helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 5, 4.5));
        player.moveTo(above.x, above.y, above.z, 0, 90);
        var eggs = List.of(com.iafabric.item.IafItemRegistry.DEATHWORM_EGG, com.iafabric.item.IafItemRegistry.HIPPOGRYPH_EGG, com.iafabric.item.IafItemRegistry.ROTTEN_EGG);
        for (var egg : eggs) {
            for (int i = 0; i < (egg == com.iafabric.item.IafItemRegistry.ROTTEN_EGG ? 30 : 1); i++) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(egg));
                egg.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
        helper.succeedWhen(() -> {
            var area = new net.minecraft.world.phys.AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8, 8, 8).inflate(2);
            List<String> missing = new ArrayList<>();
            for (var kind : List.<Class<? extends LivingEntity>>of(com.iafabric.entity.EntityDeathWorm.class, com.iafabric.entity.EntityHippogryph.class, com.iafabric.entity.EntityCockatrice.class)) {
                boolean tame = helper.getLevel().getEntitiesOfClass(kind, area).stream()
                    .anyMatch(young -> player.getUUID().equals(com.iafabric.util.EntityUtil.getOwnerUUID(young)));
                if (!tame) {
                    missing.add(kind.getSimpleName());
                }
            }
            helper.assertTrue(missing.isEmpty(), "no tame young yet: " + missing);
            log("thrown eggs hatched tame: death worm, hippogryph, cockatrice");
            for (var kind : List.<Class<? extends LivingEntity>>of(com.iafabric.entity.EntityDeathWorm.class, com.iafabric.entity.EntityHippogryph.class, com.iafabric.entity.EntityCockatrice.class)) {
                helper.getLevel().getEntitiesOfClass(kind, area).forEach(Entity::discard);
            }
        });
    }

    /**
     * A wild dragon under a roof (a cave) with a player in reach above it does not take off (it would only bump
     * into the rock), and the player who hurt it stays its target even when something else gets near.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void dragonUnderRoofKeepsAttacker(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) {
                helper.setBlock(new BlockPos(x, 7, z), net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        EntityDragonBase dragon = helper.spawn(IafEntityRegistry.FIRE_DRAGON, new BlockPos(3, 2, 3));
        dragon.setAgeInDays(0);
        dragon.growDragon(30);
        dragon.setHealth(dragon.getMaxHealth());
        var start = helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5, 2, 6.5));
        player.moveTo(start.x, start.y + 3, start.z, 0, 0);
        player.getAbilities().invulnerable = false;
        // the dragon fights back: the player must not die of it (a dead player is no target)
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 400, 4));
        dragon.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        var cow = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 2, 1));
        boolean[] flew = {false};
        int[] underRoof = {0};
        int[] kept = {0};
        helper.onEachTick(() -> {
            player.setHealth(player.getMaxHealth());
            if (DragonSmartsAccess.underRoof(dragon)) {
                underRoof[0]++;
                flew[0] |= dragon.isFlying() || dragon.isHovering();
            }
            if (helper.getTick() == 40) {
                dragon.setTarget(cow);
            }
            if (helper.getTick() > 40 && dragon.getTarget() == player) {
                kept[0]++;
            }
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(underRoof[0] > 20, "the dragon was hardly under the test roof: " + underRoof[0] + " ticks");
            helper.assertFalse(flew[0], "the dragon took off under the roof");
            // offered a cow at tick 40, it kept to the player who hurt it nearly all the time after
            helper.assertTrue(kept[0] > 60, "the dragon left the player who hurt it: on them " + kept[0] + " of 80 ticks, now " + dragon.getTarget()
                + " (player alive " + player.isAlive() + ", dragon asleep " + dragon.isSleeping() + ")");
            log("dragon under a roof stayed on its feet and kept its attacker");
            dragon.discard();
            cow.discard();
            helper.getLevel().getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    /**
     * A sky dragon leaves the player alone; a player who kills two of them is warned by an oracle pixie, then a wave
     * of three stage 4 dragons comes for them (the waits are shortened for the test).
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void skyDragonsAndTheOracle(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = survivalPlayer(helper);
        var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(4, 2, 4));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        int savedOracle = com.iafabric.event.DragonSkies.oracleDelay;
        int savedWave = com.iafabric.event.DragonSkies.waveDelay;
        com.iafabric.event.DragonSkies.oracleDelay = 20;
        com.iafabric.event.DragonSkies.waveDelay = 40;
        com.iafabric.event.DragonSkies.waveDistance = 10;
        // close by: the test world only moves what is near the test
        var calm = com.iafabric.event.DragonSkies.spawnSkyDragon(level, player, 0);
        calm.setTarget(player);
        java.util.List<EntityDragonBase> killed = new ArrayList<>();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(calm.getTarget() == null, "a sky dragon went for a player who did nothing: " + calm.getTarget()
                + " (ticks " + calm.tickCount + ", sky " + calm.skyWanderer + ", hurt by " + calm.getLastHurtByMob() + ", revenge " + calm.revengePlayer + ", dead " + calm.isModelDead() + ")");
            helper.assertTrue(calm.isFlying() || calm.isHovering() || !calm.onGround(), "the sky dragon is not in the air");
            for (int i = 0; i < 2; i++) {
                var dragon = com.iafabric.event.DragonSkies.spawnSkyDragon(level, player, 0);
                dragon.hurt(level.damageSources().playerAttack(player), 100000.0F);
                killed.add(dragon);
            }
            helper.assertTrue(killed.stream().allMatch(d -> !d.isAlive() || d.isModelDead()), "the sky dragons did not die");
        });
        helper.runAfterDelay(40, () -> {
            var oracles = level.getEntitiesOfClass(com.iafabric.entity.EntityPixie.class, player.getBoundingBox().inflate(10), p -> p.oracleTicks >= 0);
            helper.assertTrue(!oracles.isEmpty(), "no oracle pixie after two sky dragons were killed");
            log("oracle pixie: " + oracles.get(0).getDisplayName().getString());
        });
        helper.runAfterDelay(90, () -> {
            var wave = level.getEntitiesOfClass(EntityDragonBase.class, player.getBoundingBox().inflate(120), d -> d.waveDragon);
            helper.assertTrue(wave.size() == 3, "the wave has " + wave.size() + " dragons");
            helper.assertTrue(wave.stream().allMatch(d -> d.getDragonStage() >= 4 && d.getTarget() == player), "the wave's dragons are weak or do not go for the player");
            log("wave of " + wave.size() + " stage " + wave.get(0).getDragonStage() + " dragons after the player");
            com.iafabric.event.DragonSkies.oracleDelay = savedOracle;
            com.iafabric.event.DragonSkies.waveDelay = savedWave;
            com.iafabric.event.DragonSkies.waveDistance = 45;
            wave.forEach(net.minecraft.world.entity.Entity::discard);
            killed.forEach(net.minecraft.world.entity.Entity::discard);
            calm.discard();
            helper.getLevel().getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    /**
     * A pillager kingdom's dragon: its fire does not hurt the pillagers, a pillager left on the ground climbs back on,
     * killed it leaves its armor. And the rider's seal: a dragon does not hurt its owner who carries one.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 200)
    public void pillagerDragonsAndTheSeal(GameTestHelper helper) {
        var level = helper.getLevel();
        EntityDragonBase dragon = helper.spawn(IafEntityRegistry.FIRE_DRAGON, new BlockPos(4, 2, 4));
        dragon.growDragon(30);
        dragon.pillagerMount = true;
        dragon.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.DRAGONARMOR_IRON_0));
        var pillager = helper.spawn(net.minecraft.world.entity.EntityType.PILLAGER, new BlockPos(1, 2, 1));
        float before = pillager.getHealth();
        pillager.hurt(com.iafabric.misc.IafDamageRegistry.causeDragonFireDamage(dragon), 5.0F);
        helper.assertTrue(pillager.getHealth() == before, "the pillager dragon's fire hurt a pillager");
        // the seal
        var player = survivalPlayer(helper);
        EntityDragonBase own = helper.spawn(IafEntityRegistry.FIRE_DRAGON, new BlockPos(1, 2, 6));
        own.tame(player);
        player.getInventory().add(new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.RIDER_SEAL));
        // a player who just joined cannot be hurt for three seconds
        boolean[] sealChecked = {false};
        helper.runAfterDelay(70, () -> {
            float full = player.getHealth();
            player.hurt(com.iafabric.misc.IafDamageRegistry.causeDragonFireDamage(own), 4.0F);
            helper.assertTrue(player.getHealth() == full, "the owner's dragon hurt them through the seal");
            player.getInventory().clearContent();
            player.invulnerableTime = 0;
            player.hurt(com.iafabric.misc.IafDamageRegistry.causeDragonFireDamage(own), 4.0F);
            helper.assertTrue(player.getHealth() < full, "without the seal the dragon's fire does not hurt its owner");
            sealChecked[0] = true;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(sealChecked[0], "the seal is not checked yet");
            helper.assertTrue(dragon.getVirtualRider() == com.iafabric.entity.DragonRiders.PILLAGER && pillager.isRemoved(), "the pillager has not climbed on the dragon");
            dragon.hurt(level.damageSources().playerAttack(player), 100000.0F);
            var dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, dragon.getBoundingBox().inflate(8),
                item -> item.getItem().getItem() instanceof com.iafabric.item.ItemDragonArmor);
            helper.assertTrue(!dropped.isEmpty(), "the killed pillager dragon left no armor");
            log("pillager dragon: fire spares pillagers, a pillager climbed on, armor left when killed; the seal keeps its owner safe");
            dropped.forEach(net.minecraft.world.entity.Entity::discard);
            pillager.discard();
            dragon.discard();
            own.discard();
            helper.getLevel().getServer().getPlayerList().remove(player);
        });
    }

    /**
     * Pixie bonds: a tamed pixie bonded; hit, it takes nothing and its player takes a part (after its shield); its
     * orb blocks a kind of damage; it gives its effect at level 1 and its stats (no effect) at level 5; sent to rest
     * and called out again, only one of it lives.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void pixieBonds(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = survivalPlayer(helper);
        var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(3, 2, 3));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        com.iafabric.entity.EntityPixie pixie = helper.spawn(IafEntityRegistry.PIXIE, new BlockPos(4, 3, 4));
        pixie.setColor(0);
        pixie.tame(player);
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(6, 2, 6));
        zombie.setNoAi(true);
        helper.assertTrue(com.iafabric.pixie.PixieBonding.bond(player, 0), "the tamed pixie could not be bonded");
        helper.assertTrue(pixie.isBonded() && com.iafabric.pixie.PixieBonding.get(player).active == 0, "the pixie is not the bonded one out");
        helper.runAfterDelay(70, () -> {
            // after the player's join protection: two hits of 10 on the pixie
            float full = player.getHealth();
            float pixieHealth = pixie.getHealth();
            pixie.hurt(level.damageSources().mobAttack(zombie), 10.0F);
            player.invulnerableTime = 0;
            pixie.hurt(level.damageSources().mobAttack(zombie), 10.0F);
            var bond = com.iafabric.pixie.PixieBonding.get(player).get(0);
            helper.assertTrue(pixie.getHealth() == pixieHealth, "the bonded pixie was hurt");
            // 30% of 20 is 6, the level 1 shield (4) takes 4: the player takes 2
            helper.assertTrue(Math.abs(full - player.getHealth() - 2.0F) < 0.01F, "the player took " + (full - player.getHealth()) + " instead of 2");
            helper.assertTrue(bond.shield < 0.01F, "the shield was not used: " + bond.shield);
            helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "no strength from the pink pixie at level 1");
            // a physical orb and a fire orb in their slots (the pixie's menu), both worn: blows reach nobody
            var worn = com.iafabric.pixie.PixieBonding.get(player).get(0);
            worn.orbs[com.iafabric.pixie.OrbKind.PHYSICAL.ordinal()] = new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.PIXIE_ORB_PHYSICAL);
            worn.orbs[com.iafabric.pixie.OrbKind.FIRE.ordinal()] = new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.PIXIE_ORB_FIRE);
            float before = player.getHealth();
            player.invulnerableTime = 0;
            pixie.hurt(level.damageSources().mobAttack(zombie), 10.0F);
            helper.assertTrue(player.getHealth() == before, "the physical orb did not block a blow");
            // level 5: stats, not the effect
            com.iafabric.pixie.PixieBonding.addXp(player, 0, 16000);
            helper.assertTrue(com.iafabric.pixie.PixieBonding.get(player).get(0).level == 5, "16000 experience (ten stacks of dust) is not level 5");
        });
        helper.runAfterDelay(100, () -> {
            var attack = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            helper.assertTrue(attack.getModifier(com.iafabric.IafMod.id("pixie_bond_0_attack_damage")) != null, "no attack bonus at level 5");
            com.iafabric.pixie.PixieBonding.rest(player, false);
            helper.assertTrue(!pixie.isAlive(), "the pixie did not go to rest");
            helper.assertTrue(com.iafabric.pixie.PixieBonding.summon(player, 0), "the pixie did not come out again");
        });
        helper.runAfterDelay(140, () -> {
            var out = level.getEntitiesOfClass(com.iafabric.entity.EntityPixie.class, player.getBoundingBox().inflate(16), p -> p.isBonded());
            helper.assertTrue(out.size() == 1 && out.get(0).getColor() == 0, "bonded pixies out: " + out.size());
            log("pixie bond: share after the shield, the orb blocks, effect then stats, rest and out again with one pixie");
            out.forEach(net.minecraft.world.entity.Entity::discard);
            zombie.discard();
            com.iafabric.pixie.PixieBonding.rest(player, false);
            helper.getLevel().getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    /**
     * A bonded pixie's own fight: a pink pixie learns its spells from what it is given (blaze powder: rage, fire
     * dragon blood: a pillar of fire) but not another color's (ice dragon blood stays), its first spell is the one the
     * key calls, a spell of a higher level waits for it, a spell cast must wait again, and its one help (a punch of
     * light) goes by itself, slowly, at what its player hits.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void pixieAbilities(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = survivalPlayer(helper);
        var at = helper.absoluteVec(new net.minecraft.world.phys.Vec3(3, 2, 3));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 400, 4));
        com.iafabric.entity.EntityPixie pixie = helper.spawn(IafEntityRegistry.PIXIE, new BlockPos(4, 3, 4));
        pixie.setColor(0);
        pixie.tame(player);
        helper.assertTrue(com.iafabric.pixie.PixieBonding.bond(player, 0), "the tamed pixie could not be bonded");
        var slot = new net.minecraft.world.SimpleContainer(1);
        var bond = com.iafabric.pixie.PixieBonding.get(player).get(0);
        helper.assertTrue(bond.knows(com.iafabric.pixie.PixieAbility.STRIKE) && bond.selected() == null, "a new pink pixie does not have only its help");
        slot.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLAZE_POWDER));
        com.iafabric.pixie.PixieBonding.feed(player, 0, slot);
        helper.assertTrue(bond.knows(com.iafabric.pixie.PixieAbility.RAGE) && slot.getItem(0).isEmpty(), "blaze powder did not teach rage");
        helper.assertTrue(bond.selected() == com.iafabric.pixie.PixieAbility.RAGE, "its first spell is not the one the key calls");
        // another color's teacher stays where it is
        slot.setItem(0, new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.ICE_DRAGON_BLOOD));
        com.iafabric.pixie.PixieBonding.feed(player, 0, slot);
        helper.assertTrue(!slot.getItem(0).isEmpty() && !bond.knows(com.iafabric.pixie.PixieAbility.BLIZZARD), "a pink pixie learnt a blue pixie's spell");
        slot.setItem(0, new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.FIRE_DRAGON_BLOOD));
        com.iafabric.pixie.PixieBonding.feed(player, 0, slot);
        helper.assertTrue(bond.knows(com.iafabric.pixie.PixieAbility.PILLAR), "fire dragon blood did not teach the pillar of fire");
        // the pillar is for level 5: chosen, the key does nothing yet
        com.iafabric.pixie.PixieBonding.selectSpell(player, 0, com.iafabric.pixie.PixieAbility.PILLAR.ordinal());
        helper.assertTrue(bond.selected() == com.iafabric.pixie.PixieAbility.PILLAR, "the pillar could not be chosen");
        com.iafabric.pixie.PixieMagic.cast(player);
        helper.assertTrue(bond.ready[com.iafabric.pixie.PixieAbility.PILLAR.ordinal()] == 0, "a level 5 spell was cast at level 1");
        // rage: cast, then it must wait
        com.iafabric.pixie.PixieBonding.selectSpell(player, 0, com.iafabric.pixie.PixieAbility.RAGE.ordinal());
        com.iafabric.pixie.PixieMagic.cast(player);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "rage gave no strength");
        long ready = bond.ready[com.iafabric.pixie.PixieAbility.RAGE.ordinal()];
        helper.assertTrue(ready > level.getGameTime(), "rage has no wait");
        player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
        com.iafabric.pixie.PixieMagic.cast(player);
        helper.assertTrue(!player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "rage was cast again before its wait");
        // a stack of pixie dust: 1600 experience, level 2
        slot.setItem(0, new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.PIXIE_DUST, 64));
        com.iafabric.pixie.PixieBonding.feed(player, 0, slot);
        helper.assertTrue(bond.level == 2, "a stack of pixie dust did not make level 2: " + bond.level + " " + bond.xp);
        // the player hits a zombie: the pixie's help goes for it by itself
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(6, 2, 6));
        zombie.setNoAi(true);
        float zombieHealth = zombie.getHealth();
        player.setLastHurtMob(zombie);
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(zombie.getHealth() < zombieHealth || !zombie.isAlive(), "the pixie's help did not strike the zombie");
            // the pixie's magic counts as its player's (what it kills makes it grow)
            helper.assertTrue(zombie.getLastHurtByMob() == player, "the zombie was not hurt for the player: " + zombie.getLastHurtByMob());
            log("pixie abilities: pink learns rage and the pillar (not ice), the key casts the chosen one (the pillar waits for level 5, rage waits after use); its punch of light struck the zombie");
            zombie.discard();
            com.iafabric.pixie.PixieBonding.rest(player, false);
            helper.getLevel().getServer().getPlayerList().remove(player);
            helper.succeed();
        });
    }

    /**
     * A kingdom dragon: young and kept so, its pillager rides on its back (not in its jaws), it sits when all is quiet
     * and stands to fight a player who comes near; wild dragons do not fight each other, kingdom dragons are not theirs.
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 900)
    public void kingdomDragonsAndAllies(GameTestHelper helper) {
        var level = helper.getLevel();
        EntityDragonBase dragon = helper.spawn(IafEntityRegistry.FIRE_DRAGON, new BlockPos(4, 2, 4));
        dragon.setAgeInDays(0);
        dragon.growDragon(55);
        dragon.setAgingDisabled(true);
        dragon.pillagerMount = true;
        dragon.setPersistenceRequired();
        dragon.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(com.iafabric.item.IafItemRegistry.DRAGONARMOR_NETHERITE_0));
        helper.assertTrue(dragon.getArmorOrdinal(dragon.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)) == 9, "netherite dragon armor is not the ninth kind");
        // a pillager riding it the old way becomes its drawn rider
        var pillager = helper.spawn(net.minecraft.world.entity.EntityType.PILLAGER, new BlockPos(4, 2, 1));
        pillager.startRiding(dragon, true);
        EntityDragonBase ice = helper.spawn(IafEntityRegistry.ICE_DRAGON, new BlockPos(2, 2, 12));
        ice.setAgeInDays(0);
        ice.growDragon(60);
        EntityDragonBase lightning = helper.spawn(IafEntityRegistry.LIGHTNING_DRAGON, new BlockPos(12, 2, 2));
        lightning.setAgeInDays(0);
        lightning.growDragon(60);
        helper.assertTrue(ice.alliedWith(lightning), "two wild dragons are not allies");
        helper.assertTrue(!com.iafabric.entity.PillagerDragons.allowDamage(lightning, level.damageSources().mobAttack(ice)), "a wild dragon hurt another");
        helper.assertTrue(!ice.alliedWith(dragon), "a wild dragon is the ally of a kingdom's");
        helper.assertTrue(!com.iafabric.entity.PillagerDragons.allowDamage(pillager, level.damageSources().mobAttack(dragon)), "a kingdom dragon hurt its pillager");
        helper.assertTrue(dragon.isAlliedTo(pillager), "a kingdom dragon's breath would hurt the pillagers");
        ice.discard();
        lightning.discard();
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(dragon.getVirtualRider() == com.iafabric.entity.DragonRiders.PILLAGER, "the pillager did not become its rider: " + dragon.getVirtualRider());
            helper.assertTrue(pillager.isRemoved(), "the pillager that rode it is still about");
            helper.assertTrue(dragon.riderPart != null && dragon.riderPart.isAlive(), "its rider has no hitbox");
            helper.assertTrue(dragon.riderPart.getY() > dragon.getY() + 1, "its rider's hitbox is not on its back: " + dragon.riderPart.getY() + " / " + dragon.getY());
            helper.assertTrue(dragon.getDragonStage() == 3, "the kingdom dragon is not of stage 3: " + dragon.getDragonStage());
        });
        // a player there from the start keeps the place running; in creative it is no enemy of the kingdom
        net.minecraft.server.level.ServerPlayer[] player = {survivalPlayer(helper)};
        player[0].setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var aside = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1));
        player[0].moveTo(aside.x, aside.y, aside.z, 0, 0);
        helper.runAfterDelay(180, () -> {
            helper.assertTrue(dragon.isOrderedToSit(), "the kingdom dragon did not sit with nothing about");
            player[0].setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player[0].addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 900, 4));
            // inside the test's space (its walls would hide the player from the dragon)
            player[0].moveTo(dragon.getX() - 3, dragon.getY(), dragon.getZ() - 3, 90, 0);
        });
        int[] remounted = {0};
        boolean[] riderKilled = {false};
        helper.onEachTick(() -> {
            if (riderKilled[0] && remounted[0] == 0 && dragon.getVirtualRider() == com.iafabric.entity.DragonRiders.PILLAGER) {
                remounted[0] = (int) helper.getTick();
            }
        });
        helper.runAfterDelay(260, () -> {
            helper.assertTrue(!dragon.isOrderedToSit(), "the kingdom dragon did not stand up for a player near: target " + dragon.getTarget()
                + " command " + dragon.getCommand() + " asleep " + dragon.isSleeping() + " calm " + dragon.calmTicks
                + " distance " + player[0].distanceTo(dragon) + " seen " + dragon.hasLineOfSight(player[0]) + " survival " + !player[0].isCreative());
            helper.assertTrue(dragon.getTarget() == player[0], "the kingdom dragon does not fight the player: " + dragon.getTarget());
            // the player kills its rider: it falls dead, and the dragon goes to the pillager on foot, who rides it
            helper.assertTrue(dragon.riderPart != null, "its rider has no hitbox: rider " + dragon.getVirtualRider() + " health " + dragon.riderHealth
                + " no ai " + dragon.isNoAi() + " tame " + dragon.isTame() + " candidate " + dragon.riderCandidate);
            helper.assertTrue(dragon.riderPart.hurt(level.damageSources().playerAttack(player[0]), 40F), "its rider could not be hit");
            helper.assertTrue(dragon.getVirtualRider() == com.iafabric.entity.DragonRiders.NONE, "its rider did not fall");
            // (well inside the test's space: on its edge a pillager stands on the border, out of sight)
            helper.spawn(net.minecraft.world.entity.EntityType.PILLAGER, new BlockPos(9, 2, 9));
            riderKilled[0] = true;
        });
        helper.runAfterDelay(700, () -> {
            var walkers = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Pillager.class, dragon.getBoundingBox().inflate(40));
            helper.assertTrue(remounted[0] > 0, "the dragon did not get a new rider: candidate " + dragon.riderCandidate + " berserk " + dragon.berserk
                + " pillagers " + walkers.size() + (walkers.isEmpty() ? "" : " first at " + walkers.get(0).position() + " alive " + walkers.get(0).isAlive()
                + " passenger " + walkers.get(0).isPassenger() + " noai " + walkers.get(0).isNoAi()) + " dragon at " + dragon.position() + " width " + dragon.getBbWidth()
                + " can move " + dragon.canMove() + " sitting " + dragon.isOrderedToSit() + " asleep " + dragon.isSleeping() + " give up until " + dragon.riderGiveUpUntil
                + " now " + level.getGameTime() + " searching since " + dragon.riderSearchSince + " straight " + dragon.walkingToRider
                + " flying " + dragon.isFlying() + " path " + dragon.getNavigation().getPath());
            log("kingdom dragon: stage 3, a drawn pillager on its back, sits when quiet, stands for a player; its rider killed, it got a new one at tick " + remounted[0]);
            helper.getLevel().getServer().getPlayerList().remove(player[0]);
            dragon.discard();
            helper.succeed();
        });
    }

    /** The bosses' cold is fair: a freeze, then eight seconds free of ice; a thaw frees at once. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
    public void bossColdIsFair(GameTestHelper helper) {
        var level = helper.getLevel();
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        zombie.setNoAi(true);
        com.iafabric.boss.BossCold.freeze(level, zombie, 20);
        helper.assertTrue(com.iafabric.entity.props.FrozenProperties.isFrozen(zombie), "the cold did not freeze");
        com.iafabric.boss.BossCold.thaw(zombie);
        helper.assertTrue(!com.iafabric.entity.props.FrozenProperties.isFrozen(zombie), "a thaw left the ice");
        com.iafabric.boss.BossCold.freeze(level, zombie, 20);
        helper.assertTrue(!com.iafabric.entity.props.FrozenProperties.isFrozen(zombie), "frozen again right after a freeze");
        log("boss cold: frozen, thawed at once, and not frozen again so soon");
        zombie.discard();
        helper.succeed();
    }

    /** The breath of a dread dragon spares the dread (no harm, no ice on them) and breaks no blocks. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 60)
    public void dreadBreathSparesTheDread(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(3, 2, 3));
        EntityDragonBase dragon = com.iafabric.world.structure.DreadPieces.dreadDragon(level, level.getRandom(), at, 80, false);
        level.addFreshEntity(dragon);
        var knight = helper.spawn(IafEntityRegistry.DREAD_KNIGHT, new BlockPos(8, 2, 8));
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(8, 2, 10));
        helper.setBlock(new BlockPos(8, 1, 9), net.minecraft.world.level.block.Blocks.STONE);
        float[] health = {0};
        helper.runAfterDelay(5, () -> {
            health[0] = knight.getHealth();
            com.iafabric.entity.IafDragonDestructionManager.destroyAreaIce(level, knight.blockPosition(), dragon);
            com.iafabric.entity.IafDragonDestructionManager.destroyAreaIceCharge(level, knight.blockPosition(), dragon);
            helper.assertTrue(dragon.isAlliedTo(knight), "a dread dragon is no ally of a dread knight");
            helper.assertTrue(knight.getHealth() >= health[0] && !com.iafabric.entity.props.FrozenProperties.isFrozen(knight),
                "the dread dragon's breath hurt or froze its knight: " + knight.getHealth() + " / " + health[0]);
            helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, new BlockPos(8, 1, 9));
            helper.assertTrue(dragon.keepsBlocks(), "a dread dragon breaks blocks");
            log("dread breath: knight unhurt and not frozen, zombie " + (zombie.getHealth() < zombie.getMaxHealth() ? "hurt" : "untouched") + ", stone kept");
            dragon.discard();
            helper.succeed();
        });
    }

    /**
     * Cryona asleep on her throne and Black Frost prowling behind it are her sign in her citadel, creatures of their own:
     * nothing hurts them (a mob's blow, a player's), they never fight in the world (the gametest server has no realm for
     * her call: she sleeps on, and no Cryona herself comes out), Black Frost sits behind her where it was set, facing as
     * she does, and is no one's (no rider, no lead, no foe), and an admin's /kill takes them away whole. Then
     * an older world's throne in the same place (olderThrone).
     */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 260)
    public void cryonaAsleepOnHerThrone(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos bed = helper.absolutePos(new BlockPos(5, 2, 5));
        BlockPos seat = helper.absolutePos(new BlockPos(1, 2, 1));
        var frost = com.iafabric.entity.EntityBlackFrostGuard.make(level, level.getRandom(), bed, 0F);
        level.addFreshEntity(frost);
        var queen = com.iafabric.entity.EntityCryonaAsleep.make(level, seat, bed, seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5, 180F, MobSpawnType.COMMAND);
        level.addFreshEntity(queen);
        helper.assertTrue(frost.isBlackFrost(), "Black Frost by her throne has not its look");
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(1, 2, 3));
        zombie.setNoAi(true);
        float full = queen.getHealth();
        float frostFull = frost.getHealth();
        queen.hurt(level.damageSources().mobAttack(zombie), 20);
        frost.hurt(level.damageSources().mobAttack(zombie), 2000);
        zombie.discard();
        net.minecraft.server.level.ServerPlayer[] player = new net.minecraft.server.level.ServerPlayer[1];
        helper.runAfterDelay(10, () -> {
            // a player near: their blows on both, a hand on Black Frost
            player[0] = survivalPlayer(helper);
            var near = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2, 2, 6));
            player[0].moveTo(near.x, near.y, near.z, 0, 0);
            queen.hurt(level.damageSources().playerAttack(player[0]), 50);
            frost.hurt(level.damageSources().playerAttack(player[0]), 5000);
            click(frost, player[0]);
        });
        helper.runAfterDelay(80, () -> {
            // (in its own space: the tests beside this one have Cryonas of their own)
            var herself = level.getEntitiesOfClass(com.iafabric.entity.EntityDreadQueen.class, helper.getBounds().inflate(4));
            helper.assertTrue(!queen.isRemoved() && queen.getHealth() == full && queen.isNoAi(), "Cryona asleep was hurt, woke or is gone");
            helper.assertTrue(!frost.isRemoved() && frost.getHealth() == frostFull, "Black Frost by her throne was hurt or is gone: " + frost.getHealth() + " / " + frostFull);
            helper.assertTrue(herself.isEmpty(), "Cryona herself came out into the world: " + herself.size());
            helper.assertTrue(frost.getTarget() == null && !frost.isVehicle() && !player[0].isPassenger() && !frost.isSleeping(), "Black Frost by her throne has a foe or a rider, or sleeps");
            helper.assertTrue(!frost.canBeLeashed() && !queen.canBeLeashed(), "they can be led");
            // it sits where it was set (behind her: she faces north, behind her is south), facing as it was set
            double off = Math.sqrt(frost.distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(bed)));
            helper.assertTrue(off < 1 && frost.isOrderedToSit() && frost.getZ() > queen.getZ() + 2 && Math.abs(net.minecraft.util.Mth.wrapDegrees(frost.yBodyRot - 0F)) < 5,
                "Black Frost does not sit behind her throne: " + Math.round(off * 10) / 10.0 + " off its place, sitting " + frost.isOrderedToSit() + ", body " + frost.yBodyRot);
            log("on her throne: Cryona and Black Frost unhurt by a mob's blow and a player's, no rider, no Cryona out in the world (no realm here: she sleeps on); "
                + "Black Frost sits behind her, still where it was set");
            helper.getLevel().getServer().getPlayerList().remove(player[0]);
            // an admin's /kill takes them away whole
            queen.kill();
            frost.kill();
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(queen.isRemoved() && frost.isRemoved(), "a /kill did not take them away");
            // (what a dragon or she would drop: the tests beside this one leave their own things)
            var left = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atCenterOf(seat), net.minecraft.world.phys.Vec3.atCenterOf(bed)).inflate(6),
                item -> BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).getPath().matches(".*(dragon|dread_queen).*"));
            helper.assertTrue(left.isEmpty(), "they left things behind: " + left.stream().map(item -> item.getItem() + " at " + item.blockPosition()).toList() + " (bed " + bed + ")");
            for (var minion : level.getEntitiesOfClass(com.iafabric.entity.EntityDreadMob.class, new net.minecraft.world.phys.AABB(bed).inflate(40))) {
                minion.discard();
            }
            // (then, in the same place: one test at a time, or her guard would guard the queen of the test beside it)
            olderThrone(helper, seat, bed);
        });
    }

    /**
     * Cryona and Black Frost asleep on her throne in an older world (the two themselves, lying still) become her sign
     * as they come in: Cryona asleep (her throne kept, Black Frost's place found) and Black Frost prowling behind her.
     */
    private static void olderThrone(GameTestHelper helper, BlockPos seat, BlockPos bed) {
        var level = helper.getLevel();
        var access = level.registryAccess();
        // as an older world saved them: Cryona herself asleep ("Dormant"), Black Frost itself lying still
        var older = IafEntityRegistry.DREAD_QUEEN.create(level);
        older.moveTo(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5, 180, 0);
        var queenTag = older.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        queenTag.putBoolean("Dormant", true);
        queenTag.putBoolean("NoAI", true);
        queenTag.putLong("Throne", seat.asLong());
        var queen = IafEntityRegistry.DREAD_QUEEN.create(level);
        queen.load(queenTag);
        level.addFreshEntity(queen);
        EntityDragonBase olderFrost = com.iafabric.world.structure.DreadPieces.dreadDragon(level, level.getRandom(), bed, 125, true);
        olderFrost.setNoAi(true);
        olderFrost.setInSittingPose(true);
        var frostOut = olderFrost.saveWithoutId(new net.minecraft.nbt.CompoundTag());
        var frost = IafEntityRegistry.ICE_DRAGON.create(level);
        frost.load(frostOut);
        level.addFreshEntity(frost);
        helper.runAfterDelay(25, () -> {
            var signs = level.getEntitiesOfClass(com.iafabric.entity.EntityCryonaAsleep.class, new net.minecraft.world.phys.AABB(seat).inflate(8));
            var asleep = level.getEntitiesOfClass(com.iafabric.entity.EntityBlackFrostGuard.class, helper.getBounds().inflate(4));
            // (in its own space: the tests beside this one have Cryonas of their own)
            var herself = level.getEntitiesOfClass(com.iafabric.entity.EntityDreadQueen.class, helper.getBounds().inflate(4));
            var itself = level.getEntitiesOfClass(EntityDragonBase.class, helper.getBounds().inflate(4),
                dragon -> !(dragon instanceof com.iafabric.entity.EntityBlackFrostGuard));
            helper.assertTrue(queen.isRemoved() && herself.isEmpty(), "the older Cryona asleep is still herself");
            helper.assertTrue(frost.isRemoved() && itself.isEmpty(), "the older Black Frost asleep is still itself");
            helper.assertTrue(signs.size() == 1 && signs.get(0).throne().equals(seat),
                "Cryona asleep is not on her throne: " + signs.size() + " " + (signs.isEmpty() ? "-" : signs.get(0).throne()));
            helper.assertTrue(asleep.size() == 1, "Black Frost is not by her throne: " + asleep.size());
            log("an older throne: Cryona and Black Frost asleep became her sign (her throne " + signs.get(0).throne() + ", its place " + signs.get(0).bed() + ")");
            signs.forEach(Entity::discard);
            asleep.forEach(Entity::discard);
            helper.succeed();
        });
    }

    /**
     * The test's own mock player (always in creative), put in the test's space: it starts at the world's 0 0 0, a
     * chunk whose entities may not be ticking (what it throws there, or what looks for it there, is not seen).
     */
    @SuppressWarnings("removal")
    private static net.minecraft.server.level.ServerPlayer mockPlayer(GameTestHelper helper) {
        var player = (net.minecraft.server.level.ServerPlayer) helper.makeMockServerPlayerInLevel();
        var inside = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        player.moveTo(inside.x, inside.y, inside.z, 0, 0);
        return player;
    }

    /** A test player in survival, joined like the test's own mock player (which is always in creative). */
    private static net.minecraft.server.level.ServerPlayer survivalPlayer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-survivor"), false);
        var player = new net.minecraft.server.level.ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    /** A mob turned to stone (what the gorgon head does) keeps its kind and is a statue in the level. */
    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 40)
    public void stoneStatue(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Sheep sheep = helper.spawn(net.minecraft.world.entity.EntityType.SHEEP, new BlockPos(2, 2, 2));
        com.iafabric.entity.EntityStoneStatue statue = com.iafabric.entity.EntityStoneStatue.buildStatueEntity(sheep);
        statue.moveTo(sheep.getX(), sheep.getY(), sheep.getZ(), sheep.getYRot(), sheep.getXRot());
        helper.getLevel().addFreshEntity(statue);
        sheep.discard();
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(statue.isAlive(), "statue gone");
            helper.assertTrue(statue.getTrappedEntityType() == net.minecraft.world.entity.EntityType.SHEEP, "statue is not of a sheep: " + statue.getTrappedEntityTypeString());
            statue.discard();
            helper.succeed();
        });
    }

	/**
	 * Every worldgen feature of the mod placed for real (rarity set to 1 and spawn distance to 0 for the test),
	 * far from spawn in the test world, each at its own place. A feature may decline (wrong ground); it must not throw.
	 */
	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void featuresPlace(GameTestHelper helper) {
		java.util.Map<java.lang.reflect.Field, Object> saved = new java.util.HashMap<>();
		try {
			for (java.lang.reflect.Field field : com.iafabric.IafConfig.class.getFields()) {
				String name = field.getName();
				if (field.getType() == int.class && name.toLowerCase().contains("chance") && (name.startsWith("generate") || name.startsWith("spawn") || name.equals("myrmexColonyGenChance"))) {
					saved.put(field, field.get(null));
					field.setInt(null, 1);
				}
				if (name.equals("dangerousWorldGenDistanceLimit") || name.equals("dangerousWorldGenSeparationLimit")) {
					saved.put(field, field.get(null));
					field.setDouble(null, 0);
				}
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		net.minecraft.server.level.ServerLevel level = helper.getLevel();
		var generator = level.getChunkSource().getGenerator();
		var features = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.FEATURE);
		List<String> failed = new ArrayList<>();
		int index = 0;
		for (var entry : features.listElements().toList()) {
			var id = entry.key().location();
			if (!id.getNamespace().equals(IafMod.MOD_ID)) {
				continue;
			}
			index++;
			int x = 3000 + index * 160;
			int z = 3000;
			level.getChunk(x >> 4, z >> 4);
			int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
			BlockPos origin = new BlockPos(x, y, z);
			int placed = 0;
			try {
				for (int attempt = 0; attempt < 4; attempt++) {
					@SuppressWarnings("unchecked")
					var feature = (net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>) entry.value();
					if (feature.place(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE, level, generator,
						net.minecraft.util.RandomSource.create(attempt * 31L + index), origin.offset(attempt * 24, 0, 0))) {
						placed++;
					}
				}
				log("feature " + id + " placed " + placed + "/4 at " + origin);
			} catch (Throwable t) {
				failed.add(id + ": " + t);
				IafMod.LOGGER.error("[IAFTEST] feature {} threw", id, t);
			}
		}
		saved.forEach((field, value) -> {
			try {
				field.set(null, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		});
		helper.assertTrue(failed.isEmpty(), "features threw: " + failed);
		helper.succeed();
	}
}
