package com.iafabric.client.gui;

import com.iafabric.client.IafKeybinds;
import com.iafabric.entity.EntityExiledPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.message.MessageExiledAction;
import com.iafabric.message.MessageExiledSync;
import com.iafabric.network.IafNetwork;
import com.iafabric.pixie.ExiledPixieData;
import com.iafabric.pixie.ExiledPixies;
import com.iafabric.pixie.ExiledQuest;
import com.iafabric.pixie.ExiledVariants;
import com.iafabric.pixie.PixieElement;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * An exiled pixie's menu. Her picture, name, bond and the share of the pixies' gifts she gives; three tabs: talk (a
 * word from her, out or resting, how much she talks, whether she leads the way), quests (all of them: done, passed,
 * under way, offered, open or later on; one chosen is told and can be taken, put off or given up) and gifts (what she
 * gives now, and her own: a thousand years of memory). The first time, only her complaint and "I'll take care of
 * you". Drawn smaller to fit any screen; what the mouse is on is told in a strip at its foot.
 */
public class GuiExiledPixie extends Screen {
	private static final int W = 360;
	private static final int H = 214;
	private static final int RIGHT = 122;
	private static final int TOP = 26;
	private static final int STRIP = 186;
	private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
	private static final Identifier BUTTON_ON = Identifier.withDefaultNamespace("widget/button_highlighted");
	private static final Identifier BUTTON_OFF = Identifier.withDefaultNamespace("widget/button_disabled");
	private static final String[] TABS = {"talk", "quests", "gifts"};

	private int tab;
	private int chosen = -1;
	/** The first quest's row shown (the list scrolls with the wheel), and how many rows show at once. */
	private int scroll;
	private static final int ROWS = 10;
	private @Nullable EntityExiledPixie model;
	private final List<Btn> buttons = new ArrayList<>();

