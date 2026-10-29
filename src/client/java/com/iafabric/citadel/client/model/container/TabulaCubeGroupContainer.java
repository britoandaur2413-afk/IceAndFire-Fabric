package com.iafabric.citadel.client.model.container;

import java.util.ArrayList;
import java.util.List;

/** A folder of cubes in a Tabula model. */
public class TabulaCubeGroupContainer {
	public String name;
	public String identifier;
	public List<TabulaCubeContainer> cubes = new ArrayList<>();
	public List<TabulaCubeGroupContainer> cubeGroups = new ArrayList<>();

	public List<TabulaCubeContainer> getCubes() {
		return cubes;
	}

	public List<TabulaCubeGroupContainer> getCubeGroups() {
		return cubeGroups;
	}
}
