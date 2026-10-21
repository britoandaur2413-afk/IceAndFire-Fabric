package com.iafabric.client.particle;

import com.iafabric.client.model.ModelGhost;
import com.iafabric.client.render.entity.RenderGhost;
import com.iafabric.entity.EntityGhost;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** A ghost flying at the player's face from one side (first person only). */
public class ParticleGhostAppearance extends AppearanceParticle {
    private static final ModelGhost MODEL = new ModelGhost(0.0F);
    private final int ghost;
    private final boolean fromLeft;

    public ParticleGhostAppearance(ClientLevel level, double x, double y, double z, int ghost) {
        super(level, x, y, z);
        this.lifetime = 15;
        this.ghost = ghost;
        this.fromLeft = level.getRandom().nextBoolean();
    }

    @Override
    protected void draw(PoseStack poseStack, Camera camera, float partialTicks, Buffers buffers) {
        float progress = (this.age + partialTicks) / this.lifetime;
        float alpha = 0.05F + 0.5F * Mth.sin(progress * (float) Math.PI);
        Entity entity = level.getEntity(ghost);
        if (entity instanceof EntityGhost ghostEntity && Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON) {
            float angle = 150.0F * progress - 60.0F;
            if (fromLeft) {
                poseStack.rotateDegrees(Axis.YN, angle);
                poseStack.rotateDegrees(Axis.ZN, angle);
            } else {
                poseStack.rotateDegrees(Axis.YP, angle);
                poseStack.rotateDegrees(Axis.ZP, angle);
            }
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(0.0D, 0.3F, 1.25D);
            MODEL.setupAnim(ghostEntity, 0, 0, entity.tickCount + partialTicks, 0, 0);
            MODEL.renderToBuffer(poseStack, buffers.getBuffer(RenderTypes.entityTranslucent(RenderGhost.getGhostOverlayForType(ghostEntity.getColor()))),
                0xF000F0, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, alpha);
        }
    }
}
