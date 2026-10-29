package com.iafabric.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * One of the mod's armor pieces as its wearer is drawn this frame: posed like the wearer's model (as Fabric's
 * TransformCopyingModel does it) and, on a player, hidden where the player is hidden. A first-person animation (Better
 * Combat's swings, drawn by its player animation library) hides all of the player but the arms; the game's own armor
 * follows it, and so does this: without it the helmet and the chest piece were drawn round the camera during a swing.
 * On any other wearer all of it shows, as the game's armor does: a stand without arms keeps the chestplate's shoulders.
 */
final class WornArmorModel extends Model<HumanoidRenderState> {
	private final HumanoidModel<HumanoidRenderState> wearer;
	private final HumanoidModel<HumanoidRenderState> armor;
	private final EquipmentSlot slot;

	WornArmorModel(HumanoidModel<HumanoidRenderState> wearer, HumanoidModel<HumanoidRenderState> armor, EquipmentSlot slot) {
		super(armor.root(), armor.renderType());
		this.wearer = wearer;
		this.armor = armor;
		this.slot = slot;
	}

	@Override
	public void setupAnim(HumanoidRenderState state) {
		this.resetPose();
		this.wearer.setupAnim(state);
		this.armor.copyTransforms(this.wearer);
		boolean head = this.slot == EquipmentSlot.HEAD;
		boolean chest = this.slot == EquipmentSlot.CHEST;
		boolean legs = this.slot == EquipmentSlot.LEGS;
		boolean feet = this.slot == EquipmentSlot.FEET;
		// (the helmet's outer layer goes with its head, not with the wearer's own hat layer, which a skin may leave off)
		this.armor.head.visible = head && shown(state, this.wearer.head);
		this.armor.hat.visible = head;
		this.armor.body.visible = (chest || legs) && shown(state, this.wearer.body);
		this.armor.rightArm.visible = chest && shown(state, this.wearer.rightArm);
		this.armor.leftArm.visible = chest && shown(state, this.wearer.leftArm);
		this.armor.rightLeg.visible = (legs || feet) && shown(state, this.wearer.rightLeg);
		this.armor.leftLeg.visible = (legs || feet) && shown(state, this.wearer.leftLeg);
	}

	/** A part of the armor shows where the wearer's own part does on a player, always on anything else. */
	private static boolean shown(HumanoidRenderState state, ModelPart wearerPart) {
		return !(state instanceof AvatarRenderState) || wearerPart.visible;
	}
}
