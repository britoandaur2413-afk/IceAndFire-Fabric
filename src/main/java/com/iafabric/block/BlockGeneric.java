package com.iafabric.block;


import net.minecraft.world.level.material.MapColor;
import com.iafabric.entity.EntityDreadMob;
import com.iafabric.entity.util.DragonUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BlockGeneric extends Block {
    public BlockGeneric(MapColor materialIn, float hardness, float resistance, SoundType sound) {
        super(
            IafBlockRegistry.props().mapColor(materialIn)
                .sound(sound)
                .strength(hardness, resistance)
                .requiresCorrectToolForDrops()
        );
    }

    public BlockGeneric(MapColor materialIn, float hardness, float resistance, SoundType sound, boolean slippery) {
        super(
            IafBlockRegistry.props().mapColor(materialIn)
                .sound(sound)
                .strength(hardness, resistance)
                .friction(0.98F)
        );
    }

    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(BlockState state) {
        return this != IafBlockRegistry.DRAGON_ICE;
    }

    @SuppressWarnings("deprecation")
    public boolean isFullCube(BlockState state) {
        return this != IafBlockRegistry.DRAGON_ICE;
    }

    @Deprecated
    public boolean canEntitySpawn(BlockState state, Entity entityIn) {
        return entityIn instanceof EntityDreadMob || !DragonUtils.isDreadBlock(state);
    }

}
