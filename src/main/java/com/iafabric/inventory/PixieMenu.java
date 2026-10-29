package com.iafabric.inventory;

import com.iafabric.item.ItemPixieOrb;
import com.iafabric.pixie.PixieBonding;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A bonded pixie's own menu: its orbs (a slot for each kind, all can be worn), a slot to give it things (pixie dust
 * for experience, a dragon's blood or other things to learn a spell), its help in a fight and its spells (the one
 * chosen is called with the magic key).
 */
public class PixieMenu extends AbstractContainerMenu {
	public static final int BUTTON_FEED = 0;
	public static final int BUTTON_ABILITY = 100;
	public static final int BUTTON_CALL = 200;
	/** Where the slots' frames are, from the menu's corner (the screen draws them there). */
	public static final int ORB_X = 78;
	public static final int ORB_Y = 70;
	public static final int FEED_X = 172;
	public static final int FEED_Y = 110;
	public static final int INV_X = 34;
	public static final int INV_Y = 144;
	private static final int ORBS = 5;
	private static final int FEED = 5;
	private static final int INV_START = 6;
	private static final int INV_END = 42;

	public final int color;
	private final Player player;
	private final SimpleContainer feed = new SimpleContainer(1);

	/** On the client: the slots are filled by the server. */
	public PixieMenu(int id, Inventory inventory, Integer color) {
		this(id, inventory, color, new SimpleContainer(ORBS));
	}

	public PixieMenu(int id, Inventory inventory, int color, Container orbs) {
		super(IafContainerRegistry.PIXIE_MENU, id);
		this.color = color;
		this.player = inventory.player;
		for (int i = 0; i < ORBS; i++) {
			final int kind = i;
			this.addSlot(new Slot(orbs, i, ORB_X + 1 + i * 20, ORB_Y + 1) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return stack.getItem() instanceof ItemPixieOrb orb && orb.kind.ordinal() == kind;
				}

				@Override
				public int getMaxStackSize() {
					return 1;
				}
			});
		}
		this.addSlot(new Slot(feed, 0, FEED_X + 1, FEED_Y + 1) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return PixieBonding.canFeed(stack);
			}
		});
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + 1 + col * 18, INV_Y + 1 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, INV_X + 1 + col * 18, INV_Y + 59));
		}
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return true;
		}
		if (id == BUTTON_FEED) {
			PixieBonding.feed(serverPlayer, color, feed);
			return true;
		}
		if (id >= BUTTON_ABILITY && id < BUTTON_ABILITY + 32) {
			PixieBonding.selectSpell(serverPlayer, color, id - BUTTON_ABILITY);
			return true;
		}
		if (id == BUTTON_CALL) {
			if (PixieBonding.get(serverPlayer).active == color) {
				PixieBonding.rest(serverPlayer, false);
			} else {
				PixieBonding.summon(serverPlayer, color);
			}
			return true;
		}
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < INV_START) {
			if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) {
				return ItemStack.EMPTY;
			}
		} else if (stack.getItem() instanceof ItemPixieOrb orb && !this.slots.get(orb.kind.ordinal()).hasItem()) {
			if (!this.moveItemStackTo(stack, orb.kind.ordinal(), orb.kind.ordinal() + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (PixieBonding.canFeed(stack)) {
			if (!this.moveItemStackTo(stack, FEED, FEED + 1, false)) {
				return ItemStack.EMPTY;
			}
		} else if (index < INV_END - 9) {
			if (!this.moveItemStackTo(stack, INV_END - 9, INV_END, false)) {
				return ItemStack.EMPTY;
			}
		} else if (!this.moveItemStackTo(stack, INV_START, INV_END - 9, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		if (stack.getCount() == copy.getCount()) {
			return ItemStack.EMPTY;
		}
		slot.onTake(player, stack);
		return copy;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.isAlive() && (!(player instanceof ServerPlayer serverPlayer) || PixieBonding.get(serverPlayer).get(color).bonded);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		this.clearContainer(player, feed);
	}
}
