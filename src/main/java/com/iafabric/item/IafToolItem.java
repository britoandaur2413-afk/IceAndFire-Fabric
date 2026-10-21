package com.iafabric.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A sword or digging tool of the mod. The kind (sword, axe...) only sets the vanilla tool
 * components; hits and tooltips follow the material.
 */
public class IafToolItem extends Item {
	public enum Kind {
		SWORD(3.0F, -2.4F), PICKAXE(1.0F, -2.8F), AXE(5.0F, -3.0F), SHOVEL(1.5F, -3.0F), HOE(0.0F, -3.0F);

		final float damage;
		final float speed;

		Kind(float damage, float speed) {
			this.damage = damage;
			this.speed = speed;
		}
	}

	protected final IafToolMaterial tier;
	protected final Kind kind;
	private float customDamage = Float.NaN;

	public IafToolItem(IafToolMaterial tier, Kind kind) {
		this(tier, kind, IafItemRegistry.props());
	}

	public IafToolItem(IafToolMaterial tier, Kind kind, Item.Properties properties) {
		super(apply(properties, tier, kind));
		this.tier = tier;
		this.kind = kind;
	}

	/** A sword with its own damage and speed (the old SwordItem(tier, damage, speed, props)). */
	public IafToolItem(IafToolMaterial tier, float attackDamage, float attackSpeed, Item.Properties properties) {
		super(properties.sword(tier.material, attackDamage, attackSpeed));
		this.tier = tier;
		this.kind = Kind.SWORD;
		this.customDamage = attackDamage;
	}

	private static Item.Properties apply(Item.Properties properties, IafToolMaterial tier, Kind kind) {
		return switch (kind) {
			case SWORD -> properties.sword(tier.material, kind.damage, kind.speed);
			case PICKAXE -> properties.pickaxe(tier.material, kind.damage, kind.speed);
			case AXE -> properties.axe(tier.material, kind.damage, kind.speed);
			case SHOVEL -> properties.shovel(tier.material, kind.damage, kind.speed);
			case HOE -> properties.hoe(tier.material, -tier.getAttackDamageBonus(), Math.min(0.0F, kind.speed + tier.getSpeed() / 3.0F));
		};
	}

	public IafToolMaterial getTier() {
		return tier;
	}

	public float getAttackDamage() {
		return (Float.isNaN(customDamage) ? kind.damage : customDamage) + tier.getAttackDamageBonus();
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		DragonSteelOverrides.hurtEnemy(tier, getAttackDamage(), target, attacker);
		super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		DragonSteelOverrides.appendHoverText(tier, tooltip);
	}

	public static boolean isTool(ItemStack stack) {
		return stack.has(DataComponents.TOOL);
	}
}
