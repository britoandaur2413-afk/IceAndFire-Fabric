package com.iafabric.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iafabric.IafMod;
import com.iafabric.citadel.animation.Animation;
import com.iafabric.util.IAFMath;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A dragon's model as its renderer poses it, worked out alike on the server and on the players' games: its bones from its
 * tabula models (its pose on the ground and the poses it moves into: dead, asleep, hovering, flying, sitting, tackling,
 * diving, breathing, its wing beats, its steps and its attacks), blended as its animator blends them
 * (DragonTabulaModelAnimator), its neck turned to where it looks, then tilted, sized and turned as it is drawn. Its body
 * parts (EntityDragonPart) are boxes around these bones as they are posed each tick: its head, its body, its hind legs,
 * each piece of its neck and of its tail, and each wing in six (cut in three along it, each piece in two across), so they
 * hold to it on the ground, in the air and as its wings beat. Only the small sways its animator adds (its breathing, its
 * neck and tail swaying, its feet on a slope) are left out.
 */
public final class DragonBones {
	public static final int HEAD = 0, BODY = 1, LEGS = 2, NECK = 3, TAIL = 6, WING_RIGHT = 12, WING_LEFT = 18, PARTS = 24;
	/** Each wing in three along it, each piece in two across it. */
	private static final int ALONG = 3, ACROSS = 2;
	/** How much of a blow on each part reaches the dragon. */
	private static final float[] DAMAGE = new float[PARTS];

	static {
		for (int i = 0; i < PARTS; i++) {
			DAMAGE[i] = i >= WING_RIGHT ? 0.5F : 1.0F;
		}
		DAMAGE[HEAD] = 1.5F;
		DAMAGE[TAIL + 3] = DAMAGE[TAIL + 4] = DAMAGE[TAIL + 5] = 1.5F;
	}

	private static final String[] KINDS = {"fire", "ice", "lightning"};
	private static final Skeleton[] SKELETONS = new Skeleton[3];
	private static final boolean[] FAILED = new boolean[3];

	private DragonBones() {
	}

	public static float damage(int part) {
		return DAMAGE[part];
	}

	/** A player within this far of a dragon (and its size) has its parts' boxes worked out anew each tick. */
	private static final double NEAR = 128;

	/**
	 * Its parts' boxes for this tick (each as min x, y, z, max x, y, z from where it is), worked out once a tick; with no
	 * player near (none to see them or strike them) they keep the last pose, carried along with it.
	 */
	public static float @Nullable [] boxes(EntityDragonBase dragon) {
		Work work = dragon.bones;
		if (work.tick == dragon.tickCount && work.done) {
			return work.boxes;
		}
		if (work.done && !playerNear(dragon)) {
			work.tick = dragon.tickCount;
			return work.boxes;
		}
		Skeleton skeleton = skeleton(dragon instanceof EntityIceDragon ? 1 : dragon instanceof EntityLightningDragon ? 2 : 0);
		if (skeleton == null) {
			return null;
		}
		work.tick = dragon.tickCount;
		work.prepare(skeleton);
		pose(dragon, skeleton, work);
		place(dragon, skeleton, work);
		fit(dragon, skeleton, work);
		work.done = true;
		return work.boxes;
	}

	private static boolean playerNear(EntityDragonBase dragon) {
		double near = NEAR + dragon.getRenderSize() * 2;
		for (net.minecraft.world.entity.player.Player player : dragon.level().players()) {
			if (!player.isSpectator() && player.distanceToSqr(dragon) < near * near) {
				return true;
			}
		}
		return false;
	}

	/** A point of one of its bones (in model units, in the bone's own frame) where it is now in the world, from its feet. */
	public static @Nullable Vector3f point(EntityDragonBase dragon, String bone, float x, float y, float z) {
		if (boxes(dragon) == null) {
			return null;
		}
		Integer at = dragon.bones.skeleton.index.get(bone);
		return at == null ? null : dragon.bones.mats[at].transformPosition(x / 16F, y / 16F, z / 16F, new Vector3f());
	}

	/** The middle of one of its bones' boxes where it is now, from its feet. */
	public static @Nullable Vector3f middle(EntityDragonBase dragon, String bone) {
		if (boxes(dragon) == null) {
			return null;
		}
		Integer at = dragon.bones.skeleton.index.get(bone);
		if (at == null) {
			return null;
		}
		float[] cube = dragon.bones.skeleton.cube[at];
		return point(dragon, bone, cube[0] + cube[3] / 2, cube[1] + cube[4] / 2, cube[2] + cube[5] / 2);
	}

