package com.iafabric.pathfinding;

import com.iafabric.entity.EntityCyclops;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;

/** The cyclops walks through doors and swims. */
public class PathNavigateCyclops extends GroundPathNavigation {

    public PathNavigateCyclops(EntityCyclops cyclops, Level level) {
        super(cyclops, level);
        this.setCanOpenDoors(false);
        this.setCanFloat(true);
    }
}
