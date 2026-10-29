package com.iafabric.client.gui;

import com.iafabric.IafMod;
import com.iafabric.client.StatCollector;
import com.iafabric.entity.DragonSaddle;
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
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    /** Its window: the dragon drawn whole in it, turned a little (as if the mouse were there, left of and over its middle). */
    private static final int BOX_X1 = 24, BOX_Y1 = 15, BOX_X2 = 152, BOX_Y2 = 70;
    private static final float LOOK_X = -55F, LOOK_Y = -6F;
    /** How big and how high it is drawn there: worked out once, from its model as posed when the menu opens. */
    private int dragonScale = -1;
    private float dragonLift;
    private int framed = -1;

    public GuiDragon(ContainerDragon dragonInv, Inventory playerInv, Component name) {
        super(dragonInv, playerInv, name, 176, 214);
    }

    /**
     * The dragon whole in its window, at any age: its model as posed now, turned as the window turns it (InventoryScreen
     * turns a mob toward the mouse by twenty times the angle's arctangent over 40), as wide and as high as it reaches
     * (the window keeps its feet in the middle across, so the farther side counts twice), its middle in the window's.
     */
    private void frame(EntityDragonBase dragon) {
        int width = BOX_X2 - BOX_X1 - 6, height = BOX_Y2 - BOX_Y1 - 6;
        float turn = (float) Math.atan(-LOOK_X / 40.0) * 20F;
        float[] reach = com.iafabric.entity.DragonBones.inView(dragon, turn);
        if (reach == null) {
            // (its bones unknown: by its box, a dragon's wings and tail reaching well past it)
            this.dragonScale = Math.max(1, (int) Math.min(height / (dragon.getBbHeight() * 1.6F), width / (dragon.getBbWidth() * 3F)));
            this.dragonLift = dragon.getBbHeight() * 0.3F;
            return;
        }
        float half = Math.max(1.0E-3F, Math.max(-reach[0], reach[2]));
        float tall = Math.max(1.0E-3F, reach[3] - reach[1]);
        // (whole pixels a block: up to the nearer one, unless that would touch the window's edges)
        float fit = Math.min(width / 2F / half, height / tall);
        int scale = Math.round(fit);
        if (scale * half * 2F > BOX_X2 - BOX_X1 - 2 || scale * tall > BOX_Y2 - BOX_Y1 - 2) {
            scale = (int) fit;
        }
        this.dragonScale = Math.max(1, scale);
        this.dragonLift = (reach[1] + reach[3]) / 2F - dragon.getBbHeight() / 2F;
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
        if (this.menu.getSlot(DragonSaddle.SLOT).isActive()) {
            // the double saddle's place, under the body and tail armor (on a grown fire, ice or lightning dragon)
            g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, SLOT, k + 152, l + 53, 18, 18);
        }
        Entity entity = IafMod.PROXY.getReferencedMob();
        if (entity instanceof EntityDragonBase dragon) {
            if (this.dragonScale < 0 || this.framed != dragon.getId()) {
                this.frame(dragon);
                this.framed = dragon.getId();
            }
            float middleX = k + (BOX_X1 + BOX_X2) / 2F, middleY = l + (BOX_Y1 + BOX_Y2) / 2F;
            IafGui.entityInBox(g, dragon, k + BOX_X1, l + BOX_Y1, k + BOX_X2, l + BOX_Y2, this.dragonScale, this.dragonLift,
                middleX + LOOK_X, middleY + LOOK_Y);
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
