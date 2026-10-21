package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.EntitySiren;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;


public class MessageSirenSong {

    public int sirenId;
    public boolean isSinging;

    public MessageSirenSong(int sirenId, boolean isSinging) {
        this.sirenId = sirenId;
        this.isSinging = isSinging;
    }

    public MessageSirenSong() {
    }

    public static MessageSirenSong read(FriendlyByteBuf buf) {
        return new MessageSirenSong(buf.readInt(), buf.readBoolean());
    }

    public static void write(MessageSirenSong message, FriendlyByteBuf buf) {
        buf.writeInt(message.sirenId);
        buf.writeBoolean(message.isSinging);
    }


    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageSirenSong message, Player player) {
            if (player != null && player.level() != null) {
                Entity entity = player.level().getEntity(message.sirenId);
                if (entity != null && entity instanceof EntitySiren) {
                    EntitySiren siren = (EntitySiren) entity;
                    siren.setSinging(message.isSinging);
                }
            }
        }
    }

}