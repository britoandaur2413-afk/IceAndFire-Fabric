package com.iafabric.pixie;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A player's exiled pixie: whether they met her (the first tree they cut was her house), which of the twenty she is,
 * whether she is out with them, her bond (level and experience), her quests (the one under way, the one she offers,
 * those done or passed) and what she said lately (not to say it again soon).
 */
public class ExiledPixieData {
	/** Never met; met (her house broken, she waits for a word); friends. */
	public static final int NONE = 0;
	public static final int ANGRY = 1;
	public static final int FRIEND = 2;
	/** Her gifts beyond the pixies' (bits): the resistance to absolute ice, given when the right moment came. */
	public static final int GIFT_ICE = 1;
	/** Experience to reach levels 2 to 5 of her bond (a quest gives 120 to 250, a foe beaten near her a little). */
	public static final int[] LEVEL_XP = {150, 450, 900, 1500};

	public int stage = NONE;
	public int variant = -1;
	/** Out with the player (else resting, in their pocket). */
	public boolean out = true;
	public int level = 1;
	public int xp;
	/** The quest under way ({@link ExiledQuest} ordinal) and the one she offers, -1 for none. */
	public int quest = -1;
	public int offered = -1;
	/** A count for the quest under way (sheep hurt, a step reached). */
	public int progress;
	/** Where the quest under way leads, in which world, and whether she found it (else she is still looking). */
	public BlockPos target;
	public String targetLevel = "";
	/** The way to it first, when there is one to find (a cave's way in), till the player is there. */
	public BlockPos via;
	/** Her quest's place is to be looked for anew (it was found as it no longer is). */
	public transient boolean relook;
	/** Her ring's lantern at that place, once she made it (reaching it is reaching the place). */
	public BlockPos checkpoint;
	/** Quests done, and quests passed (the player went past them, or said no). */
	public long done;
	public long skipped;
	/** When she may offer a quest again, say something again, use her gift again (game time). */
	public long nextOffer;
	public long nextComment;
	public long abilityReady;
	/** How much she talks: 0 little, 1 as she likes, 2 a lot. */
	public int talk = 1;
	/** She flies ahead to the quest's place. */
	public boolean guide = true;
	/** Things said once (bits of {@link ExiledPixies} one-off remarks). */
	public long said;
	/** Her last idle remarks, not to repeat them soon. */
	public final List<Integer> recent = new ArrayList<>();
	/** The lines of each kind she said (bits), not to say one again before all of its kind went round. */
	public final Map<String, Long> told = new HashMap<>();
	/** Villages she warned of, and hidden chests she told of (packed positions), the last ones only. */
	public final List<Long> villages = new ArrayList<>();
	public final List<Long> chests = new ArrayList<>();
	/** Where her house was (the tree), and when they met. */
	public BlockPos home;
	public long metAt;
	/** Her gifts (bits of GIFT_*). */
	public int gifts;
	/** Where the player was when she took them to her glade (to bring them back), in which world, facing where. */
	public net.minecraft.world.phys.Vec3 visionFrom;
	public String visionLevel = "";
	public float visionYaw;

	public boolean hasGift(int gift) {
		return (gifts & gift) != 0;
	}

	public boolean met() {
		return stage != NONE;
	}

	/** Where to go now: her ring's lantern if she made it, else the way to the quest's place, else the place. */
	public BlockPos goal() {
		return checkpoint != null ? checkpoint : via != null ? via : target;
	}

	public boolean isDone(ExiledQuest quest) {
		return (done & quest.bit()) != 0;
	}

	public boolean isSkipped(ExiledQuest quest) {
		return (skipped & quest.bit()) != 0;
	}

	/** Done or passed: it is not offered again. */
	public boolean isOver(ExiledQuest quest) {
		return isDone(quest) || isSkipped(quest);
	}

	public ExiledQuest current() {
		return quest < 0 || quest >= ExiledQuest.values().length ? null : ExiledQuest.values()[quest];
	}

	public ExiledQuest offer() {
		return offered < 0 || offered >= ExiledQuest.values().length ? null : ExiledQuest.values()[offered];
	}

	/** Experience still needed for her next level, 0 at the last. */
	public int toNext() {
		return level >= 5 ? 0 : Math.max(0, LEVEL_XP[level - 1] - xp);
	}

	/** The share of all the pixies' gifts she gives: half of them all at her last level, a tenth at her first. */
	public float share() {
		return 0.1F * Math.max(1, Math.min(5, level));
	}

	static void remember(List<Long> list, long value, int max) {
		list.remove(value);
		list.add(value);
		while (list.size() > max) {
			list.remove(0);
		}
	}