	/**
	 * How far its posed model reaches as a screen draws it (turned by some degrees from facing the viewer, as
	 * InventoryScreen turns a mob toward the mouse), from its feet, in blocks: its left, its bottom, its right, its top.
	 * Its own turn is taken off; its pose, tilt and size are as drawn.
	 */
	public static float @Nullable [] inView(EntityDragonBase dragon, float turn) {
		if (boxes(dragon) == null) {
			return null;
		}
		Work w = dragon.bones;
		Skeleton s = w.skeleton;
		// (its bones are placed turned with its body: that turn off, the screen's on)
		Matrix4f view = new Matrix4f().rotateY((float) Math.toRadians(-turn)).rotateY((float) Math.toRadians(dragon.yBodyRot - 180.0F));
		Matrix4f bone = new Matrix4f();
		Vector3f at = new Vector3f();
		float[] reach = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		for (int i = 0; i < s.count; i++) {
			float[] c = s.cube[i];
			view.mul(w.mats[i], bone);
			for (int k = 0; k < 8; k++) {
				bone.transformPosition((c[0] + ((k & 1) == 0 ? 0 : c[3])) / 16F, (c[1] + ((k & 2) == 0 ? 0 : c[4])) / 16F,
					(c[2] + ((k & 4) == 0 ? 0 : c[5])) / 16F, at);
				reach[0] = Math.min(reach[0], at.x);
				reach[1] = Math.min(reach[1], at.y);
				reach[2] = Math.max(reach[2], at.x);
				reach[3] = Math.max(reach[3], at.y);
			}
		}
		return reach;
	}

	// ----- its pose -----

	private static void pose(EntityDragonBase dragon, Skeleton s, Work w) {
		Model base = s.base;
		int n = s.count;
		System.arraycopy(base.rx, 0, w.rx, 0, n);
		System.arraycopy(base.ry, 0, w.ry, 0, n);
		System.arraycopy(base.rz, 0, w.rz, 0, n);
		System.arraycopy(base.px, 0, w.px, 0, n);
		System.arraycopy(base.py, 0, w.py, 0, n);
		System.arraycopy(base.pz, 0, w.pz, 0, n);
		animate(dragon, s, w);

		boolean walking = !dragon.isHovering() && !dragon.isFlying() && dragon.hoverProgress <= 0 && dragon.flyProgress <= 0;
		boolean swimming = s.swim != null && dragon.isInWater() && dragon.swimProgress > 0;
		Model[] cycle = swimming ? s.swim : walking ? s.walk : s.flight;
		int index = swimming ? dragon.swimCycle / 10 : walking ? dragon.walkCycle / 10 : dragon.flightCycle / 10;
		index = Mth.clamp(index, 0, cycle.length - 1);
		int prevIndex = index - 1 < 0 ? cycle.length - 1 : index - 1;
		Model current = cycle[index];
		Model previous = cycle[prevIndex];
		float delta = ((swimming ? dragon.swimCycle : walking ? dragon.walkCycle : dragon.flightCycle) / 10.0F) % 1.0F;
		float limb = !dragon.isPassenger() && dragon.isAlive() ? Math.min(dragon.walkAnimation.speed(), 1.0F) : 0.0F;
		boolean male = dragon.isMale();
		// (its wings keep out of its steps while they blast or it roars aloud)
		boolean wingsBusy = dragon.getAnimation() == EntityDragonBase.ANIMATION_WINGBLAST || dragon.getAnimation() == EntityDragonBase.ANIMATION_EPIC_ROAR;
		float breath = dragon.fireBreathProgress;
		boolean breathRising = dragon.prevFireBreathProgress <= dragon.fireBreathProgress;

		for (int i = 0; i < n; i++) {
			if (!male && s.female.has[i] && s.male.has[i]
				&& (s.female.rx[i] != s.male.rx[i] || s.female.ry[i] != s.male.ry[i] || s.female.rz[i] != s.male.rz[i])) {
				w.rx[i] += distance(w.rx[i], s.female.rx[i]);
				w.ry[i] += distance(w.ry[i], s.female.ry[i]);
				w.rz[i] += distance(w.rz[i], s.female.rz[i]);
			}
			if (walking && dragon.flyProgress <= 0 && dragon.hoverProgress <= 0 && dragon.modelDeadProgress <= 0 && previous.has[i] && current.has[i]) {
				float k = Math.min(limb * 2, 1);
				if (!s.horn[i] && !(s.wing[i] && wingsBusy)) {
					w.rx[i] += k * distance(base.rx[i], previous.rx[i] + delta * distance(previous.rx[i], current.rx[i]));
					w.ry[i] += k * distance(base.ry[i], previous.ry[i] + delta * distance(previous.ry[i], current.ry[i]));
					w.rz[i] += k * distance(base.rz[i], previous.rz[i] + delta * distance(previous.rz[i], current.rz[i]));
				}
			}
			if (dragon.modelDeadProgress > 0 && !same(s, i, s.dead)) {
				toward(w, i, s.dead, dragon.modelDeadProgress, 20, s.thigh[i]);
			}
			if (dragon.modelDeadProgress > 0 && s.kind == 1 && i == s.bodyUpper) {
				// (as its animator does: a dead ice dragon is set down onto the ground)
				w.py[i] += 0.35F * dragon.modelDeadProgress;
			}
			if (dragon.sleepProgress > 0 && !same(s, i, s.sleeping)) {
				toward(w, i, s.sleeping, dragon.sleepProgress, 20, false);
			}
			if (dragon.hoverProgress > 0 && !same(s, i, s.hovering) && !s.wing[i] && !s.tail[i]) {
				toward(w, i, s.hovering, dragon.hoverProgress, 20, false);
			}
			if (dragon.flyProgress > 0 && !same(s, i, s.flying)) {
				toward(w, i, s.flying, dragon.flyProgress - dragon.diveProgress * 2, 20, false);
			}
			if (dragon.sitProgress > 0 && !dragon.isPassenger() && !same(s, i, s.sitting)) {
				toward(w, i, s.sitting, dragon.sitProgress, 20, false);
			}
			if (dragon.tackleProgress > 0 && !s.wing[i] && s.tackle.has[i] && s.flying.has[i]
				&& !(s.flying.rx[i] == s.tackle.rx[i] && s.flying.ry[i] == s.tackle.ry[i] && s.flying.rz[i] == s.tackle.rz[i])) {
				toward(w, i, s.tackle, dragon.tackleProgress, 5, false);
			}
			if (dragon.diveProgress > 0 && !same(s, i, s.diving)) {
				toward(w, i, s.diving, dragon.diveProgress, 10, false);
			}
			if (breath > 0 && !same(s, i, s.stream) && !s.wing[i]) {
				if (breathRising) {
					toward(w, i, s.charge, Mth.clamp(breath, 0, 5), 5, false);
				}
				toward(w, i, s.stream, Mth.clamp(breath - 5, 0, 5), 5, false);
			}
			if (!walking && previous.has[i] && current.has[i] && s.flying.has[i]
				&& (current.rx[i] != s.flying.rx[i] || current.ry[i] != s.flying.ry[i] || current.rz[i] != s.flying.rz[i])) {
				w.rx[i] += distance(w.rx[i], previous.rx[i] + delta * distance(previous.rx[i], current.rx[i]));
				w.ry[i] += distance(w.ry[i], previous.ry[i] + delta * distance(previous.ry[i], current.ry[i]));
				w.rz[i] += distance(w.rz[i], previous.rz[i] + delta * distance(previous.rz[i], current.rz[i]));
			}
		}
		if (!dragon.isNoAi()) {
			// its neck toward where it looks (as its model's faceTarget: half the turn, shared along it)
			float yaw = (float) Math.toRadians(dragon.yHeadRot - dragon.yBodyRot) / (2 * s.neck.length);
			float pitch = (float) Math.toRadians(dragon.getXRot()) / (2 * s.neck.length);
			for (int i : s.neck) {
				w.ry[i] += yaw;
				w.rx[i] += pitch;
			}
		}
		if (dragon.isBlackFrost() && !dragon.isModelDead() && dragon.flyProgress <= 0 && dragon.hoverProgress <= 0 && dragon.sitProgress <= 0
			&& dragon.sleepProgress <= 0) {
			for (int i : s.neck) {
				w.rx[i] = base.rx[i] + Mth.clamp(w.rx[i] - base.rx[i], -0.2F, 0.05F);
			}
		}
	}

