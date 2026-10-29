package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Armor stats of the original mod as a 1.20.1 {@link ArmorMaterial}. The defense array keeps the old order (feet, legs,
 * chest, head); the look is textures/models/armor/&lt;name&gt;_layer_1.png and _layer_2.png, drawn by IafArmorRenderer;
 * repairs use the item tag iceandfire_fabric:repairs/&lt;name&gt;.
 */
public final class IafArmorMaterial implements ArmorMaterial {
	public final String name;
	/** The durability multiplier (an item's durability is its type's base times this). */
	public final int durability;
	private final Map<ArmorItem.Type, Integer> defense;
	private final int enchantability;
	private final SoundEvent sound;
	private final float toughness;
	private final float knockbackResistance;
	private final TagKey<Item> repair;

	public IafArmorMaterial(String name, int durability, int[] defense, int enchantability, SoundEvent sound, float toughness) {
		this(name, durability, defense, enchantability, sound, toughness, 0.0F);
	}

	public IafArmorMaterial(String name, int durability, int[] defense, int enchantability, SoundEvent sound, float toughness,
		float knockbackResistance) {
		this.name = name.toLowerCase(Locale.ROOT).replace("iceandfire_fabric:", "").replace(" ", "_");
		this.durability = durability;
		this.toughness = toughness;
		this.knockbackResistance = knockbackResistance;
		this.enchantability = Math.max(1, enchantability);
		this.sound = sound;
		Map<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
		map.put(ArmorItem.Type.BOOTS, defense[0]);
		map.put(ArmorItem.Type.LEGGINGS, defense[1]);
		map.put(ArmorItem.Type.CHESTPLATE, defense[2]);
		map.put(ArmorItem.Type.HELMET, defense[3]);
		this.defense = map;
		this.repair = TagKey.create(Registries.ITEM, IafMod.id("repairs/" + this.name));
	}

	public int getDefenseForSlot(EquipmentSlot slot) {
		return defense.getOrDefault(typeOf(slot), 0);
	}

	public static ArmorItem.Type typeOf(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> ArmorItem.Type.HELMET;
			case LEGS -> ArmorItem.Type.LEGGINGS;
			case FEET -> ArmorItem.Type.BOOTS;
			default -> ArmorItem.Type.CHESTPLATE;
		};
	}

	@Override
	public int getDurabilityForType(ArmorItem.Type type) {
		// (the game's base durability per piece, as its own materials use)
		int base = switch (type) {
			case HELMET -> 11;
			case CHESTPLATE -> 16;
			case LEGGINGS -> 15;
			case BOOTS -> 13;
		};
		return base * durability;
	}

	@Override
	public int getDefenseForType(ArmorItem.Type type) {
		return defense.getOrDefault(type, 0);
	}

	@Override
	public int getEnchantmentValue() {
		return enchantability;
	}

	@Override
	public SoundEvent getEquipSound() {
		return sound;
	}

	@Override
	public Ingredient getRepairIngredient() {
		return Ingredient.of(repair);
	}

	/** The material's name (its pieces are drawn by IafArmorRenderer, which picks the textures). */
	@Override
	public String getName() {
		return IafMod.MOD_ID + "_" + name;
	}

	@Override
	public float getToughness() {
		return toughness;
	}

	@Override
	public float getKnockbackResistance() {
		return knockbackResistance;
	}
}
