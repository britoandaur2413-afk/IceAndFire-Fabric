package com.iafabric.item;



import com.iafabric.util.ItemNbt;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonEgg;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.enums.EnumDragonEgg;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemDragonEgg extends Item {
    public EnumDragonEgg type;

    public ItemDragonEgg(EnumDragonEgg type) {
        super(IafItemRegistry.props().stacksTo(1));
        this.type = type;
    }

        /** The shared translation key (getDescriptionId is final now). */
    public String iafDescriptionId() {
        return "item.iceandfire_fabric.dragonegg";
    }

    @Override
    public void onCraftedBy(ItemStack itemStack, Level craftLevel, Player player) {
        ItemNbt.setTag(itemStack, new CompoundTag());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("dragon." + type.toString().toLowerCase()).withStyle(type.color));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack itemstack = context.getPlayer().getItemInHand(context.getHand());
        BlockPos offset = context.getClickedPos().relative(context.getClickedFace());
        EntityDragonEgg egg = new EntityDragonEgg(IafEntityRegistry.DRAGON_EGG, context.getLevel());
        egg.setEggType(type);
        egg.moveTo(offset.getX() + 0.5, offset.getY(), offset.getZ() + 0.5, 0, 0);
        egg.onPlayerPlace(context.getPlayer());
        if (itemstack.hasCustomHoverName()) {
            egg.setCustomName(itemstack.getHoverName());
        }
        if (!context.getLevel().isClientSide) {
            context.getLevel().addFreshEntity(egg);
        }
        itemstack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.iafDescriptionId());
    }
}
