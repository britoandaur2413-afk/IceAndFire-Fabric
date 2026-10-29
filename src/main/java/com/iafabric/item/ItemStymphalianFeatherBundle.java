package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;


import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityStymphalianFeather;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
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

public class ItemStymphalianFeatherBundle extends Item {

    public ItemStymphalianFeatherBundle() {
        super(IafItemRegistry.props());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player player, InteractionHand hand) {
        ItemStack itemStackIn = player.getItemInHand(hand);
        player.startUsingItem(hand);
        player.getCooldowns().addCooldown(this, 15);
        player.playSound(SoundEvents.EGG_THROW, 1, 1);
        float rotation = player.yHeadRot;
        for (int i = 0; i < 8; i++) {
            EntityStymphalianFeather feather = new EntityStymphalianFeather(IafEntityRegistry.STYMPHALIAN_FEATHER,
                worldIn, player);
            rotation += 45;
            feather.shootFromRotation(player, 0, rotation, 0.0F, 1.5F, 1.0F);
            if (!worldIn.isClientSide) {
                worldIn.addFreshEntity(feather);
            }
        }
        if (!player.isCreative()) {
            itemStackIn.shrink(1);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {

        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.stymphalian_feather_bundle.desc_0").withStyle(ChatFormatting.GRAY));
    }
}