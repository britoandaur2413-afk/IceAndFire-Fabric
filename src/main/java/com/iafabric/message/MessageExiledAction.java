package com.iafabric.message;

import com.iafabric.pixie.ExiledPixies;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** What a player does with their exiled pixie from her menu or keys. */
public class MessageExiledAction {
	public static final int OPEN = 0;
	public static final int ADOPT = 1;
	public static final int TALK = 2;
	public static final int ACCEPT = 3;
	public static final int LATER = 4;
	public static final int DROP = 6;
	public static final int SUMMON = 7;
	public static final int REST = 8;
	public static final int GUIDE = 9;
	public static final int ABILITY = 10;
	public static final int TALK_MODE = 11;

	public final int action;
	public final int arg;

	public MessageExiledAction(int action, int arg) {
		this.action = action;
		this.arg = arg;
	}

	public static MessageExiledAction read(FriendlyByteBuf buf) {
		return new MessageExiledAction(buf.readVarInt(), buf.readVarInt());
	}

	public static void write(MessageExiledAction message, FriendlyByteBuf buf) {
		buf.writeVarInt(message.action);
		buf.writeVarInt(message.arg);
	}

	public static class Handler {
		public static void handle(MessageExiledAction message, Player player) {
			if (player instanceof ServerPlayer server) {
				ExiledPixies.act(server, message.action, message.arg);
			}
		}
	}
}