	/** A button of the menu (in its own measure), what it says, does and tells in the strip. */
	private record Btn(int x, int y, int w, int h, Component label, int action, int arg, @Nullable Component tip, boolean on) {
		boolean at(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	public GuiExiledPixie() {
		super(Component.translatable("gui.iceandfire_fabric.exiled.subtitle"));
		ExiledQuest current = data().current();
		ExiledQuest offered = data().offer();
		this.chosen = offered != null ? offered.ordinal() : current != null ? current.ordinal() : -1;
		reveal(this.chosen);
		if (offered != null) {
			this.tab = 1;
		}
	}

	private static ExiledPixieData data() {
		return MessageExiledSync.client;
	}

	private int accent() {
		return 0xFF000000 | ExiledVariants.DRESS[ExiledVariants.of(Math.max(0, data().variant))];
	}

	private float fit() {
		return Math.min(1F, Math.min((this.width - 6F) / W, (this.height - 6F) / H));
	}

	private int left() {
		return Math.round((this.width - W * fit()) / 2);
	}

	private int top() {
		return Math.round((this.height - H * fit()) / 2);
	}

	private double localX(double screenX) {
		return (screenX - left()) / fit();
	}

	private double localY(double screenY) {
		return (screenY - top()) / fit();
	}

	/** For the scenes: show a tab, choose a quest. */
	public void show(int tab, int quest) {
		this.tab = Mth.clamp(tab, 0, 2);
		this.chosen = quest;
		reveal(quest);
	}

	/** The list scrolled so that a quest's row shows. */
	private void reveal(int quest) {
		ExiledQuest[] quests = ExiledQuest.ordered();
		for (int i = 0; i < quests.length; i++) {
			if (quests[i].ordinal() == quest) {
				if (i < scroll) {
					scroll = i;
				} else if (i >= scroll + ROWS) {
					scroll = i - ROWS + 1;
				}
			}
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double x = localX(mouseX);
		double y = localY(mouseY);
		if (tab == 1 && x >= RIGHT && x < RIGHT + 122 && y >= TOP && y < TOP + 2 + ROWS * 15 && scrollY != 0) {
			scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, ExiledQuest.ordered().length - ROWS));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	/** For the scenes: where a place of the menu is on the screen (in the interface's own measure). */
	public double[] screenOf(double x, double y) {
		return new double[]{left() + x * fit(), top() + y * fit()};
	}

	/** For the scenes: the middle of the button that says this (null if none). */
	public double @Nullable [] button(String label) {
		for (Btn b : this.buttons) {
			if (b.label().getString().equals(label)) {
				return screenOf(b.x() + b.w() / 2.0, b.y() + b.h() / 2.0);
			}
		}
		return null;
	}

	private static void send(int action, int arg) {
		IafNetwork.sendToServer(new MessageExiledAction(action, arg));
	}

	private static void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			double x = localX(event.x());
			double y = localY(event.y());
			if (x >= W - 16 && x < W - 4 && y >= 4 && y < 16) {
				click();
				this.onClose();
				return true;
			}
			if (data().stage == ExiledPixieData.FRIEND) {
				for (int i = 0; i < TABS.length; i++) {
					if (x >= RIGHT + i * 74 && x < RIGHT + i * 74 + 70 && y >= 8 && y < 22 && tab != i) {
						tab = i;
						click();
						return true;
					}
				}
				if (tab == 1) {
					int row = questRow(x, y);
					if (row >= 0) {
						chosen = ExiledQuest.ordered()[row].ordinal();
						click();
						return true;
					}
				}
			}
			for (Btn b : this.buttons) {
				if (b.at(x, y) && b.on()) {
					click();
					send(b.action(), b.arg());
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (IafKeybinds.exiledPixie.matches(event)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(g, mouseX, mouseY, partialTicks);
		g.pose().pushMatrix();
		g.pose().translate(left(), top());
		g.pose().scale(fit(), fit());
		g.fill(0, 0, W, H, 0xEC0E0D14);
		g.outline(0, 0, W, H, accent());
		g.outline(1, 1, W - 2, H - 2, 0x30FFFFFF);
		// her window: a pale glade
		g.fillGradient(8, 8, 114, 128, 0xFF2A3A34, 0xFF101814);
		g.outline(8, 8, 106, 120, accent());
		g.pose().popMatrix();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(g, mouseX, mouseY, partialTicks);
		double mx = localX(mouseX);
		double my = localY(mouseY);
		portrait(g);
		g.pose().pushMatrix();
		g.pose().translate(left(), top());
		g.pose().scale(fit(), fit());
		this.buttons.clear();
		Component info = left(g, mx, my);
		Component right = data().stage == ExiledPixieData.FRIEND ? right(g, mx, my) : intro(g);
		if (right != null) {
			info = right;
		}
		for (Btn b : this.buttons) {
			boolean over = b.at(mx, my);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, !b.on() ? BUTTON_OFF : over ? BUTTON_ON : BUTTON, b.x(), b.y(), b.w(), b.h());
			IafGui.fitCentered(g, font, b.label().getString(), b.x() + b.w() / 2, b.y() + (b.h() - 8) / 2, b.w() - 6, b.on() ? 0xFFFFFFFF : 0xFFA0A0A0);
			if (over && b.tip() != null) {
				info = b.tip();
			}
		}
		boolean overClose = mx >= W - 16 && mx < W - 4 && my >= 4 && my < 16;
		g.centeredText(font, Component.literal("×"), W - 10, 5, overClose ? 0xFFFFFFFF : 0xFF9090A0);
		strip(g, info);
		g.pose().popMatrix();
	}

	// --- her side: picture, name, bond ---

	private @Nullable Component left(GuiGraphicsExtractor g, double mx, double my) {
		ExiledPixieData data = data();
		Component info = null;
		String name = ExiledVariants.name(Math.max(0, data.variant));
		g.centeredText(font, Component.literal(name).withStyle(ChatFormatting.BOLD), 61, 132, brighter(accent()));
		IafGui.fitCentered(g, font, this.title.getString(), 61, 143, 104, 0xFF9A98A8);
		if (data.stage == ExiledPixieData.FRIEND) {
			int need = data.level >= 5 ? 1 : ExiledPixieData.LEVEL_XP[data.level - 1];
			int from = data.level <= 1 ? 0 : ExiledPixieData.LEVEL_XP[data.level - 2];
			float progress = data.level >= 5 ? 1F : Mth.clamp((data.xp - from) / (float) Math.max(1, need - from), 0F, 1F);
			IafGui.fitCentered(g, font, Component.translatable("gui.iceandfire_fabric.exiled.bond", data.level).getString(), 61, 155, 104, 0xFFFFFFFF);
			g.fill(14, 165, 108, 169, 0xFF2A2A34);
			g.fill(14, 165, 14 + Math.round(94 * progress), 169, accent());
			small(g, Component.translatable("gui.iceandfire_fabric.exiled.share", Math.round(data.share() * 100)).getString(), 61, 172, 104, 0xFFC8C0E0, true);
			if (mx >= 8 && mx < 114 && my >= 152 && my < 182) {
				info = Component.translatable("gui.iceandfire_fabric.exiled.tip.bond", data.level >= 5 ? 0 : data.toNext());
			}
		}
		return info;
	}

	/** Her picture: whole, in her window, facing the player. */
	private void portrait(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return;
		}
		if (this.model == null || this.model.getVariant() != ExiledVariants.of(Math.max(0, data().variant))) {
			this.model = IafEntityRegistry.EXILED_PIXIE.create(mc.level, EntitySpawnReason.LOAD);
			if (this.model == null) {
				return;
			}
			this.model.setVariant(Math.max(0, data().variant));
		}
		this.model.tickCount = mc.player == null ? 0 : mc.player.tickCount;
		float fit = fit();
		int x1 = left() + Math.round(9 * fit);
		int y1 = top() + Math.round(9 * fit);
		int x2 = left() + Math.round(113 * fit);
		int y2 = top() + Math.round(127 * fit);
		int scale = Math.max(1, Math.round(118 * fit));
		InventoryScreen.extractEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, scale, 0.18F, (x1 + x2) / 2F, y1 + (y2 - y1) * 0.3F, this.model);
	}

	// --- the first time ---

	private @Nullable Component intro(GuiGraphicsExtractor g) {
		int y = 14;
		y = paragraph(g, Component.translatable("exiled.iceandfire_fabric.meet.2").withStyle(ChatFormatting.ITALIC), RIGHT, y, W - RIGHT - 10, 0xFFFFE0E8, 1F, 4);
		y += 8;
		y = paragraph(g, Component.translatable("gui.iceandfire_fabric.exiled.lore").withStyle(ChatFormatting.ITALIC), RIGHT, y, W - RIGHT - 10, 0xFFB8B0D0, 0.85F, 4);
		this.buttons.add(new Btn(RIGHT + 30, 130, W - RIGHT - 70, 20, Component.translatable("gui.iceandfire_fabric.exiled.adopt"), MessageExiledAction.ADOPT, 0,
			Component.translatable("gui.iceandfire_fabric.exiled.tip.adopt"), true));
		return null;
	}

	// --- the tabs ---

	private @Nullable Component right(GuiGraphicsExtractor g, double mx, double my) {
		for (int i = 0; i < TABS.length; i++) {
			int x = RIGHT + i * 74;
			boolean on = tab == i;
			g.fill(x, 8, x + 70, 22, on ? 0xFF2C2A3A : 0xFF16151E);
			g.outline(x, 8, 70, 14, on ? accent() : 0xFF404050);
			String label = Component.translatable("gui.iceandfire_fabric.exiled.tab." + TABS[i]).getString();
			if (i == 1 && data().offer() != null) {
				label = "★ " + label;
			}
			IafGui.fitCentered(g, font, label, x + 35, 11, 66, on ? 0xFFFFFFFF : 0xFF9898A8);
		}
		g.fill(RIGHT, TOP - 2, W - 8, TOP - 1, accent());
		return switch (tab) {
			case 1 -> quests(g, mx, my);
			case 2 -> gifts(g, mx, my);
			default -> talk(g);
		};
	}

	private @Nullable Component talk(GuiGraphicsExtractor g) {
		ExiledPixieData data = data();
		int y = paragraph(g, Component.translatable("gui.iceandfire_fabric.exiled.lore").withStyle(ChatFormatting.ITALIC), RIGHT + 2, TOP + 4, W - RIGHT - 12,
			0xFFB8B0D0, 0.85F, 3);
		IafGui.fit(g, font, Component.translatable(data.out ? "gui.iceandfire_fabric.exiled.out" : "gui.iceandfire_fabric.exiled.resting").getString(), RIGHT + 2, y + 4,
			W - RIGHT - 12, data.out ? 0xFF9FE39A : 0xFFB0B0B0);
		int bw = (W - RIGHT - 14) / 2;
		int x1 = RIGHT + 2;
		int x2 = RIGHT + 6 + bw;
		int by = 96;
		this.buttons.add(new Btn(x1, by, bw, 18, Component.translatable("gui.iceandfire_fabric.exiled.talk"), MessageExiledAction.TALK, 0,
			Component.translatable("gui.iceandfire_fabric.exiled.tip.talk"), data.out));
		this.buttons.add(data.out
			? new Btn(x2, by, bw, 18, Component.translatable("gui.iceandfire_fabric.exiled.rest"), MessageExiledAction.REST, 0, Component.translatable("gui.iceandfire_fabric.exiled.tip.rest"), true)
			: new Btn(x2, by, bw, 18, Component.translatable("gui.iceandfire_fabric.exiled.summon"), MessageExiledAction.SUMMON, 0, Component.translatable("gui.iceandfire_fabric.exiled.tip.summon"), true));
		this.buttons.add(new Btn(x1, by + 24, bw, 18, Component.translatable("gui.iceandfire_fabric.exiled.talk_mode",
			Component.translatable("gui.iceandfire_fabric.exiled.talk_mode." + data.talk)), MessageExiledAction.TALK_MODE, (data.talk + 1) % 3,
			Component.translatable("gui.iceandfire_fabric.exiled.tip.talk_mode"), true));
		this.buttons.add(new Btn(x2, by + 24, bw, 18, Component.translatable("gui.iceandfire_fabric.exiled.guide",
			Component.translatable(data.guide ? "gui.iceandfire_fabric.exiled.yes" : "gui.iceandfire_fabric.exiled.no")), MessageExiledAction.GUIDE, 0,
			Component.translatable("gui.iceandfire_fabric.exiled.tip.guide"), true));
		small(g, Component.translatable("gui.iceandfire_fabric.exiled.keys", IafKeybinds.exiledPixie.getTranslatedKeyMessage(),
			IafKeybinds.exiledMemory.getTranslatedKeyMessage()).getString(), RIGHT + 2, 150, W - RIGHT - 12, 0xFF7C7890, false);
		return null;
	}

	/** Which quest of the list is at a place of the menu (its place in the list; -1 none). */
	private int questRow(double x, double y) {
		if (x < RIGHT || x >= RIGHT + 118 || y < TOP + 2) {
			return -1;
		}
		int row = (int) ((y - TOP - 2) / 15);
		return row >= 0 && row < ROWS && row + scroll < ExiledQuest.ordered().length ? row + scroll : -1;
	}

	private static String state(ExiledPixieData data, ExiledQuest quest) {
		if (data.isDone(quest)) {
			return "done";
		}
		if (!quest.offered()) {
			return "waiting";
		}
		if (data.isSkipped(quest)) {
			return "passed";
		}
		if (data.quest == quest.ordinal()) {
			return "active";
		}
		if (data.offered == quest.ordinal()) {
			return "offered";
		}
		return quest.unlocked(data) ? "open" : "locked";
	}

	private static int stateColor(String state) {
		return switch (state) {
			case "done" -> 0xFF8FE08A;
			case "waiting" -> 0xFF9C8CC8;
			case "passed" -> 0xFF8A8A96;
			case "active" -> 0xFFFFFFFF;
			case "offered" -> 0xFFFFD86A;
			case "open" -> 0xFFC8C4D8;
			default -> 0xFF5E5C6C;
		};
	}

	/** A quest's name in the list (her last one has none till its moment). */
	private static Component title(ExiledPixieData data, ExiledQuest quest) {
		return !quest.offered() && !data.isDone(quest) ? Component.literal("???") : Component.translatable("exiled.iceandfire_fabric.quest." + quest.key());
	}

	private static String mark(String state) {
		return switch (state) {
			case "done" -> "✔";
			case "waiting" -> "✧";
			case "passed" -> "–";
			case "active" -> "▶";
			case "offered" -> "★";
			case "open" -> "•";
			default -> "🔒";
		};
	}

	private @Nullable Component quests(GuiGraphicsExtractor g, double mx, double my) {
		ExiledPixieData data = data();
		Component info = null;
		ExiledQuest[] quests = ExiledQuest.ordered();
		for (int i = scroll; i < Math.min(quests.length, scroll + ROWS); i++) {
			ExiledQuest quest = quests[i];
			String state = state(data, quest);
			int y = TOP + 2 + (i - scroll) * 15;
			boolean over = questRow(mx, my) == i;
			boolean picked = chosen == quest.ordinal();
			if (picked || over) {
				g.fill(RIGHT, y, RIGHT + 118, y + 14, picked ? 0x48000000 | (accent() & 0xFFFFFF) : 0x20FFFFFF);
			}
			int color = stateColor(state);
			String mark = mark(state);
			if (mark.equals("🔒")) {
				g.fill(RIGHT + 4, y + 6, RIGHT + 9, y + 11, color);
				g.fill(RIGHT + 5, y + 3, RIGHT + 8, y + 6, 0);
				g.outline(RIGHT + 5, y + 3, 3, 4, color);
			} else {
				IafGui.text(g, font, mark, RIGHT + 3, y + 3, color);
			}
			IafGui.fit(g, font, title(data, quest).getString(), RIGHT + 13, y + 3, 102, color);
			if (over) {
				info = Component.translatable("gui.iceandfire_fabric.exiled.quest." + state);
			}
		}
		if (quests.length > ROWS) {
			// the list's bar: where the rows shown are in it (the wheel scrolls it)
			int top = TOP + 2;
			int height = ROWS * 15 - 1;
			int thumb = Math.max(12, height * ROWS / quests.length);
			int at = top + (height - thumb) * scroll / Math.max(1, quests.length - ROWS);
			g.fill(RIGHT + 119, top, RIGHT + 121, top + height, 0x30FFFFFF);
			g.fill(RIGHT + 119, at, RIGHT + 121, at + thumb, accent());
		}
		if (info == null) {
			// her memories: how many are back (all of them: ready for Cryona)
			int over = 0;
			int all = 0;
			for (ExiledQuest quest : quests) {
				if (quest.offered()) {
					all++;
					over += data.isOver(quest) ? 1 : 0;
				}
			}
			info = Component.translatable(over == all ? "gui.iceandfire_fabric.exiled.memories_all" : "gui.iceandfire_fabric.exiled.memories", over, all);
		}
		// the chosen one, told
		int x = RIGHT + 124;
		int w = W - x - 8;
		ExiledQuest quest = ExiledQuest.of(chosen);
		g.fill(x - 3, TOP, x - 2, TOP + 138, 0xFF34323F);
		if (quest == null) {
			paragraph(g, Component.translatable("gui.iceandfire_fabric.exiled.pick"), x, TOP + 6, w, 0xFF9090A0, 0.85F, 4);
			return info;
		}
		String state = state(data, quest);
		int y = paragraph(g, title(data, quest).copy().withStyle(ChatFormatting.BOLD), x, TOP + 4, w,
			stateColor(state) | 0xFF000000, 1F, 2);
		y = paragraph(g, Component.translatable("gui.iceandfire_fabric.exiled.quest." + state), x, y + 1, w, 0xFF8C8AA0, 0.75F, 1);
		boolean locked = state.equals("locked");
		y = paragraph(g, Component.translatable(locked ? "gui.iceandfire_fabric.exiled.quest.locked_desc" : "exiled.iceandfire_fabric.quest." + quest.key() + ".desc"), x, y + 4, w,
			0xFFD8D4E8, 0.8F, 7);
		int by = TOP + 118;
		if (state.equals("active")) {
			this.buttons.add(new Btn(x, by, w, 18, Component.translatable("gui.iceandfire_fabric.exiled.drop"), MessageExiledAction.DROP, quest.ordinal(),
				Component.translatable("gui.iceandfire_fabric.exiled.tip.drop"), true));
			this.buttons.add(new Btn(x, by - 22, w, 18, Component.translatable("gui.iceandfire_fabric.exiled.guide",
				Component.translatable(data.guide ? "gui.iceandfire_fabric.exiled.yes" : "gui.iceandfire_fabric.exiled.no")), MessageExiledAction.GUIDE, 0,
				Component.translatable("gui.iceandfire_fabric.exiled.tip.guide"), true));
		} else if (state.equals("offered") || state.equals("open")) {
			this.buttons.add(new Btn(x, by - 22, w, 18, Component.translatable("gui.iceandfire_fabric.exiled.accept"), MessageExiledAction.ACCEPT, quest.ordinal(),
				Component.translatable(data.current() != null ? "gui.iceandfire_fabric.exiled.tip.accept_switch" : "gui.iceandfire_fabric.exiled.tip.accept"), true));
			if (state.equals("offered")) {
				this.buttons.add(new Btn(x, by, w, 18, Component.translatable("gui.iceandfire_fabric.exiled.later"), MessageExiledAction.LATER, quest.ordinal(),
					Component.translatable("gui.iceandfire_fabric.exiled.tip.later"), true));
			}
		}
		return info;
	}

	private @Nullable Component gifts(GuiGraphicsExtractor g, double mx, double my) {
		ExiledPixieData data = data();
		Component info = null;
		int y = paragraph(g, Component.translatable("gui.iceandfire_fabric.exiled.gifts.title", Math.round(data.share() * 100)), RIGHT + 2, TOP + 3, W - RIGHT - 12,
			0xFFFFFFFF, 0.85F, 2);
		int row = 0;
		for (PixieElement element : PixieElement.values()) {
			for (PixieElement.Bonus bonus : element.bonuses) {
				int col = row % 2;
				int x = RIGHT + 2 + col * 116;
				int ry = y + 3 + (row / 2) * 11;
				double value = bonus.atFive() * data.share();
				boolean percent = bonus.operation() != net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE;
				String amount = percent ? "+" + Math.round(value * 100) + "%"
					: "+" + new java.text.DecimalFormat("0.##", java.text.DecimalFormatSymbols.getInstance(java.util.Locale.ROOT)).format(value);
				int dot = 0xFF000000 | GuiPixieBondsColors.of(element.ordinal());
				g.fill(x, ry + 2, x + 4, ry + 6, dot);
				String stat = Component.translatable(bonus.attribute().value().getDescriptionId()).getString();
				small(g, amount + " " + stat, x + 7, ry, 106, data.out ? 0xFFE0DCF0 : 0xFF808090, false);
				if (mx >= x && mx < x + 114 && my >= ry - 1 && my < ry + 9) {
					info = Component.translatable("gui.iceandfire_fabric.exiled.tip.stat", Component.translatable("pixie.iceandfire_fabric.element." + element.ordinal()),
						Math.round(data.share() * 100));
				}
				row++;
			}
		}
		y = y + 3 + ((row + 1) / 2) * 11 + 4;
		small(g, Component.translatable(data.level >= 5 ? "gui.iceandfire_fabric.exiled.gifts.full" : "gui.iceandfire_fabric.exiled.gifts.grow").getString(), RIGHT + 2, y,
			W - RIGHT - 12, 0xFF8C8AA0, false);
		y += 12;
		g.fill(RIGHT, y, W - 8, y + 1, 0xFF34323F);
		y += 4;
		IafGui.fit(g, font, Component.translatable("gui.iceandfire_fabric.exiled.ability").getString(), RIGHT + 2, y, W - RIGHT - 12, brighter(accent()));
		y = paragraph(g, Component.translatable("gui.iceandfire_fabric.exiled.ability.desc", ExiledPixies.abilityRange(data.level)), RIGHT + 2, y + 11, W - RIGHT - 90,
			0xFFD8D4E8, 0.75F, 3);
		if (data.hasGift(ExiledPixieData.GIFT_ICE)) {
			// her last gift: the resistance to absolute ice
			small(g, "❄ " + Component.translatable("gui.iceandfire_fabric.exiled.gift_ice").getString(), RIGHT + 2, y + 3, W - RIGHT - 92, 0xFF9FE6FF, false);
			if (mx >= RIGHT && mx < W - 90 && my >= y + 1 && my < y + 12) {
				info = Component.translatable("gui.iceandfire_fabric.exiled.gift_ice.desc");
			}
		}
		Minecraft mc = Minecraft.getInstance();
		long wait = mc.level == null ? 0 : data.abilityReady - mc.level.getGameTime();
		boolean ready = wait <= 0 && data.out;
		Component label = wait > 0 ? Component.translatable("gui.iceandfire_fabric.exiled.ability.wait", (wait + 19) / 20 / 60 + ":" + String.format("%02d", (wait + 19) / 20 % 60))
			: Component.translatable("gui.iceandfire_fabric.exiled.ability.use");
		this.buttons.add(new Btn(W - 84, STRIP - 26, 74, 18, label, MessageExiledAction.ABILITY, 0,
			Component.translatable(data.out ? "gui.iceandfire_fabric.exiled.tip.ability" : "gui.iceandfire_fabric.exiled.tip.ability_resting",
				IafKeybinds.exiledMemory.getTranslatedKeyMessage()), ready));
		return info;
	}

	// --- text helpers ---

	/** Lines of text wrapped in a box (a width, some lines at a scale: IafGui.layout fits a longer one); where the next one goes. */
	private int paragraph(GuiGraphicsExtractor g, Component text, int x, int y, int width, int color, float scale, int maxLines) {
		return y + IafGui.paragraph(g, font, text, x, y, width, color, scale, maxLines, false, false);
	}

	/**
	 * For the tests: the texts of this menu that do not all go in their boxes (as it draws them) in the game's language
	 * now, or only with letters smaller or narrower than this part of their size.
	 */
	public static List<String> unfit(float least) {
		var font = net.minecraft.client.Minecraft.getInstance().font;
		List<String> out = new java.util.ArrayList<>();
		java.util.function.BiConsumer<Component, int[]> check = (text, box) -> {
			IafGui.Layout layout = IafGui.layout(font, text, box[0], box[1] / 100F, box[2]);
			if (!layout.whole() || layout.scale() * layout.squeeze() < box[1] / 100F * least) {
				out.add(text.getString());
			}
		};
		check.accept(Component.translatable("exiled.iceandfire_fabric.meet.2"), new int[]{W - RIGHT - 10, 100, 4});
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.lore"), new int[]{W - RIGHT - 10, 85, 4});
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.lore"), new int[]{W - RIGHT - 12, 85, 3});
		int w = W - (RIGHT + 124) - 8;
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.pick"), new int[]{w, 85, 4});
		for (ExiledQuest quest : ExiledQuest.values()) {
			check.accept(Component.translatable("exiled.iceandfire_fabric.quest." + quest.key()), new int[]{w, 100, 2});
			check.accept(Component.translatable("exiled.iceandfire_fabric.quest." + quest.key() + ".desc"), new int[]{w, 80, 7});
		}
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.quest.locked_desc"), new int[]{w, 80, 7});
		for (String state : List.of("done", "passed", "active", "offered", "open", "locked", "waiting")) {
			check.accept(Component.translatable("gui.iceandfire_fabric.exiled.quest." + state), new int[]{w, 75, 1});
		}
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.gifts.title", 50), new int[]{W - RIGHT - 12, 85, 2});
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.ability.desc", 64), new int[]{W - RIGHT - 90, 75, 3});
		// the strip at the bottom: what the mouse is on
		int[] strip = {W - 24, 85, 2};
		for (String key : List.of("hint", "tip.bond", "tip.adopt", "tip.talk", "tip.summon", "tip.rest", "tip.talk_mode", "tip.guide",
			"tip.accept", "tip.accept_switch", "tip.later", "tip.drop", "tip.ability", "tip.ability_resting", "gift_ice.desc")) {
			check.accept(Component.translatable("gui.iceandfire_fabric.exiled." + key, 99, "K"), strip);
		}
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.tip.stat", Component.translatable("pixie.iceandfire_fabric.element.0"), 50), strip);
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.memories", 14, 14), strip);
		check.accept(Component.translatable("gui.iceandfire_fabric.exiled.memories_all", 14, 14), strip);
		return out;
	}

	/** Small text (three quarters) fitted to a width, centered or not. */
	private void small(GuiGraphicsExtractor g, String text, int x, int y, int width, int color, boolean centered) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(0.75F, 0.75F);
		int room = (int) (width / 0.75F);
		if (centered) {
			IafGui.fitCentered(g, font, text, 0, 0, room, color);
		} else {
			IafGui.fit(g, font, text, 0, 0, room, color);
		}
		g.pose().popMatrix();
	}

	private void strip(GuiGraphicsExtractor g, @Nullable Component info) {
		int y = STRIP + 6;
		g.fill(8, y, W - 8, y + 20, 0x50000000);
		Component text = info != null ? info : Component.translatable("gui.iceandfire_fabric.exiled.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
		paragraph(g, text, 12, y + 2, W - 24, info != null ? 0xFFE8ECF4 : 0xFF70788A, 0.85F, 2);
	}

	private static int brighter(int color) {
		int r = Math.min(255, ((color >> 16) & 255) + 30);
		int gr = Math.min(255, ((color >> 8) & 255) + 30);
		int b = Math.min(255, (color & 255) + 30);
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** The pixies' colors, for the marks of their gifts. */
	private static final class GuiPixieBondsColors {
		private static final int[] COLORS = {0xF7A8C8, 0xC9A0F5, 0x8FD3F5, 0x9FE39A, 0xF9CF6E};

		static int of(int color) {
			return COLORS[Math.floorMod(color, COLORS.length)];
		}
	}
}
