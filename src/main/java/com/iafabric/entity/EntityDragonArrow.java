package com.iafabric.entity;

import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class EntityDragonArrow extends IafArrow {

    public EntityDragonArrow(EntityType<? extends AbstractArrow> typeIn, Level worldIn) {
        super(typeIn, worldIn);
        this.setBaseDamage(10);
    }

    public EntityDragonArrow(EntityType<? extends AbstractArrow> typeIn, double x, double y, double z,
                             Level world) {
        super(typeIn, x, y, z, world);
        this.setBaseDamage(10);
    }


        public EntityDragonArrow(EntityType<? extends AbstractArrow> typeIn, LivingEntity shooter, Level worldIn) {
        super(typeIn, shooter, worldIn);
        this.setBaseDamage(10.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tagCompoundTag) {
		ValueOutput tagCompound = new ValueOutput(tagCompoundTag, this.registryAccess());
        super.addAdditionalSaveData(tagCompoundTag);
        tagCompound.putDouble("damage", 10);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tagCompundTag) {
		ValueInput tagCompund = new ValueInput(tagCompundTag, this.registryAccess());
        super.readAdditionalSaveData(tagCompundTag);
        this.setBaseDamage(tagCompund.getDoubleOr("damage", 0.0D));
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(IafItemRegistry.DRAGONBONE_ARROW);
    }

}