package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


public class MessageDragonSyncFire {

    public int dragonId;
    public double posX;
    public double posY;
    public double posZ;
    public int syncType;

    public MessageDragonSyncFire(int dragonId, double posX, double posY, double posZ, int syncType) {
        this.dragonId = dragonId;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.syncType = syncType;
    }

    public MessageDragonSyncFire() {
    }

    public static MessageDragonSyncFire read(FriendlyByteBuf buf) {
        return new MessageDragonSyncFire(buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
    }

    public static void write(MessageDragonSyncFire message, FriendlyByteBuf buf) {
        buf.writeInt(message.dragonId);
        buf.writeDouble(message.posX);
        buf.writeDouble(message.posY);
        buf.writeDouble(message.posZ);
        buf.writeInt(message.syncType);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageDragonSyncFire message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    Entity entity = player.level().getEntity(message.dragonId);
                    if (entity != null && entity instanceof EntityDragonBase) {
                        EntityDragonBase dragon = (EntityDragonBase) entity;
                        dragon.stimulateFire(message.posX, message.posY, message.posZ, message.syncType);
                    }
                }
            }
        }
    }

}
