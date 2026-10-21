package com.iafabric.item;




import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.network.chat.ComponentSerialization;
import com.iafabric.util.EntityNbt;
import com.iafabric.util.ItemNbt;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.component.SwingAnimation;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.iafabric.IafMod;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import java.util.List;

public class ItemDragonHorn extends Item {

    public ItemDragonHorn() {
        super(IafItemRegistry.props().stacksTo(1));
    }

    public static int getDragonType(ItemStack stack) {
        if (ItemNbt.hasTag(stack)) {
            String id = ItemNbt.getOrEmpty(stack).getStringOr("DragonHornEntityID", "");
            if (EntityNbt.type(id).isPresent()) {
                EntityType entityType = EntityNbt.type(id).get();
                if (entityType == IafEntityRegistry.FIRE_DRAGON)
                    return 1;

                if (entityType == IafEntityRegistry.ICE_DRAGON)
                    return 2;

                if (entityType == IafEntityRegistry.LIGHTNING_DRAGON)
                    return 3;
            }
        }

        return 0;
    }


    @Override
    public void onCraftedBy(ItemStack itemStack, Player player) {
        ItemNbt.setTag(itemStack, new CompoundTag());
    }


    @Override
    public InteractionResult interactLivingEntity(@NotNull ItemStack stack, Player playerIn, @NotNull LivingEntity target, @NotNull InteractionHand hand) {
        ItemStack trueStack = playerIn.getItemInHand(hand);
        if (!playerIn.level().isClientSide() && hand == InteractionHand.MAIN_HAND && target instanceof EntityDragonBase && ((EntityDragonBase) target).isOwnedBy(playerIn) && (!ItemNbt.hasTag(trueStack) || (ItemNbt.hasTag(trueStack) && ItemNbt.getOrEmpty(trueStack).getCompound("EntityTag").isEmpty()))) {
            CompoundTag newTag = new CompoundTag();

            CompoundTag entityTag = EntityNbt.save(target);
            newTag.put("EntityTag", entityTag);

            newTag.putString("DragonHornEntityID", BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString());
            ItemNbt.setTag(trueStack, newTag);

            playerIn.swing(hand, SwingAnimation.DEFAULT, true);
            playerIn.level().playSound(playerIn, playerIn.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 3.0F, 0.75F);
            target.remove(Entity.RemovalReason.DISCARDED);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }


    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP)
            return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        if (ItemNbt.hasTag(stack) && !ItemNbt.getOrEmpty(stack).getString("DragonHornEntityID").isEmpty()) {
            Level world = context.getLevel();
            String id = ItemNbt.getOrEmpty(stack).getStringOr("DragonHornEntityID", "");
            EntityType type = EntityNbt.type(id).orElse(null);
            if (type != null) {
                Entity entity = type.create(world, EntitySpawnReason.BUCKET);
                if (entity == null) return InteractionResult.FAIL;
                if (entity instanceof EntityDragonBase) {
                    EntityDragonBase dragon = (EntityDragonBase) entity;
                    EntityNbt.load(dragon, ItemNbt.getOrEmpty(stack).getCompoundOrEmpty("EntityTag"));
                }
                //Still needed to allow for intercompatibility
                if (ItemNbt.getOrEmpty(stack).contains("EntityUUID"))
                    entity.setUUID(ItemNbt.getOrEmpty(stack).read("EntityUUID", UUIDUtil.CODEC).orElse(null));

                entity.absSnapTo(context.getClickedPos().getX() + 0.5D, (context.getClickedPos().getY() + 1), context.getClickedPos().getZ() + 0.5D, 180 + (context.getHorizontalDirection()).toYRot(), 0.0F);
                if (world.addFreshEntity(entity)) {
                    CompoundTag tag = ItemNbt.getTag(stack);
                    tag.remove("DragonHornEntityID");
                    tag.remove("EntityTag");
                    tag.remove("EntityUUID");
                    ItemNbt.setTag(stack, tag);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        if (ItemNbt.hasTag(stack)) {
            CompoundTag entityTag = ItemNbt.getOrEmpty(stack).getCompoundOrEmpty("EntityTag");
            if (!entityTag.isEmpty()) {
                String id = ItemNbt.getOrEmpty(stack).getStringOr("DragonHornEntityID", "");
                if (EntityNbt.type(id).isPresent()) {
                    EntityType type = EntityNbt.type(id).get();
                    tooltip.accept((Component.translatable(type.getDescriptionId())).withStyle(getTextColorForEntityType(type)));
                    String name = (Component.translatable("dragon.unnamed")).getString();
                    Component customName = entityTag.read("CustomName", ComponentSerialization.CODEC).orElse(null);
                    if (customName != null)
                        name = customName.getString();

                    tooltip.accept((Component.literal(name)).withStyle(ChatFormatting.GRAY));
                    String gender = (Component.translatable("dragon.gender")).getString() + " " + (Component.translatable(entityTag.getBooleanOr("Gender", false) ? "dragon.gender.male" : "dragon.gender.female")).getString();
                    tooltip.accept((Component.literal(gender)).withStyle(ChatFormatting.GRAY));
                    int stagenumber = entityTag.getIntOr("AgeTicks", 0) / 24000;
                    int stage1 = 0;
                    if (stagenumber >= 100) {
                        stage1 = 5;
                    } else if (stagenumber >= 75) {
                        stage1 = 4;
                    } else if (stagenumber >= 50) {
                        stage1 = 3;
                    } else if (stagenumber >= 25) {
                        stage1 = 2;
                    } else {
                        stage1 = 1;
                    }
                    String stage = (Component.translatable("dragon.stage")).getString() + " " + stage1 + " " + (Component.translatable("dragon.days.front")).getString() + stagenumber + " " + (Component.translatable("dragon.days.back")).getString();
                    tooltip.accept((Component.literal(stage)).withStyle(ChatFormatting.GRAY));
                }
            }

        }
    }

    private ChatFormatting getTextColorForEntityType(EntityType type) {
        if (type == IafEntityRegistry.FIRE_DRAGON)
            return ChatFormatting.DARK_RED;

        if (type == IafEntityRegistry.ICE_DRAGON)
            return ChatFormatting.BLUE;

        if (type == IafEntityRegistry.LIGHTNING_DRAGON)
            return ChatFormatting.DARK_PURPLE;

        return ChatFormatting.GRAY;
    }
}
