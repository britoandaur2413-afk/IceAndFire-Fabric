package com.iafabric.client.gui;

import com.iafabric.IafMod;
import com.iafabric.entity.EntityHippocampus;
import com.iafabric.inventory.HippocampusContainerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

public class GuiHippocampus extends AbstractContainerScreen<HippocampusContainerMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/gui/hippogryph.png");

    public GuiHippocampus(HippocampusContainerMenu menu, Inventory playerInv, Component name) {
        super(menu, playerInv, name);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        if (IafMod.PROXY.getReferencedMob() instanceof EntityHippocampus mount) {
            IafGui.fit(g, font, mount.getDisplayName().getString(), 8, 6, this.imageWidth - 16, 4210752);
        }
        IafGui.text(g, font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 96 + 2, 4210752);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        IafGui.blit(g, TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
        Entity entity = IafMod.PROXY.getReferencedMob();
        if (entity instanceof EntityHippocampus mount) {
            if (mount.isChested()) {
                IafGui.blit(g, TEXTURE, i + 79, j + 17, 0, this.imageHeight, 5 * 18, 54);
            }
            IafGui.entity(g, mount, i + 51, j + 60, 17, mouseX, mouseY);
        }
    }
}
