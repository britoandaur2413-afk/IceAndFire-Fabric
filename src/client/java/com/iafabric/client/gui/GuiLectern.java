package com.iafabric.client.gui;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityLectern;
import com.iafabric.enums.EnumBestiaryPages;
import com.iafabric.inventory.ContainerLectern;
import com.iafabric.item.IafItemRegistry;
import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GuiLectern extends AbstractContainerScreen<ContainerLectern> {
    private static final Identifier ENCHANTMENT_TABLE_GUI_TEXTURE = Identifier.parse("iceandfire_fabric:textures/gui/lectern.png");
    private static final Identifier ENCHANTMENT_TABLE_BOOK_TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/lectern_book.png");
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
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
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
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int k = 0; k < 3; ++k) {
            double l = event.x() - (this.leftPos + 60);
            double i1 = event.y() - (this.topPos + 14 + 19 * k);
            if (l >= 0 && i1 >= 0 && l < 108 && i1 < 19 && this.menu.clickMenuButton(this.minecraft.player, k)) {
                flapTimer = 5;
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, k);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        int i = this.leftPos;
        int j = this.topPos;
        IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
        float open = Mth.lerp(partialTicks, this.oOpen, this.open);
        float flip = Mth.lerp(partialTicks, this.oFlip, this.flip);
        g.book(bookModel, ENCHANTMENT_TABLE_BOOK_TEXTURE, 40.0F, open, flip, i + 14, j + 14, i + 52, j + 45);

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
                g.pose().pushMatrix();
                g.pose().translate(width / 2F - 10, height / 2F - 83 + (1.0F - textScale) * 55);
                g.pose().scale(textScale, textScale);
                IafGui.text(g, font, name, 0, 20 + 19 * i1, j2);
                g.pose().popMatrix();
                String cost = "3";
                g.text(font, cost, k1 + 84 - font.width(cost), j + 13 + 19 * i1 + 7, 0xFF000000 | j3, true);
            } else {
                IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, j1, j + 14 + 19 * i1, 0, 185, 108, 19);
                IafGui.blit(g, ENCHANTMENT_TABLE_GUI_TEXTURE, j1 + 1, j + 15 + 19 * i1, 16 * i1, 239, 16, 16);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(g, mouseX, mouseY, partialTicks);
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
                g.setTooltipForNextFrame(this.font, list, mouseX, mouseY);
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
}
