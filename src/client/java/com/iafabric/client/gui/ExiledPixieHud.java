package com.iafabric.client.gui;

import com.iafabric.client.render.entity.RenderExiledPixie;
import com.iafabric.message.MessageExiledSpeech;
import com.iafabric.message.MessageExiledSync;
import com.iafabric.pixie.ExiledPixieData;
import com.iafabric.pixie.ExiledVariants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * What a player's exiled pixie says, at the top left of their screen: her face, her name and her line written out
 * as she says it (an alarm in red, a quest's line in gold, a hint without her face), one after another, then gone
 * (nothing stays on the screen's side: where her quest leads is her checkpoint, FairyWaypoint). In a memory of hers
 * (MemoryOverlay) her lines are its subtitles instead, each in place of the one before.
 */
public final class ExiledPixieHud {
	private static final Deque<Line> QUEUE = new ArrayDeque<>();
	private static final Deque<Component> RECENT = new ArrayDeque<>();
	private static int heard;
	/** How many letters of a line are written a tick. */
	private static final float WRITING = 1.6F;
	/** The rows her box grows to, and a memory's subtitle; a longer line (a longer language) then has smaller letters. */
	private static final int SPEECH_ROWS = 5;
	private static final int SUBTITLE_ROWS = 4;
	private static @Nullable Line current;
	private static long shownAt;
	private static long ticks;

	/** A line, and when it came (one that waited too long is dropped: the moment is gone, and it is in the chat if it mattered). */
	private record Line(Component text, int kind, long came) {
	}

	private ExiledPixieHud() {
	}

	/** For the scenes: what she said lately (her last 64 lines), oldest first. */
	public static List<Component> recent() {
		return List.copyOf(RECENT);
	}

	/** For the scenes: how many lines she said in all (which of the recent ones are new). */
	public static int heard() {
		return heard;
	}

	/** A line of hers arrives: shown after the ones before it (a quest's line or an alarm is kept in the chat too). */
	public static void speak(Component text, int kind) {
		if (RECENT.size() >= 64) {
			RECENT.pollFirst();
		}
		RECENT.add(text);
		heard++;
		if (kind != MessageExiledSpeech.HINT) {
			// a hint waiting is about what came before: it goes
			QUEUE.removeIf(line -> line.kind() == MessageExiledSpeech.HINT);
		}
		if (MemoryOverlay.active()) {
			// a memory's line: at once, in place of the one before (the memory keeps its own pace)
			QUEUE.clear();
			current = new Line(text, kind, ticks);
			shownAt = ticks;
		} else {
			if (QUEUE.size() >= 4) {
				QUEUE.pollFirst();
			}
			QUEUE.add(new Line(text, kind, ticks));
		}
		Minecraft mc = Minecraft.getInstance();
		if ((kind == MessageExiledSpeech.QUEST || kind == MessageExiledSpeech.ALARM) && mc.player != null) {
			int color = ExiledVariants.DRESS[ExiledVariants.of(Math.max(0, MessageExiledSync.client.variant))];
			mc.player.sendSystemMessage(Component.literal(name() + ": ").withStyle(s -> s.withColor(color)).append(text.copy().withStyle(ChatFormatting.GRAY)));
		}
	}

	public static void tick() {
		ticks++;
		if (current != null && ticks - shownAt > duration(current)) {
			current = null;
		}
		while (current == null && !QUEUE.isEmpty()) {
			Line next = QUEUE.pollFirst();
			if (ticks - next.came() <= 190 || QUEUE.isEmpty() && ticks - next.came() <= 350) {
				current = next;
				shownAt = ticks;
			}
		}
		// a memory's subtitle being written out: a quill is heard writing it
		com.iafabric.client.sound.Writing.tick(MemoryOverlay.active() && current != null && ticks - shownAt < writtenIn(current));
	}

	/** The ticks a line takes to be written out (WRITING letters a tick, after her name). */
	private static float writtenIn(Line line) {
		return (line.text().getString().length() - 1) / WRITING;
	}

	/** For the scenes: whatever she is saying is said at once. */
	public static void clear() {
		QUEUE.clear();
		current = null;
	}

	private static long duration(Line line) {
		if (MemoryOverlay.active()) {
			// (a memory's subtitle: till the next line, about)
			return Mth.clamp(110 + line.text().getString().length(), 130, 240);
		}
		// time to read it once written out; longer when no other line waits
		long wait = QUEUE.isEmpty() ? 90 : 70;
		return Math.max(100L, Math.min(210L, wait + line.text().getString().length()));
	}

	private static String name() {
		return ExiledVariants.name(Math.max(0, MessageExiledSync.client.variant));
	}

	private static int accent() {
		return 0xFF000000 | ExiledVariants.DRESS[ExiledVariants.of(Math.max(0, MessageExiledSync.client.variant))];
	}

	public static void render(GuiGraphics g, float delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return;
		}
		ExiledPixieData data = MessageExiledSync.client;
		if (!data.met() || MemoryOverlay.active()) {
			return;
		}
		int width = Math.min(210, Math.max(150, g.guiWidth() * 2 / 5));
		if (current != null) {
			speech(g, mc.font, current, width, delta);
		}
	}

	/** Her line in its box; where the box ends. */
	private static int speech(GuiGraphics g, Font font, Line line, int width, float partial) {
		float age = ticks - shownAt + partial;
		long life = duration(line);
		float alpha = Mth.clamp(Math.min(age / 4F, (life - age) / 8F), 0F, 1F);
		if (alpha <= 0.02F) {
			return 4;
		}
		boolean hint = line.kind() == MessageExiledSpeech.HINT;
		int face = hint ? 0 : 24;
		int textX = 4 + 6 + face + (hint ? 0 : 5);
		int textW = width - (textX - 4) - 6;
		String full = line.text().getString();
		// written out as she says it, in the rows the whole line takes
		int shown = Math.min(full.length(), (int) (age * 1.6F) + 1);
		IafGui.Layout layout = speechLayout(font, line.text(), textW);
		float s = layout.scale();
		List<FormattedCharSequence> lines = font.split(Component.literal(full.substring(0, shown)).withStyle(line.text().getStyle()),
			(int) (textW / (s * layout.squeeze())));
		int rows = layout.lines().size();
		int h = Math.max(hint ? 0 : 30, (hint ? 6 : 16) + Math.round(rows * 10 * s));
		int border = switch (line.kind()) {
			case MessageExiledSpeech.ALARM -> 0xFFE05050;
			case MessageExiledSpeech.QUEST -> 0xFFE8C060;
			case MessageExiledSpeech.HINT -> 0xFF707080;
			default -> accent();
		};
		int a = (int) (alpha * 255) << 24;
		g.fill(4, 4, 4 + width, 4 + h, (int) (alpha * 0xD8) << 24 | 0x0E0D14);
		g.renderOutline(4, 4, width, h, (border & 0xFFFFFF) | a);
		if (!hint) {
			int fx = 4 + 4;
			int fy = 4 + 3;
			g.fill(fx - 1, fy - 1, fx + face + 1, fy + face + 1, (border & 0xFFFFFF) | a);
			// her face, from her skin (its front: eyes, fringe)
			IafGui.blitTinted(g, IafGui.white(alpha), RenderExiledPixie.texture(MessageExiledSync.client.variant), fx, fy, face, face, 8F, 8F, 8, 8, 64, 64);
			IafGui.text(g, font, name(), textX, 4 + 4, (brighter(accent()) & 0xFFFFFF) | a);
		}
		int ty = 4 + (hint ? 4 : 15);
		g.pose().pushPose();
		g.pose().translate(textX, ty, 0.0F);
		g.pose().scale(s * layout.squeeze(), s, 1.0F);
		for (int i = 0; i < Math.min(rows, lines.size()); i++) {
			g.drawString(font, lines.get(i), 0, i * 10, (hint ? 0xB8B8C8 : 0xF2F0FA) | a, false);
		}
		g.pose().popPose();
		return 4 + h;
	}

	/** Her line in her box, its text this wide: up to SPEECH_ROWS rows, then smaller letters, never cut. */
	public static IafGui.Layout speechLayout(Font font, Component text, int textW) {
		return IafGui.layout(font, text, textW, 1F, SPEECH_ROWS);
	}

	/** Her words in a memory's lower bar on a screen this wide: up to SUBTITLE_ROWS rows, then narrower letters. */
	private record Subtitle(List<FormattedText> rows, float squeeze) {
	}

	private static Subtitle subtitleRows(Font font, FormattedText whole, int w) {
		int room = Math.min(w - 24, 360);
		float squeeze = 1F;
		List<FormattedText> rows = font.getSplitter().splitLines(whole, room, Style.EMPTY);
		while (rows.size() > SUBTITLE_ROWS && squeeze > 0.6F) {
			squeeze = Math.max(0.6F, squeeze * 0.94F);
			rows = font.getSplitter().splitLines(whole, (int) (room / squeeze), Style.EMPTY);
		}
		return new Subtitle(rows, squeeze);
	}

	/** For the tests: whether this line of hers goes all in a memory's subtitle on a screen this wide. */
	public static boolean subtitleFits(Font font, Component text, int w) {
		return subtitleRows(font, Component.literal(name() + ": ").append(text), w).rows().size() <= SUBTITLE_ROWS;
	}

	/**
	 * Her line as a film's subtitle, in the lower bar of a memory: her name and her words, centered, written out as she
	 * says them (each row where it will be once all is said).
	 */
	static void subtitle(GuiGraphics g, Font font, int w, int h, int bar, float partial) {
		Line line = current;
		if (line == null) {
			return;
		}
		float age = ticks - shownAt + partial;
		float alpha = Mth.clamp(Math.min(age / 4F, (duration(line) - age) / 8F), 0F, 1F);
		if (alpha <= 0.02F) {
			return;
		}
		int a = (int) (alpha * 255) << 24;
		String name = name() + ": ";
		MutableComponent whole = Component.literal(name).withStyle(s -> s.withColor(brighter(accent()) & 0xFFFFFF))
			.append(line.text().copy().withStyle(s -> s.withColor(0xF2F0FA)));
		Subtitle subtitle = subtitleRows(font, whole, w);
		List<FormattedText> rows = subtitle.rows();
		int count = Math.min(SUBTITLE_ROWS, rows.size());
		int top = h - bar / 2 - count * 5;
		top = Math.min(top, h - 3 - count * 10);
		// her name at once, her words as she says them
		int left = name.length() + (int) (age * WRITING) + 1;
		g.pose().pushPose();
		g.pose().translate(w / 2F, 0, 0.0F);
		g.pose().scale(subtitle.squeeze(), 1F, 1.0F);
		for (int i = 0; i < count && left > 0; i++) {
			FormattedText row = rows.get(i);
			g.drawString(font, firstChars(row, left), -font.width(row) / 2, top + i * 10, 0xFFFFFF | a, true);
			left -= row.getString().length();
		}
		g.pose().popPose();
	}

	/** A row's first characters, as they are styled. */
	private static Component firstChars(FormattedText row, int count) {
		MutableComponent out = Component.empty();
		int[] left = {count};
		row.visit((style, piece) -> {
			if (left[0] <= 0) {
				return Optional.of(Boolean.TRUE);
			}
			String part = piece.length() <= left[0] ? piece : piece.substring(0, left[0]);
			left[0] -= part.length();
			out.append(Component.literal(part).withStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return out;
	}

	private static int brighter(int color) {
		int r = Math.min(255, ((color >> 16) & 255) + 30);
		int gr = Math.min(255, ((color >> 8) & 255) + 30);
		int b = Math.min(255, (color & 255) + 30);
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}
}
