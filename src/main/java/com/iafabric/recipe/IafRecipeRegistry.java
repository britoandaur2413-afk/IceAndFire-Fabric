package com.iafabric.recipe;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityAmphithereArrow;
import com.iafabric.entity.EntityCockatriceEgg;
import com.iafabric.entity.EntityDeathWormEgg;
import com.iafabric.entity.EntityDragonArrow;
import com.iafabric.entity.EntityHippogryphEgg;
import com.iafabric.entity.EntityHydraArrow;
import com.iafabric.entity.EntitySeaSerpentArrow;
import com.iafabric.entity.EntityStymphalianArrow;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.ItemLike;

/** The dragon forge recipe type, banner pattern tags and dispenser behaviors of the mod. */
public final class IafRecipeRegistry {
	public static final RecipeType<DragonForgeRecipe> DRAGON_FORGE_TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE, IafMod.id("dragonforge"),
		new RecipeType<DragonForgeRecipe>() {
			@Override
			public String toString() {
				return "iceandfire_fabric:dragonforge";
			}
		});
	public static final RecipeSerializer<DragonForgeRecipe> DRAGONFORGE_SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
		IafMod.id("dragonforge"), new DragonForgeRecipe.Serializer());

	private IafRecipeRegistry() {
	}

	/** The banner pattern a pattern item gives (data/iceandfire_fabric/tags/banner_pattern/pattern_item/&lt;name&gt;.json). */
	public static TagKey<BannerPattern> patternTag(String name) {
		return TagKey.create(Registries.BANNER_PATTERN, IafMod.id("pattern_item/" + name));
	}

	/** The mod's banner patterns (1.20.1 has them in code, not in data): textures/entity/banner|shield/&lt;name&gt;.png. */
	private static final String[] PATTERNS = {"amphithere", "bird", "dread", "eye", "fae", "feather", "fire", "fire_head", "gorgon",
		"hippocampus", "hippogryph_head", "ice", "ice_head", "lightning", "lightning_head", "mermaid", "sea_serpent", "troll", "weezer"};

	/** The name a pattern is saved under in a banner (its "hash name"). */
	public static String patternHash(String name) {
		return IafMod.MOD_ID + "_" + name;
	}

	public static void init() {
		for (String name : PATTERNS) {
			Registry.register(BuiltInRegistries.BANNER_PATTERN, IafMod.id(name), new BannerPattern(patternHash(name)));
		}
		dispense(IafItemRegistry.STYMPHALIAN_ARROW, (level, pos, stack) -> arrow(new EntityStymphalianArrow(IafEntityRegistry.STYMPHALIAN_ARROW, level, pos.x(), pos.y(), pos.z())));
		dispense(IafItemRegistry.AMPHITHERE_ARROW, (level, pos, stack) -> arrow(new EntityAmphithereArrow(IafEntityRegistry.AMPHITHERE_ARROW, level, pos.x(), pos.y(), pos.z())));
		dispense(IafItemRegistry.SEA_SERPENT_ARROW, (level, pos, stack) -> arrow(new EntitySeaSerpentArrow(IafEntityRegistry.SEA_SERPENT_ARROW, level, pos.x(), pos.y(), pos.z())));
		dispense(IafItemRegistry.DRAGONBONE_ARROW, (level, pos, stack) -> arrow(new EntityDragonArrow(IafEntityRegistry.DRAGON_ARROW, pos.x(), pos.y(), pos.z(), level)));
		dispense(IafItemRegistry.HYDRA_ARROW, (level, pos, stack) -> arrow(new EntityHydraArrow(IafEntityRegistry.HYDRA_ARROW, level, pos.x(), pos.y(), pos.z())));
		dispense(IafItemRegistry.HIPPOGRYPH_EGG, (level, pos, stack) -> new EntityHippogryphEgg(IafEntityRegistry.HIPPOGRYPH_EGG, level, pos.x(), pos.y(), pos.z(), stack));
		dispense(IafItemRegistry.ROTTEN_EGG, (level, pos, stack) -> new EntityCockatriceEgg(IafEntityRegistry.COCKATRICE_EGG, pos.x(), pos.y(), pos.z(), level));
		dispense(IafItemRegistry.DEATHWORM_EGG, (level, pos, stack) -> new EntityDeathWormEgg(IafEntityRegistry.DEATH_WORM_EGG, pos.x(), pos.y(), pos.z(), level, false));
		dispense(IafItemRegistry.DEATHWORM_EGG_GIGANTIC, (level, pos, stack) -> new EntityDeathWormEgg(IafEntityRegistry.DEATH_WORM_EGG, pos.x(), pos.y(), pos.z(), level, true));
	}

	private static Projectile arrow(AbstractArrow arrow) {
		arrow.pickup = AbstractArrow.Pickup.ALLOWED;
		return arrow;
	}

	@FunctionalInterface
	private interface ProjectileFactory {
		Projectile create(Level level, Position pos, ItemStack stack);
	}

	/** A dispenser shoots the projectile made from the item, like vanilla arrows. */
	private static void dispense(ItemLike item, ProjectileFactory factory) {
		DispenserBlock.registerBehavior(item, new DefaultDispenseItemBehavior() {
			@Override
			protected ItemStack execute(BlockSource source, ItemStack stack) {
				Level level = source.getLevel();
				Position pos = DispenserBlock.getDispensePosition(source);
				Direction direction = source.getBlockState().getValue(DispenserBlock.FACING);
				Projectile projectile = factory.create(level, pos, stack.copyWithCount(1));
				projectile.shoot(direction.getStepX(), direction.getStepY() + 0.1F, direction.getStepZ(), 1.1F, 6.0F);
				level.addFreshEntity(projectile);
				stack.shrink(1);
				return stack;
			}

			@Override
			protected void playSound(BlockSource source) {
				source.getLevel().levelEvent(1002, source.getPos(), 0);
			}
		});
	}
}
