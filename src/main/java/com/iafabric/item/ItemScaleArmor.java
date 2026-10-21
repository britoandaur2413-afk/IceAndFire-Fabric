package com.iafabric.item;



import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.iafabric.IafMod;
import com.iafabric.entity.DragonType;
import com.iafabric.enums.EnumDragonArmor;
import com.iafabric.enums.EnumDragonEgg;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import java.util.List;

public class ItemScaleArmor extends IafArmorItem implements IProtectAgainstDragonItem {

    public EnumDragonArmor armor_type;
    public EnumDragonEgg eggType;

    public ItemScaleArmor(EnumDragonEgg eggType, EnumDragonArmor armorType, IafArmorMaterial material, EquipmentSlot slot) {
        super(material, slot, IafItemRegistry.props());
        this.armor_type = armorType;
        this.eggType = eggType;
    }

        /** The shared translation key (getDescriptionId is final now). */
    public String iafDescriptionId() {
        switch (this.slot) {
            case HEAD:
                return "item.iceandfire_fabric.dragon_helmet";
            case CHEST:
                return "item.iceandfire_fabric.dragon_chestplate";
            case LEGS:
                return "item.iceandfire_fabric.dragon_leggings";
            case FEET:
                return "item.iceandfire_fabric.dragon_boots";
        }
        return "item.iceandfire_fabric.dragon_helmet";
    }




    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        tooltip.accept(Component.translatable("dragon." + eggType.toString().toLowerCase()).withStyle(eggType.color));
        tooltip.accept(Component.translatable("item.dragonscales_armor.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.iafDescriptionId());
    }
}
