package com.iafabric.gamita;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The welcome, over the title screen's panorama: GAMITA_MODELS and thanks; a box with the GAMITA_MODELS mods in this
 * game (their icons turning like coins, one at a time named and told about, in its own words from its
 * fabric.mod.json); a big "Satisfied!" that goes on to the game; smaller Discord (ideas, bugs) and Patreon buttons
 * that open the browser; what Patreon gives. Laid out from the screen's size, so it fits any GUI scale (on a small
 * screen the lesser lines go first).
 */
public class GamitaWelcomeScreen extends Screen {
	private static final int GOLD = 0xFFE8B84A;
	private static final int GOLD_DARK = 0xFF7A5A1C;
	private static final PanoramaRenderer PANORAMA = new PanoramaRenderer(TitleScreen.CUBE_MAP);
	private final Screen next;
	private final String myModId;
	private final List<Icon> icons = new ArrayList<>();
	private long opened;
	/** The mod named by the arrows (or the first) and since when: the turn goes on from it, a while later. */
	private int pick;
	private long pickedAt;
	/** Where the row of icons is (an index, sliding toward the one named) and when it was last drawn. */
	private float slide;
	private long lastFrame;
	/** The one named, in the middle; the ones next to it; how far apart their middles are. */
	private static final int BIG = 36, SMALL = 26, NEXT = 40, FAR = 30;
	// the layout (from the screen's size)
	private int panelX, panelY, panelW, panelH;
	private int boxY, boxH;
	private int aboutLines;
	private int perksY;
	private List<FormattedCharSequence> perks = List.of();

	private record Icon(String name, ModContainer mod, ResourceLocation texture, int w, int h) {
	}

	GamitaWelcomeScreen(Screen next, String myModId, List<ModContainer> mods) {
		super(Component.literal("GAMITA_MODELS"));
		this.next = next;
		this.myModId = myModId;
		for (ModContainer mod : mods) {
			this.icons.add(icon(mod));
		}
	}

	private Icon icon(ModContainer mod) {
		String name = mod.getMetadata().getName();
		try {
			var path = mod.getMetadata().getIconPath(64).flatMap(mod::findPath);
			if (path.isPresent()) {
				try (InputStream in = Files.newInputStream(path.get())) {
					NativeImage image = NativeImage.read(in);
					ResourceLocation id = new ResourceLocation(this.myModId, "gamita/icon_" + mod.getMetadata().getId());
					GamitaCollection.mc().getTextureManager().register(id, new DynamicTexture(image));
					return new Icon(name, mod, id, image.getWidth(), image.getHeight());
				}
			}
		} catch (Exception ignored) {
			// (no icon: its first letter)
		}
		return new Icon(name, mod, null, 0, 0);
	}

	private static boolean port(ModContainer mod) {
		try {
			return "port".equals(mod.getMetadata().getCustomValue("gamita_models").getAsObject().get("kind").getAsString());
		} catch (RuntimeException e) {
			return false;
		}
	}

	/** The mod's own line ("about" in its gamita_models, by language; English, then its description, when none). */
	private static String about(ModContainer mod) {
		try {
			CustomValue.CvObject about = mod.getMetadata().getCustomValue("gamita_models").getAsObject().get("about").getAsObject();
			String code = GamitaCollection.mc().getLanguageManager().getSelected();
			for (String key : new String[]{code, code.length() >= 2 ? code.substring(0, 2) : code, "en"}) {
				if (about.containsKey(key)) {
					return about.get(key).getAsString();
				}
			}
		} catch (RuntimeException ignored) {
			// (none: its description)
		}
		return mod.getMetadata().getDescription();
	}

