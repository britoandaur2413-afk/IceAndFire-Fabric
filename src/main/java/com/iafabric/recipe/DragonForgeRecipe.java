package com.iafabric.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** An item and a dragon's blood cooked by the breath of that kind of dragon in a dragon forge. */
public class DragonForgeRecipe implements Recipe<Container> {
	private final ResourceLocation id;
	private final String dragonType;
	private final int cookTime;
	private final Ingredient input;
	private final Ingredient blood;
	private final ItemStack result;

	public DragonForgeRecipe(ResourceLocation id, String dragonType, int cookTime, Ingredient input, Ingredient blood, ItemStack result) {
		this.id = id;
		this.dragonType = dragonType;
		this.cookTime = cookTime;
		this.input = input;
		this.blood = blood;
		this.result = result;
	}

	/** What a forge offers: its input and blood slots and the kind of dragon it is built for. */
	public static final class Input extends SimpleContainer {
		private final String dragonType;

		public Input(ItemStack input, ItemStack blood, String dragonType) {
			super(input, blood);
			this.dragonType = dragonType;
		}

		public ItemStack input() {
			return getItem(0);
		}

		public ItemStack blood() {
			return getItem(1);
		}

		public String dragonType() {
			return dragonType;
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
		return result.copy();
	}

	@Override
	public ItemStack getResultItem(RegistryAccess registries) {
		return result.copy();
	}

	@Override
	public ResourceLocation getId() {
		return id;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, input, blood);
	}

	public boolean isValidInput(ItemStack stack) {
		return this.input.test(stack);
	}

	public boolean isValidBlood(ItemStack stack) {
		return this.blood.test(stack);
	}

	@Override
	public boolean matches(Container inv, Level level) {
		return inv instanceof Input forge && this.input.test(forge.input()) && this.blood.test(forge.blood()) && this.dragonType.equals(forge.dragonType());
	}

	@Override
	public ItemStack assemble(Container inv, RegistryAccess registries) {
		return result.copy();
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
	public RecipeSerializer<DragonForgeRecipe> getSerializer() {
		return IafRecipeRegistry.DRAGONFORGE_SERIALIZER;
	}

	@Override
	public RecipeType<DragonForgeRecipe> getType() {
		return IafRecipeRegistry.DRAGON_FORGE_TYPE;
	}

	/** Its json (dragon_type, cook_time, input, blood, result) and its packet. */
	public static final class Serializer implements RecipeSerializer<DragonForgeRecipe> {
		@Override
		public DragonForgeRecipe fromJson(ResourceLocation id, JsonObject json) {
			JsonObject out = GsonHelper.getAsJsonObject(json, "result");
			ResourceLocation item = new ResourceLocation(GsonHelper.getAsString(out, out.has("id") ? "id" : "item"));
			ItemStack result = new ItemStack(BuiltInRegistries.ITEM.get(item), GsonHelper.getAsInt(out, "count", 1));
			return new DragonForgeRecipe(id, GsonHelper.getAsString(json, "dragon_type"), GsonHelper.getAsInt(json, "cook_time", 1000),
				Ingredient.fromJson(json.get("input")), Ingredient.fromJson(json.get("blood")), result);
		}

		@Override
		public DragonForgeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
			return new DragonForgeRecipe(id, buf.readUtf(), buf.readVarInt(), Ingredient.fromNetwork(buf), Ingredient.fromNetwork(buf), buf.readItem());
		}

		@Override
		public void toNetwork(FriendlyByteBuf buf, DragonForgeRecipe recipe) {
			buf.writeUtf(recipe.dragonType);
			buf.writeVarInt(recipe.cookTime);
			recipe.input.toNetwork(buf);
			recipe.blood.toNetwork(buf);
			buf.writeItem(recipe.result);
		}
	}
}
