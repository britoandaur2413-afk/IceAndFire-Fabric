package com.iafabric.message;

import net.minecraft.network.FriendlyByteBuf;
import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityPodium;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


public class MessageUpdatePodium {

    public long blockPos;
    public ItemStack heldStack;

    public MessageUpdatePodium(long blockPos, ItemStack heldStack) {
        this.blockPos = blockPos;
        this.heldStack = heldStack;

    }

    public MessageUpdatePodium() {
    }

    public static MessageUpdatePodium read(FriendlyByteBuf buf) {
        return new MessageUpdatePodium(buf.readLong(), ((FriendlyByteBuf) buf).readItem());
    }

    public static void write(MessageUpdatePodium message, FriendlyByteBuf buf) {
        buf.writeLong(message.blockPos);
        ((FriendlyByteBuf) buf).writeItem(message.heldStack);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageUpdatePodium message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    BlockPos pos = BlockPos.of(message.blockPos);
                    if (player.level().getBlockEntity(pos) != null) {
                        if (player.level().getBlockEntity(pos) instanceof TileEntityPodium) {
                            TileEntityPodium podium = (TileEntityPodium) player.level().getBlockEntity(pos);
                            podium.setItem(0, message.heldStack);
                        }
                    }
                }
            }
        }
    }

}