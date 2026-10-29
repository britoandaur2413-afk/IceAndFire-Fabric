package com.iafabric.entity.ai;

import com.iafabric.entity.EntityPixie;
import com.iafabric.misc.IafSoundRegistry;
import com.iafabric.util.IAFMath;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

public class PixieAIPickupItem<T extends ItemEntity> extends TargetGoal {
    protected final DragonAITargetItems.Sorter theNearestAttackableTargetSorter;
    protected final Predicate<? super ItemEntity> targetEntitySelector;
    protected ItemEntity targetEntity;

    @NotNull
    private List<ItemEntity> list = IAFMath.emptyItemEntityList;
    private long lastSearch = Long.MIN_VALUE / 2;

    public PixieAIPickupItem(EntityPixie creature, boolean checkSight) {
        this(creature, checkSight, false);
    }

    public PixieAIPickupItem(EntityPixie creature, boolean checkSight, boolean onlyNearby) {
        this(creature, 20, checkSight, onlyNearby, null);
    }

    public PixieAIPickupItem(EntityPixie creature, int chance, boolean checkSight, boolean onlyNearby, @Nullable final Predicate<? super T> targetSelector) {
        super(creature, checkSight, onlyNearby);
        this.theNearestAttackableTargetSorter = new DragonAITargetItems.Sorter(creature);

        this.targetEntitySelector = item -> item != null && !item.getItem().isEmpty() && (item.getItem().getItem() == Items.CAKE
                    && !creature.isTame()
                    || item.getItem().getItem() == Items.SUGAR && creature.isTame()
                    && creature.getHealth() < creature.getMaxHealth());
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {

        EntityPixie pixie = (EntityPixie) this.mob;
        if (pixie.isPixieSitting()) return false;

        // Look again at most every 4 ticks (26.x goals are not checked every tick, a "% 4" could never come)
        long now = this.mob.level().getGameTime();
        if (now - lastSearch >= 4) {
            lastSearch = now;
            list = this.mob.level().getEntitiesOfClass(ItemEntity.class, this.getTargetableArea(this.getFollowDistance()), this.targetEntitySelector);
        }

        if (list.isEmpty()) {
            return false;
        } else {
            list.sort(this.theNearestAttackableTargetSorter);
            this.targetEntity = list.get(0);
            return true;
        }
    }

    protected AABB getTargetableArea(double targetDistance) {
        return this.mob.getBoundingBox().inflate(targetDistance, 4.0, targetDistance);
    }

    @Override
    public void start() {
        // behaviour changed to the same as AmphitereAITargetItems
        this.mob.getMoveControl().setWantedPosition(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 0.25D);
        ((EntityPixie) this.mob).fetchingItem = true;

        LivingEntity attackTarget = this.mob.getTarget();
        if (attackTarget == null) {
            this.mob.getLookControl().setLookAt(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 180.0F, 20.0F);
        }
        super.start();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.targetEntity == null || !this.targetEntity.isAlive()) {
            this.stop();
            return;
        }
        // Keep flying to it: a pixie floats up all the time, aimed once it stayed above the item for good.
        this.mob.getMoveControl().setWantedPosition(this.targetEntity.getX(), this.targetEntity.getY() + 0.25D, this.targetEntity.getZ(), 0.5D);
        if (this.mob.distanceToSqr(this.targetEntity) < 2.25D) {
            EntityPixie pixie = (EntityPixie) this.mob;
            if (this.targetEntity.getItem() != null && this.targetEntity.getItem().getItem() != null)
                if (this.targetEntity.getItem().getItem() == Items.SUGAR) {
                    pixie.heal(5);
                } else if (this.targetEntity.getItem().getItem() == Items.CAKE) {
                    if (!pixie.isTame() && this.targetEntity.getOwner() != null && (this.targetEntity.getOwner() instanceof Player ownerPlayer ? ownerPlayer : null) != null) {
                        Player owner = (this.targetEntity.getOwner() instanceof Player ownerPlayer ? ownerPlayer : null);
                        pixie.setTame(true);
                        if (owner != null) {
                            pixie.tame(owner);
                        }
                        pixie.setPixieSitting(true);
                        pixie.setOnGround(true);  //  Entity.onGround = true
                    }
                }

            pixie.setItemInHand(InteractionHand.MAIN_HAND, this.targetEntity.getItem());
            this.targetEntity.getItem().shrink(1);
            pixie.playSound(IafSoundRegistry.PIXIE_TAUNT, 1F, 1F);
            stop();
        }
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetEntity != null && this.targetEntity.isAlive();
    }

    @Override
    public void stop() {
        ((EntityPixie) this.mob).fetchingItem = false;
        super.stop();
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