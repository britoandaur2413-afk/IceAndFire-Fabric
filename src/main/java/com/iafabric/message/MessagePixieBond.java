package com.iafabric.message;

import com.iafabric.pixie.PixieBonding;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** What the player does with their pixies: open the bonds menu, bond, call out, send to rest, open a pixie's own menu, cast its spell. */
public class MessagePixieBond {
    public static final int OPEN = 0;
    public static final int BOND = 1;
    public static final int SUMMON = 2;
    public static final int REST = 3;
    public static final int MENU = 4;
    public static final int MAGIC = 5;

    public final int action;
    public final int color;

    public MessagePixieBond(int action, int color) {
        this.action = action;
        this.color = color;
    }

    public static MessagePixieBond read(FriendlyByteBuf buf) {
        return new MessagePixieBond(buf.readVarInt(), buf.readVarInt());
    }

    public static void write(MessagePixieBond message, FriendlyByteBuf buf) {
        buf.writeVarInt(message.action);
        buf.writeVarInt(message.color);
    }

    public static class Handler {
        public static void handle(MessagePixieBond message, Player player) {
            if (!(player instanceof ServerPlayer serverPlayer) || message.color < 0 || message.color > 4) {
                return;
            }
            switch (message.action) {
                case BOND -> PixieBonding.bond(serverPlayer, message.color);
                case SUMMON -> PixieBonding.summon(serverPlayer, message.color);
                case REST -> PixieBonding.rest(serverPlayer, false);
                case MENU -> PixieBonding.openMenu(serverPlayer, message.color);
                case MAGIC -> com.iafabric.pixie.PixieMagic.cast(serverPlayer);
                default -> PixieBonding.sync(serverPlayer);
            }
        }
    }
}
