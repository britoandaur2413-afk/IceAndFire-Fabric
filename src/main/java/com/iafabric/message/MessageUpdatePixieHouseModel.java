package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityJar;
import com.iafabric.entity.tile.TileEntityPixieHouse;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;


public class MessageUpdatePixieHouseModel {

    public long blockPos;
    public int houseType;

    public MessageUpdatePixieHouseModel(long blockPos, int houseType) {
        this.blockPos = blockPos;
        this.houseType = houseType;

    }

    public MessageUpdatePixieHouseModel() {
    }

    public static MessageUpdatePixieHouseModel read(FriendlyByteBuf buf) {
        return new MessageUpdatePixieHouseModel(buf.readLong(), buf.readInt());
    }

    public static void write(MessageUpdatePixieHouseModel message, FriendlyByteBuf buf) {
        buf.writeLong(message.blockPos);
        buf.writeInt(message.houseType);
    }


    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageUpdatePixieHouseModel message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    BlockPos pos = BlockPos.of(message.blockPos);
                    if (player.level().getBlockEntity(pos) != null) {
                        if (player.level().getBlockEntity(pos) instanceof TileEntityPixieHouse) {
                            TileEntityPixieHouse house = (TileEntityPixieHouse) player.level().getBlockEntity(pos);
                            house.houseType = message.houseType;
                        }
                        if (player.level().getBlockEntity(pos) instanceof TileEntityJar) {
                            TileEntityJar jar = (TileEntityJar) player.level().getBlockEntity(pos);
                            jar.pixieType = message.houseType;
                        }
                    }
                }
            }
        }
    }
}