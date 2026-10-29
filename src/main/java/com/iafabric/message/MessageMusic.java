package com.iafabric.message;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Which of the mod's soundtracks a player hears now (Soundtracks), and in which world it belongs (empty: wherever they
 * are); none: the one playing fades out.
 */
public class MessageMusic {
	public final int track;
	public final String world;

	public MessageMusic(int track, String world) {
		this.track = track;
		this.world = world;
	}

	public static MessageMusic read(FriendlyByteBuf buf) {
		return new MessageMusic(buf.readVarInt(), buf.readUtf());
	}

	public static void write(MessageMusic message, FriendlyByteBuf buf) {
		buf.writeVarInt(message.track);
		buf.writeUtf(message.world);
	}
}
