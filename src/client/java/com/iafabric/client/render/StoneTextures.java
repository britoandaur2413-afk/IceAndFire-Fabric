package com.iafabric.client.render;

import com.iafabric.IafMod;
import com.iafabric.client.texture.Pixels;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A creature's own texture turned to stone, for statues: stone grain where the texture has paint, the
 * texture's light and dark kept as shading, and its transparent parts (a warden's antennae, wings, hair)
 * left transparent. Made once per texture.
 */
public final class StoneTextures {
	private static final ResourceLocation STONE = ResourceLocation.withDefaultNamespace("textures/block/stone.png");
	private static final Map<ResourceLocation, Optional<ResourceLocation>> MADE = new HashMap<>();
	private static @Nullable NativeImage stone;

	private StoneTextures() {
	}

	/** The stone version of the texture a render type draws with, or empty when it has none to read. */
	public static Optional<ResourceLocation> of(RenderType type) {
		return textureOf(type).flatMap(StoneTextures::of);
	}

	public static Optional<ResourceLocation> of(ResourceLocation original) {
		return MADE.computeIfAbsent(original, StoneTextures::make);
	}

	private static Optional<ResourceLocation> textureOf(RenderType type) {
		try {
			var state = ((com.iafabric.mixin.client.CompositeRenderTypeAccessor) type).iceandfire_fabric$state();
			var texture = ((com.iafabric.mixin.client.CompositeStateAccessor) (Object) state).iceandfire_fabric$textureState();
			return ((com.iafabric.mixin.client.TextureStateInvoker) texture).iceandfire_fabric$cutoutTexture();
		} catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	private static Optional<ResourceLocation> make(ResourceLocation original) {
		NativeImage source = load(original);
		NativeImage grain = stone();
		if (source == null || grain == null) {
			return Optional.empty();
		}
		int width = source.getWidth();
		int height = source.getHeight();
		NativeImage out = new NativeImage(width, height, true);
		// Stone grain at the texture's own scale (a 64-wide texture shows the 16-pixel stone four times).
		int scale = Math.max(1, width / 64);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int argb = Pixels.argb(source, x, y);
				int alpha = argb >>> 24;
				if (alpha < 8) {
					Pixels.setArgb(out, x, y, 0);
					continue;
				}
				double light = (0.3 * ((argb >> 16) & 255) + 0.59 * ((argb >> 8) & 255) + 0.11 * (argb & 255)) / 255.0;
				double shade = 0.55 + 0.6 * light;
				int s = Pixels.argb(grain, (x / scale) % grain.getWidth(), (y / scale) % grain.getHeight());
				int r = (int) Math.min(255, ((s >> 16) & 255) * shade);
				int g = (int) Math.min(255, ((s >> 8) & 255) * shade);
				int b = (int) Math.min(255, (s & 255) * shade);
				Pixels.setArgb(out, x, y, alpha << 24 | r << 16 | g << 8 | b);
			}
		}
		ResourceLocation made = IafMod.id("stone_statue/" + original.getNamespace() + "/" + original.getPath());
		Minecraft.getInstance().getTextureManager().register(made, new DynamicTexture(out));
		return Optional.of(made);
	}

	private static @Nullable NativeImage stone() {
		if (stone == null) {
			stone = load(STONE);
		}
		return stone;
	}

	/** A texture's pixels: from the resources, or from a texture made in game (the dragons' layered ones). */
	private static @Nullable NativeImage load(ResourceLocation id) {
		var resource = Minecraft.getInstance().getResourceManager().getResource(id);
		if (resource.isPresent()) {
			try (InputStream in = resource.get().open()) {
				return NativeImage.read(in);
			} catch (Exception e) {
				IafMod.LOGGER.warn("Could not read {} for a statue", id);
				return null;
			}
		}
		AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(id);
		if (texture instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
			NativeImage pixels = dynamic.getPixels();
			NativeImage copy = new NativeImage(pixels.getWidth(), pixels.getHeight(), true);
			for (int y = 0; y < pixels.getHeight(); y++) {
				for (int x = 0; x < pixels.getWidth(); x++) {
					Pixels.setArgb(copy, x, y, Pixels.argb(pixels, x, y));
				}
			}
			return copy;
		}
		return null;
	}
}
