"""Hand fixes for the server game tests where 1.21.1 calls differ (one-off; each fix is checked to apply once)."""
import pathlib
import re

D = pathlib.Path(__file__).resolve().parent.parent / "src/gametest/java/com/iafabric/test"


def edit(name, pairs, regex=()):
    p = D / name
    t = p.read_text(encoding="utf-8")
    for a, b in pairs:
        if b in t and a not in t:
            continue
        assert a in t, (name, a[:70])
        t = t.replace(a, b)
    for a, b in regex:
        t = re.sub(a, b, t)
    p.write_text(t, encoding="utf-8")


edit("IafBossTests.java", [
    ("""		var recipes = level.getServer().getRecipeManager();
		for (String kind : new String[]{"potion", "splash_potion", "lingering_potion"}) {
			var key = ResourceKey.create(Registries.RECIPE, IafMod.id("brewing/" + kind + "_awkward_ice_dragon_blood"));
			helper.assertTrue(recipes.byKey(key).isPresent(), "no brewing of the long ward: " + key.location());
		}""", """		var brewing = level.potionBrewing();
		ItemStack blood = new ItemStack(IafItemRegistry.ICE_DRAGON_BLOOD);
		for (var kind : new net.minecraft.world.item.Item[]{net.minecraft.world.item.Items.POTION, net.minecraft.world.item.Items.SPLASH_POTION,
			net.minecraft.world.item.Items.LINGERING_POTION}) {
			ItemStack awkward = net.minecraft.world.item.alchemy.PotionContents.createItemStack(kind, net.minecraft.world.item.alchemy.Potions.AWKWARD);
			ItemStack brewed = brewing.mix(blood, awkward);
			helper.assertTrue(brewing.hasMix(awkward, blood) && potion(brewed, IafEffects.LONG_FROST_WARD_POTION),
				"no brewing of the long ward in " + kind + ": " + brewed);
		}"""),
    ("""		var scribe = BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(IafVillagerRegistry.SCRIBE);
		villager.setVillagerData(villager.getVillagerData().withProfession(scribe).withLevel(1));""",
     """		villager.setVillagerData(villager.getVillagerData().setProfession(IafVillagerRegistry.SCRIBE).setLevel(1));"""),
    ("""		villager.setVillagerData(villager.getVillagerData().withLevel(3));
		IafVillagerRegistry.addSureTrades(villager, level);""", """		villager.setVillagerData(villager.getVillagerData().setLevel(3));
		IafVillagerRegistry.addSureTrades(villager);"""),
    ("var middle = net.minecraft.world.level.new ChunkPos(bed);", "var middle = new net.minecraft.world.level.ChunkPos(bed);"),
    ("if (!villager.getVillagerData().profession().is(IafVillagerRegistry.SCRIBE_KEY)) {",
     "if (villager.getVillagerData().getProfession() != IafVillagerRegistry.SCRIBE) {"),
    ('"the villager did not take the lectern: " + villager.getVillagerData().profession().unwrapKey()',
     '"the villager did not take the lectern: " + villager.getVillagerData().getProfession()'),
    ("""	/** The mod's copper is the game's now: an old world's iceandfire_fabric copper is read as the game's, not lost. */""",
     """	/**
	 * The game's copper ingot and ore stand for the mod's old ones (an old world's are read as the game's, not lost); the
	 * nugget, tools and armor are still the mod's (1.21.1 has none).
	 */"""),
    ("""		helper.assertTrue(items.getValue(net.minecraft.resources.ResourceLocation.parse("iceandfire_fabric:armor_copper_metal_helmet")) == net.minecraft.world.item.Items.COPPER_HELMET,
			"an old copper helmet is not the game's");""",
     """		helper.assertTrue(items.get(net.minecraft.resources.ResourceLocation.parse("iceandfire_fabric:armor_copper_metal_helmet")) == IafItemRegistry.COPPER_HELMET,
			"the copper helmet is not the mod's");"""),
    ("""		helper.assertTrue(items.get(net.minecraft.resources.ResourceLocation.parse("iceandfire_fabric:copper_nugget")).isPresent(), "an old copper nugget is lost");""",
     """		helper.assertTrue(items.get(net.minecraft.resources.ResourceLocation.parse("iceandfire_fabric:copper_nugget")) == IafItemRegistry.COPPER_NUGGET, "the copper nugget is lost");"""),
    ("""log("old copper: the mod's copper ingot, nugget, ore and armor are read as the game's");""",
     """log("old copper: the ingot and ore read as the game's, the nugget and armor the mod's own");"""),
    ("""helper.assertTrue(items.get(old.apply("dragonbone")).isPresent(), "an old dragon bone is not found");""",
     """helper.assertTrue(items.getHolder(old.apply("dragonbone")).isPresent(), "an old dragon bone is not found");"""),
    ("""helper.assertTrue(structures.get(old.apply("dragon_roost")).isPresent(), "an old dragon roost is lost");""",
     """helper.assertTrue(structures.get(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE, old.apply("dragon_roost"))).isPresent(), "an old dragon roost is lost");"""),
    ("helper.assertTrue(drawn.consumesAction(),", "helper.assertTrue(drawn.getResult().consumesAction(),"),
], [
    (r"chunk\.x\(\)", "chunk.x"), (r"chunk\.z\(\)", "chunk.z"), (r"middle\.x\(\)", "middle.x"), (r"middle\.z\(\)", "middle.z"),
    (r"(items|blocks|entities)\.getValue\(", r"\1.get("),
])

