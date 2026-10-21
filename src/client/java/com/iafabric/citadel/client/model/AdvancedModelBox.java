package com.iafabric.citadel.client.model;

import com.iafabric.citadel.client.model.basic.BasicEntityModel;
import com.iafabric.citadel.client.model.basic.BasicModelPart;
import net.minecraft.util.Mth;

/**
 * A model box that remembers its default pose and has the usual walk / swing / flap / bob
 * helpers (replaces Citadel's AdvancedModelBox).
 */
public class AdvancedModelBox extends BasicModelPart {
	public float defaultRotationX;
	public float defaultRotationY;
	public float defaultRotationZ;
	public float defaultPositionX;
	public float defaultPositionY;
	public float defaultPositionZ;
	public float defaultOffsetX;
	public float defaultOffsetY;
	public float defaultOffsetZ;
	public String boxName = "";

	public AdvancedModelBox(BasicEntityModel<?> model) {
		super(model);
	}

	public AdvancedModelBox(BasicEntityModel<?> model, String name) {
		super(model);
		this.boxName = name;
	}

	public AdvancedModelBox(BasicEntityModel<?> model, int texOffX, int texOffY) {
		super(model, texOffX, texOffY);
	}

	public AdvancedModelBox(BasicEntityModel<?> model, String name, int texOffX, int texOffY) {
		super(model, texOffX, texOffY);
		this.boxName = name;
	}

	@Override
	public AdvancedModelBox setTextureOffset(int x, int y) {
		super.setTextureOffset(x, y);
		return this;
	}

	@Override
	public AdvancedModelBox texOffs(int x, int y) {
		super.setTextureOffset(x, y);
		return this;
	}

	@Override
	public AdvancedModelBox addBox(float x, float y, float z, float width, float height, float depth) {
		super.addBox(x, y, z, width, height, depth);
		return this;
	}

	@Override
	public AdvancedModelBox addBox(float x, float y, float z, float width, float height, float depth, boolean mirror) {
		super.addBox(x, y, z, width, height, depth, mirror);
		return this;
	}

	@Override
	public AdvancedModelBox addBox(float x, float y, float z, float width, float height, float depth, float grow) {
		super.addBox(x, y, z, width, height, depth, grow);
		return this;
	}

	public AdvancedModelBox getParent() {
		return parent instanceof AdvancedModelBox box ? box : null;
	}

	/** Kept for the original models; children always follow their parent's scale here. */
	public void setShouldScaleChildren(boolean scaleChildren) {
	}

	public void setScale(float x, float y, float z) {
		this.scaleX = x;
		this.scaleY = y;
		this.scaleZ = z;
	}

	public void setScaleX(float x) {
		this.scaleX = x;
	}

	public void updateDefaultPose() {
		this.defaultRotationX = this.rotateAngleX;
		this.defaultRotationY = this.rotateAngleY;
		this.defaultRotationZ = this.rotateAngleZ;
		this.defaultPositionX = this.rotationPointX;
		this.defaultPositionY = this.rotationPointY;
		this.defaultPositionZ = this.rotationPointZ;
		this.defaultOffsetX = this.offsetX;
		this.defaultOffsetY = this.offsetY;
		this.defaultOffsetZ = this.offsetZ;
	}

	public void resetToDefaultPose() {
		this.rotateAngleX = this.defaultRotationX;
		this.rotateAngleY = this.defaultRotationY;
		this.rotateAngleZ = this.defaultRotationZ;
		this.rotationPointX = this.defaultPositionX;
		this.rotationPointY = this.defaultPositionY;
		this.rotationPointZ = this.defaultPositionZ;
		this.offsetX = this.defaultOffsetX;
		this.offsetY = this.defaultOffsetY;
		this.offsetZ = this.defaultOffsetZ;
	}

	public void walk(float speed, float degree, boolean invert, float offset, float weight, float walk, float walkAmount) {
		this.rotateAngleX += calculateRotation(speed, degree, invert, offset, weight, walk, walkAmount);
	}

	public void flap(float speed, float degree, boolean invert, float offset, float weight, float flap, float flapAmount) {
		this.rotateAngleZ += calculateRotation(speed, degree, invert, offset, weight, flap, flapAmount);
	}

	public void swing(float speed, float degree, boolean invert, float offset, float weight, float swing, float swingAmount) {
		this.rotateAngleY += calculateRotation(speed, degree, invert, offset, weight, swing, swingAmount);
	}

	public void bob(float speed, float degree, boolean bounce, float f, float f1) {
		float bob = (float) (Math.sin(f * speed) * f1 * degree - f1 * degree);
		if (bounce) {
			bob = (float) -Math.abs(Math.sin(f * speed) * f1 * degree);
		}
		this.rotationPointY += bob;
	}

	private float calculateRotation(float speed, float degree, boolean invert, float offset, float weight, float f, float f1) {
		float rotation = Mth.cos(f * speed + offset) * degree * f1 + weight * f1;
		return invert ? -rotation : rotation;
	}
}
