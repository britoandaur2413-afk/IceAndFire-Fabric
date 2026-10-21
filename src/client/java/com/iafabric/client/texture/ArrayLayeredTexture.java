package com.iafabric.client.texture;

import com.iafabric.IafMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/** A texture made of several pictures drawn one over the other (a dragon's body and its male frills, for example). */
public final class ArrayLayeredTexture {
    private ArrayLayeredTexture() {
    }

    /** Draws the layers into one picture and registers it at {@code location}. */
    public static void register(Identifier location, List<String> layers) {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        NativeImage base = null;
        for (String name : layers) {
            if (name == null) {
                continue;
            }
            NativeImage image = read(manager, Identifier.parse(name));
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
                        blendPixel(base, x, y, image.getPixel(x, y));
                    }
                }
            }
        }
        if (base == null) {
            base = new NativeImage(16, 16, true);
        }
        Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(location::toString, base));
    }

    private static NativeImage read(ResourceManager manager, Identifier id) {
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
        int below = image.getPixel(x, y);
        float a = ARGB.alpha(color) / 255.0F;
        float rest = 1.0F - a;
        int alpha = (int) Math.min(255, (a * a + ARGB.alpha(below) / 255.0F * rest) * 255.0F);
        int red = (int) Math.min(255, ARGB.red(color) * a + ARGB.red(below) * rest);
        int green = (int) Math.min(255, ARGB.green(color) * a + ARGB.green(below) * rest);
        int blue = (int) Math.min(255, ARGB.blue(color) * a + ARGB.blue(below) * rest);
        image.setPixel(x, y, ARGB.color(alpha, red, green, blue));
    }
}
