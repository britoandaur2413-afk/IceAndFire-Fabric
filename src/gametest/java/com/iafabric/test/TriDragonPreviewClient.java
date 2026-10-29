package com.iafabric.test;

import com.iafabric.citadel.client.model.AdvancedModelBox;
import com.iafabric.citadel.client.model.ITabulaModelAnimator;
import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.model.animator.IceDragonTabulaModelAnimator;
import com.iafabric.client.model.util.TabulaModelHandlerHelper;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.render.entity.RenderDragonBase;
import com.iafabric.client.render.entity.layer.LayerDragonEyes;
import com.iafabric.entity.EntityDragonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.IOException;

/**
 * Draws the preview dragon with the three-headed dragon's model and texture, a quarter bigger than the dragon of its
 * age (the eyes' layer is the ice dragon's: left out).
 */
public final class TriDragonPreviewClient implements ClientModInitializer {
	public static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/tridragon/tridragon.png");
	public static final Identifier EYES = Identifier.parse("iceandfire_fabric:textures/models/tridragon/tridragon_eyes.png");
	/** How much bigger than a dragon of the same age. */
	public static final float BIGGER = 1.25F;

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(TriDragonPreview.TYPE, Renderer::new);
	}

	/**
	 * Moves it as an ice dragon moves: an ice dragon's model is posed for it (all its poses and animations), and each of
	 * its pieces moves as much from its own rest as the piece of that name (its side necks' as the neck they were made
	 * from) moves from the ice dragon's rest. Its arms are its front wings: in the air they open as a dragon's do (what
	 * makes them its long arms on the ground, and its body raised on them, fades as it rises). Pieces of its own (its
	 * hands, crystals, orbs) and its back wings hold.
	 */
	static final class Animator implements ITabulaModelAnimator<EntityDragonBase> {
		private final TabulaModel skeleton;

		Animator() {
			try {
				this.skeleton = new TabulaModel(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/icedragon/icedragon_ground"),
					new IceDragonTabulaModelAnimator());
			} catch (IOException e) {
				throw new IllegalStateException("the ice dragon's model", e);
			}
		}

		@Override
		public void setRotationAngles(TabulaModel model, EntityDragonBase entity, float limbSwing, float limbSwingAmount, float ageInTicks,
									  float rotationYaw, float rotationPitch, float scale) {
			this.skeleton.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, rotationYaw, rotationPitch);
			float air = Mth.clamp(Math.max(entity.flyProgress, entity.hoverProgress) / 20F, 0F, 1F);
			for (AdvancedModelBox cube : model.getCubes().values()) {
				String name = bone(cube.boxName);
				AdvancedModelBox bone = name == null ? null : this.skeleton.getCube(name);
				if (bone == null) {
					continue;
				}
				float own = grounded(cube.boxName) ? 1F - air : 1F;
				cube.rotateAngleX = bone.rotateAngleX + (cube.defaultRotationX - bone.defaultRotationX) * own;
				cube.rotateAngleY = bone.rotateAngleY + (cube.defaultRotationY - bone.defaultRotationY) * own;
				cube.rotateAngleZ = bone.rotateAngleZ + (cube.defaultRotationZ - bone.defaultRotationZ) * own;
				cube.rotationPointX = cube.defaultPositionX + bone.rotationPointX - bone.defaultPositionX;
				cube.rotationPointY = cube.defaultPositionY + bone.rotationPointY - bone.defaultPositionY;
				cube.rotationPointZ = cube.defaultPositionZ + bone.rotationPointZ - bone.defaultPositionZ;
				cube.offsetX = cube.defaultOffsetX + bone.offsetX - bone.defaultOffsetX;
				cube.offsetY = cube.defaultOffsetY + bone.offsetY - bone.defaultOffsetY;
				cube.offsetZ = cube.defaultOffsetZ + bone.offsetZ - bone.defaultOffsetZ;
			}
		}

		/**
		 * The ice dragon's piece this one moves as (its side necks: R_ and L_; its middle neck's extra joint: M_); its back
		 * wings (B_) hold still for now.
		 */
		private static String bone(String name) {
			if (name.length() > 2 && name.charAt(1) == '_') {
				return "RLM".indexOf(name.charAt(0)) >= 0 ? name.substring(2) : null;
			}
			return name;
		}

		/** Its pieces posed for the ground only (its arms as its long front legs, its body raised on them, its hind legs). */
		private static boolean grounded(String name) {
			return name.startsWith("arm") || name.startsWith("finger") || name.equals("BodyUpper") || name.startsWith("Thigh");
		}
	}

	static final class Renderer extends RenderDragonBase {
		Renderer(EntityRendererProvider.Context context) {
			super(context, model(), 1);
			this.layers.removeIf(layer -> layer instanceof LayerDragonEyes);
			// what shines on it: each head's eyes (as its dragon's), its crystals and orbs, its wings' veins
			this.addLayer(new IafRenderLayer<>(this) {
				@Override
				public void render(PoseStack poseStack, BufferSource buffers, int light, EntityDragonBase dragon, float limbSwing, float limbSwingAmount,
								   float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
					if (dragon.shouldRenderEyes()) {
						this.getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderTypes.eyes(EYES)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
					}
				}
			});
		}

		private static TabulaModel model() {
			try {
				return new TabulaModel(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/tridragon/tridragon_ground"),
					new Animator());
			} catch (IOException e) {
				throw new IllegalStateException("the three-headed dragon's model", e);
			}
		}

		@Override
		public Identifier getTextureLocation(EntityDragonBase entity) {
			return TEXTURE;
		}

		@Override
		protected void scale(EntityDragonBase entity, PoseStack poseStack, float partialTicks) {
			super.scale(entity, poseStack, partialTicks);
			poseStack.scale(BIGGER, BIGGER, BIGGER);
		}
	}
}