	/** Whether this pose leaves this bone as it is on the ground (its animator then does not move it toward it). */
	private static boolean same(Skeleton s, int i, Model pose) {
		return !pose.has[i] || pose.rx[i] == s.base.rx[i] && pose.ry[i] == s.base.ry[i] && pose.rz[i] == s.base.rz[i];
	}

	/** As its animator moves a bone toward a pose: a share of the way for how far into it the dragon is. */
	private static void toward(Work w, int i, Model pose, float timer, float max, boolean oldFashioned) {
		if (!pose.has[i]) {
			return;
		}
		if (oldFashioned) {
			w.rx[i] += (pose.rx[i] - w.rx[i]) / max * timer;
			w.ry[i] += (pose.ry[i] - w.ry[i]) / max * timer;
			w.rz[i] += (pose.rz[i] - w.rz[i]) / max * timer;
		} else {
			w.rx[i] += distance(w.rx[i], pose.rx[i]) / max * timer;
			w.ry[i] += distance(w.ry[i], pose.ry[i]) / max * timer;
			w.rz[i] += distance(w.rz[i], pose.rz[i]) / max * timer;
		}
		w.px[i] += (pose.px[i] - w.px[i]) / max * timer;
		w.py[i] += (pose.py[i] - w.py[i]) / max * timer;
		w.pz[i] += (pose.pz[i] - w.pz[i]) / max * timer;
	}

	private static float distance(float from, float to) {
		return (float) IAFMath.atan2_accurate(Mth.sin(to - from), Mth.cos(to - from));
	}

	// ----- its attacks: its animator's keyframes -----

	/** One keyframe: so many ticks into a pose (none: back to its own), its body moved down or turned with it. */
	private record Key(int ticks, @Nullable String pose, float bodyDown, float bodyTurn) {
	}

	private static Key key(int ticks, String pose) {
		return new Key(ticks, pose, 0, 0);
	}

	private static Key back(int ticks) {
		return new Key(ticks, null, 0, 0);
	}

	private static final Map<String, Key[]> ATTACKS = new HashMap<>();

