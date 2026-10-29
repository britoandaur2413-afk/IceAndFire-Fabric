package com.iafabric.config.biome;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.biome.Biome;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which biomes something spawns in: a list of pools, each a list of conditions that must all hold.
 * Old "dictionary" and "category" entries are read as the matching convention biome tags.
 */
public class IafSpawnBiomeData {
	private static final int CITADEL_FORMAT = 0;
	private static final String CITADEL_FORMAT_STRING = "citadel_format";
	/** Old Forge dictionary / biome category names, as 1.20.1 convention tags (the nearest one where it has none of the name). */
	private static final Map<String, String> LEGACY_TAGS = Map.ofEntries(
		Map.entry("overworld", "c:in_overworld"), Map.entry("ocean", "c:ocean"), Map.entry("beach", "c:beach"),
		Map.entry("snowy", "c:snowy"), Map.entry("icy", "c:icy"), Map.entry("forest", "c:forest"),
		Map.entry("mountain", "c:mountain"), Map.entry("taiga", "c:taiga"), Map.entry("river", "c:river"),
		Map.entry("jungle", "c:jungle"), Map.entry("savanna", "c:savanna"), Map.entry("desert", "c:desert"),
		Map.entry("swamp", "c:swamp"), Map.entry("plains", "c:plains"), Map.entry("mesa", "c:badlands"),
		Map.entry("extreme_hills", "c:windswept"), Map.entry("mushroom", "c:mushroom"), Map.entry("nether", "c:in_nether"),
		Map.entry("the_end", "c:in_the_end"), Map.entry("underground", "c:underground"), Map.entry("hot", "c:climate_hot"),
		Map.entry("cold", "c:climate_cold"), Map.entry("wet", "c:climate_wet"), Map.entry("dry", "c:climate_dry"), Map.entry("sandy", "c:desert"),
		Map.entry("hills", "c:mountain_slope"), Map.entry("dense", "c:vegetation_dense"), Map.entry("sparse", "c:vegetation_sparse"),
		Map.entry("spooky", "c:dead"), Map.entry("dead", "c:dead"), Map.entry("lush", "c:vegetation_dense"), Map.entry("magical", "c:flower_forests"),
		Map.entry("rare", "c:floral"), Map.entry("plateau", "c:mountain"), Map.entry("wasteland", "c:wasteland"),
		Map.entry("water", "c:aquatic"), Map.entry("coniferous", "c:tree_coniferous"));

	private List<List<SpawnBiomeEntry>> biomes = new ArrayList<>();
	private int citadelFormat = CITADEL_FORMAT;

	public IafSpawnBiomeData() {
	}

	private IafSpawnBiomeData(SpawnBiomeEntry[][] biomesRead) {
		biomes = new ArrayList<>();
		for (SpawnBiomeEntry[] innerArray : biomesRead) {
			biomes.add(new ArrayList<>(Arrays.asList(innerArray)));
		}
	}

	public void setCitadelFormat(int format) {
		citadelFormat = format;
	}

	public IafSpawnBiomeData addBiomeEntry(BiomeEntryType type, boolean negate, String value, int pool) {
		while (biomes.size() < pool + 1) {
			biomes.add(new ArrayList<>());
		}
		biomes.get(pool).add(new SpawnBiomeEntry(type, negate, value));
		return this;
	}

	public boolean matches(Holder<Biome> biomeHolder) {
		ResourceLocation registryName = biomeHolder.unwrapKey().map(k -> k.location()).orElse(null);
		for (List<SpawnBiomeEntry> all : biomes) {
			boolean overall = true;
			for (SpawnBiomeEntry cond : all) {
				if (!cond.matches(biomeHolder, registryName)) {
					overall = false;
					break;
				}
			}
			if (overall && !all.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	public static class Deserializer implements JsonDeserializer<IafSpawnBiomeData>, JsonSerializer<IafSpawnBiomeData> {

		@Override
		public IafSpawnBiomeData deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonobject = json.getAsJsonObject();
			int citadelFormat = GsonHelper.getAsInt(jsonobject, CITADEL_FORMAT_STRING, -1);
			if (citadelFormat != CITADEL_FORMAT) {
				throw new InvalidCitadelFormatException("The file contained the %s format version, while we expected a %s format version".formatted(citadelFormat, CITADEL_FORMAT));
			}
			SpawnBiomeEntry[][] biomesRead = GsonHelper.getAsObject(jsonobject, "biomes", new SpawnBiomeEntry[0][0], context, SpawnBiomeEntry[][].class);
			return new IafSpawnBiomeData(biomesRead);
		}

		@Override
		public JsonElement serialize(IafSpawnBiomeData src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jsonobject = new JsonObject();
			jsonobject.add(CITADEL_FORMAT_STRING, context.serialize(src.citadelFormat));
			jsonobject.add("biomes", context.serialize(src.biomes));
			return jsonobject;
		}
	}

	private static class SpawnBiomeEntry {
		BiomeEntryType type;
		boolean negate;
		String value;

		SpawnBiomeEntry(BiomeEntryType type, boolean negate, String value) {
			this.type = type;
			this.negate = negate;
			this.value = value;
		}

		boolean matches(Holder<Biome> biomeHolder, ResourceLocation registryName) {
			boolean hit;
			if (type == BiomeEntryType.REGISTRY_NAME) {
				hit = registryName != null && registryName.toString().equals(value);
			} else {
				String tag = type == BiomeEntryType.BIOME_TAG ? value : LEGACY_TAGS.get(value.toLowerCase(Locale.ROOT));
				if (tag == null) {
					return negate;
				}
				hit = biomeHolder.tags().anyMatch(t -> t.location().toString().equals(tag));
			}
			return hit != negate;
		}
	}

	static class InvalidCitadelFormatException extends JsonParseException {
		InvalidCitadelFormatException(String s) {
			super(s);
		}
	}
}
