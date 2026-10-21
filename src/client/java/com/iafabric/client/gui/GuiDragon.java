package com.iafabric.client.gui;

import com.iafabric.IafMod;
import com.iafabric.client.StatCollector;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.inventory.ContainerDragon;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

public class GuiDragon extends AbstractContainerScreen<ContainerDragon> {
    private static final Identifier texture = Identifier.parse("iceandfire_fabric:textures/gui/dragon.png");

    public GuiDragon(ContainerDragon dragonInv, Inventory playerInv, Component name) {
        super(dragonInv, playerInv, name, 176, 214);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        int k = this.leftPos;
        int l = this.topPos;
        IafGui.blit(g, texture, k, l, 0, 0, this.imageWidth, this.imageHeight);
        Entity entity = IafMod.PROXY.getReferencedMob();
        if (entity instanceof EntityDragonBase dragon) {
            float dragonScale = 1F / Math.max(0.0001F, dragon.getScale());
            IafGui.entityInBox(g, dragon, k + 24, l + 15, k + 152, l + 70, Math.max(4, (int) (dragonScale * 23F)), mouseX, mouseY);
            int c = k + this.imageWidth / 2;
            String s3 = dragon.getCustomName() == null ? StatCollector.translateToLocal("dragon.unnamed") : StatCollector.translateToLocal("dragon.name") + " " + dragon.getCustomName().getString();
            IafGui.fitCentered(g, font, s3, c, l + 75, this.imageWidth - 12, 0XFFFFFF);
            String s2 = StatCollector.translateToLocal("dragon.health") + " " + Math.floor(Math.min(dragon.getHealth(), dragon.getMaxHealth())) + " / " + dragon.getMaxHealth();
            IafGui.fitCentered(g, font, s2, c, l + 84, this.imageWidth - 12, 0XFFFFFF);
            String s5 = StatCollector.translateToLocal("dragon.gender") + " " + StatCollector.translateToLocal((dragon.isMale() ? "dragon.gender.male" : "dragon.gender.female"));
            IafGui.fitCentered(g, font, s5, c, l + 93, this.imageWidth - 12, 0XFFFFFF);
            String s6 = StatCollector.translateToLocal("dragon.hunger") + " " + dragon.getHunger() + "/100";
            IafGui.fitCentered(g, font, s6, c, l + 102, this.imageWidth - 12, 0XFFFFFF);
            String s4 = StatCollector.translateToLocal("dragon.stage") + " " + dragon.getDragonStage() + " " + StatCollector.translateToLocal("dragon.days.front") + dragon.getAgeInDays() + " " + StatCollector.translateToLocal("dragon.days.back");
            IafGui.fitCentered(g, font, s4, c, l + 111, this.imageWidth - 12, 0XFFFFFF);
            String s7 = dragon.getOwner() != null ? StatCollector.translateToLocal("dragon.owner") + " " + dragon.getOwner().getName().getString() : StatCollector.translateToLocal("dragon.untamed");
            IafGui.fitCentered(g, font, s7, c, l + 120, this.imageWidth - 12, 0XFFFFFF);
        }
    }
}
