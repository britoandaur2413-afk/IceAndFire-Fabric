package com.iafabric.pathfinding;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.level.Level;

/** Walks on land and swims in water (sirens, hippocampi): vanilla's amphibious navigation. */
public class PathNavigateAmphibious extends AmphibiousPathNavigation {

    public PathNavigateAmphibious(PathfinderMob mob, Level level) {
        super(mob, level);
        this.setCanFloat(true);
    }
}
