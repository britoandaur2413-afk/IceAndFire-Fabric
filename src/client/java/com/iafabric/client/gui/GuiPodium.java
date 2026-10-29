package com.iafabric.client.gui;

import com.iafabric.inventory.ContainerPodium;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GuiPodium extends AbstractContainerScreen<ContainerPodium> {

    public static final ResourceLocation PODUIM_TEXTURE = new ResourceLocation("iceandfire_fabric:textures/gui/podium.png");

    public GuiPodium(ContainerPodium container, Inventory inv, Component name) {
        super(container, inv, name);
        this.imageWidth = 176;
        this.imageHeight = 133;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int x, int y) {
        IafGui.fitCentered(g, font, I18n.get("block.iceandfire_fabric.podium"), this.imageWidth / 2, 6, this.imageWidth - 16, 4210752);
        IafGui.text(g, font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 96 + 2, 4210752);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        IafGui.blit(g, PODUIM_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
