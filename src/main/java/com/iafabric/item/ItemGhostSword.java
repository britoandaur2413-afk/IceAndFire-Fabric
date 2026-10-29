package com.iafabric.item;



import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityGhostSword;
import com.iafabric.entity.IafEntityRegistry;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemGhostSword extends IafToolItem {

    public ItemGhostSword() {
        super(IafItemRegistry.GHOST_SWORD_TOOL_MATERIAL, 5, -1.0F, IafItemRegistry.props());
    }

    public static void spawnGhostSwordEntity(ItemStack stack, Player playerEntity) {
        if (playerEntity.getCooldowns().isOnCooldown(stack.getItem()))
            return;
        if (playerEntity.getItemInHand(InteractionHand.MAIN_HAND) != stack)
            return;
        double totalDmg = stack.getItem() instanceof IafToolItem tool ? tool.getAttackDamage() : 1.0D;
        playerEntity.playSound(SoundEvents.ZOMBIE_INFECT, 1, 1);
        EntityGhostSword shot = new EntityGhostSword(IafEntityRegistry.GHOST_SWORD, playerEntity.level(), playerEntity, totalDmg * 0.5F);
        shot.shootFromRotation(playerEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, 1, 0.5f);
        playerEntity.level().addFreshEntity(shot);
        stack.hurtAndBreak(1, playerEntity, EquipmentSlot.MAINHAND);
        playerEntity.getCooldowns().addCooldown(stack.getItem(), 10);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity targetEntity, LivingEntity attacker) {
        return super.hurtEnemy(stack, targetEntity, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.ghost_sword.desc_0").withStyle(ChatFormatting.GRAY));
    }
}