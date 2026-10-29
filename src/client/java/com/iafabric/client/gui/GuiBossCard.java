package com.iafabric.client.gui;

import com.iafabric.entity.EntityDragonBase;
import com.iafabric.item.IafItemRegistry;
import com.iafabric.message.MessageBossCard;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The altar's card of Cryona's duel, the way games show a boss before its fight: a tab for each of its two parts
 * (Black Frost with her on its back, then Cryona alone), each with the boss drawn whole and facing the player, its life
 * (and her ice shield), and its powers as icons, each with its name and how to meet it. Under them the duel's rules as
 * four marks, a strip where what the mouse is on is told in full (no box floats over the card), what it asks (one ice
 * dragon blood, how many the player has) and its buttons: fight, go home, think it over. The whole card is made smaller
 * to fit a small screen (or a big interface scale), never cut nor run over.
 */
public class GuiBossCard extends Screen {
	private static final int W = 404;
	private static final int H = 242;
	private static final int FROST = 0xFF8FD3FF;
	private static final int QUEEN = 0xFFC9A0FF;
	private static final int DEADLY = 0xFFFF5A4A;
	private static final ResourceLocation BUTTON = ResourceLocation.withDefaultNamespace("widget/button");
	private static final ResourceLocation BUTTON_ON = ResourceLocation.withDefaultNamespace("widget/button_highlighted");
	private static final ResourceLocation BUTTON_OFF = ResourceLocation.withDefaultNamespace("widget/button_disabled");
	/** The two parts' powers: icon, name (gui.iceandfire_fabric.boss.KEY, .hint, .tip), whether it takes nine tenths of a life. */
	private static final Power[] FROST_POWERS = {
		new Power(IafItemRegistry.HIPPOGRYPH_TALON, "grab", false),
		new Power(Items.POWDER_SNOW_BUCKET, "aura", false),
		new Power(Items.ICE, "volley", false),
		new Power(Items.MACE, "dive", true),
		new Power(Items.GOAT_HORN, "stampede", false),
		new Power(IafItemRegistry.DRAGON_BONE, "melee", false),
		new Power(Items.PACKED_ICE, "hail", false),
		new Power(Items.BLUE_ICE, "prison", false),
		new Power(IafItemRegistry.ICE_DRAGON_HEART, "breath", false),
		new Power(Items.NETHER_STAR, "zero_frost", true)};
	private static final Power[] QUEEN_POWERS = {
		new Power(IafItemRegistry.DREAD_QUEEN_SWORD, "cuts", false),
		new Power(Items.RABBIT_FOOT, "leap", false),
		new Power(Items.POINTED_DRIPSTONE, "lanes", false),
		new Power(Items.SNOWBALL, "burst", false),
		new Power(Items.ENDER_PEARL, "blink", false),
		new Power(Items.END_ROD, "beam", true),
		new Power(Items.SNOW_BLOCK, "skyfall", false),
		new Power(Items.SKELETON_SKULL, "dead", false),
		new Power(Items.SHIELD, "shield_power", false),
		new Power(IafItemRegistry.DREAD_QUEEN_STAFF, "zero_queen", true)};
	/** The duel's rules: name (gui.iceandfire_fabric.boss.rule.KEY, .tip) and color. */
	private static final String[] RULES = {"deadly", "fall", "limit", "stunned"};
	private static final int[] RULE_COLORS = {DEADLY, 0xFF7CE07C, 0xFFB0B0B0, 0xFFFFD25A};
	private static final ItemLike BLOOD = IafItemRegistry.ICE_DRAGON_BLOOD;
	/** Its buttons (card's own place, x, width): fight, go home, think it over. */
	private static final int BUTTONS_Y = 223;
	private static final int BUTTON_H = 16;
	private static final int[][] BUTTONS = {{168, 74}, {246, 74}, {324, 72}};

	private final MessageBossCard card;
	/** The tab shown: 0 Black Frost, 1 Cryona. */
	private int tab;

	public GuiBossCard(MessageBossCard card) {
		super(Component.translatable("iceandfire_fabric.altar.title"));
		this.card = card;
		this.tab = card.frostStands() ? 0 : 1;
	}

