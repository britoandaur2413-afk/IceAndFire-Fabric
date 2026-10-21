package com.iafabric.pixie;

import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.EntityVfx;
import com.iafabric.entity.props.FrozenProperties;
import com.iafabric.vfx.Vfx;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The pixies' spells, called by their player with the magic key (V): the one chosen in the pixie's menu, if the pixie
 * out knows it and is of the level for it, and once its wait is over. They aim at what the player looks at (or the
 * foe the pixie fights), and the big ones show on the ground first.
 */
public final class PixieMagic {
	/** Who Rebirth guards, and until when (game time). */
	private static final Map<UUID, Long> REBIRTH = new HashMap<>();

	private PixieMagic() {
	}

	public static void register() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && REBIRTH.containsKey(player.getUUID())) {
				long until = REBIRTH.remove(player.getUUID());
				if (player.level().getGameTime() <= until) {
					reborn(player);
					return false;
				}
			}
			return true;
		});
	}

	/** The magic key: casts the chosen spell of the pixie out. */
	public static void cast(ServerPlayer player) {
		PixieBonds bonds = PixieBonding.get(player);
		EntityPixie pixie = PixieBonding.activePixie(player);
		if (bonds.active < 0 || pixie == null || pixie.distanceTo(player) > 32) {
			tell(player, Component.translatable("iceandfire_fabric.pixie_magic.no_pixie"), ChatFormatting.GRAY);
			return;
		}
		PixieBonds.Bond bond = bonds.get(bonds.active);
		PixieAbility spell = bond.selected();
		if (spell == null || spell.color != bonds.active || !bond.knows(spell)) {
			tell(player, Component.translatable("iceandfire_fabric.pixie_magic.none"), ChatFormatting.GRAY);
			return;
		}
		Component name = Component.translatable("pixie.iceandfire_fabric.ability." + spell.name);
		if (bond.level < spell.minLevel) {
			tell(player, Component.translatable("iceandfire_fabric.pixie_magic.level", name, spell.minLevel), ChatFormatting.GRAY);
			return;
		}
		ServerLevel level = (ServerLevel) player.level();
		long now = level.getGameTime();
		if (bond.ready[spell.ordinal()] > now) {
			long seconds = (bond.ready[spell.ordinal()] - now + 19) / 20;
			tell(player, Component.translatable("iceandfire_fabric.pixie_magic.wait", name, seconds), ChatFormatting.GRAY);
			return;
		}
		if (!spell(level, player, pixie, bond, spell)) {
			tell(player, Component.translatable("iceandfire_fabric.pixie_magic.no_target", name), ChatFormatting.GRAY);
			return;
		}
		bond.ready[spell.ordinal()] = now + spell.cooldown;
		PixieBonding.sync(player);
	}

	private static void tell(ServerPlayer player, Component text, ChatFormatting color) {
		player.sendOverlayMessage(text.copy().withStyle(color));
	}

	/** What the player looks at (a creature, up to 24 blocks), else the foe the pixie fights. */
	private static LivingEntity target(ServerLevel level, ServerPlayer player, EntityPixie pixie) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = eye.add(look.scale(24));
		HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (block.getType() != HitResult.Type.MISS) {
			end = block.getLocation();
		}
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1.5),
			e -> e instanceof LivingEntity living && (PixieCombat.hostile(player, pixie, living) || living == player.getLastHurtMob() && living.canBeSeenAsEnemy()),
			24 * 24);
		if (hit != null && hit.getEntity() instanceof LivingEntity living) {
			return living;
		}
		return PixieCombat.foe(level, player, pixie);
	}

	/** Where the player looks on the ground (up to range blocks). */
	private static Vec3 spot(ServerLevel level, ServerPlayer player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));
		HitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 at = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
		return Vfx.ground(level, at);
	}

	private static List<LivingEntity> foesAround(ServerLevel level, ServerPlayer player, EntityPixie pixie, Vec3 at, double radius) {
		List<LivingEntity> list = new ArrayList<>();
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 4, radius), e -> PixieCombat.hostile(player, pixie, e))) {
			if (entity.position().subtract(at).horizontalDistanceSqr() <= radius * radius) {
				list.add(entity);
			}
		}
		return list;
	}

	private static int tint(EntityPixie pixie) {
		return 0xFF000000 | PixieCombat.color(pixie);
	}

	private static boolean spell(ServerLevel level, ServerPlayer player, EntityPixie pixie, PixieBonds.Bond bond, PixieAbility spell) {
		DamageSource source = level.damageSources().indirectMagic(pixie, player);
		int lv = bond.level;
		RandomSource random = player.getRandom();
		int tint = tint(pixie);
		switch (spell) {
			case RAGE -> {
				int ticks = 160 + lv * 20;
				player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ticks, 1), pixie);
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0), pixie);
				Vfx.beam(level, player.position(), 0.7F, 4F, tint, 30);
				level.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.5, 0.5, 0.5, 0);
				level.playSound(null, player.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.8F, 1.6F);
				return true;
			}
			case SHOCKWAVE -> {
				Vec3 at = Vfx.ground(level, player.position());
				Vfx.wave(level, at, 7F, tint, 12);
				Vfx.circle(level, at, 6F, tint, 1);
				level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.4F);
				for (LivingEntity foe : foesAround(level, player, pixie, at, 6)) {
					Vec3 away = foe.position().subtract(at).multiply(1, 0, 1).normalize();
					foe.hurtServer(level, source, 4F + lv * 1.5F);
					foe.push(away.x * 1.4, 0.45, away.z * 1.4);
					foe.syncVelocity = true;
				}
				return true;
			}
			case PILLAR -> {
				LivingEntity foe = target(level, player, pixie);
				if (foe == null) {
					return false;
				}
				Vec3 at = Vfx.ground(level, foe.position());
				Vfx.rune(level, at, 2.6F, EntityVfx.FLAME, Vfx.FIRE, 40, 5F, 0F);
				Vfx.circle(level, at, 2.6F, Vfx.FIRE, 16);
				Vfx.later(level, 16, () -> {
					for (int y = 0; y < 7; y++) {
						level.sendParticles(ParticleTypes.FLAME, at.x, at.y + y, at.z, 14, 0.6, 0.4, 0.6, 0.03);
					}
					Vfx.beam(level, at, 1.2F, 7F, Vfx.FIRE, 14);
					level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.6F);
					for (LivingEntity hit : foesAround(level, player, pixie, at, 2.8)) {
						hit.hurtServer(level, source, 8F + lv * 2F);
						hit.igniteForSeconds(6F);
					}
				});
				return true;
			}
			case BLINK -> {
				Vec3 eye = player.getEyePosition();
				Vec3 look = player.getLookAngle();
				HitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(10)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
				Vec3 to = block.getType() == HitResult.Type.MISS ? eye.add(look.scale(10)) : block.getLocation().subtract(look.scale(0.8));
				to = new Vec3(to.x, to.y - player.getEyeHeight() + 0.1, to.z);
				for (int i = 0; i < 6 && !level.noCollision(player, player.getBoundingBox().move(to.subtract(player.position()))); i++) {
					to = to.add(0, 0.5, 0);
				}
				if (!level.noCollision(player, player.getBoundingBox().move(to.subtract(player.position())))) {
					return false;
				}
				level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30, 0.3, 0.6, 0.3, 0.3);
				player.teleportTo(to.x, to.y, to.z);
				player.resetFallDistance();
				level.sendParticles(new DustParticleOptions(PixieCombat.color(pixie), 1.2F), to.x, to.y + 1, to.z, 30, 0.4, 0.6, 0.4, 0.02);
				level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.5F);
				return true;
			}
			case GRAVITY -> {
				Vec3 at = spot(level, player, 20);
				Vfx.rune(level, at, 5.5F, EntityVfx.DREAD, tint, 70, -6F, 0F);
				level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.2F, 0.5F);
				for (int pull = 0; pull < 12; pull++) {
					int tick = pull;
					Vfx.later(level, 5 * pull + 1, () -> {
						for (LivingEntity foe : foesAround(level, player, pixie, at, 6.5)) {
							Vec3 in = at.subtract(foe.position()).multiply(1, 0, 1);
							if (in.lengthSqr() > 0.5) {
								Vec3 pullBy = in.normalize().scale(0.28);
								foe.push(pullBy.x, 0.02, pullBy.z);
								foe.syncVelocity = true;
							}
							foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 2), player);
							if (tick % 4 == 0) {
								foe.hurtServer(level, source, 1F + lv * 0.3F);
							}
						}
						level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.5, at.z, 12, 2.5, 0.3, 2.5, 0.05);
					});
				}
				return true;
			}
			case STARFALL -> {
				LivingEntity foe = target(level, player, pixie);
				Vec3 center = foe != null ? foe.position() : spot(level, player, 20);
				for (int i = 0; i < 5; i++) {
					Vec3 at = Vfx.ground(level, i == 0 ? center : center.add(random.nextGaussian() * 3, 0, random.nextGaussian() * 3));
					int delay = 22 + i * 4;
					Vfx.circle(level, at, 2F, tint, delay);
					var star = Vfx.shape(level, at.add(0, 16, 0), Blocks.AMETHYST_BLOCK.defaultBlockState(), 0.8F, 0.8F, 45F, 45F, true);
					Vfx.later(level, delay - 8, () -> {
						if (star != null) {
							Vfx.move(star, at, 8);
						}
					});
					Vfx.later(level, delay, () -> {
						Vfx.remove(star);
						level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 25, 0.8, 0.5, 0.8, 0.1);
						level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2F, 0.8F);
						for (LivingEntity hit : foesAround(level, player, pixie, at, 2.2)) {
							hit.hurtServer(level, source, 6F + lv * 1.5F);
						}
					});
				}
				return true;
			}
			case SPEAR -> {
				Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
				Vec3 from = Vfx.ground(level, player.position().add(look.scale(1.5)));
				float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
				Vfx.lane(level, from, yaw, 14F, 2F, Vfx.ICE, 10);
				Vfx.later(level, 10, () -> {
					for (double d = 0; d <= 14; d += 1.4) {
						Vec3 at = from.add(look.scale(d));
						Vfx.spike(level, at, (random.nextBoolean() ? Blocks.PACKED_ICE : Blocks.BLUE_ICE).defaultBlockState(), 0.6F,
							1.6F + random.nextFloat() * 1.4F, (float) look.z * 20F, (float) -look.x * 20F, 3, 40);
					}
					level.playSound(null, from.x, from.y, from.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
					for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from.add(look.scale(14))).inflate(1.5, 3, 1.5),
						e -> PixieCombat.hostile(player, pixie, e))) {
						Vec3 rel = foe.position().subtract(from);
						double along = rel.dot(look);
						double side = rel.subtract(look.scale(along)).horizontalDistance();
						if (along >= -0.5 && along <= 14.5 && side <= 1.6) {
							foe.hurtServer(level, source, 6F + lv * 1.5F);
							foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1), player);
						}
					}
				});
				return true;
			}
			case PRISON -> {
				LivingEntity foe = target(level, player, pixie);
				if (foe == null) {
					return false;
				}
				FrozenProperties.setFrozenFor(foe, foe.getMaxHealth() > 150 ? 40 : 60 + lv * 10);
				foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 3), player);
				foe.hurtServer(level, source, 2F + lv);
				Vfx.rune(level, Vfx.ground(level, foe.position()), Math.max(1.2F, foe.getBbWidth()), EntityVfx.FROST, Vfx.ICE, 60, 4F, 0F);
				level.sendParticles(ParticleTypes.SNOWFLAKE, foe.getX(), foe.getY() + 1, foe.getZ(), 30, 0.5, 0.8, 0.5, 0.05);
				level.playSound(null, foe.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.2F, 0.5F);
				return true;
			}
			case BLIZZARD -> {
				Vec3 at = Vfx.ground(level, player.position());
				Vfx.rune(level, at, 7.5F, EntityVfx.FROST, Vfx.ICE, 130, 2F, 0F);
				for (int s = 0; s < 6; s++) {
					Vfx.later(level, 20 * s + 5, () -> {
						level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 3, at.z, 60, 5, 1.5, 5, 0.05);
						level.sendParticles(ParticleTypes.WHITE_ASH, at.x, at.y + 2, at.z, 30, 5, 1.5, 5, 0);
						for (LivingEntity foe : foesAround(level, player, pixie, at, 7.5)) {
							foe.hurtServer(level, source, 2F + lv * 0.5F);
							foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1), player);
							foe.setTicksFrozen(Math.min(foe.getTicksRequiredToFreeze() + 60, foe.getTicksFrozen() + 25));
						}
					});
				}
				level.playSound(null, player.blockPosition(), SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 1.2F, 0.5F);
				return true;
			}
			case ROOTS -> {
				Vec3 at = Vfx.ground(level, player.position());
				Vfx.rune(level, at, 6.5F, EntityVfx.NATURE, tint, 50, 2F, 0F);
				List<LivingEntity> foes = foesAround(level, player, pixie, at, 6);
				for (LivingEntity foe : foes) {
					foe.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60 + lv * 10, 6), player);
					foe.hurtServer(level, source, 2F);
					level.sendParticles(ParticleTypes.COMPOSTER, foe.getX(), foe.getY() + 0.3, foe.getZ(), 20, 0.4, 0.3, 0.4, 0.02);
				}
				level.playSound(null, player.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
				return true;
			}
			case BLESSING -> {
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1), pixie);
				player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 0), pixie);
				Vfx.beam(level, player.position(), 0.8F, 5F, tint, 40);
				Vfx.rune(level, Vfx.ground(level, player.position()), 2F, EntityVfx.NATURE, tint, 40, 3F, 0F);
				level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.5, player.getZ(), 6, 0.5, 0.4, 0.5, 0.02);
				level.playSound(null, player.blockPosition(), SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.PLAYERS, 1F, 0.8F);
				return true;
			}
			case REBIRTH -> {
				REBIRTH.put(player.getUUID(), level.getGameTime() + 1200);
				Vfx.beam(level, player.position(), 0.6F, 6F, tint, 40);
				level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.6, 0.4, 0.2);
				level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1F, 1.4F);
				tell(player, Component.translatable("iceandfire_fabric.pixie_magic.rebirth_on"), ChatFormatting.GREEN);
				return true;
			}
			case THUNDER -> {
				LivingEntity foe = target(level, player, pixie);
				if (foe == null) {
					return false;
				}
				Vec3 at = Vfx.ground(level, foe.position());
				Vfx.circle(level, at, 2.5F, Vfx.STORM, 14);
				Vfx.later(level, 14, () -> bolt(level, player, pixie, at, 2.5, 5F + lv * 1.5F));
				return true;
			}
			case OVERCHARGE -> {
				player.addEffect(new MobEffectInstance(MobEffects.HASTE, 200, 1), pixie);
				player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0), pixie);
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0), pixie);
				for (int s = 0; s < 10; s++) {
					Vfx.later(level, 20 * s, () -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.6, 0.4, 0.1));
				}
				Vfx.beam(level, player.position(), 0.7F, 4F, Vfx.STORM, 30);
				level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.6F, 1.6F);
				return true;
			}
			case STORM -> {
				Vfx.rune(level, Vfx.ground(level, player.position()), 3F, EntityVfx.STORM, Vfx.STORM, 90, 5F, 0F);
				for (int s = 0; s < 8; s++) {
					Vfx.later(level, 10 * s + 2, () -> {
						List<LivingEntity> foes = foesAround(level, player, pixie, player.position(), 12);
						if (foes.isEmpty()) {
							return;
						}
						LivingEntity foe = foes.get(random.nextInt(foes.size()));
						Vec3 at = Vfx.ground(level, foe.position());
						Vfx.circle(level, at, 1.8F, Vfx.STORM, 8);
						Vfx.later(level, 8, () -> bolt(level, player, pixie, at, 2, 4F + lv));
					});
				}
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private static void bolt(ServerLevel level, ServerPlayer player, EntityPixie pixie, Vec3 at, double radius, float damage) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(at.x, at.y, at.z);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		DamageSource source = level.damageSources().indirectMagic(pixie, player);
		for (LivingEntity foe : foesAround(level, player, pixie, at, radius)) {
			Vec3 away = foe.position().subtract(at).multiply(1, 0, 1).normalize();
			foe.hurtServer(level, source, damage);
			foe.push(away.x * 0.6, 0.3, away.z * 0.6);
			foe.syncVelocity = true;
		}
	}

	/** Rebirth: death turned back once, at half health. */
	private static void reborn(ServerPlayer player) {
		ServerLevel level = (ServerLevel) player.level();
		player.setHealth(player.getMaxHealth() / 2);
		player.removeAllEffects();
		player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
		player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1, 0.5, 0.4);
		level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1F, 1.2F);
		Vfx.beam(level, player.position(), 1F, 8F, 0xFF9FE39A, 40);
		tell(player, Component.translatable("iceandfire_fabric.pixie_magic.reborn"), ChatFormatting.GREEN);
	}

	/** (tests) whether Rebirth guards a player now. */
	public static boolean guarded(ServerPlayer player) {
		Long until = REBIRTH.get(player.getUUID());
		return until != null && player.level().getGameTime() <= until;
	}
}
