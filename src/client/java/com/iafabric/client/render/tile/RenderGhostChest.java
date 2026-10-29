package com.iafabric.client.render.tile;

import com.iafabric.IafMod;
import com.iafabric.entity.tile.TileEntityGhostChest;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** The ghost chest: a chest with its own (ghostly) textures, drawn as the game draws chests. */
public class RenderGhostChest implements BlockEntityRenderer<TileEntityGhostChest> {
    public static final Material GHOST_CHEST = new Material(Sheets.CHEST_SHEET, IafMod.id("entity/chest/ghost_chest"));
    private static final Material GHOST_CHEST_LEFT = new Material(Sheets.CHEST_SHEET, IafMod.id("entity/chest/ghost_chest_left"));
    private static final Material GHOST_CHEST_RIGHT = new Material(Sheets.CHEST_SHEET, IafMod.id("entity/chest/ghost_chest_right"));
    private final ModelPart[] single;
    private final ModelPart[] left;
    private final ModelPart[] right;

    public RenderGhostChest(BlockEntityRendererProvider.Context context) {
        this.single = parts(context.bakeLayer(ModelLayers.CHEST));
        this.left = parts(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.right = parts(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
    }

    /** Lid, lock and bottom. */
    public static ModelPart[] parts(ModelPart root) {
        return new ModelPart[]{root.getChild("lid"), root.getChild("lock"), root.getChild("bottom")};
    }

    @Override
    public void render(TileEntityGhostChest chest, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Level level = chest.getLevel();
        BlockState state = chest.getBlockState();
        ChestType type = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        if (!(state.getBlock() instanceof AbstractChestBlock<?> block)) {
            return;
        }
        poseStack.pushPose();
        float facing = state.getValue(ChestBlock.FACING).toYRot();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combined = level != null
            ? block.combine(state, level, chest.getBlockPos(), true) : DoubleBlockCombiner.Combiner::acceptNone;
        float open = combined.<Float2FloatFunction>apply(ChestBlock.opennessCombiner(chest)).get(partialTicks);
        open = 1.0F - open;
        open = 1.0F - open * open * open;
        int combinedLight = combined.<Int2IntFunction>apply(new BrightnessCombiner<>()).applyAsInt(light);
        Material material = type == ChestType.LEFT ? GHOST_CHEST_LEFT : type == ChestType.RIGHT ? GHOST_CHEST_RIGHT : GHOST_CHEST;
        ModelPart[] model = type == ChestType.LEFT ? left : type == ChestType.RIGHT ? right : single;
        renderParts(poseStack, material.buffer(buffers, RenderType::entityCutoutNoCull), model, open, combinedLight, overlay);
        poseStack.popPose();
    }

    /** The lid opened by this much (0 to 1), its lock with it, over the bottom. */
    public static void renderParts(PoseStack poseStack, VertexConsumer buffer, ModelPart[] parts, float open, int light, int overlay) {
        parts[0].xRot = -(open * ((float) Math.PI / 2F));
        parts[1].xRot = parts[0].xRot;
        parts[0].render(poseStack, buffer, light, overlay);
        parts[1].render(poseStack, buffer, light, overlay);
        parts[2].render(poseStack, buffer, light, overlay);
    }
}
