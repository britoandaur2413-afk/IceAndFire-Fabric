package com.iafabric.boss;

import com.iafabric.entity.EntityDragonBase;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Black Frost walking an arc behind Cryona and back (before her duel in her realm): from her right, round her back, to
 * her left, turning at each end; its head on who watches. What leads it (her duel) moves it: no mind of its own, yet no
 * stiff body (it is no "NoAi" mob, whose model stands stiff). The arc is far enough from her for its wings (some 19
 * blocks from its back) to stay behind her.
 */
public final class Prowl {
	/** Its pace (blocks a tick), and how long a turn at an end takes. */
	private static final double PACE = 0.12;
	private static final int TURN = 30;
	/** The arc behind her, from her right to her left (0 is her right, 90 degrees straight behind her). */
	private final double from;
	private final double to;
	private double angle = Math.PI / 2;
	private int way = 1;
	private int turning;

	/** An arc behind her between these angles (in degrees: 0 her right, 90 straight behind her, 180 her left). */
	public Prowl(double fromDegrees, double toDegrees) {
		this.from = Math.toRadians(fromDegrees);
		this.to = Math.toRadians(toDegrees);
	}

	/** Where on its arc it is now: round her place, by the way she faces. */
	public Vec3 at(Vec3 her, float facing, double radius, double y) {
		return on(this.angle, her, facing, radius, y);
	}

	private static Vec3 on(double angle, Vec3 her, float facing, double radius, double y) {
		double f = Math.toRadians(facing);
		// her right, and behind her
		double rx = -Math.cos(f);
		double rz = -Math.sin(f);
		double bx = Math.sin(f);
		double bz = -Math.cos(f);
		double c = Math.cos(angle);
		double s = Math.sin(angle);
		return new Vec3(her.x + radius * (c * rx + s * bx), y, her.z + radius * (c * rz + s * bz));
	}

	/** One step along it (a turn at its ends): the dragon set there along its way, its head toward who watches. */
	public void step(EntityDragonBase dragon, Vec3 her, float facing, double radius, double y, @Nullable Entity watcher) {
		dragon.setNoAi(false);
		dragon.fightLed = true;
		dragon.setTarget(null);
		dragon.setInSittingPose(false);
		dragon.setFlying(false);
		dragon.setHovering(false);
		double before = this.angle;
		if (this.turning > 0) {
			this.turning--;
		} else {
			this.angle += this.way * PACE / radius;
			if (this.angle >= this.to || this.angle <= this.from) {
				this.angle = Mth.clamp(this.angle, this.from, this.to);
				this.way = -this.way;
				this.turning = TURN;
			}
		}
		Vec3 at = this.at(her, facing, radius, y);
		dragon.teleportTo(at.x, y, at.z);
		// along its way (toward its next step; turning at an end, round to face back)
		Vec3 next = on(this.angle + this.way * 0.05, her, facing, radius, y);
		float yaw = (float) Math.toDegrees(Math.atan2(-(next.x - at.x), next.z - at.z));
		float body = this.turning > 0 ? Mth.approachDegrees(dragon.getYRot(), yaw, 180F / TURN) : yaw;
		dragon.setYRot(body);
		dragon.yBodyRot = body;
		float head = body;
		if (watcher != null) {
			double dx = watcher.getX() - dragon.getX();
			double dz = watcher.getZ() - dragon.getZ();
			float want = (float) Math.toDegrees(Math.atan2(-dx, dz));
			head = body + Mth.clamp(Mth.wrapDegrees(want - body), -50F, 50F);
		}
		dragon.yHeadRot = Mth.approachDegrees(dragon.yHeadRot, head, 6F);
		if (this.angle == before && this.turning == 0) {
			dragon.setDeltaMovement(Vec3.ZERO);
		}
	}
}
