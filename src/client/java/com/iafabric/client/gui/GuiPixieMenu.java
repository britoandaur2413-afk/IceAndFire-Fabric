package com.iafabric.client.gui;

import com.iafabric.client.IafKeybinds;
import com.iafabric.entity.EntityPixie;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.inventory.PixieMenu;
import com.iafabric.message.MessagePixieBondSync;
import com.iafabric.pixie.PixieAbility;
import com.iafabric.pixie.PixieBonds;
import com.iafabric.pixie.PixieElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * A bonded pixie's own menu: the pixie, its level and shield, what it gives its player now (its effect, or at levels
 * 4 and 5 its stats), its five orb slots, a slot to give it things (pixie dust, or a thing that teaches it a spell),
 * the help it gives by itself and its three spells (click: the one the magic key calls; a lock on the ones it does
 * not know yet, with what teaches them and from which level).
 */
public class GuiPixieMenu extends AbstractContainerScreen<PixieMenu> {
	private static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/gui/pixie_menu.png");
	private static final Identifier LOCK = Identifier.parse("iceandfire_fabric:textures/gui/pixie_lock.png");
	private static final int[] COLORS = {0xFFF7A8C8, 0xFFC9A0F5, 0xFF8FD3F5, 0xFF9FE39A, 0xFFF9CF6E};
	private static final int TEXT = 0xFF404040;
	/** The orbs' colors, faint, for the marks in their empty slots (physical, projectile, magic, fire, water). */
	private static final int[] ORB_TONES = {0x50D0D0D0, 0x5070C080, 0x509070D0, 0x50E07030, 0x504090E0};
	private static final int HELP_X = 8;
	private static final int SPELL_X = 46;
	private static final int ABILITY_Y = 110;
	/** What it gives: its own line under the orbs. */
	private static final int GIFT_Y = PixieMenu.ORB_Y + 21;
	private EntityPixie model;
	private PixieBonds shown;

