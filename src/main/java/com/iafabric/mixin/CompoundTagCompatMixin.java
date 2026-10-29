package com.iafabric.mixin;

import com.iafabric.compat.CompoundTagCompat;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;

/** Gives 1.21.1's CompoundTag the 26.x getters the mod uses (see CompoundTagCompat). */
@Mixin(CompoundTag.class)
public abstract class CompoundTagCompatMixin implements CompoundTagCompat {
}
