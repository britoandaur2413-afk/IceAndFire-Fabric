package com.iafabric.test;

import com.iafabric.entity.EntityIceDragon;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The three-headed dragon's model seen in the game before it is a creature of its own (tests only): an ice dragon that
 * is drawn with it (TriDragonPreviewClient). Its model and texture come from tools/gen_tridragon.py.
 */
public final class TriDragonPreview implements ModInitializer {
	public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE,
		Identifier.fromNamespaceAndPath("iceandfire_fabric-test", "tri_dragon_preview"));
	public static EntityType<Dragon> TYPE;

	public static class Dragon extends EntityIceDragon {
		public Dragon(EntityType<? extends Dragon> type, Level level) {
			super(type, level);
		}
	}

	@Override
	public void onInitialize() {
		TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, KEY,
			EntityType.Builder.<Dragon>of(Dragon::new, MobCategory.CREATURE).sized(0.78F, 1.2F).clientTrackingRange(10).build(KEY));
		FabricDefaultAttributeRegistry.register(TYPE, EntityIceDragon.bakeAttributes().add(Attributes.TEMPT_RANGE, 10.0D));
	}
}
