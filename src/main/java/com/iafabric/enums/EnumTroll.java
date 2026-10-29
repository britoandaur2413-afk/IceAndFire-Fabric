package com.iafabric.enums;

import com.iafabric.config.BiomeConfig;
import com.iafabric.item.IafArmorMaterial;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemTrollArmor;
import com.iafabric.item.ItemTrollLeather;
import com.iafabric.item.ItemTrollWeapon;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public enum EnumTroll {
    FOREST(IafItemRegistry.TROLL_FOREST_ARMOR_MATERIAL, Weapon.TRUNK, Weapon.COLUMN_FOREST, Weapon.AXE, Weapon.HAMMER),
    FROST(IafItemRegistry.TROLL_FROST_ARMOR_MATERIAL, Weapon.COLUMN_FROST, Weapon.TRUNK_FROST, Weapon.AXE, Weapon.HAMMER),
    MOUNTAIN(IafItemRegistry.TROLL_MOUNTAIN_ARMOR_MATERIAL, Weapon.COLUMN, Weapon.AXE, Weapon.HAMMER);

    private final Weapon[] weapons;
    public ResourceLocation TEXTURE;
    public ResourceLocation TEXTURE_STONE;
    public ResourceLocation TEXTURE_EYES;
    public IafArmorMaterial material;
    public Item leather;
    public Item helmet;
    public Item chestplate;
    public Item leggings;
    public Item boots;

    EnumTroll(IafArmorMaterial material, Weapon... weapons) {
        this.weapons = weapons;
        this.material = material;
        TEXTURE = new ResourceLocation("iceandfire_fabric:textures/models/troll/troll_" + this.name().toLowerCase(Locale.ROOT) + ".png");
        TEXTURE_STONE = new ResourceLocation("iceandfire_fabric:textures/models/troll/troll_" + this.name().toLowerCase(Locale.ROOT) + "_stone.png");
        TEXTURE_EYES = new ResourceLocation("iceandfire_fabric:textures/models/troll/troll_" + this.name().toLowerCase(Locale.ROOT) + "_eyes.png");


        //leather = IafItemRegistry.deferredRegister.register("troll_leather_" + name().toLowerCase(Locale.ROOT), () -> new ItemTrollLeather(this));

        //Function<EquipmentSlot, Item> genArmor = (slot) ->
        //        IafItemRegistry.deferredRegister.register(ItemTrollArmor.getName(this, slot), () -> new ItemTrollArmor(this, material, slot));
        //helmet = genArmor.apply(EquipmentSlot.HEAD);
        //chestplate = genArmor.apply(EquipmentSlot.CHEST);
        //leggings = genArmor.apply(EquipmentSlot.LEGS);
        //boots = genArmor.apply(EquipmentSlot.FEET);


    }

    public static EnumTroll getBiomeType(Holder<Biome> biome) {
        List<EnumTroll> types = new ArrayList<>();
        if (BiomeConfig.test(BiomeConfig.snowyTrollBiomes, biome)) {
            types.add(EnumTroll.FROST);
        }
        if (BiomeConfig.test(BiomeConfig.forestTrollBiomes, biome)) {
            types.add(EnumTroll.FOREST);
        }
        if (BiomeConfig.test(BiomeConfig.mountainTrollBiomes, biome)) {
            types.add(EnumTroll.MOUNTAIN);
        }
        if (types.isEmpty()) {
            return values()[ThreadLocalRandom.current().nextInt(values().length)];
        } else {
            return types.get(ThreadLocalRandom.current().nextInt(types.size()));
        }
    }


    public static Weapon getWeaponForType(EnumTroll troll) {
        return troll.weapons[ThreadLocalRandom.current().nextInt(troll.weapons.length)];
    }

    public static void initArmors() {
        for (EnumTroll troll: EnumTroll.values()) {
            troll.leather = IafItemRegistry.register("troll_leather_%s".formatted(troll.name().toLowerCase(Locale.ROOT)), () -> new ItemTrollLeather(troll));
            troll.helmet = IafItemRegistry.register(ItemTrollArmor.getName(troll, EquipmentSlot.HEAD), () -> new ItemTrollArmor(troll, troll.material, EquipmentSlot.HEAD));
            troll.chestplate = IafItemRegistry.register(ItemTrollArmor.getName(troll, EquipmentSlot.CHEST), () -> new ItemTrollArmor(troll, troll.material, EquipmentSlot.CHEST));
            troll.leggings = IafItemRegistry.register(ItemTrollArmor.getName(troll, EquipmentSlot.LEGS), () -> new ItemTrollArmor(troll, troll.material, EquipmentSlot.LEGS));
            troll.boots = IafItemRegistry.register(ItemTrollArmor.getName(troll, EquipmentSlot.FEET), () -> new ItemTrollArmor(troll, troll.material, EquipmentSlot.FEET));
        }
    }

    public enum Weapon {
        AXE, COLUMN, COLUMN_FOREST, COLUMN_FROST, HAMMER, TRUNK, TRUNK_FROST;
        public ResourceLocation TEXTURE;
        public Item item;

        Weapon() {
            TEXTURE = new ResourceLocation("iceandfire_fabric:textures/models/troll/weapon/weapon_" + this.name().toLowerCase(Locale.ROOT) + ".png");
            item = IafItemRegistry.register("troll_weapon_" + this.name().toLowerCase(Locale.ROOT), () -> new ItemTrollWeapon(this));
        }

    }
}
