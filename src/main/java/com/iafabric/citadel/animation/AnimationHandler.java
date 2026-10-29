package com.iafabric.citadel.animation;

import com.iafabric.network.IafNetwork;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/** Ticks entity animations and tells the clients when one starts. */
public enum AnimationHandler {
	INSTANCE;

	private final Map<Entity, Animation> lastSent = new WeakHashMap<>();

	public <T extends Entity & IAnimatedEntity> void updateAnimations(T entity) {
		Animation animation = entity.getAnimation();
		if (animation == null) {
			entity.setAnimation(IAnimatedEntity.NO_ANIMATION);
			animation = IAnimatedEntity.NO_ANIMATION;
		}
		if (!entity.level().isClientSide) {
			Animation last = lastSent.get(entity);
			boolean restarted = animation != IAnimatedEntity.NO_ANIMATION && entity.getAnimationTick() == 0;
			if (last != animation || restarted) {
				lastSent.put(entity, animation);
				sendAnimationMessage(entity, animation);
			}
		}
		if (animation != IAnimatedEntity.NO_ANIMATION) {
			if (entity.getAnimationTick() == 0) {
				// first tick of this animation
			}
			entity.setAnimationTick(entity.getAnimationTick() + 1);
			if (entity.getAnimationTick() >= animation.getDuration()) {
				entity.setAnimationTick(0);
				entity.setAnimation(IAnimatedEntity.NO_ANIMATION);
				if (!entity.level().isClientSide) {
					lastSent.put(entity, IAnimatedEntity.NO_ANIMATION);
				}
			}
		}
	}

	public <T extends Entity & IAnimatedEntity> void sendAnimationMessage(T entity, Animation animation) {
		if (entity.level().isClientSide) {
			return;
		}
		int index = -1;
		Animation[] all = entity.getAnimations();
		for (int i = 0; i < all.length; i++) {
			if (all[i] == animation) {
				index = i;
				break;
			}
		}
		IafNetwork.sendAnimation(entity, index);
	}
}
