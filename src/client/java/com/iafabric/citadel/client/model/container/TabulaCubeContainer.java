package com.iafabric.citadel.client.model.container;

import java.util.ArrayList;
import java.util.List;

/** One cube of a Tabula model, with its children. */
public class TabulaCubeContainer {
	public String name;
	public String identifier;
	public int[] dimensions = new int[3];
	public double[] position = new double[3];
	public double[] offset = new double[3];
	public double[] rotation = new double[3];
	public double[] scale = new double[]{1.0, 1.0, 1.0};
	public int[] txOffset = new int[2];
	public boolean txMirror;
	public double mcScale;
	public double opacity = 100.0;
	public boolean hidden;
	public List<TabulaCubeContainer> children = new ArrayList<>();

	public String getName() {
		return name;
	}

	public String getIdentifier() {
		return identifier;
	}

	public int[] getDimensions() {
		return dimensions;
	}

	public double[] getPosition() {
		return position;
	}

	public double[] getOffset() {
		return offset;
	}

	public double[] getRotation() {
		return rotation;
	}

	public double[] getScale() {
		return scale;
	}

	public int[] getTextureOffset() {
		return txOffset;
	}

	public boolean isTextureMirrorEnabled() {
		return txMirror;
	}

	public double getMcScale() {
		return mcScale;
	}

	public boolean isHidden() {
		return hidden;
	}

	public List<TabulaCubeContainer> getChildren() {
		return children;
	}
}
