package com.iafabric.gamita;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * The welcome, over the title screen's panorama: GAMITA_MODELS and thanks; a box with the GAMITA_MODELS mods in this
 * game (their icons turning like coins, one at a time named and told about, in its own words from its
 * fabric.mod.json); under it the future expansion of the mod named (its picture of the work so far and its name,
 * "next" in its gamita_models); a big "Satisfied!" that goes on to the game; a wide Patreon button (support the
 * project, be among the first to try it) and a small Discord one, both opening the browser. Laid out from the
 * screen's size, so it fits any GUI scale (on a small screen the expansion's strip goes first).
 */
public class GamitaWelcomeScreen extends Screen {
	private static final int GOLD = 0xFFE8B84A;
	private static final int GOLD_DARK = 0xFF7A5A1C;
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
	/** From the top of the box of mods to the top of its icons (room for its heading). */
	private static final int TOP = 26;
	// the layout (from the screen's size)
	private int panelX, panelY, panelW, panelH;
	private int boxY, boxH;
	private int aboutLines;
	private int nextY;
	private boolean showNext;
	/** On a short screen everything is drawn this much smaller, so nothing has to be left out; the real size. */
	private float fit = 1F;
	private int realW, realH;
	/** The strip of the future expansion and its picture's width (16:9). */
	private static final int NEXT_H = 54, NEXT_PIC_W = 80;

	private record Picture(Identifier texture, int w, int h) {
	}

	private record Icon(String name, ModContainer mod, Identifier texture, int w, int h, Picture next) {
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
		Picture next = this.nextPicture(mod);
		try {
			var path = mod.getMetadata().getIconPath(64).flatMap(mod::findPath);
			if (path.isPresent()) {
				try (InputStream in = Files.newInputStream(path.get())) {
					NativeImage image = NativeImage.read(in);
					Identifier id = Identifier.fromNamespaceAndPath(this.myModId, "gamita/icon_" + mod.getMetadata().getId());
					GamitaCollection.mc().getTextureManager().register(id, new DynamicTexture(() -> "gamita", image));
					return new Icon(name, mod, id, image.getWidth(), image.getHeight(), next);
				}
			}
		} catch (Exception ignored) {
			// (no icon: its first letter)
		}
		return new Icon(name, mod, null, 0, 0, next);
	}

	/** The picture of its future expansion ("next": {"image": a path in its jar}), when it has one. */
	private Picture nextPicture(ModContainer mod) {
		try {
			String file = mod.getMetadata().getCustomValue("gamita_models").getAsObject().get("next").getAsObject()
				.get("image").getAsString();
			var path = mod.findPath(file);
			if (path.isPresent()) {
				try (InputStream in = Files.newInputStream(path.get())) {
					NativeImage image = NativeImage.read(in);
					Identifier id = Identifier.fromNamespaceAndPath(this.myModId, "gamita/next_" + mod.getMetadata().getId());
					GamitaCollection.mc().getTextureManager().register(id, new DynamicTexture(() -> "gamita", image));
					return new Picture(id, image.getWidth(), image.getHeight());
				}
			}
		} catch (Exception ignored) {
			// (no picture: the name alone)
		}
		return null;
	}

