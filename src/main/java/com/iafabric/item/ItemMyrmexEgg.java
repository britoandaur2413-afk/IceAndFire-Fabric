package com.iafabric.item;



import com.iafabric.util.ItemNbt;
import net.minecraft.core.component.DataComponents;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityMyrmexEgg;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemMyrmexEgg extends Item {

    boolean isJungle;

    public ItemMyrmexEgg(boolean isJungle) {
        super(IafItemRegistry.props().stacksTo(1));
        this.isJungle = isJungle;
    }


    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        String caste;
        CompoundTag tag = ItemNbt.getTag(stack);
        int eggOrdinal = 0;
        if (tag != null) {
            eggOrdinal = tag.iceandfire_fabric$getIntOr("EggOrdinal", 0);
        }
        switch (eggOrdinal) {
            default:
                caste = "worker";
                break;
            case 1:
                caste = "soldier";
                break;
            case 2:
                caste = "royal";
                break;
            case 3:
                caste = "sentinel";
                break;
            case 4:
                caste = "queen";
        }
        if (eggOrdinal == 4) {
            tooltip.add(Component.translatable("myrmex.caste_" + caste + ".name").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            tooltip.add(Component.translatable("myrmex.caste_" + caste + ".name").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemstack = context.getPlayer().getItemInHand(context.getHand());
        BlockPos offset = context.getClickedPos().relative(context.getClickedFace());
        EntityMyrmexEgg egg = new EntityMyrmexEgg(IafEntityRegistry.MYRMEX_EGG, context.getLevel());
        CompoundTag tag = ItemNbt.getTag(itemstack);
        int eggOrdinal = 0;
        if (tag != null) {
            eggOrdinal = tag.iceandfire_fabric$getIntOr("EggOrdinal", 0);
        }
        egg.setJungle(isJungle);
        egg.setMyrmexCaste(eggOrdinal);
        egg.moveTo(offset.getX() + 0.5, offset.getY(), offset.getZ() + 0.5, 0, 0);
        egg.onPlayerPlace(context.getPlayer());
        if (itemstack.has(DataComponents.CUSTOM_NAME)) {
            egg.setCustomName(itemstack.getHoverName());
        }
        if (!context.getLevel().isClientSide) {
            context.getLevel().addFreshEntity(egg);
        }
        itemstack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag tag = ItemNbt.getTag(stack);
        int eggOrdinal = 0;
        if (tag != null) {
            eggOrdinal = tag.iceandfire_fabric$getIntOr("EggOrdinal", 0);
        }
        return super.isFoil(stack) || eggOrdinal == 4;
    }
}
