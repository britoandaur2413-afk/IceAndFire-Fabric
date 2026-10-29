package com.iafabric.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

/**
 * A memory of a player's exiled pixie, seen as a film: it begins (the screen framed, the player still, her words its
 * subtitles), a card in its middle (her gift's name), or it is over.
 */
public class MessageExiledMemory {
	public static final int OFF = 0;
	public static final int ON = 1;
	public static final int CARD = 2;

	public final int what;
	public final Component text;

	public MessageExiledMemory(int what, Component text) {
		this.what = what;
		this.text = text;
	}

	public MessageExiledMemory(int what) {
		this(what, Component.empty());
	}

	public static MessageExiledMemory read(FriendlyByteBuf buf) {
		int what = buf.readVarInt();
		return new MessageExiledMemory(what, ComponentSerialization.TRUSTED_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf));
	}

	public static void write(MessageExiledMemory message, FriendlyByteBuf buf) {
		buf.writeVarInt(message.what);
		ComponentSerialization.TRUSTED_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, message.text);
	}
}
