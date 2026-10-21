package com.iafabric.message;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/** Server to client: the extra data of an entity (chains, frozen, siren charm...) changed. */
public record MessageEntityData(int entityId, CompoundTag data) {

    public static MessageEntityData read(FriendlyByteBuf buf) {
        return new MessageEntityData(buf.readVarInt(), buf.readNbt());
    }

    public static void write(MessageEntityData message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.entityId);
        buf.writeNbt(message.data);
    }
}
