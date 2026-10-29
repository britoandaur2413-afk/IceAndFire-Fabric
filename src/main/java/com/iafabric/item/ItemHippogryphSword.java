package com.iafabric.item;



import net.minecraft.server.level.ServerLevel;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemHippogryphSword extends IafToolItem {

    public ItemHippogryphSword() {
        super(IafItemRegistry.HIPPOGRYPH_SWORD_TOOL_MATERIAL, 3, -2.4F, IafItemRegistry.props());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity targetEntity, LivingEntity attacker) {
        float f = (float) attacker.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
        float f3 = 1.0F + net.minecraft.world.item.enchantment.EnchantmentHelper.getSweepingDamageRatio(attacker) * f;
        if (attacker instanceof Player) {
            Player player = (Player) attacker;
            for (LivingEntity LivingEntity : attacker.level().getEntitiesOfClass(LivingEntity.class, targetEntity.getBoundingBox().inflate(1.0D, 0.25D, 1.0D))) {
                if (LivingEntity != player && LivingEntity != targetEntity && !attacker.isAlliedTo(LivingEntity) && attacker.distanceToSqr(LivingEntity) < 9.0D) {
                    LivingEntity.knockback(0.4F, Mth.sin(attacker.getYRot() * 0.017453292F), -Mth.cos(attacker.getYRot() * 0.017453292F));
                    LivingEntity.hurt(LivingEntity.damageSources().playerAttack(player), f3);
                }
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, player.getSoundSource(), 1.0F, 1.0F);
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, player.getX() - Mth.sin(player.getYRot() * Mth.DEG_TO_RAD), player.getY(0.5D), player.getZ() + Mth.cos(player.getYRot() * Mth.DEG_TO_RAD), 0, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        return super.hurtEnemy(stack, targetEntity, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.hippogryph_sword.desc_0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.hippogryph_sword.desc_1").withStyle(ChatFormatting.GRAY));
    }
}
