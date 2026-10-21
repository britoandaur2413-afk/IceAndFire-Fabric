package com.iafabric.item;




import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.iafabric.IafMod;
import com.iafabric.enums.EnumTroll;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.Locale;

public class ItemTrollArmor extends IafArmorItem{

    public EnumTroll troll;

    public ItemTrollArmor(EnumTroll troll, IafArmorMaterial material, EquipmentSlot slot) {
        super(material, slot, IafItemRegistry.props());
        this.troll = troll;
    }

    public static String getName(EnumTroll troll, EquipmentSlot slot) {
        return "%s_troll_leather_%s".formatted(troll.name().toLowerCase(Locale.ROOT), getArmorPart(slot));
    }



    private static String getArmorPart(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            default -> "";
        };
    }



    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        tooltip.accept(Component.translatable("item.iceandfire_fabric.troll_leather_armor_" + getArmorPart(slot) + ".desc").withStyle(ChatFormatting.GREEN));
    }
}
