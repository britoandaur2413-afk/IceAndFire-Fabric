package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityDragonforge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;


public class MessageUpdateDragonforge {

    public long blockPos;
    public int cookTime;

    public MessageUpdateDragonforge(long blockPos, int houseType) {
        this.blockPos = blockPos;
        this.cookTime = houseType;

    }

    public MessageUpdateDragonforge() {
    }

    public static MessageUpdateDragonforge read(FriendlyByteBuf buf) {
        return new MessageUpdateDragonforge(buf.readLong(), buf.readInt());
    }

    public static void write(MessageUpdateDragonforge message, FriendlyByteBuf buf) {
        buf.writeLong(message.blockPos);
        buf.writeInt(message.cookTime);
    }


    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageUpdateDragonforge message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    BlockPos pos = BlockPos.of(message.blockPos);
                    if (player.level().getBlockEntity(pos) != null) {
                        if (player.level().getBlockEntity(pos) instanceof TileEntityDragonforge) {
                            TileEntityDragonforge house = (TileEntityDragonforge) player.level().getBlockEntity(pos);
                            house.cookTime = message.cookTime;
                            if (message.cookTime > 0) {
                                house.lastDragonFlameTimer = 40;
                            }
                        }
                    }
                }
            }
        }
    }
}