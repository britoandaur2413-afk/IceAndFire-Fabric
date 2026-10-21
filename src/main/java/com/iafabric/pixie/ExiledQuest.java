package com.iafabric.pixie;

/**
 * The exiled pixie's quests, in the order she offers them ({@link #ordered}: those that came later into the game are
 * last here, where the order is what a save keeps); each is offered once its earlier ones are done or passed, done
 * once (never again), and passed when the player already went past it (a better weapon than the one it leads to, the
 * thing it seeks already theirs). Each one done, with her or not, gives back a memory of hers; all of them over, the
 * player is ready for Cryona (her last one, at its moment).
 */
public enum ExiledQuest {
	/** A cake for her friends: the witches she knows keep a chest full of them in their cottage. */
	CAKE(150),
	/** Her friends' sanctuary: the cake makes one of them the player's. */
	FRIENDS(200),
	/** An old graveyard nearby: ancient ruins to see. */
	GRAVEYARD(120),
	/** A dragon's skeleton: its bones, to arm oneself. */
	BONES(200),
	/** A cyclops: hurt one of his sheep and he comes; his eye is the prize. */
	CYCLOPS(250),
	/** A mausoleum, deeper ruins. */
	MAUSOLEUM(150),
	/** A dragon's egg from a nest or a cave. */
	EGG(250),
	/** The gorgon's temple (not to look at her). */
	TEMPLE(150),
	/**
	 * Nameless till its moment: "not yet; I will lend you my strength when the right moment comes". It is not
	 * offered: it comes the first time the player finds Cryona (her glade, her gift: FairyVision).
	 */
	CRYONA(400),
	/** A village nearby, the nearest there really is: her first quest, anecdotes of villagers on the way. */
	VILLAGE(100),
	/** Ride a horse. */
	HORSE(100),
	/** Raise a pixie of one's own to its fifth level. */
	PIXIE5(200),
	/** Beat a troll. */
	TROLL(200),
	/** Ride a hippogryph. */
	HIPPOGRYPH(200),
	/** Beat a hydra. */
	HYDRA(250);

	private static final ExiledQuest[] ORDER = {VILLAGE, CAKE, FRIENDS, HORSE, GRAVEYARD, BONES, PIXIE5, TROLL, CYCLOPS, HIPPOGRYPH, MAUSOLEUM, EGG,
		TEMPLE, HYDRA, CRYONA};

	/** Her bond's experience for it. */
	public final int xp;

	ExiledQuest(int xp) {
		this.xp = xp;
	}

	/** All of them, in the order she offers them and her menu lists them. */
	public static ExiledQuest[] ordered() {
		return ORDER.clone();
	}

	/** The quests to be over before this one. */
	private ExiledQuest[] after() {
		return switch (this) {
			case CAKE, HORSE -> new ExiledQuest[]{VILLAGE};
			case FRIENDS -> new ExiledQuest[]{CAKE};
			case PIXIE5 -> new ExiledQuest[]{FRIENDS};
			case TROLL -> new ExiledQuest[]{BONES};
			case HIPPOGRYPH -> new ExiledQuest[]{HORSE};
			case HYDRA -> new ExiledQuest[]{CYCLOPS};
			case CYCLOPS -> new ExiledQuest[]{BONES};
			case MAUSOLEUM, EGG -> new ExiledQuest[]{CYCLOPS};
			case TEMPLE -> new ExiledQuest[]{MAUSOLEUM};
			default -> new ExiledQuest[0];
		};
	}

	public long bit() {
		return 1L << this.ordinal();
	}

	/** She offers it (her last one comes on its own, at its moment). */
	public boolean offered() {
		return this != CRYONA;
	}

	public String key() {
		return this.name().toLowerCase(java.util.Locale.ROOT);
	}

	/** Its earlier quests are done or passed. */
	public boolean unlocked(ExiledPixieData data) {
		for (ExiledQuest quest : this.after()) {
			if (!data.isOver(quest)) {
				return false;
			}
		}
		return true;
	}

	/** All of them over but her last one (which comes on its own): the player is ready for Cryona. */
	public static boolean ready(ExiledPixieData data) {
		for (ExiledQuest quest : ORDER) {
			if (quest.offered() && !data.isOver(quest)) {
				return false;
			}
		}
		return true;
	}

	/** How many are not over yet (her last one aside). */
	public static int left(ExiledPixieData data) {
		int left = 0;
		for (ExiledQuest quest : ORDER) {
			if (quest.offered() && !data.isOver(quest)) {
				left++;
			}
		}
		return left;
	}

	public static ExiledQuest of(int id) {
		return id < 0 || id >= values().length ? null : values()[id];
	}
}
