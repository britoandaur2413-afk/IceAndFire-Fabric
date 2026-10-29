package com.iafabric.citadel.animation;

/** An entity that plays {@link Animation}s, ticked by {@link AnimationHandler}. */
public interface IAnimatedEntity {
	Animation NO_ANIMATION = Animation.create(0);

	int getAnimationTick();

	void setAnimationTick(int tick);

	Animation getAnimation();

	void setAnimation(Animation animation);

	Animation[] getAnimations();
}
