package com.iafabric.citadel.client.model.container;

import java.util.ArrayList;
import java.util.List;

/** The model.json inside a Tabula .tbl file (read with Gson). */
public class TabulaModelContainer {
	public String modelName;
	public int textureWidth = 64;
	public int textureHeight = 32;
	public double[] scale = new double[]{1.0, 1.0, 1.0};
	public List<TabulaCubeGroupContainer> cubeGroups = new ArrayList<>();
	public List<TabulaCubeContainer> cubes = new ArrayList<>();

	public int getTextureWidth() {
		return textureWidth;
	}

	public int getTextureHeight() {
		return textureHeight;
	}

	public List<TabulaCubeContainer> getCubes() {
		return cubes;
	}

	public List<TabulaCubeGroupContainer> getCubeGroups() {
		return cubeGroups;
	}
}