	public CompoundTag toTag() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Version", 3);
		tag.putInt("Stage", stage);
		tag.putInt("Variant", variant);
		tag.putBoolean("Out", out);
		tag.putInt("Level", level);
		tag.putInt("Xp", xp);
		tag.putInt("Quest", quest);
		tag.putInt("Offered", offered);
		tag.putInt("Progress", progress);
		if (target != null) {
			tag.putLong("Target", target.asLong());
			tag.putString("TargetLevel", targetLevel);
		}
		if (via != null) {
			tag.putLong("Via", via.asLong());
		}
		if (checkpoint != null) {
			tag.putLong("Checkpoint", checkpoint.asLong());
		}
		tag.putLong("Done", done);
		tag.putLong("Skipped", skipped);
		tag.putLong("NextOffer", nextOffer);
		tag.putLong("NextComment", nextComment);
		tag.putLong("AbilityReady", abilityReady);
		tag.putInt("Talk", talk);
		tag.putBoolean("Guide", guide);
		tag.putLong("Said", said);
		tag.putIntArray("Recent", recent.stream().mapToInt(Integer::intValue).toArray());
		if (!told.isEmpty()) {
			CompoundTag lines = new CompoundTag();
			told.forEach(lines::putLong);
			tag.put("Told", lines);
		}
		tag.putLongArray("Villages", villages.stream().mapToLong(Long::longValue).toArray());
		tag.putLongArray("Chests", chests.stream().mapToLong(Long::longValue).toArray());
		if (home != null) {
			tag.putLong("Home", home.asLong());
		}
		tag.putLong("MetAt", metAt);
		tag.putInt("Gifts", gifts);
		if (visionFrom != null) {
			tag.putDouble("VisionX", visionFrom.x);
			tag.putDouble("VisionY", visionFrom.y);
			tag.putDouble("VisionZ", visionFrom.z);
			tag.putString("VisionLevel", visionLevel);
			tag.putFloat("VisionYaw", visionYaw);
		}
		return tag;
	}

	public static ExiledPixieData fromTag(CompoundTag tag) {
		ExiledPixieData data = new ExiledPixieData();
		data.stage = Math.max(NONE, Math.min(FRIEND, tag.getIntOr("Stage", NONE)));
		data.variant = tag.getIntOr("Variant", -1);
		data.out = tag.getBooleanOr("Out", true);
		data.level = Math.max(1, Math.min(5, tag.getIntOr("Level", 1)));
		data.xp = tag.getIntOr("Xp", 0);
		data.quest = tag.getIntOr("Quest", -1);
		data.offered = tag.getIntOr("Offered", -1);
		data.progress = tag.getIntOr("Progress", 0);
		data.target = tag.getLong("Target").map(BlockPos::of).orElse(null);
		data.targetLevel = tag.getStringOr("TargetLevel", "");
		data.via = tag.getLong("Via").map(BlockPos::of).orElse(null);
		data.checkpoint = tag.getLong("Checkpoint").map(BlockPos::of).orElse(null);
		data.done = tag.getLongOr("Done", 0L);
		data.skipped = tag.getLongOr("Skipped", 0L);
		data.nextOffer = tag.getLongOr("NextOffer", 0L);
		data.nextComment = tag.getLongOr("NextComment", 0L);
		data.abilityReady = tag.getLongOr("AbilityReady", 0L);
		data.talk = Math.max(0, Math.min(2, tag.getIntOr("Talk", 1)));
		data.guide = tag.getBooleanOr("Guide", true);
		data.said = tag.getLongOr("Said", 0L);
		for (int r : tag.getIntArray("Recent").orElse(new int[0])) {
			data.recent.add(r);
		}
		tag.getCompound("Told").ifPresent(lines -> {
			for (String kind : lines.keySet()) {
				data.told.put(kind, lines.getLongOr(kind, 0L));
			}
		});
		for (long v : tag.getLongArray("Villages").orElse(new long[0])) {
			data.villages.add(v);
		}
		for (long c : tag.getLongArray("Chests").orElse(new long[0])) {
			data.chests.add(c);
		}
		data.home = tag.getLong("Home").map(BlockPos::of).orElse(null);
		data.metAt = tag.getLongOr("MetAt", 0L);
		data.gifts = tag.getIntOr("Gifts", 0);
		if (tag.contains("VisionX")) {
			data.visionFrom = new net.minecraft.world.phys.Vec3(tag.getDoubleOr("VisionX", 0), tag.getDoubleOr("VisionY", 0), tag.getDoubleOr("VisionZ", 0));
			data.visionLevel = tag.getStringOr("VisionLevel", "");
			data.visionYaw = tag.getFloatOr("VisionYaw", 0F);
		}
		int version = tag.getIntOr("Version", 1);
		if (version < 2 && data.quest == ExiledQuest.CAKE.ordinal()) {
			// the cake once led to its ingredients: now to the witches' cottage, looked for anew
			data.target = null;
			data.checkpoint = null;
			data.progress = 0;
		}
		if (version < 3 && data.quest == ExiledQuest.FRIENDS.ordinal()) {
			// her friends' place was once any pixie village (and her ring there could be under the land): her
			// ring there goes and the great tree or the pixies' cave is looked for
			data.relook = true;
		}
		return data;
	}
}
