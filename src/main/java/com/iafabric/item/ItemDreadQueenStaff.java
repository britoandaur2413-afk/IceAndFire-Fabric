package com.iafabric.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/**
 * Cryona's staff (with her sword, what she entrusts to who beats her): held by the rider or the owner of an ice
 * dragon, that dragon's ice freezes solid what it strikes, Frost Ward or not (boss/BossCold).
 */
public class ItemDreadQueenStaff extends Item {

    public ItemDreadQueenStaff() {
        super(IafItemRegistry.props().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("iceandfire_fabric.cryona_weapon.desc").withStyle(ChatFormatting.AQUA));
    }

    /** Whether someone holds one of Cryona's weapons (in either hand). */
    public static boolean holdsCryonasWeapon(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        for (ItemStack stack : new ItemStack[]{entity.getMainHandItem(), entity.getOffhandItem()}) {
            if (stack.is(IafItemRegistry.DREAD_QUEEN_STAFF) || stack.is(IafItemRegistry.DREAD_QUEEN_SWORD)) {
                return true;
            }
        }
        return false;
    }
}
