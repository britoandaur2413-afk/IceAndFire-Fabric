package com.iafabric.pixie;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * The kinds of damage a bonded pixie's orb blocks: physical (blows, a mace, explosions, falls), projectiles (arrows,
 * a thrown trident or spear), magic, fire and water (drowning, freezing).
 */
public enum OrbKind {
	PHYSICAL, PROJECTILE, MAGIC, FIRE, WATER;

	public static OrbKind of(DamageSource source) {
		if (source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA) || source.getMsgId().contains("fire") || source.getMsgId().contains("lightning")) {
			return FIRE;
		}
		if (source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FREEZING) || source.getMsgId().contains("ice")) {
			return WATER;
		}
		if (source.is(DamageTypeTags.IS_PROJECTILE) || source.getDirectEntity() instanceof Projectile) {
			return PROJECTILE;
		}
		if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.WITHER) || source.is(DamageTypes.DRAGON_BREATH)
			|| source.is(DamageTypes.SONIC_BOOM) || source.is(DamageTypes.THORNS) || source.getMsgId().contains("gorgon")) {
			return MAGIC;
		}
		return PHYSICAL;
	}
}
