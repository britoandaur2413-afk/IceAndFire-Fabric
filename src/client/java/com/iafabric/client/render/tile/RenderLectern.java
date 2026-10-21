package com.iafabric.client.render.tile;

import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.iafabric.client.render.BufferSource;
import com.iafabric.block.BlockLectern;
import com.iafabric.entity.tile.TileEntityLectern;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class RenderLectern<T extends TileEntityLectern> extends IafBlockEntityRenderer<T> {

    private static final RenderType ENCHANTMENT_TABLE_BOOK_TEXTURE = RenderTypes.entityCutout(Identifier.parse("iceandfire_fabric:textures/models/lectern_book.png"));
    private final BookModel bookModel;

    public RenderLectern(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(T entity, float partialTicks, PoseStack matrixStackIn, BufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
        TileEntityLectern lectern = entity;
        matrixStackIn.pushPose();
        matrixStackIn.translate(0.5F, 1.1F, 0.5F);
        matrixStackIn.scale(0.8F, 0.8F, 0.8F);
        matrixStackIn.rotateDegrees(Axis.YP, (this.getRotation(lectern)));
        matrixStackIn.rotateDegrees(Axis.XP, (112));
        matrixStackIn.rotateDegrees(Axis.YP, (90));
        float f4 = lectern.pageFlipPrev + (lectern.pageFlip - lectern.pageFlipPrev) * partialTicks + 0.25F;
        float f5 = lectern.pageFlipPrev + (lectern.pageFlip - lectern.pageFlipPrev) * partialTicks + 0.75F;
        f4 = (f4 - Mth.floor(f4)) * 1.6F - 0.3F;
        f5 = (f5 - Mth.floor(f5)) * 1.6F - 0.3F;

        if (f4 < 0.0F) {
            f4 = 0.0F;
        }

        if (f5 < 0.0F) {
            f5 = 0.0F;
        }

        if (f4 > 1.0F) {
            f4 = 1.0F;
        }

        if (f5 > 1.0F) {
            f5 = 1.0F;
        }
        float f6 = 1.29F;

        BookModel.State book = BookModel.State.forAnimation(partialTicks, Mth.clamp(f4, 0.0F, 1.0F), Mth.clamp(f5, 0.0F, 1.0F), f6);
        bufferIn.collector().submitModel(this.bookModel, book, matrixStackIn, ENCHANTMENT_TABLE_BOOK_TEXTURE, combinedLightIn, combinedOverlayIn, -1, null, 0);
        matrixStackIn.popPose();
    }

    private float getRotation(TileEntityLectern lectern) {
        switch (lectern.getBlockState().getValue(BlockLectern.FACING)) {
            default:
                return 180;
            case EAST:
                return 90;
            case WEST:
                return -90;
            case SOUTH:
                return 0;

        }
    }

}
