package com.iafabric.client.gui;

import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.message.MessagePixieBond;
import com.iafabric.message.MessagePixieBondSync;
import com.iafabric.network.IafNetwork;
import com.iafabric.pixie.PixieBonds;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The pixie bonds menu: a card for each pixie color. A bonded one shows its pixie, level, shield, the part of its
 * damage its player takes and its orbs; "Open" (or a click on the pixie) opens its own menu, and it is called out or
 * sent to rest. One not bonded shows a lock, and "Bond" when a tamed pixie of that color is near.
 */
public class GuiPixieBonds extends Screen {
	private static final Identifier LOCK = Identifier.parse("iceandfire_fabric:textures/gui/pixie_lock.png");
	private static final int CARD_W = 78;
	private static final int CARD_H = 150;
	private static final int GAP = 4;
	/** The pixies' colors, for the card borders. */
	private static final int[] COLORS = {0xFFF7A8C8, 0xFFC9A0F5, 0xFF8FD3F5, 0xFF9FE39A, 0xFFF9CF6E};

	private final EntityPixie[] models = new EntityPixie[PixieBonds.COLORS];
	private PixieBonds shown;

	public GuiPixieBonds() {
		super(Component.translatable("gui.iceandfire_fabric.pixie_bonds"));
	}

	private int left() {
		return (this.width - (CARD_W * PixieBonds.COLORS + GAP * (PixieBonds.COLORS - 1))) / 2;
	}

	private int top() {
		return Math.max(18, (this.height - CARD_H) / 2 + 6);
	}

	@Override
	protected void init() {
		super.init();
		shown = MessagePixieBondSync.client;
		for (int color = 0; color < PixieBonds.COLORS; color++) {
			int x = left() + color * (CARD_W + GAP);
			int y = top();
			PixieBonds.Bond bond = shown.get(color);
			final int c = color;
			if (bond.bonded) {
				this.addRenderableWidget(Button.builder(Component.translatable("gui.iceandfire_fabric.pixie_bonds.open"), b -> send(MessagePixieBond.MENU, c))
					.bounds(x + 4, y + 112, CARD_W - 8, 16).build());
				boolean out = shown.active == color;
				Component label = Component.translatable(out ? "gui.iceandfire_fabric.pixie_bonds.rest" : "gui.iceandfire_fabric.pixie_bonds.summon");
				this.addRenderableWidget(Button.builder(label, b -> send(out ? MessagePixieBond.REST : MessagePixieBond.SUMMON, c)).bounds(x + 4, y + 130, CARD_W - 8, 16).build());
			} else if (pixieNear(color)) {
				this.addRenderableWidget(Button.builder(Component.translatable("gui.iceandfire_fabric.pixie_bonds.bond"), b -> send(MessagePixieBond.BOND, c))
					.bounds(x + 4, y + CARD_H - 22, CARD_W - 8, 18).build());
			}
		}
	}

	private void send(int action, int color) {
		IafNetwork.sendToServer(new MessagePixieBond(action, color));
	}

	/** A tamed pixie of that color, of this player, not bonded, within 16 blocks. */
	private boolean pixieNear(int color) {
		var player = Minecraft.getInstance().player;
		return player != null && !player.level().getEntitiesOfClass(EntityPixie.class, player.getBoundingBox().inflate(16),
			p -> p.isTame() && p.isOwnedBy(player) && p.getColor() == color && !p.isBonded() && !(p instanceof com.iafabric.entity.EntityExiledPixie)).isEmpty();
	}

	/** The mouse on a bonded pixie's picture. */
	private boolean onPixie(int color, double mouseX, double mouseY) {
		int x = left() + color * (CARD_W + GAP);
		int y = top();
		return shown.get(color).bonded && mouseX >= x + 4 && mouseX < x + CARD_W - 4 && mouseY >= y + 14 && mouseY < y + 62;
	}

