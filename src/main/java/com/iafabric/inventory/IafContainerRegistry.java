package com.iafabric.inventory;

import com.iafabric.IafMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class IafContainerRegistry {

    public static final MenuType<ContainerLectern> IAF_LECTERN_CONTAINER = register("iaf_lectern", new MenuType<>(ContainerLectern::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<ContainerPodium> PODIUM_CONTAINER = register("podium", new MenuType<>(ContainerPodium::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<ContainerDragon> DRAGON_CONTAINER = register("dragon", new MenuType<>(ContainerDragon::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<ContainerHippogryph> HIPPOGRYPH_CONTAINER = register("hippogryph", new MenuType<>(ContainerHippogryph::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<HippocampusContainerMenu> HIPPOCAMPUS_CONTAINER = register("hippocampus", new MenuType<>(HippocampusContainerMenu::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<ContainerDragonForge> DRAGON_FORGE_CONTAINER = register("dragon_forge", new MenuType<>(ContainerDragonForge::new, FeatureFlags.VANILLA_SET));
    /** A bonded pixie's menu: the client is told which pixie (its color). */
    public static final MenuType<PixieMenu> PIXIE_MENU = register("pixie", new net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<>(PixieMenu::new,
        net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

    private IafContainerRegistry() {
    }

    private static <C extends AbstractContainerMenu> MenuType<C> register(String name, MenuType<C> type) {
        return Registry.register(BuiltInRegistries.MENU, IafMod.id(name), type);
    }

    public static void init() {
    }
}
