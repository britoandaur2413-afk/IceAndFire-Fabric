package com.iafabric.entity.ai;

import com.iafabric.entity.EntityMyrmexBase;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

public class MyrmexAIAttackPlayers extends NearestAttackableTargetGoal {

    private final EntityMyrmexBase myrmex;

    @SuppressWarnings("unchecked")
    public MyrmexAIAttackPlayers(EntityMyrmexBase myrmex) {
        super(myrmex, Player.class, 10, true, true, entity -> entity != null && (myrmex.getHive() == null
                    || myrmex.getHive().isPlayerReputationLowEnoughToFight(((Player) entity).getUUID())));
        this.myrmex = myrmex;
    }

    @Override
    public boolean canUse() {
        return myrmex.shouldHaveNormalAI() && super.canUse();
    }
}
