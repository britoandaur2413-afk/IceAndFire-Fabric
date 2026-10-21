package com.iafabric.message;

import com.iafabric.pixie.ExiledPixieData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A player's exiled pixie as their screen needs her (her bond, quests, where the quest under way leads, what she
 * found for it), and whether to open her menu now.
 */
public class MessageExiledSync {
	/** What was last sent to this client (read by her menu and the screen's tracker). */
	public static ExiledPixieData client = new ExiledPixieData();
	/** The rest of the last tag: the quest's counts, the hidden chest she shows, her search. */
	public static CompoundTag extra = new CompoundTag();

	public final CompoundTag tag;
	public final boolean open;

	public MessageExiledSync(CompoundTag tag, boolean open) {
		this.tag = tag;
		this.open = open;
	}

	public static MessageExiledSync read(FriendlyByteBuf buf) {
		return new MessageExiledSync(buf.readNbt(), buf.readBoolean());
	}

	public static void write(MessageExiledSync message, FriendlyByteBuf buf) {
		buf.writeNbt(message.tag);
		buf.writeBoolean(message.open);
	}

	/** Keeps what came (the client's copy). */
	public void keep() {
		client = ExiledPixieData.fromTag(this.tag);
		extra = this.tag.getCompoundOrEmpty("Extra");
	}
}
