package com.iafabric.client.render.entity;

import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.particle.LightningBoltData;
import com.iafabric.client.particle.LightningRender;
import com.iafabric.client.render.BufferSource;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityLightningDragon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/** A lightning dragon, and the lightning it breathes at its target. */
public class RenderLightningDragon extends RenderDragonBase {

    private final LightningRender lightningRender = new LightningRender();

    public RenderLightningDragon(EntityRendererProvider.Context context, TabulaModel model, int dragonType) {
        super(context, model, dragonType);
    }

    @Override
    public boolean shouldRender(EntityDragonBase entity, Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(entity, camera, camX, camY, camZ)) {
            return true;
        }
        EntityLightningDragon lightningDragon = (EntityLightningDragon) entity;
        if (lightningDragon.hasLightningTarget()) {
            Vec3 head = lightningDragon.getHeadPosition();
            Vec3 target = new Vec3(lightningDragon.getLightningTargetX(), lightningDragon.getLightningTargetY(), lightningDragon.getLightningTargetZ());
            return camera.isVisible(new AABB(head.x, head.y, head.z, target.x, target.y, target.z));
        }
        return false;
    }

    @Override
    protected void renderExtra(EntityDragonBase entity, float partialTicks, PoseStack poseStack, BufferSource buffers, int light) {
        EntityLightningDragon lightningDragon = (EntityLightningDragon) entity;
        if (!lightningDragon.hasLightningTarget() || Minecraft.getInstance().player == null) {
            return;
        }
        double dist = Minecraft.getInstance().player.distanceTo(lightningDragon);
        if (dist > Math.max(256, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16F)) {
            return;
        }
        // from its mouth as it is drawn this frame (its old head point if it is not drawn)
        Vec3 head = com.iafabric.client.particle.DragonBreathFx.mouth(lightningDragon, partialTicks);
        if (head == null) {
            head = lightningDragon.getHeadPosition();
        }
        Vec3 target = new Vec3(lightningDragon.getLightningTargetX(), lightningDragon.getLightningTargetY(), lightningDragon.getLightningTargetZ());
        float energyScale = 0.4F * lightningDragon.getDragonScale();
        LightningBoltData bolt = new LightningBoltData(LightningBoltData.BoltRenderInfo.ELECTRICITY, head, target, 15)
            .size(0.05F * (0.5F + Math.min(energyScale, 1F) * 1.5F))
            .lifespan(4)
            .spawn(LightningBoltData.SpawnFunction.NO_DELAY);
        lightningRender.update(null, bolt, partialTicks);
        poseStack.pushPose();
        poseStack.translate(-Mth.lerp(partialTicks, entity.xOld, entity.getX()), -Mth.lerp(partialTicks, entity.yOld, entity.getY()), -Mth.lerp(partialTicks, entity.zOld, entity.getZ()));
        lightningRender.render(partialTicks, poseStack, buffers);
        poseStack.popPose();
    }
}
