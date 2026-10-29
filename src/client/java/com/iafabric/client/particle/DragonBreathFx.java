package com.iafabric.client.particle;

import com.iafabric.entity.DragonSeatPose;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityIceDragon;
import com.iafabric.entity.EntityLightningDragon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

/**
 * A dragon's breath as the player's game draws it. Each tick it breathes, puffs (ParticleDragonBreath) are born right
 * in its mouth as its model is posed (the point between its jaws, kept by its renderer), leave it along the way its
 * jaws open (the line from their hinge to their tips: two points of the model itself) and turn toward where the
 * breath strikes (the point its server breathes at). Each is born at its moment within the tick, where the mouth was
 * then, so the stream leaves the mouth unbroken however fast it flies or turns its head. A few specks go with them:
 * embers, snowflakes, sparks.
 */
public final class DragonBreathFx {
	/** Its breath is seen while the server has said so this recently (it says so every tick it breathes). */
	private static final int SEEN = 2;

	private DragonBreathFx() {
	}

	public static void tick(EntityDragonBase dragon) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || dragon.tickCount - dragon.breathSeenTick > SEEN || dragon.isModelDead()) {
			dragon.mouthPrev = null;
			return;
		}
		int kind = kind(dragon);
		Vec3 target = new Vec3(dragon.burnParticleX, dragon.burnParticleY, dragon.burnParticleZ);
		Vec3 mouth = mouth(dragon);
		Vec3 dir;
		if (mouth != null) {
			dir = turned(dragon, dragon.mouthDir);
		} else {
			// not drawn lately (out of view): its old head point, toward where it strikes
			mouth = dragon.getHeadPosition();
			dir = target.subtract(mouth).normalize();
		}
		Vec3 prev = dragon.mouthPrev != null && dragon.mouthPrev.distanceToSqr(mouth) < 64 ? dragon.mouthPrev : mouth;
		dragon.mouthPrev = mouth;
		// its own speed: its breath leaves with it
		Vec3 moving = dragon.position().subtract(dragon.xo, dragon.yo, dragon.zo);
		RandomSource random = dragon.getRandom();
		// its mouth's width (three pixels of its model, at its size)
		float s = Mth.clamp(dragon.getRenderSize() / 16F, 0.3F, 4F);
		double dist = target.distanceTo(mouth);
		Minecraft mc = Minecraft.getInstance();
		if (kind == ParticleDragonBreath.LIGHTNING) {
			// the bolt is the breath (its renderer draws it from the mouth): balls of it run along it, and sparks
			for (int k = 0; k < 3; k++) {
				float into = (k + random.nextFloat()) / 3;
				int life = 10 + random.nextInt(5);
				double speed = Mth.clamp(dist / (life * 0.5), 0.6, 3.5);
				mc.particleEngine.add(new ParticleDragonBreath(level, kind, false, prev.lerp(mouth, into),
					cone(dir, 0.03F, random).scale(speed).add(moving), into, target, 0.35F, 0.9F * s, 1.3F * s, life));
			}
			for (int k = 0; k < 2; k++) {
				speck(level, kind, prev, mouth, dir, moving, dist, s, random);
			}
			return;
		}
		int life = 18 + random.nextInt(6);
		double speed = Mth.clamp(dist / (life * 0.45), 0.5, 3.0);
		// (its pictures grow from a spark to a cloud within the quad: the quad, a little more)
		float size0 = 1.5F * s;
		// enough of them that they touch from the mouth on (a spark there is a third of the quad), however fast they go
		int puffs = Mth.clamp(Mth.ceil(speed / (0.5F * s)), 3, 8);
		for (int k = 0; k < puffs; k++) {
			float into = (k + random.nextFloat()) / puffs;
			int own = life + random.nextInt(3) - 1;
			// (each a little apart within the gap of its jaws, and its own size: not a string of beads)
			Vec3 at = prev.lerp(mouth, into).add((random.nextFloat() - 0.5F) * 0.3F * s, (random.nextFloat() - 0.5F) * 0.3F * s, (random.nextFloat() - 0.5F) * 0.3F * s);
			float own0 = size0 * (0.85F + random.nextFloat() * 0.3F);
			mc.particleEngine.add(new ParticleDragonBreath(level, kind, false, at,
				cone(dir, kind == ParticleDragonBreath.ICE ? 0.08F : 0.06F, random).scale(speed * (0.85 + random.nextFloat() * 0.3)).add(moving),
				into, target, 0.18F, own0, (2.0F + random.nextFloat() * 0.6F) * s, own));
		}
		speck(level, kind, prev, mouth, dir, moving, dist, s, random);
	}

	/** An ember, a snowflake or a spark thrown from its mouth, off the stream a little, that does not seek the target. */
	private static void speck(ClientLevel level, int kind, Vec3 prev, Vec3 mouth, Vec3 dir, Vec3 moving, double dist, float s, RandomSource random) {
		float into = random.nextFloat();
		int life = 8 + random.nextInt(8);
		double speed = Mth.clamp(dist / (life * 0.6), 0.4, 2.5) * (0.6 + random.nextFloat() * 0.5);
		Minecraft.getInstance().particleEngine.add(new ParticleDragonBreath(level, kind, true, prev.lerp(mouth, into),
			cone(dir, 0.25F, random).scale(speed).add(moving), into, null, 0F, 0.3F * s, 0.4F * s, life));
	}

	/** A baby's try at it: a little puff from its mouth that comes to nothing. */
	public static void cough(EntityDragonBase dragon) {
		ClientLevel level = Minecraft.getInstance().level;
		Vec3 mouth = mouth(dragon);
		if (level == null || mouth == null) {
			return;
		}
		Vec3 dir = turned(dragon, dragon.mouthDir);
		RandomSource random = dragon.getRandom();
		float s = Mth.clamp(dragon.getRenderSize() / 16F, 0.15F, 1F);
		for (int k = 0; k < 4; k++) {
			Minecraft.getInstance().particleEngine.add(new ParticleDragonBreath(level, kind(dragon), false, mouth,
				cone(dir, 0.2F, random).scale(0.08 + random.nextFloat() * 0.1), 0F, null, 0F, 0.9F * s, 1.3F * s, 8 + random.nextInt(5)));
		}
	}

	private static int kind(EntityDragonBase dragon) {
		return dragon instanceof EntityIceDragon ? ParticleDragonBreath.ICE : dragon instanceof EntityLightningDragon ? ParticleDragonBreath.LIGHTNING
			: ParticleDragonBreath.FIRE;
	}

	/** Its mouth in the world now (the end of this tick): as last drawn, turned with its body, where it is. */
	public static @Nullable Vec3 mouth(EntityDragonBase dragon) {
		if (dragon.mouthAt == null || dragon.mouthDir == null || dragon.tickCount - dragon.mouthTick > 10) {
			return null;
		}
		return DragonSeatPose.onIt(dragon, dragon.mouthAt);
	}

	/** The way its jaws open, in the world now (from their hinge to their tips, as last drawn). */
	public static @Nullable Vec3 mouthWay(EntityDragonBase dragon) {
		return mouth(dragon) == null ? null : turned(dragon, dragon.mouthDir);
	}

	/** Its mouth in the world as it is drawn this frame (what is drawn with it starts there: the lightning it breathes). */
	public static @Nullable Vec3 mouth(EntityDragonBase dragon, float partialTicks) {
		if (dragon.mouthAt == null || dragon.tickCount - dragon.mouthTick > 10) {
			return null;
		}
		float bodyRot = Mth.rotLerp(partialTicks, dragon.yBodyRotO, dragon.yBodyRot);
		Vector3f at = new Quaternionf().rotateY((float) Math.toRadians(180.0F - bodyRot))
			.transform(new Vector3f((float) dragon.mouthAt.x, (float) dragon.mouthAt.y, (float) dragon.mouthAt.z));
		return new Vec3(Mth.lerp(partialTicks, dragon.xOld, dragon.getX()) + at.x(), Mth.lerp(partialTicks, dragon.yOld, dragon.getY()) + at.y(),
			Mth.lerp(partialTicks, dragon.zOld, dragon.getZ()) + at.z());
	}

	/**
	 * How far from where it is a ball a dragon shoots is drawn: its server shoots it from a point near the dragon's head;
	 * it is drawn leaving the dragon's mouth (as drawn) and on its own way within a few ticks.
	 */
	public static Vec3 chargeShift(com.iafabric.entity.EntityDragonCharge charge, float partialTicks) {
		float now = charge.tickCount + partialTicks;
		if (charge.fromMouth == null) {
			charge.fromMouth = Vec3.ZERO;
			charge.fromMouthAt = now;
			if (charge.tickCount < 4 && charge.getOwner() instanceof EntityDragonBase dragon) {
				Vec3 mouth = mouth(dragon, partialTicks);
				if (mouth != null) {
					charge.fromMouth = mouth.subtract(Mth.lerp(partialTicks, charge.xOld, charge.getX()), Mth.lerp(partialTicks, charge.yOld, charge.getY()),
						Mth.lerp(partialTicks, charge.zOld, charge.getZ()));
				}
			}
		}
		float left = 1.0F - Mth.clamp((now - charge.fromMouthAt) / 5.0F, 0.0F, 1.0F);
		return left <= 0 ? Vec3.ZERO : charge.fromMouth.scale(left);
	}

	/** A way in its own frame (before its turn), in the world: turned with its body. */
	private static Vec3 turned(EntityDragonBase dragon, Vec3 local) {
		Vector3f v = new Quaternionf().rotateY((float) Math.toRadians(180.0F - dragon.yBodyRot))
			.transform(new Vector3f((float) local.x, (float) local.y, (float) local.z));
		return new Vec3(v.x(), v.y(), v.z());
	}

	/** A way a little off this one, at random: within this many radians of it. */
	private static Vec3 cone(Vec3 dir, float spread, RandomSource random) {
		return dir.add((random.nextFloat() - 0.5F) * 2 * spread, (random.nextFloat() - 0.5F) * 2 * spread, (random.nextFloat() - 0.5F) * 2 * spread).normalize();
	}
}
