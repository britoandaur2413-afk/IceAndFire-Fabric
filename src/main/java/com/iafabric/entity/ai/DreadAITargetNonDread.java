package com.iafabric.entity.ai;

import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.IDreadMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.function.Predicate;

public class DreadAITargetNonDread extends NearestAttackableTargetGoal<LivingEntity> {

    public DreadAITargetNonDread(Mob entityIn, Class<LivingEntity> classTarget, boolean checkSight,
                                 Predicate<LivingEntity> targetSelector) {
        // the dread never picks one of its own, nor its own undead dragons, as a foe
        super(entityIn, classTarget, 0, checkSight, false, target -> !(target instanceof IDreadMob)
            && !(target instanceof com.iafabric.entity.EntityDragonBase dragon && dragon.dreadMount)
            && (targetSelector == null || targetSelector.test(target)));
    }

    @Override
    protected boolean canAttack(@Nullable LivingEntity target, @NotNull TargetingConditions targetPredicate) {
        if (super.canAttack(target, targetPredicate)) {
            // the dread's own dragons are not foes
            return !(target instanceof IDreadMob) && !(target instanceof com.iafabric.entity.EntityDragonBase dragon && dragon.dreadMount)
                && DragonUtils.isAlive(target);
        }
        return false;
    }

}
