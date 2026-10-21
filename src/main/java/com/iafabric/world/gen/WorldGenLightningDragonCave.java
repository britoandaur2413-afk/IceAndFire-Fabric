package com.iafabric.world.gen;

import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityLightningDragon;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.entity.util.HomePosition;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.util.RandomSource;

public class WorldGenLightningDragonCave extends WorldGenDragonCave {
    public static Identifier LIGHTNING_DRAGON_CHEST = Identifier.fromNamespaceAndPath(IafMod.MOD_ID, "chest/lightning_dragon_female_cave");
    public static Identifier LIGHTNING_DRAGON_CHEST_MALE = Identifier.fromNamespaceAndPath(IafMod.MOD_ID, "chest/lightning_dragon_male_cave");

    public WorldGenLightningDragonCave(final Object configuration) {
        super(configuration);
        DRAGON_CHEST = LIGHTNING_DRAGON_CHEST;
        DRAGON_MALE_CHEST = LIGHTNING_DRAGON_CHEST_MALE;
        CEILING_DECO = new WorldGenCaveStalactites(IafBlockRegistry.CRACKLED_STONE, 6);
        PALETTE_BLOCK1 = IafBlockRegistry.CRACKLED_STONE.defaultBlockState();
        PALETTE_BLOCK2 = IafBlockRegistry.CRACKLED_COBBLESTONE.defaultBlockState();
        TREASURE_PILE = IafBlockRegistry.COPPER_PILE.defaultBlockState();
        PALETTE_ORE1 = IafBlockRegistry.AMYTHEST_ORE.defaultBlockState();
        PALETTE_ORE2 = Blocks.EMERALD_ORE.defaultBlockState();
        generateGemOre = IafConfig.generateAmythestOre;
    }

    @Override
    EntityDragonBase createDragon(WorldGenLevel worldIn, RandomSource rand, BlockPos position, int dragonAge) {
        EntityLightningDragon dragon = new EntityLightningDragon(IafEntityRegistry.LIGHTNING_DRAGON,
            worldIn.getLevel());
        dragon.setGender(isMale);
        dragon.growDragon(dragonAge);
        dragon.setAgingDisabled(true);
        dragon.setHealth(dragon.getMaxHealth());
        dragon.setVariant(rand.nextInt(4));
        dragon.absSnapTo(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5, rand.nextFloat() * 360, 0);
        dragon.setInSittingPose(true);
        dragon.homePos = new HomePosition(position, worldIn.getLevel());
        dragon.setHunger(50);
        return dragon;
    }

}
