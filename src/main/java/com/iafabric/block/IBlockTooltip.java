package com.iafabric.block;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** A block whose item shows extra tooltip lines (blocks no longer add item tooltips themselves). */
public interface IBlockTooltip {
    void appendBlockTooltip(Consumer<Component> tooltip);
}
