package com.iafabric.message;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The altar's card of Cryona's duel, for the player before it: the two bosses (their entities to show, their life and
 * her ice shield), how much ice dragon blood the player has (and whether they need none, in creative), and whether the
 * duel goes on where it was left.
 */
public class MessageBossCard {
	public final int frostId;
	public final int queenId;
	public final float frostLife;
	public final float frostMax;
	public final float queenLife;
	public final float queenMax;
	public final float shield;
	public final float shieldMax;
	public final int blood;
	public final boolean free;
	public final boolean resumed;

	public MessageBossCard(int frostId, int queenId, float frostLife, float frostMax, float queenLife, float queenMax, float shield, float shieldMax,
		int blood, boolean free, boolean resumed) {
		this.frostId = frostId;
		this.queenId = queenId;
		this.frostLife = frostLife;
		this.frostMax = frostMax;
		this.queenLife = queenLife;
		this.queenMax = queenMax;
		this.shield = shield;
		this.shieldMax = shieldMax;
		this.blood = blood;
		this.free = free;
		this.resumed = resumed;
	}

	public static MessageBossCard read(FriendlyByteBuf buf) {
		return new MessageBossCard(buf.readInt(), buf.readInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
			buf.readFloat(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
	}

	public static void write(MessageBossCard message, FriendlyByteBuf buf) {
		buf.writeInt(message.frostId);
		buf.writeInt(message.queenId);
		buf.writeFloat(message.frostLife);
		buf.writeFloat(message.frostMax);
		buf.writeFloat(message.queenLife);
		buf.writeFloat(message.queenMax);
		buf.writeFloat(message.shield);
		buf.writeFloat(message.shieldMax);
		buf.writeVarInt(message.blood);
		buf.writeBoolean(message.free);
		buf.writeBoolean(message.resumed);
	}

	/** Whether Black Frost still stands (the duel's first part is still to fight). */
	public boolean frostStands() {
		return frostId >= 0 && frostLife > 0;
	}
}
