package com.iafabric.world.gen;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityIceDragon;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.util.HomePosition;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.util.RandomSource;

public class WorldGenIceDragonCave extends WorldGenDragonCave {
    public static ResourceLocation ICE_DRAGON_CHEST = new ResourceLocation(IafMod.MOD_ID, "chest/ice_dragon_female_cave");
    public static ResourceLocation ICE_DRAGON_CHEST_MALE = new ResourceLocation(IafMod.MOD_ID, "chest/ice_dragon_male_cave");

    public WorldGenIceDragonCave(final Object configuration) {
        super(configuration);
        DRAGON_CHEST = ICE_DRAGON_CHEST;
        DRAGON_MALE_CHEST = ICE_DRAGON_CHEST_MALE;
        CEILING_DECO = new WorldGenCaveStalactites(IafBlockRegistry.FROZEN_STONE, 3);
        PALETTE_BLOCK1 = IafBlockRegistry.FROZEN_STONE.defaultBlockState();
        PALETTE_BLOCK2 = IafBlockRegistry.FROZEN_COBBLESTONE.defaultBlockState();
        TREASURE_PILE = IafBlockRegistry.SILVER_PILE.defaultBlockState();
        PALETTE_ORE1 = IafBlockRegistry.SAPPHIRE_ORE.defaultBlockState();
        PALETTE_ORE2 = Blocks.EMERALD_ORE.defaultBlockState();
        generateGemOre = IafConfig.generateSapphireOre;
    }

    @Override
    EntityDragonBase createDragon(WorldGenLevel worldIn, RandomSource rand, BlockPos position, int dragonAge) {
        EntityIceDragon dragon = new EntityIceDragon(IafEntityRegistry.ICE_DRAGON, worldIn.getLevel());
        dragon.setGender(isMale);
        dragon.growDragon(dragonAge);
        dragon.setAgingDisabled(true);
        dragon.setHealth(dragon.getMaxHealth());
        dragon.setVariant(rand.nextInt(4));
        dragon.absMoveTo(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5, rand.nextFloat() * 360, 0);
        dragon.setInSittingPose(true);
        dragon.homePos = new HomePosition(position, worldIn.getLevel());
        dragon.setHunger(50);
        return dragon;
    }

}
