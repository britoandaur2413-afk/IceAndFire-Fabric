package com.iafabric.citadel.client.model;

import net.minecraft.world.entity.Entity;

/** Poses a {@link TabulaModel} for an entity each frame. */
public interface ITabulaModelAnimator<T extends Entity> {
	void setRotationAngles(TabulaModel model, T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float rotationYaw,
		float rotationPitch, float scale);
}
