package com.iafabric.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** A line of a player's exiled pixie, shown by her face at the top of their screen. */
public class MessageExiledSpeech {
	/** What kind of line: a word, a quest's (also kept in the chat), an alarm, a hint (how to do something). */
	public static final int TALK = 0;
	public static final int QUEST = 1;
	public static final int ALARM = 2;
	public static final int HINT = 3;

	public final Component text;
	public final int kind;

	public MessageExiledSpeech(Component text, int kind) {
		this.text = text;
		this.kind = kind;
	}

	public static MessageExiledSpeech read(FriendlyByteBuf buf) {
		return new MessageExiledSpeech(((FriendlyByteBuf) buf).readComponent(), buf.readVarInt());
	}

	public static void write(MessageExiledSpeech message, FriendlyByteBuf buf) {
		((FriendlyByteBuf) buf).writeComponent(message.text);
		buf.writeVarInt(message.kind);
	}
}
