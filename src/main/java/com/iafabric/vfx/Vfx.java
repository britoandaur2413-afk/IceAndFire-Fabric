package com.iafabric.vfx;

import com.iafabric.entity.EntityVfx;
import com.iafabric.entity.IafEntityRegistry;
import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The effects of the mod's fights, for every eye: warning circles and lanes on the ground that fill until the blow
 * falls, magic circles, shock waves and columns of light (EntityVfx), ice spikes rising from the ground and blocks
 * lifted, turned and thrown (block displays, moving smoothly), and things done a while later (the blow after its
 * warning).
 */
public final class Vfx {
	/** Colors (with full alpha): ice, dread, fire, storm, warning red. */
	public static final int ICE = 0xFF8FE3FF;
	public static final int DREAD = 0xFFB07CFF;
	public static final int FIRE = 0xFFFF8A3C;
	public static final int STORM = 0xFFFFE27A;
	public static final int DANGER = 0xFFFF4A3A;
	private static final List<Task> TASKS = new ArrayList<>();
	private static final List<Task> ADDED = new ArrayList<>();
	/** Where and when ice broke a moment ago, by world (x, y, z, time): many blocks at once are one crash, not a din. */
	private static final Map<Object, List<double[]>> SHATTERS = new HashMap<>();
	/** The same for any sound played with once(), by world and sound. */
	private static final Map<Object, List<double[]>> SOUNDS = new HashMap<>();

	private Vfx() {
	}

	private record Task(ServerLevel level, long due, Runnable run) {
	}

