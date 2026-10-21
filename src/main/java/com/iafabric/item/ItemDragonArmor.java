package com.iafabric.item;



import com.iafabric.util.RegistryUtil;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.iafabric.IafMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import org.jspecify.annotations.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemDragonArmor extends Item {

    public DragonArmorType type;
    public int dragonSlot;
    public String name;
    private Pattern baseName = Pattern.compile("[a-z]+_[a-z]+");

    public ItemDragonArmor(DragonArmorType type, int dragonSlot) {
        super(IafItemRegistry.props().stacksTo(1));
        this.type = type;
        this.dragonSlot = dragonSlot;
        if (type == DragonArmorType.FIRE || type == DragonArmorType.ICE || type == DragonArmorType.LIGHTNING)
            baseName = Pattern.compile("[a-z]+_[a-z]+_[a-z]+");
    }

        /** The shared translation key (getDescriptionId is final now). */
    public String iafDescriptionId() {
        String fullName = RegistryUtil.getName(this).getPath();
        Matcher matcher = baseName.matcher(fullName);
        name = matcher.find() ? matcher.group() : fullName;
        return "item.iceandfire_fabric." + name;
    }

    static String getNameForSlot(int slot){
        return switch (slot) {
            case 0 -> "head";
            case 1 -> "neck";
            case 2 -> "body";
            case 3 -> "tail";
            default -> "";
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flagIn) {
        String words = switch (dragonSlot) {
            case 1 -> "dragon.armor_neck";
            case 2 -> "dragon.armor_body";
            case 3 -> "dragon.armor_tail";
            default -> "dragon.armor_head";
        };
        tooltip.accept(Component.translatable(words).withStyle(ChatFormatting.GRAY));
    }

    public enum DragonArmorType {
        IRON,
        GOLD,
        DIAMOND,
        SILVER,
        FIRE,
        ICE,
        COPPER,
        LIGHTNING,
        NETHERITE
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.iafDescriptionId());
    }
}
