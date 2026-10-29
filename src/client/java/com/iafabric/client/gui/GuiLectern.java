package com.iafabric.client.gui;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityLectern;
import com.iafabric.enums.EnumBestiaryPages;
import com.iafabric.inventory.ContainerLectern;
import com.iafabric.item.IafItemRegistry;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GuiLectern extends AbstractContainerScreen<ContainerLectern> {
    private static final ResourceLocation ENCHANTMENT_TABLE_GUI_TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/gui/lectern.png");
    private static final ResourceLocation ENCHANTMENT_TABLE_BOOK_TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/models/lectern_book.png");
    private BookModel bookModel;
    private final RandomSource random = RandomSource.create();
    private final Component nameable;
    public int ticks;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    private ItemStack last = ItemStack.EMPTY;
    private int flapTimer = 0;

    public GuiLectern(ContainerLectern container, Inventory inv, Component name) {
        super(container, inv, name);
        this.nameable = name;
    }

    @Override
    protected void init() {
        super.init();
        bookModel = new BookModel(this.minecraft.getEntityModels().bakeLayer(ModelLayers.BOOK));
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        IafGui.fit(g, font, this.nameable.getString(), 12, 4, this.imageWidth - 24, 4210752);
        IafGui.text(g, font, this.playerInventoryTitle.getString(), 8, this.imageHeight - 96 + 2, 4210752);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.menu.onUpdate();
        this.tickBook();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (int k = 0; k < 3; ++k) {
            double l = mouseX - (this.leftPos + 60);
            double i1 = mouseY - (this.topPos + 14 + 19 * k);
            if (l >= 0 && i1 >= 0 && l < 108 && i1 < 19 && this.menu.clickMenuButton(this.minecraft.player, k)) {
                flapTimer = 5;
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, k);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
        int i = this.leftPos;
        int j = this.topPos;
        IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
        float open = Mth.lerp(partialTicks, this.oOpen, this.open);
        float flip = Mth.lerp(partialTicks, this.oFlip, this.flip);
        this.renderBook(g, i, j, open, flip);

        for (int i1 = 0; i1 < 3; ++i1) {
            int j1 = i + 60;
            int k1 = j1 + 20;
            EnumBestiaryPages page = this.menu.getPossiblePages()[i1];
            if (page == null) {
                IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, j1, j + 14 + 19 * i1, 0, 185, 108, 19);
                continue;
            }
            if (!(IafMod.PROXY.getRefrencedTE() instanceof TileEntityLectern)) {
                continue;
            }
            if (menu.getSlot(0).getItem().getItem() == IafItemRegistry.BESTIARY) {
                String name = I18n.get("bestiary." + page.toString().toLowerCase());
                float textScale = font.width(name) > 80 ? 1.0F - (font.width(name) - 80) * 0.01F : 1.0F;
                int k2 = mouseX - (i + 60);
                int l2 = mouseY - (j + 14 + 19 * i1);
                int j2 = 6839882;
                int j3 = 0X9F988C;
                if (k2 >= 0 && l2 >= 0 && k2 < 108 && l2 < 19) {
                    IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, j1, j + 14 + 19 * i1, 0, 204, 108, 19);
                    j2 = 16777088;
                    j3 = 16777088;
                } else {
                    IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, j1, j + 14 + 19 * i1, 0, 166, 108, 19);
                }
                IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, j1 + 1, j + 15 + 19 * i1, 16 * i1, 223, 16, 16);
                g.pose().pushPose();
                g.pose().translate(width / 2F - 10, height / 2F - 83 + (1.0F - textScale) * 55, 0.0F);
                g.pose().scale(textScale, textScale, 1.0F);
                IafGui.text(g, font, name, 0, 20 + 19 * i1, j2);
                g.pose().popPose();
                String cost = "3";
                g.drawString(font, cost, k1 + 84 - font.width(cost), j + 13 + 19 * i1 + 7, 0xFF000000 | j3, true);
            } else {
                IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, j1, j + 14 + 19 * i1, 0, 185, 108, 19);
                IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, j1 + 1, j + 15 + 19 * i1, 16 * i1, 239, 16, 16);
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        boolean creative = this.minecraft.player.isCreative();
        int manuscripts = this.menu.getManuscriptAmount();
        for (int j = 0; j < 3; ++j) {
            EnumBestiaryPages page = this.menu.getPossiblePages()[j];
            if (this.isHovering(60, 14 + 19 * j, 108, 17, mouseX, mouseY)) {
                List<FormattedCharSequence> list = Lists.newArrayList();
                if (page == null) {
                    list.add(Component.literal(ChatFormatting.RED + I18n.get("container.lectern.no_bestiary")).getVisualOrderText());
                } else if (!creative) {
                    list.add(Component.literal("" + ChatFormatting.WHITE + ChatFormatting.ITALIC + I18n.get("bestiary." + page.name().toLowerCase())).getVisualOrderText());
                    ChatFormatting formatting = manuscripts >= 3 ? ChatFormatting.GRAY : ChatFormatting.RED;
                    list.add(Component.literal(formatting + I18n.get("container.lectern.costs")).getVisualOrderText());
                    list.add(Component.literal(formatting + I18n.get("container.lectern.manuscript.many", 3)).getVisualOrderText());
                }
                g.renderTooltip(this.font, list, mouseX, mouseY);
                break;
            }
        }
    }

    public void tickBook() {
        ItemStack itemstack = this.menu.getSlot(0).getItem();
        if (!ItemStack.matches(itemstack, this.last)) {
            this.last = itemstack;
            do {
                this.flipT += this.random.nextInt(4) - this.random.nextInt(4);
            } while (this.flip <= this.flipT + 1.0F && this.flip >= this.flipT - 1.0F);
        }
        ++this.ticks;
        this.oFlip = this.flip;
        this.oOpen = this.open;
        boolean any = false;
        for (int i = 0; i < 3; ++i) {
            if (this.menu.getPossiblePages()[i] != null) {
                any = true;
            }
        }
        this.open = Mth.clamp(this.open + (any ? 0.2F : -0.2F), 0.0F, 1.0F);
        float f1 = (this.flipT - this.flip) * 0.4F;
        if (flapTimer > 0) {
            f1 = ticks * 0.5F;
            flapTimer--;
        }
        f1 = Mth.clamp(f1, -0.2F, 0.2F);
        this.flipA += (f1 - this.flipA) * 0.9F;
        this.flip += this.flipA;
    }

    /** The open book in its box, as the enchanting table's screen draws it. */
    private void renderBook(GuiGraphics g, int i, int j, float open, float flip) {
        com.mojang.blaze3d.platform.Lighting.setupForEntityInInventory();
        g.pose().pushPose();
        g.pose().translate(i + 33.0F, j + 31.0F, 100.0F);
        g.pose().scale(-40.0F, 40.0F, 40.0F);
        g.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(25.0F));
        g.pose().translate((1.0F - open) * 0.2F, (1.0F - open) * 0.1F, (1.0F - open) * 0.25F);
        g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(-(1.0F - open) * 90.0F - 90.0F));
        g.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
        float right = Mth.clamp(Mth.frac(flip + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float left = Mth.clamp(Mth.frac(flip + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        this.bookModel.setupAnim(0.0F, right, left, open);
        this.bookModel.renderToBuffer(g.pose(), g.bufferSource().getBuffer(this.bookModel.renderType(ENCHANTMENT_TABLE_BOOK_TEXTURE)), 0xF000F0,
            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        g.flush();
        g.pose().popPose();
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
    }
}
