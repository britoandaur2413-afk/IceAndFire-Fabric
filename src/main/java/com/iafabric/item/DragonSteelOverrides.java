package com.iafabric.item;

import net.minecraft.world.entity.EntityType;
import com.iafabric.IafConfig;
import com.iafabric.entity.EntityDeathWorm;
import com.iafabric.entity.props.FrozenProperties;
import com.iafabric.event.ServerEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

/** What the special tool materials do on a hit (silver, chitin, dragonsteel), and their tooltips. */
public final class DragonSteelOverrides {
	private DragonSteelOverrides() {
	}

	public static boolean isDragonsteel(IafToolMaterial tier) {
		return tier == IafItemRegistry.DRAGONSTEEL_TIER_FIRE || tier == IafItemRegistry.DRAGONSTEEL_TIER_ICE
			|| tier == IafItemRegistry.DRAGONSTEEL_TIER_LIGHTNING;
	}

	public static void hurtEnemy(IafToolMaterial tier, float attackDamage, LivingEntity target, LivingEntity attacker) {
		if (tier == IafItemRegistry.SILVER_TOOL_MATERIAL) {
			if (target.getType().is(EntityTypeTags.UNDEAD)) {
				target.hurt(target.damageSources().magic(), attackDamage + 3.0F);
			}
		}
		if (tier == IafItemRegistry.MYRMEX_CHITIN_TOOL_MATERIAL) {
			if (!target.getType().is(EntityTypeTags.ARTHROPOD)) {
				target.hurt(target.damageSources().generic(), attackDamage + 5.0F);
			}
			if (target instanceof EntityDeathWorm) {
				target.hurt(target.damageSources().generic(), attackDamage + 5.0F);
			}
		}
		if (tier == IafItemRegistry.DRAGONSTEEL_TIER_FIRE && IafConfig.dragonWeaponFireAbility) {
			target.igniteForSeconds(15);
			knockback(target, attacker);
		}
		if (tier == IafItemRegistry.DRAGONSTEEL_TIER_ICE && IafConfig.dragonWeaponIceAbility) {
			FrozenProperties.setFrozenFor(target, 300);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 2));
			knockback(target, attacker);
		}
		if (tier == IafItemRegistry.DRAGONSTEEL_TIER_LIGHTNING && IafConfig.dragonWeaponLightningAbility) {
			strikeLightning(target, attacker);
			knockback(target, attacker);
		}
	}

	static void knockback(LivingEntity target, LivingEntity attacker) {
		target.knockback(1F, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
	}

	static void strikeLightning(LivingEntity target, LivingEntity attacker) {
		boolean flag = !(attacker instanceof Player) || attacker.getAttackAnim(0.0F) <= 0.2;
		if (!attacker.level().isClientSide && flag) {
			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(target.level());
			if (bolt != null) {
				bolt.addTag(ServerEvents.BOLT_DONT_DESTROY_LOOT);
				bolt.addTag(attacker.getStringUUID());
				bolt.moveTo(target.position());
				target.level().addFreshEntity(bolt);
			}
		}
	}

	public static void appendHoverText(IafToolMaterial tier, Consumer<Component> tooltip) {
		if (tier == IafItemRegistry.SILVER_TOOL_MATERIAL) {
			tooltip.accept(Component.translatable("silvertools.hurt").withStyle(ChatFormatting.GREEN));
		}
		if (tier == IafItemRegistry.MYRMEX_CHITIN_TOOL_MATERIAL) {
			tooltip.accept(Component.translatable("myrmextools.hurt").withStyle(ChatFormatting.GREEN));
		}
		if (tier == IafItemRegistry.DRAGONSTEEL_TIER_FIRE && IafConfig.dragonWeaponFireAbility) {
			tooltip.accept(Component.translatable("dragon_sword_fire.hurt2").withStyle(ChatFormatting.DARK_RED));
		}
		if (tier == IafItemRegistry.DRAGONSTEEL_TIER_ICE && IafConfig.dragonWeaponIceAbility) {
			tooltip.accept(Component.translatable("dragon_sword_ice.hurt2").withStyle(ChatFormatting.AQUA));
		}
		if (tier == IafItemRegistry.DRAGONSTEEL_TIER_LIGHTNING && IafConfig.dragonWeaponLightningAbility) {
			tooltip.accept(Component.translatable("dragon_sword_lightning.hurt2").withStyle(ChatFormatting.DARK_PURPLE));
		}
		if (tier == IafItemRegistry.DRAGONSTEEL_TIER_DREAD_QUEEN) {
			tooltip.accept(Component.translatable("iceandfire_fabric.cryona_weapon.desc").withStyle(ChatFormatting.AQUA));
		}
	}
}
