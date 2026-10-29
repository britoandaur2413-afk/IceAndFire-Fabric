"""Item one-off patches for 1.21.1: sword tool rules, the forge recipe, the tide trident, wall items."""
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parent.parent / "src/main/java/com/iafabric"


def patch(rel, pairs, regex=()):
    p = ROOT / rel
    t = p.read_text(encoding="utf-8")
    for old, new in pairs:
        if old not in t:
            print("MISSING in", rel, ":", old[:80])
            continue
        t = t.replace(old, new)
    for pat, rep in regex:
        t, n = re.subn(pat, rep, t)
        if not n:
            print("NO MATCH in", rel, ":", pat[:80])
    p.write_text(t, encoding="utf-8")


# SwordItem.createToolProperties() is private in 1.21.1: the same rules here
patch("item/IafToolItem.java", [
    ("SwordItem.createToolProperties()", "swordTool()"),
    ("	@Override\n	public IafToolMaterial getTier() {", """	/** A sword's tool rules (cobwebs fast, sword-efficient blocks a bit faster), as vanilla swords have. */
	public static Tool swordTool() {
		return new Tool(List.of(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F), Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)), 1.0F, 2);
	}

	@Override
	public IafToolMaterial getTier() {"""),
    ("DragonSteelOverrides.appendHoverText(tier, tooltip);", "DragonSteelOverrides.appendHoverText(tier, tooltip::add);"),
    ("import net.minecraft.world.item.SwordItem;\n", "import net.minecraft.world.item.component.Tool;\nimport net.minecraft.world.level.block.Blocks;\n"),
], [(r"swordTool\(\)", "swordTool()")])

patch("item/IafItemRegistry.java", [
    ("new StandingAndWallBlockItem(block, wall.wallBlock(), Direction.DOWN, props)", "new StandingAndWallBlockItem(block, wall.wallBlock(), props, Direction.DOWN)"),
])

patch("recipe/DragonForgeRecipe.java", [
    ("import net.minecraft.world.item.ItemStackTemplate;\n", ""),
    ("import net.minecraft.world.item.crafting.PlacementInfo;\n", ""),
    ("import net.minecraft.client.RecipeBookCategories;\n", ""),
    ("import net.minecraft.world.item.crafting.RecipeBookCategory;\n", "import net.minecraft.core.HolderLookup;\nimport net.minecraft.core.NonNullList;\n"),
    ("ItemStackTemplate.CODEC.fieldOf(\"result\")", "ItemStack.STRICT_CODEC.fieldOf(\"result\")"),
    ("ItemStackTemplate.STREAM_CODEC, r -> r.result,", "ItemStack.STREAM_CODEC, r -> r.result,"),
    ("private final ItemStackTemplate result;", "private final ItemStack result;"),
    ("Ingredient blood, ItemStackTemplate result) {", "Ingredient blood, ItemStack result) {"),
    ("""	public ItemStack getResultItem() {
		return result.create();
	}""", """	public ItemStack getResultItem() {
		return result.copy();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, input, blood);
	}"""),
    ("""	public ItemStack assemble(Input inv) {
		return result.create();
	}""", """	public ItemStack assemble(Input inv, HolderLookup.Provider registries) {
		return result.copy();
	}"""),
    ("""	@Override
	public String group() {
		return "";
	}
""", ""),
    ("""
	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CAMPFIRE;
	}
""", ""),
])

patch("recipe/IafRecipeRegistry.java", [
    ("new RecipeSerializer<>(DragonForgeRecipe.CODEC, DragonForgeRecipe.STREAM_CODEC));", """new RecipeSerializer<>() {
			@Override
			public com.mojang.serialization.MapCodec<DragonForgeRecipe> codec() {
				return DragonForgeRecipe.CODEC;
			}

			@Override
			public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, DragonForgeRecipe> streamCodec() {
				return DragonForgeRecipe.STREAM_CODEC;
			}
		});"""),
])

patch("item/ItemTideTrident.java", [
    (".component(DataComponents.TOOL, TridentItem.createToolProperties()).enchantable(1).component(DataComponents.WEAPON, new Weapon(1)));",
     ".component(DataComponents.TOOL, TridentItem.createToolProperties()));"),
    ("import net.minecraft.world.item.component.Weapon;\n", ""),
    ("if (itemStack.nextDamageWillBreak()) {", "if (itemStack.getDamageValue() >= itemStack.getMaxDamage() - 1) {"),
    ("itemStack.hurtWithoutBreaking(1, player);", "itemStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));"),
    ("ItemStack thrown = itemStack.consumeAndReturn(1, player);", "ItemStack thrown = player.hasInfiniteMaterials() ? itemStack.copyWithCount(1) : itemStack.split(1);"),
])
print("done")
