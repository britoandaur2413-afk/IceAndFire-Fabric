package com.iafabric.mixin;

import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** How far a jigsaw structure (a village, a trial chamber) may reach from its start: the room it may take. */
@Mixin(JigsawStructure.class)
public interface JigsawStructureAccessor {
	@Accessor("maxDistanceFromCenter")
	int iceandfire_fabric$maxDistanceFromCenter();
}
