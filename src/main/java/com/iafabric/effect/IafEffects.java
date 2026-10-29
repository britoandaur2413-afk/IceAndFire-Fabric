package com.iafabric.effect;

import com.iafabric.IafMod;
import com.iafabric.item.IafItemRegistry;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

/**
 * The Frost Ward: the ice dragons' breath and the frost of Cryona and Black Frost do not freeze who has it, and their
 * breath hurts less. The scribes sell a short potion of it (twenty seconds: a fight needs several) and, for ice dragon
 * blood, a long one (a minute), which is also brewed from an awkward potion and ice dragon blood. It does
 * not hold against Black Frost's Glacial Prison.
 */
public final class IafEffects {
	public static final Holder<MobEffect> FROST_WARD = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, IafMod.id("frost_ward"), new FrostWard());
	/**
	 * Black Frost's ice cauterized them: no life comes back while it lasts (shown as an effect; what holds it is
	 * BossCold's, which milk does not take away).
	 */
	public static final Holder<MobEffect> FROST_WOUND = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, IafMod.id("frost_wound"), new FrostWound());
	public static final Holder<Potion> FROST_WARD_POTION = Registry.registerForHolder(BuiltInRegistries.POTION, IafMod.id("frost_ward"),
		new Potion("frost_ward", new MobEffectInstance(FROST_WARD, 20 * 20)));
	public static final Holder<Potion> LONG_FROST_WARD_POTION = Registry.registerForHolder(BuiltInRegistries.POTION, IafMod.id("long_frost_ward"),
		new Potion("frost_ward", new MobEffectInstance(FROST_WARD, 60 * 20)));
	/** What is left of an ice breath's blow on who has the ward. */
	public static final float WARDED_BREATH = 0.6F;

	private IafEffects() {
	}

	private static final class FrostWard extends MobEffect {
		FrostWard() {
			super(MobEffectCategory.BENEFICIAL, 0x8FD8FF);
		}
	}

	private static final class FrostWound extends MobEffect {
		FrostWound() {
			super(MobEffectCategory.HARMFUL, 0x3E7FB8);
		}
	}

	/** Whether the cold may not freeze it now. */
	public static boolean warded(LivingEntity entity) {
		return entity.hasEffect(FROST_WARD);
	}

	/** Loads the class (its fields register the effect and the potions) and adds the long potion's brewing. */
	public static void init() {
		FabricBrewingRecipeRegistryBuilder.BUILD.register(builder ->
			builder.addMix(Potions.AWKWARD, IafItemRegistry.ICE_DRAGON_BLOOD, LONG_FROST_WARD_POTION));
	}
}
