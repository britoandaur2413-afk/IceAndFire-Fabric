package com.iafabric.client.gui;

import com.iafabric.network.IafNetwork;
import com.iafabric.client.ClientProxy;
import com.iafabric.client.gui.bestiary.ChangePageButton;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessageGetMyrmexHive;
import com.iafabric.world.gen.WorldGenMyrmexHive;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class GuiMyrmexStaff extends Screen {
    private static final ResourceLocation JUNGLE_TEXTURE = new ResourceLocation("iceandfire_fabric:textures/gui/myrmex_staff_jungle.png");
    private static final ResourceLocation DESERT_TEXTURE = new ResourceLocation("iceandfire_fabric:textures/gui/myrmex_staff_desert.png");
    private static final WorldGenMyrmexHive.RoomType[] ROOMS = {WorldGenMyrmexHive.RoomType.FOOD, WorldGenMyrmexHive.RoomType.NURSERY, WorldGenMyrmexHive.RoomType.EMPTY};
    private static final int ROOMS_PER_PAGE = 5;
    private final List<Room> allRoomPos = Lists.newArrayList();
    public ChangePageButton previousPage;
    public ChangePageButton nextPage;
    int ticksSinceDeleted = 0;
    int currentPage = 0;
    private final boolean jungle;

    public GuiMyrmexStaff(ItemStack staff) {
        super(Component.translatable("gui.iceandfire_fabric.myrmex_staff"));
        this.jungle = staff.getItem() == IafItemRegistry.MYRMEX_JUNGLE_STAFF;
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - 248) / 2;
        int j = (this.height - 166) / 2;
        if (ClientProxy.getReferedClientHive() == null) {
            return;
        }
        populateRoomMap();
        if (allRoomPos.size() <= ROOMS_PER_PAGE * this.currentPage && this.currentPage > 0) {
            this.currentPage--;
        }
        this.addRenderableWidget(Button.builder(ClientProxy.getReferedClientHive().reproduces ? Component.translatable("myrmex.message.disablebreeding") : Component.translatable("myrmex.message.enablebreeding"), button -> {
            ClientProxy.getReferedClientHive().reproduces = !ClientProxy.getReferedClientHive().reproduces;
            this.rebuildWidgets();
        }).bounds(i + 124, j + 15, 120, 20).build());
        this.previousPage = this.addRenderableWidget(new ChangePageButton(i + 5, j + 150, false, this.jungle ? 2 : 1, button -> {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.rebuildWidgets();
            }
        }));
        this.nextPage = this.addRenderableWidget(new ChangePageButton(i + 225, j + 150, true, this.jungle ? 2 : 1, button -> {
            if (this.currentPage < (this.allRoomPos.size() - 1) / ROOMS_PER_PAGE) {
                this.currentPage++;
                this.rebuildWidgets();
            }
        }));
        for (int rooms = ROOMS_PER_PAGE * this.currentPage; rooms < Math.min(allRoomPos.size(), ROOMS_PER_PAGE * (this.currentPage + 1)); rooms++) {
            BlockPos pos = allRoomPos.get(rooms).pos;
            this.addRenderableWidget(new MyrmexDeleteButton(i + 193, j + 37 + (rooms % ROOMS_PER_PAGE) * 22, pos, Component.translatable("myrmex.message.delete"), button -> {
                if (ticksSinceDeleted <= 0) {
                    ClientProxy.getReferedClientHive().removeRoom(pos);
                    ticksSinceDeleted = 5;
                    this.rebuildWidgets();
                }
            }));
        }
    }

    private void populateRoomMap() {
        allRoomPos.clear();
        for (WorldGenMyrmexHive.RoomType type : ROOMS) {
            String name = type == WorldGenMyrmexHive.RoomType.FOOD ? "food" : type == WorldGenMyrmexHive.RoomType.NURSERY ? "nursery" : "misc";
            for (BlockPos pos : ClientProxy.getReferedClientHive().getRooms(type)) {
                allRoomPos.add(new Room(pos, name));
            }
        }
        for (BlockPos pos : ClientProxy.getReferedClientHive().getEntrances().keySet()) {
            allRoomPos.add(new Room(pos, "enterance_surface"));
        }
        for (BlockPos pos : ClientProxy.getReferedClientHive().getEntranceBottoms().keySet()) {
            allRoomPos.add(new Room(pos, "enterance_bottom"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (ticksSinceDeleted > 0) {
            ticksSinceDeleted--;
        }
    }

    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(g);
        IafGui.blit(g, jungle ? JUNGLE_TEXTURE : DESERT_TEXTURE, (this.width - 248) / 2, (this.height - 166) / 2, 0, 0, 248, 166);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(g, mouseX, mouseY, partialTicks);
        super.render(g, mouseX, mouseY, partialTicks);
        int i = (this.width - 248) / 2 + 10;
        int j = (this.height - 166) / 2 + 8;
        int color = this.jungle ? 0X35EA15 : 0XFFBF00;
        int line = 0;
        for (int rooms = ROOMS_PER_PAGE * this.currentPage; rooms < Math.min(allRoomPos.size(), ROOMS_PER_PAGE * (this.currentPage + 1)); rooms++) {
            Room room = this.allRoomPos.get(rooms);
            IafGui.fit(g, font, I18n.get("myrmex.message.room." + room.string, room.pos.getX(), room.pos.getY(), room.pos.getZ()), i, j + 36 + line * 22, 178, color);
            line++;
        }
        if (ClientProxy.getReferedClientHive() != null) {
            if (!ClientProxy.getReferedClientHive().colonyName.isEmpty()) {
                IafGui.fitCentered(g, font, I18n.get("myrmex.message.colony_named", ClientProxy.getReferedClientHive().colonyName), i + 114, j - 3, 220, color);
            } else {
                IafGui.fitCentered(g, font, I18n.get("myrmex.message.colony"), i + 114, j - 3, 220, color);
            }
            int opinion = ClientProxy.getReferedClientHive().getPlayerReputation(Minecraft.getInstance().player.getUUID());
            IafGui.fit(g, font, I18n.get("myrmex.message.hive_opinion", opinion), i, j + 12, 108, color);
            IafGui.text(g, font, I18n.get("myrmex.message.rooms"), i, j + 25, color);
        }
    }

    @Override
    public void removed() {
        if (ClientProxy.getReferedClientHive() != null) {
            IafNetwork.sendToServer(new MessageGetMyrmexHive(ClientProxy.getReferedClientHive().toNBT()));
        }
    }

    private record Room(BlockPos pos, String string) {
    }
}
