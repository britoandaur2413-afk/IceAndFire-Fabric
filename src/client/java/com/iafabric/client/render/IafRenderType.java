package com.iafabric.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The special looks of the mod (ghosts, stone statues, the dread portal), on 26.x render types.
 * The 1.18 mod had its own blend modes and shaders for some; these use the closest vanilla ones.
 */
public final class IafRenderType {
    private static final Identifier STONE_TEXTURE = Identifier.withDefaultNamespace("textures/block/stone.png");

    private IafRenderType() {
    }

    public static RenderType getGhost(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType getGhostDaytime(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType getDreadlandsPortal() {
        return RenderTypes.endPortal();
    }

    public static RenderType getStoneMobRenderType(float x, float y) {
        return RenderTypes.entityCutout(STONE_TEXTURE);
    }

    public static RenderType getIce(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType getStoneCrackRenderType(Identifier crackTexture) {
        return RenderTypes.crumbling(crackTexture);
    }
}
