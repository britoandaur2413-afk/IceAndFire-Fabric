package com.iafabric.citadel.client.model.basic;

import com.iafabric.citadel.animation.IRotatablePart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A box tree drawn with its own pose (replaces Citadel's BasicModelPart). Rotation point in
 * pixels, angles in radians, rotated Z then Y then X like vanilla parts.
 */
public class BasicModelPart implements IRotatablePart {
	private static final Set<Direction> ALL_FACES = EnumSet.allOf(Direction.class);

	public float textureWidth = 64.0F;
	public float textureHeight = 32.0F;
	public int textureOffsetX;
	public int textureOffsetY;
	public float rotationPointX;
	public float rotationPointY;
	public float rotationPointZ;
	public float rotateAngleX;
	public float rotateAngleY;
	public float rotateAngleZ;
	public float offsetX;
	public float offsetY;
	public float offsetZ;
	public float scaleX = 1.0F;
	public float scaleY = 1.0F;
	public float scaleZ = 1.0F;
	public boolean mirror;
	public boolean showModel = true;
	public boolean isHidden;
	public final List<ModelPart.Cube> cubeList = new ArrayList<>();
	public final List<BasicModelPart> childModels = new ArrayList<>();
	public BasicModelPart parent;

	public BasicModelPart(BasicEntityModel<?> model) {
		if (model != null) {
			setTextureSize(model.texWidth, model.texHeight);
		}
	}

	public BasicModelPart(BasicEntityModel<?> model, int texOffX, int texOffY) {
		this(model);
		setTextureOffset(texOffX, texOffY);
	}

	public BasicModelPart setTextureOffset(int x, int y) {
		this.textureOffsetX = x;
		this.textureOffsetY = y;
		return this;
	}

	public BasicModelPart texOffs(int x, int y) {
		return setTextureOffset(x, y);
	}

	public BasicModelPart setTextureSize(int width, int height) {
		this.textureWidth = width;
		this.textureHeight = height;
		return this;
	}

	public void setPos(float x, float y, float z) {
		this.rotationPointX = x;
		this.rotationPointY = y;
		this.rotationPointZ = z;
	}

	public void setRotationPoint(float x, float y, float z) {
		setPos(x, y, z);
	}

	public BasicModelPart addBox(float x, float y, float z, float width, float height, float depth) {
		return addBox(x, y, z, width, height, depth, 0.0F, this.mirror);
	}

	public BasicModelPart addBox(float x, float y, float z, float width, float height, float depth, boolean mirror) {
		return addBox(x, y, z, width, height, depth, 0.0F, mirror);
	}

	public BasicModelPart addBox(float x, float y, float z, float width, float height, float depth, float grow) {
		return addBox(x, y, z, width, height, depth, grow, this.mirror);
	}

	public BasicModelPart addBox(float x, float y, float z, float width, float height, float depth, float grow, boolean mirror) {
		cubeList.add(new ModelPart.Cube(textureOffsetX, textureOffsetY, x, y, z, width, height, depth, grow, grow, grow, mirror,
			textureWidth, textureHeight, ALL_FACES));
		return this;
	}

	public BasicModelPart addBox(String partName, float x, float y, float z, int width, int height, int depth, float grow, int texOffX, int texOffY) {
		setTextureOffset(texOffX, texOffY);
		return addBox(x, y, z, width, height, depth, grow, this.mirror);
	}

	public void addChild(BasicModelPart child) {
		childModels.add(child);
		child.parent = this;
	}

	@Override
	public void addRotation(float x, float y, float z) {
		this.rotateAngleX += x;
		this.rotateAngleY += y;
		this.rotateAngleZ += z;
	}

	public void copyFrom(BasicModelPart other) {
		this.rotateAngleX = other.rotateAngleX;
		this.rotateAngleY = other.rotateAngleY;
		this.rotateAngleZ = other.rotateAngleZ;
		this.rotationPointX = other.rotationPointX;
		this.rotationPointY = other.rotationPointY;
		this.rotationPointZ = other.rotationPointZ;
	}

	public void translateRotate(PoseStack poseStack) {
		poseStack.translate(this.offsetX, this.offsetY, this.offsetZ);
		poseStack.translate(this.rotationPointX / 16.0F, this.rotationPointY / 16.0F, this.rotationPointZ / 16.0F);
		if (this.rotateAngleZ != 0.0F) {
			poseStack.rotate(Axis.ZP, this.rotateAngleZ);
		}
		if (this.rotateAngleY != 0.0F) {
			poseStack.rotate(Axis.YP, this.rotateAngleY);
		}
		if (this.rotateAngleX != 0.0F) {
			poseStack.rotate(Axis.XP, this.rotateAngleX);
		}
		if (this.scaleX != 1.0F || this.scaleY != 1.0F || this.scaleZ != 1.0F) {
			poseStack.scale(this.scaleX, this.scaleY, this.scaleZ);
		}
	}

	public void translateAndRotate(PoseStack poseStack) {
		translateRotate(poseStack);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int light, int overlay) {
		render(poseStack, buffer, light, overlay, -1);
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float red, float green, float blue, float alpha) {
		render(poseStack, buffer, light, overlay, ARGB.colorFromFloat(alpha, red, green, blue));
	}

	public void render(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
		if (!this.showModel || this.isHidden) {
			return;
		}
		if (this.cubeList.isEmpty() && this.childModels.isEmpty()) {
			return;
		}
		poseStack.pushPose();
		translateRotate(poseStack);
		PoseStack.Pose pose = poseStack.last();
		for (ModelPart.Cube cube : this.cubeList) {
			cube.compile(pose, buffer, light, overlay, color);
		}
		for (BasicModelPart child : this.childModels) {
			child.render(poseStack, buffer, light, overlay, color);
		}
		poseStack.popPose();
	}
}
