package com.iafabric.client.particle;

import com.mojang.math.Axis;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ParticleBlood extends IafParticle {
    private static final Identifier BLOOD = Identifier.parse("iceandfire_fabric:textures/particles/blood.png");

    public ParticleBlood(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z, 0, Math.random() * (double) 0.2F + 0.1, 0, "blood");
        this.setPos(x, y, z);
        this.yd += 0.01D;
    }



    @Override
    protected int getLightCoords(float partialTick) {
        return 240;
    }

    public int getFXLayer() {
        return 3;
    }


}
