package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;
import com.iafabric.util.ItemNbt;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.UUIDUtil;
import com.iafabric.network.IafNetwork;
import com.iafabric.IafMod;
import com.iafabric.entity.util.MyrmexHive;
import com.iafabric.message.MessageGetMyrmexHive;
import com.iafabric.message.MessageSetMyrmexHiveNull;
import com.iafabric.world.MyrmexWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ItemMyrmexStaff extends Item {

    public ItemMyrmexStaff(boolean jungle) {
        super(IafItemRegistry.props().stacksTo(1));
    }

    @Override
    public void onCraftedBy(ItemStack itemStack, Level craftLevel, Player player) {
        ItemNbt.setTag(itemStack, new CompoundTag());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level tickLevel, Entity entity, int slotId, boolean isSelected) {
        if (!(tickLevel instanceof ServerLevel world)) {
            return;
        }
        EquipmentSlot slot = com.iafabric.compat.ItemSlots.slotOf(entity, stack, isSelected);
        if (!ItemNbt.hasTag(stack)) {
            ItemNbt.setTag(stack, new CompoundTag());
            ItemNbt.getOrEmpty(stack).iceandfire_fabric$store("HiveUUID", UUIDUtil.CODEC, new UUID(0, 0));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand hand) {
        ItemStack itemStackIn = playerIn.getItemInHand(hand);
        if (playerIn.isShiftKeyDown()) {
            return super.use(worldIn, playerIn, hand);
        }
        if (ItemNbt.hasTag(itemStackIn) && ItemNbt.getOrEmpty(itemStackIn).iceandfire_fabric$read("HiveUUID", UUIDUtil.CODEC).isPresent()) {
            UUID id = ItemNbt.getOrEmpty(itemStackIn).iceandfire_fabric$read("HiveUUID", UUIDUtil.CODEC).orElse(null);
            if (!worldIn.isClientSide) {
                MyrmexHive hive = MyrmexWorldData.get(worldIn).getHiveFromUUID(id);
                MyrmexWorldData.addHive(worldIn, new MyrmexHive());
                if (hive != null) {
                    IafNetwork.sendToAll(new MessageGetMyrmexHive(hive.toNBT()));
                } else {
                    IafNetwork.sendToAll(new MessageSetMyrmexHiveNull());
                }
            } else if (id != null && !id.equals(new UUID(0, 0))) {
                IafMod.PROXY.openMyrmexStaffGui(itemStackIn);
            }
        }
        playerIn.swing(hand, true);
        return InteractionResultHolder.pass(playerIn.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getPlayer().isShiftKeyDown()) {
            return super.useOn(context);
        } else {
            CompoundTag tag = ItemNbt.getTag(context.getPlayer().getItemInHand(context.getHand()));
            if (tag != null && tag.iceandfire_fabric$read("HiveUUID", UUIDUtil.CODEC).isPresent()) {
                UUID id = tag.iceandfire_fabric$read("HiveUUID", UUIDUtil.CODEC).orElse(null);
                if (!context.getLevel().isClientSide) {
                    MyrmexHive hive = MyrmexWorldData.get(context.getLevel()).getHiveFromUUID(id);
                    if (hive != null) {
                        IafNetwork.sendToAll(new MessageGetMyrmexHive(hive.toNBT()));
                    } else {
                        IafNetwork.sendToAll(new MessageSetMyrmexHiveNull());
                    }
                } else if (id != null && !id.equals(new UUID(0, 0))) {
                    IafMod.PROXY.openMyrmexAddRoomGui(context.getPlayer().getItemInHand(context.getHand()), context.getClickedPos(), context.getPlayer().getDirection());
                }
            }
            context.getPlayer().swing(context.getHand(), true);
            return InteractionResult.SUCCESS;
        }
    }
}