	static {
		ATTACKS.put("firecharge", new Key[]{key(10, "AttackBlastCharge1"), key(10, "AttackBlastCharge2"), key(5, "AttackBlastCharge3"), back(5)});
		ATTACKS.put("bite", new Key[]{key(10, "Bite1"), key(5, "Bite2"), key(5, "Bite3"), back(10)});
		ATTACKS.put("shakeprey", new Key[]{key(15, "Grab1"), key(10, "Grab2"), key(10, "GrabShake1"), key(10, "GrabShake2"), key(10, "GrabShake3"), back(10)});
		ATTACKS.put("tailwhack", new Key[]{key(10, "Tail1"), key(10, "Tail2"), key(10, "Tail3"), back(10)});
		ATTACKS.put("wingblast", new Key[]{new Key(5, "WingBlast1", 0, 0), new Key(5, "WingBlast2", -2F, 0), new Key(5, "WingBlast3", -4F, 0),
			new Key(5, "WingBlast4", -4F, 0), new Key(5, "WingBlast5", -4F, 0), new Key(5, "WingBlast6", -4F, 0), new Key(5, "WingBlast7", -2F, 0), back(10)});
		ATTACKS.put("roar", new Key[]{key(10, "Roar1"), key(10, "Roar2"), key(10, "Roar3"), back(10)});
		ATTACKS.put("epicroar", new Key[]{new Key(10, "EpicRoar1", 0, -0.1F), new Key(10, "EpicRoar2", 0, -0.2F), new Key(10, "EpicRoar3", 0, -0.2F),
			new Key(10, "EpicRoar2", 0, -0.2F), new Key(10, "EpicRoar3", 0, -0.1F), back(10)});
	}

	/** Its attack as its animator draws it now: between the keyframe before and this one, eased in. */
	private static void animate(EntityDragonBase dragon, Skeleton s, Work w) {
		Animation animation = dragon.getAnimation();
		Key[] keys = animation == null ? null
			: animation == EntityDragonBase.ANIMATION_FIRECHARGE ? ATTACKS.get("firecharge")
			: animation == EntityDragonBase.ANIMATION_BITE ? ATTACKS.get("bite")
			: animation == EntityDragonBase.ANIMATION_SHAKEPREY ? ATTACKS.get("shakeprey")
			: animation == EntityDragonBase.ANIMATION_TAILWHACK ? ATTACKS.get("tailwhack")
			: animation == EntityDragonBase.ANIMATION_WINGBLAST ? ATTACKS.get("wingblast")
			: animation == EntityDragonBase.ANIMATION_ROAR ? ATTACKS.get("roar")
			: animation == EntityDragonBase.ANIMATION_EPIC_ROAR ? ATTACKS.get("epicroar") : null;
		if (keys == null) {
			return;
		}
		int tick = dragon.getAnimationTick();
		int start = 0;
		Key previous = null;
		for (Key key : keys) {
			int end = start + key.ticks();
			if (tick >= start && tick < end) {
				float into = Mth.sin((float) ((tick - start) / (float) key.ticks() * Math.PI / 2.0F));
				apply(s, w, previous, 1 - into);
				apply(s, w, key, into);
				return;
			}
			previous = key;
			start = end;
		}
	}

	/** A keyframe's share: each bone the way from its own pose to the keyframe's (and its body's move and turn). */
	private static void apply(Skeleton s, Work w, @Nullable Key key, float amount) {
		if (key == null || amount == 0) {
			return;
		}
		Model base = s.base;
		Model pose = key.pose() == null ? null : s.poses.get(key.pose());
		if (pose != null) {
			for (int i = 0; i < s.count; i++) {
				if (!pose.has[i]) {
					continue;
				}
				if (!(pose.rx[i] == base.rx[i] && pose.ry[i] == base.ry[i] && pose.rz[i] == base.rz[i])) {
					w.rx[i] += amount * distance(base.rx[i], pose.rx[i]);
					w.ry[i] += amount * distance(base.ry[i], pose.ry[i]);
					w.rz[i] += amount * distance(base.rz[i], pose.rz[i]);
				}
				if (!(pose.px[i] == base.px[i] && pose.py[i] == base.py[i] && pose.pz[i] == base.pz[i])) {
					w.px[i] += amount * (pose.px[i] - base.px[i]);
					w.py[i] += amount * (pose.py[i] - base.py[i]);
					w.pz[i] += amount * (pose.pz[i] - base.pz[i]);
				}
			}
		}
		if (s.bodyUpper >= 0) {
			w.py[s.bodyUpper] += amount * key.bodyDown();
			w.rx[s.bodyUpper] += amount * key.bodyTurn();
		}
	}

	// ----- where its bones are -----

	/** Each bone as it is drawn: turned with its body, upside down, tilted and sized as its renderer draws it. */
	private static void place(EntityDragonBase dragon, Skeleton s, Work w) {
		float size = dragon.getRenderSize() / 3F;
		Matrix4f root = w.root.identity()
			.rotateY((float) Math.toRadians(180.0F - dragon.yBodyRot))
			.scale(-1.0F, -1.0F, 1.0F)
			.rotateX((float) Math.toRadians(dragon.getDragonPitch()))
			.scale(size)
			.translate(0.0F, -1.501F, 0.0F);
		for (int i = 0; i < s.count; i++) {
			Matrix4f m = w.mats[i].set(s.parent[i] < 0 ? root : w.mats[s.parent[i]])
				.translate(w.px[i] / 16.0F, w.py[i] / 16.0F, w.pz[i] / 16.0F);
			if (w.rz[i] != 0) {
				m.rotateZ(w.rz[i]);
			}
			if (w.ry[i] != 0) {
				m.rotateY(w.ry[i]);
			}
			if (w.rx[i] != 0) {
				m.rotateX(w.rx[i]);
			}
			if (s.sx[i] != 1 || s.sy[i] != 1 || s.sz[i] != 1) {
				m.scale(s.sx[i], s.sy[i], s.sz[i]);
			}
		}
	}

