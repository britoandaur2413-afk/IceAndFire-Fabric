package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityJar;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;


public class MessageUpdatePixieJar {

    public long blockPos;
    public boolean isProducing;

    public MessageUpdatePixieJar(long blockPos, boolean isProducing) {
        this.blockPos = blockPos;
        this.isProducing = isProducing;

    }

    public MessageUpdatePixieJar() {
    }

    public static MessageUpdatePixieJar read(FriendlyByteBuf buf) {
        return new MessageUpdatePixieJar(buf.readLong(), buf.readBoolean());
    }

    public static void write(MessageUpdatePixieJar message, FriendlyByteBuf buf) {
        buf.writeLong(message.blockPos);
        buf.writeBoolean(message.isProducing);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageUpdatePixieJar message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    BlockPos pos = BlockPos.of(message.blockPos);
                    if (player.level().getBlockEntity(pos) != null) {
                        if (player.level().getBlockEntity(pos) instanceof TileEntityJar) {
                            TileEntityJar jar = (TileEntityJar) player.level().getBlockEntity(pos);
                            jar.hasProduced = message.isProducing;
                        }
                    }
                }
            }
        }
    }
}