package com.iafabric.item;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.block.INoTab;
import com.iafabric.block.IWallBlock;
import com.iafabric.block.BlockGhostChest;
import com.iafabric.block.BlockDreadPortal;
import com.iafabric.block.BlockPixieHouse;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.enums.*;
import com.iafabric.recipe.IafRecipeRegistry;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallTorchBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class IafItemRegistry {
    /** Items shown in the mod's item tab, in registration order. */
    public static final List<Item> TAB_ITEMS = new ArrayList<>();
    /** Block items shown in the mod's block tab. */
    public static final List<Item> TAB_BLOCKS = new ArrayList<>();
    private static ResourceKey<Item> pending;
    private static boolean hidden;

    public static final IafArmorMaterial SILVER_ARMOR_MATERIAL = new IafArmorMaterial("silver", 15, new int[]{1, 4, 5, 2}, 20, SoundEvents.ARMOR_EQUIP_CHAIN, 0);
    public static final IafArmorMaterial BLINDFOLD_ARMOR_MATERIAL = new IafArmorMaterial("blindfold", 5, new int[]{1, 1, 1, 1}, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 0);
    public static final IafArmorMaterial SHEEP_ARMOR_MATERIAL = new IafArmorMaterial("sheep", 5, new int[]{1, 3, 2, 1}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0);
    public static final IafArmorMaterial MYRMEX_DESERT_ARMOR_MATERIAL = new IafArmorMaterial("myrmexdesert", 20, new int[]{3, 5, 8, 4}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0);
    public static final IafArmorMaterial MYRMEX_JUNGLE_ARMOR_MATERIAL = new IafArmorMaterial("myrmexjungle", 20, new int[]{3, 5, 8, 4}, 15, SoundEvents.ARMOR_EQUIP_LEATHER, 0);
    public static final IafArmorMaterial EARPLUGS_ARMOR_MATERIAL = new IafArmorMaterial("earplugs", 5, new int[]{1, 1, 1, 1}, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 0);
    public static final IafArmorMaterial DEATHWORM_0_ARMOR_MATERIAL = new IafArmorMaterial("yellow seathworm", 15, new int[]{2, 5, 7, 3}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 1.5F);
    public static final IafArmorMaterial DEATHWORM_1_ARMOR_MATERIAL = new IafArmorMaterial("white seathworm", 15, new int[]{2, 5, 7, 3}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 1.5F);
    public static final IafArmorMaterial DEATHWORM_2_ARMOR_MATERIAL = new IafArmorMaterial("red deathworm", 15, new int[]{2, 5, 7, 3}, 5, SoundEvents.ARMOR_EQUIP_LEATHER, 1.5F);
    public static final IafArmorMaterial TROLL_MOUNTAIN_ARMOR_MATERIAL = new IafArmorMaterial("mountain troll", 20, new int[]{2, 5, 7, 3}, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 1F);
    public static final IafArmorMaterial TROLL_FOREST_ARMOR_MATERIAL = new IafArmorMaterial("forest troll", 20, new int[]{2, 5, 7, 3}, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 1F);
    public static final IafArmorMaterial TROLL_FROST_ARMOR_MATERIAL = new IafArmorMaterial("frost troll", 20, new int[]{2, 5, 7, 3}, 10, SoundEvents.ARMOR_EQUIP_LEATHER, 1F);
    public static final IafArmorMaterial DRAGONSTEEL_FIRE_ARMOR_MATERIAL = new IafArmorMaterial("dragonsteel_fire", (int) (0.02D * IafConfig.dragonsteelBaseDurabilityEquipment), new int[]{IafConfig.dragonsteelBaseArmor - 6, IafConfig.dragonsteelBaseArmor - 3, IafConfig.dragonsteelBaseArmor, IafConfig.dragonsteelBaseArmor - 5}, 30, SoundEvents.ARMOR_EQUIP_DIAMOND, 6.0F);
    public static final IafArmorMaterial DRAGONSTEEL_ICE_ARMOR_MATERIAL = new IafArmorMaterial("dragonsteel_ice", (int) (0.02D * IafConfig.dragonsteelBaseDurabilityEquipment), new int[]{IafConfig.dragonsteelBaseArmor - 6, IafConfig.dragonsteelBaseArmor - 3, IafConfig.dragonsteelBaseArmor, IafConfig.dragonsteelBaseArmor - 5}, 30, SoundEvents.ARMOR_EQUIP_DIAMOND, 6.0F);
    public static final IafArmorMaterial DRAGONSTEEL_LIGHTNING_ARMOR_MATERIAL = new IafArmorMaterial("dragonsteel_lightning", (int) (0.02D * IafConfig.dragonsteelBaseDurabilityEquipment), new int[]{IafConfig.dragonsteelBaseArmor - 6, IafConfig.dragonsteelBaseArmor - 3, IafConfig.dragonsteelBaseArmor, IafConfig.dragonsteelBaseArmor - 5}, 30, SoundEvents.ARMOR_EQUIP_DIAMOND, 6.0F);
    public static final IafToolMaterial DRAGONSTEEL_TIER_FIRE = new IafToolMaterial("dragonsteel_fire", 5, IafConfig.dragonsteelBaseDurability, (float) IafConfig.dragonsteelBaseDamage - 4.0F, 10.0F, 21);
    public static final IafToolMaterial DRAGONSTEEL_TIER_ICE = new IafToolMaterial("dragonsteel_ice", 5, IafConfig.dragonsteelBaseDurability, (float) IafConfig.dragonsteelBaseDamage - 4.0F, 10.0F, 21);
    public static final IafToolMaterial DRAGONSTEEL_TIER_LIGHTNING = new IafToolMaterial("dragonsteel_lightning", 5, IafConfig.dragonsteelBaseDurability, (float) IafConfig.dragonsteelBaseDamage - 4.0F, 10.0F, 21);
    public static final IafToolMaterial DRAGONSTEEL_TIER_DREAD_QUEEN = new IafToolMaterial("dread_queen", 5, IafConfig.dragonsteelBaseDurability, (float) IafConfig.dragonsteelBaseDamage - 4.0F, 10.0F, 21);
    public static final IafToolMaterial SILVER_TOOL_MATERIAL = new IafToolMaterial("silver", 2, 460, 1.0F, 11.0F, 18);
    public static final IafToolMaterial DRAGONBONE_TOOL_MATERIAL = new IafToolMaterial("Dragonbone", 4, 1660, 4.0F, 10.0F, 22);
    public static final IafToolMaterial FIRE_DRAGONBONE_TOOL_MATERIAL = new IafToolMaterial("FireDragonbone", 4, 2000, 5.5F, 10F, 22);
    public static final IafToolMaterial ICE_DRAGONBONE_TOOL_MATERIAL = new IafToolMaterial("IceDragonbone", 4, 2000, 5.5F, 10F, 22);
    public static final IafToolMaterial LIGHTNING_DRAGONBONE_TOOL_MATERIAL = new IafToolMaterial("LightningDragonbone", 4, 2000, 5.5F, 10F, 22);
    public static final IafToolMaterial TROLL_WEAPON_TOOL_MATERIAL = new IafToolMaterial("trollWeapon", 2, 300, 1F, 10F, 1);
    public static final IafToolMaterial MYRMEX_CHITIN_TOOL_MATERIAL = new IafToolMaterial("MyrmexChitin", 3, 600, 1.0F, 6.0F, 8);
    public static final IafToolMaterial HIPPOGRYPH_SWORD_TOOL_MATERIAL = new IafToolMaterial("HippogryphSword", 2, 500, 2.5F, 10F, 10);
    public static final IafToolMaterial STYMHALIAN_SWORD_TOOL_MATERIAL = new IafToolMaterial("StymphalianSword", 2, 500, 2, 10.0F, 10);
    public static final IafToolMaterial AMPHITHERE_SWORD_TOOL_MATERIAL = new IafToolMaterial("AmphithereSword", 2, 500, 1F, 10F, 10);
    public static final IafToolMaterial HIPPOCAMPUS_SWORD_TOOL_MATERIAL = new IafToolMaterial("HippocampusSword", 0, 500, -2F, 0F, 50);
    public static final IafToolMaterial DREAD_SWORD_TOOL_MATERIAL = new IafToolMaterial("DreadSword", 0, 100, 1F, 10F, 0);
    public static final IafToolMaterial DREAD_KNIGHT_TOOL_MATERIAL = new IafToolMaterial("DreadKnightSword", 0, 1200, 13F, 0F, 10);
    public static final IafToolMaterial GHOST_SWORD_TOOL_MATERIAL = new IafToolMaterial("GhostSword", 2, 3000, 5, 10.0F, 25);

    public static final Item BESTIARY = register("bestiary", () -> new ItemBestiary());
    public static final Item MANUSCRIPT = register("manuscript", () -> new ItemGeneric());
    public static final Item SAPPHIRE_GEM = register("sapphire_gem", () -> new ItemGeneric());
    public static final Item SILVER_INGOT = register("silver_ingot", () -> new ItemGeneric());
    public static final Item RIDER_SEAL = register("rider_seal", () -> new ItemRiderSeal());
    public static final Item PIXIE_ORB_PHYSICAL = register("pixie_orb_physical", () -> new ItemPixieOrb(com.iafabric.pixie.OrbKind.PHYSICAL));
    public static final Item PIXIE_ORB_PROJECTILE = register("pixie_orb_projectile", () -> new ItemPixieOrb(com.iafabric.pixie.OrbKind.PROJECTILE));
    public static final Item PIXIE_ORB_MAGIC = register("pixie_orb_magic", () -> new ItemPixieOrb(com.iafabric.pixie.OrbKind.MAGIC));
    public static final Item PIXIE_ORB_FIRE = register("pixie_orb_fire", () -> new ItemPixieOrb(com.iafabric.pixie.OrbKind.FIRE));
    public static final Item PIXIE_ORB_WATER = register("pixie_orb_water", () -> new ItemPixieOrb(com.iafabric.pixie.OrbKind.WATER));
    public static final Item SILVER_NUGGET = register("silver_nugget", () -> new ItemGeneric());
    public static final Item AMYTHEST_GEM = register("amythest_gem", () -> new ItemGeneric());
    public static final Item SILVER_HELMET = register("armor_silver_metal_helmet", () -> new ItemSilverArmor(SILVER_ARMOR_MATERIAL, EquipmentSlot.HEAD));
    public static final Item SILVER_CHESTPLATE = register("armor_silver_metal_chestplate", () -> new ItemSilverArmor(SILVER_ARMOR_MATERIAL, EquipmentSlot.CHEST));
    public static final Item SILVER_LEGGINGS = register("armor_silver_metal_leggings", () -> new ItemSilverArmor(SILVER_ARMOR_MATERIAL, EquipmentSlot.LEGS));
    public static final Item SILVER_BOOTS = register("armor_silver_metal_boots", () -> new ItemSilverArmor(SILVER_ARMOR_MATERIAL, EquipmentSlot.FEET));
    public static final Item SILVER_SWORD = register("silver_sword", () -> new IafToolItem(SILVER_TOOL_MATERIAL, IafToolItem.Kind.SWORD));
    public static final Item SILVER_SHOVEL = register("silver_shovel", () -> new IafToolItem(SILVER_TOOL_MATERIAL, IafToolItem.Kind.SHOVEL));
    public static final Item SILVER_PICKAXE = register("silver_pickaxe", () -> new IafToolItem(SILVER_TOOL_MATERIAL, IafToolItem.Kind.PICKAXE));
    public static final Item SILVER_AXE = register("silver_axe", () -> new IafToolItem(SILVER_TOOL_MATERIAL, IafToolItem.Kind.AXE));
    public static final Item SILVER_HOE = register("silver_hoe", () -> new IafToolItem(SILVER_TOOL_MATERIAL, IafToolItem.Kind.HOE));
    public static final Item FIRE_STEW = register("fire_stew", () -> new ItemGeneric());
    public static final Item FROST_STEW = register("frost_stew", () -> new ItemGeneric());
    public static final Item LIGHTNING_STEW = register("lightning_stew", () -> new ItemGeneric());
    public static final Item DRAGONEGG_RED = register("dragonegg_red", () -> new ItemDragonEgg(EnumDragonEgg.RED));
    public static final Item DRAGONEGG_GREEN = register("dragonegg_green", () -> new ItemDragonEgg(EnumDragonEgg.GREEN));
    public static final Item DRAGONEGG_BRONZE = register("dragonegg_bronze", () -> new ItemDragonEgg(EnumDragonEgg.BRONZE));
    public static final Item DRAGONEGG_GRAY = register("dragonegg_gray", () -> new ItemDragonEgg(EnumDragonEgg.GRAY));
    public static final Item DRAGONEGG_BLUE = register("dragonegg_blue", () -> new ItemDragonEgg(EnumDragonEgg.BLUE));
    public static final Item DRAGONEGG_WHITE = register("dragonegg_white", () -> new ItemDragonEgg(EnumDragonEgg.WHITE));
    public static final Item DRAGONEGG_SAPPHIRE = register("dragonegg_sapphire", () -> new ItemDragonEgg(EnumDragonEgg.SAPPHIRE));
    public static final Item DRAGONEGG_SILVER = register("dragonegg_silver", () -> new ItemDragonEgg(EnumDragonEgg.SILVER));
    public static final Item DRAGONEGG_ELECTRIC = register("dragonegg_electric", () -> new ItemDragonEgg(EnumDragonEgg.ELECTRIC));
    public static final Item DRAGONEGG_AMYTHEST = register("dragonegg_amythest", () -> new ItemDragonEgg(EnumDragonEgg.AMYTHEST));
    public static final Item DRAGONEGG_COPPER = register("dragonegg_copper", () -> new ItemDragonEgg(EnumDragonEgg.COPPER));
    public static final Item DRAGONEGG_BLACK = register("dragonegg_black", () -> new ItemDragonEgg(EnumDragonEgg.BLACK));
    public static final Item DRAGONSCALES_RED = register("dragonscales_red", () -> new ItemDragonScales(EnumDragonEgg.RED));
    public static final Item DRAGONSCALES_GREEN = register("dragonscales_green", () -> new ItemDragonScales(EnumDragonEgg.GREEN));
    public static final Item DRAGONSCALES_BRONZE = register("dragonscales_bronze", () -> new ItemDragonScales(EnumDragonEgg.BRONZE));
    public static final Item DRAGONSCALES_GRAY = register("dragonscales_gray", () -> new ItemDragonScales(EnumDragonEgg.GRAY));
    public static final Item DRAGONSCALES_BLUE = register("dragonscales_blue", () -> new ItemDragonScales(EnumDragonEgg.BLUE));
    public static final Item DRAGONSCALES_WHITE = register("dragonscales_white", () -> new ItemDragonScales(EnumDragonEgg.WHITE));
    public static final Item DRAGONSCALES_SAPPHIRE = register("dragonscales_sapphire", () -> new ItemDragonScales(EnumDragonEgg.SAPPHIRE));
    public static final Item DRAGONSCALES_SILVER = register("dragonscales_silver", () -> new ItemDragonScales(EnumDragonEgg.SILVER));
    public static final Item DRAGONSCALES_ELECTRIC = register("dragonscales_electric", () -> new ItemDragonScales(EnumDragonEgg.ELECTRIC));
    public static final Item DRAGONSCALES_AMYTHEST = register("dragonscales_amythest", () -> new ItemDragonScales(EnumDragonEgg.AMYTHEST));
    public static final Item DRAGONSCALES_COPPER = register("dragonscales_copper", () -> new ItemDragonScales(EnumDragonEgg.COPPER));
    public static final Item DRAGONSCALES_BLACK = register("dragonscales_black", () -> new ItemDragonScales(EnumDragonEgg.BLACK));
    public static final Item DRAGON_BONE = register("dragonbone", () -> new ItemDragonBone());
    public static final Item WITHERBONE = register("witherbone", () -> new ItemGeneric());
    public static final Item FISHING_SPEAR = register("fishing_spear", () -> new ItemFishingSpear());
    public static final Item WITHER_SHARD = register("wither_shard", () -> new ItemGeneric());
    public static final Item DRAGONBONE_SWORD = register("dragonbone_sword", () -> new IafToolItem(DRAGONBONE_TOOL_MATERIAL, IafToolItem.Kind.SWORD));
    public static final Item DRAGONBONE_SHOVEL = register("dragonbone_shovel", () -> new IafToolItem(DRAGONBONE_TOOL_MATERIAL, IafToolItem.Kind.SHOVEL));
    public static final Item DRAGONBONE_PICKAXE = register("dragonbone_pickaxe", () -> new IafToolItem(DRAGONBONE_TOOL_MATERIAL, IafToolItem.Kind.PICKAXE));
    public static final Item DRAGONBONE_AXE = register("dragonbone_axe", () -> new IafToolItem(DRAGONBONE_TOOL_MATERIAL, IafToolItem.Kind.AXE));
    public static final Item DRAGONBONE_HOE = register("dragonbone_hoe", () -> new IafToolItem(DRAGONBONE_TOOL_MATERIAL, IafToolItem.Kind.HOE));
    public static final Item DRAGONBONE_SWORD_FIRE = register("dragonbone_sword_fire", () -> new ItemAlchemySword(FIRE_DRAGONBONE_TOOL_MATERIAL));
    public static final Item DRAGONBONE_SWORD_ICE = register("dragonbone_sword_ice", () -> new ItemAlchemySword(ICE_DRAGONBONE_TOOL_MATERIAL));
    public static final Item DRAGONBONE_SWORD_LIGHTNING = register("dragonbone_sword_lightning", () -> new ItemAlchemySword(LIGHTNING_DRAGONBONE_TOOL_MATERIAL));
    public static final Item DRAGONBONE_ARROW = register("dragonbone_arrow", () -> new ItemDragonArrow());
    public static final Item DRAGON_BOW = register("dragonbone_bow", () -> new ItemDragonBow());
    public static final Item DRAGON_SKULL_FIRE = register(ItemDragonSkull.getName(0), () -> new ItemDragonSkull(0));
    public static final Item DRAGON_SKULL_ICE = register(ItemDragonSkull.getName(1), () -> new ItemDragonSkull(1));
    public static final Item DRAGON_SKULL_LIGHTNING = register(ItemDragonSkull.getName(2), () -> new ItemDragonSkull(2));
    public static final ItemDragonArmor DRAGONARMOR_IRON_0 = register("dragonarmor_iron_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.IRON, 0));
    public static final ItemDragonArmor DRAGONARMOR_IRON_1 = register("dragonarmor_iron_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.IRON, 1));
    public static final ItemDragonArmor DRAGONARMOR_IRON_2 = register("dragonarmor_iron_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.IRON, 2));
    public static final ItemDragonArmor DRAGONARMOR_IRON_3 = register("dragonarmor_iron_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.IRON, 3));
    public static final ItemDragonArmor DRAGONARMOR_COPPER_0 = register("dragonarmor_copper_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.COPPER, 0));
    public static final ItemDragonArmor DRAGONARMOR_COPPER_1 = register("dragonarmor_copper_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.COPPER, 1));
    public static final ItemDragonArmor DRAGONARMOR_COPPER_2 = register("dragonarmor_copper_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.COPPER, 2));
    public static final ItemDragonArmor DRAGONARMOR_COPPER_3 = register("dragonarmor_copper_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.COPPER, 3));
    public static final ItemDragonArmor DRAGONARMOR_GOLD_0 = register("dragonarmor_gold_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.GOLD, 0));
    public static final ItemDragonArmor DRAGONARMOR_GOLD_1 = register("dragonarmor_gold_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.GOLD, 1));
    public static final ItemDragonArmor DRAGONARMOR_GOLD_2 = register("dragonarmor_gold_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.GOLD, 2));
    public static final ItemDragonArmor DRAGONARMOR_GOLD_3 = register("dragonarmor_gold_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.GOLD, 3));
    public static final ItemDragonArmor DRAGONARMOR_DIAMOND_0 = register("dragonarmor_diamond_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.DIAMOND, 0));
    public static final ItemDragonArmor DRAGONARMOR_DIAMOND_1 = register("dragonarmor_diamond_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.DIAMOND, 1));
    public static final ItemDragonArmor DRAGONARMOR_DIAMOND_2 = register("dragonarmor_diamond_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.DIAMOND, 2));
    public static final ItemDragonArmor DRAGONARMOR_DIAMOND_3 = register("dragonarmor_diamond_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.DIAMOND, 3));
    public static final ItemDragonArmor DRAGONARMOR_SILVER_0 = register("dragonarmor_silver_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.SILVER, 0));
    public static final ItemDragonArmor DRAGONARMOR_SILVER_1 = register("dragonarmor_silver_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.SILVER, 1));
    public static final ItemDragonArmor DRAGONARMOR_SILVER_2 = register("dragonarmor_silver_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.SILVER, 2));
    public static final ItemDragonArmor DRAGONARMOR_SILVER_3 = register("dragonarmor_silver_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.SILVER, 3));
    public static final ItemDragonArmor DRAGONARMOR_NETHERITE_0 = register("dragonarmor_netherite_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.NETHERITE, 0));
    public static final ItemDragonArmor DRAGONARMOR_NETHERITE_1 = register("dragonarmor_netherite_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.NETHERITE, 1));
    public static final ItemDragonArmor DRAGONARMOR_NETHERITE_2 = register("dragonarmor_netherite_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.NETHERITE, 2));
    public static final ItemDragonArmor DRAGONARMOR_NETHERITE_3 = register("dragonarmor_netherite_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.NETHERITE, 3));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_FIRE_0 = register("dragonarmor_dragonsteel_fire_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.FIRE, 0));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_FIRE_1 = register("dragonarmor_dragonsteel_fire_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.FIRE, 1));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_FIRE_2 = register("dragonarmor_dragonsteel_fire_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.FIRE, 2));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_FIRE_3 = register("dragonarmor_dragonsteel_fire_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.FIRE, 3));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_ICE_0 = register("dragonarmor_dragonsteel_ice_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.ICE, 0));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_ICE_1 = register("dragonarmor_dragonsteel_ice_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.ICE, 1));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_ICE_2 = register("dragonarmor_dragonsteel_ice_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.ICE, 2));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_ICE_3 = register("dragonarmor_dragonsteel_ice_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.ICE, 3));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_LIGHTNING_0 = register("dragonarmor_dragonsteel_lightning_" + ItemDragonArmor.getNameForSlot(0), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.LIGHTNING, 0));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_LIGHTNING_1 = register("dragonarmor_dragonsteel_lightning_" + ItemDragonArmor.getNameForSlot(1), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.LIGHTNING, 1));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_LIGHTNING_2 = register("dragonarmor_dragonsteel_lightning_" + ItemDragonArmor.getNameForSlot(2), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.LIGHTNING, 2));
    public static final ItemDragonArmor DRAGONARMOR_DRAGONSTEEL_LIGHTNING_3 = register("dragonarmor_dragonsteel_lightning_" + ItemDragonArmor.getNameForSlot(3), () -> new ItemDragonArmor(ItemDragonArmor.DragonArmorType.LIGHTNING, 3));
    public static final Item DRAGON_MEAL = register("dragon_meal", () -> new ItemGeneric());
    public static final Item SICKLY_DRAGON_MEAL = register("sickly_dragon_meal", () -> new ItemGeneric(1));
    public static final Item CREATIVE_DRAGON_MEAL = register("creative_dragon_meal", () -> new ItemGeneric(2));
    public static final Item FIRE_DRAGON_FLESH = register(ItemDragonFlesh.getNameForType(0), () -> new ItemDragonFlesh(0));
    public static final Item ICE_DRAGON_FLESH = register(ItemDragonFlesh.getNameForType(1), () -> new ItemDragonFlesh(1));
    public static final Item LIGHTNING_DRAGON_FLESH = register(ItemDragonFlesh.getNameForType(2), () -> new ItemDragonFlesh(2));
    public static final Item FIRE_DRAGON_HEART = register("fire_dragon_heart", () -> new ItemGeneric());
    public static final Item ICE_DRAGON_HEART = register("ice_dragon_heart", () -> new ItemGeneric());
    public static final Item LIGHTNING_DRAGON_HEART = register("lightning_dragon_heart", () -> new ItemGeneric());
    public static final Item FIRE_DRAGON_BLOOD = register("fire_dragon_blood", () -> new ItemGeneric());
    public static final Item ICE_DRAGON_BLOOD = register("ice_dragon_blood", () -> new ItemGeneric());
    public static final Item LIGHTNING_DRAGON_BLOOD = register("lightning_dragon_blood", () -> new ItemGeneric());
    public static final Item DRAGON_STAFF = register("dragon_stick", () -> new ItemDragonStaff());
    public static final Item DRAGON_HORN = register("dragon_horn", () -> new ItemDragonHorn());
    public static final Item DRAGON_FLUTE = register("dragon_flute", () -> new ItemDragonFlute());
    public static final Item SUMMONING_CRYSTAL_FIRE = register("summoning_crystal_fire", () -> new ItemSummoningCrystal());
    public static final Item SUMMONING_CRYSTAL_ICE = register("summoning_crystal_ice", () -> new ItemSummoningCrystal());
    public static final Item SUMMONING_CRYSTAL_LIGHTNING = register("summoning_crystal_lightning", () -> new ItemSummoningCrystal());
    public static final Item HIPPOGRYPH_EGG = register("hippogryph_egg", () -> new ItemHippogryphEgg());
    public static final Item IRON_HIPPOGRYPH_ARMOR = register("iron_hippogryph_armor", () -> new ItemGeneric(0, 1));
    public static final Item GOLD_HIPPOGRYPH_ARMOR = register("gold_hippogryph_armor", () -> new ItemGeneric(0, 1));
    public static final Item DIAMOND_HIPPOGRYPH_ARMOR = register("diamond_hippogryph_armor", () -> new ItemGeneric(0, 1));
    public static final Item HIPPOGRYPH_TALON = register("hippogryph_talon", () -> new ItemGeneric(1));
    public static final Item HIPPOGRYPH_SWORD = register("hippogryph_sword", () -> new ItemHippogryphSword());
    public static final Item GORGON_HEAD = register("gorgon_head", () -> new ItemGorgonHead());
    public static final Item STONE_STATUE = register("stone_statue", () -> new ItemStoneStatue());
    public static final Item BLINDFOLD = register("blindfold", () -> new ItemBlindfold());
    public static final Item PIXIE_DUST = register("pixie_dust", () -> new ItemPixieDust());
    public static final Item PIXIE_WINGS = register("pixie_wings", () -> new ItemGeneric(1));
    public static final Item PIXIE_WAND = register("pixie_wand", () -> new ItemPixieWand());
    public static final Item AMBROSIA = register("ambrosia", () -> new ItemAmbrosia());
    public static final Item CYCLOPS_EYE = register("cyclops_eye", () -> new ItemCyclopsEye());
    public static final Item SHEEP_HELMET = register("sheep_helmet", () -> new ItemModArmor(SHEEP_ARMOR_MATERIAL, EquipmentSlot.HEAD));
    public static final Item SHEEP_CHESTPLATE = register("sheep_chestplate", () -> new ItemModArmor(SHEEP_ARMOR_MATERIAL, EquipmentSlot.CHEST));
    public static final Item SHEEP_LEGGINGS = register("sheep_leggings", () -> new ItemModArmor(SHEEP_ARMOR_MATERIAL, EquipmentSlot.LEGS));
    public static final Item SHEEP_BOOTS = register("sheep_boots", () -> new ItemModArmor(SHEEP_ARMOR_MATERIAL, EquipmentSlot.FEET));
    public static final Item SHINY_SCALES = register("shiny_scales", () -> new ItemGeneric());
    public static final Item SIREN_TEAR = register("siren_tear", () -> new ItemGeneric(1));
    public static final Item SIREN_FLUTE = register("siren_flute", () -> new ItemSirenFlute());
    public static final Item HIPPOCAMPUS_FIN = register("hippocampus_fin", () -> new ItemGeneric(1));
    public static final Item HIPPOCAMPUS_SLAPPER = register("hippocampus_slapper", () -> new ItemHippocampusSlapper());
    public static final Item EARPLUGS = register("earplugs", () -> new ItemModArmor(EARPLUGS_ARMOR_MATERIAL, EquipmentSlot.HEAD));
    public static final Item DEATH_WORM_CHITIN_YELLOW = register("deathworm_chitin_yellow", () -> new ItemGeneric());
    public static final Item DEATH_WORM_CHITIN_WHITE = register("deathworm_chitin_white", () -> new ItemGeneric());
    public static final Item DEATH_WORM_CHITIN_RED = register("deathworm_chitin_red", () -> new ItemGeneric());
    public static final Item DEATHWORM_YELLOW_HELMET = register("deathworm_yellow_helmet", () -> new ItemDeathwormArmor(DEATHWORM_0_ARMOR_MATERIAL, EquipmentSlot.HEAD));
    public static final Item DEATHWORM_YELLOW_CHESTPLATE = register("deathworm_yellow_chestplate", () -> new ItemDeathwormArmor(DEATHWORM_0_ARMOR_MATERIAL, EquipmentSlot.CHEST));
    public static final Item DEATHWORM_YELLOW_LEGGINGS = register("deathworm_yellow_leggings", () -> new ItemDeathwormArmor(DEATHWORM_0_ARMOR_MATERIAL, EquipmentSlot.LEGS));
    public static final Item DEATHWORM_YELLOW_BOOTS = register("deathworm_yellow_boots", () -> new ItemDeathwormArmor(DEATHWORM_0_ARMOR_MATERIAL, EquipmentSlot.FEET));
    public static final Item DEATHWORM_WHITE_HELMET = register("deathworm_white_helmet", () -> new ItemDeathwormArmor(DEATHWORM_1_ARMOR_MATERIAL, EquipmentSlot.HEAD));
    public static final Item DEATHWORM_WHITE_CHESTPLATE = register("deathworm_white_chestplate", () -> new ItemDeathwormArmor(DEATHWORM_1_ARMOR_MATERIAL, EquipmentSlot.CHEST));
    public static final Item DEATHWORM_WHITE_LEGGINGS = register("deathworm_white_leggings", () -> new ItemDeathwormArmor(DEATHWORM_1_ARMOR_MATERIAL, EquipmentSlot.LEGS));
    public static final Item DEATHWORM_WHITE_BOOTS = register("deathworm_white_boots", () -> new ItemDeathwormArmor(DEATHWORM_1_ARMOR_MATERIAL, EquipmentSlot.FEET));
    public static final Item DEATHWORM_RED_HELMET = register("deathworm_red_helmet", () -> new ItemDeathwormArmor(DEATHWORM_2_ARMOR_MATERIAL, EquipmentSlot.HEAD));
    public static final Item DEATHWORM_RED_CHESTPLATE = register("deathworm_red_chestplate", () -> new ItemDeathwormArmor(DEATHWORM_2_ARMOR_MATERIAL, EquipmentSlot.CHEST));
    public static final Item DEATHWORM_RED_LEGGINGS = register("deathworm_red_leggings", () -> new ItemDeathwormArmor(DEATHWORM_2_ARMOR_MATERIAL, EquipmentSlot.LEGS));
    public static final Item DEATHWORM_RED_BOOTS = register("deathworm_red_boots", () -> new ItemDeathwormArmor(DEATHWORM_2_ARMOR_MATERIAL, EquipmentSlot.FEET));
    public static final Item DEATHWORM_EGG = register("deathworm_egg", () -> new ItemDeathwormEgg(false));
    public static final Item DEATHWORM_EGG_GIGANTIC = register("deathworm_egg_giant", () -> new ItemDeathwormEgg(true));
    public static final Item DEATHWORM_TOUNGE = register("deathworm_tounge", () -> new ItemGeneric(1));
    public static final Item DEATHWORM_GAUNTLET_YELLOW = register("deathworm_gauntlet_yellow", () -> new ItemDeathwormGauntlet());
    public static final Item DEATHWORM_GAUNTLET_WHITE = register("deathworm_gauntlet_white", () -> new ItemDeathwormGauntlet());
    public static final Item DEATHWORM_GAUNTLET_RED = register("deathworm_gauntlet_red", () -> new ItemDeathwormGauntlet());
    public static final Item ROTTEN_EGG = register("rotten_egg", () -> new ItemRottenEgg());
    public static final Item COCKATRICE_EYE = register("cockatrice_eye", () -> new ItemGeneric(1));
    public static final Item ITEM_COCKATRICE_SCEPTER = register("cockatrice_scepter", () -> new ItemCockatriceScepter());
    public static final Item STYMPHALIAN_BIRD_FEATHER = register("stymphalian_bird_feather", () -> new ItemGeneric());
    public static final Item STYMPHALIAN_ARROW = register("stymphalian_arrow", () -> new ItemStymphalianArrow());
    public static final Item STYMPHALIAN_FEATHER_BUNDLE = register("stymphalian_feather_bundle", () -> new ItemStymphalianFeatherBundle());
    public static final Item STYMPHALIAN_DAGGER = register("stymphalian_bird_dagger", () -> new ItemStymphalianDagger());
    public static final Item TROLL_TUSK = register("troll_tusk", () -> new ItemGeneric());
    public static final Item MYRMEX_DESERT_EGG = register("myrmex_desert_egg", () -> new ItemMyrmexEgg(false));
    public static final Item MYRMEX_JUNGLE_EGG = register("myrmex_jungle_egg", () -> new ItemMyrmexEgg(true));
    public static final Item MYRMEX_DESERT_RESIN = register("myrmex_desert_resin", () -> new ItemGeneric());
    public static final Item MYRMEX_JUNGLE_RESIN = register("myrmex_jungle_resin", () -> new ItemGeneric());
    public static final Item MYRMEX_DESERT_CHITIN = register("myrmex_desert_chitin", () -> new ItemGeneric());
    public static final Item MYRMEX_JUNGLE_CHITIN = register("myrmex_jungle_chitin", () -> new ItemGeneric());
    public static final Item MYRMEX_STINGER = register("myrmex_stinger", () -> new ItemGeneric());
    public static final Item MYRMEX_DESERT_SWORD = register("myrmex_desert_sword", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.SWORD));
    public static final Item MYRMEX_DESERT_SWORD_VENOM = register("myrmex_desert_sword_venom", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.SWORD));
    public static final Item MYRMEX_DESERT_SHOVEL = register("myrmex_desert_shovel", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.SHOVEL));
    public static final Item MYRMEX_DESERT_PICKAXE = register("myrmex_desert_pickaxe", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.PICKAXE));
    public static final Item MYRMEX_DESERT_AXE = register("myrmex_desert_axe", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.AXE));
    public static final Item MYRMEX_DESERT_HOE = register("myrmex_desert_hoe", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.HOE));
    public static final Item MYRMEX_JUNGLE_SWORD = register("myrmex_jungle_sword", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.SWORD));
    public static final Item MYRMEX_JUNGLE_SWORD_VENOM = register("myrmex_jungle_sword_venom", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.SWORD));
    public static final Item MYRMEX_JUNGLE_SHOVEL = register("myrmex_jungle_shovel", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.SHOVEL));
    public static final Item MYRMEX_JUNGLE_PICKAXE = register("myrmex_jungle_pickaxe", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.PICKAXE));
    public static final Item MYRMEX_JUNGLE_AXE = register("myrmex_jungle_axe", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.AXE));
    public static final Item MYRMEX_JUNGLE_HOE = register("myrmex_jungle_hoe", () -> new IafToolItem(MYRMEX_CHITIN_TOOL_MATERIAL, IafToolItem.Kind.HOE));
    public static final Item MYRMEX_DESERT_STAFF = register("myrmex_desert_staff", () -> new ItemMyrmexStaff(false));
    public static final Item MYRMEX_JUNGLE_STAFF = register("myrmex_jungle_staff", () -> new ItemMyrmexStaff(true));
    public static final Item MYRMEX_DESERT_HELMET = register("myrmex_desert_helmet", () -> new ItemModArmor(MYRMEX_DESERT_ARMOR_MATERIAL, EquipmentSlot.HEAD));
    public static final Item MYRMEX_DESERT_CHESTPLATE = register("myrmex_desert_chestplate", () -> new ItemModArmor(MYRMEX_DESERT_ARMOR_MATERIAL, EquipmentSlot.CHEST));
    public static final Item MYRMEX_DESERT_LEGGINGS = register("myrmex_desert_leggings", () -> new ItemModArmor(MYRMEX_DESERT_ARMOR_MATERIAL, EquipmentSlot.LEGS));
    public static final Item MYRMEX_DESERT_BOOTS = register("myrmex_desert_boots", () -> new ItemModArmor(MYRMEX_DESERT_ARMOR_MATERIAL, EquipmentSlot.FEET));
    public static final Item MYRMEX_JUNGLE_HELMET = register("myrmex_jungle_helmet", () -> new ItemModArmor(MYRMEX_JUNGLE_ARMOR_MATERIAL, EquipmentSlot.HEAD));
    public static final Item MYRMEX_JUNGLE_CHESTPLATE = register("myrmex_jungle_chestplate", () -> new ItemModArmor(MYRMEX_JUNGLE_ARMOR_MATERIAL, EquipmentSlot.CHEST));
    public static final Item MYRMEX_JUNGLE_LEGGINGS = register("myrmex_jungle_leggings", () -> new ItemModArmor(MYRMEX_JUNGLE_ARMOR_MATERIAL, EquipmentSlot.LEGS));
    public static final Item MYRMEX_JUNGLE_BOOTS = register("myrmex_jungle_boots", () -> new ItemModArmor(MYRMEX_JUNGLE_ARMOR_MATERIAL, EquipmentSlot.FEET));
    public static final Item MYRMEX_DESERT_SWARM = register("myrmex_desert_swarm", () -> new ItemMyrmexSwarm(false));
    public static final Item MYRMEX_JUNGLE_SWARM = register("myrmex_jungle_swarm", () -> new ItemMyrmexSwarm(true));
    public static final Item AMPHITHERE_FEATHER = register("amphithere_feather", () -> new ItemGeneric());
    public static final Item AMPHITHERE_ARROW = register("amphithere_arrow", () -> new ItemAmphithereArrow());
    public static final Item AMPHITHERE_MACUAHUITL = register("amphithere_macuahuitl", () -> new ItemAmphithereMacuahuitl());
    public static final Item SERPENT_FANG = register("sea_serpent_fang", () -> new ItemGeneric());
    public static final Item SEA_SERPENT_ARROW = register("sea_serpent_arrow", () -> new ItemSeaSerpentArrow());
    public static final Item TIDE_TRIDENT_INVENTORY = register("tide_trident_inventory", () -> new ItemGeneric(0, true));
    public static final Item TIDE_TRIDENT = register("tide_trident", () -> new ItemTideTrident());
    public static final Item CHAIN = register("chain", () -> new ItemChain(false));
    public static final Item CHAIN_STICKY = register("chain_sticky", () -> new ItemChain(true));
    public static final Item DRAGONSTEEL_FIRE_INGOT = register("dragonsteel_fire_ingot", () -> new ItemGeneric());
    public static final Item DRAGONSTEEL_FIRE_SWORD = register("dragonsteel_fire_sword", () -> new IafToolItem(DRAGONSTEEL_TIER_FIRE, IafToolItem.Kind.SWORD));
    public static final Item DRAGONSTEEL_FIRE_PICKAXE = register("dragonsteel_fire_pickaxe", () -> new IafToolItem(DRAGONSTEEL_TIER_FIRE, IafToolItem.Kind.PICKAXE));
    public static final Item DRAGONSTEEL_FIRE_AXE = register("dragonsteel_fire_axe", () -> new IafToolItem(DRAGONSTEEL_TIER_FIRE, IafToolItem.Kind.AXE));
    public static final Item DRAGONSTEEL_FIRE_SHOVEL = register("dragonsteel_fire_shovel", () -> new IafToolItem(DRAGONSTEEL_TIER_FIRE, IafToolItem.Kind.SHOVEL));
    public static final Item DRAGONSTEEL_FIRE_HOE = register("dragonsteel_fire_hoe", () -> new IafToolItem(DRAGONSTEEL_TIER_FIRE, IafToolItem.Kind.HOE));
    public static final Item DRAGONSTEEL_FIRE_HELMET = register("dragonsteel_fire_helmet", () -> new ItemDragonsteelArmor(DRAGONSTEEL_FIRE_ARMOR_MATERIAL, 0, EquipmentSlot.HEAD));
    public static final Item DRAGONSTEEL_FIRE_CHESTPLATE = register("dragonsteel_fire_chestplate", () -> new ItemDragonsteelArmor(DRAGONSTEEL_FIRE_ARMOR_MATERIAL, 1, EquipmentSlot.CHEST));
    public static final Item DRAGONSTEEL_FIRE_LEGGINGS = register("dragonsteel_fire_leggings", () -> new ItemDragonsteelArmor(DRAGONSTEEL_FIRE_ARMOR_MATERIAL, 2, EquipmentSlot.LEGS));
    public static final Item DRAGONSTEEL_FIRE_BOOTS = register("dragonsteel_fire_boots", () -> new ItemDragonsteelArmor(DRAGONSTEEL_FIRE_ARMOR_MATERIAL, 3, EquipmentSlot.FEET));
    public static final Item DRAGONSTEEL_ICE_INGOT = register("dragonsteel_ice_ingot", () -> new ItemGeneric());
    public static final Item DRAGONSTEEL_ICE_SWORD = register("dragonsteel_ice_sword", () -> new IafToolItem(DRAGONSTEEL_TIER_ICE, IafToolItem.Kind.SWORD));
    public static final Item DRAGONSTEEL_ICE_PICKAXE = register("dragonsteel_ice_pickaxe", () -> new IafToolItem(DRAGONSTEEL_TIER_ICE, IafToolItem.Kind.PICKAXE));
    public static final Item DRAGONSTEEL_ICE_AXE = register("dragonsteel_ice_axe", () -> new IafToolItem(DRAGONSTEEL_TIER_ICE, IafToolItem.Kind.AXE));
    public static final Item DRAGONSTEEL_ICE_SHOVEL = register("dragonsteel_ice_shovel", () -> new IafToolItem(DRAGONSTEEL_TIER_ICE, IafToolItem.Kind.SHOVEL));
    public static final Item DRAGONSTEEL_ICE_HOE = register("dragonsteel_ice_hoe", () -> new IafToolItem(DRAGONSTEEL_TIER_ICE, IafToolItem.Kind.HOE));
    public static final Item DRAGONSTEEL_ICE_HELMET = register("dragonsteel_ice_helmet", () -> new ItemDragonsteelArmor(DRAGONSTEEL_ICE_ARMOR_MATERIAL, 0, EquipmentSlot.HEAD));
    public static final Item DRAGONSTEEL_ICE_CHESTPLATE = register("dragonsteel_ice_chestplate", () -> new ItemDragonsteelArmor(DRAGONSTEEL_ICE_ARMOR_MATERIAL, 1, EquipmentSlot.CHEST));
    public static final Item DRAGONSTEEL_ICE_LEGGINGS = register("dragonsteel_ice_leggings", () -> new ItemDragonsteelArmor(DRAGONSTEEL_ICE_ARMOR_MATERIAL, 2, EquipmentSlot.LEGS));
    public static final Item DRAGONSTEEL_ICE_BOOTS = register("dragonsteel_ice_boots", () -> new ItemDragonsteelArmor(DRAGONSTEEL_ICE_ARMOR_MATERIAL, 3, EquipmentSlot.FEET));
    public static final Item DRAGONSTEEL_LIGHTNING_INGOT = register("dragonsteel_lightning_ingot", () -> new ItemGeneric());
    public static final Item DRAGONSTEEL_LIGHTNING_SWORD = register("dragonsteel_lightning_sword", () -> new IafToolItem(DRAGONSTEEL_TIER_LIGHTNING, IafToolItem.Kind.SWORD));
    public static final Item DRAGONSTEEL_LIGHTNING_PICKAXE = register("dragonsteel_lightning_pickaxe", () -> new IafToolItem(DRAGONSTEEL_TIER_LIGHTNING, IafToolItem.Kind.PICKAXE));
    public static final Item DRAGONSTEEL_LIGHTNING_AXE = register("dragonsteel_lightning_axe", () -> new IafToolItem(DRAGONSTEEL_TIER_LIGHTNING, IafToolItem.Kind.AXE));
    public static final Item DRAGONSTEEL_LIGHTNING_SHOVEL = register("dragonsteel_lightning_shovel", () -> new IafToolItem(DRAGONSTEEL_TIER_LIGHTNING, IafToolItem.Kind.SHOVEL));
    public static final Item DRAGONSTEEL_LIGHTNING_HOE = register("dragonsteel_lightning_hoe", () -> new IafToolItem(DRAGONSTEEL_TIER_LIGHTNING, IafToolItem.Kind.HOE));
    public static final Item DRAGONSTEEL_LIGHTNING_HELMET = register("dragonsteel_lightning_helmet", () -> new ItemDragonsteelArmor(DRAGONSTEEL_LIGHTNING_ARMOR_MATERIAL, 0, EquipmentSlot.HEAD));
    public static final Item DRAGONSTEEL_LIGHTNING_CHESTPLATE = register("dragonsteel_lightning_chestplate", () -> new ItemDragonsteelArmor(DRAGONSTEEL_LIGHTNING_ARMOR_MATERIAL, 1, EquipmentSlot.CHEST));
    public static final Item DRAGONSTEEL_LIGHTNING_LEGGINGS = register("dragonsteel_lightning_leggings", () -> new ItemDragonsteelArmor(DRAGONSTEEL_LIGHTNING_ARMOR_MATERIAL, 2, EquipmentSlot.LEGS));
    public static final Item DRAGONSTEEL_LIGHTNING_BOOTS = register("dragonsteel_lightning_boots", () -> new ItemDragonsteelArmor(DRAGONSTEEL_LIGHTNING_ARMOR_MATERIAL, 3, EquipmentSlot.FEET));
    public static final Item WEEZER_BLUE_ALBUM = register("weezer_blue_album", () -> new ItemGeneric(1, true));
    public static final Item DRAGON_DEBUG_STICK = register("dragon_debug_stick", () -> new ItemGeneric(1, true));
    public static final Item DREAD_SWORD = register("dread_sword", () -> new IafToolItem(DREAD_SWORD_TOOL_MATERIAL, IafToolItem.Kind.SWORD));
    public static final Item DREAD_KNIGHT_SWORD = register("dread_knight_sword", () -> new IafToolItem(DREAD_KNIGHT_TOOL_MATERIAL, IafToolItem.Kind.SWORD));
    public static final Item LICH_STAFF = register("lich_staff", () -> new ItemLichStaff());
    public static final Item DREAD_QUEEN_SWORD = register("dread_queen_sword", () -> new IafToolItem(DRAGONSTEEL_TIER_DREAD_QUEEN, IafToolItem.Kind.SWORD));
    public static final Item DREAD_QUEEN_STAFF = register("dread_queen_staff", () -> new ItemDreadQueenStaff());
    public static final Item DREAD_SHARD = register("dread_shard", () -> new ItemGeneric(0));
    public static final Item DREAD_KEY = register("dread_key", () -> new ItemGeneric(0));
    public static final Item HYDRA_FANG = register("hydra_fang", () -> new ItemGeneric(0));
    public static final Item HYDRA_HEART = register("hydra_heart", () -> new ItemHydraHeart());
    public static final Item HYDRA_ARROW = register("hydra_arrow", () -> new ItemHydraArrow());
    public static final Item CANNOLI = register("cannoli", () -> new ItemCannoli());
    public static final Item ECTOPLASM = register("ectoplasm", () -> new ItemGeneric());
    public static final Item GHOST_INGOT = register("ghost_ingot", () -> new ItemGeneric(1));
    public static final Item GHOST_SWORD = register("ghost_sword", () -> new ItemGhostSword());
    public static final Item PATTERN_FIRE = register("banner_pattern_fire", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("fire")))));
    public static final Item PATTERN_ICE = register("banner_pattern_ice", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("ice")))));
    public static final Item PATTERN_LIGHTNING = register("banner_pattern_lightning", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("lightning")))));
    public static final Item PATTERN_FIRE_HEAD = register("banner_pattern_fire_head", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("fire_head")))));
    public static final Item PATTERN_ICE_HEAD = register("banner_pattern_ice_head", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("ice_head")))));
    public static final Item PATTERN_LIGHTNING_HEAD = register("banner_pattern_lightning_head", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("lightning_head")))));
    public static final Item PATTERN_AMPHITHERE = register("banner_pattern_amphithere", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("amphithere")))));
    public static final Item PATTERN_BIRD = register("banner_pattern_bird", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("bird")))));
    public static final Item PATTERN_EYE = register("banner_pattern_eye", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("eye")))));
    public static final Item PATTERN_FAE = register("banner_pattern_fae", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("fae")))));
    public static final Item PATTERN_FEATHER = register("banner_pattern_feather", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("feather")))));
    public static final Item PATTERN_GORGON = register("banner_pattern_gorgon", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("gorgon")))));
    public static final Item PATTERN_HIPPOCAMPUS = register("banner_pattern_hippocampus", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("hippocampus")))));
    public static final Item PATTERN_HIPPOGRYPH_HEAD = register("banner_pattern_hippogryph_head", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("hippogryph_head")))));
    public static final Item PATTERN_MERMAID = register("banner_pattern_mermaid", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("mermaid")))));
    public static final Item PATTERN_SEA_SERPENT = register("banner_pattern_sea_serpent", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("sea_serpent")))));
    public static final Item PATTERN_TROLL = register("banner_pattern_troll", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("troll")))));
    public static final Item PATTERN_WEEZER = register("banner_pattern_weezer", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("weezer")))));
    public static final Item PATTERN_DREAD = register("banner_pattern_dread", () -> new Item(unstackable().delayedComponent(DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(IafRecipeRegistry.patternTag("dread")))));

    private IafItemRegistry() {
    }

    /** Properties for the item being registered right now (items need their id before they exist). */
    public static Item.Properties props() {
        if (pending == null) {
            throw new IllegalStateException("Item properties made outside IafItemRegistry.register");
        }
        return new Item.Properties().setId(pending);
    }

    public static Item.Properties defaultBuilder() {
        return props();
    }

    public static Item.Properties unstackable() {
        return props().stacksTo(1);
    }

    /** Keeps the next registered item out of the creative tab. */
    public static void hideNext() {
        hidden = true;
    }

    public static <T extends Item> T register(String name, Supplier<T> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, IafMod.id(name));
        pending = key;
        T item;
        try {
            item = factory.get();
        } finally {
            pending = null;
        }
        Registry.register(BuiltInRegistries.ITEM, key, item);
        if (!hidden) {
            TAB_ITEMS.add(item);
        }
        hidden = false;
        return item;
    }

    private static void registerBlockItems() {
        for (Block block : IafBlockRegistry.BLOCKS) {
            if (block instanceof WallTorchBlock) {
                continue;
            }
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
            Item.Properties props = new Item.Properties().setId(key).useBlockDescriptionPrefix();
            Item item;
            if (block instanceof IWallBlock wall) {
                item = new StandingAndWallBlockItem(block, wall.wallBlock(), Direction.DOWN, props);
            } else {
                item = new BlockItem(block, props);
            }
            Registry.register(BuiltInRegistries.ITEM, key, item);
            if (!(block instanceof INoTab noTab) || noTab.shouldBeInTab()) {
                TAB_BLOCKS.add(item);
            }
        }
    }

    public static void init() {
        registerBlockItems();
        EnumDragonArmor.initArmors();
        EnumSeaSerpent.initArmors();
        EnumSkullType.initItems();
        EnumTroll.initArmors();
        register("spawn_egg_fire_dragon", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.FIRE_DRAGON)));
        register("spawn_egg_ice_dragon", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.ICE_DRAGON)));
        register("spawn_egg_lightning_dragon", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.LIGHTNING_DRAGON)));
        register("spawn_egg_hippogryph", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.HIPPOGRYPH)));
        register("spawn_egg_gorgon", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.GORGON)));
        register("spawn_egg_pixie", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.PIXIE)));
        register("spawn_egg_cyclops", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.CYCLOPS)));
        register("spawn_egg_siren", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.SIREN)));
        register("spawn_egg_dread_queen", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DREAD_QUEEN)));
        register("spawn_egg_hippocampus", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.HIPPOCAMPUS)));
        register("spawn_egg_death_worm", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DEATH_WORM)));
        register("spawn_egg_cockatrice", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.COCKATRICE)));
        register("spawn_egg_stymphalian_bird", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.STYMPHALIAN_BIRD)));
        register("spawn_egg_troll", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.TROLL)));
        register("spawn_egg_myrmex_worker", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.MYRMEX_WORKER)));
        register("spawn_egg_myrmex_soldier", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.MYRMEX_SOLDIER)));
        register("spawn_egg_myrmex_sentinel", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.MYRMEX_SENTINEL)));
        register("spawn_egg_myrmex_royal", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.MYRMEX_ROYAL)));
        register("spawn_egg_myrmex_queen", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.MYRMEX_QUEEN)));
        register("spawn_egg_amphithere", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.AMPHITHERE)));
        register("spawn_egg_sea_serpent", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.SEA_SERPENT)));
        register("spawn_egg_dread_thrall", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DREAD_THRALL)));
        register("spawn_egg_dread_ghoul", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DREAD_GHOUL)));
        register("spawn_egg_dread_beast", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DREAD_BEAST)));
        register("spawn_egg_dread_scuttler", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DREAD_SCUTTLER)));
        register("spawn_egg_lich", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DREAD_LICH)));
        register("spawn_egg_dread_knight", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DREAD_KNIGHT)));
        register("spawn_egg_dread_horse", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.DREAD_HORSE)));
        register("spawn_egg_hydra", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.HYDRA)));
        register("spawn_egg_ghost", () -> new SpawnEggItem(props().spawnEgg(IafEntityRegistry.GHOST)));
    }
}
