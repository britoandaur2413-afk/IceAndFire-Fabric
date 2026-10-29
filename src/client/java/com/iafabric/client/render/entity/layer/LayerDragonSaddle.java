package com.iafabric.client.render.entity.layer;

import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.citadel.client.model.basic.BasicModelPart;
import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.entity.DragonSaddle;
import com.iafabric.entity.DragonSeatPose;
import com.iafabric.entity.EntityDragonBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.EnumSet;

/**
 * The double saddle on a grown dragon's back (DragonSaddle): a saddle cloth on each bone of its back, a leather seat
 * on each (a cushion, its back and its horn) where its riders sit, and a girth strap round its body under each seat.
 * The pieces on BodyUpper and on BodyLower move with them, as the model is posed. In model units, with the dragon's
 * size; two texels a unit, as its own skin and armor (their 512x256 pictures on a 256x128 model): every size is in
 * half units, so each texel falls whole. No two faces lie in one plane: each piece stands off the body and the others.
 */
public class LayerDragonSaddle extends IafRenderLayer<EntityDragonBase, TabulaModel> {
    private static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/misc/double_dragon_saddle.png");
    /** The model's texture space (the picture is twice as big: 128x64). */
    private static final int TEX_W = 64, TEX_H = 32;

    private final BasicModelPart frontCloth = part();
    private final BasicModelPart backCloth = part();
    private final BasicModelPart frontSeat = part();
    private final BasicModelPart backSeat = part();

    public LayerDragonSaddle(IafMobRenderer<EntityDragonBase, TabulaModel> renderer) {
        super(renderer);
        // the cloths: over the top of each bone and down its sides a little (BodyUpper behind its wings, BodyLower to
        // past the seat behind); BodyUpper's to 7.05: past its own end at 7, short of the seat behind's cushion at 7.1
        box(frontCloth, 0, 0, -3.25F, -2.05F, 4.05F, 6.5F, 2.5F, 3F, 0F);
        box(backCloth, 0, 6, -3.75F, -1.75F, -0.1F, 7.5F, 2.5F, 4.5F, 0F);
        // the seats, from the top of their bone under the seat (a rider's seat is the cushion's top): the cushion (as
        // thin as before: its picture is half a unit high), its back (narrower than it) and its horn
        for (BasicModelPart seat : new BasicModelPart[]{frontSeat, backSeat}) {
            box(seat, 26, 0, -1.5F, -0.5F, -1.4F, 3F, 0.5F, 2.5F, -0.1F);
            box(seat, 26, 4, -1.25F, -1.15F, 0.55F, 2.5F, 1F, 0.5F, 0F);
            box(seat, 34, 4, -0.5F, -0.95F, -1.3F, 1F, 0.5F, 0.5F, 0F);
        }
        // the girth straps, round each bone's body under its seat
        box(frontSeat, 0, 14, -3.5F, -0.35F, -0.5F, 7F, 7.5F, 1F, 0F);
        box(backSeat, 0, 23, -4F, -0.35F, -0.5F, 8F, 6.5F, 1F, 0F);
    }

    private static BasicModelPart part() {
        return new BasicModelPart(null).setTextureSize(TEX_W, TEX_H);
    }

    /**
     * A box from its lowest corner and its size (model units, as its texture's), and how far its height grows or
     * shrinks to its real height (only the cushion's: a thin seat with a whole texel round its sides).
     */
    private static void box(BasicModelPart part, int u, int v, float x, float y, float z, float w, float h, float d, float gy) {
        part.cubeList.add(new ModelPart.Cube(u, v, x, y + gy, z, w, h, d, 0F, gy, 0F, false, TEX_W, TEX_H, EnumSet.allOf(Direction.class)));
    }

    @Override
    public void render(PoseStack poseStack, BufferSource buffers, int light, EntityDragonBase dragon, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!DragonSaddle.worn(dragon)) {
            return;
        }
        var upper = this.getParentModel().getCube("BodyUpper");
        var lower = this.getParentModel().getCube("BodyLower");
        if (upper == null || lower == null) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderTypes.entityCutout(TEXTURE));
        frontSeat.setRotationPoint(0F, DragonSaddle.UPPER_TOP, DragonSeatPose.SEAT_Z);
        backSeat.setRotationPoint(0F, DragonSaddle.LOWER_TOP, DragonSaddle.backZ(dragon));
        poseStack.pushPose();
        upper.translateRotate(poseStack);
        frontCloth.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        frontSeat.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        lower.translateRotate(poseStack);
        backCloth.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        backSeat.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