	public GuiPixieMenu(PixieMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 230, 226);
		this.inventoryLabelX = PixieMenu.INV_X;
		this.inventoryLabelY = PixieMenu.INV_Y - 10;
	}

	public static Identifier icon(PixieAbility ability) {
		return Identifier.parse("iceandfire_fabric:textures/gui/pixie_ability/" + ability.name + ".png");
	}

	private PixieBonds.Bond bond() {
		return shown.get(menu.color);
	}

	@Override
	protected void init() {
		super.init();
		shown = MessagePixieBondSync.client;
		boolean out = shown.active == menu.color;
		this.addRenderableWidget(Button.builder(Component.translatable(out ? "gui.iceandfire_fabric.pixie_bonds.rest" : "gui.iceandfire_fabric.pixie_bonds.summon"),
			b -> click(PixieMenu.BUTTON_CALL)).bounds(leftPos + 150, topPos + 40, 72, 18).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.iceandfire_fabric.pixie_menu.give"), b -> click(PixieMenu.BUTTON_FEED))
			.bounds(leftPos + PixieMenu.FEED_X + 22, topPos + PixieMenu.FEED_Y - 1, 30, 20).build());
		List<PixieAbility> spells = PixieAbility.spellsOf(menu.color);
		for (int i = 0; i < spells.size(); i++) {
			PixieAbility spell = spells.get(i);
			this.addRenderableWidget(Button.builder(Component.empty(), b -> click(PixieMenu.BUTTON_ABILITY + spell.ordinal()))
				.bounds(leftPos + SPELL_X + i * 20, topPos + ABILITY_Y, 18, 18).build());
		}
	}

	private void click(int id) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
		}
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (shown != MessagePixieBondSync.client) {
			this.rebuildWidgets();
		}
	}

	private EntityPixie model() {
		if (model == null && Minecraft.getInstance().level != null) {
			model = IafGui.drawnOnly(IafEntityRegistry.PIXIE);
			if (model != null) {
				model.setColor(menu.color);
			}
		}
		return model;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(g, mouseX, mouseY, partialTicks);
		IafGui.blit(g, TEXTURE, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
		EntityPixie pixie = model();
		if (pixie != null) {
			IafGui.entityInBox(g, pixie, leftPos + 9, topPos + 18, leftPos + 70, topPos + 94, 62, mouseX, mouseY);
		}
	}

	private static String key() {
		return IafKeybinds.pixieMagic.getTranslatedKeyMessage().getString();
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		PixieBonds.Bond bond = bond();
		int tint = COLORS[Math.floorMod(menu.color, COLORS.length)];
		IafGui.fit(g, font, this.title.getString(), 8, 6, this.imageWidth - 16, TEXT);
		IafGui.text(g, font, this.playerInventoryTitle.getString(), this.inventoryLabelX, this.inventoryLabelY, TEXT);
		IafGui.text(g, font, Component.translatable("gui.iceandfire_fabric.pixie_bonds.level", bond.level).getString(), 78, 18, TEXT);
		String xp = bond.level >= 5 ? Component.translatable("gui.iceandfire_fabric.pixie_menu.max").getString()
			: bond.xp + " / " + PixieBonds.LEVEL_XP[bond.level - 1];
		IafGui.text(g, font, xp, 222 - font.width(xp), 18, 0xFF606060);
		int need = bond.level >= 5 ? 1 : PixieBonds.LEVEL_XP[bond.level - 1];
		int from = bond.level <= 1 ? 0 : PixieBonds.LEVEL_XP[bond.level - 2];
		float progress = bond.level >= 5 ? 1F : Math.max(0F, Math.min(1F, (bond.xp - from) / (float) Math.max(1, need - from)));
		g.fill(78, 28, 222, 33, 0xFF303030);
		g.fill(78, 28, 78 + (int) (144 * progress), 33, tint);
		g.outline(78, 28, 144, 5, 0xFF202020);
		IafGui.text(g, font, Component.translatable("gui.iceandfire_fabric.pixie_bonds.shield", (int) bond.shield, bond.maxShield()).getString(), 78, 38, 0xFF2E5C8A);
		IafGui.text(g, font, Component.translatable("gui.iceandfire_fabric.pixie_bonds.share", Math.round(bond.share() * 100)).getString(), 78, 48, 0xFF8A3E3E);
		IafGui.text(g, font, Component.translatable("gui.iceandfire_fabric.pixie_menu.orbs").getString(), PixieMenu.ORB_X, PixieMenu.ORB_Y - 10, TEXT);
		// what it gives its player now, on its own line under the orbs
		List<Component> gift = gift(menu.color, bond.level);
		IafGui.fit(g, font, gift.get(0).getString(), PixieMenu.ORB_X, GIFT_Y, 222 - PixieMenu.ORB_X, darker(tint));
		IafGui.fit(g, font, Component.translatable("gui.iceandfire_fabric.pixie_menu.help").getString(), HELP_X, ABILITY_Y - 9, SPELL_X - HELP_X - 4, TEXT);
		IafGui.fit(g, font, Component.translatable("gui.iceandfire_fabric.pixie_menu.magic", key()).getString(), SPELL_X, ABILITY_Y - 9, 60, TEXT);
		IafGui.text(g, font, Component.translatable("gui.iceandfire_fabric.pixie_menu.feed").getString(), PixieMenu.FEED_X, PixieMenu.FEED_Y - 10, TEXT);
		// the spell the key calls, and whether it is ready
		PixieAbility spell = bond.selected();
		if (spell != null && spell.color == menu.color) {
			IafGui.fit(g, font, Component.translatable("pixie.iceandfire_fabric.ability." + spell.name).getString(), SPELL_X + 62, ABILITY_Y + 1, 166 - SPELL_X - 62, darker(tint));
			IafGui.fit(g, font, state(bond, spell).getString(), SPELL_X + 62, ABILITY_Y + 11, 166 - SPELL_X - 62, 0xFF606060);
		}
	}

	private static int darker(int color) {
		int r = (int) (((color >> 16) & 0xFF) * 0.55F);
		int gr = (int) (((color >> 8) & 0xFF) * 0.55F);
		int b = (int) ((color & 0xFF) * 0.55F);
		return 0xFF000000 | r << 16 | gr << 8 | b;
	}

	/** Ready, the wait left, or the level it needs. */
	private static Component state(PixieBonds.Bond bond, PixieAbility spell) {
		if (bond.level < spell.minLevel) {
			return Component.translatable("gui.iceandfire_fabric.pixie_menu.from_level", spell.minLevel);
		}
		long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
		long wait = bond.ready[spell.ordinal()] - now;
		return wait > 0 ? Component.translatable("gui.iceandfire_fabric.pixie_menu.wait", (wait + 19) / 20) : Component.translatable("gui.iceandfire_fabric.pixie_menu.ready", key());
	}

	/**
	 * What a pixie of that color gives at that level: its effect (levels 1 to 3: "Strength II"), or its stats (levels
	 * 4 and 5: "+6 Attack Damage"); the first line shown, all of them in its tooltip.
	 */
	public static List<Component> gift(int color, int level) {
		PixieElement element = PixieElement.of(color);
		List<Component> lines = new ArrayList<>();
		int amplifier = PixieElement.amplifier(level);
		if (amplifier >= 0) {
			MutableComponent effect = Component.translatable(element.effect.value().getDescriptionId()).append(" " + roman(amplifier + 1));
			lines.add(Component.translatable("gui.iceandfire_fabric.pixie_menu.gives", effect));
			return lines;
		}
		for (PixieElement.Bonus bonus : element.bonuses) {
			double value = bonus.at(level);
			String amount = bonus.operation() == AttributeModifier.Operation.ADD_VALUE ? "+" + trim(value) : "+" + Math.round(value * 100) + "%";
			MutableComponent stat = Component.literal(amount + " ").append(Component.translatable(bonus.attribute().value().getDescriptionId()));
			lines.add(Component.translatable("gui.iceandfire_fabric.pixie_menu.gives", stat));
		}
		return lines;
	}

	private static String trim(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
	}

	private static String roman(int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			default -> String.valueOf(n);
		};
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(g, mouseX, mouseY, partialTicks);
		PixieBonds.Bond bond = bond();
		// the empty orb slots show which orb goes there: a faint round mark of its color (items are drawn over fills)
		for (int i = 0; i < 5; i++) {
			int x = leftPos + PixieMenu.ORB_X + 1 + i * 20;
			int y = topPos + PixieMenu.ORB_Y + 1;
			if (!menu.getSlot(i).hasItem()) {
				int tone = ORB_TONES[i];
				g.fill(x + 5, y + 3, x + 11, y + 13, tone);
				g.fill(x + 3, y + 5, x + 13, y + 11, tone);
				g.fill(x + 4, y + 4, x + 12, y + 12, tone);
			}
			if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17 && !menu.getSlot(i).hasItem() && menu.getCarried().isEmpty()) {
				g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("item.iceandfire_fabric.pixie_orb_" + orbName(i)),
					Component.translatable("gui.iceandfire_fabric.pixie_menu.orb_hint").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
			}
		}
		if (!menu.getSlot(5).hasItem() && menu.getCarried().isEmpty()) {
			int x = leftPos + PixieMenu.FEED_X + 1;
			int y = topPos + PixieMenu.FEED_Y + 1;
			if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
				g.setComponentTooltipForNextFrame(font, List.of(Component.translatable("gui.iceandfire_fabric.pixie_menu.feed"),
					Component.translatable("gui.iceandfire_fabric.pixie_menu.feed_dust").withStyle(ChatFormatting.GRAY),
					Component.translatable("gui.iceandfire_fabric.pixie_menu.feed_teach").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
			}
		}
		// what it gives: all of it on the line's tooltip
		int giftWidth = 222 - PixieMenu.ORB_X;
		if (mouseX >= leftPos + PixieMenu.ORB_X && mouseX < leftPos + PixieMenu.ORB_X + giftWidth && mouseY >= topPos + GIFT_Y - 1 && mouseY < topPos + GIFT_Y + 9) {
			List<Component> lines = new ArrayList<>(gift(menu.color, bond.level));
			lines.add(Component.translatable(bond.level >= 4 ? "gui.iceandfire_fabric.pixie_menu.gift_stats" : "gui.iceandfire_fabric.pixie_menu.gift_effect").withStyle(ChatFormatting.GRAY));
			g.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
		}
		int tint = COLORS[Math.floorMod(menu.color, COLORS.length)];
		// its help: always on
		PixieAbility help = PixieAbility.supportOf(menu.color);
		int hx = leftPos + HELP_X;
		int hy = topPos + ABILITY_Y;
		g.fill(hx, hy, hx + 18, hy + 18, 0xFF8B8B8B);
		g.outline(hx, hy, 18, 18, tint);
		IafGui.blit(g, icon(help), hx + 1, hy + 1, 0F, 0F, 16, 16, 16, 16, 1F);
		if (mouseX >= hx && mouseX < hx + 18 && mouseY >= hy && mouseY < hy + 18) {
			g.setComponentTooltipForNextFrame(font, List.of(
				Component.translatable("pixie.iceandfire_fabric.ability." + help.name).withStyle(ChatFormatting.LIGHT_PURPLE),
				Component.translatable("pixie.iceandfire_fabric.ability." + help.name + ".desc").withStyle(ChatFormatting.GRAY),
				Component.translatable("gui.iceandfire_fabric.pixie_menu.by_itself", help.cooldown / 20F).withStyle(ChatFormatting.GREEN)), mouseX, mouseY);
		}
		// its spells: their pictures on the buttons; the chosen one framed in the pixie's color, unknown ones locked
		List<PixieAbility> spells = PixieAbility.spellsOf(menu.color);
		for (int i = 0; i < spells.size(); i++) {
			PixieAbility spell = spells.get(i);
			int x = leftPos + SPELL_X + i * 20;
			int y = topPos + ABILITY_Y;
			boolean known = bond.knows(spell);
			boolean chosen = known && bond.spell == spell.ordinal();
			if (!known) {
				IafGui.blit(g, icon(spell), x + 1, y + 1, 0F, 0F, 16, 16, 16, 16, 0.35F);
				g.fill(x + 1, y + 1, x + 17, y + 17, 0x70202020);
				IafGui.blit(g, LOCK, x + 3, y + 3, 0F, 0F, 12, 12, 12, 12, 1F);
			} else {
				IafGui.blit(g, icon(spell), x + 1, y + 1, 0F, 0F, 16, 16, 16, 16, bond.level >= spell.minLevel ? 1F : 0.45F);
			}
			if (chosen) {
				g.outline(x - 1, y - 1, 20, 20, tint);
				g.outline(x, y, 18, 18, tint);
			}
			if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
				g.setComponentTooltipForNextFrame(font, spellTip(bond, spell, known, chosen), mouseX, mouseY);
			}
		}
	}

	private static List<Component> spellTip(PixieBonds.Bond bond, PixieAbility spell, boolean known, boolean chosen) {
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("pixie.iceandfire_fabric.ability." + spell.name).withStyle(ChatFormatting.LIGHT_PURPLE));
		lines.add(Component.translatable("pixie.iceandfire_fabric.ability." + spell.name + ".desc").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("gui.iceandfire_fabric.pixie_menu.cooldown", spell.cooldown / 20).withStyle(ChatFormatting.DARK_GRAY));
		if (!known) {
			lines.add(Component.translatable("gui.iceandfire_fabric.pixie_menu.taught_by", spell.teacher().getHoverName()).withStyle(ChatFormatting.YELLOW));
		}
		if (bond.level < spell.minLevel) {
			lines.add(Component.translatable("gui.iceandfire_fabric.pixie_menu.from_level", spell.minLevel).withStyle(ChatFormatting.RED));
		}
		if (known) {
			lines.add(Component.translatable(chosen ? "gui.iceandfire_fabric.pixie_menu.chosen" : "gui.iceandfire_fabric.pixie_menu.choose", key())
				.withStyle(chosen ? ChatFormatting.GREEN : ChatFormatting.WHITE));
		}
		return lines;
	}

	private static String orbName(int kind) {
		return switch (kind) {
			case 0 -> "physical";
			case 1 -> "projectile";
			case 2 -> "magic";
			case 3 -> "fire";
			default -> "water";
		};
	}
}
