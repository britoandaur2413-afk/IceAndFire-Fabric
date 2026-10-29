package com.iafabric.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A sword of the mod; {@link #create} makes its digging tools too. As in the original mod they are the game's own
 * swords, pickaxes, axes, shovels and hoes (enchantable as such, the axe strips logs...), with the material's exact
 * attack damage (1.20.1's swords, pickaxes and hoes take whole numbers); hits and tooltips follow the material.
 */
public class IafToolItem extends SwordItem {
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
	private final float attackDamage;
	private final Multimap<Attribute, AttributeModifier> modifiers;

	public IafToolItem(IafToolMaterial tier, Kind kind) {
		this(tier, kind.damage, kind.speed, IafItemRegistry.props());
	}

	/** A sword with its own damage and speed (the old SwordItem(tier, damage, speed, props)). */
	public IafToolItem(IafToolMaterial tier, float attackDamage, float attackSpeed, Item.Properties properties) {
		super(tier, (int) attackDamage, attackSpeed, properties);
		this.tier = tier;
		this.attackDamage = attackDamage + tier.getAttackDamageBonus();
		this.modifiers = modifiers(this.attackDamage, attackSpeed);
	}

	/** A tool of this kind and material: a sword, or the game's pickaxe, axe, shovel or hoe made of it. */
	public static Item create(IafToolMaterial tier, Kind kind) {
		Item.Properties properties = IafItemRegistry.props();
		return switch (kind) {
			case SWORD -> new IafToolItem(tier, kind);
			case PICKAXE -> new Pickaxe(tier, kind.damage, kind.speed, properties);
			case AXE -> new Axe(tier, kind.damage, kind.speed, properties);
			case SHOVEL -> new Shovel(tier, kind.damage, kind.speed, properties);
			case HOE -> new Hoe(tier, -tier.getAttackDamageBonus(), Math.min(0.0F, kind.speed + tier.getSpeed() / 3.0F), properties);
		};
	}

	/** Main-hand attack damage and speed. */
	static Multimap<Attribute, AttributeModifier> modifiers(float damage, float speed) {
		return ImmutableMultimap.<Attribute, AttributeModifier>builder()
			.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(Tools.BASE_DAMAGE, "Weapon modifier", damage, AttributeModifier.Operation.ADDITION))
			.put(Attributes.ATTACK_SPEED, new AttributeModifier(Tools.BASE_SPEED, "Weapon modifier", speed, AttributeModifier.Operation.ADDITION))
			.build();
	}

	@Override
	public IafToolMaterial getTier() {
		return tier;
	}

	public float getAttackDamage() {
		return attackDamage;
	}

	@Override
	public float getDamage() {
		return attackDamage;
	}

	@Override
	public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
		return slot == EquipmentSlot.MAINHAND ? modifiers : super.getDefaultAttributeModifiers(slot);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		DragonSteelOverrides.hurtEnemy(tier, getAttackDamage(), target, attacker);
		return super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		DragonSteelOverrides.appendHoverText(tier, tooltip::add);
	}

	public static boolean isTool(ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof SwordItem || item instanceof DiggerItem;
	}

	/** The game's own attack modifier ids (Item's are protected). */
	private static final class Tools extends Item {
		static final UUID BASE_DAMAGE = BASE_ATTACK_DAMAGE_UUID;
		static final UUID BASE_SPEED = BASE_ATTACK_SPEED_UUID;

		private Tools() {
			super(new Item.Properties());
		}
	}

	private static final class Pickaxe extends PickaxeItem {
		private final Multimap<Attribute, AttributeModifier> modifiers;

		Pickaxe(IafToolMaterial tier, float damage, float speed, Item.Properties properties) {
			super(tier, (int) damage, speed, properties);
			this.modifiers = IafToolItem.modifiers(damage + tier.getAttackDamageBonus(), speed);
		}

		@Override
		public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
			return slot == EquipmentSlot.MAINHAND ? modifiers : super.getDefaultAttributeModifiers(slot);
		}

		@Override
		public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			DragonSteelOverrides.hurtEnemy((IafToolMaterial) getTier(), getAttackDamage(), target, attacker);
			return super.hurtEnemy(stack, target, attacker);
		}

		@Override
		public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
			DragonSteelOverrides.appendHoverText((IafToolMaterial) getTier(), tooltip::add);
		}
	}

	private static final class Axe extends AxeItem {
		Axe(IafToolMaterial tier, float damage, float speed, Item.Properties properties) {
			super(tier, damage, speed, properties);
		}

		@Override
		public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			DragonSteelOverrides.hurtEnemy((IafToolMaterial) getTier(), getAttackDamage(), target, attacker);
			return super.hurtEnemy(stack, target, attacker);
		}

		@Override
		public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
			DragonSteelOverrides.appendHoverText((IafToolMaterial) getTier(), tooltip::add);
		}
	}

	private static final class Shovel extends ShovelItem {
		Shovel(IafToolMaterial tier, float damage, float speed, Item.Properties properties) {
			super(tier, damage, speed, properties);
		}

		@Override
		public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			DragonSteelOverrides.hurtEnemy((IafToolMaterial) getTier(), getAttackDamage(), target, attacker);
			return super.hurtEnemy(stack, target, attacker);
		}

		@Override
		public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
			DragonSteelOverrides.appendHoverText((IafToolMaterial) getTier(), tooltip::add);
		}
	}

	private static final class Hoe extends HoeItem {
		private final Multimap<Attribute, AttributeModifier> modifiers;

		Hoe(IafToolMaterial tier, float damage, float speed, Item.Properties properties) {
			super(tier, (int) damage, speed, properties);
			this.modifiers = IafToolItem.modifiers(damage + tier.getAttackDamageBonus(), speed);
		}

		@Override
		public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
			return slot == EquipmentSlot.MAINHAND ? modifiers : super.getDefaultAttributeModifiers(slot);
		}

		@Override
		public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			DragonSteelOverrides.hurtEnemy((IafToolMaterial) getTier(), getAttackDamage(), target, attacker);
			return super.hurtEnemy(stack, target, attacker);
		}

		@Override
		public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
			DragonSteelOverrides.appendHoverText((IafToolMaterial) getTier(), tooltip::add);
		}
	}
}
