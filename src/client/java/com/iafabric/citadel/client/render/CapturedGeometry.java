package com.iafabric.citadel.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * Vertices recorded right away and drawn later. Entity models share one instance per renderer and
 * are posed per entity, but 26.x draws submitted geometry after every entity was posed, so the
 * posed vertices are captured at submit time and replayed when the frame is drawn.
 */
public final class CapturedGeometry implements VertexConsumer {
	private final FloatArrayList floats = new FloatArrayList();
	private final IntArrayList ints = new IntArrayList();
	private float x, y, z, u, v, nx, ny, nz;
	private int color = -1, overlay, light;
	private boolean pending;

	private void flush() {
		if (pending) {
			floats.add(x);
			floats.add(y);
			floats.add(z);
			floats.add(u);
			floats.add(v);
			floats.add(nx);
			floats.add(ny);
			floats.add(nz);
			ints.add(color);
			ints.add(overlay);
			ints.add(light);
			pending = false;
		}
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		flush();
		this.x = x;
		this.y = y;
		this.z = z;
		this.pending = true;
		return this;
	}

	@Override
	public void addVertex(float x, float y, float z, int color, float u, float v, int overlayCoords, int lightCoords, float nx, float ny, float nz) {
		flush();
		this.x = x;
		this.y = y;
		this.z = z;
		this.color = color;
		this.u = u;
		this.v = v;
		this.overlay = overlayCoords;
		this.light = lightCoords;
		this.nx = nx;
		this.ny = ny;
		this.nz = nz;
		this.pending = true;
	}

	@Override
	public VertexConsumer setColor(int r, int g, int b, int a) {
		this.color = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
		return this;
	}

	@Override
	public VertexConsumer setColor(int color) {
		this.color = color;
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		this.u = u;
		this.v = v;
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		this.overlay = u & 0xFFFF | (v & 0xFFFF) << 16;
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		this.light = u & 0xFFFF | (v & 0xFFFF) << 16;
		return this;
	}

	@Override
	public VertexConsumer setUv3(float u, float v) {
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		this.nx = x;
		this.ny = y;
		this.nz = z;
		return this;
	}

	@Override
	public VertexConsumer setLineWidth(float width) {
		return this;
	}

	public boolean isEmpty() {
		flush();
		return ints.isEmpty();
	}

	/** Sends every recorded vertex to the real buffer. */
	public void replay(VertexConsumer buffer) {
		flush();
		int count = ints.size() / 3;
		for (int i = 0; i < count; i++) {
			int f = i * 8;
			int n = i * 3;
			buffer.addVertex(floats.getFloat(f), floats.getFloat(f + 1), floats.getFloat(f + 2), ints.getInt(n), floats.getFloat(f + 3),
				floats.getFloat(f + 4), ints.getInt(n + 1), ints.getInt(n + 2), floats.getFloat(f + 5), floats.getFloat(f + 6), floats.getFloat(f + 7));
		}
	}
}
