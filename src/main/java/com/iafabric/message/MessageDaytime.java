package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


public class MessageDaytime {

    public int dragonId;
    public boolean isDay;

    public MessageDaytime(int dragonId, boolean isDay) {
        this.dragonId = dragonId;
        this.isDay = isDay;
    }

    public MessageDaytime() {
    }

    public static MessageDaytime read(FriendlyByteBuf buf) {
        return new MessageDaytime(buf.readInt(), buf.readBoolean());
    }

    public static void write(MessageDaytime message, FriendlyByteBuf buf) {
        buf.writeInt(message.dragonId);
        buf.writeBoolean(message.isDay);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageDaytime message, Player player) {
            if (player != null) {
                Entity entity = player.level().getEntity(message.dragonId);
                if (entity instanceof EntityDragonBase) {
                    EntityDragonBase dragon = (EntityDragonBase) entity;
                    dragon.isDaytime = message.isDay;
                }
            }
        }
    }
}