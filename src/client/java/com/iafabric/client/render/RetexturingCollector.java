package com.iafabric.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.function.Function;

/**
 * Passes on only the geometry of what a renderer draws, each part with the render type this gives for it: an
 * entity drawn through its own renderer comes out made of stone (a statue), or as a cracking overlay. Its name
 * and leash are left out.
 */
public final class RetexturingCollector implements MultiBufferSource {
	private static final VertexConsumer NOWHERE = new VertexConsumer() {
		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}
	};

	private final MultiBufferSource delegate;
	private final Function<RenderType, RenderType> renderType;

	public RetexturingCollector(MultiBufferSource delegate, RenderType renderType) {
		this(delegate, any -> renderType);
	}

	/** Each render type the renderer asks for becomes the one this gives (from its texture, for example). */
	public RetexturingCollector(MultiBufferSource delegate, Function<RenderType, RenderType> renderType) {
		this.delegate = delegate;
		this.renderType = renderType;
	}

	@Override
	public VertexConsumer getBuffer(RenderType type) {
		String name = type.toString();
		if (name.contains("[text") || name.contains("[leash") || name.contains("[lines")) {
			return NOWHERE;
		}
		return delegate.getBuffer(renderType.apply(type));
	}
}
