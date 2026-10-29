package com.iafabric.item;



import com.iafabric.util.EntityNbt;
import com.iafabric.util.ItemNbt;
import java.util.function.Consumer;
import com.iafabric.entity.EntityStoneStatue;
import com.iafabric.entity.IafEntityRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemStoneStatue extends Item {

    public ItemStoneStatue() {
        super(IafItemRegistry.props().stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level context, List<Component> tooltip, TooltipFlag flagIn) {
        if (ItemNbt.hasTag(stack)) {
            boolean isPlayer = ItemNbt.getOrEmpty(stack).iceandfire_fabric$getBooleanOr("IAFStoneStatuePlayerEntity", false);
            String id = ItemNbt.getOrEmpty(stack).iceandfire_fabric$getStringOr("IAFStoneStatueEntityID", "");
            if (EntityNbt.type(id).orElse(null) != null) {
                EntityType type = EntityNbt.type(id).orElse(null);
                MutableComponent untranslated = isPlayer ? Component.translatable("entity.minecraft.player") : Component.translatable(type.getDescriptionId());
                tooltip.add(untranslated.withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public void onCraftedBy(ItemStack itemStack, Level craftLevel, Player player) {
        ItemNbt.setTag(itemStack, new CompoundTag());
        ItemNbt.update(itemStack, nbtTag -> nbtTag.putBoolean("IAFStoneStatuePlayerEntity", true));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        } else {
            ItemStack stack = context.getPlayer().getItemInHand(context.getHand());
            if (ItemNbt.hasTag(stack)) {
                String id = ItemNbt.getOrEmpty(stack).iceandfire_fabric$getStringOr("IAFStoneStatueEntityID", "");
                CompoundTag statueNBT = ItemNbt.getOrEmpty(stack).getCompound("IAFStoneStatueNBT");
                EntityStoneStatue statue = new EntityStoneStatue(IafEntityRegistry.STONE_STATUE,
                    context.getLevel());
                statue.readAdditionalSaveData(EntityNbt.input(statue, statueNBT));
                statue.setTrappedEntityTypeString(id);
                double d1 = context.getPlayer().getX() - (context.getClickedPos().getX() + 0.5);
                double d2 = context.getPlayer().getZ() - (context.getClickedPos().getZ() + 0.5);
                float yaw = (float) (Mth.atan2(d2, d1) * (180F / (float) Math.PI)) - 90;
                statue.yRotO = yaw;
                statue.setYRot(yaw);
                statue.yHeadRot = yaw;
                statue.yBodyRot = yaw;
                statue.yBodyRotO = yaw;
                statue.absMoveTo(context.getClickedPos().getX() + 0.5, context.getClickedPos().getY() + 1, context.getClickedPos().getZ() + 0.5, yaw, 0);
                if (!context.getLevel().isClientSide) {
                    context.getLevel().addFreshEntity(statue);
                    statue.readAdditionalSaveData(EntityNbt.input(statue, ItemNbt.getTag(stack)));
                }
                statue.setCrackAmount(0);

                if (!context.getPlayer().isCreative()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.SUCCESS;
    }
}
