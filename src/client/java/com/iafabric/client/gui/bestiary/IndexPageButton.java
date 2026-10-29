package com.iafabric.client.gui.bestiary;

import com.iafabric.client.gui.IafGui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class IndexPageButton extends Button {
    private static final ResourceLocation WIDGETS = new ResourceLocation("iceandfire_fabric:textures/gui/bestiary/widgets.png");

    public IndexPageButton(int x, int y, Component buttonText, Button.OnPress butn) {
        super(x, y, 160, 32, buttonText, butn, DEFAULT_NARRATION);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (this.active) {
            Font font = GuiBestiary.bookFont();
            boolean flag = this.isHoveredOrFocused();
            IafGui.blit(g, WIDGETS, this.getX(), this.getY(), 0, flag ? 32 : 0, this.width, this.height);
            int j = (flag ? 0XFAE67D : 0X303030) | Mth.ceil(this.alpha * 255.0F) << 24;
            String text = this.getMessage().getString();
            IafGui.fitCentered(g, font, text, this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, this.width - 16, j);
        }
    }
}
