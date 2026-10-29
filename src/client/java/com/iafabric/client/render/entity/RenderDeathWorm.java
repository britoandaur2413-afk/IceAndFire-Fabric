package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelDeathWorm;
import com.iafabric.entity.EntityDeathWorm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;


public class RenderDeathWorm extends IafMobRenderer<EntityDeathWorm, ModelDeathWorm> {
    public static final ResourceLocation TEXTURE_RED = new ResourceLocation("iceandfire_fabric:textures/models/deathworm/deathworm_red.png");
    public static final ResourceLocation TEXTURE_WHITE = new ResourceLocation("iceandfire_fabric:textures/models/deathworm/deathworm_white.png");
    public static final ResourceLocation TEXTURE_YELLOW = new ResourceLocation("iceandfire_fabric:textures/models/deathworm/deathworm_yellow.png");

    public RenderDeathWorm(EntityRendererProvider.Context context) {
        super(context, new ModelDeathWorm(), 0);
    }

    @Override
    protected void scale(EntityDeathWorm entity, PoseStack matrixStackIn, float partialTickTime) {
        this.shadowRadius = entity.getModelScale() / 3;
        matrixStackIn.scale(entity.getModelScale(), entity.getModelScale(), entity.getModelScale());
    }


    @Override
    protected int getBlockLightLevel(EntityDeathWorm entityIn, @NotNull BlockPos partialTicks) {
        return entityIn.isOnFire() ? 15 : entityIn.getWormBrightness(false);
    }

    @Override
    protected int getSkyLightLevel(EntityDeathWorm entity, @NotNull BlockPos pos) {
        return entity.getWormBrightness(true);
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EntityDeathWorm entity) {
        return entity.getVariant() == 2 ? TEXTURE_WHITE : entity.getVariant() == 1 ? TEXTURE_RED : TEXTURE_YELLOW;
    }
}
