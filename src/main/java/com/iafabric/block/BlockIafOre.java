package com.iafabric.block;

import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.material.MapColor;

public class BlockIafOre extends DropExperienceBlock {
    public Item itemBlock;

    /** Sapphire and amethyst ores (the hard ones) drop experience like diamond ore. */
    public BlockIafOre(int toollevel, float hardness, float resistance) {
        super(IafBlockRegistry.props().mapColor(MapColor.STONE)
                .strength(hardness, resistance)
                .requiresCorrectToolForDrops(), hardness >= 4.0F ? UniformInt.of(3, 7) : ConstantInt.of(0));
    }
}
