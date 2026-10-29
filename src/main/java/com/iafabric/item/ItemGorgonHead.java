package com.iafabric.item;

import net.minecraft.world.InteractionResultHolder;

import com.iafabric.util.ItemNbt;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityStoneStatue;
import com.iafabric.entity.util.DragonUtils;
import com.iafabric.entity.util.IBlacklistedFromStatues;
import com.iafabric.misc.IafDamageRegistry;
import com.iafabric.misc.IafSoundRegistry;
import com.google.common.base.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import java.util.function.Consumer;

public class ItemGorgonHead extends Item {

    public ItemGorgonHead() {
        super(IafItemRegistry.props().durability(1));
    }


    @Override
    public void onCraftedBy(ItemStack itemStack, Level craftLevel, Player player) {
        ItemNbt.setTag(itemStack, new CompoundTag());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entity, int timeLeft) {
        double dist = 32;
        Vec3 Vector3d = entity.getEyePosition(1.0F);
        Vec3 Vector3d1 = entity.getViewVector(1.0F);
        Vec3 Vector3d2 = Vector3d.add(Vector3d1.x * dist, Vector3d1.y * dist, Vector3d1.z * dist);
        double d1 = dist;
        Entity pointedEntity = null;
        List<Entity> list = worldIn.getEntities(entity, entity.getBoundingBox().expandTowards(Vector3d1.x * dist, Vector3d1.y * dist, Vector3d1.z * dist).inflate(1.0D, 1.0D, 1.0D), target -> {
                boolean blindness = target instanceof LivingEntity && ((LivingEntity) target).hasEffect(MobEffects.BLINDNESS) || (target instanceof IBlacklistedFromStatues && !((IBlacklistedFromStatues) target).canBeTurnedToStone());
                return target != null && target.isPickable() && !blindness && (target instanceof Player || (target instanceof LivingEntity && DragonUtils.isAlive((LivingEntity) target)));
            });
        double d2 = d1;
        for (int j = 0; j < list.size(); ++j) {
            Entity entity1 = list.get(j);
            AABB axisalignedbb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
            Optional<Vec3> optional = axisalignedbb.clip(Vector3d, Vector3d2);

            if (axisalignedbb.contains(Vector3d)) {
                if (d2 >= 0.0D) {
                    //pointedEntity = entity1;
                    d2 = 0.0D;
                }
            } else if (optional.isPresent()) {
                double d3 = Vector3d.distanceTo(optional.get());

                if (d3 < d2 || d2 == 0.0D) {
                    if (entity1.getRootVehicle() == entity.getRootVehicle() && true) {
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
        if (pointedEntity != null && com.iafabric.event.ServerEvents.isBlindMob(pointedEntity)) {
            // No eyes (a warden...): the gaze does nothing and the head is not used up.
            if (entity instanceof Player player && !worldIn.isClientSide) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.iceandfire_fabric.gorgon_head.no_eyes", pointedEntity.getDisplayName()), true);
            }
            pointedEntity = null;
        }
        if (pointedEntity != null) {
            if (pointedEntity instanceof LivingEntity) {
                pointedEntity.playSound(IafSoundRegistry.TURN_STONE, 1, 1);
                EntityStoneStatue statue = EntityStoneStatue.buildStatueEntity((LivingEntity) pointedEntity);
                if (pointedEntity instanceof Player) {
                    pointedEntity.hurt(IafDamageRegistry.causeGorgonDamage(pointedEntity), Integer.MAX_VALUE);
                } else {
                    if (!worldIn.isClientSide)
                        pointedEntity.remove(Entity.RemovalReason.KILLED);
                }
                statue.absMoveTo(pointedEntity.getX(), pointedEntity.getY(), pointedEntity.getZ(), pointedEntity.getYRot(), pointedEntity.getXRot());
                statue.yBodyRot = pointedEntity.getYRot();
                if (!worldIn.isClientSide) {
                    worldIn.addFreshEntity(statue);
                }
                if (entity instanceof Player && !((Player) entity).isCreative()) {
                    stack.shrink(1);
                }
            }
        }
        ItemNbt.update(stack, nbtTag -> nbtTag.putBoolean("Active", false));
        return;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand hand) {
        ItemStack itemStackIn = playerIn.getItemInHand(hand);
        playerIn.startUsingItem(hand);
        ItemNbt.update(itemStackIn, nbtTag -> nbtTag.putBoolean("Active", true));
        return InteractionResultHolder.success(playerIn.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
    }
}
