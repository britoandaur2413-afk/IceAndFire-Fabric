package com.iafabric.item;



import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemHydraHeart extends Item {

    public ItemHydraHeart() {
        super(IafItemRegistry.props().stacksTo(1));
    }


    @Override
    public void inventoryTick(ItemStack stack, Level tickLevel, Entity entity, int slotId, boolean isSelected) {
        if (!(tickLevel instanceof ServerLevel world)) {
            return;
        }
        EquipmentSlot slot = com.iafabric.compat.ItemSlots.slotOf(entity, stack, isSelected);
        if (entity instanceof Player && 0 >= 0 && 0 <= 8) {
            double healthPercentage = ((Player) entity).getHealth() / Math.max(1, ((Player) entity).getMaxHealth());
            if (healthPercentage < 1.0D) {
                int level = 0;
                if (healthPercentage < 0.25D) {
                    level = 3;
                } else if (healthPercentage < 0.5D) {
                    level = 2;
                } else if (healthPercentage < 0.75D) {
                    level = 1;
                }
                //Consider using EffectInstance.combine
                if (!((Player) entity).hasEffect(MobEffects.REGENERATION) || ((Player) entity).getEffect(MobEffects.REGENERATION).getAmplifier() < level)
                    ((Player) entity).addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, level, true, false));
            }
            //In hotbar
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.hydra_heart.desc_0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.hydra_heart.desc_1").withStyle(ChatFormatting.GRAY));
    }
}
