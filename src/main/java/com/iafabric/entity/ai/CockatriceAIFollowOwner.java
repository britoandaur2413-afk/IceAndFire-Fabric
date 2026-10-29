package com.iafabric.entity.ai;

import com.iafabric.entity.EntityCockatrice;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

public class CockatriceAIFollowOwner extends FollowOwnerGoal {
    EntityCockatrice cockatrice;

    public CockatriceAIFollowOwner(EntityCockatrice cockatrice, double speed, float minDist, float maxDist) {
        super(cockatrice, speed, minDist, maxDist, false);
        this.cockatrice = cockatrice;
    }

    @Override
    public boolean canUse() {
        return super.canUse() && cockatrice.getCommand() == 2;
    }
}
