package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;


import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import com.iafabric.util.ItemNbt;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.enums.EnumBestiaryPages;
import com.google.common.primitives.Ints;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Set;

public class ItemBestiary extends Item {

    public ItemBestiary() {
        super(IafItemRegistry.props().stacksTo(1));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level craftLevel, Player playerIn) {
        ItemNbt.setTag(stack, new CompoundTag());
        ItemNbt.update(stack, nbtTag -> nbtTag.putIntArray("Pages", new int[]{0}));

    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemStackIn = playerIn.getItemInHand(handIn);
        if (worldIn.isClientSide) {
            IafMod.PROXY.openBestiaryGui(itemStackIn);
        }
        return InteractionResultHolder.pass(playerIn.getItemInHand(handIn));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level tickLevel, Entity entityIn, int slotId, boolean isSelected) {
        if (!(tickLevel instanceof ServerLevel worldIn)) {
            return;
        }
        EquipmentSlot slot = com.iafabric.compat.ItemSlots.slotOf(entityIn, stack, isSelected);
        if (!ItemNbt.hasTag(stack)) {
            ItemNbt.setTag(stack, new CompoundTag());
            ItemNbt.update(stack, nbtTag -> nbtTag.putIntArray("Pages", new int[]{EnumBestiaryPages.INTRODUCTION.ordinal()}));

        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        if (ItemNbt.hasTag(stack)) {
            if (IafMod.PROXY.shouldSeeBestiaryContents()) {
                tooltip.add(Component.translatable("bestiary.contains").withStyle(ChatFormatting.GRAY));
                final Set<EnumBestiaryPages> pages = EnumBestiaryPages
                    .containedPages(Ints.asList(ItemNbt.getOrEmpty(stack).getIntArray("Pages")));
                for (EnumBestiaryPages page : pages) {
                    tooltip.add(Component.literal(ChatFormatting.WHITE + "-").append(Component.translatable("bestiary." + EnumBestiaryPages.values()[page.ordinal()].toString().toLowerCase())).withStyle(ChatFormatting.GRAY));
                }
            } else {
                tooltip.add(Component.translatable("bestiary.hold_shift").withStyle(ChatFormatting.GRAY));
            }

        }
    }

}
