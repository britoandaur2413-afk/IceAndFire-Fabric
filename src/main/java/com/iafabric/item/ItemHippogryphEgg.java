package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;


import com.iafabric.util.ItemNbt;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityHippogryphEgg;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.enums.EnumHippogryphTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ItemHippogryphEgg extends Item {

    public ItemHippogryphEgg() {
        super(IafItemRegistry.props().stacksTo(1));
    }

    public static ItemStack createEggStack(EnumHippogryphTypes parent1, EnumHippogryphTypes parent2) {
        EnumHippogryphTypes eggType = ThreadLocalRandom.current().nextBoolean() ? parent1 : parent2;
        ItemStack stack = new ItemStack(IafItemRegistry.HIPPOGRYPH_EGG);
        CompoundTag tag = new CompoundTag();
        tag.putInt("EggOrdinal", eggType.ordinal());
        ItemNbt.setTag(stack, tag);
        return stack;
    }



    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);

        if (!playerIn.isCreative()) {
            itemstack.shrink(1);
        }

        worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (worldIn.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!worldIn.isClientSide) {
            EntityHippogryphEgg entityegg = new EntityHippogryphEgg(IafEntityRegistry.HIPPOGRYPH_EGG, worldIn,
                playerIn, itemstack);
            entityegg.shootFromRotation(playerIn, playerIn.getXRot(), playerIn.getYRot(), 0.0F, 1.5F, 1.0F);
            worldIn.addFreshEntity(entityegg);
        }

        return InteractionResultHolder.success(playerIn.getItemInHand(handIn));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        CompoundTag tag = ItemNbt.getTag(stack);
        int eggOrdinal = 0;
        if (tag != null) {
            eggOrdinal = tag.iceandfire_fabric$getIntOr("EggOrdinal", 0);
        }

        String type = EnumHippogryphTypes.values()[Mth.clamp(eggOrdinal, 0, EnumHippogryphTypes.values().length - 1)].name().toLowerCase();
        tooltip.add(Component.translatable("entity.iceandfire_fabric.hippogryph." + type).withStyle(ChatFormatting.GRAY));
    }
}
