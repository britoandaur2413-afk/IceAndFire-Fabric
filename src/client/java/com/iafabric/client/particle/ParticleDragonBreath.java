package com.iafabric.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A puff of a dragon's breath (fire, frost or lightning): one picture that lives its frames as it flies (born a spark
 * in its mouth, a bright ball, wild and growing, then smoke, mist or stray sparks). It leaves the mouth along the way
 * the jaws open and turns toward where the breath strikes, as a missile from its mouth would; there it spreads and
 * slows. A speck (an ember, a snowflake, a spark) is a small one of three frames that only flies. One quad each.
 */
public class ParticleDragonBreath extends IafParticle {
	public static final int FIRE = 0, ICE = 1, LIGHTNING = 2;
	private static final String[] PUFF = {"breath_fire_", "breath_ice_", "breath_lightning_"};
	private static final String[] SPECK = {"breath_ember_", "breath_snow_", "breath_spark_"};
	private static final int FULL_BRIGHT = 0xF000F0;
	/** Where each of its ten frames ends, in its life: a spark only a moment, its fire the longest. */
	private static final float[] FRAME_ENDS = {0.04F, 0.10F, 0.20F, 0.32F, 0.44F, 0.56F, 0.68F, 0.79F, 0.90F, 1.0F};

	private final int kind;
	private final boolean speck;
	private final @Nullable Vec3 target;
	private final float homing;
	private final float size0;
	private final float size1;
	private final float spin;
	private final double arrive;
	private boolean arrived;
	private int frame = -1;

	/**
	 * Born at {@code at} (where the mouth was), {@code into} of the way into this tick: where it is at the tick's end
	 * and where it was at its start are set as if it had flown from there all along, so it is seen leaving the mouth
	 * at its moment (the stream unbroken however fast the dragon flies or turns its head).
	 */
	public ParticleDragonBreath(ClientLevel level, int kind, boolean speck, Vec3 at, Vec3 velocity, float into, @Nullable Vec3 target,
								float homing, float size0, float size1, int lifetime) {
		super(level, at.x + velocity.x * (1 - into), at.y + velocity.y * (1 - into), at.z + velocity.z * (1 - into), (speck ? SPECK : PUFF)[kind] + 0);
		this.xo = at.x - velocity.x * into;
		this.yo = at.y - velocity.y * into;
		this.zo = at.z - velocity.z * into;
		this.xd = velocity.x;
		this.yd = velocity.y;
		this.zd = velocity.z;
		this.kind = kind;
		this.speck = speck;
		this.target = target;
		this.homing = homing;
		this.size0 = size0;
		this.size1 = size1;
		this.quadSize = size0;
		this.lifetime = lifetime;
		this.friction = 1.0F;
		this.gravity = 0.0F;
		this.roll = this.oRoll = this.random.nextFloat() * Mth.TWO_PI;
		this.spin = (this.random.nextFloat() - 0.5F) * (speck ? 0.6F : 0.2F);
		this.arrive = Math.max(1.0D, size1);
		this.frame(0);
	}

	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		this.oRoll = this.roll;
		if (this.age++ >= this.lifetime) {
			this.remove();
			return;
		}
		float life = (float) this.age / this.lifetime;
		if (this.target != null && !this.arrived) {
			double tx = this.target.x - this.x;
			double ty = this.target.y - this.y;
			double tz = this.target.z - this.z;
			double d = Math.sqrt(tx * tx + ty * ty + tz * tz);
			if (d < this.arrive || tx * this.xd + ty * this.yd + tz * this.zd <= 0) {
				// it strikes (or has passed it): it spreads there
				this.arrived = true;
			} else {
				double v = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
				this.xd += (tx / d * v - this.xd) * this.homing;
				this.yd += (ty / d * v - this.yd) * this.homing;
				this.zd += (tz / d * v - this.zd) * this.homing;
			}
		}
		double drag = this.arrived ? 0.72D : this.speck ? 0.93D : 0.97D;
		this.xd *= drag;
		this.yd *= drag;
		this.zd *= drag;
		// fire rises as it cools, frost sinks, a spark crackles about
		if (this.kind == FIRE && life > 0.3F) {
			this.yd += this.speck ? 0.006D : 0.012D;
		} else if (this.kind == ICE) {
			this.yd -= this.speck ? 0.004D : 0.008D;
		} else if (this.kind == LIGHTNING && this.speck) {
			this.xd += (this.random.nextFloat() - 0.5F) * 0.25F;
			this.yd += (this.random.nextFloat() - 0.5F) * 0.25F;
			this.zd += (this.random.nextFloat() - 0.5F) * 0.25F;
		}
		this.move(this.xd, this.yd, this.zd);
		this.roll += this.spin;
		this.frame(this.speck ? Math.min(2, (int) (life * 3)) : puffFrame(life));
		this.alpha = 1.0F - Mth.clamp((life - 0.8F) / 0.2F, 0.0F, 1.0F);
	}

	private static int puffFrame(float life) {
		int frame = 0;
		while (frame < 9 && life >= FRAME_ENDS[frame]) {
			frame++;
		}
		return frame;
	}

	private void frame(int frame) {
		if (frame != this.frame) {
			this.frame = frame;
			this.setTexture((this.speck ? SPECK : PUFF)[this.kind] + frame);
		}
	}

	/** It swells fast as it leaves the mouth, then slowly. */
	@Override
	public float getQuadSize(float partialTick) {
		float t = Mth.clamp((this.age + partialTick) / this.lifetime, 0.0F, 1.0F);
		float grow = 1.0F - (1.0F - t) * (1.0F - t);
		return Mth.lerp(grow, this.size0, this.size1);
	}

	/** Fire and lightning glow while they burn (their smoke as the world is lit); frost shines a little. */
	@Override
	protected int getLightCoords(float partialTick) {
		float t = (this.age + partialTick) / this.lifetime;
		if (this.kind != ICE && (this.speck || t < 0.5F)) {
			return FULL_BRIGHT;
		}
		int world = super.getLightCoords(partialTick);
		int glow = this.kind == ICE ? 11 : (int) (15 * (1.0F - Mth.clamp((t - 0.5F) / 0.35F, 0.0F, 1.0F)));
		int block = Math.max((world >> 4) & 15, glow);
		return (world & 0xF00000) | block << 4;
	}
}
