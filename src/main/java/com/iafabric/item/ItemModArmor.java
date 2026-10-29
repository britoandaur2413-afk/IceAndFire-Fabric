package com.iafabric.item;




import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ItemModArmor extends IafArmorItem {

    public ItemModArmor(IafArmorMaterial material, EquipmentSlot slot) {
        super(material, slot, IafItemRegistry.props());
    }

        public String iafDescriptionId(ItemStack stack) {
        if (this == IafItemRegistry.EARPLUGS) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            if (calendar.get(Calendar.MONTH) + 1 == 4 && calendar.get(Calendar.DATE) == 1) {
                return "item.iceandfire_fabric.air_pods";
            }
        }
        return this.getDescriptionId();
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        if (this == IafItemRegistry.EARPLUGS) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            if (calendar.get(Calendar.MONTH) + 1 == 4 && calendar.get(Calendar.DATE) == 1) {
                tooltip.add(Component.translatable("item.iceandfire_fabric.air_pods.desc").withStyle(ChatFormatting.GREEN));
            }
        }
        super.appendHoverText(stack, context,tooltip, flagIn);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.iafDescriptionId(stack));
    }
}
