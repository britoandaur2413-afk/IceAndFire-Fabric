package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;


import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityGorgon;
import com.iafabric.entity.props.MiscProperties;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.IBlacklistedFromStatues;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;
import net.minecraft.util.RandomSource;
import java.util.function.Predicate;

public class ItemCockatriceScepter extends Item {

    private final RandomSource rand = RandomSource.create();
    private int specialWeaponDmg;

    public ItemCockatriceScepter() {
        super(IafItemRegistry.props().durability(700));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.cockatrice_scepter.desc_0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.cockatrice_scepter.desc_1").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity livingEntity, int timeLeft) {
        if (specialWeaponDmg > 0) {
            stack.hurtAndBreak(specialWeaponDmg, livingEntity, EquipmentSlot.MAINHAND);
            specialWeaponDmg = 0;
        }
        MiscProperties.getTargeting(livingEntity).forEach(target -> {
            MiscProperties.removeTargetedBy(livingEntity, target);
        });
        MiscProperties.removeTargets(livingEntity);
        return;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 1;
    }

    @Override
    public UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand hand) {
        ItemStack itemStackIn = playerIn.getItemInHand(hand);
        playerIn.startUsingItem(hand);
        return InteractionResultHolder.pass(playerIn.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
        if (player instanceof Player) {
            double dist = 32;
            Vec3 playerEyePosition = player.getEyePosition(1.0F);
            Vec3 playerLook = player.getViewVector(1.0F);
            Vec3 Vector3d2 = playerEyePosition.add(playerLook.x * dist, playerLook.y * dist, playerLook.z * dist);
            Entity pointedEntity = null;
            List<Entity> nearbyEntities = player.level().getEntities(player, player.getBoundingBox().expandTowards(playerLook.x * dist, playerLook.y * dist, playerLook.z * dist).inflate(1.0D, 1.0D, 1.0D), entity -> {
                    boolean blindness = entity instanceof LivingEntity && ((LivingEntity) entity).hasEffect(MobEffects.BLINDNESS) || (entity instanceof IBlacklistedFromStatues && !((IBlacklistedFromStatues) entity).canBeTurnedToStone());
                    return entity != null && entity.isPickable() && !blindness && (entity instanceof Player || (entity instanceof LivingEntity && DragonUtils.isAlive((LivingEntity) entity)));
                });
            double d2 = dist;
            for (Entity nearbyEntity : nearbyEntities) {
                AABB axisalignedbb = nearbyEntity.getBoundingBox().inflate(nearbyEntity.getPickRadius());
                Optional<Vec3> optional = axisalignedbb.clip(playerEyePosition, Vector3d2);

                if (axisalignedbb.contains(playerEyePosition)) {
                    if (d2 >= 0.0D) {
                        pointedEntity = nearbyEntity;
                        d2 = 0.0D;
                    }
                } else if (optional.isPresent()) {
                    double d3 = playerEyePosition.distanceTo(optional.get());

                    if (d3 < d2 || d2 == 0.0D) {
                        if (nearbyEntity.getRootVehicle() == player.getRootVehicle() && true) {
                            if (d2 == 0.0D) {
                                pointedEntity = nearbyEntity;
                            }
                        } else {
                            pointedEntity = nearbyEntity;
                            d2 = d3;
                        }
                    }
                }

            }
            if (pointedEntity instanceof LivingEntity) {
                if (!pointedEntity.isAlive())
                    return;
                MiscProperties.addScepterTargetData(player, (LivingEntity) pointedEntity);
            }
            attackTargets(player);
        }
    }

    private void attackTargets(LivingEntity caster) {
        List<LivingEntity> targets = MiscProperties.getTargeting(caster);
        for (LivingEntity target : targets) {
            if (!EntityGorgon.isEntityLookingAt(caster, target, 0.2F) || !caster.isAlive()) {
                MiscProperties.removeTargetedBy(caster, target);
                MiscProperties.removeTarget(caster, target);
            }
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 2));
            if (caster.tickCount % 20 == 0) {
                specialWeaponDmg++;
                target.hurt(target.damageSources().wither(), 2);
            }
            drawParticleBeam(caster, target);
            if (!target.isAlive()) {
                MiscProperties.removeTarget(caster, target);
            }

        }
    }

    private void drawParticleBeam(LivingEntity origin, LivingEntity target) {
        double d5 = 80F;
        double d0 = target.getX() - origin.getX();
        double d1 = target.getY() + (double) (target.getBbHeight() * 0.5F)
            - (origin.getY() + (double) origin.getEyeHeight() * 0.5D);
        double d2 = target.getZ() - origin.getZ();
        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        d0 = d0 / d3;
        d1 = d1 / d3;
        d2 = d2 / d3;
        double d4 = this.rand.nextDouble();
        while (d4 < d3) {
            d4 += 1.0D;
            origin.level().addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000), origin.getX() + d0 * d4, origin.getY() + d1 * d4 + (double) origin.getEyeHeight() * 0.5D, origin.getZ() + d2 * d4, 0.0D, 0.0D, 0.0D);
        }
    }

}
