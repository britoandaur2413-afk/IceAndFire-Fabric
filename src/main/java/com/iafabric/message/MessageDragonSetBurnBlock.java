package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


public class MessageDragonSetBurnBlock {

    public int dragonId;
    public boolean breathingFire;
    public int posX;
    public int posY;
    public int posZ;

    public MessageDragonSetBurnBlock(int dragonId, boolean breathingFire, BlockPos pos) {
        this.dragonId = dragonId;
        this.breathingFire = breathingFire;
        posX = pos.getX();
        posY = pos.getY();
        posZ = pos.getZ();
    }

    public MessageDragonSetBurnBlock() {
    }


    public static MessageDragonSetBurnBlock read(FriendlyByteBuf buf) {
        return new MessageDragonSetBurnBlock(buf.readInt(), buf.readBoolean(), new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
    }

    public static void write(MessageDragonSetBurnBlock message, FriendlyByteBuf buf) {
        buf.writeInt(message.dragonId);
        buf.writeBoolean(message.breathingFire);
        buf.writeInt(message.posX);
        buf.writeInt(message.posY);
        buf.writeInt(message.posZ);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageDragonSetBurnBlock message, Player player) {
            if (player != null) {
                if (player.level() != null) {
                    if (player.level() != null) {
                        Entity entity = player.level().getEntity(message.dragonId);
                        if (entity != null && entity instanceof EntityDragonBase) {
                            EntityDragonBase dragon = (EntityDragonBase) entity;
                            dragon.setBreathingFire(message.breathingFire);
                            dragon.burningTarget = new BlockPos(message.posX, message.posY, message.posZ);
                        }
                    }
                }
            }
        }
    }
}