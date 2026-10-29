package com.iafabric.item;



import java.util.function.Consumer;
import com.iafabric.IafMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemGeneric extends Item {
    int description = 0;

    public ItemGeneric() {
        super(IafItemRegistry.props());
    }

    public ItemGeneric(int textLength) {
        super(IafItemRegistry.props());
        this.description = textLength;
    }

    public ItemGeneric(int textLength, boolean hide) {
        super(hidden(IafItemRegistry.props()));
        this.description = textLength;
    }

    public ItemGeneric(int textLength, int stacksize) {
        super(IafItemRegistry.props().stacksTo(1));
        this.description = textLength;
    }

    private static Item.Properties hidden(Item.Properties properties) {
        IafItemRegistry.hideNext();
        return properties;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack stack) {
        if (this == IafItemRegistry.CREATIVE_DRAGON_MEAL) {
            return true;
        } else {
            return super.isFoil(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        if (description > 0) {
            for (int i = 0; i < description; i++) {
                tooltip.add(Component.translatable(this.getDescriptionId() + ".desc_" + i).withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
