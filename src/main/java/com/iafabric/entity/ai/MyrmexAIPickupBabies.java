package com.iafabric.entity.ai;

import com.iafabric.entity.EntityMyrmexBase;
import com.iafabric.entity.EntityMyrmexEgg;
import com.iafabric.entity.EntityMyrmexWorker;
import com.iafabric.util.IAFMath;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class MyrmexAIPickupBabies<T extends ItemEntity> extends TargetGoal {
    protected final DragonAITargetItems.Sorter theNearestAttackableTargetSorter;
    protected final Predicate<? super LivingEntity> targetEntitySelector;
    public EntityMyrmexWorker myrmex;
    protected LivingEntity targetEntity;

    private List<LivingEntity> listBabies = IAFMath.emptyLivingEntityList;

    public MyrmexAIPickupBabies(EntityMyrmexWorker myrmex) {
        super(myrmex, false, false);
        this.theNearestAttackableTargetSorter = new DragonAITargetItems.Sorter(myrmex);
        this.targetEntitySelector = other -> (other instanceof EntityMyrmexBase && ((EntityMyrmexBase) other).getGrowthStage() < 2
                    && !((EntityMyrmexBase) other).isInNursery()
                    || other instanceof EntityMyrmexEgg && !((EntityMyrmexEgg) other).isInNursery());
        this.myrmex = myrmex;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!this.myrmex.canMove() || this.myrmex.holdingSomething() || !this.myrmex.getNavigation().isDone() || this.myrmex.shouldEnterHive() || !this.myrmex.keepSearching || this.myrmex.holdingBaby()) {
            listBabies = IAFMath.emptyLivingEntityList;
            return false;
        }

        if (this.myrmex.level().getGameTime() % 4 <= 1) // update the list every 4 ticks (26.x checks goals every other tick: two ticks so one is seen)
            listBabies = this.mob.level().getEntitiesOfClass(LivingEntity.class, this.getTargetableArea(20), this.targetEntitySelector);

        if (listBabies.isEmpty())
            return false;

        listBabies.sort(this.theNearestAttackableTargetSorter);
        this.targetEntity = listBabies.get(0);
        return true;
    }

    protected AABB getTargetableArea(double targetDistance) {
        return this.mob.getBoundingBox().inflate(targetDistance, 4.0D, targetDistance);
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 1);
        super.start();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetEntity != null && this.targetEntity.isAlive()
            && this.mob.distanceToSqr(this.targetEntity) < 2) {
            this.targetEntity.startRiding(this.myrmex);
        }
        stop();
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    public static class Sorter implements Comparator<Entity> {
        private final Entity theEntity;

        public Sorter(EntityMyrmexBase theEntityIn) {
            this.theEntity = theEntityIn;
        }

        @Override
        public int compare(Entity p_compare_1_, Entity p_compare_2_) {
            final double d0 = this.theEntity.distanceToSqr(p_compare_1_);
            final double d1 = this.theEntity.distanceToSqr(p_compare_2_);
            return Double.compare(d0, d1);
        }
    }
}