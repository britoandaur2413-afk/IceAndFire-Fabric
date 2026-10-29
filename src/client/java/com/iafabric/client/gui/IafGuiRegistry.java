package com.iafabric.client.gui;

import com.iafabric.inventory.IafContainerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class IafGuiRegistry {

    public static void register() {
        MenuScreens.register(IafContainerRegistry.IAF_LECTERN_CONTAINER, GuiLectern::new);
        MenuScreens.register(IafContainerRegistry.PODIUM_CONTAINER, GuiPodium::new);
        MenuScreens.register(IafContainerRegistry.DRAGON_CONTAINER, GuiDragon::new);
        MenuScreens.register(IafContainerRegistry.HIPPOGRYPH_CONTAINER, GuiHippogryph::new);
        MenuScreens.register(IafContainerRegistry.HIPPOCAMPUS_CONTAINER, GuiHippocampus::new);
        MenuScreens.register(IafContainerRegistry.DRAGON_FORGE_CONTAINER, GuiDragonForge::new);
        MenuScreens.register(IafContainerRegistry.PIXIE_MENU, GuiPixieMenu::new);
    }
}
