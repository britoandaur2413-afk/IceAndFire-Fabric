package com.iafabric.entity.util;

import net.minecraft.world.entity.player.Player;

public interface IFlyingMount {

    Player getRidingPlayer();

    double getFlightSpeedModifier();

    default boolean fliesLikeElytra() {
        return false;
    }

    boolean isFlying();

    default boolean isGoingUp() {
        return false;
    }

    default boolean isGoingDown() {
        return false;
    }

    default boolean isHovering() {
        return false;
    }

    default double getYSpeedMod(){ return 10; }

    /** A rider may get off a flying mount only on the ground, in water, or when it is not in the air. */
    static boolean canDismount(net.minecraft.world.entity.Entity mount) {
        if (!(mount instanceof IFlyingMount flying)) {
            return true;
        }
        boolean hovering = mount instanceof com.iafabric.entity.EntityDragonBase dragon && dragon.isHovering()
            || mount instanceof com.iafabric.entity.EntityHippogryph hippogryph && hippogryph.isHovering();
        return mount.onGround() || mount.isInWater() || mount.isInLava() || !flying.isFlying() && !hovering;
    }
}