	// ----- its parts around them -----

	private static void fit(EntityDragonBase dragon, Skeleton s, Work w) {
		for (int part = 0; part < WING_RIGHT; part++) {
			int count = gather(s, w, s.parts[part]);
			empty(w.boxes, part);
			for (int p = 0; p < count; p++) {
				grow(w.boxes, part, w.pts[p * 3], w.pts[p * 3 + 1], w.pts[p * 3 + 2]);
			}
			settle(w.boxes, part);
		}
		// (a point near a cut is in the pieces on both sides of it: no gap opens between them)
		float margin = 1.5F / 16F * dragon.getRenderSize() / 3F;
		for (int side = 0; side < 2; side++) {
			int count = gather(s, w, s.wings[side]);
			int first = side == 0 ? WING_RIGHT : WING_LEFT;
			// along it: from its shoulder toward its middle, the way it spreads most
			Vector3f shoulder = w.mats[s.wingRoot[side]].transformPosition(0, 0, 0, w.v);
			float sx = shoulder.x, sy = shoulder.y, sz = shoulder.z;
			float mx = 0, my = 0, mz = 0;
			for (int p = 0; p < count; p++) {
				w.all[p] = p;
				mx += w.pts[p * 3];
				my += w.pts[p * 3 + 1];
				mz += w.pts[p * 3 + 2];
			}
			if (count > 0) {
				mx /= count;
				my /= count;
				mz /= count;
			}
			axis(w, w.all, count, mx - sx, my - sy, mz - sz);
			float ax = w.axis[0], ay = w.axis[1], az = w.axis[2];
			float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
			for (int p = 0; p < count; p++) {
				w.t[p] = w.pts[p * 3] * ax + w.pts[p * 3 + 1] * ay + w.pts[p * 3 + 2] * az;
				lo = Math.min(lo, w.t[p]);
				hi = Math.max(hi, w.t[p]);
			}
			float step = (hi - lo) / ALONG;
			for (int k = 0; k < ALONG; k++) {
				float a = lo + k * step - margin, b = lo + (k + 1) * step + margin;
				int in = 0;
				for (int p = 0; p < count; p++) {
					if (w.t[p] >= a && w.t[p] <= b) {
						w.piece[in++] = p;
					}
				}
				// then across: the way this piece spreads most
				axis(w, w.piece, in, ax, ay, az);
				float bx = w.axis[0], by = w.axis[1], bz = w.axis[2];
				float lo2 = Float.MAX_VALUE, hi2 = -Float.MAX_VALUE;
				for (int q = 0; q < in; q++) {
					int p = w.piece[q];
					w.t2[q] = w.pts[p * 3] * bx + w.pts[p * 3 + 1] * by + w.pts[p * 3 + 2] * bz;
					lo2 = Math.min(lo2, w.t2[q]);
					hi2 = Math.max(hi2, w.t2[q]);
				}
				float step2 = (hi2 - lo2) / ACROSS;
				for (int h = 0; h < ACROSS; h++) {
					int part = first + k * ACROSS + h;
					float a2 = lo2 + h * step2 - margin, b2 = lo2 + (h + 1) * step2 + margin;
					empty(w.boxes, part);
					for (int q = 0; q < in; q++) {
						if (w.t2[q] >= a2 && w.t2[q] <= b2) {
							int p = w.piece[q];
							grow(w.boxes, part, w.pts[p * 3], w.pts[p * 3 + 1], w.pts[p * 3 + 2]);
						}
					}
					settle(w.boxes, part);
				}
			}
		}
	}

	/** These bones' points where they are, into the work's list; how many. */
	private static int gather(Skeleton s, Work w, int[] bones) {
		int count = 0;
		for (int b : bones) {
			float[] local = s.samples[b];
			Matrix4f m = w.mats[b];
			for (int p = 0; p < local.length; p += 3) {
				Vector3f v = m.transformPosition(local[p], local[p + 1], local[p + 2], w.v);
				w.pts[count * 3] = v.x;
				w.pts[count * 3 + 1] = v.y;
				w.pts[count * 3 + 2] = v.z;
				count++;
			}
		}
		return count;
	}

	/**
	 * The way these points (listed by index) spread most, into the work's axis: from a hint, a few steps toward the main
	 * axis of how they spread, kept pointing the hint's way.
	 */
	private static void axis(Work w, int[] list, int count, float hx, float hy, float hz) {
		double mx = 0, my = 0, mz = 0;
		for (int q = 0; q < count; q++) {
			int p = list[q];
			mx += w.pts[p * 3];
			my += w.pts[p * 3 + 1];
			mz += w.pts[p * 3 + 2];
		}
		if (count > 0) {
			mx /= count;
			my /= count;
			mz /= count;
		}
		double xx = 0, xy = 0, xz = 0, yy = 0, yz = 0, zz = 0;
		for (int q = 0; q < count; q++) {
			int p = list[q];
			double dx = w.pts[p * 3] - mx, dy = w.pts[p * 3 + 1] - my, dz = w.pts[p * 3 + 2] - mz;
			xx += dx * dx;
			xy += dx * dy;
			xz += dx * dz;
			yy += dy * dy;
			yz += dy * dz;
			zz += dz * dz;
		}
		double vx = hx, vy = hy, vz = hz;
		double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
		if (length < 1.0E-6) {
			vx = 1;
			vy = vz = 0;
			length = 1;
		}
		vx /= length;
		vy /= length;
		vz /= length;
		for (int step = 0; step < 16; step++) {
			double nx = xx * vx + xy * vy + xz * vz;
			double ny = xy * vx + yy * vy + yz * vz;
			double nz = xz * vx + yz * vy + zz * vz;
			double nl = Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (nl < 1.0E-12) {
				break;
			}
			vx = nx / nl;
			vy = ny / nl;
			vz = nz / nl;
		}
		if (vx * hx + vy * hy + vz * hz < 0) {
			vx = -vx;
			vy = -vy;
			vz = -vz;
		}
		w.axis[0] = (float) vx;
		w.axis[1] = (float) vy;
		w.axis[2] = (float) vz;
	}

