package com.iafabric.message;

import com.iafabric.IafMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;


public class MessageMultipartInteract {

    public int creatureID;
    public float dmg;

    public MessageMultipartInteract(int creatureID, float dmg) {
        this.creatureID = creatureID;
        this.dmg = dmg;
    }

    public MessageMultipartInteract() {
    }

    public static MessageMultipartInteract read(FriendlyByteBuf buf) {
        return new MessageMultipartInteract(buf.readInt(), buf.readFloat());
    }

    public static void write(MessageMultipartInteract message, FriendlyByteBuf buf) {
        buf.writeInt(message.creatureID);
        buf.writeFloat(message.dmg);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageMultipartInteract message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    Entity entity = player.level().getEntity(message.creatureID);
                    if (entity != null && entity instanceof LivingEntity) {
                        double dist = player.distanceTo(entity);
                        LivingEntity mob = (LivingEntity) entity;
                        if (dist < 100) {
                            if (message.dmg > 0F) {
                                mob.hurtOrSimulate(mob.damageSources().mobAttack(player), message.dmg);
                            } else {
                                mob.interact(player, InteractionHand.MAIN_HAND, mob.position());
                            }
                        }
                    }
                }
            }
        }
    }
}