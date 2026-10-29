package com.iafabric.item;



import com.iafabric.util.ItemNbt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonSkull;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemDragonSkull extends Item {
    private final int dragonType;

    public ItemDragonSkull(int dragonType) {
        super(IafItemRegistry.props().stacksTo(1));
        this.dragonType = dragonType;
    }

    static String getName(int type) {
        return "dragon_skull_%s".formatted(getType(type));
    }

    private static String getType(int type) {
        if (type == 2) {
            return "lightning";
        } else if (type == 1) {
            return "ice";
        } else {
            return "fire";
        }
    }

    @Override
    public void onCraftedBy(ItemStack itemStack, Level craftLevel, Player player) {
        ItemNbt.setTag(itemStack, new CompoundTag());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level tickLevel, Entity entityIn, int slotId, boolean isSelected) {
        if (!(tickLevel instanceof ServerLevel worldIn)) {
            return;
        }
        EquipmentSlot slot = com.iafabric.compat.ItemSlots.slotOf(entityIn, stack, isSelected);
        if (!ItemNbt.hasTag(stack)) {
            ItemNbt.setTag(stack, new CompoundTag());
            ItemNbt.update(stack, nbtTag -> nbtTag.putInt("Stage", 4));
            ItemNbt.update(stack, nbtTag -> nbtTag.putInt("DragonAge", 75));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        String iceorfire = "dragon." + getType(dragonType);
        tooltip.add(Component.translatable(iceorfire).withStyle(ChatFormatting.GRAY));
        if (ItemNbt.hasTag(stack)) {
            tooltip.add(Component.translatable("dragon.stage").withStyle(ChatFormatting.GRAY).append(Component.literal(" " + ItemNbt.getOrEmpty(stack).iceandfire_fabric$getIntOr("Stage", 1))));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getPlayer().getItemInHand(context.getHand());
        /*
         * EntityDragonEgg egg = new EntityDragonEgg(worldIn);
         * egg.setPosition(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() +
         * 0.5); if(!worldIn.isRemote){ worldIn.spawnEntityInWorld(egg); }
         */
        if (ItemNbt.hasTag(stack)) {
            EntityDragonSkull skull = new EntityDragonSkull(IafEntityRegistry.DRAGON_SKULL, context.getLevel());
            skull.setDragonType(dragonType);
            skull.setStage(ItemNbt.getOrEmpty(stack).iceandfire_fabric$getIntOr("Stage", 0));
            skull.setDragonAge(ItemNbt.getOrEmpty(stack).iceandfire_fabric$getIntOr("DragonAge", 0));
            BlockPos offset = context.getClickedPos().relative(context.getClickedFace(), 1);
            skull.moveTo(offset.getX() + 0.5, offset.getY(), offset.getZ() + 0.5, 0, 0);
            float yaw = context.getPlayer().getYRot();
            if (context.getClickedFace() != Direction.UP) {
                yaw = context.getPlayer().getDirection().toYRot();
            }
            skull.setYaw(yaw);
            if (stack.hasCustomHoverName()) {
                skull.setCustomName(stack.getHoverName());
            }
            if (!context.getLevel().isClientSide) {
                context.getLevel().addFreshEntity(skull);
            }
            if (!context.getPlayer().isCreative()) {
                stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;

    }
}
