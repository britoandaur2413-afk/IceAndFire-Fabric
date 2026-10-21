package com.iafabric.entity.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.SwingAnimation;
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
    protected void checkAndPerformAttack(@NotNull LivingEntity entity) {
        final double distance = this.mob.distanceToSqr(entity);
        if (isCyclopsBlinded() && distance >= 36D) {
            this.stop();
            return;
        }
        if (this.canPerformAttack(entity)) {
            this.resetAttackCooldown();
            this.mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
            this.mob.doHurtTarget((ServerLevel) this.mob.level(), entity);
        }
    }

    private boolean isCyclopsBlinded() {
        return this.mob instanceof EntityCyclops && ((EntityCyclops) this.mob).isBlinded();
    }
}
