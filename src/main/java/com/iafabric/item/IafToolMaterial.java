package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

/**
 * Tool stats of the original mod (harvest level, uses, speed, damage, enchantability) as a vanilla
 * {@link Tier}. Repairs use the item tag iceandfire_fabric:repairs/&lt;name&gt;.
 */
public final class IafToolMaterial implements Tier {
	public static final TagKey<Block> INCORRECT_FOR_DRAGONSTEEL = TagKey.create(Registries.BLOCK, IafMod.id("incorrect_for_dragonsteel_tool"));

	public final String name;
	/** The vanilla tier (this material itself; the 26.x port kept a separate ToolMaterial here). */
	public final Tier material = this;
	private final TagKey<Block> incorrect;
	private final TagKey<Item> repair;
	private final int uses;
	private final float damage;
	private final float speed;
	private final int enchantability;

	public IafToolMaterial(String name, int harvestLevel, int uses, float damage, float speed, int enchantability) {
		this.name = name.toLowerCase(Locale.ROOT);
		this.incorrect = switch (harvestLevel) {
			case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
			case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
			case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
			case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
			case 4 -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
			default -> INCORRECT_FOR_DRAGONSTEEL;
		};
		this.repair = TagKey.create(Registries.ITEM, IafMod.id("repairs/" + this.name));
		this.uses = uses;
		this.damage = damage;
		this.speed = speed;
		this.enchantability = Math.max(1, enchantability);
	}

	@Override
	public int getUses() {
		return uses;
	}

	@Override
	public float getAttackDamageBonus() {
		return damage;
	}

	@Override
	public float getSpeed() {
		return speed;
	}

	@Override
	public TagKey<Block> getIncorrectBlocksForDrops() {
		return incorrect;
	}

	@Override
	public int getEnchantmentValue() {
		return enchantability;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.of(repair);
	}
}
