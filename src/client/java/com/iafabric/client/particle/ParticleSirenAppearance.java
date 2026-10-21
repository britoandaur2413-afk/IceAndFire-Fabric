package com.iafabric.client.particle;

import com.iafabric.client.model.ModelSiren;
import com.iafabric.client.render.entity.RenderSiren;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

/** The siren that appears before a charmed player's eyes. */
public class ParticleSirenAppearance extends AppearanceParticle {
    private static final ModelSiren MODEL = new ModelSiren();
    private final int sirenType;

    public ParticleSirenAppearance(ClientLevel level, double x, double y, double z, int sirenType) {
        super(level, x, y, z);
        this.lifetime = 30;
        this.sirenType = sirenType;
    }

    @Override
    protected void draw(PoseStack poseStack, Camera camera, float partialTicks, Buffers buffers) {
        float progress = (this.age + partialTicks) / this.lifetime;
        float alpha = 0.05F + 0.5F * Mth.sin(progress * (float) Math.PI);
        poseStack.rotateDegrees(Axis.XP, 150.0F * progress - 60.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.101F, 1.5D);
        MODEL.renderToBuffer(poseStack, buffers.getBuffer(RenderTypes.entityTranslucent(RenderSiren.getSirenOverlayTexture(sirenType))), 0xF000F0,
            OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
    }
}
