package com.iafabric.entity.ai;

import com.iafabric.api.FoodUtils;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityIceDragon;
import com.iafabric.util.IAFMath;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.NotNull;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class DragonAITargetItems<T extends ItemEntity> extends TargetGoal {

    protected final DragonAITargetItems.Sorter theNearestAttackableTargetSorter;
    protected final Predicate<? super ItemEntity> targetEntitySelector;
    private final int targetChance;
    protected ItemEntity targetEntity;
    private boolean isIce = false;

    @NotNull
    private List<ItemEntity> list = IAFMath.emptyItemEntityList;

    public DragonAITargetItems(EntityDragonBase creature, boolean checkSight) {
        this(creature, checkSight, false);
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    public DragonAITargetItems(EntityDragonBase creature, boolean checkSight, boolean onlyNearby) {
        this(creature, 20, checkSight, onlyNearby);
        isIce = creature instanceof EntityIceDragon;
    }

    public DragonAITargetItems(EntityDragonBase creature, int chance, boolean checkSight, boolean onlyNearby) {
        super(creature, checkSight, onlyNearby);
        isIce = creature instanceof EntityIceDragon;
        this.targetChance = chance;
        this.theNearestAttackableTargetSorter = new DragonAITargetItems.Sorter(creature);
        this.setFlags(EnumSet.of(Flag.MOVE));
        this.targetEntitySelector = item -> item != null && !item.getItem().isEmpty() && item.getItem().getItem() != null
                    && FoodUtils.getFoodPoints(item.getItem(), true, isIce) > 0;
    }

    @Override
    public boolean canUse() {
        final EntityDragonBase dragon = (EntityDragonBase) this.mob;
        if (dragon.getHunger() >= 100 || !dragon.canMove() || (this.targetChance > 0 && this.mob.getRandom().nextInt(10) != 0)) {
            list = IAFMath.emptyItemEntityList;
            return false;
        } else {
            return updateList();
        }
    }

    private boolean updateList() {
        if (this.mob.level().getGameTime() % 4 <= 1) // update the list every 4 ticks (26.x checks goals every other tick: two ticks so one is seen)
            list = this.mob.level().getEntitiesOfClass(ItemEntity.class,
                    this.getTargetableArea(this.getFollowDistance()), this.targetEntitySelector);

        if (list.isEmpty()) {
            return false;
        } else {
            list.sort(this.theNearestAttackableTargetSorter);
            this.targetEntity = list.get(0);
            return true;
        }
    }

    protected AABB getTargetableArea(double targetDistance) {
        return this.mob.getBoundingBox().inflate(targetDistance, 4.0D, targetDistance);
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(this.targetEntity.getX(), this.targetEntity.getY(),
            this.targetEntity.getZ(), 1);
        super.start();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetEntity == null || !this.targetEntity.isAlive()) {
            this.stop();
        } else if (this.mob.distanceToSqr(this.targetEntity) < this.mob.getBbWidth() * 2 + this.mob.getBbHeight() / 2 ||
            (this.mob instanceof EntityDragonBase &&
                ((EntityDragonBase) this.mob).getHeadPosition().distanceToSqr(this.targetEntity.position()) < this.mob.getBbHeight())) {

            this.mob.playSound(SoundEvents.GENERIC_EAT, 1, 1);
            final int hunger = FoodUtils.getFoodPoints(this.targetEntity.getItem(), true, isIce);
            final EntityDragonBase dragon = ((EntityDragonBase) this.mob);
            dragon.setHunger(Math.min(100, ((EntityDragonBase) this.mob).getHunger() + hunger));
            dragon.eatFoodBonus(this.targetEntity.getItem());
            this.mob.setHealth(Math.min(this.mob.getMaxHealth(), (int) (this.mob.getHealth()
                + FoodUtils.getFoodPoints(this.targetEntity.getItem(), true, isIce))));
            if (EntityDragonBase.ANIMATION_EAT != null) {
                dragon.setAnimation(EntityDragonBase.ANIMATION_EAT);
            }
            for (int i = 0; i < 4; i++) {
                dragon.spawnItemCrackParticles(this.targetEntity.getItem().getItem());
            }
            this.targetEntity.getItem().shrink(1);
            stop();
        } else {
            updateList();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.getNavigation().isDone();
    }

    public static class Sorter implements Comparator<Entity> {

        private final Entity theEntity;

        public Sorter(Entity theEntityIn) {
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
