package com.iafabric.item;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import com.iafabric.entity.EntityTideTrident;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** The tide trident: a trident that throws an EntityTideTrident (harder hits, pierces mobs). */
public class ItemTideTrident extends TridentItem {

    public ItemTideTrident() {
        super(IafItemRegistry.props().rarity(Rarity.EPIC).durability(400));
    }

    /** As the game's trident throws (1.20.1's TridentItem.releaseUsing), an EntityTideTrident. */
    @Override
    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int timeHeld = this.getUseDuration(itemStack) - remainingTime;
        if (timeHeld < 10) {
            return;
        }
        int riptide = EnchantmentHelper.getRiptide(itemStack);
        if (riptide > 0 && !player.isInWaterOrRain()) {
            return;
        }
        if (!level.isClientSide) {
            itemStack.hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(entity.getUsedItemHand()));
            if (riptide == 0) {
                EntityTideTrident trident = new EntityTideTrident(level, player, itemStack);
                trident.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
                if (player.getAbilities().instabuild) {
                    trident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }
                level.addFreshEntity(trident);
                level.playSound(null, trident, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    player.getInventory().removeItem(itemStack);
                }
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        if (riptide > 0) {
            float yRot = player.getYRot();
            float xRot = player.getXRot();
            float xd = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
            float yd = -Mth.sin(xRot * Mth.DEG_TO_RAD);
            float zd = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
            float dist = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            float strength = 3.0F * ((1.0F + riptide) / 4.0F);
            player.push(xd * strength / dist, yd * strength / dist, zd * strength / dist);
            player.startAutoSpinAttack(20);
            if (player.onGround()) {
                player.move(MoverType.SELF, new Vec3(0.0D, 1.1999999F, 0.0D));
            }
            SoundEvent sound = riptide >= 3 ? SoundEvents.TRIDENT_RIPTIDE_3 : riptide == 2 ? SoundEvents.TRIDENT_RIPTIDE_2 : SoundEvents.TRIDENT_RIPTIDE_1;
            level.playSound(null, player, sound, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.tide_trident.desc_0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.tide_trident.desc_1").withStyle(ChatFormatting.GRAY));
    }
}
