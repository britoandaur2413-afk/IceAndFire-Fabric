package com.iafabric.item;

import com.iafabric.IafMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Armor stats of the original mod, turned into a vanilla {@link ArmorMaterial}. The defense array
 * keeps the old order (feet, legs, chest, head), and the look comes from
 * assets/iceandfire_fabric/equipment/&lt;name&gt;.json; repairs use the item tag iceandfire_fabric:repairs/&lt;name&gt;.
 */
public final class IafArmorMaterial {
	public final String name;
	public final ArmorMaterial material;

	public IafArmorMaterial(String name, int durability, int[] defense, int enchantability, Holder<SoundEvent> sound, float toughness) {
		this(name, durability, defense, enchantability, sound, toughness, 0.0F);
	}

	public IafArmorMaterial(String name, int durability, int[] defense, int enchantability, Holder<SoundEvent> sound, float toughness,
		float knockbackResistance) {
		this.name = name.toLowerCase(Locale.ROOT).replace("iceandfire_fabric:", "").replace(" ", "_");
		Map<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
		map.put(ArmorType.BOOTS, defense[0]);
		map.put(ArmorType.LEGGINGS, defense[1]);
		map.put(ArmorType.CHESTPLATE, defense[2]);
		map.put(ArmorType.HELMET, defense[3]);
		map.put(ArmorType.BODY, defense[2]);
		TagKey<Item> repair = TagKey.create(Registries.ITEM, IafMod.id("repairs/" + this.name));
		ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, IafMod.id(this.name));
		this.material = new ArmorMaterial(durability, map, Math.max(1, enchantability), sound, toughness, knockbackResistance, repair, asset);
	}

	public int getDefenseForSlot(EquipmentSlot slot) {
		return material.defense().getOrDefault(typeOf(slot), 0);
	}

	public float getToughness() {
		return material.toughness();
	}

	public static ArmorType typeOf(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> ArmorType.HELMET;
			case CHEST -> ArmorType.CHESTPLATE;
			case LEGS -> ArmorType.LEGGINGS;
			case FEET -> ArmorType.BOOTS;
			default -> ArmorType.BODY;
		};
	}
}
