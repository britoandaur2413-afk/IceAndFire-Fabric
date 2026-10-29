package com.iafabric.entity.ai;

import net.minecraft.server.level.ServerLevel;
import com.iafabric.entity.EntityCyclops;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.jetbrains.annotations.NotNull;

public class CyclopsAIAttackMelee extends MeleeAttackGoal {

    public CyclopsAIAttackMelee(EntityCyclops creature, double speedIn, boolean useLongMemory) {
        super(creature, speedIn, useLongMemory);
    }

    @Override
    protected void checkAndPerformAttack(@NotNull LivingEntity entity, double distance) {
        if (isCyclopsBlinded() && distance >= 36D) {
            this.stop();
            return;
        }
        if (this.isTimeToAttack() && distance <= this.getAttackReachSqr(entity) && this.mob.getSensing().hasLineOfSight(entity)) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND, true);
            this.mob.doHurtTarget(entity);
        }
    }

    private boolean isCyclopsBlinded() {
        return this.mob instanceof EntityCyclops && ((EntityCyclops) this.mob).isBlinded();
    }
}
