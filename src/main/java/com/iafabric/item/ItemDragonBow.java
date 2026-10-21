package com.iafabric.item;

import com.iafabric.misc.IafTagRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The dragonbone bow: any arrow (it shot only dragonbone arrows, so with the game's arrows it did not even draw);
 * dragonbone arrows are still the ones that hit hardest.
 */
public class ItemDragonBow extends BowItem {
    public static final Predicate<ItemStack> DRAGON_ARROWS = (stack)
        -> stack.is(TagKey.create(Registries.ITEM, IafTagRegistry.DRAGON_ARROWS));

    public ItemDragonBow() {
        super(IafItemRegistry.props().durability(584));
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return DRAGON_ARROWS.or(ARROW_ONLY);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.iceandfire_fabric.dragonbone_bow.desc").withStyle(ChatFormatting.GRAY));
    }
}
