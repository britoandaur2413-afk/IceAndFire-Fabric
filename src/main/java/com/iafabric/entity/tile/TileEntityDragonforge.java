package com.iafabric.entity.tile;

import net.minecraft.core.HolderLookup;
import com.iafabric.compat.ValueInput;
import com.iafabric.compat.ValueOutput;
import com.iafabric.network.IafNetwork;
import com.iafabric.IafMod;
import com.iafabric.block.BlockDragonforgeBricks;
import com.iafabric.block.BlockDragonforgeCore;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.DragonType;
import com.iafabric.inventory.ContainerDragonForge;
import com.iafabric.message.MessageUpdateDragonforge;
import com.iafabric.recipe.DragonForgeRecipe;
import com.iafabric.recipe.IafRecipeRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;

public class TileEntityDragonforge extends BaseContainerBlockEntity implements WorldlyContainer {

    private static final int[] SLOTS_TOP = new int[]{0, 1};
    private static final int[] SLOTS_BOTTOM = new int[]{2};
    private static final int[] SLOTS_SIDES = new int[]{0, 1};
    private static final Direction[] HORIZONTALS = new Direction[]{
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };
    public int fireType;
    public int cookTime;
    public int lastDragonFlameTimer = 0;
    private NonNullList<ItemStack> forgeItemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
    private boolean prevAssembled;
    private boolean canAddFlameAgain = true;

    public TileEntityDragonforge(BlockPos pos, BlockState state) {
        super(IafTileEntityRegistry.DRAGONFORGE_CORE, pos, state);
    }

    public TileEntityDragonforge(BlockPos pos, BlockState state, int fireType) {
        super(IafTileEntityRegistry.DRAGONFORGE_CORE, pos, state);
        this.fireType = fireType;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityDragonforge entityDragonforge) {
        boolean flag = entityDragonforge.isBurning();
        boolean flag1 = false;
        entityDragonforge.fireType = entityDragonforge.getFireType(entityDragonforge.getBlockState().getBlock());
        if (entityDragonforge.lastDragonFlameTimer > 0) {
            entityDragonforge.lastDragonFlameTimer--;
        }
        entityDragonforge.updateGrills(entityDragonforge.assembled());
        if (!level.isClientSide) {
            if (entityDragonforge.prevAssembled != entityDragonforge.assembled()) {
                BlockDragonforgeCore.setState(entityDragonforge.fireType, entityDragonforge.prevAssembled, level, pos);
            }
            entityDragonforge.prevAssembled = entityDragonforge.assembled();
            if (!entityDragonforge.assembled())
                return;
        }
        if (entityDragonforge.cookTime > 0 && entityDragonforge.canSmelt() && entityDragonforge.lastDragonFlameTimer == 0) {
            entityDragonforge.cookTime--;
        }
        if (entityDragonforge.getItem(0).isEmpty() && !level.isClientSide) {
            entityDragonforge.cookTime = 0;
        }
        if (!entityDragonforge.level.isClientSide) {
            if (entityDragonforge.isBurning()) {
                if (entityDragonforge.canSmelt()) {
                    ++entityDragonforge.cookTime;
                    if (entityDragonforge.cookTime >= entityDragonforge.getMaxCookTime()) {
                        entityDragonforge.cookTime = 0;
                        entityDragonforge.smeltItem();
                        flag1 = true;
                    }
                } else {
                    if (entityDragonforge.cookTime > 0) {
                        IafNetwork.sendToAll(new MessageUpdateDragonforge(pos.asLong(), entityDragonforge.cookTime));
                        entityDragonforge.cookTime = 0;
                    }
                }
            } else if (!entityDragonforge.isBurning() && entityDragonforge.cookTime > 0) {
                entityDragonforge.cookTime = Mth.clamp(entityDragonforge.cookTime - 2, 0,
                        entityDragonforge.getMaxCookTime());
            }

            if (flag != entityDragonforge.isBurning()) {
                flag1 = true;
            }
        }

        if (flag1) {
            entityDragonforge.setChanged();
        }
        if (!entityDragonforge.canAddFlameAgain) {
            entityDragonforge.canAddFlameAgain = true;
        }
    }

    @Override
    public int getContainerSize() {
        return this.forgeItemStacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.forgeItemStacks) {
            if (!itemstack.isEmpty())
                return false;
        }

