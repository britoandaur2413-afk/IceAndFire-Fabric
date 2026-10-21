package com.iafabric.config.biome;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
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
	/** Old Forge dictionary / biome category names, as convention tags. */
	private static final Map<String, String> LEGACY_TAGS = Map.ofEntries(
		Map.entry("overworld", "c:is_overworld"), Map.entry("ocean", "c:is_ocean"), Map.entry("beach", "c:is_beach"),
		Map.entry("snowy", "c:is_snowy"), Map.entry("icy", "c:is_icy"), Map.entry("forest", "c:is_forest"),
		Map.entry("mountain", "c:is_mountain"), Map.entry("taiga", "c:is_taiga"), Map.entry("river", "c:is_river"),
		Map.entry("jungle", "c:is_jungle"), Map.entry("savanna", "c:is_savanna"), Map.entry("desert", "c:is_desert"),
		Map.entry("swamp", "c:is_swamp"), Map.entry("plains", "c:is_plains"), Map.entry("mesa", "c:is_badlands"),
		Map.entry("extreme_hills", "c:is_windswept"), Map.entry("mushroom", "c:is_mushroom"), Map.entry("nether", "c:is_nether"),
		Map.entry("the_end", "c:is_end"), Map.entry("underground", "c:is_underground"), Map.entry("hot", "c:is_hot"),
		Map.entry("cold", "c:is_cold"), Map.entry("wet", "c:is_wet"), Map.entry("dry", "c:is_dry"), Map.entry("sandy", "c:is_sandy"),
		Map.entry("hills", "c:is_hill"), Map.entry("dense", "c:is_vegetation_dense"), Map.entry("sparse", "c:is_vegetation_sparse"),
		Map.entry("spooky", "c:is_spooky"), Map.entry("dead", "c:is_dead"), Map.entry("lush", "c:is_lush"), Map.entry("magical", "c:is_magical"),
		Map.entry("rare", "c:is_rare"), Map.entry("plateau", "c:is_plateau"), Map.entry("wasteland", "c:is_wasteland"),
		Map.entry("water", "c:is_aquatic"), Map.entry("coniferous", "c:is_taiga"));

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
		Identifier registryName = biomeHolder.unwrapKey().map(k -> k.identifier()).orElse(null);
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

		boolean matches(Holder<Biome> biomeHolder, Identifier registryName) {
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