	@Override
	protected void init() {
		this.opened = net.minecraft.Util.getMillis();
		if (this.pickedAt == 0) {
			this.pickedAt = this.opened;
		}
		this.panelW = Math.min(380, this.width - 16);
		int inner = this.panelW - 24;
		this.perks = this.font.split(Component.literal(GamitaTexts.get(GamitaTexts.PERKS)), inner);
		this.aboutLines = 3;
		int perksH = this.perks.size() * 10 + 6;
		// title 22, thanks 12, gap 10, box, gap 10, big 30, gap 8, small 20, perks, padding
		int fixed = 10 + 22 + 12 + 10 + 10 + 30 + 8 + 20 + 8;
		this.boxH = 36 + BIG + this.aboutLines * 10 + (this.icons.size() == 1 ? 12 : 0);
		if (fixed + this.boxH + perksH > this.height - 8) {
			perksH = 0;
			this.perks = List.of();
		}
		while (fixed + this.boxH + perksH > this.height - 8 && this.aboutLines > 0) {
			this.aboutLines--;
			this.boxH -= 10;
		}
		this.panelH = fixed + this.boxH + perksH;
		this.panelX = this.width / 2 - this.panelW / 2;
		this.panelY = Math.max(4, (this.height - this.panelH) / 2);
		this.boxY = this.panelY + 10 + 22 + 12 + 10;
		int arrowY = this.boxY + 18 + BIG / 2 - 9;
		if (this.icons.size() > 1) {
		this.addRenderableWidget(new Fancy(this.panelX + 16, arrowY, 18, 18, "◀", 0xFF6A5A2A, false, () -> this.step(-1)));
		this.addRenderableWidget(new Fancy(this.panelX + this.panelW - 34, arrowY, 18, 18, "▶", 0xFF6A5A2A, false, () -> this.step(1)));
		}
		int y = this.boxY + this.boxH + 10;
		int bigW = Math.min(220, inner);
		this.addRenderableWidget(new Fancy(this.width / 2 - bigW / 2, y, bigW, 30, "✔ " + GamitaTexts.get(GamitaTexts.SATISFIED),
			0xFF2E9E4F, true, this::onClose));
		y += 38;
		int smallW = (inner - 8) / 2;
		this.addRenderableWidget(new Fancy(this.width / 2 - smallW - 4, y, smallW, 20, GamitaTexts.get(GamitaTexts.DISCORD),
			0xFF5865F2, false, () -> GamitaCollection.open(GamitaCollection.DISCORD)));
		this.addRenderableWidget(new Fancy(this.width / 2 + 4, y, smallW, 20, "❤ " + GamitaTexts.get(GamitaTexts.PATREON),
			0xFFF96854, false, () -> GamitaCollection.open(GamitaCollection.PATREON)));
		this.perksY = y + 26;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
		PANORAMA.render(partial, 1F);
		g.fillGradient(0, 0, this.width, this.height, 0x90000000, 0xC0000000);
		float t = (net.minecraft.Util.getMillis() - this.opened) / 1000F;
		int cx = this.width / 2;
		this.panel(g, t);
		// the name, big, gold, outlined dark, its gold breathing a little
		float pulse = 0.5F + 0.5F * Mth.sin(t * 2F);
		int nameW = this.font.width("GAMITA_MODELS") * 2;
		g.pose().pushPose();
		g.pose().translate(cx, this.panelY + 11, 0);
		g.pose().scale(2F, 2F, 1F);
		int w = this.font.width("GAMITA_MODELS");
		for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
			g.drawString(this.font, "GAMITA_MODELS", -w / 2 + d[0], d[1], 0xFF1A1206, false);
		}
		int gold = 0xFF000000 | (int) Mth.lerp(pulse, 0xD8, 0xFF) << 16 | (int) Mth.lerp(pulse, 0xA8, 0xD0) << 8 | 0x4A;
		g.drawString(this.font, "GAMITA_MODELS", -w / 2, 0, gold, false);
		g.pose().popPose();
		this.star(g, cx - nameW / 2 - 14, this.panelY + 19, t);
		this.star(g, cx + nameW / 2 + 14, this.panelY + 19, t + 1.5F);
		g.drawCenteredString(this.font, GamitaTexts.get(GamitaTexts.THANKS), cx, this.panelY + 34, 0xFFF2E6C8);
		this.collection(g, t);
		super.render(g, mouseX, mouseY, partial);
		int y = this.perksY;
		for (var line : this.perks) {
			g.drawCenteredString(this.font, line, cx, y, 0xFFB8AFA0);
			y += 10;
		}
	}

	/** A dark panel, a little lighter at the top, with a double gold border and small gold corners. */
	private void panel(GuiGraphics g, float t) {
		int x0 = this.panelX, y0 = this.panelY, x1 = x0 + this.panelW, y1 = y0 + this.panelH;
		g.fillGradient(x0, y0, x1, y1, 0xF0241A30, 0xF00C0A12);
		g.renderOutline(x0, y0, this.panelW, this.panelH, GOLD);
		g.renderOutline(x0 + 2, y0 + 2, this.panelW - 4, this.panelH - 4, GOLD_DARK);
		for (int[] c : new int[][]{{x0, y0}, {x1 - 5, y0}, {x0, y1 - 5}, {x1 - 5, y1 - 5}}) {
			g.fill(c[0], c[1], c[0] + 5, c[1] + 5, GOLD);
			g.fill(c[0] + 1, c[1] + 1, c[0] + 4, c[1] + 4, 0xFF2A1F12);
		}
		// a thin gold line under the thanks, fading to the sides
		int ly = y0 + 47, half = this.panelW / 3;
		for (int i = 0; i < half; i += 2) {
			int a = (int) (0xC0 * (1F - i / (float) half));
			g.fill(this.width / 2 - i - 2, ly, this.width / 2 - i, ly + 1, a << 24 | 0xE8B84A);
			g.fill(this.width / 2 + i, ly, this.width / 2 + i + 2, ly + 1, a << 24 | 0xE8B84A);
		}
	}

	/** A small four-pointed star, turning and shining. */
	private void star(GuiGraphics g, int x, int y, float t) {
		float s = 0.8F + 0.2F * Mth.sin(t * 3F);
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().mulPose(com.mojang.math.Axis.ZP.rotation(t));
		g.pose().scale(s, s, 1F);
		g.fill(-1, -5, 1, 5, GOLD);
		g.fill(-5, -1, 5, 1, GOLD);
		g.fill(-1, -1, 1, 1, 0xFFFFFFFF);
		g.pose().popPose();
	}

	/**
	 * The box of the mods: the one named big in the middle, the others smaller to its sides (the farther,
	 * the smaller and darker), each in the same frame; the row slides from one to the next. Its name in
	 * gold and its own words under them.
	 */
	private void collection(GuiGraphics g, float t) {
		int x0 = this.panelX + 10, x1 = this.panelX + this.panelW - 10;
		g.fillGradient(x0, this.boxY, x1, this.boxY + this.boxH, 0xC0101018, 0xC0181424);
		g.renderOutline(x0, this.boxY, x1 - x0, this.boxH, GOLD_DARK);
		g.drawCenteredString(this.font, "— " + GamitaTexts.get(GamitaTexts.COLLECTION) + " —", this.width / 2, this.boxY + 5, GOLD);
		if (this.icons.isEmpty()) {
			return;
		}
		int n = this.icons.size();
		int current = this.current();
		this.slideTo(current);
		int cx = this.width / 2, cy = this.boxY + 18 + BIG / 2;
		// room to each side, between the arrows
		float room = (x1 - x0) / 2F - (n > 1 ? 34 : 4);
		// the farthest first, the one in the middle last (over the others)
		List<float[]> row = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			row.add(new float[]{i, cyclic(i - this.slide, n)});
		}
		row.sort((a, b) -> Float.compare(Math.abs(b[1]), Math.abs(a[1])));
		for (float[] r : row) {
			float d = r[1], ad = Math.abs(d);
			float size = ad <= 1 ? Mth.lerp(ad, BIG, SMALL) : Math.max(16, SMALL - 6 * (ad - 1));
			float off = ad <= 1 ? ad * NEXT : NEXT + (ad - 1) * FAR;
			if (off + size / 2 > room) {
				continue;
			}
			this.tile(g, this.icons.get((int) r[0]), cx + Math.signum(d) * off, cy, size, (int) r[0] == current, t, Math.min(0.55F, ad * 0.3F));
		}
		Icon now = this.icons.get(current);
		g.drawCenteredString(this.font, now.name(), this.width / 2, this.boxY + 18 + BIG + 4, GOLD);
		int y = this.boxY + 18 + BIG + 16;
		int maxW = x1 - x0 - 16;
		var lines = this.font.getSplitter().splitLines(about(now.mod()), maxW, net.minecraft.network.chat.Style.EMPTY);
		for (int i = 0; i < Math.min(this.aboutLines, lines.size()); i++) {
			String line = lines.get(i).getString().strip();
			if (i == this.aboutLines - 1 && lines.size() > this.aboutLines) {
				// more than fits: it ends in "..."
				while (!line.isEmpty() && this.font.width(line + "...") > maxW) {
					line = line.substring(0, line.length() - 1);
				}
				line = line.strip() + "...";
			}
			g.drawCenteredString(this.font, line, this.width / 2, y, 0xFFCFC6B8);
			y += 10;
		}
		if (this.icons.size() == 1) {
			// the only one of them here: a mod or a port ("kind" in its gamita_models)
			g.drawCenteredString(this.font, GamitaTexts.get(port(now.mod()) ? GamitaTexts.ONLY_PORT : GamitaTexts.ONLY_MOD),
				this.width / 2, this.boxY + this.boxH - 12, 0xFF9C8A5A);
		}
	}

	/** An icon in its frame (the same for all of them): a gold edge, bright and glowing on the one named; dark inside. */
	private void tile(GuiGraphics g, Icon icon, float x, float y, float size, boolean named, float t, float dark) {
		int h = BIG / 2;
		g.pose().pushPose();
		g.pose().translate(x, y, 0);
		g.pose().scale(size / BIG, size / BIG, 1F);
		if (named) {
			int a = (int) (0x40 + 0x40 * (0.5F + 0.5F * Mth.sin(t * 4F)));
			g.fill(-h - 4, -h - 4, h + 4, h + 4, a << 24 | 0xE8B84A);
		}
		g.fill(-h, -h, h, h, named ? GOLD : GOLD_DARK);
		g.fill(-h + 1, -h + 1, h - 1, h - 1, 0xFF2A1F12);
		g.fill(-h + 2, -h + 2, h - 2, h - 2, 0xFF15111C);
		int in = h - 4;
		if (icon.texture() != null) {
			g.blit(icon.texture(), -in, -in, in * 2, in * 2, 0, 0, icon.w(), icon.h(), icon.w(), icon.h());
		} else {
			g.drawCenteredString(this.font, icon.name().substring(0, 1), 0, -4, 0xFFFFFF);
		}
		if (dark > 0) {
			// the ones away from the middle, a little in the shade
			g.fill(-h, -h, h, h, (int) (dark * 255) << 24 | 0x0C0A12);
		}
		g.pose().popPose();
	}

	/** Moves the row toward the one named, by the shortest way round, easing in. */
	private void slideTo(int target) {
		long now = net.minecraft.Util.getMillis();
		float dt = this.lastFrame == 0 ? 1F : Math.min(1F, (now - this.lastFrame) / 1000F);
		this.lastFrame = now;
		int n = this.icons.size();
		float step = cyclic(target - this.slide, n) * (1F - (float) Math.exp(-dt * 9F));
		this.slide = ((this.slide + step) % n + n) % n;
	}

	/** How far {@code v} places are from the middle in a ring of {@code n}: from -n/2 to n/2. */
	private static float cyclic(float v, int n) {
		float r = (v % n + n) % n;
		return r > n / 2F ? r - n : r;
	}

	/** The mod named now: the one picked, then the next every 3 s (the first wait after an arrow is 8 s). */
	private int current() {
		if (this.icons.isEmpty()) {
			return 0;
		}
		long since = net.minecraft.Util.getMillis() - this.pickedAt;
		int steps = since < 0 ? 0 : (int) (since / 3000L);
		return Math.floorMod(this.pick + steps, this.icons.size());
	}

	private void step(int by) {
		if (this.icons.isEmpty()) {
			return;
		}
		this.pick = Math.floorMod(this.current() + by, this.icons.size());
		// a while on it before the turn goes on
		this.pickedAt = net.minecraft.Util.getMillis() + 5000L;
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.next);
	}

	/** A button lighter at the top, that grows, glows in its color and has a shine run across it under the cursor. */
	private static class Fancy extends AbstractWidget {
		private final int color;
		private final boolean big;
		private final Runnable action;
		private float hover;

		Fancy(int x, int y, int w, int h, String text, int color, boolean big, Runnable action) {
			super(x, y, w, h, Component.literal(text));
			this.color = color;
			this.big = big;
			this.action = action;
		}

		private static int shade(int color, float f) {
			int r = Math.min(255, (int) ((color >> 16 & 255) * f));
			int gr = Math.min(255, (int) ((color >> 8 & 255) * f));
			int b = Math.min(255, (int) ((color & 255) * f));
			return 0xFF000000 | r << 16 | gr << 8 | b;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
			this.hover = Mth.lerp(0.25F, this.hover, this.isHoveredOrFocused() ? 1F : 0F);
			float pulse = this.big ? 0.03F * (0.5F + 0.5F * Mth.sin(net.minecraft.Util.getMillis() / 300F)) : 0F;
			float scale = 1F + 0.07F * this.hover + pulse * (1F - this.hover);
			int hw = this.width / 2, hh = this.height / 2;
			g.pose().pushPose();
			g.pose().translate(this.getX() + hw, this.getY() + hh, 0);
			g.pose().scale(scale, scale, 1F);
			if (this.hover > 0.02F) {
				int glow = (int) (0x80 * this.hover) << 24 | (this.color & 0xFFFFFF);
				g.fill(-hw - 4, -hh - 4, hw + 4, hh + 4, (int) (0x30 * this.hover) << 24 | (this.color & 0xFFFFFF));
				g.fill(-hw - 2, -hh - 2, hw + 2, hh + 2, glow);
			}
			float light = 1F + 0.2F * this.hover;
			g.fillGradient(-hw, -hh, hw, hh, shade(this.color, 1.25F * light), shade(this.color, 0.75F * light));
			g.fill(-hw, -hh, hw, -hh + 1, 0x60FFFFFF);
			if (this.hover > 0.5F) {
				// a shine running across, only over the button: before it comes in and once it has gone out
				// there is nothing to draw (fill would swap the two edges and paint a strip outside)
				float run = (net.minecraft.Util.getMillis() % 1200) / 1200F;
				int sx = (int) (-hw + run * (this.width + 20)) - 10;
				int from = Math.max(-hw, sx), to = Math.min(hw, sx + 6);
				if (from < to) {
					g.fill(from, -hh, to, hh, 0x50FFFFFF);
				}
			}
			g.renderOutline(-hw, -hh, this.width, this.height, shade(this.color, 0.4F));
			var font = GamitaCollection.mc().font;
			if (this.big) {
				g.pose().scale(1.3F, 1.3F, 1F);
			}
			g.drawCenteredString(font, this.getMessage(), 0, -4, 0xFFFFFF);
			g.pose().popPose();
		}

		@Override
		public void onClick(double mouseX, double mouseY) {
			this.action.run();
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput out) {
			this.defaultButtonNarrationText(out);
		}
	}
}
