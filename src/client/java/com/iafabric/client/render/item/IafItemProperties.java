package com.iafabric.client.render.item;

import com.iafabric.IafMod;
import com.iafabric.item.ItemDragonHorn;
import com.iafabric.item.ItemSummoningCrystal;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** The item model properties the mod's item definitions use (assets/iceandfire_fabric/items). */
public final class IafItemProperties {
	private IafItemProperties() {
	}

	public static void init() {
		RangeSelectItemModelProperties.ID_MAPPER.put(IafMod.id("horn_dragon"), HornDragon.MAP_CODEC);
		ConditionalItemModelProperties.ID_MAPPER.put(IafMod.id("has_dragon"), HasDragon.MAP_CODEC);
	}

	/** The kind of dragon in a dragon horn: 0 none, 0.25 fire, 0.5 ice, 0.75 lightning. */
	public record HornDragon() implements RangeSelectItemModelProperty {
		public static final MapCodec<HornDragon> MAP_CODEC = MapCodec.unit(new HornDragon());

		@Override
		public float get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
			return ItemDragonHorn.getDragonType(itemStack) * 0.25F;
		}

		@Override
		public MapCodec<HornDragon> type() {
			return MAP_CODEC;
		}
	}

	/** Whether a summoning crystal holds a dragon. */
	public record HasDragon() implements ConditionalItemModelProperty {
		public static final MapCodec<HasDragon> MAP_CODEC = MapCodec.unit(new HasDragon());

		@Override
		public boolean get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
			return ItemSummoningCrystal.hasDragon(itemStack);
		}

		@Override
		public MapCodec<HasDragon> type() {
			return MAP_CODEC;
		}
	}
}