	/** The name of its future expansion ("next": {"title": by language}), or null when it has none. */
	private static String nextTitle(ModContainer mod) {
		try {
			CustomValue.CvObject title = mod.getMetadata().getCustomValue("gamita_models").getAsObject().get("next")
				.getAsObject().get("title").getAsObject();
			String code = GamitaCollection.mc().getLanguageManager().getSelected();
			for (String key : new String[]{code, code.length() >= 2 ? code.substring(0, 2) : code, "en"}) {
				if (title.containsKey(key)) {
					return title.get(key).getAsString();
				}
			}
		} catch (RuntimeException ignored) {
			// (none)
		}
		return null;
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
		this.opened = net.minecraft.util.Util.getMillis();
		if (this.pickedAt == 0) {
			this.pickedAt = this.opened;
		}
		// the whole panel, nothing left out; on a short screen it is drawn smaller (as if the screen were bigger)
		this.realW = this.width;
		this.realH = this.height;
		int need = 10 + 22 + 12 + 10 + 10 + 30 + 8 + 22 + 4 + 16 + 8 + TOP + BIG + 24 + 33 + 8 + (this.icons.size() == 1 ? 16 : 0)
			+ NEXT_H + 8 + 8;
		this.fit = Math.min(1F, this.realH / (float) need);
		this.width = Math.round(this.realW / this.fit);
		this.height = Math.round(this.realH / this.fit);
		this.panelW = Math.min(380, this.width - 16);
		int inner = this.panelW - 24;
		// as many lines for the mods' own words as the longest of them needs (up to 3)
		int aboutW = this.panelW - 20 - 16;
		this.aboutLines = Math.max(1, Math.min(3, this.icons.stream().mapToInt(i -> this.font.getSplitter()
			.splitLines(about(i.mod()), aboutW, net.minecraft.network.chat.Style.EMPTY).size()).max().orElse(1)));
		this.showNext = !this.icons.isEmpty();
		int nextH = this.showNext ? NEXT_H + 8 : 0;
		// title 22, thanks 12, gap 10, box, [gap 8, expansion], gap 10, big 30, gap 8, Patreon 22, gap 4, Discord 16, padding
		int fixed = 10 + 22 + 12 + 10 + 10 + 30 + 8 + 22 + 4 + 16 + 8;
		this.boxH = TOP + BIG + 24 + this.aboutLines * 11 + 8 + (this.icons.size() == 1 ? 16 : 0);
		if (fixed + this.boxH + nextH > this.height - 8) {
			nextH = 0;
			this.showNext = false;
		}
		while (fixed + this.boxH + nextH > this.height - 8 && this.aboutLines > 0) {
			this.aboutLines--;
			this.boxH -= 10;
		}
		this.panelH = fixed + this.boxH + nextH;
		this.panelX = this.width / 2 - this.panelW / 2;
		this.panelY = Math.max(4, (this.height - this.panelH) / 2);
		this.boxY = this.panelY + 10 + 22 + 12 + 10;
		int arrowY = this.boxY + TOP + BIG / 2 - 9;
		if (this.icons.size() > 1) {
		this.addRenderableWidget(new Fancy(this.panelX + 16, arrowY, 18, 18, "◀", 0xFF6A5A2A, false, () -> this.step(-1)));
		this.addRenderableWidget(new Fancy(this.panelX + this.panelW - 34, arrowY, 18, 18, "▶", 0xFF6A5A2A, false, () -> this.step(1)));
		}
		this.nextY = this.boxY + this.boxH + 8;
		int y = this.boxY + this.boxH + nextH + 10;
		int bigW = Math.min(220, inner);
		this.addRenderableWidget(new Fancy(this.width / 2 - bigW / 2, y, bigW, 30, "✔ " + GamitaTexts.get(GamitaTexts.SATISFIED),
			0xFF2E9E4F, true, this::onClose));
		y += 38;
		this.addRenderableWidget(new Fancy(this.width / 2 - inner / 2, y, inner, 22, "❤ " + GamitaTexts.get(GamitaTexts.PATREON),
			0xFFF96854, false, () -> GamitaCollection.open(GamitaCollection.PATREON)));
		y += 26;
		int smallW = Math.min(150, inner / 2);
		this.addRenderableWidget(new Fancy(this.width / 2 - smallW / 2, y, smallW, 16, GamitaTexts.get(GamitaTexts.DISCORD),
			0xFF5865F2, false, () -> GamitaCollection.open(GamitaCollection.DISCORD)));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		this.extractPanorama(g, partial);
		g.fillGradient(0, 0, this.realW, this.realH, 0x90000000, 0xC0000000);
		g.pose().pushMatrix();
		g.pose().scale(this.fit, this.fit);
		this.drawAll(g, Math.round(mouseX / this.fit), Math.round(mouseY / this.fit), partial);
		g.pose().popMatrix();
	}

