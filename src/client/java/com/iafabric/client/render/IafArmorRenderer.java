package com.iafabric.client.render;

import com.iafabric.client.model.armor.IafArmorMeshes;
import com.iafabric.entity.DragonType;
import com.iafabric.item.ItemDeathwormArmor;
import com.iafabric.item.ItemDragonsteelArmor;
import com.iafabric.item.ItemScaleArmor;
import com.iafabric.item.ItemSeaSerpentArmor;
import com.iafabric.item.ItemSilverArmor;
import com.iafabric.item.ItemTrollArmor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * The armors the 1.18 mod drew with their own 3D models (dragon scales, dragonsteel, troll leather,
 * death worm chitin, tide guardian, silver and copper): the model of the armor's kind, its texture
 * (the "_legs" one for leggings), posed like the wearer.
 */
public final class IafArmorRenderer implements ArmorRenderer {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private static final Map<Function<CubeDeformation, MeshDefinition>, HumanoidModel<HumanoidRenderState>[]> MODELS = new HashMap<>();

	/** One model for each slot (head, chest, legs, feet): the game draws them later, so they cannot share one. */
	private final HumanoidModel<HumanoidRenderState>[] models;
	private final Identifier texture;
	private final Identifier legsTexture;

	private IafArmorRenderer(Function<CubeDeformation, MeshDefinition> mesh, String texture, String legsTexture) {
		this.models = MODELS.computeIfAbsent(mesh, IafArmorRenderer::bake);
		this.texture = Identifier.parse("iceandfire_fabric:textures/models/armor/" + texture + ".png");
		this.legsTexture = Identifier.parse("iceandfire_fabric:textures/models/armor/" + legsTexture + ".png");
	}

	@SuppressWarnings("unchecked")
	private static HumanoidModel<HumanoidRenderState>[] bake(Function<CubeDeformation, MeshDefinition> mesh) {
		HumanoidModel<HumanoidRenderState>[] models = new HumanoidModel[SLOTS.length];
		for (int i = 0; i < SLOTS.length; i++) {
			EquipmentSlot slot = SLOTS[i];
			// Like 1.18: the helmet and leggings use the inner model, the chestplate and boots the outer one.
			boolean inner = slot == EquipmentSlot.LEGS || slot == EquipmentSlot.HEAD;
			HumanoidModel<HumanoidRenderState> model = new HumanoidModel<>(mesh.apply(new CubeDeformation(inner ? 0.5F : 1.0F)).getRoot().bake(64, 64));
			model.head.visible = slot == EquipmentSlot.HEAD;
			model.hat.visible = slot == EquipmentSlot.HEAD;
			model.body.visible = slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS;
			model.rightArm.visible = slot == EquipmentSlot.CHEST;
			model.leftArm.visible = slot == EquipmentSlot.CHEST;
			model.rightLeg.visible = slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
			model.leftLeg.visible = slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
			models[i] = model;
		}
		return models;
	}

	public static void init() {
		for (Item item : BuiltInRegistries.ITEM) {
			IafArmorRenderer renderer = forItem(item);
			if (renderer != null) {
				ArmorRenderer.register(renderer, item);
			}
		}
	}

	private static IafArmorRenderer forItem(Item item) {
		String id = BuiltInRegistries.ITEM.getKey(item).getPath();
		if (item instanceof ItemScaleArmor scale) {
			DragonType type = scale.eggType.dragonType;
			Function<CubeDeformation, MeshDefinition> mesh = type == DragonType.ICE ? IafArmorMeshes::iceScale
				: type == DragonType.LIGHTNING ? IafArmorMeshes::lightningScale : IafArmorMeshes::fireScale;
			String name = scale.armor_type.name();
			return new IafArmorRenderer(mesh, name, name + "_legs");
		}
		if (item instanceof ItemDragonsteelArmor) {
			String kind = id.contains("ice") ? "ice" : id.contains("lightning") ? "lightning" : "fire";
			Function<CubeDeformation, MeshDefinition> mesh = kind.equals("ice") ? IafArmorMeshes::iceSteel
				: kind.equals("lightning") ? IafArmorMeshes::lightningSteel : IafArmorMeshes::fireSteel;
			return new IafArmorRenderer(mesh, "armor_dragonsteel_" + kind, "armor_dragonsteel_" + kind + "_legs");
		}
		if (item instanceof ItemTrollArmor troll) {
			String name = "armor_troll_" + troll.troll.name().toLowerCase(Locale.ROOT);
			return new IafArmorRenderer(IafArmorMeshes::troll, name, name + "_legs");
		}
		if (item instanceof ItemDeathwormArmor) {
			String name = "armor_deathworm_" + (id.contains("red") ? "red" : id.contains("white") ? "white" : "yellow");
			return new IafArmorRenderer(IafArmorMeshes::deathworm, name, name + "_legs");
		}
		if (item instanceof ItemSeaSerpentArmor serpent) {
			String name = "armor_tide_" + serpent.armor_type.resourceName;
			return new IafArmorRenderer(IafArmorMeshes::seaSerpent, name, name + "_legs");
		}
		if (item instanceof ItemSilverArmor) {
			return new IafArmorRenderer(IafArmorMeshes::silver, "armor_silver_metal_layer_1", "armor_silver_metal_layer_2");
		}
		return null;
	}

	@Override
	public void render(PoseStack poseStack, SubmitNodeCollector collector, ItemStack stack, HumanoidRenderState state, EquipmentSlot slot, int light,
		HumanoidModel<HumanoidRenderState> contextModel) {
		HumanoidModel<HumanoidRenderState> model = models[switch (slot) {
			case HEAD -> 0;
			case CHEST -> 1;
			case LEGS -> 2;
			default -> 3;
		}];
		Identifier tex = slot == EquipmentSlot.LEGS ? legsTexture : texture;
		ArmorRenderer.submitTransformCopyingModel(contextModel, state, model, state, false, collector, poseStack,
			stack.hasFoil() ? RenderTypes.armorCutoutNoCullGlint(tex) : RenderTypes.armorCutoutNoCull(tex), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
	}
}
