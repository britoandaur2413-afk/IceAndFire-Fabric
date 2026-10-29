package com.iafabric.world.gen;

import com.iafabric.IafMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import net.minecraft.util.RandomSource;

public class WorldGenDreadExitPortal {
    private static final ResourceLocation STRUCTURE = new ResourceLocation(IafMod.MOD_ID, "dread_exit_portal");

    public boolean generate(Level worldIn, RandomSource rand, BlockPos position) {
        /*
        MinecraftServer server = worldIn.getMinecraftServer();
        TemplateManager templateManager = worldIn.getSaveHandler().getStructureTemplateManager();
        PlacementSettings settings = new PlacementSettings().setRotation(Rotation.NONE);
        Template template = templateManager.getTemplate(server, STRUCTURE);
        Biome biome = worldIn.getBiome(position);
        template.addBlocksToWorld(worldIn, position, new DreadPortalProcessor(position, settings, biome), settings, 2);

         */
        return true;
    }
}