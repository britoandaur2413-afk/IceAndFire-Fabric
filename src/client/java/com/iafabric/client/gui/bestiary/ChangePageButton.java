package com.iafabric.client.gui.bestiary;

import com.iafabric.client.gui.IafGui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ChangePageButton extends Button {
    private static final ResourceLocation WIDGETS = ResourceLocation.parse("iceandfire_fabric:textures/gui/bestiary/widgets.png");
    private final boolean right;
    public int lastpage = 1;
    private final int color;

    public ChangePageButton(int x, int y, boolean right, int color, OnPress press) {
        super(x, y, 23, 10, Component.literal(""), press, DEFAULT_NARRATION);
        this.right = right;
        this.color = color;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (this.active) {
            int i = this.isHovered ? 23 : 0;
            int j = 64 + (this.right ? 0 : 13) + color * 23;
            IafGui.blit(g, WIDGETS, this.getX(), this.getY(), i, j, width, height);
        }
    }
}
