package com.iafabric.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** An item and a dragon's blood cooked by the breath of that kind of dragon in a dragon forge. */
public class DragonForgeRecipe implements Recipe<DragonForgeRecipe.Input> {
	public static final MapCodec<DragonForgeRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Codec.STRING.fieldOf("dragon_type").forGetter(r -> r.dragonType),
		Codec.INT.optionalFieldOf("cook_time", 1000).forGetter(r -> r.cookTime),
		Ingredient.CODEC.fieldOf("input").forGetter(r -> r.input),
		Ingredient.CODEC.fieldOf("blood").forGetter(r -> r.blood),
		ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
	).apply(i, DragonForgeRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, DragonForgeRecipe> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8, r -> r.dragonType,
		ByteBufCodecs.VAR_INT, r -> r.cookTime,
		Ingredient.CONTENTS_STREAM_CODEC, r -> r.input,
		Ingredient.CONTENTS_STREAM_CODEC, r -> r.blood,
		ItemStackTemplate.STREAM_CODEC, r -> r.result,
		DragonForgeRecipe::new);

	private final String dragonType;
	private final int cookTime;
	private final Ingredient input;
	private final Ingredient blood;
	private final ItemStackTemplate result;

	public DragonForgeRecipe(String dragonType, int cookTime, Ingredient input, Ingredient blood, ItemStackTemplate result) {
		this.dragonType = dragonType;
		this.cookTime = cookTime;
		this.input = input;
		this.blood = blood;
		this.result = result;
	}

	/** What a forge offers: its input and blood slots and the kind of dragon it is built for. */
	public record Input(ItemStack input, ItemStack blood, String dragonType) implements RecipeInput {
		@Override
		public ItemStack getItem(int index) {
			return index == 0 ? input : blood;
		}

		@Override
		public int size() {
			return 2;
		}
	}

	public Ingredient getInput() {
		return input;
	}

	public Ingredient getBlood() {
		return blood;
	}

	public int getCookTime() {
		return cookTime;
	}

	public String getDragonType() {
		return dragonType;
	}

	public ItemStack getResultItem() {
		return result.create();
	}

	public boolean isValidInput(ItemStack stack) {
		return this.input.test(stack);
	}

	public boolean isValidBlood(ItemStack stack) {
		return this.blood.test(stack);
	}

	@Override
	public boolean matches(Input inv, Level level) {
		return this.input.test(inv.input()) && this.blood.test(inv.blood()) && this.dragonType.equals(inv.dragonType());
	}

	@Override
	public ItemStack assemble(Input inv) {
		return result.create();
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public String group() {
		return "";
	}

	@Override
	public RecipeSerializer<DragonForgeRecipe> getSerializer() {
		return IafRecipeRegistry.DRAGONFORGE_SERIALIZER;
	}

	@Override
	public RecipeType<DragonForgeRecipe> getType() {
		return IafRecipeRegistry.DRAGON_FORGE_TYPE;
	}

	@Override
	public PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return RecipeBookCategories.CAMPFIRE;
	}
}
