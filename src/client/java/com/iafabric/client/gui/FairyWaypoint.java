package com.iafabric.client.gui;

import com.iafabric.message.MessageExiledSync;
import com.iafabric.pixie.ExiledPixieData;
import com.iafabric.pixie.ExiledVariants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * Her checkpoint on the player's screen, as the story missions of other mods show theirs (and beside them: its own
 * pin, color and beam): a beam of light in her color where her quest leads (her lantern once she lit it; on the way
 * when it is farther than the player sees), and a pin over the place with her mark and the distance under it; out of
 * sight the pin waits at the edge of the screen with an arrow toward it. The hidden chest she shows has a pin of its
 * own.
 */
public final class FairyWaypoint {
	private static final Identifier TEX = Identifier.parse("iceandfire_fabric:textures/gui/exiled_pin.png");
	private static final int CELL = 64;
	private static final int TEX_W = 320;
	private static final int TEX_H = 64;
	/** The pin's size on the screen (gui pixels); where its tip and the middle of its head are in a cell. */
	private static final float PIN = 22F;
	private static final float TIP_AT = 60.5F / 64F;
	private static final float BALL_AT = 26F / 64F;

	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix4f PROJ = new Matrix4f();
	private static Vec3 cam = Vec3.ZERO;
	private static boolean framed;

	private FairyWaypoint() {
	}