	private static void empty(float[] box, int part) {
		int o = part * 6;
		box[o] = box[o + 1] = box[o + 2] = Float.MAX_VALUE;
		box[o + 3] = box[o + 4] = box[o + 5] = -Float.MAX_VALUE;
	}

	private static void grow(float[] box, int part, float x, float y, float z) {
		int o = part * 6;
		box[o] = Math.min(box[o], x);
		box[o + 1] = Math.min(box[o + 1], y);
		box[o + 2] = Math.min(box[o + 2], z);
		box[o + 3] = Math.max(box[o + 3], x);
		box[o + 4] = Math.max(box[o + 4], y);
		box[o + 5] = Math.max(box[o + 5], z);
	}

	/** A box with no point in it is none (nothing, at its middle). */
	private static void settle(float[] box, int part) {
		int o = part * 6;
		if (box[o] > box[o + 3]) {
			box[o] = box[o + 1] = box[o + 2] = box[o + 3] = box[o + 4] = box[o + 5] = 0;
		}
	}

	// ----- what is kept -----

	/** A dragon's own room for working out its pose (its bones' turns and places, their matrices, its parts' boxes). */
	public static final class Work {
		Skeleton skeleton;
		int tick = Integer.MIN_VALUE;
		boolean done;
		float[] rx, ry, rz, px, py, pz;
		Matrix4f[] mats;
		final Matrix4f root = new Matrix4f();
		final Vector3f v = new Vector3f();
		float[] pts = new float[0];
		float[] t = new float[0], t2 = new float[0];
		int[] all = new int[0], piece = new int[0];
		final float[] axis = new float[3];
		public final float[] boxes = new float[PARTS * 6];

		void prepare(Skeleton s) {
			if (this.skeleton == s) {
				return;
			}
			this.skeleton = s;
			int n = s.count;
			rx = new float[n];
			ry = new float[n];
			rz = new float[n];
			px = new float[n];
			py = new float[n];
			pz = new float[n];
			mats = new Matrix4f[n];
			for (int i = 0; i < n; i++) {
				mats[i] = new Matrix4f();
			}
			int points = s.mostPoints;
			pts = new float[points * 3];
			t = new float[points];
			t2 = new float[points];
			all = new int[points];
			piece = new int[points];
		}
	}

	/** One tabula model: each bone's place (its pivot) and turn, by the bones of the dragon's own model. */
	private static final class Model {
		final float[] px, py, pz, rx, ry, rz;
		final boolean[] has;

		Model(int n) {
			px = new float[n];
			py = new float[n];
			pz = new float[n];
			rx = new float[n];
			ry = new float[n];
			rz = new float[n];
			has = new boolean[n];
		}
	}

	/** A kind's model: its bones (in the order its renderer draws them, parents first), its cubes and its poses. */
	private static final class Skeleton {
		int kind;
		int count;
		final List<String> names = new ArrayList<>();
		final Map<String, Integer> index = new HashMap<>();
		int[] parent;
		float[] sx, sy, sz;
		float[][] cube;
		float[][] samples;
		boolean[] wing, horn, tail, thigh;
		int[] neck;
		int bodyUpper = -1;
		final int[][] parts = new int[WING_RIGHT][];
		final int[][] wings = new int[2][];
		final int[] wingRoot = new int[2];
		int mostPoints;
		Model base, male, female, dead, sleeping, hovering, flying, sitting, tackle, diving, charge, stream;
		Model[] walk, flight, swim;
		final Map<String, Model> poses = new HashMap<>();
	}

	// ----- reading its models -----

	private static synchronized @Nullable Skeleton skeleton(int kind) {
		if (SKELETONS[kind] != null || FAILED[kind]) {
			return SKELETONS[kind];
		}
		try {
			SKELETONS[kind] = load(kind);
		} catch (IOException | RuntimeException e) {
			FAILED[kind] = true;
			IafMod.LOGGER.error("Could not read the {} dragon's model for its body parts", KINDS[kind], e);
		}
		return SKELETONS[kind];
	}

