package com.iafabric.entity.ai;

import com.iafabric.entity.EntityHippogryph;
import com.iafabric.entity.util.DragonUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;
import java.util.function.Predicate;

public class HippogryphAITarget<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    private final EntityHippogryph hippogryph;

    public HippogryphAITarget(EntityHippogryph entityIn, Class<T> classTarget, boolean checkSight, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(entityIn, classTarget, 20, checkSight, false, targetPredicate == null ? null : target -> targetPredicate.test(target));
        this.hippogryph = entityIn;
    }

    public HippogryphAITarget(EntityHippogryph entityIn, Class<T> classTarget, int i, boolean checkSight, @Nullable Predicate<LivingEntity> targetPredicate) {
        super(entityIn, classTarget, i, checkSight, false, targetPredicate == null ? null : target -> targetPredicate.test(target));
        this.hippogryph = entityIn;
    }


    @Override
    public boolean canUse() {
        if (super.canUse() && target != null && !target.getClass().equals(this.hippogryph.getClass())) {
            if (this.hippogryph.getBbWidth() >= target.getBbWidth()) {
                if (target instanceof Player) {
                    return !hippogryph.isTame();
                } else {
                    if (!hippogryph.isOwnedBy(target) && hippogryph.canMove() && target instanceof Animal) {
                        if (hippogryph.isTame()) {
                            return DragonUtils.canTameDragonAttack(hippogryph, target);
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}