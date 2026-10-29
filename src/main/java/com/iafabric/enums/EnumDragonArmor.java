package com.iafabric.enums;

import com.iafabric.item.IafArmorMaterial;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemScaleArmor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;

public enum EnumDragonArmor {

    armor_red(12, EnumDragonEgg.RED),
    armor_bronze(13, EnumDragonEgg.BRONZE),
    armor_green(14, EnumDragonEgg.GREEN),
    armor_gray(15, EnumDragonEgg.GRAY),
    armor_blue(12, EnumDragonEgg.BLUE),
    armor_white(13, EnumDragonEgg.WHITE),
    armor_sapphire(14, EnumDragonEgg.SAPPHIRE),
    armor_silver(15, EnumDragonEgg.SILVER),
    armor_electric(12, EnumDragonEgg.ELECTRIC),
    armor_amythest(13, EnumDragonEgg.AMYTHEST),
    armor_copper(14, EnumDragonEgg.COPPER),
    armor_black(15, EnumDragonEgg.BLACK);

    public IafArmorMaterial material;
    public int armorId;
    public EnumDragonEgg eggType;
    public Item helmet;
    public Item chestplate;
    public Item leggings;
    public Item boots;
    public IafArmorMaterial armorMaterial;

    EnumDragonArmor(int armorId, EnumDragonEgg eggType) {
        this.armorId = armorId;
        this.eggType = eggType;
    }

    public static void initArmors() {
        for (int i = 0; i < EnumDragonArmor.values().length; i++) {
            EnumDragonArmor.values()[i].armorMaterial = new IafArmorMaterial("iceandfire_fabric:armor_dragon_scales" + (i + 1), 36, new int[]{5, 7, 9, 5}, 15, SoundEvents.ARMOR_EQUIP_CHAIN, 2);
            String sub = EnumDragonArmor.values()[i].name();

            int finalI = i;
            EnumDragonArmor.values()[finalI].helmet = IafItemRegistry.register(sub + "_helmet", () ->
                new ItemScaleArmor(EnumDragonArmor.values()[finalI].eggType, EnumDragonArmor.values()[finalI], EnumDragonArmor.values()[finalI].armorMaterial, EquipmentSlot.HEAD));
            EnumDragonArmor.values()[finalI].chestplate = IafItemRegistry.register(sub + "_chestplate", () ->
                new ItemScaleArmor(EnumDragonArmor.values()[finalI].eggType, EnumDragonArmor.values()[finalI], EnumDragonArmor.values()[finalI].armorMaterial, EquipmentSlot.CHEST));
            EnumDragonArmor.values()[finalI].leggings = IafItemRegistry.register(sub + "_leggings", () ->
                new ItemScaleArmor(EnumDragonArmor.values()[finalI].eggType, EnumDragonArmor.values()[finalI], EnumDragonArmor.values()[finalI].armorMaterial, EquipmentSlot.LEGS));
            EnumDragonArmor.values()[finalI].boots = IafItemRegistry.register(sub + "_boots", () ->
                new ItemScaleArmor(EnumDragonArmor.values()[finalI].eggType, EnumDragonArmor.values()[finalI], EnumDragonArmor.values()[finalI].armorMaterial, EquipmentSlot.FEET));
        }
    }

    public static Item getScaleItem(EnumDragonArmor armor) {
        switch (armor) {
            case armor_red:
                return IafItemRegistry.DRAGONSCALES_RED;
            case armor_bronze:
                return IafItemRegistry.DRAGONSCALES_BRONZE;
            case armor_green:
                return IafItemRegistry.DRAGONSCALES_GREEN;
            case armor_gray:
                return IafItemRegistry.DRAGONSCALES_GRAY;
            case armor_blue:
                return IafItemRegistry.DRAGONSCALES_BLUE;
            case armor_white:
                return IafItemRegistry.DRAGONSCALES_WHITE;
            case armor_sapphire:
                return IafItemRegistry.DRAGONSCALES_SAPPHIRE;
            case armor_silver:
                return IafItemRegistry.DRAGONSCALES_SILVER;
            case armor_electric:
                return IafItemRegistry.DRAGONSCALES_ELECTRIC;
            case armor_amythest:
                return IafItemRegistry.DRAGONSCALES_AMYTHEST;
            case armor_copper:
                return IafItemRegistry.DRAGONSCALES_COPPER;
            case armor_black:
                return IafItemRegistry.DRAGONSCALES_BLACK;
            default:
                return IafItemRegistry.DRAGONSCALES_RED;
        }
    }
}
