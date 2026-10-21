package com.iafabric.entity.util;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * The troll's club strike: a small explosion that breaks blocks (dropping them) and hurts what is near.
 * Vanilla's explosion does all of it, on the server only.
 */
public class BlockBreakExplosion {
    private final Level world;
    private final Mob exploder;
    private final double x;
    private final double y;
    private final double z;
    private final float size;

    public BlockBreakExplosion(Level world, Mob entity, double x, double y, double z, float size) {
        this.world = world;
        this.exploder = entity;
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
    }

    public void explode() {
        if (!world.isClientSide()) {
            world.explode(exploder, x, y, z, size, Level.ExplosionInteraction.MOB);
        }
    }

    public void finalizeExplosion(boolean spawnParticles) {
    }
}
