package com.iafabric.inventory;

import com.iafabric.IafMod;
import com.iafabric.entity.DragonSaddle;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemDragonArmor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ContainerDragon extends AbstractContainerMenu {
    /** The empty double saddle's place shows a saddle, as a horse's does. */
    private static final Identifier SADDLE_ICON = Identifier.withDefaultNamespace("container/slot/saddle");
    private final Container dragonInventory;
    private final EntityDragonBase dragon;

    public ContainerDragon(int i, Inventory playerInventory) {
        this(i, new SimpleContainer(6), playerInventory, null);
    }

    public ContainerDragon(int id, Container dragonInventory, Inventory playerInventory, EntityDragonBase rat) {
        super(IafContainerRegistry.DRAGON_CONTAINER, id);
        this.dragonInventory = dragonInventory;
        this.dragon = rat;
        byte b0 = 3;
        dragonInventory.startOpen(playerInventory.player);
        int i = (b0 - 4) * 18;
        this.addSlot(new Slot(dragonInventory, 0, 8, 54) {
            @Override
            public void setChanged() {
                this.container.setChanged();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && stack.getItem() instanceof BannerItem;
            }
        });
        this.addSlot(new Slot(dragonInventory, 1, 8, 18) {
            @Override
            public void setChanged() {
                this.container.setChanged();
            }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && !stack.isEmpty() && stack.getItem() instanceof ItemDragonArmor && ((ItemDragonArmor) stack.getItem()).dragonSlot == 0;
            }
        });
        this.addSlot(new Slot(dragonInventory, 2, 8, 36) {
            @Override
            public void setChanged() {
                this.container.setChanged();
            }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && !stack.isEmpty() && stack.getItem() instanceof ItemDragonArmor && ((ItemDragonArmor) stack.getItem()).dragonSlot == 1;
            }

        });
        this.addSlot(new Slot(dragonInventory, 3, 153, 18) {
            @Override
            public void setChanged() {
                this.container.setChanged();
            }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && !stack.isEmpty() && stack.getItem() instanceof ItemDragonArmor && ((ItemDragonArmor) stack.getItem()).dragonSlot == 2;
            }
        });
        this.addSlot(new Slot(dragonInventory, 4, 153, 36) {
            @Override
            public void setChanged() {
                this.container.setChanged();
            }
            @Override
            public boolean mayPlace(ItemStack stack) {
                return super.mayPlace(stack) && !stack.isEmpty() && stack.getItem() instanceof ItemDragonArmor && ((ItemDragonArmor) stack.getItem()).dragonSlot == 3;
            }
        });
        // the double saddle (DragonSaddle): there on a grown fire, ice or lightning dragon; it stays on while a friend sits behind
        this.addSlot(new Slot(dragonInventory, DragonSaddle.SLOT, 153, 54) {
            @Override
            public void setChanged() {
                this.container.setChanged();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                EntityDragonBase dragon = ContainerDragon.this.dragon();
                return super.mayPlace(stack) && stack.is(IafItemRegistry.DOUBLE_DRAGON_SADDLE) && dragon != null && DragonSaddle.fits(dragon);
            }

            @Override
            public boolean mayPickup(Player player) {
                EntityDragonBase dragon = ContainerDragon.this.dragon();
                return super.mayPickup(player) && (dragon == null || DragonSaddle.behind(dragon) == null);
            }

            @Override
            public boolean isActive() {
                EntityDragonBase dragon = ContainerDragon.this.dragon();
                return dragon != null && DragonSaddle.fits(dragon) || this.hasItem();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public Identifier getNoItemIcon() {
                return SADDLE_ICON;
            }
        });
        int j;
        int k;
        for (j = 0; j < 3; ++j) {
            for (k = 0; k < 9; ++k) {
                this.addSlot(new Slot(playerInventory, k + j * 9 + 9, 8 + k * 18, 150 + j * 18 + i));
            }
        }

        for (j = 0; j < 9; ++j) {
            this.addSlot(new Slot(playerInventory, j, 8 + j * 18, 208 + i));
        }
    }

    /** Its dragon: on the server the one it was opened for, on the player's game the one they clicked (kept by the proxy). */
    private EntityDragonBase dragon() {
        if (this.dragon != null) {
            return this.dragon;
        }
        return IafMod.PROXY.getReferencedMob() instanceof EntityDragonBase clicked ? clicked : null;
    }

    @Override
    public boolean stillValid(@NotNull Player playerIn) {
        return this.dragon == null || !this.dragon.hasInventoryChanged(this.dragonInventory) && this.dragonInventory.stillValid(playerIn) && this.dragon.isAlive() && this.dragon.distanceTo(playerIn) < 8.0F;
    }

    @Override
    public ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < this.dragonInventory.getContainerSize()) {
                if (!this.moveItemStackTo(itemstack1, this.dragonInventory.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (this.getSlot(2).mayPlace(itemstack1) && !this.getSlot(2).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (this.getSlot(3).mayPlace(itemstack1) && !this.getSlot(3).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (this.getSlot(4).mayPlace(itemstack1) && !this.getSlot(4).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, 4, 5, false)) {
                    return ItemStack.EMPTY;
                }

            } else if (this.getSlot(DragonSaddle.SLOT).mayPlace(itemstack1) && !this.getSlot(DragonSaddle.SLOT).hasItem()) {
                if (!this.moveItemStackTo(itemstack1, DragonSaddle.SLOT, DragonSaddle.SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.getSlot(0).mayPlace(itemstack1)) {
                if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.dragonInventory.getContainerSize() <= 6 || !this.moveItemStackTo(itemstack1, 6, this.dragonInventory.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public void removed(@NotNull Player playerIn) {
        super.removed(playerIn);
        this.dragonInventory.stopOpen(playerIn);
    }

}