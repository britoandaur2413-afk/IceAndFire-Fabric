package com.iafabric.block;


import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;

public class BlockDreadWoodLog extends RotatedPillarBlock implements IDragonProof, IDreadBlock {

    public BlockDreadWoodLog() {
        super(
    		IafBlockRegistry.props().mapColor(MapColor.WOOD)
				.strength(2F, 10000F)
    			.sound(SoundType.WOOD)
		);
    }
}
