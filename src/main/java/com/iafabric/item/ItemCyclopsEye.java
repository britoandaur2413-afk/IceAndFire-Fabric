package com.iafabric.item;



import com.iafabric.util.ItemNbt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;
import java.util.function.Consumer;
import com.iafabric.IafMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemCyclopsEye extends Item {

    public ItemCyclopsEye() {
        super(IafItemRegistry.props().durability(500));
    }


    @Override
    public void inventoryTick(ItemStack stack, Level tickLevel, Entity entity, int slotId, boolean isSelected) {
        if (!(tickLevel instanceof ServerLevel world)) {
            return;
        }
        EquipmentSlot slot = com.iafabric.compat.ItemSlots.slotOf(entity, stack, isSelected);
        if (!ItemNbt.hasTag(stack)) {
            ItemNbt.setTag(stack, new CompoundTag());
        } else {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                if (living.getMainHandItem() == stack || living.getOffhandItem() == stack) {
                    double range = 15;
                    boolean inflictedDamage = false;
                    for (Mob LivingEntity : world.getEntitiesOfClass(Mob.class, new AABB(living.getX() - range, living.getY() - range, living.getZ() - range, living.getX() + range, living.getY() + range, living.getZ() + range))) {
                        if (!LivingEntity.is(living) && !LivingEntity.isAlliedTo(living) && (LivingEntity.getTarget() == living || LivingEntity.getLastHurtByMob() == living || LivingEntity instanceof Enemy)) {
                            LivingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 1));
                            inflictedDamage = true;
                        }
                    }
                    if (inflictedDamage) {
                        ItemNbt.update(stack, nbtTag -> nbtTag.putInt("HurtingTicks", ItemNbt.getOrEmpty(stack).iceandfire_fabric$getIntOr("HurtingTicks", 0) + 1));
                    }
                }
                if (ItemNbt.getOrEmpty(stack).iceandfire_fabric$getIntOr("HurtingTicks", 0) > 120) {
                    stack.hurtAndBreak(1, (LivingEntity) entity, EquipmentSlot.MAINHAND);
                    ItemNbt.update(stack, nbtTag -> nbtTag.putInt("HurtingTicks", 0));
                }
            }

        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.iceandfire_fabric.legendary_weapon.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.cyclops_eye.desc_0").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.iceandfire_fabric.cyclops_eye.desc_1").withStyle(ChatFormatting.GRAY));
    }
}
