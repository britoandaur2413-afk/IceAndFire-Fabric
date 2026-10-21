package com.iafabric.item;



import com.iafabric.util.ItemNbt;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.component.SwingAnimation;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.world.DragonPosWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemSummoningCrystal extends Item {


    public ItemSummoningCrystal() {
        super(IafItemRegistry.props().stacksTo(1));
    }

    public static boolean hasDragon(ItemStack stack) {
        if (stack.getItem() instanceof ItemSummoningCrystal && ItemNbt.hasTag(stack)) {
            for (String tagInfo : ItemNbt.getOrEmpty(stack).keySet()) {
                if (tagInfo.contains("Dragon")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onCraftedBy(ItemStack itemStack, Player player) {
        ItemNbt.setTag(itemStack, new CompoundTag());
    }

    public ItemStack onItemUseFinish(Level worldIn, LivingEntity LivingEntity) {
        return new ItemStack(this);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {

        boolean flag = false;
        String desc = "entity.iceandfire_fabric.fire_dragon";
        if (stack.getItem() == IafItemRegistry.SUMMONING_CRYSTAL_ICE) {
            desc = "entity.iceandfire_fabric.ice_dragon";
        }
        if (stack.getItem() == IafItemRegistry.SUMMONING_CRYSTAL_LIGHTNING) {
            desc = "entity.iceandfire_fabric.lightning_dragon";
        }
        if (ItemNbt.hasTag(stack)) {
            for (String tagInfo : ItemNbt.getOrEmpty(stack).keySet()) {
                if (tagInfo.contains("Dragon")) {
                    CompoundTag dragonTag = ItemNbt.getOrEmpty(stack).getCompoundOrEmpty(tagInfo);
                    String dragonName = Component.translatable(desc).getString();
                    if (!dragonTag.getString("CustomName").isEmpty()) {
                        dragonName = dragonTag.getStringOr("CustomName", "");
                    }
                    tooltip.accept(Component.translatable("item.iceandfire_fabric.summoning_crystal.bound", dragonName).withStyle(ChatFormatting.GRAY));
                    flag = true;
                }
            }
        }
        if (!flag) {
            tooltip.accept(Component.translatable("item.iceandfire_fabric.summoning_crystal.desc_0").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("item.iceandfire_fabric.summoning_crystal.desc_1").withStyle(ChatFormatting.GRAY));

        }

    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getPlayer().getItemInHand(context.getHand());
        boolean flag = false;
        BlockPos offsetPos = context.getClickedPos().relative(context.getClickedFace());
        float yaw = context.getPlayer().getYRot();
        boolean displayError = false;
        if (stack.getItem() == this && hasDragon(stack)) {
            int dragonCount = 0;
            if (ItemNbt.hasTag(stack)) {
                for (String tagInfo : ItemNbt.getOrEmpty(stack).keySet()) {
                    if (tagInfo.contains("Dragon")) {
                        dragonCount++;
                        CompoundTag dragonTag = ItemNbt.getOrEmpty(stack).getCompoundOrEmpty(tagInfo);
                        UUID id = dragonTag.read("DragonUUID", UUIDUtil.CODEC).orElse(null);
                        if (id != null) {
                            if (!context.getLevel().isClientSide()) {
                                try {
                                    Entity entity = context.getLevel().getServer().getLevel(context.getPlayer().level().dimension()).getEntity(id);
                                    if (entity != null) {
                                        flag = true;
                                        summonEntity(entity, context.getLevel(), offsetPos, yaw);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    displayError = true;
                                }
                                // ForgeChunkManager.Ticket ticket = null;
                                DragonPosWorldData data = DragonPosWorldData.get(context.getLevel());
                                BlockPos dragonChunkPos = null;
                                if (data != null) {
                                    dragonChunkPos = data.getDragonPos(id);
                                }
                                if (IafConfig.chunkLoadSummonCrystal) {
                                    try {
                                        boolean flag2 = false;
                                        if (!flag) {//server side but couldn't find dragon
                                            if (data != null) {
                                                if (context.getLevel().isClientSide()) {
                                                    ServerLevel serverWorld = (ServerLevel) context.getLevel();
                                                    ChunkPos pos = ChunkPos.containing(dragonChunkPos);
                                                    serverWorld.setChunkForced(pos.x(), pos.z(), true);
                                                }
                                                /*ticket = ForgeChunkManager.requestPlayerTicket(IceAndFire.INSTANCE, player.getName(), worldIn, ForgeChunkManager.Type.NORMAL);
                                                if (ticket != null) {
                                                    if (dragonChunkPos != null) {
                                                        ForgeChunkManager.forceChunk(ticket, new ChunkPos(dragonChunkPos));
                                                    } else {
                                                        displayError = true;
                                                    }
                                                    lastChunkTicket = ticket;
                                                    flag2 = true;
                                                }*/
                                            }
                                        }
                                        if (flag2) {
                                            try {
                                                Entity entity = context.getLevel().getServer().getLevel(context.getPlayer().level().dimension()).getEntity(id);
                                                if (entity != null) {
                                                    flag = true;
                                                    summonEntity(entity, context.getLevel(), offsetPos, yaw);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                       /* if (flag && lastChunkTicket != null && dragonChunkPos != null) {
                                            ForgeChunkManager.releaseTicket(lastChunkTicket);
                                            lastChunkTicket = null;
                                        }*/
                                    } catch (Exception e) {
                                        IafMod.LOGGER.warn("Could not load chunk when summoning dragon");
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (flag) {
                context.getPlayer().playSound(SoundEvents.ENDERMAN_TELEPORT, 1, 1);
                context.getPlayer().playSound(SoundEvents.GLASS_BREAK, 1, 1);
                context.getPlayer().swing(context.getHand(), SwingAnimation.DEFAULT, true);
                context.getPlayer().sendOverlayMessage(Component.translatable("message.iceandfire_fabric.dragonTeleport"));
                ItemNbt.setTag(stack, new CompoundTag());
            } else if (displayError) {
                context.getPlayer().sendOverlayMessage(Component.translatable("message.iceandfire_fabric.noDragonTeleport"));

            }
        }
        return InteractionResult.PASS;
    }

    public void summonEntity(Entity entity, Level worldIn, BlockPos offsetPos, float yaw) {
        entity.snapTo(offsetPos.getX() + 0.5D, offsetPos.getY() + 0.5D, offsetPos.getZ() + 0.5D, yaw, 0);
        if (entity instanceof EntityDragonBase) {
            ((EntityDragonBase) entity).setCrystalBound(false);
        }
        if (IafConfig.chunkLoadSummonCrystal) {
            DragonPosWorldData data = DragonPosWorldData.get(worldIn);
            if (data != null) {
                data.removeDragon(entity.getUUID());
            }
        }
    }


}
