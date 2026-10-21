package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityJar;
import com.iafabric.entity.tile.TileEntityPixieHouse;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;


public class MessageUpdatePixieHouse {

    public long blockPos;
    public boolean hasPixie;
    public int pixieType;

    public MessageUpdatePixieHouse(long blockPos, boolean hasPixie, int pixieType) {
        this.blockPos = blockPos;
        this.hasPixie = hasPixie;
        this.pixieType = pixieType;

    }

    public MessageUpdatePixieHouse() {
    }

    public static MessageUpdatePixieHouse read(FriendlyByteBuf buf) {
        return new MessageUpdatePixieHouse(buf.readLong(), buf.readBoolean(), buf.readInt());
    }

    public static void write(MessageUpdatePixieHouse message, FriendlyByteBuf buf) {
        buf.writeLong(message.blockPos);
        buf.writeBoolean(message.hasPixie);
        buf.writeInt(message.pixieType);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageUpdatePixieHouse message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    BlockPos pos = BlockPos.of(message.blockPos);
                    if (player.level().getBlockEntity(pos) != null && player.level().getBlockEntity(pos) instanceof TileEntityPixieHouse) {
                        TileEntityPixieHouse house = (TileEntityPixieHouse) player.level().getBlockEntity(pos);
                        house.hasPixie = message.hasPixie;
                        house.pixieType = message.pixieType;
                    } else if (player.level().getBlockEntity(pos) != null && player.level().getBlockEntity(pos) instanceof TileEntityJar) {
                        TileEntityJar jar = (TileEntityJar) player.level().getBlockEntity(pos);
                        jar.hasPixie = message.hasPixie;
                        jar.pixieType = message.pixieType;
                    }
                }
            }
        }
    }

}