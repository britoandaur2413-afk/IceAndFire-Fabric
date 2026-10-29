package com.iafabric.enums;

import com.iafabric.block.BlockSeaSerpentScales;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.item.IafArmorMaterial;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemSeaSerpentArmor;
import com.iafabric.item.ItemSeaSerpentScales;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

public enum EnumSeaSerpent {
    BLUE(ChatFormatting.BLUE),
    BRONZE(ChatFormatting.GOLD),
    DEEPBLUE(ChatFormatting.DARK_BLUE),
    GREEN(ChatFormatting.DARK_GREEN),
    PURPLE(ChatFormatting.DARK_PURPLE),
    RED(ChatFormatting.DARK_RED),
    TEAL(ChatFormatting.AQUA);

    public String resourceName;
    public ChatFormatting color;
    public IafArmorMaterial armorMaterial;
    public Item scale;
    public Item helmet;
    public Item chestplate;
    public Item leggings;
    public Item boots;
    public Block scaleBlock;

    EnumSeaSerpent(ChatFormatting color) {
        this.resourceName = this.name().toLowerCase(Locale.ROOT);
        this.color = color;
        this.scaleBlock = IafBlockRegistry.register("sea_serpent_scale_block_%s".formatted(this.resourceName), () -> new BlockSeaSerpentScales(resourceName, color));
        //this.scaleBlock = new BlockSeaSerpentScales(this.resourceName, this.color);
    }


    public static void initArmors() {
        for (EnumSeaSerpent color : EnumSeaSerpent.values()) {
            color.armorMaterial = new IafArmorMaterial("iceandfire_fabric:sea_serpent_scales_" + color.resourceName, 30, new int[]{4, 8, 7, 4}, 25, SoundEvents.ARMOR_EQUIP_GOLD, 2.5F);
            color.scale = IafItemRegistry.register("sea_serpent_scales_" + color.resourceName, () ->
                new ItemSeaSerpentScales(color.resourceName, color.color));
            color.helmet = IafItemRegistry.register("tide_" + color.resourceName + "_helmet", () ->
                new ItemSeaSerpentArmor(color, color.armorMaterial, EquipmentSlot.HEAD));
            color.chestplate = IafItemRegistry.register("tide_" + color.resourceName + "_chestplate", () ->
                new ItemSeaSerpentArmor(color, color.armorMaterial, EquipmentSlot.CHEST));
            color.leggings = IafItemRegistry.register("tide_" + color.resourceName + "_leggings", () ->
                new ItemSeaSerpentArmor(color, color.armorMaterial, EquipmentSlot.LEGS));
            color.boots = IafItemRegistry.register("tide_" + color.resourceName + "_boots", () ->
                new ItemSeaSerpentArmor(color, color.armorMaterial, EquipmentSlot.FEET));
        }
    }
}