	/** A click on a bonded pixie opens its own menu. */
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			for (int color = 0; color < PixieBonds.COLORS; color++) {
				if (onPixie(color, event.x(), event.y())) {
					send(MessagePixieBond.MENU, color);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void tick() {
		super.tick();
		if (shown != MessagePixieBondSync.client) {
			this.rebuildWidgets();
		}
	}

	private EntityPixie model(int color) {
		if (models[color] == null && Minecraft.getInstance().level != null) {
			EntityPixie pixie = IafEntityRegistry.PIXIE.create(Minecraft.getInstance().level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
			if (pixie != null) {
				pixie.setColor(color);
			}
			models[color] = pixie;
		}
		return models[color];
	}

	/** The cards' panels, under the buttons. */
	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(g, mouseX, mouseY, partialTicks);
		for (int color = 0; color < PixieBonds.COLORS; color++) {
			int x = left() + color * (CARD_W + GAP);
			int y = top();
			g.fill(x, y, x + CARD_W, y + CARD_H, 0xB0101018);
			g.outline(x, y, CARD_W, CARD_H, shown.active == color ? 0xFFFFFFFF : COLORS[color]);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(g, mouseX, mouseY, partialTicks);
		IafGui.fitCentered(g, font, this.title.getString(), this.width / 2, top() - 13, this.width - 20, 0xFFFFFFFF);
		for (int color = 0; color < PixieBonds.COLORS; color++) {
			int x = left() + color * (CARD_W + GAP);
			int y = top();
			PixieBonds.Bond bond = shown.get(color);
			int tint = COLORS[color];
			IafGui.fitCentered(g, font, Component.translatable("pixie.iceandfire_fabric.element." + color).getString(), x + CARD_W / 2, y + 4, CARD_W - 6, tint);
			int cx = x + CARD_W / 2;
			if (bond.bonded) {
				EntityPixie pixie = model(color);
				if (pixie != null) {
					IafGui.entityInBox(g, pixie, x + 4, y + 14, x + CARD_W - 4, y + 62, 60, mouseX, mouseY);
				}
				if (onPixie(color, mouseX, mouseY)) {
					g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.iceandfire_fabric.pixie_bonds.open_hint")), mouseX, mouseY);
				}
				IafGui.fitCentered(g, font, Component.translatable("gui.iceandfire_fabric.pixie_bonds.level", bond.level).getString(), cx, y + 64, CARD_W - 6, 0xFFFFFFFF);
				int need = bond.level >= 5 ? 1 : PixieBonds.LEVEL_XP[bond.level - 1];
				int from = bond.level <= 1 ? 0 : PixieBonds.LEVEL_XP[bond.level - 2];
				float progress = bond.level >= 5 ? 1F : Math.max(0F, Math.min(1F, (bond.xp - from) / (float) Math.max(1, need - from)));
				g.fill(x + 6, y + 75, x + CARD_W - 6, y + 78, 0xFF303030);
				g.fill(x + 6, y + 75, x + 6 + (int) ((CARD_W - 12) * progress), y + 78, tint);
				IafGui.fitCentered(g, font, Component.translatable("gui.iceandfire_fabric.pixie_bonds.shield", (int) bond.shield, bond.maxShield()).getString(), cx, y + 80, CARD_W - 6, 0xFFB8E0FF);
				IafGui.fitCentered(g, font, Component.translatable("gui.iceandfire_fabric.pixie_bonds.share", Math.round(bond.share() * 100)).getString(), cx, y + 89, CARD_W - 6, 0xFFE0C0C0);
				// its orbs, small, in a row (a faint mark where one is missing)
				for (int k = 0; k < bond.orbs.length; k++) {
					int ox = x + 6 + k * 14;
					int oy = y + 99;
					if (bond.orbs[k].isEmpty()) {
						g.fill(ox + 2, oy + 2, ox + 10, oy + 10, 0x40FFFFFF);
					} else {
						g.pose().pushMatrix();
						g.pose().translate(ox, oy);
						g.pose().scale(0.75F, 0.75F);
						g.item(bond.orbs[k], 0, 0);
						g.pose().popMatrix();
					}
				}
				if (mouseX >= x + 6 && mouseX < x + 6 + 14 * 5 && mouseY >= y + 99 && mouseY < y + 111) {
					List<Component> tip = new ArrayList<>();
					tip.add(Component.translatable("gui.iceandfire_fabric.pixie_menu.orbs"));
					for (ItemStack orb : bond.orbs) {
						if (!orb.isEmpty()) {
							tip.add(orb.getHoverName().copy().withStyle(ChatFormatting.GRAY));
						}
					}
					if (tip.size() == 1) {
						tip.add(Component.translatable("gui.iceandfire_fabric.pixie_bonds.no_orb").withStyle(ChatFormatting.GRAY));
					}
					g.setComponentTooltipForNextFrame(font, tip, mouseX, mouseY);
				}
			} else {
				IafGui.blit(g, LOCK, cx - 16, y + 26, 0F, 0F, 32, 32, 32, 32, 1F);
				IafGui.fitCentered(g, font, Component.translatable("gui.iceandfire_fabric.pixie_bonds.locked").getString(), cx, y + 66, CARD_W - 6, 0xFFA0A0A0);
				if (!pixieNear(color)) {
					IafGui.paragraph(g, font, Component.translatable("gui.iceandfire_fabric.pixie_bonds.need_pixie"), cx, y + 84, CARD_W - 8, 0xFF909090, 1F, 3,
						true, true);
				}
			}
		}
	}

	/** For the tests: whether "bring a tamed pixie of this color" goes all in its card in the game's language now, its letters at least this size. */
	public static boolean needFits(float least) {
		IafGui.Layout layout = IafGui.layout(net.minecraft.client.Minecraft.getInstance().font,
			Component.translatable("gui.iceandfire_fabric.pixie_bonds.need_pixie"), CARD_W - 8, 1F, 3);
		return layout.whole() && layout.scale() * layout.squeeze() >= least;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
