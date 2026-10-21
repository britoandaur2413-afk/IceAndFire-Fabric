package com.iafabric.client.particle;

import com.mojang.math.Axis;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ParticleSerpentBubble extends IafParticle {
    private static final Identifier BUBBLE_TEXTURE = Identifier.parse("iceandfire_fabric:textures/particles/sea_serpent_bubble.png");

    public ParticleSerpentBubble(ClientLevel world, double x, double y, double z, double motX, double motY, double motZ, float size) {
        super(world, x, y, z, motX, motY, motZ, "sea_serpent_bubble");
        this.setPos(x, y, z);
        this.quadSize = 0.3F;
    }


    @Override
    protected int getLightCoords(float partialTick) {
        BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
        return 240;
    }

    public int getFXLayer() {
        return 3;
    }


}
