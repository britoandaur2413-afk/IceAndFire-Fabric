package com.iafabric.item;

import org.jetbrains.annotations.Nullable;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonArrow;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemDragonArrow extends ArrowItem {
    public ItemDragonArrow() {
        super(IafItemRegistry.props());
    }

    @Override
    public AbstractArrow createArrow(Level worldIn, ItemStack stack, LivingEntity shooter) {
        EntityDragonArrow arrowentity = new EntityDragonArrow(IafEntityRegistry.DRAGON_ARROW, shooter, worldIn);
        return arrowentity;
    }

}