	public static void init() {
		LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
			try {
				var camera = context.levelState().cameraRenderState;
				VIEW.set(camera.viewRotationMatrix);
				PROJ.set(camera.projectionMatrix);
				cam = camera.pos;
				framed = true;
			} catch (Throwable ignored) {
				framed = false;
			}
			try {
				beam(context.poseStack(), context.submitNodeCollector(), context.levelState().worldPartialTicks);
			} catch (Throwable ignored) {
				// another renderer (shaders...) must never break the game
			}
		});
	}

	private static ExiledPixieData data() {
		return MessageExiledSync.client;
	}

	private static int color() {
		return ExiledVariants.DRESS[ExiledVariants.of(Math.max(0, data().variant))];
	}

	/** Where her quest leads now, in the player's world (null: nowhere, or another world). */
	private static @Nullable BlockPos goal(Minecraft mc) {
		ExiledPixieData data = data();
		if (data.stage != ExiledPixieData.FRIEND || data.current() == null || data.target == null || mc.level == null
			|| !mc.level.dimension().identifier().toString().equals(data.targetLevel)) {
			return null;
		}
		return data.goal();
	}

	/** The hidden chest she shows, while she shows it. */
	private static @Nullable BlockPos chest(Minecraft mc) {
		CompoundTag extra = MessageExiledSync.extra;
		BlockPos hint = extra.getLong("Hint").map(BlockPos::of).orElse(null);
		return hint != null && mc.level != null && extra.getLongOr("HintUntil", 0L) > mc.level.getGameTime()
			&& mc.level.dimension().identifier().toString().equals(extra.getStringOr("HintLevel", "")) ? hint : null;
	}

	/** Her beam over her quest's place. */
	private static void beam(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector buffers, float partial) {
		Minecraft mc = Minecraft.getInstance();
		BlockPos goal = goal(mc);
		if (goal == null || pose == null || buffers == null) {
			return;
		}
		double bx = goal.getX() + 0.5;
		double bz = goal.getZ() + 0.5;
		double dx = bx - cam.x;
		double dz = bz - cam.z;
		double d = Math.sqrt(dx * dx + dz * dz);
		if (d < 6) {
			return;
		}
		// too far to be seen: the beam stands on the way to it, at the edge of what the player sees
		double reach = Math.max(48, mc.options.getEffectiveRenderDistance() * 16 - 24);
		if (d > reach) {
			bx = cam.x + dx / d * reach;
			bz = cam.z + dz / d * reach;
		}
		int c = color();
		int col = ARGB.colorFromFloat(1F, ((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F);
		int base = d > reach ? mc.level.getMinY() : goal.getY();
		pose.pushPose();
		pose.translate(bx - 0.5 - cam.x, base - cam.y, bz - 0.5 - cam.z);
		BeaconRenderer.submitBeaconBeam(pose, buffers, BeaconRenderer.BEAM_LOCATION, 1.0F, Math.floorMod(mc.level.getGameTime(), 40) + partial, 0,
			(mc.level.getMaxY() + 1) - base, col, 0.2F, 0.3F);
		pose.popPose();
	}

	public static void hud(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null || mc.gui.hud.isHidden() || !framed || MemoryOverlay.showing()) {
			return;
		}
		float partial = delta.getGameTimeDeltaPartialTick(false);
		BlockPos goal = goal(mc);
		if (goal != null) {
			int y = MessageExiledSync.extra.getIntOr("TargetY", goal.getY());
			pin(g, mc, new Vec3(goal.getX() + 0.5, y + 2.6, goal.getZ() + 0.5), color(), 3, partial);
		}
		BlockPos chest = chest(mc);
		if (chest != null) {
			pin(g, mc, new Vec3(chest.getX() + 0.5, chest.getY() + 1.8, chest.getZ() + 0.5), 0xF2C14E, 4, partial);
		}
	}

	/** A cell of the sheet as a square of this size at (x, y), tinted. */
	private static void cell(GuiGraphicsExtractor g, float x, float y, float size, int col, int tint) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		float k = size / CELL;
		g.pose().scale(k, k);
		g.blit(RenderPipelines.GUI_TEXTURED, TEX, 0, 0, col * CELL, 0, CELL, CELL, CELL, CELL, TEX_W, TEX_H, tint);
		g.pose().popMatrix();
	}

	/** A pin over a place (or at the edge of the screen with an arrow toward it), the distance under it. */
	private static void pin(GuiGraphicsExtractor g, Minecraft mc, Vec3 at, int rgb, int symbol, float partial) {
		int sw = g.guiWidth();
		int sh = g.guiHeight();
		Vector4f eye = new Vector4f((float) (at.x - cam.x), (float) (at.y - cam.y), (float) (at.z - cam.z), 1F);
		VIEW.transform(eye);
		Vector4f v = new Vector4f(eye);
		PROJ.transform(v);
		boolean behind = v.w() < 0.05F;
		float w = Math.max(0.05F, Math.abs(v.w()));
		float sx = (v.x() / w * 0.5F + 0.5F) * sw;
		float sy = (0.5F - v.y() / w * 0.5F) * sh;
		float m = 24F;
		float top = 26F;
		float bottom = sh - 56F;
		float cx = sw / 2F;
		float cy = sh / 2F;
		boolean off = behind || sx < m || sx > sw - m || sy < top || sy > bottom;
		float angle = 0F;
		if (off) {
			// out of sight: toward its side from the player's eyes (right behind them: the bottom)
			float dx = eye.x();
			float dy = -eye.y();
			if (!behind) {
				dx = sx - cx;
				dy = sy - cy;
			} else if (Math.abs(dx) < 0.25F * Math.abs(eye.z()) && Math.abs(dy) < 0.25F * Math.abs(eye.z())) {
				dy = Math.abs(eye.z());
			}
			float kx = Math.abs(dx) < 1.0E-3F ? Float.MAX_VALUE : (dx > 0 ? sw - m - cx : cx - m) / Math.abs(dx);
			float ky = Math.abs(dy) < 1.0E-3F ? Float.MAX_VALUE : (dy > 0 ? bottom - cy : cy - top) / Math.abs(dy);
			float k = Math.min(kx, ky);
			sx = cx + dx * k;
			sy = cy + dy * k;
			angle = (float) Math.atan2(dy, dx);
		}
		double ddx = at.x - mc.player.getX();
		double ddz = at.z - mc.player.getZ();
		double dist = Math.sqrt(ddx * ddx + ddz * ddz);
		// right at it, the pin fades away
		float alpha = Mth.clamp((float) ((dist - 2.0) / 3.0), 0F, 1F);
		if (alpha <= 0F) {
			return;
		}
		// over the crosshair it lets the player see through it a little
		if (!off && Math.abs(sx - cx) < 14 && Math.abs(sy - PIN * 0.5F - cy) < PIN * 0.7F) {
			alpha *= 0.7F;
		}
		int a = Math.max(8, (int) (alpha * 255));
		int tint = (a << 24) | (rgb & 0xFFFFFF);
		float px = sx;
		float tip = sy;
		if (off) {
			g.pose().pushMatrix();
			g.pose().translate(sx, sy);
			g.pose().rotate(angle);
			cell(g, -9F, -9F, 18F, 2, tint);
			g.pose().popMatrix();
			px = sx - Mth.cos(angle) * 18F;
			tip = sy - Mth.sin(angle) * 18F + PIN * (TIP_AT - BALL_AT);
		}
		float x0 = px - PIN / 2F;
		float y0 = tip - PIN * TIP_AT;
		// its outline, the pin in her color, her mark on it (darker)
		cell(g, x0, y0, PIN, 1, (a << 24) | 0xFFFFFF);
		cell(g, x0, y0, PIN, 0, tint);
		int dark = ((int) (((rgb >> 16) & 255) * 0.42F) << 16) | ((int) (((rgb >> 8) & 255) * 0.42F) << 8) | (int) ((rgb & 255) * 0.42F);
		cell(g, x0, y0, PIN, symbol, (a << 24) | dark);
		// the distance under it (out of sight: on the pin's inner side)
		String d = (int) dist + " m";
		int ix = (int) px;
		int iy = (int) tip + 2;
		if (off) {
			float ox = -Mth.cos(angle);
			float oy = -Mth.sin(angle);
			float ball = tip - PIN * (TIP_AT - BALL_AT);
			ix = (int) (px + ox * (PIN * 0.7F + mc.font.width(d) / 2F * Math.abs(ox)));
			iy = (int) (ball + oy * PIN * 0.75F - 4);
		}
		g.text(mc.font, d, ix - mc.font.width(d) / 2, iy, (a << 24) | 0xFFFFFF, true);
	}
}