	/** The buttons, drawn at the same size as the panel. */
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		g.pose().pushMatrix();
		g.pose().scale(this.fit, this.fit);
		super.extractRenderState(g, Math.round(mouseX / this.fit), Math.round(mouseY / this.fit), partial);
		g.pose().popMatrix();
	}

	private MouseButtonEvent fitted(MouseButtonEvent event) {
		return new MouseButtonEvent(event.x() / this.fit, event.y() / this.fit, event.buttonInfo());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		return super.mouseClicked(this.fitted(event), doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		return super.mouseReleased(this.fitted(event));
	}

	@Override
	public void mouseMoved(double x, double y) {
		super.mouseMoved(x / this.fit, y / this.fit);
	}

	private void drawAll(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		float t = (net.minecraft.util.Util.getMillis() - this.opened) / 1000F;
		int cx = this.width / 2;
		this.panel(g, t);
		// the name, big, gold, outlined dark, its gold breathing a little
		float pulse = 0.5F + 0.5F * Mth.sin(t * 2F);
		int nameW = this.font.width("GAMITA_MODELS") * 2;
		g.pose().pushMatrix();
		g.pose().translate((float) (cx), (float) (this.panelY + 11));
		g.pose().scale(2F, 2F);
		int w = this.font.width("GAMITA_MODELS");
		for (int[] d : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
			g.text(this.font, "GAMITA_MODELS", -w / 2 + d[0], d[1], 0xFF1A1206, false);
		}
		int gold = 0xFF000000 | (int) Mth.lerp(pulse, 0xD8, 0xFF) << 16 | (int) Mth.lerp(pulse, 0xA8, 0xD0) << 8 | 0x4A;
		g.text(this.font, "GAMITA_MODELS", -w / 2, 0, gold, false);
		g.pose().popMatrix();
		this.star(g, cx - nameW / 2 - 14, this.panelY + 19, t);
		this.star(g, cx + nameW / 2 + 14, this.panelY + 19, t + 1.5F);
		g.centeredText(this.font, GamitaTexts.get(GamitaTexts.THANKS), cx, this.panelY + 34, 0xFFF2E6C8);
		this.collection(g, t);
		if (this.showNext && !this.icons.isEmpty()) {
			this.expansion(g, this.icons.get(this.current()), t);
		}
	}

	/**
	 * The future expansion of the mod named: its picture of the work so far on the left (in a gold frame, a light
	 * crossing it now and then), "FUTURE EXPANSION" in gold and its name on the right. A mod without one planned
	 * shows a "?" and a short line instead (none yet, ideas to Discord; when there is, Patreon tries it first).
	 */
	private void expansion(GuiGraphicsExtractor g, Icon icon, float t) {
		String title = nextTitle(icon.mod());
		int x0 = this.panelX + 10, x1 = this.panelX + this.panelW - 10, y0 = this.nextY;
		g.fillGradient(x0, y0, x1, y0 + NEXT_H, 0xC0181424, 0xC0101018);
		g.outline(x0, y0, x1 - x0, NEXT_H, GOLD_DARK);
		int px = x0 + 4, py = y0 + 4, ph = NEXT_H - 8;
		int pw = title == null ? ph : icon.next() == null ? 0 : NEXT_PIC_W;
		if (title == null) {
			// none planned yet: a big "?" in the frame, breathing
			g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, GOLD_DARK);
			g.fill(px, py, px + pw, py + ph, 0xFF15111C);
			float s = 2.6F + 0.15F * Mth.sin(t * 2.5F);
			g.pose().pushMatrix();
			g.pose().translate((float) (px + pw / 2F), (float) (py + ph / 2F));
			g.pose().scale(s, s);
			g.centeredText(this.font, "?", 0, -4, GOLD);
			g.pose().popMatrix();
		} else if (icon.next() != null) {
			Picture p = icon.next();
			g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, GOLD);
			// the middle of the picture, cut to the frame
			int sw = p.w(), sh = p.h();
			int cw = Math.min(sw, sh * pw / ph), ch = Math.min(sh, sw * ph / pw);
			g.blit(RenderPipelines.GUI_TEXTURED, p.texture(), px, py, (sw - cw) / 2F, (sh - ch) / 2F, pw, ph, cw, ch, sw, sh);
			float run = (t % 4F) / 4F;
			int sx = px + (int) (run * (pw + 30)) - 15;
			int from = Math.max(px, sx), to = Math.min(px + pw, sx + 8);
			if (from < to) {
				g.fill(from, py, to, py + ph, 0x30FFFFFF);
			}
		}
		int tx = px + pw + (pw > 0 ? 8 : 4), tw = x1 - 6 - tx;
		float pulse = 0.5F + 0.5F * Mth.sin(t * 3F);
		int gold = 0xFF000000 | (int) Mth.lerp(pulse, 0xC8, 0xFF) << 16 | (int) Mth.lerp(pulse, 0x98, 0xD0) << 8 | 0x4A;
		g.text(this.font, "✦ " + GamitaTexts.get(GamitaTexts.NEXT), tx, y0 + 6, gold, true);
		if (title != null) {
			var lines = this.font.split(Component.literal(title), tw);
			int y = y0 + 20;
			for (int i = 0; i < Math.min(3, lines.size()); i++) {
				g.text(this.font, lines.get(i), tx, y, 0xFFF2E6C8, true);
				y += 10;
			}
		} else {
			var lines = this.font.split(Component.literal(GamitaTexts.get(GamitaTexts.NONE)), tw);
			int y = y0 + 17;
			for (int i = 0; i < Math.min(4, lines.size()); i++) {
				g.text(this.font, lines.get(i), tx, y, 0xFFCFC6B8, false);
				y += 9;
			}
		}
	}

	/** A dark panel, a little lighter at the top, with a double gold border and small gold corners. */
	private void panel(GuiGraphicsExtractor g, float t) {
		int x0 = this.panelX, y0 = this.panelY, x1 = x0 + this.panelW, y1 = y0 + this.panelH;
		g.fillGradient(x0, y0, x1, y1, 0xF0241A30, 0xF00C0A12);
		g.outline(x0, y0, this.panelW, this.panelH, GOLD);
		g.outline(x0 + 2, y0 + 2, this.panelW - 4, this.panelH - 4, GOLD_DARK);
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
	private void star(GuiGraphicsExtractor g, int x, int y, float t) {
		float s = 0.8F + 0.2F * Mth.sin(t * 3F);
		g.pose().pushMatrix();
		g.pose().translate((float) (x), (float) (y));
		g.pose().rotate(t);
		g.pose().scale(s, s);
		g.fill(-1, -5, 1, 5, GOLD);
		g.fill(-5, -1, 5, 1, GOLD);
		g.fill(-1, -1, 1, 1, 0xFFFFFFFF);
		g.pose().popMatrix();
	}

	/**
	 * The box of the mods: the one named big in the middle, the others smaller to its sides (the farther,
	 * the smaller and darker), each in the same frame; the row slides from one to the next. Its name in
	 * gold and its own words under them.
	 */
	private void collection(GuiGraphicsExtractor g, float t) {
		int x0 = this.panelX + 10, x1 = this.panelX + this.panelW - 10;
		g.fillGradient(x0, this.boxY, x1, this.boxY + this.boxH, 0xC0101018, 0xC0181424);
		g.outline(x0, this.boxY, x1 - x0, this.boxH, GOLD_DARK);
		g.centeredText(this.font, "— " + GamitaTexts.get(GamitaTexts.COLLECTION) + " —", this.width / 2, this.boxY + 7, GOLD);
		if (this.icons.isEmpty()) {
			return;
		}
		int n = this.icons.size();
		int current = this.current();
		this.slideTo(current);
		int cx = this.width / 2, cy = this.boxY + TOP + BIG / 2;
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
		g.centeredText(this.font, now.name(), this.width / 2, this.boxY + TOP + BIG + 9, GOLD);
		int y = this.boxY + TOP + BIG + 24;
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
			g.centeredText(this.font, line, this.width / 2, y, 0xFFCFC6B8);
			y += 11;
		}
		if (this.icons.size() == 1) {
			// the only one of them here: a mod or a port ("kind" in its gamita_models)
			g.centeredText(this.font, GamitaTexts.get(port(now.mod()) ? GamitaTexts.ONLY_PORT : GamitaTexts.ONLY_MOD),
				this.width / 2, this.boxY + this.boxH - 14, 0xFF9C8A5A);
		}
	}

	/** An icon in its frame (the same for all of them): a gold edge, bright and glowing on the one named; dark inside. */
	private void tile(GuiGraphicsExtractor g, Icon icon, float x, float y, float size, boolean named, float t, float dark) {
		int h = BIG / 2;
		g.pose().pushMatrix();
		g.pose().translate((float) (x), (float) (y));
		g.pose().scale(size / BIG, size / BIG);
		if (named) {
			int a = (int) (0x40 + 0x40 * (0.5F + 0.5F * Mth.sin(t * 4F)));
			g.fill(-h - 4, -h - 4, h + 4, h + 4, a << 24 | 0xE8B84A);
		}
		g.fill(-h, -h, h, h, named ? GOLD : GOLD_DARK);
		g.fill(-h + 1, -h + 1, h - 1, h - 1, 0xFF2A1F12);
		g.fill(-h + 2, -h + 2, h - 2, h - 2, 0xFF15111C);
		int in = h - 4;
		if (icon.texture() != null) {
			g.blit(RenderPipelines.GUI_TEXTURED, icon.texture(), -in, -in, 0F, 0F, in * 2, in * 2, icon.w(), icon.h(), icon.w(), icon.h());
		} else {
			g.centeredText(this.font, icon.name().substring(0, 1), 0, -4, 0xFFFFFFFF);
		}
		if (dark > 0) {
			// the ones away from the middle, a little in the shade
			g.fill(-h, -h, h, h, (int) (dark * 255) << 24 | 0x0C0A12);
		}
		g.pose().popMatrix();
	}

	/** Moves the row toward the one named, by the shortest way round, easing in. */
	private void slideTo(int target) {
		long now = net.minecraft.util.Util.getMillis();
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
		long since = net.minecraft.util.Util.getMillis() - this.pickedAt;
		int steps = since < 0 ? 0 : (int) (since / 3000L);
		return Math.floorMod(this.pick + steps, this.icons.size());
	}

	private void step(int by) {
		if (this.icons.isEmpty()) {
			return;
		}
		this.pick = Math.floorMod(this.current() + by, this.icons.size());
		// a while on it before the turn goes on
		this.pickedAt = net.minecraft.util.Util.getMillis() + 5000L;
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.next);
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
		protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
			this.hover = Mth.lerp(0.25F, this.hover, this.isHoveredOrFocused() ? 1F : 0F);
			float pulse = this.big ? 0.03F * (0.5F + 0.5F * Mth.sin(net.minecraft.util.Util.getMillis() / 300F)) : 0F;
			float scale = 1F + 0.07F * this.hover + pulse * (1F - this.hover);
			int hw = this.width / 2, hh = this.height / 2;
			g.pose().pushMatrix();
			g.pose().translate(this.getX() + hw, this.getY() + hh);
			g.pose().scale(scale, scale);
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
				float run = (net.minecraft.util.Util.getMillis() % 1200) / 1200F;
				int sx = (int) (-hw + run * (this.width + 20)) - 10;
				int from = Math.max(-hw, sx), to = Math.min(hw, sx + 6);
				if (from < to) {
					g.fill(from, -hh, to, hh, 0x50FFFFFF);
				}
			}
			g.outline(-hw, -hh, this.width, this.height, shade(this.color, 0.4F));
			var font = GamitaCollection.mc().font;
			if (this.big) {
				g.pose().scale(1.3F, 1.3F);
			}
			// a long text in a narrow button gets smaller to fit
			int textW = font.width(this.getMessage());
			if (!this.big && textW > this.width - 10) {
				float k = (this.width - 10) / (float) textW;
				g.pose().scale(k, k);
			}
			g.centeredText(font, this.getMessage(), 0, -4, 0xFFFFFFFF);
			g.pose().popMatrix();
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			this.action.run();
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput out) {
			this.defaultButtonNarrationText(out);
		}
	}
}
