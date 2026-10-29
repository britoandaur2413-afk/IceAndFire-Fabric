package com.iafabric.message;

import com.iafabric.pixie.PixieBonds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

/** The player's pixie bonds, for their menu. */
public class MessagePixieBondSync {
    /** The bonds as last sent to this client (read by the menu). */
    public static PixieBonds client = new PixieBonds();

    public final CompoundTag tag;

    public MessagePixieBondSync(CompoundTag tag) {
        this.tag = tag;
    }

    public static MessagePixieBondSync read(FriendlyByteBuf buf) {
        return new MessagePixieBondSync(buf.readNbt());
    }

    public static void write(MessagePixieBondSync message, FriendlyByteBuf buf) {
        buf.writeNbt(message.tag);
    }

    public static class Handler {
        public static void handle(MessagePixieBondSync message, Player player) {
            client = PixieBonds.fromTag(message.tag);
        }
    }
}
