package com.iafabric.test;

import com.iafabric.IafMod;
import com.iafabric.test.ct.FabricClientGameTest;
import com.iafabric.test.ct.ClientGameTestContext;
import com.iafabric.test.ct.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.ArrayList;
import java.util.List;

/**
 * Client checks with screenshots (build/run/clientGameTest/screenshots): every block of the mod
 * placed in a flat world, every item in item frames, and the creatures in a row.
 */
public class IafClientTests implements FabricClientGameTest {
	private static final int GROUND = -60;

	private static void log(String message) {
		IafMod.LOGGER.info("[IAFCLIENT] {}", message);
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("IAF_BATTLE") != null || IafTestOnly.skip("scenes")) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(settings -> {
			settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
			settings.setGenerateStructures(false);
			for (WorldCreationUiState.WorldTypeEntry entry : settings.getNormalPresetList()) {
				if (entry.preset() != null && entry.preset().is(WorldPresets.FLAT)) {
					settings.setWorldType(entry);
				}
			}
		}).create()) {
			world.getServer().runCommand("gamerule doDaylightCycle false");
			world.getServer().runCommand("gamerule doWeatherCycle false");
			world.getServer().runCommand("gamerule doMobSpawning false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			context.waitTicks(20);

			world.getServer().runOnServer(server -> placeBlocks(server.overworld()));
			context.waitTicks(20);
			world.getServer().runOnServer(server -> log("server sees at 0,-60,0: " + server.overworld().getBlockState(new BlockPos(0, GROUND, 0))
				+ " / 2,-60,0: " + server.overworld().getBlockState(new BlockPos(2, GROUND, 0))));
			context.runOnClient(mc -> log("client sees at 0,-60,0: " + mc.level.getBlockState(new BlockPos(0, GROUND, 0))
				+ " / 2,-60,0: " + mc.level.getBlockState(new BlockPos(2, GROUND, 0)) + " player at " + mc.player.position()));
			look(context, world, 12.5, -48, -6, 0, 45);
			context.takeScreenshot("iaf_blocks");

			world.getServer().runOnServer(server -> placeItems(server.overworld()));
			look(context, world, 7.5, -55, 49, 180, 0);
			context.takeScreenshot("iaf_items_1");
			look(context, world, 22.5, -55, 49, 180, 0);
			context.takeScreenshot("iaf_items_2");
			look(context, world, 7.5, -47, 49, 180, 0);
			context.takeScreenshot("iaf_items_3");
			look(context, world, 22.5, -47, 49, 180, 0);
			context.takeScreenshot("iaf_items_4");

			world.getServer().runOnServer(server -> placeDragons(server.overworld()));
			look(context, world, 14.5, -56, -88, 0, 8);
			context.takeScreenshot("iaf_dragons");
			look(context, world, -4.5, -57, -66, 270, 5);
			context.takeScreenshot("iaf_dragons_side");

			world.getServer().runOnServer(server -> placeCreatures(server.overworld()));
			int[] maxX = new int[1];
			context.runOnClient(mc -> maxX[0] = (int) CREATURES_END);
			for (int x = 0, shot = 0; x < maxX[0]; x += 14, shot++) {
				look(context, world, x + 7.5, -56.5, -31, 0, 12);
				context.takeScreenshot("iaf_creatures_" + shot);
			}
			context.runOnClient(mc -> log("the player's effects among the creatures: " + mc.player.getActiveEffects()));
			// The statues up close, a live warden beside them to compare.
			look(context, world, 7.5, GROUND + 2.5, -16, 0, 12);
			context.takeScreenshot("iaf_statues");
			world.getServer().runOnServer(server -> {
				// The first creatures frozen, to see the ice around them.
				for (var mob : server.overworld().getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(0, GROUND - 1, -25, 14, GROUND + 10, -15))) {
					com.iafabric.entity.props.FrozenProperties.setFrozenFor(mob, 1000);
				}
			});
			look(context, world, 7.5, -56.5, -31, 0, 12);
			context.takeScreenshot("iaf_frozen");
			world.getServer().runOnServer(server -> placeArmors(server.overworld()));
			for (int x = 0, shot = 0; x < ARMOR_SETS * 2; x += 16, shot++) {
				look(context, world, x + 8, GROUND + 1.5, -112, 0, 5);
				context.takeScreenshot("iaf_armor_" + shot);
			}
			int sx = -80;
			for (String structure : List.of("graveyard", "mausoleum", "gorgon_temple")) {
				int z = 120;
				look(context, world, sx - 18, GROUND + 28, z - 18, -45, 35);
				String command = "place structure iceandfire_fabric:" + structure + " " + sx + " " + GROUND + " " + z;
				world.getServer().runOnServer(server -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSource(new net.minecraft.commands.CommandSource() {
					public void sendSystemMessage(net.minecraft.network.chat.Component message) {
						log(structure + ": " + message.getString());
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
				context.waitTicks(60);
				int[] center = new int[2];
				int fx = sx;
				world.getServer().runOnServer(server -> {
					// Where it was built: the middle of the blocks above the flat ground around the place.
					long cx = 0, cz = 0, n = 0;
					for (int x = fx - 96; x <= fx + 96; x++) {
						for (int zz = z - 96; zz <= z + 96; zz++) {
							if (!server.overworld().getBlockState(new BlockPos(x, GROUND + 1, zz)).isAir()) {
								cx += x;
								cz += zz;
								n++;
							}
						}
					}
					center[0] = n == 0 ? fx : (int) (cx / n);
					center[1] = n == 0 ? z : (int) (cz / n);
					log(structure + " built around " + center[0] + ", " + center[1] + " (" + n + " blocks)");
				});
				look(context, world, center[0], GROUND + 30, center[1] - 30, 0, 35);
				context.runOnClient(mc -> log("camera at " + mc.player.position() + " pitch " + mc.player.getXRot() + " vehicle " + mc.player.getVehicle()));
				context.takeScreenshot("iaf_structure_" + structure);
				sx -= 120;
			}
			world.getServer().runOnServer(server -> {
				var pools = server.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
				for (String type : List.of("plains", "desert", "snowy", "savanna", "taiga")) {
					var pool = pools.get(new net.minecraft.resources.ResourceLocation("minecraft:village/" + type + "/houses"));
					long scribe = pool == null ? -1 : ((com.iafabric.mixin.StructureTemplatePoolAccessor) pool).iceandfire_fabric$templates().stream()
						.filter(e -> e.toString().contains("scriber")).count();
					log("village " + type + " houses with the scribe's house: " + scribe);
				}
			});
			look(context, world, -400, GROUND + 12, 100, 0, 30);
			world.getServer().runCommand("place template iceandfire_fabric:village/plains_scriber_1 -405 " + (GROUND + 1) + " 110");
			context.waitTicks(40);
			context.takeScreenshot("iaf_structure_scribe_house");
			// A siren charms the player: the screen gets the pink veil.
			look(context, world, 7.5, -56.5, -31, 0, 12);
			world.getServer().runOnServer(server -> {
				var level = server.overworld();
				var player = server.getPlayerList().getPlayers().get(0);
				var siren = com.iafabric.entity.IafEntityRegistry.SIREN.create(level);
				siren.setNoAi(true);
				siren.moveTo(7.5, GROUND, -16, 180, 0);
				level.addFreshEntity(siren);
				siren.setSinging(true);
				// The charm lets go of players in creative.
				player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
				com.iafabric.entity.props.SirenProperties.setCharmedBy(player, siren);
			});
			context.waitTicks(10);
			context.runOnClient(mc -> log("charmed: " + com.iafabric.entity.props.SirenProperties.isCharmed(mc.player) + " effects " + (mc.gameRenderer.currentEffect() == null ? "none" : mc.gameRenderer.currentEffect().getName())));
			context.takeScreenshot("iaf_siren_charm");
			world.getServer().runOnServer(server -> {
				server.getPlayerList().getPlayers().get(0).setGameMode(net.minecraft.world.level.GameType.CREATIVE);
				for (var siren : server.overworld().getEntitiesOfClass(com.iafabric.entity.EntitySiren.class, new net.minecraft.world.phys.AABB(0, GROUND - 2, -25, 15, GROUND + 5, -15))) {
					siren.discard();
				}
			});
			context.waitTicks(20);
			screens(context, world, "");
			// The same screens in Spanish: a longer text must stay in its place.
			setLanguage(context, "es_es");
			screens(context, world, "_es");
			// a Spanish of the Americas reads the book of Spain (it has no book of its own)
			setLanguage(context, "es_mx");
			screens(context, world, "_mx");
			setLanguage(context, "en_us");
			riding(context, world);
		}
	}

	/** The mod's screens: the bestiary (index and a page), a dragon's inventory, the dragon forge and the lectern. */
	private static void screens(ClientGameTestContext context, TestSingleplayerContext world, String suffix) {
		context.runOnClient(mc -> {
			ItemStack book = new ItemStack(com.iafabric.item.IafItemRegistry.BESTIARY);
			int[] pages = new int[com.iafabric.enums.EnumBestiaryPages.values().length];
			for (int i = 0; i < pages.length; i++) {
				pages[i] = i;
			}
			com.iafabric.util.ItemNbt.update(book, tag -> tag.putIntArray("Pages", pages));
			IafMod.PROXY.openBestiaryGui(book);
		});
		context.waitTicks(20);
		context.takeScreenshot("iaf_gui_bestiary_index" + suffix);
		context.runOnClient(mc -> {
			var bestiary = (com.iafabric.client.gui.bestiary.GuiBestiary) mc.screen;
			bestiary.index = false;
			bestiary.pageType = com.iafabric.enums.EnumBestiaryPages.FIREDRAGON;
			bestiary.bookPages = 0;
		});
		context.waitTicks(10);
		context.takeScreenshot("iaf_gui_bestiary_page" + suffix);
		context.runOnClient(mc -> mc.setScreen(null));

		BlockPos forge = new BlockPos(-10, GROUND, -10);
		BlockPos lectern = new BlockPos(-12, GROUND, -10);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			level.setBlock(forge, com.iafabric.block.IafBlockRegistry.DRAGONFORGE_FIRE_CORE.defaultBlockState(), 2);
			level.setBlock(lectern, com.iafabric.block.IafBlockRegistry.LECTERN.defaultBlockState(), 2);
		});
		look(context, world, -11, GROUND + 1, -12, 0, 30);
		for (BlockPos pos : List.of(forge, lectern)) {
			context.runOnClient(mc -> IafMod.PROXY.setRefrencedTE(mc.level.getBlockEntity(pos)));
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				player.openMenu((net.minecraft.world.MenuProvider) server.overworld().getBlockEntity(pos));
			});
			context.waitTicks(20);
			context.takeScreenshot((pos.equals(forge) ? "iaf_gui_forge" : "iaf_gui_lectern") + suffix);
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).closeContainer());
			context.waitTicks(5);
		}

		look(context, world, 14.5, GROUND + 1, -71, 0, 10);
		int[] dragonId = {-1};
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			for (var dragon : server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityDragonBase.class, player.getBoundingBox().inflate(7))) {
				dragonId[0] = dragon.getId();
				dragon.openInventory(player);
				log("opened dragon " + dragon + " menu " + player.containerMenu);
				break;
			}
		});
		context.runOnClient(mc -> IafMod.PROXY.setReferencedMob(mc.level.getEntity(dragonId[0])));
		context.waitTicks(2);
		context.runOnClient(mc -> log("dragon id " + dragonId[0] + " client screen " + mc.screen));
		context.waitTicks(20);
		context.takeScreenshot("iaf_gui_dragon" + suffix);
		world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().get(0).closeContainer());
		context.waitTicks(5);

		// The myrmex staff: a colony with one room of each kind, and the screen to add a room.
		context.runOnClient(mc -> {
			var hive = new com.iafabric.entity.util.MyrmexHive(mc.level, new BlockPos(-1200, 40, 3400), 100);
			hive.colonyName = "Reina Hormiguera";
			hive.addRoom(new BlockPos(-1234, 37, 3456), com.iafabric.world.gen.WorldGenMyrmexHive.RoomType.FOOD);
			hive.addRoom(new BlockPos(-1250, 30, 3470), com.iafabric.world.gen.WorldGenMyrmexHive.RoomType.NURSERY);
			hive.addRoom(new BlockPos(-1210, 33, 3480), com.iafabric.world.gen.WorldGenMyrmexHive.RoomType.EMPTY);
			hive.getEntrances().put(new BlockPos(-1222, 70, 3444), Direction.NORTH);
			hive.getEntranceBottoms().put(new BlockPos(-1222, 45, 3444), Direction.NORTH);
			IafMod.PROXY.setReferencedHive(hive);
			IafMod.PROXY.openMyrmexStaffGui(new ItemStack(com.iafabric.item.IafItemRegistry.MYRMEX_DESERT_STAFF));
		});
		context.waitTicks(10);
		context.takeScreenshot("iaf_gui_myrmex_staff" + suffix);
		context.runOnClient(mc -> IafMod.PROXY.openMyrmexAddRoomGui(new ItemStack(com.iafabric.item.IafItemRegistry.MYRMEX_JUNGLE_STAFF), new BlockPos(-1234, 37, 3456), Direction.NORTH));
		context.waitTicks(10);
		context.takeScreenshot("iaf_gui_myrmex_add_room" + suffix);
		context.runOnClient(mc -> {
			// the screen sends its hive back when it closes: keep it from reaching the server's (no such hive there)
			IafMod.PROXY.setReferencedHive(null);
			mc.setScreen(null);
		});
		context.waitTicks(5);
		if (suffix.isEmpty()) {
			logBestiaryWidths(context);
		}
	}

	/** The game's language, changed as its menu does (only the texts are loaded again). */
	private static void setLanguage(ClientGameTestContext context, String code) {
		context.runOnClient(mc -> {
			mc.getLanguageManager().setSelected(code);
			mc.options.languageCode = code;
			mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
			log("language " + code + ": " + net.minecraft.client.resources.language.I18n.get("item.iceandfire_fabric.dragonbone"));
		});
		context.waitTicks(5);
	}

	/** How wide the longest line of each bestiary page is, in English and Spanish, with the font each uses. */
	private static void logBestiaryWidths(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			for (String lang : List.of("en_us", "es_es")) {
				var font = lang.equals("en_us") && !com.iafabric.IafConfig.useVanillaFont ? (net.minecraft.client.gui.Font) IafMod.PROXY.getFontRenderer() : mc.font;
				int widest = 0;
				String where = "";
				for (var entry : mc.getResourceManager().listResources("lang/bestiary/" + lang + "_0", id -> id.getPath().endsWith(".txt")).entrySet()) {
					try (var reader = entry.getValue().openAsReader()) {
						int number = 0;
						for (String line : reader.lines().toList()) {
							line = line.trim();
							if (line.contains("<") || line.contains(">")) {
								continue;
							}
							int width = font.width(line);
							if (width > widest) {
								widest = width;
								where = entry.getKey().getPath() + " line " + number;
							}
							number++;
						}
					} catch (java.io.IOException e) {
						log("could not read " + entry.getKey());
					}
				}
				log("bestiary " + lang + ": widest line " + widest + " (" + where + ")");
			}
		});
	}

	/** Getting on a dragon from its screen's place: the view from its back. */
	private static void riding(ClientGameTestContext context, TestSingleplayerContext world) {
		look(context, world, 14.5, GROUND + 1, -71, 0, 10);
		int[] dragonId = {-1};
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			for (var dragon : server.overworld().getEntitiesOfClass(com.iafabric.entity.EntityDragonBase.class, player.getBoundingBox().inflate(7))) {
				dragonId[0] = dragon.getId();
				break;
			}
		});
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			var dragon = (com.iafabric.entity.EntityDragonBase) server.overworld().getEntity(dragonId[0]);
			dragon.tame(player);
			dragon.growDragon(60);
			player.startRiding(dragon, true);
		});
		context.waitTicks(40);
		context.runOnClient(mc -> log("riding " + mc.player.getVehicle() + " camera " + mc.options.getCameraType()));
		context.takeScreenshot("iaf_riding");
	}

	/** Puts the player (flying, chat hidden) at a place looking somewhere, and waits for the view to settle. */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, double x, double y, double z, float yaw, float pitch) {
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		world.getServer().runCommand(String.format(java.util.Locale.ROOT, "tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		context.runOnClient(mc -> {
			mc.player.getAbilities().flying = true;
			if (!mc.options.hideGui) {
				mc.options.hideGui = !mc.options.hideGui;
			}
		});
		context.waitTicks(40);
	}

	/** Every block of the mod on the ground, 13 to a row, two blocks apart. */
	private static void placeBlocks(ServerLevel level) {
		List<Block> blocks = new ArrayList<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			if (IafMod.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
				blocks.add(block);
			}
		}
		for (int i = 0; i < blocks.size(); i++) {
			BlockPos pos = new BlockPos((i % 13) * 2, GROUND, (i / 13) * 2);
			try {
				level.setBlock(pos, blocks.get(i).defaultBlockState(), 2);
			} catch (Throwable t) {
				log("could not place " + BuiltInRegistries.BLOCK.getKey(blocks.get(i)) + ": " + t);
			}
		}
		log("placed " + blocks.size() + " blocks");
	}

	/** A stone wall at z = 40 with every item of the mod in a frame on its south side, 15 to a row. */
	private static void placeItems(ServerLevel level) {
		List<Item> items = new ArrayList<>();
		for (Item item : BuiltInRegistries.ITEM) {
			if (IafMod.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
				items.add(item);
			}
		}
		int columns = 30;
		int rows = (items.size() + columns - 1) / columns;
		for (int x = 0; x < columns; x++) {
			for (int y = 0; y < rows; y++) {
				level.setBlock(new BlockPos(x, GROUND + y, 40), Blocks.STONE.defaultBlockState(), 2);
			}
		}
		for (int i = 0; i < items.size(); i++) {
			int x = i % columns;
			int y = rows - 1 - i / columns;
			BlockPos pos = new BlockPos(x, GROUND + y, 41);
			ItemFrame frame = new ItemFrame(level, pos, Direction.SOUTH);
			frame.setItem(new ItemStack(items.get(i)), false);
			level.addFreshEntity(frame);
		}
		log("framed " + items.size() + " items in " + rows + " rows");
	}

	/** A young fire, ice and lightning dragon side by side (stage 2), and a grown fire dragon behind them. */
	private static void placeDragons(ServerLevel level) {
		int x = 4;
		for (EntityType<? extends com.iafabric.entity.EntityDragonBase> type : List.of(com.iafabric.entity.IafEntityRegistry.FIRE_DRAGON,
			com.iafabric.entity.IafEntityRegistry.ICE_DRAGON, com.iafabric.entity.IafEntityRegistry.LIGHTNING_DRAGON)) {
			com.iafabric.entity.EntityDragonBase dragon = type.create(level);
			dragon.setNoAi(true);
			dragon.setPersistenceRequired();
			dragon.growDragon(30);
			dragon.moveTo(x, GROUND, -66, 180, 0);
			level.addFreshEntity(dragon);
			x += 10;
		}
		com.iafabric.entity.EntityDragonBase big = com.iafabric.entity.IafEntityRegistry.FIRE_DRAGON.create(level);
		big.setNoAi(true);
		big.setPersistenceRequired();
		big.growDragon(90);
		big.moveTo(14, GROUND, -40, 180, 0);
		level.addFreshEntity(big);
	}

	private static int ARMOR_SETS;

	/** An armor stand for every armor set of the mod (helmet, chestplate, leggings, boots by name), in a row at z = -106. */
	private static void placeArmors(ServerLevel level) {
		java.util.Map<String, java.util.Map<net.minecraft.world.entity.EquipmentSlot, Item>> sets = new java.util.TreeMap<>();
		for (Item item : BuiltInRegistries.ITEM) {
			String id = BuiltInRegistries.ITEM.getKey(item).getPath();
			if (!IafMod.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()) || !(item instanceof com.iafabric.item.IafArmorItem armor)) {
				continue;
			}
			String set = id.replaceAll("_(helmet|chestplate|leggings|boots)$", "");
			sets.computeIfAbsent(set, k -> new java.util.EnumMap<>(net.minecraft.world.entity.EquipmentSlot.class)).put(armor.getEquipmentSlot(), item);
		}
		int x = 1;
		for (var entry : sets.entrySet()) {
			var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, x, GROUND, -106);
			stand.setYRot(180);
			stand.setYBodyRot(180);
			stand.setYHeadRot(180);
			entry.getValue().forEach((slot, item) -> stand.setItemSlot(slot, new ItemStack(item)));
			level.addFreshEntity(stand);
			log("armor " + entry.getKey() + " at x=" + x);
			x += 2;
		}
		ARMOR_SETS = sets.size();
	}

	private static double CREATURES_END;

	/** The mod's creatures in a row, without AI so they stay put, each with room for its size. */
	private static void placeCreatures(ServerLevel level) {
		double x = 1;
		for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
			if (!IafMod.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
				continue;
			}
			var entity = type.create(level);
			if (entity instanceof Mob mob) {
				mob.setNoAi(true);
				mob.setPersistenceRequired();
				if (mob instanceof com.iafabric.entity.EntityDragonBase dragon) {
					dragon.growDragon(10);
				}
				double width = Math.max(1.5, Math.min(mob.getBbWidth(), 8));
				mob.moveTo(x + width / 2, GROUND, -20, 180, 0);
				level.addFreshEntity(mob);
				log("creature " + BuiltInRegistries.ENTITY_TYPE.getKey(type) + " at x=" + (int) (x + width / 2));
				x += width + 1.5;
			}
		}
		// Mobs turned to stone, like the gorgon head does: a sheep and a zombie holding a sword.
		var sheep = net.minecraft.world.entity.EntityType.SHEEP.create(level);
		var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
		zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
		var warden = net.minecraft.world.entity.EntityType.WARDEN.create(level);
		double sx = 3;
		for (var mob : List.of(sheep, zombie, warden)) {
			mob.moveTo(sx, GROUND, -10, 180, 0);
			var statue = com.iafabric.entity.EntityStoneStatue.buildStatueEntity(mob);
			statue.moveTo(sx, GROUND, -10, 180, 0);
			level.addFreshEntity(statue);
			sx += 3;
		}
		var live = net.minecraft.world.entity.EntityType.WARDEN.create(level);
		live.setNoAi(true);
		live.setPersistenceRequired();
		live.moveTo(sx, GROUND, -10, 180, 0);
		level.addFreshEntity(live);
		CREATURES_END = x;
		log("placed creatures up to x=" + x);
	}
}
