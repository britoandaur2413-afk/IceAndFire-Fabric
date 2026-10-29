package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;


import java.util.function.Consumer;
import com.iafabric.IafMod;
import com.iafabric.entity.props.MiscProperties;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.IBlacklistedFromStatues;
import com.iafabric.misc.IafSoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ItemSirenFlute extends Item {

    public ItemSirenFlute() {
        super(IafItemRegistry.props().durability(200));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player player, InteractionHand hand) {
        ItemStack itemStackIn = player.getItemInHand(hand);
        player.startUsingItem(hand);
        player.getCooldowns().addCooldown(this, 900);

        double dist = 32;
        Vec3 Vector3d = player.getEyePosition(1.0F);
        Vec3 Vector3d1 = player.getViewVector(1.0F);
        Vec3 Vector3d2 = Vector3d.add(Vector3d1.x * dist, Vector3d1.y * dist, Vector3d1.z * dist);

        double d1 = dist;
        Entity pointedEntity = null;
        List<Entity> list = player.level().getEntities(player, player.getBoundingBox().expandTowards(Vector3d1.x * dist, Vector3d1.y * dist, Vector3d1.z * dist).inflate(1.0D, 1.0D, 1.0D), entity -> {
                boolean blindness = entity instanceof LivingEntity && ((LivingEntity) entity).hasEffect(MobEffects.BLINDNESS) || (entity instanceof IBlacklistedFromStatues && !((IBlacklistedFromStatues) entity).canBeTurnedToStone());
                return entity != null && entity.isPickable() && !blindness && (entity instanceof Player || (entity instanceof LivingEntity && DragonUtils.isAlive((LivingEntity) entity)));
            });

        double d2 = d1;
        for (int j = 0; j < list.size(); ++j) {
            Entity entity1 = list.get(j);
            AABB axisalignedbb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
            Optional<Vec3> raytraceresult = axisalignedbb.clip(Vector3d, Vector3d2);

            if (axisalignedbb.contains(Vector3d)) {
                if (d2 >= 0.0D) {
                    pointedEntity = entity1;
                    d2 = 0.0D;
                }
            } else if (raytraceresult.isPresent()) {
                double d3 = Vector3d.distanceTo(raytraceresult.get());
                if (d3 < d2 || d2 == 0.0D) {
                    if (entity1.getRootVehicle() == player.getRootVehicle() && true) {
                        if (d2 == 0.0D) {
                            pointedEntity = entity1;
                        }
                    } else {
                        pointedEntity = entity1;
                        d2 = d3;
                    }
                }
            }
        }
        if (pointedEntity != null) {
            if (pointedEntity instanceof LivingEntity) {
                MiscProperties.setLoveTicks((LivingEntity) pointedEntity, 600);
                itemStackIn.hurtAndBreak(2, player, broken -> broken.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
        }
        player.playSound(IafSoundRegistry.SIREN_SONG, 1, 1);
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }


    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.siren_flute.desc_0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.siren_flute.desc_1").withStyle(ChatFormatting.GRAY));
    }
}
