package com.iafabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Passes on only the geometry of what a renderer draws, all of it with one render type: an entity drawn
 * through its own renderer comes out made of stone (a statue), or as a cracking overlay. Names, shadows,
 * flames, held items and the rest are left out.
 */
public final class RetexturingCollector implements SubmitNodeCollector {
	private final SubmitNodeCollector delegate;
	private final java.util.function.Function<RenderType, RenderType> renderType;

	public RetexturingCollector(SubmitNodeCollector delegate, RenderType renderType) {
		this(delegate, any -> renderType);
	}

	/** Each render type the renderer asks for becomes the one this gives (from its texture, for example). */
	public RetexturingCollector(SubmitNodeCollector delegate, java.util.function.Function<RenderType, RenderType> renderType) {
		this.delegate = delegate;
		this.renderType = renderType;
	}

	@Override
	public OrderedSubmitNodeCollector order(int order) {
		return this;
	}

	@Override
	public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
	}

	@Override
	public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords,
		CameraRenderState camera) {
	}

	@Override
	public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode,
		int lightCoords, int color, int backgroundColor, int outlineColor) {
	}

	@Override
	public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode displayMode, int lightCoords) {
	}

	@Override
	public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
	}

	@Override
	public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
	}

	@Override
	public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType type, int lightCoords, int overlayCoords, int tintedColor,
		@Nullable UvMapping uvMapping, int outlineColor) {
		delegate.submitModel(model, state, poseStack, this.renderType.apply(type), lightCoords, overlayCoords, 0xFFFFFFFF, null, 0);
	}

	@Override
	public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType type, int lightCoords, int overlayCoords,
		int tintedColor, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
	}

	@Override
	public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
	}

	@Override
	public void submitBlockModel(PoseStack poseStack, RenderType type, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords,
		int outlineColor) {
	}

	@Override
	public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {
	}

	@Override
	public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType type, int color, float width, boolean afterTerrain) {
	}

	@Override
	public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers,
		ItemQuads quads, ItemStackRenderState.FoilType foilType) {
	}

	@Override
	public void submitCustomGeometry(PoseStack poseStack, RenderType type, CustomGeometryRenderer customGeometryRenderer) {
		delegate.submitCustomGeometry(poseStack, this.renderType.apply(type), customGeometryRenderer);
	}

	@Override
	public void submitQuadParticleGroup(QuadParticleRenderState particles) {
	}

	@Override
	public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
	}
}
