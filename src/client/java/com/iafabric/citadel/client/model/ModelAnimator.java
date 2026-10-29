package com.iafabric.citadel.client.model;

import com.iafabric.citadel.animation.Animation;
import com.iafabric.citadel.animation.IAnimatedEntity;
import com.iafabric.citadel.client.render.ModelRendering;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/** Keyframe animator for {@link IAnimatedEntity} animations (replaces Citadel's ModelAnimator). */
public class ModelAnimator {
	private int tempTick;
	private int prevTempTick;
	private boolean correctAnimation;
	private IAnimatedEntity entity;
	private final Map<AdvancedModelBox, Transform> transformMap = new HashMap<>();
	private final Map<AdvancedModelBox, Transform> prevTransformMap = new HashMap<>();

	public static ModelAnimator create() {
		return new ModelAnimator();
	}

	public IAnimatedEntity getEntity() {
		return entity;
	}

	public void update(IAnimatedEntity entity) {
		this.tempTick = this.prevTempTick = 0;
		this.correctAnimation = false;
		this.entity = entity;
		this.transformMap.clear();
		this.prevTransformMap.clear();
	}

	public boolean setAnimation(Animation animation) {
		this.tempTick = this.prevTempTick = 0;
		this.correctAnimation = this.entity != null && this.entity.getAnimation() == animation;
		return this.correctAnimation;
	}

	public void startKeyframe(int duration) {
		if (!this.correctAnimation) {
			return;
		}
		this.prevTempTick = this.tempTick;
		this.tempTick += duration;
	}

	public void setStaticKeyframe(int duration) {
		startKeyframe(duration);
		endKeyframe(true);
	}

	public void resetKeyframe(int duration) {
		startKeyframe(duration);
		endKeyframe();
	}

	public void rotate(AdvancedModelBox box, float x, float y, float z) {
		if (!this.correctAnimation) {
			return;
		}
		getTransform(box).addRotation(x, y, z);
	}

	public void move(AdvancedModelBox box, float x, float y, float z) {
		if (!this.correctAnimation) {
			return;
		}
		getTransform(box).addOffset(x, y, z);
	}

	private Transform getTransform(AdvancedModelBox box) {
		return this.transformMap.computeIfAbsent(box, b -> new Transform());
	}

	public void endKeyframe() {
		endKeyframe(false);
	}

	private void endKeyframe(boolean stationary) {
		if (!this.correctAnimation) {
			return;
		}
		int animationTick = this.entity.getAnimationTick();
		if (animationTick >= this.prevTempTick && animationTick < this.tempTick) {
			if (stationary) {
				for (Map.Entry<AdvancedModelBox, Transform> e : this.prevTransformMap.entrySet()) {
					e.getValue().apply(e.getKey(), 1.0F);
				}
			} else {
				float tick = (animationTick - this.prevTempTick + ModelRendering.partialTick()) / (this.tempTick - this.prevTempTick);
				float inc = Mth.sin((float) (tick * Math.PI / 2.0F));
				float dec = 1.0F - inc;
				for (Map.Entry<AdvancedModelBox, Transform> e : this.prevTransformMap.entrySet()) {
					e.getValue().apply(e.getKey(), dec);
				}
				for (Map.Entry<AdvancedModelBox, Transform> e : this.transformMap.entrySet()) {
					e.getValue().apply(e.getKey(), inc);
				}
			}
		}
		if (!stationary) {
			this.prevTransformMap.clear();
			this.prevTransformMap.putAll(this.transformMap);
			this.transformMap.clear();
		}
	}

	private static final class Transform {
		float rotationX, rotationY, rotationZ, offsetX, offsetY, offsetZ;

		void addRotation(float x, float y, float z) {
			rotationX += x;
			rotationY += y;
			rotationZ += z;
		}

		void addOffset(float x, float y, float z) {
			offsetX += x;
			offsetY += y;
			offsetZ += z;
		}

		void apply(AdvancedModelBox box, float amount) {
			box.rotateAngleX += amount * rotationX;
			box.rotateAngleY += amount * rotationY;
			box.rotateAngleZ += amount * rotationZ;
			box.rotationPointX += amount * offsetX;
			box.rotationPointY += amount * offsetY;
			box.rotationPointZ += amount * offsetZ;
		}
	}
}
