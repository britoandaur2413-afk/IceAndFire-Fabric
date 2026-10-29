package com.iafabric.client.render;

import com.iafabric.IafMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The mod's armors that look like the game's own (blindfold, earplugs, sheep disguise, myrmex chitin): the game's
 * armor model with the mod's textures, textures/models/armor/&lt;material&gt;_layer_1.png (_layer_2 for leggings).
 * 1.20.1 would look for them in the game's own folder, so they are drawn here.
 */
final class PlainArmorRenderer implements ArmorRenderer {
	private final ResourceLocation texture;
	private final ResourceLocation legsTexture;
	/** One model for each slot (head, chest, legs, feet), made the first time one is drawn (the models load with the resources). */
	private HumanoidModel<LivingEntity>[] models;

	PlainArmorRenderer(String material) {
		this.texture = IafMod.id("textures/models/armor/" + material + "_layer_1.png");
		this.legsTexture = IafMod.id("textures/models/armor/" + material + "_layer_2.png");
	}

	@SuppressWarnings("unchecked")
	private HumanoidModel<LivingEntity>[] models() {
		if (models == null) {
			var set = Minecraft.getInstance().getEntityModels();
			models = new HumanoidModel[4];
			for (int i = 0; i < 4; i++) {
				// the leggings use the inner model, the rest the outer one, as the game does
				HumanoidModel<LivingEntity> model = new HumanoidModel<>(set.bakeLayer(i == 2 ? ModelLayers.PLAYER_INNER_ARMOR : ModelLayers.PLAYER_OUTER_ARMOR));
				model.setAllVisible(false);
				switch (i) {
					case 0 -> {
						model.head.visible = true;
						model.hat.visible = true;
					}
					case 1 -> {
						model.body.visible = true;
						model.rightArm.visible = true;
						model.leftArm.visible = true;
					}
					case 2 -> {
						model.body.visible = true;
						model.rightLeg.visible = true;
						model.leftLeg.visible = true;
					}
					default -> {
						model.rightLeg.visible = true;
						model.leftLeg.visible = true;
					}
				}
				models[i] = model;
			}
		}
		return models;
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource buffers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light,
		HumanoidModel<LivingEntity> contextModel) {
		HumanoidModel<LivingEntity> model = models()[switch (slot) {
			case HEAD -> 0;
			case CHEST -> 1;
			case LEGS -> 2;
			default -> 3;
		}];
		contextModel.copyPropertiesTo(model);
		ArmorRenderer.renderPart(poseStack, buffers, light, stack, model, slot == EquipmentSlot.LEGS ? legsTexture : texture);
	}
}