	private record Power(ItemLike icon, String key, boolean deadly) {
	}

	/** How much smaller the card is drawn, to fit the screen (1: as made). */
	private float fit() {
		return Math.min(1F, Math.min((this.width - 6F) / W, (this.height - 6F) / H));
	}

	private int left() {
		return Math.round((this.width - W * fit()) / 2);
	}

	private int top() {
		return Math.round((this.height - H * fit()) / 2);
	}

	/** A place of the screen in the card's own (as it was made, before it is made smaller). */
	private double localX(double screenX) {
		return (screenX - left()) / fit();
	}

	private double localY(double screenY) {
		return (screenY - top()) / fit();
	}

	private boolean canPay() {
		return card.free || card.blood > 0;
	}

	private void choose(String command) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.connection.sendCommand(command);
		}
		this.onClose();
	}

	/** Which tab is at a place of the card (-1 none). */
	private static int tabAt(double x, double y) {
		if (y < 28 || y >= 41) {
			return -1;
		}
		for (int i = 0; i < 2; i++) {
			if (x >= 8 + i * 124 && x < 8 + i * 124 + 120) {
				return i;
			}
		}
		return -1;
	}

	/** Which button is at a place of the card (-1 none). */
	private static int buttonAt(double x, double y) {
		if (y < BUTTONS_Y || y >= BUTTONS_Y + BUTTON_H) {
			return -1;
		}
		for (int i = 0; i < BUTTONS.length; i++) {
			if (x >= BUTTONS[i][0] && x < BUTTONS[i][0] + BUTTONS[i][1]) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		if (mouseButton == InputConstants.MOUSE_BUTTON_LEFT) {
			double x = localX(mouseX);
			double y = localY(mouseY);
			int at = tabAt(x, y);
			if (at >= 0 && at != tab) {
				tab = at;
				click();
				return true;
			}
			int button = buttonAt(x, y);
			if (button == 0 && canPay()) {
				click();
				choose("iceandfire duel accept");
				return true;
			}
			if (button == 1) {
				click();
				choose("iceandfire duel leave");
				return true;
			}
			if (button == 2) {
				click();
				this.onClose();
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	private static void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
	}

	/** For the scenes: show a tab. */
	public void show(int tab) {
		this.tab = Mth.clamp(tab, 0, 1);
	}

	/** For the scenes: where a place of the card is on the screen (in the interface's own measure). */
	public double[] screenOf(double x, double y) {
		return new double[]{left() + x * fit(), top() + y * fit()};
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(g, mouseX, mouseY, partialTicks);
		int color = tab == 0 ? FROST : QUEEN;
		g.pose().pushPose();
		g.pose().translate(left(), top(), 0.0F);
		g.pose().scale(fit(), fit(), 1.0F);
		g.fill(0, 0, W, H, 0xE80C1018);
		g.renderOutline(0, 0, W, H, color);
		g.renderOutline(1, 1, W - 2, H - 2, 0x40FFFFFF);
		// the boss's portrait: a cold dark window
		g.fillGradient(8, 45, 136, 177, 0xFF141E33, 0xFF06080E);
		g.renderOutline(8, 45, 128, 132, color);
		g.pose().popPose();
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		super.render(g, mouseX, mouseY, partialTicks);
		double mx = localX(mouseX);
		double my = localY(mouseY);
		// (the portrait is drawn in the screen's own measure: what draws a creature in a box keeps to it)
		portrait(g);
		g.pose().pushPose();
		g.pose().translate(left(), top(), 0.0F);
		g.pose().scale(fit(), fit(), 1.0F);
		Component info = card(g, mx, my);
		infoStrip(g, info);
		g.pose().popPose();
	}

	/** All the card but its portrait and its strip; what the mouse is on, told in full. */
	private @Nullable Component card(GuiGraphics g, double mx, double my) {
		Component info = null;
		int color = tab == 0 ? FROST : QUEEN;
		g.drawCenteredString(font, this.title.copy().withStyle(ChatFormatting.BOLD), W / 2, 5, 0xFFD7B4FF);
		Component intro = Component.translatable(card.resumed ? "iceandfire_fabric.altar.intro_again" : "gui.iceandfire_fabric.boss.intro");
		IafGui.fitCentered(g, font, intro.getString(), W / 2, 16, W - 16, card.resumed ? 0xFF8FE3FF : 0xFFA8B0C0);
		// the two parts (named as each language names them)
		Component[] names = {Component.translatable("gui.iceandfire_fabric.boss.frost_name"), Component.translatable("gui.iceandfire_fabric.boss.queen_name")};
		String[] marks = {"I · ", "II · "};
		int hover = tabAt(mx, my);
		for (int i = 0; i < 2; i++) {
			int tx = 8 + i * 124;
			int tint = i == 0 ? FROST : QUEEN;
			boolean on = i == tab;
			g.fill(tx, 28, tx + 120, 41, on ? (tint & 0x00FFFFFF) | 0x60000000 : hover == i ? 0x40FFFFFF : 0x30000000);
			g.renderOutline(tx, 28, 120, 13, on ? tint : 0x80808080);
			IafGui.fitCentered(g, font, marks[i] + names[i].getString(), tx + 60, 31, 114, on ? 0xFFFFFFFF : 0xFF9098A8);
		}
		if (hover >= 0 && hover != tab) {
			info = Component.translatable("gui.iceandfire_fabric.boss.tab_hint");
		}
		// its name, what it is, its life
		int rx = 144;
		boolean frost = tab == 0;
		g.drawString(font, names[frost ? 0 : 1].copy().withStyle(ChatFormatting.BOLD), rx, 46, color);
		IafGui.fit(g, font, Component.translatable(frost ? "gui.iceandfire_fabric.boss.frost_what" : "gui.iceandfire_fabric.boss.queen_what").getString(), rx, 57, 252, 0xFFA8B0C0);
		float life = frost ? card.frostLife : card.queenLife;
		float max = frost ? card.frostMax : card.queenMax;
		boolean down = frost ? !card.frostStands() : card.queenId < 0;
		bar(g, rx, 67, 252, 10, down ? 0 : life / Math.max(1F, max), frost ? 0xFF3A8CFF : 0xFF6A3AC0, frost ? 0xFFA8E4FF : 0xFFD8B8FF,
			down ? Component.translatable("gui.iceandfire_fabric.boss.down").getString() : Math.round(life) + " / " + Math.round(max));
		if (!frost && card.shieldMax > 0) {
			bar(g, rx, 79, 252, 7, card.shield / card.shieldMax, 0xFF2AA8C8, 0xFF9FF0FF, null);
			String shield = card.shield > 0 ? Component.translatable("gui.iceandfire_fabric.boss.shield", Math.round(card.shield), Math.round(card.shieldMax)).getString()
				: Component.translatable("gui.iceandfire_fabric.boss.shield_later", Math.round(card.shieldMax)).getString();
			small(g, shield, rx + 3, 80, 246, card.shield / card.shieldMax > 0.35F ? 0xFF0A2030 : 0xFFB8F0FF);
		}
		g.fill(rx, 89, rx + 252, 90, (color & 0x00FFFFFF) | 0x80000000);
		// its powers
		Power[] powers = frost ? FROST_POWERS : QUEEN_POWERS;
		for (int i = 0; i < powers.length; i++) {
			Power power = powers[i];
			int px = rx + (i % 2) * 128;
			int py = 92 + (i / 2) * 17;
			boolean on = mx >= px - 1 && mx < px + 125 && my >= py - 1 && my < py + 16;
			if (on) {
				g.fill(px - 2, py - 1, px + 125, py + 16, 0x30FFFFFF);
				info = Component.translatable("gui.iceandfire_fabric.boss." + power.key).withStyle(power.deadly ? ChatFormatting.RED : ChatFormatting.WHITE)
					.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
					.append(Component.translatable("gui.iceandfire_fabric.boss." + power.key + ".tip").withStyle(ChatFormatting.GRAY));
			}
			if (power.deadly) {
				g.fill(px - 1, py - 1, px + 17, py + 17, 0x80FF2A1A);
			}
			g.renderItem(new ItemStack(power.icon), px, py);
			IafGui.fit(g, font, Component.translatable("gui.iceandfire_fabric.boss." + power.key).getString(), px + 19, py, 104, power.deadly ? DEADLY : 0xFFFFFFFF);
			small(g, Component.translatable("gui.iceandfire_fabric.boss." + power.key + ".hint").getString(), px + 19, py + 9, 104, 0xFF9AA4B8);
		}
		// the rules, as marks
		for (int i = 0; i < RULES.length; i++) {
			int cx = 8 + i * 98;
			int cy = 181;
			boolean on = mx >= cx && mx < cx + 94 && my >= cy && my < cy + 14;
			g.fill(cx, cy, cx + 94, cy + 14, on ? 0x40FFFFFF : 0x60000000);
			g.fill(cx, cy, cx + 3, cy + 14, RULE_COLORS[i]);
			IafGui.fit(g, font, Component.translatable("gui.iceandfire_fabric.boss.rule." + RULES[i]).getString(), cx + 6, cy + 3, 85, RULE_COLORS[i]);
			if (on) {
				info = Component.translatable("gui.iceandfire_fabric.boss.rule." + RULES[i] + ".tip");
			}
		}
		// what it asks
		g.renderItem(new ItemStack(BLOOD), 10, BUTTONS_Y);
		String cost = Component.translatable(card.free ? "gui.iceandfire_fabric.boss.cost_free" : "gui.iceandfire_fabric.boss.cost").getString();
		IafGui.fit(g, font, cost, 29, BUTTONS_Y, 134, 0xFFFFFFFF);
		if (!card.free) {
			small(g, Component.translatable("gui.iceandfire_fabric.boss.have", card.blood).getString(), 29, BUTTONS_Y + 10, 134, card.blood > 0 ? 0xFF7CE07C : DEADLY);
		}
		if (mx >= 8 && mx < 160 && my >= BUTTONS_Y - 1 && my < BUTTONS_Y + 17) {
			info = new ItemStack(BLOOD).getHoverName().copy().append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.translatable("iceandfire_fabric.altar.fight_tip").withStyle(ChatFormatting.GRAY));
		}
		// its buttons
		int pressed = buttonAt(mx, my);
		Component[] labels = {Component.translatable("gui.iceandfire_fabric.boss.fight").withStyle(ChatFormatting.LIGHT_PURPLE),
			Component.translatable("gui.iceandfire_fabric.boss.home").withStyle(ChatFormatting.AQUA), Component.translatable("iceandfire_fabric.altar.wait")};
		for (int i = 0; i < BUTTONS.length; i++) {
			boolean active = i != 0 || canPay();
			ResourceLocation sprite = !active ? BUTTON_OFF : pressed == i ? BUTTON_ON : BUTTON;
			g.blitSprite(sprite, BUTTONS[i][0], BUTTONS_Y, BUTTONS[i][1], BUTTON_H);
			IafGui.fitCentered(g, font, labels[i].getString(), BUTTONS[i][0] + BUTTONS[i][1] / 2, BUTTONS_Y + 4, BUTTONS[i][1] - 6,
				!active ? 0xFF808080 : i == 0 ? 0xFFE79BFF : i == 1 ? 0xFF8FE3FF : 0xFFFFFFFF);
		}
		if (pressed == 0) {
			info = Component.translatable(canPay() ? "iceandfire_fabric.altar.fight_tip" : "iceandfire_fabric.throne.need_blood");
		} else if (pressed == 1) {
			info = Component.translatable("iceandfire_fabric.altar.home_tip");
		}
		return info;
	}

	/** The strip where what the mouse is on is told in full, two lines at most (a hint when it is on nothing). */
	private void infoStrip(GuiGraphics g, @Nullable Component info) {
		int y = 198;
		g.fill(8, y, W - 8, y + 21, 0x50000000);
		Component text = info != null ? info : Component.translatable("gui.iceandfire_fabric.boss.info_hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
		IafGui.paragraph(g, font, text, 12, y + 2, W - 24, info != null ? 0xFFE8ECF4 : 0xFF70788A, 0.85F, 2, false, false);
	}

	/**
	 * For the tests: the texts of the card's strip (each power's and rule's in full, its hints) that do not all go in it
	 * now, or only with letters smaller or narrower than this part of their size.
	 */
	public static List<String> unfit(float least) {
		var font = Minecraft.getInstance().font;
		List<String> keys = new java.util.ArrayList<>(List.of("gui.iceandfire_fabric.boss.info_hint", "gui.iceandfire_fabric.boss.tab_hint"));
		for (Power[] powers : new Power[][]{FROST_POWERS, QUEEN_POWERS}) {
			for (Power power : powers) {
				keys.add("gui.iceandfire_fabric.boss." + power.key() + ".tip");
			}
		}
		for (String rule : RULES) {
			keys.add("gui.iceandfire_fabric.boss.rule." + rule + ".tip");
		}
		List<String> out = new java.util.ArrayList<>();
		for (String key : keys) {
			Component text = Component.translatable(key);
			IafGui.Layout layout = IafGui.layout(font, text, W - 24, 0.85F, 2);
			if (!layout.whole() || layout.scale() * layout.squeeze() < 0.85F * least) {
				out.add(text.getString());
			}
		}
		return out;
	}

	/** The boss of the tab drawn whole in its window, turned a little but facing the player, its middle in the middle. */
	private void portrait(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		float fit = fit();
		int bx1 = left() + Math.round(9 * fit);
		int by1 = top() + Math.round(46 * fit);
		int bx2 = left() + Math.round(135 * fit);
		int by2 = top() + Math.round(176 * fit);
		Entity entity = mc.level == null ? null : mc.level.getEntity(tab == 0 ? card.frostId : card.queenId);
		boolean down = tab == 0 ? !card.frostStands() : card.queenId < 0;
		if (!(entity instanceof LivingEntity living) || down || entity instanceof EntityDragonBase dragon && dragon.isModelDead()) {
			g.pose().pushPose();
			g.pose().translate((bx1 + bx2) / 2F, (by1 + by2) / 2F - 4 * fit, 0.0F);
			g.pose().scale(fit, fit, 1.0F);
			IafGui.fitCentered(g, font, Component.translatable(down ? "gui.iceandfire_fabric.boss.down" : "gui.iceandfire_fabric.boss.far").getString(), 0, 0, 120, 0xFF808898);
			g.pose().popPose();
			return;
		}
		// (pixels a block: a dragon whole in its window with its wings and tail, a rider or a knight standing tall)
		boolean dragon = living instanceof EntityDragonBase;
		float perBlock = dragon
			? Math.min(52F / Math.max(1F, living.getBbHeight()), 118F / Math.max(1F, living.getBbWidth() * 2.8F))
			: 118F / Math.max(1F, living.getBbHeight() * 1.25F);
		perBlock = Mth.clamp(perBlock, 2F, 60F) * fit;
		int scale = Math.max(1, Math.round(perBlock * living.getScale()));
		// a dragon is drawn higher than its box (its wings rise over its back): it sits a fifth of the window lower
		float lower = dragon ? (by2 - by1) * 0.2F / Math.max(0.5F, perBlock) : 0.0625F;
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, bx1, by1, bx2, by2, scale, lower, (bx1 + bx2) / 2F - 55 * fit, (by1 + by2) / 2F - 6 * fit,
			living);
	}

	/** A bar of life or shield, filled to a share, with its numbers over it. */
	private void bar(GuiGraphics g, int x, int y, int w, int h, float share, int from, int to, @Nullable String text) {
		share = Mth.clamp(share, 0F, 1F);
		g.fill(x, y, x + w, y + h, 0xFF1A1E28);
		int filled = Math.round((w - 2) * share);
		if (filled > 0) {
			g.fillGradient(x + 1, y + 1, x + 1 + filled, y + h - 1, to, from);
		}
		g.renderOutline(x, y, w, h, 0xFF000000);
		if (text != null) {
			IafGui.fitCentered(g, font, text, x + w / 2, y + (h - 8) / 2, w - 4, 0xFFFFFFFF);
		}
	}

	/** Small text (seven tenths), for the lines under the powers' names. */
	/** A line at seven tenths, narrowed to a width if it is longer (a longer translation): never cut. */
	private void small(GuiGraphics g, String text, int x, int y, int width, int color) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale(0.7F, 0.7F, 1.0F);
		IafGui.fit(g, font, text, 0, 0, (int) (width / 0.7F), color);
		g.pose().popPose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
