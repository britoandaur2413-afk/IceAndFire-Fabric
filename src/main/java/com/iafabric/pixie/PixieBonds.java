package com.iafabric.pixie;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * A player's pixie bonds: one slot for each pixie color, each with its level, experience, shield, orbs (one of each
 * kind), the spells it knows, the one its magic key calls and when each is ready again, and the pixie itself (saved
 * while it rests); and which one is out with the player now.
 */
public class PixieBonds {
	public static final int COLORS = 5;
	/**
	 * Experience to reach levels 2, 3, 4 and 5: from a level to the next, one stack of pixie dust more each time
	 * (1, 2, 3 and 4 stacks: ten in all; a dust is 25, a kill half its foe's health).
	 */
	public static final int[] LEVEL_XP = {1600, 4800, 9600, 16000};
	/** The part of a bonded pixie's damage its player takes, by level. */
	public static final float[] SHARE = {0.30F, 0.24F, 0.18F, 0.11F, 0.05F};

	public final Bond[] bonds = new Bond[COLORS];
	/** The color of the pixie out with the player, -1 for none. */
	public int active = -1;
	/** The living pixie that is out (a pixie with another token is an old copy and goes). */
	public UUID activeId;
	public long token;

	public PixieBonds() {
		for (int i = 0; i < COLORS; i++) {
			bonds[i] = new Bond();
		}
	}

	public Bond get(int color) {
		return bonds[Math.floorMod(color, COLORS)];
	}

	public static class Bond {
		public boolean bonded;
		public int level = 1;
		public int xp;
		public float shield;
		/** Its orbs: a slot for each kind ({@link OrbKind}), each blocks its kind of damage. */
		public final ItemStack[] orbs = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
		/** The spells it knows (bits of {@link PixieAbility}); its help it always knows. */
		public int known;
		/** The spell its magic key calls ({@link PixieAbility} ordinal), -1 for none. */
		public int spell = -1;
		/** When each ability is ready again (game time). */
		public final long[] ready = new long[PixieAbility.values().length];
		/** The pixie as it was when it went to rest (its name, its look). */
		public CompoundTag pixie = new CompoundTag();

		public int maxShield() {
			return 2 + 2 * level;
		}

		public boolean knows(PixieAbility ability) {
			return ability.support() || (known & ability.bit()) != 0;
		}

		/** Known and its level reached: it can be used. */
		public boolean canUse(PixieAbility ability) {
			return knows(ability) && level >= ability.minLevel;
		}

		/** The spell the magic key calls, or null. */
		public PixieAbility selected() {
			return spell < 0 || spell >= PixieAbility.values().length ? null : PixieAbility.of(spell);
		}

		/** Whether an orb of that kind is in its slot. */
		public boolean blocks(OrbKind kind) {
			return !orbs[kind.ordinal()].isEmpty();
		}

		public float share() {
			return SHARE[Math.max(0, Math.min(4, level - 1))];
		}

		/** Experience still needed for the next level, 0 at level 5. */
		public int toNext() {
			return level >= 5 ? 0 : Math.max(0, LEVEL_XP[level - 1] - xp);
		}
	}

	public CompoundTag toTag() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Active", active);
		if (activeId != null) {
			tag.putString("ActiveId", activeId.toString());
		}
		tag.putLong("Token", token);
		ListTag list = new ListTag();
		for (Bond bond : bonds) {
			CompoundTag b = new CompoundTag();
			b.putBoolean("Bonded", bond.bonded);
			b.putInt("Level", bond.level);
			b.putInt("Xp", bond.xp);
			b.putFloat("Shield", bond.shield);
			ListTag orbs = new ListTag();
			for (ItemStack orb : bond.orbs) {
				orbs.add(net.minecraft.nbt.StringTag.valueOf(orb.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(orb.getItem()).toString()));
			}
			b.put("Orbs", orbs);
			b.putInt("V", 3);
			b.putInt("Known", bond.known);
			b.putInt("Spell", bond.spell);
			b.putLongArray("Ready", bond.ready.clone());
			b.put("Pixie", bond.pixie);
			list.add(b);
		}
		tag.put("Bonds", list);
		return tag;
	}

	private static ItemStack orbOf(String id) {
		if (id.isEmpty()) {
			return ItemStack.EMPTY;
		}
		net.minecraft.resources.Identifier key = net.minecraft.resources.Identifier.tryParse(id);
		return key == null ? ItemStack.EMPTY : BuiltInRegistries.ITEM.getOptional(key).map(ItemStack::new).orElse(ItemStack.EMPTY);
	}

	public static PixieBonds fromTag(CompoundTag tag) {
		PixieBonds bonds = new PixieBonds();
		bonds.active = tag.getIntOr("Active", -1);
		String id = tag.getStringOr("ActiveId", "");
		bonds.activeId = id.isEmpty() ? null : UUID.fromString(id);
		bonds.token = tag.getLongOr("Token", 0L);
		ListTag list = tag.getListOrEmpty("Bonds");
		for (int i = 0; i < Math.min(COLORS, list.size()); i++) {
			CompoundTag b = list.getCompoundOrEmpty(i);
			Bond bond = bonds.bonds[i];
			bond.bonded = b.getBooleanOr("Bonded", false);
			bond.level = Math.max(1, Math.min(5, b.getIntOr("Level", 1)));
			bond.xp = b.getIntOr("Xp", 0);
			bond.shield = b.getFloatOr("Shield", 0F);
			ListTag orbs = b.getListOrEmpty("Orbs");
			for (int k = 0; k < Math.min(bond.orbs.length, orbs.size()); k++) {
				bond.orbs[k] = orbOf(orbs.getStringOr(k, ""));
			}
			// a bond saved with a single orb: it goes to its kind's slot
			ItemStack old = orbOf(b.getStringOr("Orb", ""));
			if (old.getItem() instanceof com.iafabric.item.ItemPixieOrb orb && bond.orbs[orb.kind.ordinal()].isEmpty()) {
				bond.orbs[orb.kind.ordinal()] = old;
			}
			if (b.getIntOr("V", 0) >= 3) {
				bond.known = b.getIntOr("Known", 0);
				bond.spell = b.getIntOr("Spell", -1);
				long[] ready = b.getLongArray("Ready").orElse(new long[0]);
				System.arraycopy(ready, 0, bond.ready, 0, Math.min(ready.length, bond.ready.length));
			}
			// (a bond from before the pixies' own spells starts with none known)
			bond.pixie = b.getCompoundOrEmpty("Pixie");
		}
		return bonds;
	}
}
