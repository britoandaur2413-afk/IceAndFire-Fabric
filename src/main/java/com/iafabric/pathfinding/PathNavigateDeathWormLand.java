package com.iafabric.pathfinding;

import com.iafabric.entity.EntityDeathWorm;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;

/** A death worm out of the sand walks like any land mob, and may path while in sand. */
public class PathNavigateDeathWormLand extends GroundPathNavigation {
    private final EntityDeathWorm worm;

    public PathNavigateDeathWormLand(EntityDeathWorm worm, Level level) {
        super(worm, level);
        this.worm = worm;
        this.setCanFloat(true);
    }

    @Override
    protected boolean canUpdatePath() {
        return super.canUpdatePath() || this.worm.isInSand();
    }
}
