package com.iafabric.item;

import java.util.List;
import com.iafabric.entity.EntityTideTrident;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** The tide trident: a trident that throws an EntityTideTrident (harder hits, pierces mobs). */
public class ItemTideTrident extends TridentItem {

    public ItemTideTrident() {
        super(IafItemRegistry.props().rarity(Rarity.EPIC).durability(400).attributes(TridentItem.createAttributes())
            .component(DataComponents.TOOL, TridentItem.createToolProperties()));
    }

    @Override
    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int timeHeld = this.getUseDuration(itemStack, entity) - remainingTime;
        if (timeHeld < 10) {
            return;
        }
        float riptideStrength = EnchantmentHelper.getTridentSpinAttackStrength(itemStack, player);
        if (riptideStrength > 0.0F && !player.isInWaterOrRain()) {
            return;
        }
        if (itemStack.getDamageValue() >= itemStack.getMaxDamage() - 1) {
            return;
        }
        Holder<SoundEvent> sound = EnchantmentHelper.pickHighestLevel(itemStack, EnchantmentEffectComponents.TRIDENT_SOUND).orElse(SoundEvents.TRIDENT_THROW);
        player.awardStat(Stats.ITEM_USED.get(this));
        if (level instanceof ServerLevel serverLevel) {
            itemStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
            if (riptideStrength == 0.0F) {
                ItemStack thrown = player.hasInfiniteMaterials() ? itemStack.copyWithCount(1) : itemStack.split(1);
                EntityTideTrident trident = new EntityTideTrident(serverLevel, player, thrown);
                trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
                if (player.hasInfiniteMaterials()) {
                    trident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
                serverLevel.addFreshEntity(trident);
                level.playSound(null, trident, sound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                return;
            }
        }
        if (riptideStrength > 0.0F) {
            float yRot = player.getYRot();
            float xRot = player.getXRot();
            float xd = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
            float yd = -Mth.sin(xRot * Mth.DEG_TO_RAD);
            float zd = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
            float dist = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            player.push(xd * riptideStrength / dist, yd * riptideStrength / dist, zd * riptideStrength / dist);
            player.startAutoSpinAttack(20, 12.0F, itemStack);
            if (player.onGround()) {
                player.move(MoverType.SELF, new Vec3(0.0D, 1.1999999F, 0.0D));
            }
            level.playSound(null, player, sound.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            return;
        }
        return;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.tide_trident.desc_0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.tide_trident.desc_1").withStyle(ChatFormatting.GRAY));
    }
}
