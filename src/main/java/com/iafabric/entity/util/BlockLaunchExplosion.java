package com.iafabric.entity.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * An explosion that throws the blocks it breaks into the air as falling blocks (death worms, dragon breath).
 * Damage, sound and particles come from a vanilla explosion that keeps the blocks; then the blocks the rays
 * reach are launched away from the center.
 */
public class BlockLaunchExplosion {
    private final Level world;
    private final Mob exploder;
    private final DamageSource source;
    private final double x;
    private final double y;
    private final double z;
    private final float size;
    private final boolean destroy;

    public BlockLaunchExplosion(Level world, Mob entity, double x, double y, double z, float size) {
        this(world, entity, null, x, y, z, size, true);
    }

    public BlockLaunchExplosion(Level world, Mob entity, DamageSource source, double x, double y, double z, float size, boolean destroy) {
        this.world = world;
        this.exploder = entity;
        this.source = source;
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.destroy = destroy;
    }

    public void explode() {
        if (world.isClientSide) {
            return;
        }
        // the blast spares the one who made it and its side (a dread dragon's knights, a kingdom dragon's pillagers)
        Mob maker = exploder;
        net.minecraft.world.level.ExplosionDamageCalculator spare = new net.minecraft.world.level.ExplosionDamageCalculator() {
            @Override
            public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, net.minecraft.world.entity.Entity entity) {
                return (maker == null || entity != maker && !maker.isAlliedTo(entity) && !(entity instanceof com.iafabric.entity.EntityMutlipartPart part && part.getParent() == maker))
                    && super.shouldDamageEntity(explosion, entity);
            }

            @Override
            public float getKnockbackMultiplier(net.minecraft.world.entity.Entity entity) {
                return maker != null && (entity == maker || maker.isAlliedTo(entity)) ? 0F : super.getKnockbackMultiplier(entity);
            }
        };
        world.explode(exploder, source, spare, x, y, z, size, false, Level.ExplosionInteraction.NONE);
        if (!destroy) {
            return;
        }
        Set<BlockPos> blocks = new HashSet<>();
        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    if (j != 0 && j != 15 && k != 0 && k != 15 && l != 0 && l != 15) {
                        continue;
                    }
                    double dx = j / 15.0D * 2.0D - 1.0D;
                    double dy = k / 15.0D * 2.0D - 1.0D;
                    double dz = l / 15.0D * 2.0D - 1.0D;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    dx /= len;
                    dy /= len;
                    dz /= len;
                    float power = size * (0.7F + world.getRandom().nextFloat() * 0.6F);
                    double px = x, py = y, pz = z;
                    for (; power > 0.0F; power -= 0.22500001F) {
                        BlockPos pos = BlockPos.containing(px, py, pz);
                        BlockState state = world.getBlockState(pos);
                        FluidState fluid = world.getFluidState(pos);
                        if (!world.isInWorldBounds(pos)) {
                            break;
                        }
                        if (!state.isAir() || !fluid.isEmpty()) {
                            float resistance = Math.max(state.getBlock().getExplosionResistance(), fluid.getExplosionResistance());
                            power -= (resistance + 0.3F) * 0.3F;
                        }
                        if (power > 0.0F && !state.isAir() && fluid.isEmpty() && state.getDestroySpeed(world, pos) >= 0) {
                            blocks.add(pos);
                        }
                        px += dx * 0.3D;
                        py += dy * 0.3D;
                        pz += dz * 0.3D;
                    }
                }
            }
        }
        Vec3 center = new Vec3(x, y, z);
        float reach = size * 2.0F;
        for (BlockPos pos : blocks) {
            BlockState state = world.getBlockState(pos);
            if (state.isAir() || state.hasBlockEntity()) {
                continue;
            }
            FallingBlockEntity falling = FallingBlockEntity.fall(world, pos, state);
            Vec3 away = falling.position().subtract(center);
            double push = Math.max(0.0D, 1.0D - away.length() / reach);
            falling.setDeltaMovement(away.x * push, away.y * push + 0.3D, away.z * push);
        }
    }

    public void finalizeExplosion(boolean spawnParticles) {
    }
}
