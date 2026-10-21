package com.iafabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iafabric.IafMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A plain JSON file with every public static field of a config class. Missing entries keep their
 * defaults, and the file is written back so new options show up in it.
 */
public final class IafConfigFile {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private IafConfigFile() {
	}

	/** The config under the mod's first id (config/iceandfire_fabric.json and config/iceandfire_fabric/) moved to its own names, once. */
	public static void moveOld() {
		Path dir = FabricLoader.getInstance().getConfigDir();
		move(dir.resolve(com.iafabric.IafMod.STORED + ".json"), dir.resolve(com.iafabric.IafMod.MOD_ID + ".json"));
		move(dir.resolve(com.iafabric.IafMod.STORED), dir.resolve(com.iafabric.IafMod.MOD_ID));
	}

	private static void move(Path from, Path to) {
		if (Files.exists(from) && !Files.exists(to)) {
			try {
				Files.move(from, to);
			} catch (java.io.IOException e) {
				com.iafabric.IafMod.LOGGER.warn("Could not move {} to {}", from, to, e);
			}
		}
	}

	public static void load(Class<?> holder, String fileName) {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(fileName);
		JsonObject json = new JsonObject();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				json = JsonParser.parseReader(reader).getAsJsonObject();
			} catch (Exception e) {
				IafMod.LOGGER.error("Could not read config {}, using defaults", path, e);
			}
		}
		JsonObject out = new JsonObject();
		for (Field field : holder.getFields()) {
			int mod = field.getModifiers();
			if (!Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
				continue;
			}
			try {
				JsonElement value = json.get(field.getName());
				if (value != null) {
					field.set(null, GSON.fromJson(value, field.getGenericType()));
				}
				out.add(field.getName(), GSON.toJsonTree(field.get(null), field.getGenericType()));
			} catch (Exception e) {
				IafMod.LOGGER.warn("Bad config value {} in {}", field.getName(), path, e);
			}
		}
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(out, writer);
			}
		} catch (IOException e) {
			IafMod.LOGGER.error("Could not write config {}", path, e);
		}
	}
}
