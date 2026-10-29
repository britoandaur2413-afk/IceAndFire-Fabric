package com.iafabric.enums;

import com.iafabric.item.IafItemRegistry;
import com.iafabric.item.ItemMobSkull;
import net.minecraft.world.item.Item;

import java.util.Locale;

public enum EnumSkullType {
    HIPPOGRYPH,
    CYCLOPS,
    COCKATRICE,
    STYMPHALIAN,
    TROLL,
    AMPHITHERE,
    SEASERPENT,
    HYDRA;

    public String itemResourceName;
    public Item skull_item;

    EnumSkullType() {
        itemResourceName = this.name().toLowerCase(Locale.ROOT) + "_skull";
    }

    public static void initItems() {
        for (EnumSkullType skull : EnumSkullType.values()) {
            skull.skull_item = IafItemRegistry.register(skull.itemResourceName, () -> new ItemMobSkull(skull));
        }
    }
}
