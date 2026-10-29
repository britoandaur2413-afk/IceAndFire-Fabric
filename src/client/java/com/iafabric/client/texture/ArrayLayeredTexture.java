package com.iafabric.client.texture;

import com.iafabric.IafMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/** A texture made of several pictures drawn one over the other (a dragon's body and its male frills, for example). */
public final class ArrayLayeredTexture {
    private ArrayLayeredTexture() {
    }

    /** Draws the layers into one picture and registers it at {@code location}. */
    public static void register(ResourceLocation location, List<String> layers) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        NativeImage base = null;
        for (String name : layers) {
            if (name == null) {
                continue;
            }
            NativeImage image = read(manager, new ResourceLocation(name));
            if (image == null) {
                continue;
            }
            if (base == null) {
                base = image;
                continue;
            }
            try (image) {
                for (int y = 0; y < Math.min(image.getHeight(), base.getHeight()); y++) {
                    for (int x = 0; x < Math.min(image.getWidth(), base.getWidth()); x++) {
                        blendPixel(base, x, y, Pixels.argb(image, x, y));
                    }
                }
            }
        }
        if (base == null) {
            base = new NativeImage(16, 16, true);
        }
        Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(base));
    }

    private static NativeImage read(ResourceManager manager, ResourceLocation id) {
        Optional<Resource> resource = manager.getResource(id);
        if (resource.isEmpty()) {
            IafMod.LOGGER.warn("Missing texture layer {}", id);
            return null;
        }
        try (InputStream stream = resource.get().open()) {
            return NativeImage.read(stream);
        } catch (IOException e) {
            IafMod.LOGGER.error("Couldn't read texture layer {}", id, e);
            return null;
        }
    }

    /** Draws an ARGB colour over a pixel with its alpha. */
    public static void blendPixel(NativeImage image, int x, int y, int color) {
        int below = Pixels.argb(image, x, y);
        float a = FastColor.ARGB32.alpha(color) / 255.0F;
        float rest = 1.0F - a;
        int alpha = (int) Math.min(255, (a * a + FastColor.ARGB32.alpha(below) / 255.0F * rest) * 255.0F);
        int red = (int) Math.min(255, FastColor.ARGB32.red(color) * a + FastColor.ARGB32.red(below) * rest);
        int green = (int) Math.min(255, FastColor.ARGB32.green(color) * a + FastColor.ARGB32.green(below) * rest);
        int blue = (int) Math.min(255, FastColor.ARGB32.blue(color) * a + FastColor.ARGB32.blue(below) * rest);
        Pixels.setArgb(image, x, y, FastColor.ARGB32.color(alpha, red, green, blue));
    }
}
