package com.iafabric.client.render.tile;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityGhostChest;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;

/** The ghost chest: a chest with its own (ghostly) textures. */
public class RenderGhostChest extends ChestRenderer<TileEntityGhostChest> {
    private static final SpriteId GHOST_CHEST = new SpriteId(Sheets.CHEST_SHEET, IafMod.id("entity/chest/ghost_chest"));
    private static final SpriteId GHOST_CHEST_LEFT = new SpriteId(Sheets.CHEST_SHEET, IafMod.id("entity/chest/ghost_chest_left"));
    private static final SpriteId GHOST_CHEST_RIGHT = new SpriteId(Sheets.CHEST_SHEET, IafMod.id("entity/chest/ghost_chest_right"));
    private final SpriteGetter sprites;
    private final ChestModel single;
    private final ChestModel left;
    private final ChestModel right;

    public RenderGhostChest(BlockEntityRendererProvider.Context context) {
        super(context);
        this.sprites = context.sprites();
        this.single = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.left = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.right = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
    }

    @Override
    public void submit(ChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(modelTransformation(state.facing));
        float open = 1.0F - state.open;
        open = 1.0F - open * open * open;
        SpriteId sprite;
        ChestModel model;
        switch (state.type) {
            case LEFT -> {
                sprite = GHOST_CHEST_LEFT;
                model = left;
            }
            case RIGHT -> {
                sprite = GHOST_CHEST_RIGHT;
                model = right;
            }
            default -> {
                sprite = GHOST_CHEST;
                model = single;
            }
        }
        collector.submitModel(model, open, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, sprite, this.sprites, 0);
        poseStack.popPose();
    }
}