edit("IafFairyGameTests.java", [
    ("""BlockPos shown = ExiledQuests.extra(player, d).getLong("Hint").map(BlockPos::of).orElse(null);""",
     """var extra = ExiledQuests.extra(player, d);
			BlockPos shown = extra.contains("Hint") ? BlockPos.of(extra.getLong("Hint")) : null;"""),
])

edit("IafGameTests.java", [
    ("""textKeys(keys, profession.name(), "profession");""",
     """textKeys(keys, net.minecraft.network.chat.Component.translatable("entity.minecraft.villager." + profession.name()), "profession");"""),
    ("""textKeys(keys, display.title(), "advancement \"""", """textKeys(keys, display.getTitle(), "advancement \""""),
    ("""textKeys(keys, display.description(), "advancement \"""", """textKeys(keys, display.getDescription(), "advancement \""""),
    ("""				var saving = new net.minecraft.util.ProblemReporter.Collector();
				var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(saving, access);
				entity.saveWithoutId(output);
				if (!saving.isEmpty()) {
					failed.add(id + " saving: " + saving.getReport());
				}
				Entity copy = type.create(helper.getLevel());
				var loading = new net.minecraft.util.ProblemReporter.Collector();
				copy.load(net.minecraft.world.level.storage.TagValueInput.create(loading, access, output.buildResult()));
				if (!loading.isEmpty()) {
					failed.add(id + " loading: " + loading.getReport());
				}""", """				var output = entity.saveWithoutId(new net.minecraft.nbt.CompoundTag());
				Entity copy = type.create(helper.getLevel());
				copy.load(output);"""),
    ("""        var queenOut = net.minecraft.world.level.storage.TagValueOutput.createWithContext(new net.minecraft.util.ProblemReporter.Collector(), access);
        older.saveWithoutId(queenOut);
        var queenTag = queenOut.buildResult();""", """        var queenTag = older.saveWithoutId(new net.minecraft.nbt.CompoundTag());"""),
    ("""        queen.load(net.minecraft.world.level.storage.TagValueInput.create(new net.minecraft.util.ProblemReporter.Collector(), access, queenTag));""",
     """        queen.load(queenTag);"""),
    ("""        var frostOut = net.minecraft.world.level.storage.TagValueOutput.createWithContext(new net.minecraft.util.ProblemReporter.Collector(), access);
        olderFrost.saveWithoutId(frostOut);""", """        var frostOut = olderFrost.saveWithoutId(new net.minecraft.nbt.CompoundTag());"""),
    ("""        frost.load(net.minecraft.world.level.storage.TagValueInput.create(new net.minecraft.util.ProblemReporter.Collector(), access, frostOut.buildResult()));""",
     """        frost.load(frostOut);"""),
    ("player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(player.getMainHandItem()));",
     "player.getCooldowns().removeCooldown(player.getMainHandItem().getItem());"),
    ("player.getCooldowns().isOnCooldown(player.getMainHandItem())", "player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())"),
    ("player.gameMode()", "player.gameMode.getGameModeForPlayer()"),
    ("""					if (entry.value().place(level, generator, net.minecraft.util.RandomSource.create(attempt * 31L + index), origin.offset(attempt * 24, 0, 0))) {""",
     """					@SuppressWarnings("unchecked")
					var feature = (net.minecraft.world.level.levelgen.feature.Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>) entry.value();
					if (feature.place(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE, level, generator,
						net.minecraft.util.RandomSource.create(attempt * 31L + index), origin.offset(attempt * 24, 0, 0))) {"""),
], [
    (r"(\w+(?:\[0\])?)\.interact\((\w+(?:\[0\])?), net\.minecraft\.world\.InteractionHand\.MAIN_HAND, \w+\.position\(\)\)",
     r"\1.interact(\2, net.minecraft.world.InteractionHand.MAIN_HAND)"),
    (r"player\.setInvulnerableTime\(0\);", "player.invulnerableTime = 0;"),
])
print("sites: done")
