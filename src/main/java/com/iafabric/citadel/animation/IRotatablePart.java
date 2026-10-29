package com.iafabric.citadel.animation;

/** A model part that animation helpers on the entity side can turn (implemented by the client model boxes). */
public interface IRotatablePart {
	void addRotation(float x, float y, float z);
}
