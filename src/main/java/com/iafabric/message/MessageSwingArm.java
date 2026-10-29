package com.iafabric.message;

import com.iafabric.event.ServerEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;


public class MessageSwingArm {

    public MessageSwingArm() {

    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageSwingArm message, Player player) {
            if (player != null) {
                ServerEvents.onLeftClick(player, player.getItemInHand(InteractionHand.MAIN_HAND));
            }
        }
    }


    public static MessageSwingArm read(FriendlyByteBuf buf) {
        return new MessageSwingArm();
    }

    public static void write(MessageSwingArm message, FriendlyByteBuf buf) {
    }

}
