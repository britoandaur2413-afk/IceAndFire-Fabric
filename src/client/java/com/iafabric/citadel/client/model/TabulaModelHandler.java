package com.iafabric.citadel.client.model;

import com.google.gson.Gson;
import com.iafabric.citadel.client.model.container.TabulaModelContainer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Reads the model.json of a Tabula model. */
public enum TabulaModelHandler {
	INSTANCE;

	private final Gson gson = new Gson();

	public TabulaModelContainer loadTabulaModel(InputStream stream) {
		return gson.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), TabulaModelContainer.class);
	}
}
