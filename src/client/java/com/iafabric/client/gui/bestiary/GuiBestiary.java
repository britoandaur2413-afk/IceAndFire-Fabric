package com.iafabric.client.gui.bestiary;

import com.iafabric.util.ItemNbt;
import com.iafabric.IafConfig;
import com.iafabric.IafMod;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.client.StatCollector;
import com.iafabric.enums.EnumBestiaryPages;
import com.iafabric.enums.EnumDragonArmor;
import com.iafabric.enums.EnumSeaSerpent;
import com.iafabric.enums.EnumTroll;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.misc.IafSoundRegistry;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import net.minecraft.client.gui.GuiGraphics;
import com.iafabric.client.gui.IafGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class GuiBestiary extends Screen {
    protected static final int X = 390;
    protected static final int Y = 245;
    /** How wide a line of text may be on one page. */
    private static final int PAGE_TEXT_WIDTH = 205;
    private static final ResourceLocation TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/gui/bestiary/bestiary.png");
    private static final ResourceLocation DRAWINGS_0 = ResourceLocation.parse("iceandfire_fabric:textures/gui/bestiary/drawings_0.png");
    private static final ResourceLocation DRAWINGS_1 = ResourceLocation.parse("iceandfire_fabric:textures/gui/bestiary/drawings_1.png");
    private static final ResourceLocation BOOK_WIDGET_TEXTURE = ResourceLocation.parse("citadel:textures/gui/book/widgets.png");
    private static final Map<String, ResourceLocation> PICTURE_LOCATION_CACHE = Maps.newHashMap();
    public List<EnumBestiaryPages> allPageTypes = new ArrayList<>();
    public EnumBestiaryPages pageType;
    public List<IndexPageButton> indexButtons = new ArrayList<>();
    public ChangePageButton previousPage;
    public ChangePageButton nextPage;
    public int bookPages;
    public int bookPagesTotal = 1;
    public int indexPages;
    public int indexPagesTotal = 1;
    protected ItemStack book;
    public boolean index;
    protected Font font = bookFont();

    public GuiBestiary(ItemStack book) {
        super(Component.translatable("bestiary_gui"));
        this.book = book;
        if (!book.isEmpty() && book.getItem() != null && book.getItem() == IafItemRegistry.BESTIARY) {
            if (ItemNbt.hasTag(book)) {
                Set<EnumBestiaryPages> pages = EnumBestiaryPages
                    .containedPages(Ints.asList(ItemNbt.getOrEmpty(book).getIntArray("Pages")));
                allPageTypes.addAll(pages);
                // Make sure the pages are sorted according to the enum
                allPageTypes.sort(Comparator.comparingInt(Enum::ordinal));
                indexPagesTotal = (int) Math.ceil(pages.size() / 10D);
            }
        }
        index = true;
    }

    public static Font bookFont() {
        if (IafConfig.useVanillaFont || !Minecraft.getInstance().options.languageCode.equalsIgnoreCase("en_us")) {
            return Minecraft.getInstance().font;
        } else {
            return (Font) IafMod.PROXY.getFontRenderer();
        }
    }

    private static Item getItemByRegistryName(String registryName) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(registryName));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        this.indexButtons.clear();
        int centerX = (width - X) / 2;
        int centerY = (height - Y) / 2;
        this.previousPage = new ChangePageButton(centerX + 15, centerY + 215, false, 0, (p_214132_1_) -> {
            if ((this.index ? this.indexPages > 0 : this.pageType != null)) {
                if (this.index) {
                    this.indexPages--;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(IafSoundRegistry.BESTIARY_PAGE, 1.0F));
                } else {
                    if (this.bookPages > 0) {
                        this.bookPages--;
                        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(IafSoundRegistry.BESTIARY_PAGE, 1.0F));
                    } else {
                        this.index = true;
                    }
                }
            }
        });
        this.addRenderableWidget(previousPage);
        this.nextPage = new ChangePageButton(centerX + 357, centerY + 215, true, 0, (p_214132_1_) -> {
            if ((this.index ? this.indexPages < this.indexPagesTotal - 1 : this.pageType != null && this.bookPages < this.pageType.pages)) {
                if (this.index) {
                    this.indexPages++;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(IafSoundRegistry.BESTIARY_PAGE, 1.0F));
                } else {
                    this.bookPages++;
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(IafSoundRegistry.BESTIARY_PAGE, 1.0F));
                }
            }
        });
        this.addRenderableWidget(this.nextPage);
        if (!allPageTypes.isEmpty()) {
            for (int i = 0; i < allPageTypes.size(); i++) {
                int xIndex = i % -2;
                int yIndex = i % 10;
                int id = 2 + i;
                IndexPageButton button = new IndexPageButton(centerX + 15 + (xIndex * 200),
                    centerY + 10 + (yIndex * 20) - (xIndex == 1 ? 20 : 0),
                    Component.translatable("bestiary."
                        + EnumBestiaryPages.values()[allPageTypes.get(i).ordinal()].toString().toLowerCase()),
                    (p_214132_1_) -> {
                        if (this.indexButtons.get(id - 2) != null && allPageTypes.get(id - 2) != null) {
                            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(IafSoundRegistry.BESTIARY_PAGE, 1.0F));
                            this.index = false;
                            this.bookPages = 0;
                            this.pageType = allPageTypes.get(id - 2);
                        }
                    });
                this.indexButtons.add(button);
                this.addRenderableWidget(button);
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(ms, mouseX, mouseY, partialTicks);
        IafGui.blit(ms, TEXTURE, (width - X) / 2, (height - Y) / 2, 0, 0, X, Y, 390, 390, 1.0F);
    }

    @Override
    public void render(GuiGraphics ms, int mouseX, int mouseY, float partialTicks) {
        for (int i = 0; i < this.indexButtons.size(); i++) {
            IndexPageButton button = this.indexButtons.get(i);
            button.visible = index && i < 10 * (this.indexPages + 1) && i >= 10 * (this.indexPages);
            button.active = button.visible;
        }
        super.render(ms, mouseX, mouseY, partialTicks);
        int cornerX = (width - X) / 2;
        int cornerY = (height - Y) / 2;
        ms.pose().pushPose();
        ms.pose().translate(cornerX, cornerY, 0.0F);
        if (!index) {
            drawPerPage(ms, bookPages);
            int pageLeft = bookPages * 2 + 1;
            int pageRight = pageLeft + 1;
            drawText(ms, "" + pageLeft, cornerX, cornerY - (int) (Y * 0.13), 0X303030);
            drawText(ms, "" + pageRight, cornerX, cornerY - (int) (Y * 0.13), 0X303030);
        }
        ms.pose().popPose();
    }

    private void drawText(GuiGraphics ms, Object text, float x, float y, int color) {
        IafGui.text(ms, font, String.valueOf(text instanceof Component c ? c.getString() : text), (int) x, (int) y, color);
    }

    public void drawPerPage(GuiGraphics ms, int bookPages) {
        imageFromTxt(ms);
        switch (this.pageType) {
            case INTRODUCTION:
                if (bookPages == 1) {
                    drawItemStack(ms, new ItemStack(IafBlockRegistry.SAPPHIRE_ORE), 30, 20, 2.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SAPPHIRE_GEM), 40, 60, 2F);
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 0, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    boolean drawGold = Minecraft.getInstance().player.tickCount % 20 < 10;
                    drawItemStack(ms, new ItemStack(drawGold ? Items.GOLD_NUGGET : IafItemRegistry.SILVER_NUGGET), 144, 34, 1.5F);
                    drawItemStack(ms, new ItemStack(drawGold ? Items.GOLD_NUGGET : IafItemRegistry.SILVER_NUGGET), 161, 34, 1.5F);
                    drawItemStack(ms, new ItemStack(drawGold ? IafBlockRegistry.GOLD_PILE : IafBlockRegistry.SILVER_PILE), 151, 7, 2F);
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 90, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(Blocks.OAK_PLANKS), 161, 124, 1.5F);
                    drawItemStack(ms, new ItemStack(Blocks.OAK_PLANKS), 161, 107, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MANUSCRIPT), 161, 91, 1.5F);
                    drawItemStack(ms, new ItemStack(IafBlockRegistry.LECTERN), 151, 78, 2F);
                }
                break;
            case FIREDRAGON:
                break;
            case FIREDRAGONEGG:
                break;
            case LIGHTNINGDRAGON:
                break;
            case LIGHTNINGDRAGONEGG:
                break;
            case ICEDRAGON:
                break;
            case ICEDRAGONEGG:
                break;
            case TAMEDDRAGONS:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 90, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(Items.BONE), 145, 124, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.PORKCHOP), 145, 107, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.BONE), 145, 91, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.PORKCHOP), 161, 124, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.BONE), 161, 107, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.PORKCHOP), 161, 91, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.BONE), 177, 124, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.PORKCHOP), 177, 107, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.BONE), 177, 91, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_MEAL), 151, 78, 2F);
                }
                if (bookPages == 1) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 0, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_SKULL_FIRE), 161, 17, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.STICK), 161, 32, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_STAFF), 151, 10, 2F);
                }
                if (bookPages == 2) {
                    ms.pose().pushPose();
                    drawItemStack(ms, new ItemStack(IafBlockRegistry.FIRE_LILY), 5, 14, 3.75F);
                    drawItemStack(ms, new ItemStack(IafBlockRegistry.FROST_LILY), 30, 14, 3.75F);
                    ms.pose().popPose();
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 0, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    boolean drawFire = Minecraft.getInstance().player.tickCount % 40 < 20;
                    drawItemStack(ms, new ItemStack(drawFire ? IafBlockRegistry.FIRE_LILY : IafBlockRegistry.FROST_LILY), 161, 17, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.BOWL), 161, 32, 1.5F);
                    drawItemStack(ms, new ItemStack(drawFire ? Items.BLAZE_ROD : Items.PRISMARINE_CRYSTALS), 177, 17, 1.5F);
                    drawItemStack(ms, new ItemStack(drawFire ? IafItemRegistry.FIRE_STEW : IafItemRegistry.FROST_STEW), 151, 10, 2F);

                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 65, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();

                    drawItemStack(ms, new ItemStack(Items.STICK), 144, 97, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_BONE), 180, 110, 1.35f);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_BONE), 180, 92, 1.35f);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_BONE), 198, 92, 1.35f);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_BONE), 198, 74, 1.35f);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_HORN), 151, 60, 2F);

                }
                if (bookPages == 3) {
                    int j = 18;
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONARMOR_IRON_0, 1), j += 16, 60, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONARMOR_IRON_1, 1), j += 16, 60, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONARMOR_IRON_2, 1), j += 16, 60, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONARMOR_IRON_3, 1), j += 16, 60, 1.5F);

                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 10, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_BONE), 160, 12, 1.35f);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_BONE), 180, 31, 1.35f);
                    drawItemStack(ms, new ItemStack(Items.IRON_INGOT), 199, 50, 1.35f);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_FLUTE), 151, 18, 2F);
                }
                break;
            case MATERIALS:
                if (bookPages == 0) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONSCALES_RED), 18, 16, 3.75F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_BONE), 70, 10, 3.75F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.WITHERBONE), 112, 70, 2.5F);
                    {
                        int j = 18;
                        drawItemStack(ms, new ItemStack(EnumDragonArmor.armor_red.helmet), j += 16, 115, 1.5F);
                        drawItemStack(ms, new ItemStack(EnumDragonArmor.armor_red.chestplate), j += 16, 115, 1.5F);
                        drawItemStack(ms, new ItemStack(EnumDragonArmor.armor_red.leggings), j += 16, 115, 1.5F);
                        drawItemStack(ms, new ItemStack(EnumDragonArmor.armor_red.boots), j += 16, 115, 1.5F);
                    }
                }
                if (bookPages == 1) {
                    {
                        int j = 1;
                        drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONBONE_SWORD), j += 16, 14, 1.5F);
                        drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONBONE_PICKAXE), j += 16, 14, 1.5F);
                        drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONBONE_AXE), j += 16, 14, 1.5F);
                        drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONBONE_SHOVEL), j += 16, 14, 1.5F);
                        drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONBONE_HOE), j += 16, 14, 1.5F);
                        drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_BOW), j += 16, 14, 1.5F);
                    }
                    drawItemStack(ms, new ItemStack(IafItemRegistry.FIRE_DRAGON_FLESH), 18, 24, 3.75F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.FIRE_DRAGON_HEART), 70, 14, 3.75F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGON_SKULL_FIRE), 70, 39, 3.75F);
                }
                if (bookPages == 2) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.FIRE_DRAGON_BLOOD), 18, 24, 3.75F);
                }
                break;
            case ALCHEMY:
                if (bookPages == 0) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.FIRE_DRAGON_BLOOD), 10, 24, 3.75F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.ICE_DRAGON_BLOOD), 26, 24, 3.75F);
                    boolean drawFire = Minecraft.getInstance().player.tickCount % 40 < 20;
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DRAGONBONE_SWORD), 161, 17, 1.5F);
                    drawItemStack(ms, new ItemStack(drawFire ? IafItemRegistry.FIRE_DRAGON_BLOOD : IafItemRegistry.ICE_DRAGON_BLOOD), 161, 32, 1.5F);
                    drawItemStack(ms, new ItemStack(drawFire ? IafItemRegistry.DRAGONBONE_SWORD_FIRE : IafItemRegistry.DRAGONBONE_SWORD_ICE), 151, 10, 2F);
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 0, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                }
                break;
            case HIPPOGRYPH:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (0.8F), (float) (0.8F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 29, 150, 303, 151, 61, 36, 512F);
                    drawImage(ms, DRAWINGS_0, 91, 150, 364, 151, 61, 36, 512F);
                    drawImage(ms, DRAWINGS_0, 151, 150, 425, 151, 61, 36, 512F);
                    drawImage(ms, DRAWINGS_0, 29, 190, 303, 187, 61, 36, 512F);
                    drawImage(ms, DRAWINGS_0, 91, 190, 364, 187, 61, 36, 512F);
                    drawImage(ms, DRAWINGS_0, 151, 190, 425, 187, 61, 36, 512F);
                    drawImage(ms, DRAWINGS_0, 90, 230, 425, 223, 61, 35, 512F);
                    ms.pose().popPose();

                    drawItemStack(ms, new ItemStack(Items.RABBIT_FOOT), 70, 20, 3.75F);
                }

                if (bookPages == 1) {
                    drawItemStack(ms, new ItemStack(Items.STICK), 16, 24, 3.75F);

                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 10, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(Items.FEATHER), 160, 31, 1.35F);
                    int drawType = Minecraft.getInstance().player.tickCount % 60 > 40 ? 2 : Minecraft.getInstance().player.tickCount % 60 > 20 ? 1 : 0;
                    drawItemStack(ms, new ItemStack(drawType == 0 ? Items.IRON_HORSE_ARMOR : drawType == 1 ? Items.GOLDEN_HORSE_ARMOR : Items.DIAMOND_HORSE_ARMOR), 180, 31, 1.35F);
                    drawItemStack(ms, new ItemStack(Items.FEATHER), 199, 31, 1.35F);
                    drawItemStack(ms, new ItemStack(drawType == 0 ? IafItemRegistry.IRON_HIPPOGRYPH_ARMOR : drawType == 1 ? IafItemRegistry.GOLD_HIPPOGRYPH_ARMOR : IafItemRegistry.DIAMOND_HIPPOGRYPH_ARMOR), 151, 18, 2F);
                    drawItemStack(ms, new ItemStack(Items.RABBIT_STEW), 70, 23, 3.75F);
                }
                break;
            case GORGON:
                if (bookPages == 0) {

                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 10, 89, 473, 117, 19, 34, 512F);
                    drawImage(ms, DRAWINGS_0, 50, 78, 399, 106, 28, 45, 512F);
                    drawImage(ms, DRAWINGS_0, 100, 89, 455, 117, 18, 34, 512F);
                    ms.pose().popPose();

                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 70, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();

                    drawItemStack(ms, new ItemStack(Items.STRING), 160, 97, 1.35F);
                    drawItemStack(ms, new ItemStack(Items.LEATHER), 180, 97, 1.35F);
                    drawItemStack(ms, new ItemStack(Items.STRING), 199, 97, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.BLINDFOLD), 171, 65, 2F);
                }
                if (bookPages == 1) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.GORGON_HEAD), 16, 12, 3.75F);
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.7F), (float) (1.7F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 37, 95, 473, 117, 19, 34, 512F);
                    drawImage(ms, DRAWINGS_0, 60, 95, 455, 117, 18, 34, 512F);
                    ms.pose().popPose();

                }
                break;
            case PIXIE:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    drawImage(ms, DRAWINGS_0, 20, 60, 371, 258, 47, 35, 512F);
                    drawImage(ms, DRAWINGS_0, 42, 95, 416, 258, 45, 35, 512F);
                    drawImage(ms, DRAWINGS_0, 67, 60, 462, 258, 47, 35, 512F);
                    drawImage(ms, DRAWINGS_0, 88, 95, 370, 293, 47, 35, 512F);
                    drawImage(ms, DRAWINGS_0, 110, 60, 416, 293, 47, 35, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(IafItemRegistry.PIXIE_DUST), 70, 10, 3.75F);
                    ms.pose().pushPose();
                    ms.pose().scale((float) (0.9F), (float) (0.9F), 1.0F);
                    ms.pose().translate((float) (20), (float) (24), 0.0F);
                    ms.pose().pushPose();
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 150, 100, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(Blocks.GLASS), 160, 113, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.GLASS), 199, 113, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.OAK_PLANKS), 180, 113, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.GLASS), 160, 131, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.GLASS), 199, 131, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.GLASS), 180, 150, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.GLASS), 160, 150, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.GLASS), 199, 150, 1.35F);
                    drawItemStack(ms, new ItemStack(IafBlockRegistry.JAR_EMPTY), 171, 85, 2F);
                    ms.pose().popPose();
                    ms.pose().popPose();

                }
                if (bookPages == 1) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.AMBROSIA), 14, 22, 3.75F);
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 100, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();

                    drawItemStack(ms, new ItemStack(IafItemRegistry.PIXIE_DUST), 180, 131, 1.35F);
                    drawItemStack(ms, new ItemStack(Items.BOWL), 180, 150, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.AMBROSIA), 171, 85, 2F);
                }
                break;
            case CYCLOPS:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 185, 8, 399, 328, 24, 63, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 1) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 50, 35, 423, 328, 24, 63, 512F);
                    //drawImage(ms, DRAWINGS_0, 68, 60, 447, 328, 24, 63, 512F);
                    ms.pose().popPose();

                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 50, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();

                    drawItemStack(ms, new ItemStack(Items.LEATHER_HELMET), 180, 76, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 160, 76, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 199, 76, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 160, 57, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 180, 57, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 199, 57, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SHEEP_HELMET), 165, 45, 2F);

                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 144, 95, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(Items.LEATHER_CHESTPLATE), 180, 126, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 160, 126, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 199, 126, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 160, 107, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 199, 107, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 160, 145, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 180, 145, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 199, 145, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SHEEP_CHESTPLATE), 165, 95, 2F);
                }
                if (bookPages == 2) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 185, 30, 447, 328, 24, 63, 512F);
                    ms.pose().popPose();

                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 13, 24, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(Items.LEATHER_LEGGINGS), 34, 46, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 14, 46, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 53, 46, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 14, 27, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 34, 27, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 53, 27, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 14, 65, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 53, 65, 1.35F);

                    drawItemStack(ms, new ItemStack(IafItemRegistry.SHEEP_LEGGINGS), 64, 27, 2F);
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 13, 84, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(Items.LEATHER_BOOTS), 34, 94, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 14, 113, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 53, 113, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 14, 94, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.WHITE_WOOL), 53, 94, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SHEEP_BOOTS), 64, 73, 2F);
                }
                break;
            case SIREN:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.25F), (float) (1.25F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 190, 25, 0, 0, 25, 42, 512F);
                    drawImage(ms, DRAWINGS_1, 220, 15, 25, 0, 25, 42, 512F);
                    drawImage(ms, DRAWINGS_1, 255, 25, 50, 0, 25, 42, 512F);

                    drawImage(ms, DRAWINGS_1, 190, 135, 0, 42, 26, 28, 512F);
                    drawImage(ms, DRAWINGS_1, 220, 125, 26, 42, 26, 28, 512F);
                    drawImage(ms, DRAWINGS_1, 255, 135, 52, 42, 26, 28, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 1) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.EARPLUGS), 18, 40, 3.75F);


                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 160, 0, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();


                    drawItemStack(ms, new ItemStack(Blocks.OAK_BUTTON), 180, 20, 1.35F);
                    drawItemStack(ms, new ItemStack(Blocks.OAK_BUTTON), 215, 20, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.EARPLUGS), 170, 10, 2F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SHINY_SCALES), 123, 75, 2.25F);
                }
                break;
            case HIPPOCAMPUS:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    drawImage(ms, DRAWINGS_1, 210, 25, 0, 70, 57, 49, 512F);
                    drawImage(ms, DRAWINGS_1, 265, 25, 57, 70, 57, 49, 512F);
                    drawImage(ms, DRAWINGS_1, 320, 25, 0, 119, 57, 49, 512F);
                    drawImage(ms, DRAWINGS_1, 210, 80, 57, 119, 57, 49, 512F);
                    drawImage(ms, DRAWINGS_1, 265, 80, 0, 168, 57, 49, 512F);
                    drawImage(ms, DRAWINGS_1, 320, 80, 57, 168, 57, 49, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 1) {
                    drawItemStack(ms, new ItemStack(Items.KELP), 37, 33, 2.25F);
                    drawItemStack(ms, new ItemStack(Items.PRISMARINE_CRYSTALS), 37, 73, 2.25F);
                }
                if (bookPages == 2) {
                    drawItemStack(ms, new ItemStack(Items.STICK), 35, 25, 2.25F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SHINY_SCALES), 35, 75, 2.25F);
                }
                break;
            case DEATHWORM:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    drawImage(ms, DRAWINGS_1, 230, 25, 0, 217, 133, 16, 512F);
                    drawImage(ms, DRAWINGS_1, 230, 50, 0, 233, 133, 16, 512F);
                    drawImage(ms, DRAWINGS_1, 230, 75, 0, 249, 133, 16, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 1) {
                    ms.pose().pushPose();
                    drawImage(ms, DRAWINGS_1, 25, 95, 0, 265, 148, 44, 512F);
                    drawImage(ms, DRAWINGS_1, 250, 5, 0, 309, 81, 162, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 2) {
                    int drawType = Minecraft.getInstance().player.tickCount % 60 > 40 ? 2 : Minecraft.getInstance().player.tickCount % 60 > 20 ? 1 : 0;
                    Item chitin = IafItemRegistry.DEATH_WORM_CHITIN_YELLOW;
                    if (drawType == 2) {
                        chitin = IafItemRegistry.DEATH_WORM_CHITIN_RED;
                    }
                    if (drawType == 1) {
                        chitin = IafItemRegistry.DEATH_WORM_CHITIN_WHITE;
                    }
                    drawItemStack(ms, new ItemStack(chitin, 1), 17, 30, 3.75F);
                    drawItemStack(ms, new ItemStack(drawType == 2 ? IafItemRegistry.DEATHWORM_RED_HELMET : drawType == 1 ? IafItemRegistry.DEATHWORM_WHITE_HELMET : IafItemRegistry.DEATHWORM_YELLOW_HELMET), 92, 8, 2.25F);
                    drawItemStack(ms, new ItemStack(drawType == 2 ? IafItemRegistry.DEATHWORM_RED_CHESTPLATE : drawType == 1 ? IafItemRegistry.DEATHWORM_WHITE_CHESTPLATE : IafItemRegistry.DEATHWORM_YELLOW_CHESTPLATE), 112, 8, 2.25F);
                    drawItemStack(ms, new ItemStack(drawType == 2 ? IafItemRegistry.DEATHWORM_RED_LEGGINGS : drawType == 1 ? IafItemRegistry.DEATHWORM_WHITE_LEGGINGS : IafItemRegistry.DEATHWORM_YELLOW_LEGGINGS), 132, 8, 2.25F);
                    drawItemStack(ms, new ItemStack(drawType == 2 ? IafItemRegistry.DEATHWORM_RED_BOOTS : drawType == 1 ? IafItemRegistry.DEATHWORM_WHITE_BOOTS : IafItemRegistry.DEATHWORM_YELLOW_BOOTS), 152, 8, 2.25F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DEATHWORM_EGG), 125, 42, 2.25F);
                }
                if (bookPages == 3) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.DEATHWORM_EGG_GIGANTIC, 1), 125, 4, 2.25F);
                    drawItemStack(ms, new ItemStack(Items.FISHING_ROD), 115, 55, 2.25F);
                    drawItemStack(ms, new ItemStack(Items.FISHING_ROD), 135, 55, 2.25F);
                }
                break;
            case COCKATRICE:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 155, 10, 114, 0, 88, 36, 512F);
                    drawImage(ms, DRAWINGS_1, 155, 45, 114, 36, 88, 36, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 1) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 18, 10, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();

                    drawItemStack(ms, new ItemStack(Items.STRING), 20, 30, 1.35F);
                    drawItemStack(ms, new ItemStack(Items.LEATHER), 40, 30, 1.35F);
                    drawItemStack(ms, new ItemStack(Items.STRING), 59, 30, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.BLINDFOLD), 60, 18, 2F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.WITHERBONE), 30, 58, 2.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.ROTTEN_EGG), 109, 18, 2.5F);
                }
                break;
            case STYMPHALIANBIRD:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 34, 46, 114, 72, 59, 37, 512F);
                    drawImage(ms, DRAWINGS_1, 155, 35, 114, 109, 67, 35, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(IafItemRegistry.STYMPHALIAN_BIRD_FEATHER), 109, 60, 2.5F);
                }
                if (bookPages == 1) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 18, 10, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();

                    drawItemStack(ms, new ItemStack(Items.FLINT), 40, 13, 1.35F);
                    drawItemStack(ms, new ItemStack(Items.STICK), 40, 30, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.STYMPHALIAN_BIRD_FEATHER), 40, 49, 1.35F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.STYMPHALIAN_ARROW), 60, 18, 2F);

                }
                break;
            case TROLL:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 15, 60, 156, 211, 25, 58, 512F);
                    drawImage(ms, DRAWINGS_1, 50, 55, 181, 211, 25, 58, 512F);
                    drawImage(ms, DRAWINGS_1, 85, 60, 206, 211, 25, 58, 512F);
                    drawImage(ms, DRAWINGS_1, 155, 22, 114, 145, 24, 66, 512F);
                    drawImage(ms, DRAWINGS_1, 190, 19, 188, 142, 47, 69, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 1) {
                    int i = (Minecraft.getInstance().player.tickCount % (EnumTroll.Weapon.values().length * 20)) / 20;
                    drawItemStack(ms, new ItemStack(EnumTroll.Weapon.values()[i].item), 30, 7, 2.5F);
                    int j = (Minecraft.getInstance().player.tickCount % (EnumTroll.values().length * 20)) / 20;
                    drawItemStack(ms, new ItemStack(EnumTroll.values()[j].leather), 100, 30, 2.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.TROLL_TUSK), 120, 30, 2.5F);
                }
                if (bookPages == 2) {
                    int j = (Minecraft.getInstance().player.tickCount % (EnumTroll.values().length * 20)) / 20;
                    drawItemStack(ms, new ItemStack(EnumTroll.values()[j].helmet), 27, 15, 1.5F);
                    drawItemStack(ms, new ItemStack(EnumTroll.values()[j].chestplate), 47, 15, 1.5F);
                    drawItemStack(ms, new ItemStack(EnumTroll.values()[j].leggings), 67, 15, 1.5F);
                    drawItemStack(ms, new ItemStack(EnumTroll.values()[j].boots), 87, 15, 1.5F);
                }
                break;
            case MYRMEX:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.51F), (float) (1.51F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 137, 10, 202, 16, 57, 21, 512F);
                    drawImage(ms, DRAWINGS_1, 195, 10, 278, 16, 57, 21, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 1) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.51F), (float) (1.51F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 7, 17, 202, 37, 59, 21, 512F);
                    drawImage(ms, DRAWINGS_1, 65, 17, 278, 37, 59, 21, 512F);
                    drawImage(ms, DRAWINGS_1, 7, 77, 202, 58, 59, 21, 512F);
                    drawImage(ms, DRAWINGS_1, 65, 77, 278, 58, 59, 21, 512F);
                    drawImage(ms, DRAWINGS_1, 145, 20, 278, 103, 43, 45, 512F);
                    drawImage(ms, DRAWINGS_1, 195, 20, 321, 103, 43, 45, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 2) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.51F), (float) (1.51F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 25, 13, 202, 79, 76, 24, 512F);
                    drawImage(ms, DRAWINGS_1, 25, 40, 278, 79, 76, 24, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_DESERT_CHITIN), 125, 43, 2F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_JUNGLE_CHITIN), 155, 43, 2F);
                    int i = 133;
                    boolean jungle = Minecraft.getInstance().player.tickCount % 60 > 30;
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_SHOVEL : IafItemRegistry.MYRMEX_DESERT_SHOVEL), i += 16, 100, 1.51F);
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_PICKAXE : IafItemRegistry.MYRMEX_DESERT_PICKAXE), i += 16, 100, 1.5F);
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_AXE : IafItemRegistry.MYRMEX_DESERT_AXE), i += 16, 100, 1.5F);
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_SWORD : IafItemRegistry.MYRMEX_DESERT_SWORD), i += 16, 100, 1.5F);
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_SWORD_VENOM : IafItemRegistry.MYRMEX_DESERT_SWORD_VENOM), i += 16, 100, 1.5F);
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_HOE : IafItemRegistry.MYRMEX_DESERT_HOE), i += 16, 100, 1.5F);
                    int j = 148;
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_HELMET : IafItemRegistry.MYRMEX_DESERT_HELMET), j += 16, 115, 1.5F);
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_CHESTPLATE : IafItemRegistry.MYRMEX_DESERT_CHESTPLATE), j += 16, 115, 1.5F);
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_LEGGINGS : IafItemRegistry.MYRMEX_DESERT_LEGGINGS), j += 16, 115, 1.5F);
                    drawItemStack(ms, new ItemStack(jungle ? IafItemRegistry.MYRMEX_JUNGLE_BOOTS : IafItemRegistry.MYRMEX_DESERT_BOOTS), j += 16, 115, 1.5F);
                }
                if (bookPages == 3) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_STINGER), 35, 22, 2.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_DESERT_RESIN), 25, 64, 2F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_JUNGLE_RESIN), 55, 64, 2F);
                }
                if (bookPages == 4) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_DESERT_STAFF), 25, 73, 2F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_JUNGLE_STAFF), 55, 73, 2F);

                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_DESERT_EGG), 125, 90, 2F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.MYRMEX_JUNGLE_EGG), 155, 90, 2F);
                }
                break;
            case AMPHITHERE:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (0.75F), (float) (0.75F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 70, 97, 257, 163, 136, 93, 512F);
                    drawImage(ms, DRAWINGS_1, 270, 50, 148, 267, 120, 51, 512F);
                    drawImage(ms, DRAWINGS_1, 380, 50, 148, 318, 120, 51, 512F);
                    drawImage(ms, DRAWINGS_1, 270, 100, 148, 369, 120, 51, 512F);
                    drawImage(ms, DRAWINGS_1, 380, 100, 148, 420, 120, 51, 512F);
                    drawImage(ms, DRAWINGS_1, 330, 150, 268, 267, 120, 51, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 2) {
                    drawItemStack(ms, new ItemStack(IafItemRegistry.AMPHITHERE_FEATHER), 30, 20, 2.5F);
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 19, 71, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    drawItemStack(ms, new ItemStack(Items.FLINT), 36, 73, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.STICK), 36, 89, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.AMPHITHERE_FEATHER), 36, 106, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.AMPHITHERE_ARROW), 60, 65, 2F);
                }
                break;
            case SEASERPENT:
                if (bookPages == 0) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (0.75F), (float) (0.75F), 1.0F);
                    drawImage(ms, DRAWINGS_1, 290, 5, 422, 0, 90, 64, 512F);
                    drawImage(ms, DRAWINGS_1, 380, 5, 422, 64, 90, 64, 512F);
                    drawImage(ms, DRAWINGS_1, 290, 70, 422, 128, 90, 64, 512F);
                    drawImage(ms, DRAWINGS_1, 380, 70, 422, 192, 90, 64, 512F);
                    drawImage(ms, DRAWINGS_1, 290, 140, 422, 256, 90, 64, 512F);
                    drawImage(ms, DRAWINGS_1, 380, 140, 422, 320, 90, 64, 512F);
                    drawImage(ms, DRAWINGS_1, 345, 210, 422, 384, 90, 64, 512F);
                    ms.pose().popPose();
                }
                if (bookPages == 1) {
                    drawImage(ms, DRAWINGS_1, 60, 90, 337, 0, 70, 83, 512F);
                    int j = (Minecraft.getInstance().player.tickCount % (EnumSeaSerpent.values().length * 20)) / 20;
                    drawItemStack(ms, new ItemStack(EnumSeaSerpent.values()[j].scale), 130, 40, 2.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SERPENT_FANG), 90, 40, 2.5F);
                }
                if (bookPages == 2) {
                    ms.pose().pushPose();
                    ms.pose().scale((float) (1.5F), (float) (1.5F), 1.0F);
                    drawImage(ms, DRAWINGS_0, 19, 31, 389, 1, 50, 50, 512F);
                    ms.pose().popPose();
                    int j = (Minecraft.getInstance().player.tickCount % (EnumSeaSerpent.values().length * 20)) / 20;
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SERPENT_FANG), 36, 32, 1.5F);
                    drawItemStack(ms, new ItemStack(Items.STICK), 36, 48, 1.5F);
                    drawItemStack(ms, new ItemStack(EnumSeaSerpent.values()[j].scale), 36, 66, 1.5F);
                    drawItemStack(ms, new ItemStack(EnumSeaSerpent.values()[j].helmet), 34, 125, 1.5F);
                    drawItemStack(ms, new ItemStack(EnumSeaSerpent.values()[j].chestplate), 50, 125, 1.5F);
                    drawItemStack(ms, new ItemStack(EnumSeaSerpent.values()[j].leggings), 66, 125, 1.5F);
                    drawItemStack(ms, new ItemStack(EnumSeaSerpent.values()[j].boots), 82, 125, 1.5F);
                    drawItemStack(ms, new ItemStack(IafItemRegistry.SEA_SERPENT_ARROW), 60, 33, 2F);
                }
                break;
            default:
                break;
        }
        writeFromTxt(ms);
    }

    /**
     * A page of the book in the game's language: its own, else the same language's (Spanish of Spain for any Spanish,
     * Brazilian for any Portuguese, traditional Chinese for Hong Kong), else English.
     */
    private static Resource pageResource(String fileName) {
        String languageName = Minecraft.getInstance().options.languageCode.toLowerCase(Locale.ROOT);
        String sameLanguage = languageName.startsWith("es_") ? "es_es" : languageName.startsWith("pt_") ? "pt_br"
            : languageName.equals("zh_hk") ? "zh_tw" : languageName;
        for (String folder : new String[]{languageName, sameLanguage, "en_us"}) {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.parse("iceandfire_fabric:lang/bestiary/" + folder + "_0/" + fileName));
            if (resource.isPresent()) {
                return resource.get();
            }
        }
        IafMod.LOGGER.warn("The bestiary has no page {}", fileName);
        return null;
    }

    public void imageFromTxt(GuiGraphics ms) {
        String fileName = this.pageType.toString().toLowerCase(Locale.ROOT) + "_" + this.bookPages + ".txt";
        Resource resource = pageResource(fileName);
        try {
            final List<String> lines = IOUtils.readLines(resource.open(), StandardCharsets.UTF_8);
            int zLevelAdd = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.contains("<") || line.contains(">")) {
                    if (line.contains("<image>")) {
                        line = line.substring(8, line.length() - 1);
                        String[] split = line.split(" ");
                        String texture = "iceandfire_fabric:textures/gui/bestiary/" + split[0];
                        ResourceLocation resourcelocation = PICTURE_LOCATION_CACHE.get(texture);
                        if (resourcelocation == null) {
                            resourcelocation = ResourceLocation.parse(texture);
                            PICTURE_LOCATION_CACHE.put(texture, resourcelocation);
                        }
                        ms.pose().pushPose();
                        drawImage(ms, resourcelocation, Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]), Integer.parseInt(split[5]), Integer.parseInt(split[6]), Float.parseFloat(split[7]) * 512F);
                        ms.pose().popPose();
                    }
                }
                if (line.contains("<item>")) {
                    line = line.substring(7, line.length() - 1);
                    String[] split = line.split(" ");
                    drawItemStack(ms, new ItemStack(getItemByRegistryName(split[0]), 1), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Float.parseFloat(split[4]) * 2F);
                }
                if (line.contains("<block>")) {
                    zLevelAdd += 1;
                    line = line.substring(8, line.length() - 1);
                    String[] split = line.split(" ");
                    drawBlockStack(ms, new ItemStack(getItemByRegistryName(split[0]), 1), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Float.parseFloat(split[4]) * 2F, zLevelAdd);
                }
                if (line.contains("<recipe>")) {
                    line = line.substring(9, line.length() - 1);
                    String[] split = line.split(" ");
                    float scale = Float.parseFloat(split[split.length - 1]);
                    int x = Integer.parseInt(split[split.length - 3]);
                    int y = Integer.parseInt(split[split.length - 2]);
                    ItemStack result = new ItemStack(getItemByRegistryName(split[0]), 1);
                    ItemStack[] ingredients = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
                    int j = 8;
                    for (int i = split.length - 5; i >= 2; i -= 2) {
                        ingredients[j] = new ItemStack(getItemByRegistryName(split[i]), 1);
                        j--;
                    }
                    ms.pose().pushPose();
                    drawRecipe(ms, result, ingredients, x, y, scale);
                    ms.pose().popPose();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawRecipe(GuiGraphics ms, ItemStack result, ItemStack[] ingredients, int x, int y, float scale) {
        // The pose is at the book's corner; the recipe grid is offset like Citadel's GuiBasicBook did.
        int k = 0;
        int l = 20;
        for (int i = 0; i < 9; i++) {
            ms.pose().pushPose();
            ms.pose().translate(42 + k + x + (i % 3 * 22) * scale, l + y + (i / 3 * 22) * scale, 0.0F);
            ms.pose().scale(scale, scale, 1.0F);
            ms.renderItem(ingredients[i], 0, 0);
            ms.pose().popPose();
        }
        float finScale = scale * 1.5F;
        ms.pose().pushPose();
        ms.pose().translate(42 + k + x + 70.0F * finScale, l + y + 10.0F * finScale, 0.0F);
        ms.pose().scale(finScale, finScale, 1.0F);
        ms.renderItem(result, 0, 0);
        ms.pose().popPose();
        ms.pose().pushPose();
        ms.pose().translate(x, y, 0.0F);
        ms.pose().scale(scale, scale, 1.0F);
        ms.pose().translate(37F, 13, 0.0F);
        ms.pose().scale(1.5F, 1.5F, 1.0F);
        drawImage(ms, DRAWINGS_0, 0, 0, 389, 1, 50, 50, 512F);
        ms.pose().popPose();
    }

    public void writeFromTxt(GuiGraphics ms) {
        String fileName = this.pageType.toString().toLowerCase(Locale.ROOT) + "_" + this.bookPages + ".txt";
        Resource resource = pageResource(fileName);
        try {
            final List<String> lines = IOUtils.readLines(resource.open(), "UTF-8");
            int linenumber = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.contains("<") || line.contains(">")) {
                    continue;
                }
                ms.pose().pushPose();
                if (usingVanillaFont()) {
                    ms.pose().scale((float) (0.945F), (float) (0.945F), 1.0F);
                    ms.pose().translate((float) (0), (float) (5.5F), 0.0F);
                }
                if (linenumber <= 19) {
                    IafGui.fit(ms, font, line, 15, 20 + linenumber * 10, PAGE_TEXT_WIDTH, 0X303030);
                } else {
                    IafGui.fit(ms, font, line, 220, (linenumber - 19) * 10, PAGE_TEXT_WIDTH, 0X303030);
                }
                linenumber++;
                ms.pose().popPose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ms.pose().pushPose();
        String s = StatCollector.translateToLocal("bestiary." + this.pageType.toString().toLowerCase(Locale.ROOT));
        float scale = Math.min(2.0F, 170.0F / Math.max(1, font.width(s)));
        ms.pose().scale((float) (scale), (float) (scale), 1.0F);
        drawText(ms, s, 10, 2, 0X7A756A);
        ms.pose().popPose();
    }

    private boolean usingVanillaFont() {
        return font == Minecraft.getInstance().font;
    }

    public void drawImage(GuiGraphics ms, ResourceLocation texture, int x, int y, int u, int v, int width, int height, float scale) {
        ms.pose().pushPose();
        ms.pose().scale(scale / 512F, scale / 512F, 1.0F);
        IafGui.blit(ms, texture, x, y, u, v, width, height, 512, 512, 1.0F);
        ms.pose().popPose();
    }

    /** An item at (x, y) in a grid scaled by scale, from the book's corner (the pose is already there). */
    private void drawItemStack(GuiGraphics ms, ItemStack stack, int x, int y, float scale) {
        ms.pose().pushPose();
        ms.pose().scale(scale, scale, 1.0F);
        ms.renderItem(stack, x, y);
        ms.pose().popPose();
    }

    private void drawBlockStack(GuiGraphics ms, ItemStack stack, int x, int y, float scale, int zScale) {
        drawItemStack(ms, stack, x, y, scale);
    }
}
