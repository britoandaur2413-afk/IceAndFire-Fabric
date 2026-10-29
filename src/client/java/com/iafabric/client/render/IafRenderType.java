package com.iafabric.client.render;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * The special looks of the mod (ghosts, stone statues, the dread portal), on 26.x render types.
 * The 1.18 mod had its own blend modes and shaders for some; these use the closest vanilla ones.
 */
public final class IafRenderType {
    private static final ResourceLocation STONE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/block/stone.png");

    private IafRenderType() {
    }

    public static RenderType getGhost(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType getGhostDaytime(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType getDreadlandsPortal() {
        return RenderType.endPortal();
    }

    public static RenderType getStoneMobRenderType(float x, float y) {
        return RenderType.entityCutoutNoCull(STONE_TEXTURE);
    }

    public static RenderType getIce(ResourceLocation texture) {
        return RenderType.entityTranslucent(texture);
    }

    public static RenderType getStoneCrackRenderType(ResourceLocation crackTexture) {
        return RenderType.crumbling(crackTexture);
    }
}
