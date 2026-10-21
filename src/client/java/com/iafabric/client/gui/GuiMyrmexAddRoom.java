package com.iafabric.client.gui;

import com.iafabric.network.IafNetwork;
import com.iafabric.client.ClientProxy;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessageGetMyrmexHive;
import com.iafabric.world.gen.WorldGenMyrmexHive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class GuiMyrmexAddRoom extends Screen {
    private static final Identifier JUNGLE_TEXTURE = Identifier.parse("iceandfire_fabric:textures/gui/myrmex_staff_jungle.png");
    private static final Identifier DESERT_TEXTURE = Identifier.parse("iceandfire_fabric:textures/gui/myrmex_staff_desert.png");
    private final boolean jungle;
    private final BlockPos interactPos;
    private final Direction facing;

    public GuiMyrmexAddRoom(ItemStack staff, BlockPos interactPos, Direction facing) {
        super(Component.translatable("gui.iceandfire_fabric.myrmex_add_room"));
        this.jungle = staff.getItem() == IafItemRegistry.MYRMEX_JUNGLE_STAFF;
        this.interactPos = interactPos;
        this.facing = facing;
    }

    public static void onGuiClosed() {
        IafNetwork.sendToServer(new MessageGetMyrmexHive(ClientProxy.getReferedClientHive().toNBT()));
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - 248) / 2;
        int j = (this.height - 166) / 2;
        if (ClientProxy.getReferedClientHive() != null) {
            Player player = Minecraft.getInstance().player;
            room(i, j + 35, "establishroom_food", hive -> hive.addRoomWithMessage(player, interactPos, WorldGenMyrmexHive.RoomType.FOOD));
            room(i, j + 60, "establishroom_nursery", hive -> hive.addRoomWithMessage(player, interactPos, WorldGenMyrmexHive.RoomType.NURSERY));
            room(i, j + 85, "establishroom_enterance_surface", hive -> hive.addEnteranceWithMessage(player, false, interactPos, facing));
            room(i, j + 110, "establishroom_enterance_bottom", hive -> hive.addEnteranceWithMessage(player, true, interactPos, facing));
            room(i, j + 135, "establishroom_misc", hive -> hive.addRoomWithMessage(player, interactPos, WorldGenMyrmexHive.RoomType.EMPTY));
        }
    }

    private void room(int i, int y, String key, Consumer<com.iafabric.entity.util.MyrmexHive> action) {
        this.addRenderableWidget(Button.builder(Component.translatable("myrmex.message." + key), button -> {
            action.accept(ClientProxy.getReferedClientHive());
            onGuiClosed();
            Minecraft.getInstance().gui.setScreen(null);
        }).bounds(i + 50, y, 150, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(g, mouseX, mouseY, partialTicks);
        IafGui.blit(g, jungle ? JUNGLE_TEXTURE : DESERT_TEXTURE, (this.width - 248) / 2, (this.height - 166) / 2, 0, 0, 248, 166);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(g, mouseX, mouseY, partialTicks);
        int i = (this.width - 248) / 2 + 10;
        int j = (this.height - 166) / 2 + 8;
        int color = this.jungle ? 0X35EA15 : 0XFFBF00;
        if (ClientProxy.getReferedClientHive() != null) {
            if (!ClientProxy.getReferedClientHive().colonyName.isEmpty()) {
                IafGui.fitCentered(g, font, I18n.get("myrmex.message.colony_named", ClientProxy.getReferedClientHive().colonyName), i + 114, j - 3, 220, color);
            } else {
                IafGui.fitCentered(g, font, I18n.get("myrmex.message.colony"), i + 114, j - 3, 220, color);
            }
            IafGui.fitCentered(g, font, I18n.get("myrmex.message.create_new_room", interactPos.getX(), interactPos.getY(), interactPos.getZ()), i + 114, j + 6, 220, color);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
