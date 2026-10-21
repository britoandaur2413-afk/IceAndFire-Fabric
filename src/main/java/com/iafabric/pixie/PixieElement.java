package com.iafabric.pixie;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * What a pixie of each color gives its bonded player: its effect at levels 1 to 3 (I, I, II), and at levels 4 and 5
 * the same strength as stats of its own, without the effect.
 */
public enum PixieElement {
	STRENGTH(MobEffects.STRENGTH, new Bonus(Attributes.ATTACK_DAMAGE, 6, 9, AttributeModifier.Operation.ADD_VALUE)),
	JUMP(MobEffects.JUMP_BOOST, new Bonus(Attributes.JUMP_STRENGTH, 0.2, 0.3, AttributeModifier.Operation.ADD_VALUE),
		new Bonus(Attributes.SAFE_FALL_DISTANCE, 2, 4, AttributeModifier.Operation.ADD_VALUE)),
	SPEED(MobEffects.SPEED, new Bonus(Attributes.MOVEMENT_SPEED, 0.4, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)),
	LUCK(MobEffects.LUCK, new Bonus(Attributes.LUCK, 2, 4, AttributeModifier.Operation.ADD_VALUE)),
	HASTE(MobEffects.HASTE, new Bonus(Attributes.BLOCK_BREAK_SPEED, 0.4, 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
		new Bonus(Attributes.ATTACK_SPEED, 0.2, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

	public final Holder<MobEffect> effect;
	public final Bonus[] bonuses;

	PixieElement(Holder<MobEffect> effect, Bonus... bonuses) {
		this.effect = effect;
		this.bonuses = bonuses;
	}

	public static PixieElement of(int color) {
		return values()[Math.floorMod(color, values().length)];
	}

	/** The effect's amplifier at a level (levels 1 to 3), or -1 when the level gives stats instead. */
	public static int amplifier(int level) {
		return level <= 2 ? 0 : level == 3 ? 1 : -1;
	}

	/** A stat raised at levels 4 and 5. */
	public record Bonus(Holder<Attribute> attribute, double atFour, double atFive, AttributeModifier.Operation operation) {
		public double at(int level) {
			return level >= 5 ? atFive : level == 4 ? atFour : 0;
		}
	}
}
