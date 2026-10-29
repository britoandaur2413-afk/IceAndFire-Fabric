package com.iafabric.pixie;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * What a pixie of each color gives its bonded player: its effect at levels 1 to 3 (I, I, II), and at levels 4 and 5
 * the same strength as stats of its own, without the effect. 1.20.1 players have no jump, safe fall or mining speed
 * stats: the jump and haste pixies give their effect stronger there (II, III), the haste one with its attack speed.
 */
public enum PixieElement {
	STRENGTH(MobEffects.DAMAGE_BOOST, new Bonus(Attributes.ATTACK_DAMAGE, 6, 9, AttributeModifier.Operation.ADDITION)),
	JUMP(MobEffects.JUMP, true),
	SPEED(MobEffects.MOVEMENT_SPEED, new Bonus(Attributes.MOVEMENT_SPEED, 0.4, 0.5, AttributeModifier.Operation.MULTIPLY_BASE)),
	LUCK(MobEffects.LUCK, new Bonus(Attributes.LUCK, 2, 4, AttributeModifier.Operation.ADDITION)),
	HASTE(MobEffects.DIG_SPEED, true, new Bonus(Attributes.ATTACK_SPEED, 0.2, 0.3, AttributeModifier.Operation.MULTIPLY_BASE));

	public final MobEffect effect;
	public final Bonus[] bonuses;
	/** Its effect goes on at levels 4 and 5, stronger (its stats are not a 1.20.1 player's). */
	private final boolean effectAtTop;

	PixieElement(MobEffect effect, Bonus... bonuses) {
		this(effect, false, bonuses);
	}

	PixieElement(MobEffect effect, boolean effectAtTop, Bonus... bonuses) {
		this.effect = effect;
		this.effectAtTop = effectAtTop;
		this.bonuses = bonuses;
	}

	/** This pixie's effect amplifier at a level, or -1 when the level gives only stats. */
	public int effectAmplifier(int level) {
		if (effectAtTop && level >= 4) {
			return level >= 5 ? 2 : 1;
		}
		return amplifier(level);
	}

	public static PixieElement of(int color) {
		return values()[Math.floorMod(color, values().length)];
	}

	/** The effect's amplifier at a level (levels 1 to 3), or -1 when the level gives stats instead. */
	public static int amplifier(int level) {
		return level <= 2 ? 0 : level == 3 ? 1 : -1;
	}

	/** A stat raised at levels 4 and 5. */
	public record Bonus(Attribute attribute, double atFour, double atFive, AttributeModifier.Operation operation) {
		public double at(int level) {
			return level >= 5 ? atFive : level == 4 ? atFour : 0;
		}
	}
}