	private static Skeleton load(int kind) throws IOException {
		Skeleton s = new Skeleton();
		s.kind = kind;
		JsonObject ground = read(kind, "Ground");
		List<JsonObject> cubes = new ArrayList<>();
		List<Integer> parents = new ArrayList<>();
		cubes(ground, cubes, parents);
		int n = cubes.size();
		s.count = n;
		s.parent = new int[n];
		s.sx = new float[n];
		s.sy = new float[n];
		s.sz = new float[n];
		s.cube = new float[n][];
		s.samples = new float[n][];
		s.wing = new boolean[n];
		s.horn = new boolean[n];
		s.tail = new boolean[n];
		s.thigh = new boolean[n];
		for (int i = 0; i < n; i++) {
			JsonObject cube = cubes.get(i);
			String name = cube.get("name").getAsString();
			s.names.add(name);
			s.index.putIfAbsent(name, i);
			s.parent[i] = parents.get(i);
			double[] scale = doubles(cube, "scale", 1);
			s.sx[i] = (float) scale[0];
			s.sy[i] = (float) scale[1];
			s.sz[i] = (float) scale[2];
			double[] offset = doubles(cube, "offset", 0);
			double[] dims = doubles(cube, "dimensions", 0);
			s.cube[i] = new float[]{(float) offset[0], (float) offset[1], (float) offset[2], (float) dims[0], (float) dims[1], (float) dims[2]};
			s.samples[i] = samples(s.cube[i]);
			s.horn[i] = name.contains("Horn");
			s.tail[i] = name.contains("Tail");
			s.thigh[i] = name.equals("ThighR") || name.equals("ThighL");
		}
		s.base = model(s, ground);
		// (its animator: armL1 and armR1 and their own children are its wings)
		for (String arm : new String[]{"armL1", "armR1"}) {
			Integer at = s.index.get(arm);
			if (at != null) {
				s.wing[at] = true;
				for (int i = 0; i < n; i++) {
					if (s.parent[i] == at) {
						s.wing[i] = true;
					}
				}
			}
		}
		s.bodyUpper = s.index.getOrDefault("BodyUpper", -1);
		s.neck = indices(s, "Neck1", "Neck2", "Neck3", "Head");
		s.male = pose(s, kind, "BaseMale");
		s.female = pose(s, kind, "BaseFemale");
		s.dead = pose(s, kind, "Dead");
		s.sleeping = pose(s, kind, "Sleeping");
		s.hovering = pose(s, kind, "Hovering");
		s.flying = pose(s, kind, "Flying");
		s.sitting = pose(s, kind, "Sitting");
		s.tackle = pose(s, kind, "AttackTackle");
		s.diving = pose(s, kind, "Diving");
		s.charge = pose(s, kind, "AttackBlastCharge3");
		s.stream = pose(s, kind, "AttackStreamBreath");
		s.walk = new Model[]{pose(s, kind, "Walk1"), pose(s, kind, "Walk2"), pose(s, kind, "Walk3"), pose(s, kind, "Walk4")};
		s.flight = new Model[]{pose(s, kind, "Flight1"), pose(s, kind, "Flight2"), pose(s, kind, "Flight3"), pose(s, kind, "Flight4"),
			pose(s, kind, "Flight5"), pose(s, kind, "Flight6")};
		if (kind == 1) {
			s.swim = new Model[]{pose(s, kind, "Swim1"), pose(s, kind, "Swim2"), pose(s, kind, "Swim3"), pose(s, kind, "Swim4"), pose(s, kind, "Swim5")};
		}
		for (Key[] keys : ATTACKS.values()) {
			for (Key key : keys) {
				if (key.pose() != null && !s.poses.containsKey(key.pose())) {
					s.poses.put(key.pose(), pose(s, kind, key.pose()));
				}
			}
		}
		// its parts' bones
		s.parts[HEAD] = indices(s, "Head", "HeadFront", "Jaw", "Teeth1", "Teeth2", "Teeth3", "Teeth4");
		s.parts[BODY] = indices(s, "BodyUpper", "BodyLower");
		s.parts[LEGS] = concat(subtree(s, "ThighR"), subtree(s, "ThighL"));
		for (int k = 0; k < 3; k++) {
			s.parts[NECK + k] = indices(s, "Neck" + (k + 1));
		}
		for (int k = 0; k < 5; k++) {
			s.parts[TAIL + k] = indices(s, "Tail" + (k + 1));
		}
		s.parts[TAIL + 5] = subtree(s, "Club");
		s.wings[0] = subtree(s, "armR1");
		s.wings[1] = subtree(s, "armL1");
		s.wingRoot[0] = s.index.getOrDefault("armR1", 0);
		s.wingRoot[1] = s.index.getOrDefault("armL1", 0);
		int most = Math.max(points(s, s.wings[0]), points(s, s.wings[1]));
		for (int[] bones : s.parts) {
			most = Math.max(most, points(s, bones));
		}
		s.mostPoints = Math.max(1, most);
		return s;
	}

	private static int points(Skeleton s, int[] bones) {
		int count = 0;
		for (int b : bones) {
			count += s.samples[b].length / 3;
		}
		return count;
	}