        return true;
    }

    private void updateGrills(boolean grill) {
        for (Direction facing : HORIZONTALS) {
            BlockPos grillPos = this.getBlockPos().relative(facing);
            if (grillMatches(level.getBlockState(grillPos).getBlock())) {
                BlockState grillState = getGrillBlock().defaultBlockState().setValue(BlockDragonforgeBricks.GRILL, grill);
                if (level.getBlockState(grillPos) != grillState) {
                    level.setBlockAndUpdate(grillPos, grillState);
                }
            }
        }
    }

    public Block getGrillBlock() {
        switch (fireType) {
            case 1:
                return IafBlockRegistry.DRAGONFORGE_ICE_BRICK;
            case 2:
                return IafBlockRegistry.DRAGONFORGE_LIGHTNING_BRICK;
            default:
                return IafBlockRegistry.DRAGONFORGE_FIRE_BRICK; // isFire == 0
        }
    }

    public boolean grillMatches(Block block) {
        switch (fireType) {
            case 0:
                return block == IafBlockRegistry.DRAGONFORGE_FIRE_BRICK;
            case 1:
                return block == IafBlockRegistry.DRAGONFORGE_ICE_BRICK;
            case 2:
                return block == IafBlockRegistry.DRAGONFORGE_LIGHTNING_BRICK;
            default:
                return false;
        }
    }

    @Override
    public ItemStack getItem(int index) {
        return this.forgeItemStacks.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return ContainerHelper.removeItem(this.forgeItemStacks, index, count);
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(this.forgeItemStacks, index);
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        ItemStack itemstack = this.forgeItemStacks.get(index);
        boolean flag = !stack.isEmpty() && ItemStack.isSameItem(stack, itemstack)
                && ItemStack.isSameItemSameTags(stack, itemstack);
        this.forgeItemStacks.set(index, stack);

        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        if (index == 0 && !flag
                || this.cookTime > this.getMaxCookTime()) {
            this.cookTime = 0;
            this.setChanged();
        }
    }

    @Override
    public void load(CompoundTag compoundTag) {
		ValueInput compound = new ValueInput(compoundTag);
        super.load(compoundTag);
        this.forgeItemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compound.tag(), this.forgeItemStacks);
        this.cookTime = compound.getIntOr("CookTime", 0);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
		ValueOutput compound = new ValueOutput(compoundTag);
        compound.putInt("CookTime", (short) this.cookTime);
        ContainerHelper.saveAllItems(compound.tag(), this.forgeItemStacks);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    public boolean isBurning() {
        return this.cookTime > 0;
    }

    public int getFireType(Block block) {
        if (block == IafBlockRegistry.DRAGONFORGE_FIRE_CORE
                || block == IafBlockRegistry.DRAGONFORGE_FIRE_CORE_DISABLED) {
            return 0;
        }
        if (block == IafBlockRegistry.DRAGONFORGE_ICE_CORE || block == IafBlockRegistry.DRAGONFORGE_ICE_CORE_DISABLED) {
            return 1;
        }
        if (block == IafBlockRegistry.DRAGONFORGE_LIGHTNING_CORE
                || block == IafBlockRegistry.DRAGONFORGE_LIGHTNING_CORE_DISABLED) {
            return 2;
        }
        return 0;
    }

    public String getTypeID() {
        switch (getFireType(this.getBlockState().getBlock())) {
            case 0:
                return "fire";
            case 1:
                return "ice";
            case 2:
                return "lightning";
            default:
                return "";
        }
    }

    public int getMaxCookTime() {
        return getCurrentRecipe().map(DragonForgeRecipe::getCookTime).orElse(100);
    }

    private Block getDefaultOutput() {
        return fireType == 1 ? IafBlockRegistry.DRAGON_ICE : IafBlockRegistry.ASH;
    }

    private ItemStack getCurrentResult() {
        Optional<DragonForgeRecipe> recipe = getCurrentRecipe();
        if (recipe.isPresent())
            return recipe.get().getResultItem();
        return new ItemStack(getDefaultOutput());
    }

    public Optional<DragonForgeRecipe> getCurrentRecipe() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        DragonForgeRecipe.Input input = new DragonForgeRecipe.Input(this.getItem(0), this.getItem(1), this.getTypeID());
        return serverLevel.getRecipeManager().getRecipeFor(IafRecipeRegistry.DRAGON_FORGE_TYPE, input, serverLevel);
    }

    public List<DragonForgeRecipe> getRecipes() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return List.of();
        }
        return serverLevel.getRecipeManager().getAllRecipesFor(IafRecipeRegistry.DRAGON_FORGE_TYPE);
    }

    public boolean canSmelt() {
        ItemStack cookStack = this.forgeItemStacks.get(0);
        if (cookStack.isEmpty())
            return false;

        ItemStack forgeRecipeOutput = getCurrentResult();

        if (forgeRecipeOutput.isEmpty())
            return false;

        ItemStack outputStack = this.forgeItemStacks.get(2);
        if (!outputStack.isEmpty() && !ItemStack.isSameItem(outputStack, forgeRecipeOutput))
            return false;

        int calculatedOutputCount = outputStack.getCount() + forgeRecipeOutput.getCount();
        return (calculatedOutputCount <= this.getMaxStackSize()
                && calculatedOutputCount <= outputStack.getMaxStackSize());
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return player.distanceToSqr(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                    this.worldPosition.getZ() + 0.5D) <= 64.0D;
        }
    }

    public void smeltItem() {
        if (!this.canSmelt())
            return;

        ItemStack cookStack = this.forgeItemStacks.get(0);
        ItemStack bloodStack = this.forgeItemStacks.get(1);
        ItemStack outputStack = this.forgeItemStacks.get(2);

        ItemStack output = getCurrentResult();

        if (outputStack.isEmpty()) {
            this.forgeItemStacks.set(2, output.copy());
        } else {
            outputStack.grow(output.getCount());
        }

        cookStack.shrink(1);
        bloodStack.shrink(1);
    }

    @Override
    public boolean canPlaceItem(int index, @NotNull ItemStack stack) {
        switch (index) {
            case 1:
                return getRecipes().stream().anyMatch(item -> item.isValidBlood(stack));
            case 0:
                return true;//getRecipes().stream().anyMatch(item -> item.isValidInput(stack))
            default:
                return false;
        }
    }

    @Override
    public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
        if (side == Direction.DOWN) {
            return SLOTS_BOTTOM;
        } else {
            return side == Direction.UP ? SLOTS_TOP : SLOTS_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, @NotNull ItemStack itemStackIn, Direction direction) {
        return this.canPlaceItem(index, itemStackIn);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, @NotNull ItemStack stack, @NotNull Direction direction) {
        if (direction == Direction.DOWN && index == 1) {
            Item item = stack.getItem();

            return item == Items.WATER_BUCKET || item == Items.BUCKET;
        }

        return true;
    }

    @Override
    public void clearContent() {
        this.forgeItemStacks.clear();
    }


    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.iceandfire_fabric.dragonforge_" + DragonType.getNameFromInt(fireType) + "_core");
    }

    public void transferPower(int i) {
        if (!level.isClientSide) {
            if (this.canSmelt()) {
                if (canAddFlameAgain) {
                    cookTime = Math.min(this.getMaxCookTime() + 1,
                            cookTime + i);
                    canAddFlameAgain = false;
                }
            } else {
                cookTime = 0;
            }
            IafNetwork.sendToAll(new MessageUpdateDragonforge(worldPosition.asLong(), cookTime));
        }
        lastDragonFlameTimer = 40;
    }

    private boolean checkBoneCorners(BlockPos pos) {
        return doesBlockEqual(pos.north().east(), IafBlockRegistry.DRAGON_BONE_BLOCK)
                && doesBlockEqual(pos.north().west(), IafBlockRegistry.DRAGON_BONE_BLOCK)
                && doesBlockEqual(pos.south().east(), IafBlockRegistry.DRAGON_BONE_BLOCK)
                && doesBlockEqual(pos.south().west(), IafBlockRegistry.DRAGON_BONE_BLOCK);
    }

    private boolean checkBrickCorners(BlockPos pos) {
        return doesBlockEqual(pos.north().east(), getBrick()) && doesBlockEqual(pos.north().west(), getBrick())
                && doesBlockEqual(pos.south().east(), getBrick()) && doesBlockEqual(pos.south().west(), getBrick());
    }

    private boolean checkBrickSlots(BlockPos pos) {
        return doesBlockEqual(pos.north(), getBrick()) && doesBlockEqual(pos.east(), getBrick())
                && doesBlockEqual(pos.west(), getBrick()) && doesBlockEqual(pos.south(), getBrick());
    }

    private boolean checkY(BlockPos pos) {
        return doesBlockEqual(pos.above(), getBrick()) && doesBlockEqual(pos.below(), getBrick());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public boolean assembled() {
        return checkBoneCorners(worldPosition.below()) && checkBrickSlots(worldPosition.below()) && checkBrickCorners(worldPosition)
                && atleastThreeAreBricks(worldPosition) && checkY(worldPosition) && checkBoneCorners(worldPosition.above()) && checkBrickSlots(worldPosition.above());
    }

    private Block getBrick() {
        switch (fireType) {
            case 0:
                return IafBlockRegistry.DRAGONFORGE_FIRE_BRICK;
            case 1:
                return IafBlockRegistry.DRAGONFORGE_ICE_BRICK;
            default:
                return IafBlockRegistry.DRAGONFORGE_LIGHTNING_BRICK;
        }
    }

    private boolean doesBlockEqual(BlockPos pos, Block block) {
        return level.getBlockState(pos).getBlock() == block;
    }

    private boolean atleastThreeAreBricks(BlockPos pos) {
        int count = 0;
        for (Direction facing : HORIZONTALS) {
            if (level.getBlockState(pos.relative(facing)).getBlock() == getBrick()) {
                count++;
            }
        }
        return count > 2;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new ContainerDragonForge(id, this, playerInventory, new SimpleContainerData(0));
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, @NotNull Inventory player) {
        return new ContainerDragonForge(id, this, player, new SimpleContainerData(0));
    }

    protected NonNullList<ItemStack> getItems() {
        return this.forgeItemStacks;
    }

    protected void setItems(NonNullList<ItemStack> items) {
        this.forgeItemStacks = items;
    }
}
