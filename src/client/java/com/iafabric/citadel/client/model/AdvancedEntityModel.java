package com.iafabric.citadel.client.model;

import com.iafabric.citadel.client.model.basic.BasicEntityModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** A model of {@link AdvancedModelBox}es with chain and pose helpers (replaces Citadel's AdvancedEntityModel). */
public abstract class AdvancedEntityModel<T extends Entity> extends BasicEntityModel<T> {

	public abstract Iterable<AdvancedModelBox> getAllParts();

	public void updateDefaultPose() {
		for (AdvancedModelBox box : getAllParts()) {
			box.updateDefaultPose();
		}
	}

	public void resetToDefaultPose() {
		for (AdvancedModelBox box : getAllParts()) {
			box.resetToDefaultPose();
		}
	}

	public void setRotateAngle(AdvancedModelBox box, float x, float y, float z) {
		box.rotateAngleX = x;
		box.rotateAngleY = y;
		box.rotateAngleZ = z;
	}

	public void faceTarget(float yaw, float pitch, float rotationDivisor, AdvancedModelBox... boxes) {
		float actualRotationDivisor = rotationDivisor * boxes.length;
		float yawAmount = yaw / (180.0F / (float) Math.PI) / actualRotationDivisor;
		float pitchAmount = pitch / (180.0F / (float) Math.PI) / actualRotationDivisor;
		for (AdvancedModelBox box : boxes) {
			box.rotateAngleY += yawAmount;
			box.rotateAngleX += pitchAmount;
		}
	}

	public void walk(AdvancedModelBox box, float speed, float degree, boolean invert, float offset, float weight, float walk, float walkAmount) {
		box.walk(speed, degree, invert, offset, weight, walk, walkAmount);
	}

	public void flap(AdvancedModelBox box, float speed, float degree, boolean invert, float offset, float weight, float flap, float flapAmount) {
		box.flap(speed, degree, invert, offset, weight, flap, flapAmount);
	}

	public void swing(AdvancedModelBox box, float speed, float degree, boolean invert, float offset, float weight, float swing, float swingAmount) {
		box.swing(speed, degree, invert, offset, weight, swing, swingAmount);
	}

	public void bob(AdvancedModelBox box, float speed, float degree, boolean bounce, float f, float f1) {
		box.bob(speed, degree, bounce, f, f1);
	}

	public void chainSwing(AdvancedModelBox[] boxes, float speed, float degree, double rootOffset, float swing, float swingAmount) {
		float offset = calculateChainOffset(rootOffset, boxes);
		for (int index = 0; index < boxes.length; index++) {
			boxes[index].rotateAngleY += calculateChainRotation(speed, degree, swing, swingAmount, offset, index);
		}
	}

	public void chainWave(AdvancedModelBox[] boxes, float speed, float degree, double rootOffset, float swing, float swingAmount) {
		float offset = calculateChainOffset(rootOffset, boxes);
		for (int index = 0; index < boxes.length; index++) {
			boxes[index].rotateAngleX += calculateChainRotation(speed, degree, swing, swingAmount, offset, index);
		}
	}

	public void chainFlap(AdvancedModelBox[] boxes, float speed, float degree, double rootOffset, float swing, float swingAmount) {
		float offset = calculateChainOffset(rootOffset, boxes);
		for (int index = 0; index < boxes.length; index++) {
			boxes[index].rotateAngleZ += calculateChainRotation(speed, degree, swing, swingAmount, offset, index);
		}
	}

	private float calculateChainRotation(float speed, float degree, float swing, float swingAmount, float offset, int boxIndex) {
		return Mth.cos(swing * speed + offset * boxIndex) * swingAmount * degree;
	}

	private float calculateChainOffset(double rootOffset, AdvancedModelBox... boxes) {
		return (float) (rootOffset * Math.PI / (2 * boxes.length));
	}

	public void progressRotation(AdvancedModelBox box, float progress, float rotX, float rotY, float rotZ, float divisor) {
		box.rotateAngleX += progress * (rotX - box.defaultRotationX) / divisor;
		box.rotateAngleY += progress * (rotY - box.defaultRotationY) / divisor;
		box.rotateAngleZ += progress * (rotZ - box.defaultRotationZ) / divisor;
	}

	public void progressRotationPrev(AdvancedModelBox box, float progress, float rotX, float rotY, float rotZ, float divisor) {
		box.rotateAngleX += progress * rotX / divisor;
		box.rotateAngleY += progress * rotY / divisor;
		box.rotateAngleZ += progress * rotZ / divisor;
	}

	public void progressPosition(AdvancedModelBox box, float progress, float x, float y, float z, float divisor) {
		box.rotationPointX += progress * (x - box.defaultPositionX) / divisor;
		box.rotationPointY += progress * (y - box.defaultPositionY) / divisor;
		box.rotationPointZ += progress * (z - box.defaultPositionZ) / divisor;
	}

	public void progressPositionPrev(AdvancedModelBox box, float progress, float x, float y, float z, float divisor) {
		box.rotationPointX += progress * x / divisor;
		box.rotationPointY += progress * y / divisor;
		box.rotationPointZ += progress * z / divisor;
	}

	public void rotate(ModelAnimator animator, AdvancedModelBox box, float x, float y, float z) {
		animator.rotate(box, (float) Math.toRadians(x), (float) Math.toRadians(y), (float) Math.toRadians(z));
	}

	public void rotateMinus(ModelAnimator animator, AdvancedModelBox box, float x, float y, float z) {
		animator.rotate(box, (float) Math.toRadians(x) - box.defaultRotationX, (float) Math.toRadians(y) - box.defaultRotationY,
			(float) Math.toRadians(z) - box.defaultRotationZ);
	}
}
