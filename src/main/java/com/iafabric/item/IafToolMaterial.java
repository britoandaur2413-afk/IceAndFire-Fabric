package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

/**
 * Tool stats of the original mod (harvest level, uses, speed, damage, enchantability) as a vanilla
 * {@link ToolMaterial}. Repairs use the item tag iceandfire_fabric:repairs/&lt;name&gt;.
 */
public final class IafToolMaterial {
	public static final TagKey<Block> INCORRECT_FOR_DRAGONSTEEL = TagKey.create(Registries.BLOCK, IafMod.id("incorrect_for_dragonsteel_tool"));

	public final String name;
	public final ToolMaterial material;

	public IafToolMaterial(String name, int harvestLevel, int uses, float damage, float speed, int enchantability) {
		this.name = name.toLowerCase(Locale.ROOT);
		TagKey<Block> incorrect = switch (harvestLevel) {
			case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
			case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
			case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
			case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
			case 4 -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
			default -> INCORRECT_FOR_DRAGONSTEEL;
		};
		TagKey<Item> repair = TagKey.create(Registries.ITEM, IafMod.id("repairs/" + this.name));
		this.material = new ToolMaterial(incorrect, uses, speed, damage, Math.max(1, enchantability), repair);
	}

	public int getUses() {
		return material.durability();
	}

	public float getAttackDamageBonus() {
		return material.attackDamageBonus();
	}

	public float getSpeed() {
		return material.speed();
	}
}
