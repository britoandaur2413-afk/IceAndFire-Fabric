package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityDeathWormEgg;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemDeathwormEgg extends Item {
    private final boolean gigantic;

    public ItemDeathwormEgg(boolean gigantic) {
        super(IafItemRegistry.props().stacksTo(1));
        this.gigantic = gigantic;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);

        if (!playerIn.isCreative()) {
            itemstack.shrink(1);
        }

        worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (worldIn.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!worldIn.isClientSide) {
            EntityDeathWormEgg entityegg = new EntityDeathWormEgg(IafEntityRegistry.DEATH_WORM_EGG, playerIn,
                worldIn, gigantic);
            entityegg.shootFromRotation(playerIn, playerIn.getXRot(), playerIn.getYRot(), 0.0F, 1.5F, 1.0F);
            worldIn.addFreshEntity(entityegg);
        }

        return InteractionResultHolder.success(playerIn.getItemInHand(handIn));
    }
}
