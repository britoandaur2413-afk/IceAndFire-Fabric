package com.iafabric.item;

import java.util.List;
import com.iafabric.pixie.OrbKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.function.Consumer;

/** A bonded pixie's armor: in its orb slot (pixie bonds menu), its player takes none of this kind of damage through it. */
public class ItemPixieOrb extends Item {
    public final OrbKind kind;

    public ItemPixieOrb(OrbKind kind) {
        super(IafItemRegistry.props().stacksTo(16));
        this.kind = kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.pixie_orb.desc", Component.translatable("pixie.iceandfire_fabric.orb_kind." + kind.ordinal())).withStyle(ChatFormatting.GRAY));
    }
}
