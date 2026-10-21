package com.iafabric.message;

import net.minecraft.network.FriendlyByteBuf;

/** Server to client: an animated entity starts an animation (index into its animation list, -1 for none). */
public record MessageAnimation(int entityId, int index) {

    public static MessageAnimation read(FriendlyByteBuf buf) {
        return new MessageAnimation(buf.readVarInt(), buf.readVarInt());
    }

    public static void write(MessageAnimation message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeVarInt(message.index);
    }
}