	/** Points over a cube (blocks, in its bone's frame): a few along each side, one across a flat one (a membrane). */
	private static float[] samples(float[] cube) {
		int[] n = new int[3];
		for (int a = 0; a < 3; a++) {
			n[a] = cube[3 + a] <= 0 ? 1 : Math.max(2, Math.min(11, (int) (cube[3 + a] / 4) + 2));
		}
		float[] out = new float[n[0] * n[1] * n[2] * 3];
		int o = 0;
		for (int i = 0; i < n[0]; i++) {
			for (int j = 0; j < n[1]; j++) {
				for (int k = 0; k < n[2]; k++) {
					out[o++] = (cube[0] + (n[0] == 1 ? 0 : cube[3] * i / (n[0] - 1))) / 16F;
					out[o++] = (cube[1] + (n[1] == 1 ? 0 : cube[4] * j / (n[1] - 1))) / 16F;
					out[o++] = (cube[2] + (n[2] == 1 ? 0 : cube[5] * k / (n[2] - 1))) / 16F;
				}
			}
		}
		return out;
	}

	private static int[] indices(Skeleton s, String... names) {
		List<Integer> out = new ArrayList<>();
		for (String name : names) {
			Integer at = s.index.get(name);
			if (at != null) {
				out.add(at);
			}
		}
		return out.stream().mapToInt(Integer::intValue).toArray();
	}

	/** A bone and all below it (parents come first: a bone is in it when its parent is). */
	private static int[] subtree(Skeleton s, String root) {
		Integer at = s.index.get(root);
		if (at == null) {
			return new int[0];
		}
		List<Integer> out = new ArrayList<>();
		boolean[] in = new boolean[s.count];
		in[at] = true;
		out.add(at);
		for (int i = at + 1; i < s.count; i++) {
			if (s.parent[i] >= 0 && in[s.parent[i]]) {
				in[i] = true;
				out.add(i);
			}
		}
		return out.stream().mapToInt(Integer::intValue).toArray();
	}

	private static int[] concat(int[] a, int[] b) {
		int[] out = new int[a.length + b.length];
		System.arraycopy(a, 0, out, 0, a.length);
		System.arraycopy(b, 0, out, a.length, b.length);
		return out;
	}

	/** A model's cubes, parents first (as its model reads them: its cubes, then its groups' cubes). */
	private static void cubes(JsonObject json, List<JsonObject> cubes, List<Integer> parents) {
		if (json.has("cubes")) {
			for (JsonElement cube : json.getAsJsonArray("cubes")) {
				walk(cube.getAsJsonObject(), -1, cubes, parents);
			}
		}
		if (json.has("cubeGroups")) {
			for (JsonElement group : json.getAsJsonArray("cubeGroups")) {
				cubes(group.getAsJsonObject(), cubes, parents);
			}
		}
	}

	private static void walk(JsonObject cube, int parent, List<JsonObject> cubes, List<Integer> parents) {
		int at = cubes.size();
		cubes.add(cube);
		parents.add(parent);
		if (cube.has("children")) {
			for (JsonElement child : cube.getAsJsonArray("children")) {
				walk(child.getAsJsonObject(), at, cubes, parents);
			}
		}
	}

	/** A pose's bones, matched by name to the dragon's own (a bone the pose lacks is left out). */
	private static Model pose(Skeleton s, int kind, String pose) throws IOException {
		return model(s, read(kind, pose));
	}

	private static Model model(Skeleton s, JsonObject json) {
		Model model = new Model(s.count);
		List<JsonObject> cubes = new ArrayList<>();
		cubes(json, cubes, new ArrayList<>());
		Map<String, JsonObject> byName = new HashMap<>();
		for (JsonObject cube : cubes) {
			// (as its model's own map: the last of a name is the one kept)
			byName.put(cube.get("name").getAsString(), cube);
		}
		for (int i = 0; i < s.count; i++) {
			JsonObject cube = byName.get(s.names.get(i));
			if (cube == null) {
				continue;
			}
			double[] position = doubles(cube, "position", 0);
			double[] rotation = doubles(cube, "rotation", 0);
			model.px[i] = (float) position[0];
			model.py[i] = (float) position[1];
			model.pz[i] = (float) position[2];
			model.rx[i] = (float) Math.toRadians(rotation[0]);
			model.ry[i] = (float) Math.toRadians(rotation[1]);
			model.rz[i] = (float) Math.toRadians(rotation[2]);
			model.has[i] = true;
		}
		return model;
	}

	private static double[] doubles(JsonObject cube, String key, double otherwise) {
		double[] out = {otherwise, otherwise, otherwise};
		if (cube.has(key) && cube.get(key).isJsonArray()) {
			JsonArray array = cube.getAsJsonArray(key);
			for (int i = 0; i < Math.min(3, array.size()); i++) {
				out[i] = array.get(i).getAsDouble();
			}
		}
		return out;
	}

	private static JsonObject read(int kind, String pose) throws IOException {
		String path = ("assets/" + IafMod.MOD_ID + "/models/tabula/" + KINDS[kind] + "dragon/" + KINDS[kind] + "dragon_" + pose + ".tbl").toLowerCase(Locale.ROOT);
		InputStream stream = DragonBones.class.getClassLoader().getResourceAsStream(path);
		if (stream == null) {
			throw new IOException("No tabula model at " + path);
		}
		try (ZipInputStream zip = new ZipInputStream(stream)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (entry.getName().equals("model.json")) {
					return JsonParser.parseReader(new InputStreamReader(zip, StandardCharsets.UTF_8)).getAsJsonObject();
				}
			}
		}
		throw new IOException("No model.json in " + path);
	}
}
