package com.iafabric.block;


import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;

public class BlockDragonBone extends RotatedPillarBlock implements IDragonProof {

    public BlockDragonBone() {
        super(
            IafBlockRegistry.props().mapColor(MapColor.STONE)
                .sound(SoundType.WOOD)
                .strength(30F, 500F)
                .requiresCorrectToolForDrops()
		);

    }

    }
