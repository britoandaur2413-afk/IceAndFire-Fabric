package com.iafabric.message;

import net.minecraft.network.FriendlyByteBuf;

/** The player's eyes in a scene of their exiled pixie: closing (the screen goes dark) or opening little by little, over some ticks. */
public class MessageExiledEyes {
	public final boolean open;
	public final int ticks;

	public MessageExiledEyes(boolean open, int ticks) {
		this.open = open;
		this.ticks = ticks;
	}

	public static MessageExiledEyes read(FriendlyByteBuf buf) {
		return new MessageExiledEyes(buf.readBoolean(), buf.readVarInt());
	}

	public static void write(MessageExiledEyes message, FriendlyByteBuf buf) {
		buf.writeBoolean(message.open);
		buf.writeVarInt(message.ticks);
	}
}