	public static void register() {
		ServerTickEvents.END_LEVEL_TICK.register(Vfx::tick);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			synchronized (TASKS) {
				TASKS.clear();
				ADDED.clear();
			}
			SHATTERS.clear();
			SOUNDS.clear();
		});
	}

	/**
	 * Ice breaking: its sound, not too loud, unless ice broke a moment ago near there (circles falling all at once, a
	 * wave of spikes: one crash, never a din of glass over and over).
	 */
	public static void shatter(ServerLevel level, Vec3 at, float volume, float pitch) {
		long now = level.getGameTime();
		List<double[]> recent = SHATTERS.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
		recent.removeIf(s -> now - (long) s[3] > 5 || now < (long) s[3]);
		for (double[] s : recent) {
			if (at.distanceToSqr(s[0], s[1], s[2]) < 14 * 14) {
				return;
			}
		}
		recent.add(new double[]{at.x, at.y, at.z, now});
		level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, Math.min(volume, 1.3F), pitch);
	}

	/**
	 * A sound of a fight, unless the same one was played near there (within distance) a moment ago (within ticks):
	 * many at once are one, never the same sound over and over.
	 */
	public static void once(ServerLevel level, Vec3 at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch, int ticks, double distance) {
		long now = level.getGameTime();
		List<double[]> recent = SOUNDS.computeIfAbsent(List.of(level.dimension(), sound), key -> new ArrayList<>());
		recent.removeIf(s -> now - (long) s[3] > ticks || now < (long) s[3]);
		for (double[] s : recent) {
			if (at.distanceToSqr(s[0], s[1], s[2]) < distance * distance) {
				return;
			}
		}
		recent.add(new double[]{at.x, at.y, at.z, now});
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
	}

	/**
	 * Places in a ring round a middle, that many at that distance, the first turned so many degrees: circles on them
	 * never touch one another when the ring is wide enough for them (2 · distance · sin(180 / count) over their width).
	 */
	public static List<Vec3> ring(Vec3 middle, int count, double distance, double turn) {
		List<Vec3> spots = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			double a = Math.toRadians(turn) + Math.PI * 2 * i / count;
			spots.add(middle.add(Math.cos(a) * distance, 0, Math.sin(a) * distance));
		}
		return spots;
	}

	/** Does something a number of ticks from now (on the server, in that level). */
	public static void later(ServerLevel level, int ticks, Runnable run) {
		synchronized (TASKS) {
			ADDED.add(new Task(level, level.getGameTime() + Math.max(1, ticks), run));
		}
	}

	private static void tick(ServerLevel level) {
		List<Task> due = new ArrayList<>();
		synchronized (TASKS) {
			TASKS.addAll(ADDED);
			ADDED.clear();
			long now = level.getGameTime();
			for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
				Task task = it.next();
				if (task.level == level && task.due <= now) {
					due.add(task);
					it.remove();
				}
			}
		}
		for (Task task : due) {
			try {
				task.run.run();
			} catch (RuntimeException e) {
				com.iafabric.IafMod.LOGGER.warn("An effect task failed", e);
			}
		}
	}

	/** The ground's top at a place (from a little above it down), for marks on the ground. */
	public static Vec3 ground(ServerLevel level, Vec3 at) {
		BlockPos.MutableBlockPos pos = BlockPos.containing(at.x, at.y + 3, at.z).mutable();
		for (int i = 0; i < 12; i++) {
			if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
				double top = pos.getY() + level.getBlockState(pos).getCollisionShape(level, pos).max(net.minecraft.core.Direction.Axis.Y);
				return new Vec3(at.x, top, at.z);
			}
			pos.move(0, -1, 0);
		}
		return at;
	}

	private static EntityVfx spawn(ServerLevel level, Vec3 at, float yaw, int kind, int style, int color, float size, float length, int charge, int life, float spin) {
		return spawn(level, at, yaw, 0F, kind, style, color, size, length, charge, life, spin);
	}

	/** (Its turn and tilt set before it is seen: an effect's are sent to the players only now and then.) */
	private static EntityVfx spawn(ServerLevel level, Vec3 at, float yaw, float pitch, int kind, int style, int color, float size, float length, int charge, int life,
		float spin) {
		EntityVfx vfx = IafEntityRegistry.VFX.create(level, EntitySpawnReason.TRIGGERED);
		if (vfx == null) {
			return null;
		}
		vfx.snapTo(at.x, at.y, at.z, yaw, pitch);
		vfx.setup(kind, style, color, size, length, charge, life, spin);
		level.addFreshEntity(vfx);
		return vfx;
	}

	/** A warning circle on the ground that fills in charge ticks, then flashes (the blow falls then). */
	public static EntityVfx circle(ServerLevel level, Vec3 at, float radius, int color, int charge) {
		return spawn(level, ground(level, at), 0F, EntityVfx.CIRCLE, 0, color, radius, 0F, charge, charge + 8, 0F);
	}

	/** A warning lane on the ground from a place, along a way (yaw), filling in charge ticks. */
	public static EntityVfx lane(ServerLevel level, Vec3 from, float yaw, float length, float width, int color, int charge) {
		return spawn(level, ground(level, from), yaw, EntityVfx.LINE, 0, color, width, length, charge, charge + 8, 0F);
	}

	/** A magic circle (frost, dread, storm, flame) turning for a while, lift blocks over the place. */
	public static EntityVfx rune(ServerLevel level, Vec3 at, float radius, int style, int color, int life, float spin, float lift) {
		return spawn(level, at, 0F, EntityVfx.RUNE, style, color, radius, lift, 1, life, spin);
	}

	/** A shock wave going out to radius in life ticks. */
	public static EntityVfx wave(ServerLevel level, Vec3 at, float radius, int color, int life) {
		return spawn(level, at, 0F, EntityVfx.WAVE, 0, color, radius, 0F, 1, life, 0F);
	}

	/** A column of light from a place. */
	public static EntityVfx beam(ServerLevel level, Vec3 at, float radius, float height, int color, int life) {
		return spawn(level, at, 0F, EntityVfx.BEAM, 0, color, radius, height, 1, life, 0F);
	}

	/**
	 * A jet (a breath of frost): from a place along a way (yaw, and pitch down), widening from a third of its width to
	 * all of it at its far end, flickering, for its life.
	 */
	public static EntityVfx stream(ServerLevel level, Vec3 from, float yaw, float pitch, float length, float width, int color, int life) {
		return spawn(level, from, yaw, pitch, EntityVfx.STREAM, 0, color, width, length, 1, life, 0F);
	}

	/** An item shown floating (a weapon given), turning slowly, full bright; it stays until taken away. */
	public static Display.ItemDisplay item(ServerLevel level, Vec3 at, net.minecraft.world.item.ItemStack stack, float size) {
		Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return null;
		}
		display.snapTo(at.x, at.y, at.z, 0F, 0F);
		display.setItemStack(stack);
		display.setItemTransform(net.minecraft.world.item.ItemDisplayContext.FIXED);
		display.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateZ((float) Math.toRadians(-45)), new Vector3f(size, size, size),
			new Quaternionf()));
		display.setBrightnessOverride(Brightness.FULL_BRIGHT);
		display.setViewRange(2F);
		level.addFreshEntity(display);
		return display;
	}

	/**
	 * A block shown as a shape (a shard of ice, a stone): its bottom middle at the place, size w x h x w, leaning by
	 * the given degrees; full bright when asked. It stays until taken away.
	 */
	public static Display.BlockDisplay shape(ServerLevel level, Vec3 at, BlockState state, float w, float h, float leanX, float leanZ, boolean bright) {
		Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.TRIGGERED);
		if (display == null) {
			return null;
		}
		display.snapTo(at.x, at.y, at.z, 0F, 0F);
		display.setBlockState(state);
		display.setTransformation(transform(w, h, leanX, leanZ, 0F));
		display.setViewRange(2F);
		if (bright) {
			display.setBrightnessOverride(Brightness.FULL_BRIGHT);
		}
		level.addFreshEntity(display);
		return display;
	}

	/** The shape of a block display: w x h x w, its bottom middle at its place, leaning, turned about its upright. */
	public static Transformation transform(float w, float h, float leanX, float leanZ, float turn) {
		Quaternionf rotation = new Quaternionf().rotateY((float) Math.toRadians(turn)).rotateX((float) Math.toRadians(leanX)).rotateZ((float) Math.toRadians(leanZ));
		Vector3f offset = new Vector3f(-w / 2, 0, -w / 2).rotate(rotation);
		return new Transformation(offset, rotation, new Vector3f(w, h, w), new Quaternionf());
	}

	/** Changes a display's shape smoothly over ticks. */
	public static void reshape(Display display, Transformation to, int ticks) {
		display.setTransformationInterpolationDelay(0);
		display.setTransformationInterpolationDuration(ticks);
		display.setTransformation(to);
	}

	/** Moves a display smoothly to a place over ticks. */
	public static void move(Display display, Vec3 to, int ticks) {
		display.setPosRotInterpolationDuration(ticks);
		display.teleportTo(to.x, to.y, to.z);
	}

	/**
	 * An ice spike out of the ground: a shard (w wide, h high, leaning) rising in rise ticks, staying, then sinking
	 * back and gone after life ticks in all.
	 */
	public static Display.BlockDisplay spike(ServerLevel level, Vec3 at, BlockState state, float w, float h, float leanX, float leanZ, int rise, int life) {
		Display.BlockDisplay spike = shape(level, ground(level, at).subtract(0, 0.2, 0), state, w, 0.05F, leanX, leanZ, true);
		if (spike == null) {
			return null;
		}
		later(level, 1, () -> reshape(spike, transform(w, h, leanX, leanZ, 0F), rise));
		later(level, Math.max(rise + 2, life - 8), () -> {
			if (!spike.isRemoved()) {
				reshape(spike, transform(w * 0.6F, 0.05F, leanX, leanZ, 0F), 8);
			}
		});
		later(level, life, () -> remove(spike));
		return spike;
	}

	public static void remove(Entity entity) {
		if (entity != null && !entity.isRemoved()) {
			entity.discard();
		}
	}
}
