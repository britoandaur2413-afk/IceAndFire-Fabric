package com.iafabric.pixie;

import com.iafabric.item.IafItemRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * What a bonded pixie does in a fight, its own by its color, so each is for a different fight: one help it gives by
 * itself, slowly (a punch of light, a lift, a frost bolt, mending, a spark), and three spells its player calls with
 * the magic key, each learnt by giving the pixie a thing and usable from a level on, each with its wait.
 * Pink (strength): rage, a shock wave, a pillar of fire. Purple (jump): a blink, a gravity well, a fall of stars.
 * Blue (speed): an ice spear, an ice prison, a blizzard. Green (luck): roots, a blessing, rebirth.
 * Yellow (haste): a thunderbolt, overcharge, a spark storm.
 */
public enum PixieAbility {
	STRIKE(0, null, 1, 70, "strike"),
	RAGE(0, () -> Items.BLAZE_POWDER, 1, 900, "rage"),
	SHOCKWAVE(0, () -> IafItemRegistry.DRAGON_BONE, 3, 400, "shockwave"),
	PILLAR(0, () -> IafItemRegistry.FIRE_DRAGON_BLOOD, 5, 600, "pillar"),
	LIFT(1, null, 1, 100, "lift"),
	BLINK(1, () -> Items.ENDER_PEARL, 1, 240, "blink"),
	GRAVITY(1, () -> Items.PHANTOM_MEMBRANE, 3, 500, "gravity"),
	STARFALL(1, () -> Items.AMETHYST_SHARD, 5, 700, "starfall"),
	FROST(2, null, 1, 60, "frost"),
	SPEAR(2, () -> Items.BLUE_ICE, 1, 300, "spear"),
	PRISON(2, () -> Items.POWDER_SNOW_BUCKET, 3, 400, "prison"),
	BLIZZARD(2, () -> IafItemRegistry.ICE_DRAGON_BLOOD, 5, 700, "blizzard"),
	MEND(3, null, 1, 100, "mend"),
	ROOTS(3, () -> Items.SPORE_BLOSSOM, 1, 400, "roots"),
	BLESSING(3, () -> IafItemRegistry.AMBROSIA, 3, 800, "blessing"),
	REBIRTH(3, () -> Items.TOTEM_OF_UNDYING, 5, 6000, "rebirth"),
	SPARK(4, null, 1, 70, "spark"),
	THUNDER(4, () -> Items.COPPER_INGOT, 1, 240, "thunder"),
	OVERCHARGE(4, () -> Items.GLOWSTONE, 3, 800, "overcharge"),
	STORM(4, () -> IafItemRegistry.LIGHTNING_DRAGON_BLOOD, 5, 700, "storm");

	/** The pixie color it belongs to. */
	public final int color;
	private final Supplier<Item> teacher;
	/** The bond level it can be used from. */
	public final int minLevel;
	/** Ticks before it is used again. */
	public final int cooldown;
	/** Its picture (textures/gui/pixie_ability/name.png) and its name in texts. */
	public final String name;

	PixieAbility(int color, Supplier<Item> teacher, int minLevel, int cooldown, String name) {
		this.color = color;
		this.teacher = teacher;
		this.minLevel = minLevel;
		this.cooldown = cooldown;
		this.name = name;
	}

	public static PixieAbility of(int id) {
		return values()[Math.floorMod(id, values().length)];
	}

	public int bit() {
		return 1 << ordinal();
	}

	/** The help it gives by itself (one per color), not a spell. */
	public boolean support() {
		return teacher == null;
	}

	/** The thing that teaches it (none for the help it gives by itself). */
	public ItemStack teacher() {
		return teacher == null ? ItemStack.EMPTY : new ItemStack(teacher.get());
	}

	/** The pixie of that color's help. */
	public static PixieAbility supportOf(int color) {
		for (PixieAbility ability : values()) {
			if (ability.color == Math.floorMod(color, PixieBonds.COLORS) && ability.support()) {
				return ability;
			}
		}
		return STRIKE;
	}

	/** The pixie of that color's three spells, in order. */
	public static List<PixieAbility> spellsOf(int color) {
		List<PixieAbility> list = new ArrayList<>();
		for (PixieAbility ability : values()) {
			if (ability.color == Math.floorMod(color, PixieBonds.COLORS) && !ability.support()) {
				list.add(ability);
			}
		}
		return list;
	}

	/** The spell a thing teaches a pixie of that color, or null. */
	public static PixieAbility taughtBy(int color, Item item) {
		for (PixieAbility ability : spellsOf(color)) {
			if (ability.teacher.get() == item) {
				return ability;
			}
		}
		return null;
	}

	/** Whether a thing teaches a spell to a pixie of any color. */
	public static boolean teachesAny(Item item) {
		for (PixieAbility ability : values()) {
			if (!ability.support() && ability.teacher.get() == item) {
				return true;
			}
		}
		return false;
	}
}
