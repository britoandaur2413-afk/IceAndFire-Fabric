package com.iafabric.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iafabric.IafMod;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * The blocks drawn cut out or see-through (jars, torches, lilies, resin, ice...): 26.x tells by their textures, 1.21.1
 * must be told. The list (assets/iceandfire_fabric/render_layers.json) is made by tools/gen_render_layers.py.
 */
public final class IafRenderLayers {
	private IafRenderLayers() {
	}

	public static void init() {
		try (var in = IafRenderLayers.class.getResourceAsStream("/assets/iceandfire_fabric/render_layers.json")) {
			if (in == null) {
				IafMod.LOGGER.warn("no render_layers.json: the mod's glass and torches will look solid");
				return;
			}
			JsonObject layers = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			put(layers, "cutout", RenderType.cutout());
			put(layers, "translucent", RenderType.translucent());
		} catch (IOException e) {
			IafMod.LOGGER.error("could not read render_layers.json", e);
		}
	}

	private static void put(JsonObject layers, String name, RenderType type) {
		for (var id : layers.getAsJsonArray(name)) {
			BlockRenderLayerMap.INSTANCE.putBlock(BuiltInRegistries.BLOCK.get(IafMod.id(id.getAsString())), type);
		}
	}
}
