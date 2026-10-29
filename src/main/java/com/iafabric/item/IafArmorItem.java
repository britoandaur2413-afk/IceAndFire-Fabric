package com.iafabric.item;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A piece of armor of the mod: a vanilla armor item of its material. */
public class IafArmorItem extends ArmorItem {
	protected final IafArmorMaterial iafMaterial;
	protected final EquipmentSlot slot;

	public IafArmorItem(IafArmorMaterial material, EquipmentSlot slot, Item.Properties properties) {
		super(material, IafArmorMaterial.typeOf(slot), properties);
		this.iafMaterial = material;
		this.slot = slot;
	}

	public IafArmorMaterial getIafMaterial() {
		return iafMaterial;
	}

	@Override
	public EquipmentSlot getEquipmentSlot() {
		return slot;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity owner, int slotId, boolean selected) {
		if (!level.isClientSide && owner instanceof Player player && player.getItemBySlot(this.slot) == stack) {
			onArmorTick(stack, level, player);
		}
	}

	/** Called every tick (on the server) while a player wears this piece. */
	public void onArmorTick(ItemStack stack, Level world, Player player) {
	}
}
