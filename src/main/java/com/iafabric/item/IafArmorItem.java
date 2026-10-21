package com.iafabric.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** A piece of armor of the mod: a plain item with the humanoid armor components of its material. */
public class IafArmorItem extends Item {
	protected final IafArmorMaterial material;
	protected final EquipmentSlot slot;
	protected final ArmorType type;

	public IafArmorItem(IafArmorMaterial material, EquipmentSlot slot, Item.Properties properties) {
		super(properties.humanoidArmor(material.material, IafArmorMaterial.typeOf(slot)));
		this.material = material;
		this.slot = slot;
		this.type = IafArmorMaterial.typeOf(slot);
	}

	public IafArmorMaterial getMaterial() {
		return material;
	}

	public EquipmentSlot getEquipmentSlot() {
		return slot;
	}

	public ArmorType getType() {
		return type;
	}

	@Override
	public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot equippedSlot) {
		if (equippedSlot == this.slot && owner instanceof Player player) {
			onArmorTick(stack, level, player);
		}
	}

	/** Called every tick while a player wears this piece. */
	public void onArmorTick(ItemStack stack, Level world, Player player) {
	}
}
