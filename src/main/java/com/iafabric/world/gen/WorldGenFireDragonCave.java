package com.iafabric.world.gen;

import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityFireDragon;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.util.HomePosition;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.util.RandomSource;

public class WorldGenFireDragonCave extends WorldGenDragonCave{

    public static ResourceLocation FIRE_DRAGON_CHEST = ResourceLocation.fromNamespaceAndPath(IafMod.MOD_ID, "chest/fire_dragon_female_cave");
    public static ResourceLocation FIRE_DRAGON_CHEST_MALE = ResourceLocation.fromNamespaceAndPath(IafMod.MOD_ID, "chest/fire_dragon_male_cave");

    public WorldGenFireDragonCave(final Object configuration) {
        super(configuration);
        DRAGON_CHEST = FIRE_DRAGON_CHEST;
        DRAGON_MALE_CHEST = FIRE_DRAGON_CHEST_MALE;
        CEILING_DECO = new WorldGenCaveStalactites(IafBlockRegistry.CHARRED_STONE, 3);
        PALETTE_BLOCK1 = IafBlockRegistry.CHARRED_STONE.defaultBlockState();
        PALETTE_BLOCK2 = IafBlockRegistry.CHARRED_COBBLESTONE.defaultBlockState();
        TREASURE_PILE = IafBlockRegistry.GOLD_PILE.defaultBlockState();
        PALETTE_ORE1 = Blocks.EMERALD_ORE.defaultBlockState();
        PALETTE_ORE2 = Blocks.EMERALD_ORE.defaultBlockState();
        generateGemOre = true;
    }

    @Override
    EntityDragonBase createDragon(WorldGenLevel worldIn, RandomSource rand, BlockPos position, int dragonAge) {
        EntityFireDragon dragon = new EntityFireDragon(IafEntityRegistry.FIRE_DRAGON, worldIn.getLevel());
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
