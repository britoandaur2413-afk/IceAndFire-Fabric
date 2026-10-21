package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDeathWorm;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;



public class MessageDeathWormHitbox {

    public int deathWormId;
    public float scale;

    public MessageDeathWormHitbox(int deathWormId, float scale) {
        this.deathWormId = deathWormId;
        this.scale = scale;
    }

    public MessageDeathWormHitbox() {
    }

    public static MessageDeathWormHitbox read(FriendlyByteBuf buf) {
        return new MessageDeathWormHitbox(buf.readInt(), buf.readFloat());
    }

    public static void write(MessageDeathWormHitbox message, FriendlyByteBuf buf) {
        buf.writeInt(message.deathWormId);
        buf.writeFloat(message.scale);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageDeathWormHitbox message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    Entity entity = player.level().getEntity(message.deathWormId);
                    if (entity != null && entity instanceof EntityDeathWorm) {
                        EntityDeathWorm worm = (EntityDeathWorm) entity;
                        worm.initSegments(message.scale);
                    }
                }
            }
        }
    }
}