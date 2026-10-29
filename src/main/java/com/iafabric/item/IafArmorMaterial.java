package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Armor stats of the original mod, turned into a registered vanilla {@link ArmorMaterial}. The defense array
 * keeps the old order (feet, legs, chest, head); the look is textures/models/armor/&lt;name&gt;_layer_1.png and
 * _layer_2.png; repairs use the item tag iceandfire_fabric:repairs/&lt;name&gt;.
 */
public final class IafArmorMaterial {
	public final String name;
	public final Holder<ArmorMaterial> material;
	/** The durability multiplier (an item's durability is its type's base times this). */
	public final int durability;
	private final Map<ArmorItem.Type, Integer> defense;
	private final float toughness;

	public IafArmorMaterial(String name, int durability, int[] defense, int enchantability, Holder<SoundEvent> sound, float toughness) {
		this(name, durability, defense, enchantability, sound, toughness, 0.0F);
	}

	public IafArmorMaterial(String name, int durability, int[] defense, int enchantability, Holder<SoundEvent> sound, float toughness,
		float knockbackResistance) {
		this.name = name.toLowerCase(Locale.ROOT).replace("iceandfire_fabric:", "").replace(" ", "_");
		this.durability = durability;
		this.toughness = toughness;
		Map<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
		map.put(ArmorItem.Type.BOOTS, defense[0]);
		map.put(ArmorItem.Type.LEGGINGS, defense[1]);
		map.put(ArmorItem.Type.CHESTPLATE, defense[2]);
		map.put(ArmorItem.Type.HELMET, defense[3]);
		map.put(ArmorItem.Type.BODY, defense[2]);
		this.defense = map;
		TagKey<Item> repair = TagKey.create(Registries.ITEM, IafMod.id("repairs/" + this.name));
		List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(IafMod.id(this.name)));
		this.material = Registry.registerForHolder(BuiltInRegistries.ARMOR_MATERIAL, IafMod.id(this.name),
			new ArmorMaterial(map, Math.max(1, enchantability), sound, () -> Ingredient.of(repair), layers, toughness, knockbackResistance));
	}

	public int getDefenseForSlot(EquipmentSlot slot) {
		return defense.getOrDefault(typeOf(slot), 0);
	}

	public float getToughness() {
		return toughness;
	}

	public static ArmorItem.Type typeOf(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> ArmorItem.Type.HELMET;
			case CHEST -> ArmorItem.Type.CHESTPLATE;
			case LEGS -> ArmorItem.Type.LEGGINGS;
			case FEET -> ArmorItem.Type.BOOTS;
			default -> ArmorItem.Type.BODY;
		};
	}
}
