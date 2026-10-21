package com.iafabric.client.gui;

import com.iafabric.IafMod;
import com.iafabric.entity.DragonType;
import com.iafabric.entity.tile.TileEntityDragonforge;
import com.iafabric.inventory.ContainerDragonForge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class GuiDragonForge extends AbstractContainerScreen<ContainerDragonForge> {
    private static final Identifier TEXTURE_FIRE = Identifier.parse("iceandfire_fabric:textures/gui/dragonforge_fire.png");
    private static final Identifier TEXTURE_ICE = Identifier.parse("iceandfire_fabric:textures/gui/dragonforge_ice.png");
    private static final Identifier TEXTURE_LIGHTNING = Identifier.parse("iceandfire_fabric:textures/gui/dragonforge_lightning.png");
    /** Every dragon forge recipe takes this long (the client has no recipes to ask). */
    private static final int COOK_TIME = 1000;
    private final int dragonType;

    public GuiDragonForge(ContainerDragonForge container, Inventory inv, Component name) {
        super(container, inv, name);
        this.dragonType = container.fireType;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        IafGui.fitCentered(g, font, I18n.get("block.iceandfire_fabric.dragonforge_" + DragonType.getNameFromInt(dragonType) + "_core"), this.imageWidth / 2, 6, this.imageWidth - 16, 4210752);
        IafGui.text(g, font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 96 + 2, 4210752);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        Identifier texture = dragonType == 0 ? TEXTURE_FIRE : dragonType == 1 ? TEXTURE_ICE : TEXTURE_LIGHTNING;
        IafGui.blit(g, texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        IafGui.blit(g, texture, this.leftPos + 12, this.topPos + 23, 0, 166, this.getCookTime(126), 38);
    }

    private int getCookTime(int width) {
        int j = IafMod.PROXY.getRefrencedTE() instanceof TileEntityDragonforge forge ? Math.min(forge.cookTime, COOK_TIME) : 0;
        return j * width / COOK_TIME;
    }
}
