package com.iafabric.entity;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The seat on a dragon's back: the top of its back bone (BodyUpper, the root of its body in the three kinds), on the back
 * half of it. The player's game draws the riders there as the bone is posed every frame (RenderDragonBase); here, for the
 * server (where the riders are, what strikes them, what they see from), the bone as the poses of its model put it: its
 * height and its tilt in each (on the ground, dead, asleep, hovering, flying, sitting, tackling, diving), blended by how
 * far into each the dragon is, in the order and the way its animator blends them (DragonTabulaModelAnimator), then its
 * tilt in the air and its size. Only the small beat of its body is left out.
 */
public final class DragonSeatPose {
	/** The seat on the bone, in model units: its top (the box's top is at -1.8), on its back half (0 to 7). */
	public static final float SEAT_Y = -1.8F, SEAT_Z = 5.5F;
	/** Where a rider's seat is over its feet (a player's, an illager's, Cryona's). */
	public static final float RIDER_SEAT = 0.6F;

	// the bone in each pose of the three kinds (fire, ice, lightning): its height (model units) and its tilt (degrees),
	// from their tabula models (it stands 10 units ahead of the middle in all of them)
	private static final float[][] GROUND = {{11.6F, 5.22F}, {11.6F, 5.22F}, {11.6F, 5.22F}};
	private static final float[][] DEAD = {{20.6F, 13.04F}, {20.6F, 5.22F}, {18.7F, 2.22F}};
	private static final float[][] SLEEP = {{17.6F, 13.04F}, {17.6F, 13.04F}, {10.6F, -28.7F}};
	private static final float[][] HOVER = {{10.6F, -10.43F}, {10.6F, -10.78F}, {10.6F, -10.78F}};
	private static final float[][] FLY = {{10.6F, 5.22F}, {10.6F, 5.22F}, {10.6F, 5.22F}};
	private static final float[][] SIT = {{12.8F, -9.78F}, {12.5F, -9.78F}, {11.6F, -15.78F}};
	private static final float[][] TACKLE = {{10.6F, -15.65F}, {10.6F, -15.78F}, {10.6F, 3.0F}};
	private static final float[][] DIVE = {{10.6F, 5.22F}, {11.6F, 5.22F}, {12.6F, 5.22F}};

	private DragonSeatPose() {
	}

	/** The seat's height on the bone: its top, or the cushion of a double saddle's front seat on it (DragonSaddle). */
	public static float seatY(EntityDragonBase dragon) {
		return DragonSaddle.worn(dragon) ? DragonSaddle.UPPER_TOP - DragonSaddle.CUSHION : SEAT_Y;
	}

	/** The seat in the dragon's own frame (before its turn): for its poses now, its tilt in the air and its size. */
	public static Vec3 local(EntityDragonBase dragon, float pitch) {
		int kind = dragon instanceof EntityIceDragon ? 1 : dragon instanceof EntityLightningDragon ? 2 : 0;
		float[] bone = {GROUND[kind][0], GROUND[kind][1]};
		if (dragon.modelDeadProgress > 0) {
			toward(bone, DEAD[kind], dragon.modelDeadProgress, 20);
		}
		if (dragon.sleepProgress > 0) {
			toward(bone, SLEEP[kind], dragon.sleepProgress, 20);
		}
		if (dragon.hoverProgress > 0) {
			toward(bone, HOVER[kind], dragon.hoverProgress, 20);
		}
		if (dragon.flyProgress > 0) {
			toward(bone, FLY[kind], dragon.flyProgress - dragon.diveProgress * 2, 20);
		}
		if (dragon.sitProgress > 0) {
			toward(bone, SIT[kind], dragon.sitProgress, 20);
		}
		if (dragon.tackleProgress > 0) {
			toward(bone, TACKLE[kind], dragon.tackleProgress, 5);
		}
		if (dragon.diveProgress > 0) {
			toward(bone, DIVE[kind], dragon.diveProgress, 10);
		}
		// the seat on the bone, turned with it, from its place (the model's y is down; it is drawn 1.501 lower)
		double tilt = Math.toRadians(bone[1]);
		double sy = seatY(dragon) / 16.0, sz = SEAT_Z / 16.0;
		double y = sy * Math.cos(tilt) - sz * Math.sin(tilt) + bone[0] / 16.0 - 1.501;
		double z = sy * Math.sin(tilt) + sz * Math.cos(tilt) - 10.0 / 16.0;
		// its tilt in the air and its size, as its renderer draws it; then upright (the model is drawn upside down)
		double p = Math.toRadians(pitch);
		double ty = y * Math.cos(p) - z * Math.sin(p);
		double tz = y * Math.sin(p) + z * Math.cos(p);
		float size = dragon.getRenderSize() / 3F;
		return new Vec3(0, -ty * size, tz * size);
	}

	/** The seat in the world: on the dragon where it is, turned with its body. */
	public static Vec3 world(EntityDragonBase dragon) {
		return onIt(dragon, local(dragon, dragon.getDragonPitch()));
	}

	/** A point in the dragon's own frame (before its turn), in the world: turned with its body, where it is. */
	public static Vec3 onIt(EntityDragonBase dragon, Vec3 local) {
		Vector3f at = new Quaternionf().rotateY((float) Math.toRadians(180.0F - dragon.yBodyRot))
			.transform(new Vector3f((float) local.x, (float) local.y, (float) local.z));
		return dragon.position().add(at.x(), at.y(), at.z());
	}

	/** As its animator moves a part toward a pose: by a share of the way for how far into it the dragon is. */
	private static void toward(float[] bone, float[] pose, float timer, float max) {
		bone[0] += (pose[0] - bone[0]) / max * timer;
		bone[1] += (pose[1] - bone[1]) / max * timer;
	}
}
