package com.iafabric.client.render.entity;

import com.iafabric.client.render.BufferSource;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.citadel.client.model.AdvancedEntityModel;
import com.iafabric.client.model.ModelCockatrice;
import com.iafabric.client.model.ModelCockatriceChick;
import com.iafabric.client.particle.CockatriceBeamRender;
import com.iafabric.entity.EntityCockatrice;
import com.iafabric.entity.EntityGorgon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class RenderCockatrice extends IafMobRenderer<EntityCockatrice, AdvancedEntityModel<EntityCockatrice>> {

    public static final ResourceLocation TEXTURE_ROOSTER = ResourceLocation.parse("iceandfire_fabric:textures/models/cockatrice/cockatrice_0.png");
    public static final ResourceLocation TEXTURE_HEN = ResourceLocation.parse("iceandfire_fabric:textures/models/cockatrice/cockatrice_1.png");
    public static final ResourceLocation TEXTURE_ROOSTER_CHICK = ResourceLocation.parse("iceandfire_fabric:textures/models/cockatrice/cockatrice_0_chick.png");
    public static final ResourceLocation TEXTURE_HEN_CHICK = ResourceLocation.parse("iceandfire_fabric:textures/models/cockatrice/cockatrice_1_chick.png");
    public static final ModelCockatrice ADULT_MODEL = new ModelCockatrice();
    public static final ModelCockatriceChick BABY_MODEL = new ModelCockatriceChick();

    public RenderCockatrice(EntityRendererProvider.Context context) {
        super(context, new ModelCockatrice(), 0.6F);
    }


    private Vec3 getPosition(LivingEntity LivingEntityIn, double p_177110_2_, float p_177110_4_) {
        double d0 = LivingEntityIn.xOld + (LivingEntityIn.getX() - LivingEntityIn.xOld) * (double) p_177110_4_;
        double d1 = p_177110_2_ + LivingEntityIn.yOld + (LivingEntityIn.getY() - LivingEntityIn.yOld) * (double) p_177110_4_;
        double d2 = LivingEntityIn.zOld + (LivingEntityIn.getZ() - LivingEntityIn.zOld) * (double) p_177110_4_;
        return new Vec3(d0, d1, d2);
    }

    @Override
    public boolean shouldRender(@NotNull EntityCockatrice livingEntityIn, @NotNull Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            if (livingEntityIn.hasTargetedEntity()) {
                LivingEntity livingentity = livingEntityIn.getTargetedEntity();
                if (livingentity != null) {
                    Vec3 Vector3d = this.getPosition(livingentity, (double) livingentity.getBbHeight() * 0.5D, 1.0F);
                    Vec3 Vector3d1 = this.getPosition(livingEntityIn, livingEntityIn.getEyeHeight(), 1.0F);
                    return camera.isVisible(new AABB(Vector3d1.x, Vector3d1.y, Vector3d1.z, Vector3d.x, Vector3d.y, Vector3d.z));
                }
            }

            return false;
        }
    }

    @Override
    protected void beforeRender(EntityCockatrice entityIn, float partialTicks) {
        if (entityIn.isBaby()) {
            model = BABY_MODEL;
        } else {
            model = ADULT_MODEL;
        }
    }

    @Override
    protected void renderExtra(EntityCockatrice entityIn, float partialTicks, PoseStack matrixStackIn, BufferSource bufferIn, int packedLightIn) {
        LivingEntity livingentity = entityIn.getTargetedEntity();
        boolean blindness = entityIn.hasEffect(MobEffects.BLINDNESS) || livingentity != null && livingentity.hasEffect(MobEffects.BLINDNESS);
        if (!blindness && livingentity != null && EntityGorgon.isEntityLookingAt(entityIn, livingentity, EntityCockatrice.VIEW_RADIUS) && EntityGorgon.isEntityLookingAt(livingentity, entityIn, EntityCockatrice.VIEW_RADIUS)) {
            if (livingentity != null) {
                CockatriceBeamRender.render(entityIn, livingentity, matrixStackIn, bufferIn, partialTicks);
            }
        }

    }

    @Override
    protected void scale(EntityCockatrice entity, @NotNull PoseStack matrixStackIn, float partialTickTime) {
        if (entity.isBaby()) {
            matrixStackIn.scale(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCockatrice cockatrice) {
        if (cockatrice.isBaby()) {
            return cockatrice.isHen() ? TEXTURE_HEN_CHICK : TEXTURE_ROOSTER_CHICK;
        } else {
            return cockatrice.isHen() ? TEXTURE_HEN : TEXTURE_ROOSTER;
        }
    }

}
