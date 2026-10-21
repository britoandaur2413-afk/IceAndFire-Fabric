package com.iafabric.client.model.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** The 3D armors: the humanoid armor boxes with the horns, spikes and plates of each armor. */
public final class IafArmorMeshes {
	private IafArmorMeshes() {
	}

	/** From the 1.18 ModelTrollArmor. */
	public static MeshDefinition troll(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("head").addOrReplaceChild("hornL", CubeListBuilder.create().texOffs(3, 41).mirror().addBox(-1.0F, -0.5F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(3.0F, -2.2F, -3.0F, -0.7740535232594852F, 2.9595548126067843F, -0.27314402793711257F));
		partdefinition.getChild("head").getChild("hornL").addOrReplaceChild("hornL2", CubeListBuilder.create().texOffs(15, 50).mirror().addBox(-0.51F, -0.8F, 0.0F, 1, 2, 7), PartPose.offsetAndRotation(-0.4F, 1.3F, 4.5F, 1.2747884856566583F, 0.0F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("hornR", CubeListBuilder.create().texOffs(4, 41).mirror().addBox(-0.5F, -0.5F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(-3.3F, -2.2F, -3.0F, -0.7740535232594852F, -2.9595548126067843F, 0.27314402793711257F));
		partdefinition.getChild("head").getChild("hornR").addOrReplaceChild("hornR2", CubeListBuilder.create().texOffs(15, 50).mirror().addBox(-0.01F, -0.8F, 0.0F, 1, 2, 7), PartPose.offsetAndRotation(-0.6F, 1.3F, 4.5F, 1.2747884856566583F, 0.0F, 0.0F));
			return meshdefinition;
	}

	/** From the 1.18 ModelFireDragonScaleArmor. */
	public static MeshDefinition fireScale(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(-0.8F, 0.0F, -0.8F, -1.2217304763960306F, 1.2217304763960306F, -0.17453292519943295F));
		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(-0.7F, 3.6F, -0.4F, -1.4114477660878142F, 0.0F, 0.0F));
		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 5.0F, 0.4F, -1.4114477660878142F, 0.0F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.8F, 0.0F, -0.8F, -1.2217304763960306F, -1.2217304763960306F, 0.17453292519943295F));
		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.7F, 3.6F, -0.4F, -1.4114477660878142F, 0.0F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 5.0F, 0.4F, -1.4114477660878142F, 0.0F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornR", CubeListBuilder.create().texOffs(48, 44).addBox(-1.0F, -0.5F, 0.0F, 2, 3, 5), PartPose.offsetAndRotation(-3.6F, -8.0F, 1.0F, 0.3141592653589793F, -0.33161255787892263F, -0.19198621771937624F));
		partdefinition.getChild("head").addOrReplaceChild("HornL", CubeListBuilder.create().texOffs(48, 44).mirror().addBox(-1.0F, -0.5F, 0.0F, 2, 3, 5), PartPose.offsetAndRotation(3.6F, -8.0F, 1.0F, 0.3141592653589793F, 0.33161255787892263F, 0.19198621771937624F));

		partdefinition.getChild("head").addOrReplaceChild("HornR3", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(-4.0F, -4.0F, 0.7F, -0.06981317007977318F, -0.4886921905584123F, -0.08726646259971647F));
		partdefinition.getChild("head").addOrReplaceChild("HornL3", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(4.0F, -4.0F, 0.7F, -0.06981317007977318F, 0.4886921905584123F, 0.08726646259971647F));

		partdefinition.getChild("head").addOrReplaceChild("HeadFront", CubeListBuilder.create().texOffs(6, 44).addBox(-3.5F, -2.8F, -8.8F, 7, 2, 5), PartPose.offsetAndRotation(0.0F, -5.6F, 0.0F, 0.045553093477052F, 0.0F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("Jaw", CubeListBuilder.create().texOffs(6, 51).addBox(-3.5F, 4.0F, -7.4F, 7, 2, 5), PartPose.offsetAndRotation(0.0F, -5.4F, 0.0F, -0.091106186954104F, 0.0F, 0.0F));

		partdefinition.getChild("right_arm").addOrReplaceChild("RightShoulderSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(-0.5F, -1.2F, 0.0F, -3.141592653589793F, 0.0F, -0.17453292519943295F));
		partdefinition.getChild("left_arm").addOrReplaceChild("LeftShoulderSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.5F, -1.2F, 0.0F, -3.141592653589793F, 0.0F, 0.17453292519943295F));

		partdefinition.getChild("right_arm").addOrReplaceChild("RightShoulderSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(-1.8F, -0.1F, 0.0F, -3.141592653589793F, 0.0F, -0.2617993877991494F));
		partdefinition.getChild("left_arm").addOrReplaceChild("LeftShoulderSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(1.8F, -0.1F, 0.0F, -3.141592653589793F, 0.0F, 0.2617993877991494F));

		partdefinition.getChild("body").addOrReplaceChild("BackSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 0.9F, 0.2F, 1.1838568316277536F, 0.0F, 0.0F));
		partdefinition.getChild("body").addOrReplaceChild("BackSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 3.5F, 0.6F, 1.1838568316277536F, 0.0F, 0.0F));
		partdefinition.getChild("body").addOrReplaceChild("BackSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 6.4F, 0.0F, 1.1838568316277536F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").addOrReplaceChild("HornR2", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 4.5F, -0.07504915783575616F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").addOrReplaceChild("HornL2", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 4.5F, -0.07504915783575616F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HeadFront").addOrReplaceChild("Teeth1", CubeListBuilder.create().texOffs(6, 34).addBox(-3.6F, 0.1F, -8.9F, 4, 1, 5), PartPose.offset(0.0F, -1.0F, 0.0F));
		partdefinition.getChild("head").getChild("HeadFront").addOrReplaceChild("Teeth2", CubeListBuilder.create().texOffs(6, 34).mirror().addBox(-0.4F, 0.1F, -8.9F, 4, 1, 5), PartPose.offset(0.0F, -1.0F, 0.0F));

			return meshdefinition;
	}

	/** From the 1.18 ModelIceDragonScaleArmor. */
	public static MeshDefinition iceScale(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(-0.8F, 0.0F, -0.8F, -1.2217304763960306F, 1.2217304763960306F, -0.17453292519943295F));
		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(-0.7F, 3.6F, -0.4F, -1.4114477660878142F, 0.0F, 0.0F));
		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 5.0F, 0.4F, -1.4114477660878142F, 0.0F, 0.0F));

		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.8F, 0.0F, -0.8F, -1.2217304763960306F, -1.2217304763960306F, 0.17453292519943295F));
		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.7F, 3.6F, -0.4F, -1.4114477660878142F, 0.0F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 5.0F, 0.4F, -1.4114477660878142F, 0.0F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornR", CubeListBuilder.create().texOffs(48, 44).addBox(-1.0F, -0.5F, 0.0F, 2, 3, 5), PartPose.offsetAndRotation(-3.6F, -8.0F, 1.0F, 0.3141592653589793F, -0.33161255787892263F, -0.19198621771937624F));
		partdefinition.getChild("head").addOrReplaceChild("HornL", CubeListBuilder.create().texOffs(48, 44).mirror().addBox(-1.0F, -0.5F, 0.0F, 2, 3, 5), PartPose.offsetAndRotation(3.6F, -8.0F, 1.0F, 0.3141592653589793F, 0.33161255787892263F, 0.19198621771937624F));

		partdefinition.getChild("head").addOrReplaceChild("HornR3", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(-4.0F, -4.0F, 0.7F, -0.06981317007977318F, -0.4886921905584123F, -0.08726646259971647F));
		partdefinition.getChild("head").addOrReplaceChild("HornL3", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(4.0F, -4.0F, 0.7F, -0.06981317007977318F, 0.4886921905584123F, 0.08726646259971647F));

		partdefinition.getChild("head").addOrReplaceChild("HornR4", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(-1.0F, -7.5F, 1.9F, 0.45378560551852565F, -0.3141592653589793F, -0.03490658503988659F));
		partdefinition.getChild("head").addOrReplaceChild("HornL4", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(1.0F, -7.5F, 1.9F, 0.45378560551852565F, 0.3141592653589793F, 0.03490658503988659F));

		partdefinition.getChild("head").addOrReplaceChild("HeadFront", CubeListBuilder.create().texOffs(6, 44).addBox(-3.5F, -2.8F, -8.8F, 7, 2, 5), PartPose.offsetAndRotation(0.0F, -5.6F, 0.0F, 0.045553093477052F, 0.0F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("Jaw", CubeListBuilder.create().texOffs(6, 51).addBox(-3.5F, 4.0F, -7.4F, 7, 2, 5), PartPose.offsetAndRotation(0.0F, -5.4F, 0.0F, -0.091106186954104F, 0.0F, 0.0F));

		partdefinition.getChild("right_arm").addOrReplaceChild("RightShoulderSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(-0.5F, -1.2F, 0.0F, -3.141592653589793F, 0.0F, -0.17453292519943295F));
		partdefinition.getChild("left_arm").addOrReplaceChild("LeftShoulderSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.5F, -1.2F, 0.0F, -3.141592653589793F, 0.0F, 0.17453292519943295F));

		partdefinition.getChild("right_arm").addOrReplaceChild("RightShoulderSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(-1.8F, -0.1F, 0.0F, -3.141592653589793F, 0.0F, -0.2617993877991494F));
		partdefinition.getChild("left_arm").addOrReplaceChild("LeftShoulderSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(1.8F, -0.1F, 0.0F, -3.141592653589793F, 0.0F, 0.2617993877991494F));

		partdefinition.getChild("body").addOrReplaceChild("BackSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 0.9F, 0.2F, 1.1838568316277536F, 0.0F, 0.0F));
		partdefinition.getChild("body").addOrReplaceChild("BackSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 3.5F, 0.6F, 1.1838568316277536F, 0.0F, 0.0F));
		partdefinition.getChild("body").addOrReplaceChild("BackSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 6.4F, 0.0F, 1.1838568316277536F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").addOrReplaceChild("HornR2", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 4.5F, -0.07504915783575616F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").addOrReplaceChild("HornL2", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 4.5F, -0.07504915783575616F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("Jaw").addOrReplaceChild("Teeth1", CubeListBuilder.create().texOffs(6, 34).addBox(-3.6F, 0.1F, -8.9F, 4, 1, 5), PartPose.offset(0.0F, 3.0F, 1.4F));
		partdefinition.getChild("head").getChild("Jaw").addOrReplaceChild("Teeth2", CubeListBuilder.create().texOffs(6, 34).mirror().addBox(-0.4F, 0.1F, -8.9F, 4, 1, 5), PartPose.offset(0.0F, 3.0F, 1.4F));

			return meshdefinition;
	}

	/** From the 1.18 ModelLightningDragonScaleArmor. */
	public static MeshDefinition lightningScale(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(-0.8F, 0.0F, -0.8F, -1.2217304763960306F, 1.2217304763960306F, -0.17453292519943295F));
		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(-0.7F, 3.6F, -0.4F, -1.4114477660878142F, 0.0F, 0.0F));
		partdefinition.getChild("right_leg").addOrReplaceChild("RightLegSpike", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 5.0F, 0.4F, -1.4114477660878142F, 0.0F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.8F, 0.0F, -0.8F, -1.2217304763960306F, -1.2217304763960306F, 0.17453292519943295F));
		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.7F, 3.6F, -0.4F, -1.4114477660878142F, 0.0F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("LeftLegSpike", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, 0.0F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 5.0F, 0.4F, -1.4114477660878142F, 0.0F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornR", CubeListBuilder.create().texOffs(48, 44).addBox(-1.0F, -0.5F, 0.0F, 2, 3, 5), PartPose.offsetAndRotation(-3.6F, -8.0F, 0.0F, 0.4363323129985824F, -0.33161255787892263F, -0.19198621771937624F));
		partdefinition.getChild("head").addOrReplaceChild("HornL", CubeListBuilder.create().texOffs(48, 44).mirror().addBox(-1.0F, -0.5F, 0.0F, 2, 3, 5), PartPose.offsetAndRotation(3.6F, -8.0F, 0.0F, 0.4363323129985824F, 0.33161255787892263F, 0.19198621771937624F));

		partdefinition.getChild("head").addOrReplaceChild("HornR3", CubeListBuilder.create().texOffs(47, 37).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 4), PartPose.offsetAndRotation(-4.0F, -3.0F, 0.7F, -0.06981317007977318F, -0.4886921905584123F, -0.08726646259971647F));
		partdefinition.getChild("head").addOrReplaceChild("HornL3", CubeListBuilder.create().texOffs(47, 37).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 4), PartPose.offsetAndRotation(4.0F, -3.0F, 0.7F, -0.06981317007977318F, 0.4886921905584123F, 0.08726646259971647F));

		partdefinition.getChild("head").addOrReplaceChild("HeadFront", CubeListBuilder.create().texOffs(6, 44).addBox(-3.5F, -2.8F, -8.8F, 7, 2, 5), PartPose.offsetAndRotation(0.0F, -5.6F, 0.0F, 0.045553093477052F, 0.0F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("Jaw", CubeListBuilder.create().texOffs(6, 51).addBox(-3.5F, 4.0F, -7.4F, 7, 2, 5), PartPose.offsetAndRotation(0.0F, -5.4F, 0.0F, -0.091106186954104F, 0.0F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornR4", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(-4.0F, -5.1F, 0.1F, 0.12217304763960307F, -0.3141592653589793F, -0.03490658503988659F));
		partdefinition.getChild("head").addOrReplaceChild("HornL4", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(4.0F, -5.1F, -0.1F, 0.12217304763960307F, 0.3141592653589793F, 0.03490658503988659F));

		partdefinition.getChild("head").addOrReplaceChild("Teeth1", CubeListBuilder.create().texOffs(6, 34).addBox(-3.6F, 0.1F, -8.9F, 4, 1, 5), PartPose.offset(0.0F, -6.2F, 0.0F));

		partdefinition.getChild("right_arm").addOrReplaceChild("RightShoulderSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(-0.5F, -1.2F, 0.0F, -3.141592653589793F, 0.0F, -0.17453292519943295F));
		partdefinition.getChild("left_arm").addOrReplaceChild("LeftShoulderSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.5F, -1.2F, 0.0F, -3.141592653589793F, 0.0F, 0.17453292519943295F));

		partdefinition.getChild("right_arm").addOrReplaceChild("RightShoulderSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(-1.8F, -0.1F, 0.0F, -3.141592653589793F, 0.0F, -0.2617993877991494F));
		partdefinition.getChild("left_arm").addOrReplaceChild("LeftShoulderSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(1.8F, -0.1F, 0.0F, -3.141592653589793F, 0.0F, 0.2617993877991494F));

		partdefinition.getChild("body").addOrReplaceChild("BackSpike1", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 0.9F, 0.2F, 1.1838568316277536F, 0.0F, 0.0F));
		partdefinition.getChild("body").addOrReplaceChild("BackSpike2", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 3.5F, 0.6F, 1.1838568316277536F, 0.0F, 0.0F));
		partdefinition.getChild("body").addOrReplaceChild("BackSpike3", CubeListBuilder.create().texOffs(0, 34).addBox(-0.5F, 0.0F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, 6.4F, 0.0F, 1.1838568316277536F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").addOrReplaceChild("HornR2", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 4.5F, -0.07504915783575616F, 0.5009094953223726F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").addOrReplaceChild("HornL2", CubeListBuilder.create().texOffs(46, 36).mirror().addBox(-0.5F, -0.8F, 0.0F, 1, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 4.5F, -0.07504915783575616F, -0.5009094953223726F, 0.0F));

		partdefinition.getChild("head").getChild("HeadFront").addOrReplaceChild("Teeth2", CubeListBuilder.create().texOffs(6, 34).mirror().addBox(-0.4F, 0.1F, -8.9F, 4, 1, 5), PartPose.offset(0.0F, -1.0F, 0.0F));

		partdefinition.getChild("head").getChild("Jaw").addOrReplaceChild("Teeth3", CubeListBuilder.create().texOffs(6, 34).addBox(-3.6F, 0.1F, -8.9F, 4, 1, 5), PartPose.offset(0.0F, 3.0F, 1.4F));
		partdefinition.getChild("head").getChild("Jaw").addOrReplaceChild("Teeth4", CubeListBuilder.create().texOffs(6, 34).mirror().addBox(-0.4F, 0.1F, -8.9F, 4, 1, 5), PartPose.offset(0.0F, 3.0F, 1.4F));

			return meshdefinition;
	}

	/** From the 1.18 ModelDragonsteelFireArmor. */
	public static MeshDefinition fireSteel(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("head").addOrReplaceChild("HornR", CubeListBuilder.create().texOffs(9, 39).addBox(-1.0F, -0.5F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(-2.5F, -7.9F, -4.2F, 0.43022366061660217F, -0.15707963267948966F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("HornL", CubeListBuilder.create().texOffs(9, 39).addBox(-1.0F, -0.5F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(2.5F, -7.9F, -4.2F, 0.43022366061660217F, 0.15707963267948966F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornL4", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(3.2F, -7.4F, -3.0F, -0.14713125594312196F, 0.296705972839036F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("HornR4", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(-3.2F, -7.4F, -3.0F, -0.14713125594312196F, -0.296705972839036F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("visor1", CubeListBuilder.create().texOffs(27, 50).addBox(-4.7F, -13.3F, -4.9F, 4, 5, 8), PartPose.offset(0.0F, 9.0F, 0.2F));
		partdefinition.getChild("head").addOrReplaceChild("visor2", CubeListBuilder.create().texOffs(27, 50).mirror().addBox(0.8F, -13.3F, -4.9F, 4, 5, 8), PartPose.offset(-0.1F, 9.0F, 0.2F));

		partdefinition.getChild("right_arm").addOrReplaceChild("sleeveRight", CubeListBuilder.create().texOffs(36, 33).addBox(-4.5F, -2.1F, -2.4F, 5, 6, 5), PartPose.offsetAndRotation(0.3F, -0.3F, 0.0F, 0.0F, 0.0F, -0.12217304763960307F));
		partdefinition.getChild("left_arm").addOrReplaceChild("sleeveLeft", CubeListBuilder.create().texOffs(36, 33).mirror().addBox(-0.5F, -2.1F, -2.4F, 5, 6, 5), PartPose.offsetAndRotation(-0.7F, -0.3F, 0.0F, 0.0F, 0.0F, 0.12217304763960307F));

		partdefinition.getChild("right_leg").addOrReplaceChild("robeLowerRight", CubeListBuilder.create().texOffs(4, 51).mirror().addBox(-2.1F, 0.0F, -2.5F, 4, 7, 5), PartPose.offset(0.0F, -0.2F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("robeLowerLeft", CubeListBuilder.create().texOffs(4, 51).addBox(-1.9F, 0.0F, -2.5F, 4, 7, 5), PartPose.offset(0.0F, -0.2F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").addOrReplaceChild("HornR2", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 3.6F, -0.3391174736624982F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").addOrReplaceChild("HornL2", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 3.6F, -0.3391174736624982F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").getChild("HornR2").addOrReplaceChild("HornR3", CubeListBuilder.create().texOffs(24, 44).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(0.0F, -0.1F, 4.3F, 0.5918411493512771F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").getChild("HornL2").addOrReplaceChild("HornL3", CubeListBuilder.create().texOffs(24, 44).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(0.0F, -0.1F, 4.3F, 0.5918411493512771F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR4").addOrReplaceChild("HornR5", CubeListBuilder.create().texOffs(25, 45).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, -0.1F, 4.3F, 0.3649483465920143F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL4").addOrReplaceChild("HornL5", CubeListBuilder.create().texOffs(25, 45).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, -0.1F, 4.3F, 0.3649483465920143F, 0.0F, 0.0F));
			return meshdefinition;
	}

	/** From the 1.18 ModelDragonsteelIceArmor. */
	public static MeshDefinition iceSteel(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("head").addOrReplaceChild("HornR", CubeListBuilder.create().texOffs(9, 39).addBox(-1.0F, -0.5F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(-2.8F, -7.9F, -4.2F, 0.27314402793711257F, -0.24434609527920614F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("HornL", CubeListBuilder.create().texOffs(9, 39).mirror().addBox(-1.0F, -0.5F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(2.8F, -7.9F, -4.2F, 0.27314402793711257F, 0.24434609527920614F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornR4", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(-3.2F, -6.4F, -3.0F, -0.14713125594312196F, -0.296705972839036F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("HornL4", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(3.2F, -6.4F, -3.0F, -0.14713125594312196F, 0.296705972839036F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornR6", CubeListBuilder.create().texOffs(9, 38).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(-1.1F, -7.6F, -4.0F, 0.5126032013107347F, -0.13962634015954636F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("HornL6", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(1.1F, -7.6F, -4.0F, 0.5126032013107347F, 0.13962634015954636F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("visor1", CubeListBuilder.create().texOffs(27, 50).addBox(-4.7F, -13.3F, -4.9F, 4, 5, 8), PartPose.offset(0.0F, 9.0F, 0.2F));
		partdefinition.getChild("head").addOrReplaceChild("visor2", CubeListBuilder.create().texOffs(27, 50).mirror().addBox(0.8F, -13.3F, -4.9F, 4, 5, 8), PartPose.offset(-0.1F, 9.0F, 0.2F));

		partdefinition.getChild("right_arm").addOrReplaceChild("sleeveRight", CubeListBuilder.create().texOffs(36, 33).addBox(-4.5F, -2.1F, -2.4F, 5, 4, 5), PartPose.offsetAndRotation(0.3F, -0.3F, 0.0F, 0.0F, 0.0F, -0.12217304763960307F));
		partdefinition.getChild("left_arm").addOrReplaceChild("sleeveLeft", CubeListBuilder.create().texOffs(36, 33).mirror().addBox(-0.5F, -2.1F, -2.4F, 5, 4, 5), PartPose.offsetAndRotation(-0.7F, -0.3F, 0.0F, 0.0F, 0.0F, 0.12217304763960307F));

		partdefinition.getChild("right_leg").addOrReplaceChild("robeLowerRight", CubeListBuilder.create().texOffs(4, 51).mirror().addBox(-2.1F, 0.0F, -2.5F, 4, 7, 5), PartPose.offset(0.0F, -0.2F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("robeLowerLeft", CubeListBuilder.create().texOffs(4, 51).addBox(-1.9F, 0.0F, -2.5F, 4, 7, 5), PartPose.offset(0.0F, -0.2F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").addOrReplaceChild("HornR2", CubeListBuilder.create().texOffs(9, 38).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 3.6F, -0.08726646259971647F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").addOrReplaceChild("HornL2", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 3.6F, -0.08726646259971647F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").getChild("HornR2").addOrReplaceChild("HornR3", CubeListBuilder.create().texOffs(24, 44).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.17453292519943295F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").getChild("HornL2").addOrReplaceChild("HornL3", CubeListBuilder.create().texOffs(24, 44).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.17453292519943295F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR4").addOrReplaceChild("HornR5", CubeListBuilder.create().texOffs(25, 45).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.13962634015954636F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL4").addOrReplaceChild("HornL5", CubeListBuilder.create().texOffs(25, 45).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.13962634015954636F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR6").addOrReplaceChild("HornR7", CubeListBuilder.create().texOffs(25, 45).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.12217304763960307F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL6").addOrReplaceChild("HornL7", CubeListBuilder.create().texOffs(25, 45).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.12217304763960307F, 0.0F, 0.0F));

			return meshdefinition;
	}

	/** From the 1.18 ModelDragonsteelLightningArmor. */
	public static MeshDefinition lightningSteel(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("head").addOrReplaceChild("HornR", CubeListBuilder.create().texOffs(9, 39).addBox(-1.0F, -0.5F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(-2.8F, -7.9F, -4.2F, 0.27314402793711257F, -0.24434609527920614F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("HornL", CubeListBuilder.create().texOffs(9, 39).mirror().addBox(-1.0F, -0.5F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(2.8F, -7.9F, -4.2F, 0.27314402793711257F, 0.24434609527920614F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornR2", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(-3.2F, -5.4F, -4.0F, -0.25185101106278174F, -0.296705972839036F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("HornL2", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(3.2F, -5.4F, -4.0F, -0.25185101106278174F, 0.296705972839036F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("HornR4", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(-3.2F, -6.4F, -3.0F, 0.0F, -0.296705972839036F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("HornL4", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(3.2F, -6.4F, -3.0F, 0.0F, 0.296705972839036F, 0.0F));

		partdefinition.getChild("head").addOrReplaceChild("visor1", CubeListBuilder.create().texOffs(27, 50).addBox(-4.7F, -13.3F, -4.9F, 4, 5, 8), PartPose.offset(0.0F, 9.0F, 0.2F));
		partdefinition.getChild("head").addOrReplaceChild("visor2", CubeListBuilder.create().texOffs(27, 50).mirror().addBox(0.8F, -13.3F, -4.9F, 4, 5, 8), PartPose.offset(-0.1F, 9.0F, 0.2F));

		partdefinition.getChild("right_arm").addOrReplaceChild("sleeveRight", CubeListBuilder.create().texOffs(36, 33).addBox(-4.5F, -2.1F, -2.4F, 5, 4, 5), PartPose.offsetAndRotation(0.3F, -0.3F, 0.0F, 0.0F, 0.0F, -0.12217304763960307F));
		partdefinition.getChild("left_arm").addOrReplaceChild("sleeveLeft", CubeListBuilder.create().texOffs(36, 33).mirror().addBox(-0.5F, -2.1F, -2.4F, 5, 4, 5), PartPose.offsetAndRotation(-0.7F, -0.3F, 0.0F, 0.0F, 0.0F, 0.12217304763960307F));

		partdefinition.getChild("right_leg").addOrReplaceChild("robeLowerRight", CubeListBuilder.create().texOffs(4, 51).mirror().addBox(-2.1F, 0.0F, -2.5F, 4, 7, 5), PartPose.offset(0.0F, -0.2F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("robeLowerLeft", CubeListBuilder.create().texOffs(4, 51).addBox(-1.9F, 0.0F, -2.5F, 4, 7, 5), PartPose.offset(0.0F, -0.2F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").addOrReplaceChild("HornR2_1", CubeListBuilder.create().texOffs(9, 38).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 3.6F, -0.08726646259971647F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").addOrReplaceChild("HornL2_1", CubeListBuilder.create().texOffs(9, 38).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 5), PartPose.offsetAndRotation(0.0F, 0.3F, 3.6F, -0.08726646259971647F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR").getChild("HornR2_1").addOrReplaceChild("HornR3", CubeListBuilder.create().texOffs(24, 44).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.17453292519943295F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL").getChild("HornL2_1").addOrReplaceChild("HornL3", CubeListBuilder.create().texOffs(24, 44).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 4), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.17453292519943295F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR2").addOrReplaceChild("HornR3_1", CubeListBuilder.create().texOffs(25, 45).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.13962634015954636F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL2").addOrReplaceChild("HornL3_1", CubeListBuilder.create().texOffs(25, 45).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.13962634015954636F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("HornR4").addOrReplaceChild("HornR5", CubeListBuilder.create().texOffs(25, 45).mirror().addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.13962634015954636F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("HornL4").addOrReplaceChild("HornL5", CubeListBuilder.create().texOffs(25, 45).addBox(-1.0F, -0.8F, 0.0F, 2, 2, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 4.3F, 0.13962634015954636F, 0.0F, 0.0F));

			return meshdefinition;
	}

	/** From the 1.18 ModelDeathWormArmor. */
	public static MeshDefinition deathworm(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("right_arm").addOrReplaceChild("spineR1", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -1.7F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(-1.0F, -2.7F, 0.0F, -0.4914847173616032F, 0.0F, 0.0F));
		partdefinition.getChild("left_arm").addOrReplaceChild("spineL1", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -1.7F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(1.0F, -2.7F, 0.0F, -0.4914847173616032F, 0.0F, 0.0F));

		partdefinition.getChild("right_arm").addOrReplaceChild("spineR2", CubeListBuilder.create().texOffs(32, 40).addBox(-0.6F, -1.7F, -0.5F, 1, 2, 1), PartPose.offsetAndRotation(-2.5F, -1.6F, 0.0F, -0.4914847173616032F, 0.0F, 0.0F));
		partdefinition.getChild("left_arm").addOrReplaceChild("spineL2", CubeListBuilder.create().texOffs(32, 40).addBox(-0.4F, -1.7F, -0.5F, 1, 2, 1), PartPose.offsetAndRotation(2.5F, -1.6F, 0.0F, -0.4914847173616032F, 0.0F, 0.0F));

		partdefinition.getChild("head").getChild("hat").addOrReplaceChild("spineH1", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -1.7F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, -9.0F, -3.0F, -0.4914847173616032F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("hat").addOrReplaceChild("spineH2", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -2.7F, -0.5F, 1, 4, 1), PartPose.offsetAndRotation(0.0F, -9.0F, 0.0F, -0.4914847173616032F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("hat").addOrReplaceChild("spineH3", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -1.7F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, -9.0F, 3.0F, -0.8651597102135892F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("hat").addOrReplaceChild("spineH4", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -2.7F, -0.5F, 1, 4, 1), PartPose.offsetAndRotation(0.0F, -8.0F, 5.0F, -1.5481070465189704F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("hat").addOrReplaceChild("spineH5", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -1.7F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, -6.0F, 5.0F, -1.8212510744560826F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("hat").addOrReplaceChild("spineH6", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -2.7F, -0.5F, 1, 5, 1), PartPose.offsetAndRotation(0.0F, -3.5F, 5.0F, -2.0032889154390916F, 0.0F, 0.0F));
		partdefinition.getChild("head").getChild("hat").addOrReplaceChild("spineH7", CubeListBuilder.create().texOffs(32, 40).addBox(-0.5F, -1.7F, -0.5F, 1, 3, 1), PartPose.offsetAndRotation(0.0F, -1.3F, 4.5F, -2.0032889154390916F, 0.0F, 0.0F));

			return meshdefinition;
	}

	/** From the 1.18 ModelSeaSerpentArmor. */
	public static MeshDefinition seaSerpent(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("head").addOrReplaceChild("headFin", CubeListBuilder.create().texOffs(0, 32).addBox(-0.5F, -8.4F, -7.9F, 1, 16, 14), PartPose.offsetAndRotation(-3.5F, -8.8F, 3.5F, 3.141592653589793F, -0.5235987755982988F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("headFin2", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(-0.5F, -8.4F, -7.9F, 1, 16, 14), PartPose.offsetAndRotation(3.5F, -8.8F, 3.5F, 3.141592653589793F, 0.5235987755982988F, 0.0F));

		partdefinition.getChild("right_arm").addOrReplaceChild("armFinR", CubeListBuilder.create().texOffs(30, 32).addBox(-0.5F, -5.4F, -6.0F, 1, 7, 5), PartPose.offsetAndRotation(-1.5F, 4.0F, -0.4F, 3.141592653589793F, -1.3089969389957472F, -0.003490658503988659F));
		partdefinition.getChild("left_arm").addOrReplaceChild("armFinL", CubeListBuilder.create().texOffs(30, 32).mirror().addBox(-0.5F, -5.4F, -6.0F, 1, 7, 5), PartPose.offsetAndRotation(1.5F, 4.0F, -0.4F, 3.141592653589793F, 1.3089969389957472F, 0.0F));

		partdefinition.getChild("right_leg").addOrReplaceChild("legFinR", CubeListBuilder.create().texOffs(45, 31).addBox(-0.5F, -5.4F, -6.0F, 1, 7, 6), PartPose.offsetAndRotation(-1.5F, 5.2F, 1.6F, 3.141592653589793F, -1.3089969389957472F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("legFinL", CubeListBuilder.create().texOffs(45, 31).mirror().addBox(-0.5F, -5.4F, -6.0F, 1, 7, 6), PartPose.offsetAndRotation(1.5F, 5.2F, 1.6F, 3.141592653589793F, 1.3089969389957472F, 0.0F));

		partdefinition.getChild("right_arm").addOrReplaceChild("shoulderR", CubeListBuilder.create().texOffs(38, 46).addBox(-3.5F, -2.0F, -2.5F, 5, 12, 5), PartPose.offset(0.0F, -0.5F, 0.0F));
		partdefinition.getChild("left_arm").addOrReplaceChild("shoulderL", CubeListBuilder.create().texOffs(38, 46).mirror().addBox(-1.5F, -2.0F, -2.5F, 5, 12, 5), PartPose.offset(0.0F, -0.5F, 0.0F));

			return meshdefinition;
	}

	/** From the 1.18 ModelSilverArmor. */
	public static MeshDefinition silver(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();
		partdefinition.getChild("head").addOrReplaceChild("faceGuard", CubeListBuilder.create().texOffs(30, 47).addBox(-4.5F, -3.0F, -6.1F, 9, 9, 8), PartPose.offsetAndRotation(0.0F, -6.6F, 1.9F, -0.7285004297824331F, 0.0F, 0.0F));
		partdefinition.getChild("head").addOrReplaceChild("helmWingR", CubeListBuilder.create().texOffs(2, 37).addBox(-0.5F, -1.0F, 0.0F, 1, 4, 6), PartPose.offsetAndRotation(-3.0F, -6.3F, 1.3F, 0.5235987755982988F, -0.4363323129985824F, -0.05235987755982988F));
		partdefinition.getChild("head").addOrReplaceChild("helmWingL", CubeListBuilder.create().texOffs(2, 37).mirror().addBox(-0.5F, -1.0F, 0.0F, 1, 4, 6), PartPose.offsetAndRotation(3.0F, -6.3F, 1.3F, 0.5235987755982988F, 0.4363323129985824F, 0.05235987755982988F));

		partdefinition.getChild("head").getChild("hat").addOrReplaceChild("crest", CubeListBuilder.create().texOffs(18, 32).addBox(0.0F, -0.5F, 0.0F, 1, 9, 9), PartPose.offsetAndRotation(0.0F, -7.9F, -0.1F, 1.2292353921796064F, 0.0F, 0.0F));

		partdefinition.getChild("body").addOrReplaceChild("robeLowerBack", CubeListBuilder.create().texOffs(4, 55).mirror().addBox(-4.0F, 0.0F, -2.5F, 8, 8, 1), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 3.141592653589793F, 0.0F));
		partdefinition.getChild("body").addOrReplaceChild("robeLower", CubeListBuilder.create().texOffs(4, 55).addBox(-4.0F, 0.0F, -2.5F, 8, 8, 1), PartPose.offset(0.0F, 12.0F, 0.0F));
			return meshdefinition;
	}

	/** From the 1.18 ModelCopperArmor. */
	public static MeshDefinition copper(CubeDeformation deformation) {
		float offset = 0.0F;
		MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, offset);
		PartDefinition partdefinition = meshdefinition.getRoot();

		partdefinition.getChild("head").addOrReplaceChild("crest", CubeListBuilder.create().texOffs(23, 31).addBox(0.0F, -7.5F, -9.0F, 0, 16, 14), PartPose.offset(0.0F, -7.6F, 2.6F));
		partdefinition.getChild("head").addOrReplaceChild("facePlate", CubeListBuilder.create().texOffs(34, 32).addBox(-4.5F, -8.2F, -4.01F, 9, 10, 1), PartPose.offset(0.0F, 0.0F, 0.0F));

		partdefinition.getChild("right_leg").addOrReplaceChild("robeLowerRight", CubeListBuilder.create().texOffs(0, 51).addBox(-2.1F, 0.0F, -2.5F, 4, 8, 5).mirror(), PartPose.offset(0.0F, -0.2F, 0.0F));
		partdefinition.getChild("left_leg").addOrReplaceChild("robeLowerLeft", CubeListBuilder.create().texOffs(0, 51).addBox(-1.9F, 0.0F, -2.5F, 4, 8, 5), PartPose.offset(0.0F, -0.2F, 0.0F));

			return meshdefinition;
	}
}
