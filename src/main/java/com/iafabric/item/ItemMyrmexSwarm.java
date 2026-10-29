package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;


import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityMyrmexSwarmer;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemMyrmexSwarm extends Item {
    private final boolean jungle;

    public ItemMyrmexSwarm(boolean jungle) {
        super(IafItemRegistry.props().stacksTo(1));
        this.jungle = jungle;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand hand) {
        ItemStack itemStackIn = playerIn.getItemInHand(hand);
        playerIn.startUsingItem(hand);
        playerIn.swing(hand, true);
        if (!playerIn.isCreative()) {
            itemStackIn.shrink(1);
            playerIn.getCooldowns().addCooldown(this, 20);
        }
        for (int i = 0; i < 5; i++) {
            EntityMyrmexSwarmer myrmex = new EntityMyrmexSwarmer(IafEntityRegistry.MYRMEX_SWARMER, worldIn);
            myrmex.setPos(playerIn.getX(), playerIn.getY(), playerIn.getZ());
            myrmex.setJungleVariant(jungle);
            myrmex.setSummonedBy(playerIn);
            myrmex.setFlying(true);
            if (!worldIn.isClientSide) {
                worldIn.addFreshEntity(myrmex);
            }
        }
        playerIn.getCooldowns().addCooldown(this, 1800);
        return InteractionResultHolder.pass(playerIn.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.myrmex_swarm.desc_0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.myrmex_swarm.desc_1").withStyle(ChatFormatting.GRAY));
    }
}