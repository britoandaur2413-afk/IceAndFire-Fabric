package com.iafabric.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * A sword or digging tool of the mod. The kind (sword, axe...) only sets the vanilla tool
 * component and attack attributes; hits and tooltips follow the material.
 */
public class IafToolItem extends TieredItem {
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
		super(tier, apply(properties, tier, kind));
		this.tier = tier;
		this.kind = kind;
	}

	/** A sword with its own damage and speed (the old SwordItem(tier, damage, speed, props)). */
	public IafToolItem(IafToolMaterial tier, float attackDamage, float attackSpeed, Item.Properties properties) {
		super(tier, properties.attributes(attributes(tier, attackDamage, attackSpeed)).component(DataComponents.TOOL, swordTool()));
		this.tier = tier;
		this.kind = Kind.SWORD;
		this.customDamage = attackDamage;
	}

	private static Item.Properties apply(Item.Properties properties, IafToolMaterial tier, Kind kind) {
		return switch (kind) {
			case SWORD -> properties.attributes(attributes(tier, kind.damage, kind.speed)).component(DataComponents.TOOL, swordTool());
			case PICKAXE -> properties.attributes(attributes(tier, kind.damage, kind.speed)).component(DataComponents.TOOL, tier.createToolProperties(BlockTags.MINEABLE_WITH_PICKAXE));
			case AXE -> properties.attributes(attributes(tier, kind.damage, kind.speed)).component(DataComponents.TOOL, tier.createToolProperties(BlockTags.MINEABLE_WITH_AXE));
			case SHOVEL -> properties.attributes(attributes(tier, kind.damage, kind.speed)).component(DataComponents.TOOL, tier.createToolProperties(BlockTags.MINEABLE_WITH_SHOVEL));
			case HOE -> properties.attributes(attributes(tier, -tier.getAttackDamageBonus(), Math.min(0.0F, kind.speed + tier.getSpeed() / 3.0F)))
				.component(DataComponents.TOOL, tier.createToolProperties(BlockTags.MINEABLE_WITH_HOE));
		};
	}

	/** Main-hand attack damage (the kind's own plus the material's bonus) and attack speed. */
	public static ItemAttributeModifiers attributes(Tier tier, float damage, float speed) {
		return ItemAttributeModifiers.builder()
			.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage + tier.getAttackDamageBonus(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
			.build();
	}

	/** A sword's tool rules (cobwebs fast, sword-efficient blocks a bit faster), as vanilla swords have. */
	public static Tool swordTool() {
		return new Tool(List.of(Tool.Rule.minesAndDrops(List.of(Blocks.COBWEB), 15.0F), Tool.Rule.overrideSpeed(BlockTags.SWORD_EFFICIENT, 1.5F)), 1.0F, 2);
	}

	@Override
	public IafToolMaterial getTier() {
		return tier;
	}

	public float getAttackDamage() {
		return (Float.isNaN(customDamage) ? kind.damage : customDamage) + tier.getAttackDamageBonus();
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		DragonSteelOverrides.hurtEnemy(tier, getAttackDamage(), target, attacker);
		return true;
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.hurtAndBreak(kind == Kind.SWORD ? 1 : 2, attacker, EquipmentSlot.MAINHAND);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		DragonSteelOverrides.appendHoverText(tier, tooltip::add);
	}

	public static boolean isTool(ItemStack stack) {
		return stack.has(DataComponents.TOOL);
	}
}
