package com.iafabric.pixie;

import com.iafabric.IafMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.RandomSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The twenty exiled pixies (tools/gen_exiled_pixies.py paints them): a name and the colors of each, faded half to
 * white. Which one a new world gets goes round: none comes again before all twenty have come (kept in
 * config/iceandfire_fabric/exiled_pixies.json, for every world of this game).
 */
public final class ExiledVariants {
	public static final int COUNT = 20;
	/** Their names (the same in every language). */
	public static final String[] NAMES = {"Albia", "Brisa", "Ceniza", "Lumi", "Selene", "Opala", "Nácar", "Bruma", "Escarcha", "Marfil",
		"Lirio", "Rocío", "Alba", "Perla", "Vela", "Hiedra", "Sauce", "Cirro", "Azahar", "Ondina"};
	/** Her dress's color as a pixie's would be (her name, her dust, the borders of her menu). */
	public static final int[] DRESS = {0xF29AB8, 0x8EDDBE, 0xB5AEF0, 0xEED27E, 0xA6B9F2, 0xE3A8E0, 0xF6BE9C, 0xCBCBD6, 0x8FDDEB, 0xE3D3B0,
		0xCDA9EE, 0xC8E68E, 0xF4A390, 0xD9AFC2, 0xEEE58E, 0xA8E3A0, 0xBFD3A9, 0x9CCBF0, 0xF7C68A, 0x8FD6CF};
	/** Her eyes' color, the one strong color left in her. */
	public static final int[] EYES = {0xE0508A, 0x2FA57C, 0x6F5CC9, 0xD49A12, 0x4C66D1, 0x1FA9C2, 0xD9823F, 0x6E7C9A, 0x16A9C6, 0xB08A2E,
		0x9C4FD1, 0x5BA82E, 0xDE5E3E, 0xB85878, 0xCFA00E, 0x3E9E5A, 0x6F9142, 0x2F72CC, 0xDB7F22, 0x23918C};

	private ExiledVariants() {
	}

	public static int of(int variant) {
		return Math.floorMod(variant, COUNT);
	}

	public static String name(int variant) {
		return NAMES[of(variant)];
	}

	/** Her dust's color, paler than her dress (red, green, blue from 0 to 1). */
	public static float[] dust(int variant) {
		int c = DRESS[of(variant)];
		return new float[]{(0.55F + 0.45F * ((c >> 16) & 255) / 255F), (0.55F + 0.45F * ((c >> 8) & 255) / 255F), (0.55F + 0.45F * (c & 255) / 255F)};
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(IafMod.MOD_ID).resolve("exiled_pixies.json");
	}

	/** The ones that have come since the round began. */
	static synchronized Set<Integer> seen() {
		Set<Integer> seen = new LinkedHashSet<>();
		try {
			Path file = file();
			if (Files.exists(file)) {
				JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
				for (var element : json.getAsJsonArray("seen")) {
					int v = element.getAsInt();
					if (v >= 0 && v < COUNT) {
						seen.add(v);
					}
				}
			}
		} catch (IOException | RuntimeException e) {
			IafMod.LOGGER.warn("Could not read the exiled pixies' round: {}", e.toString());
		}
		return seen;
	}

	private static void save(Set<Integer> seen) {
		JsonObject json = new JsonObject();
		json.addProperty("about", "The exiled pixies met so far in this round (one per world; none comes again before all twenty have come).");
		JsonArray list = new JsonArray();
		seen.forEach(list::add);
		json.add("seen", list);
		try {
			Path file = file();
			Files.createDirectories(file.getParent());
			Files.writeString(file, json.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			IafMod.LOGGER.warn("Could not keep the exiled pixies' round: {}", e.toString());
		}
	}

	/**
	 * The next one to meet: one that has not come in this round (a new round begins when all twenty have, without
	 * the last one first).
	 */
	public static synchronized int next(RandomSource random) {
		Set<Integer> seen = seen();
		List<Integer> left = new ArrayList<>();
		for (int i = 0; i < COUNT; i++) {
			if (!seen.contains(i)) {
				left.add(i);
			}
		}
		if (left.isEmpty()) {
			int last = seen.isEmpty() ? -1 : new ArrayList<>(seen).get(seen.size() - 1);
			seen.clear();
			for (int i = 0; i < COUNT; i++) {
				if (i != last) {
					left.add(i);
				}
			}
		}
		int chosen = left.get(random.nextInt(left.size()));
		seen.add(chosen);
		save(seen);
		return chosen;
	}
}
