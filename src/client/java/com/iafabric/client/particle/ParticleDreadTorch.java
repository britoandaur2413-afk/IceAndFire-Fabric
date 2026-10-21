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

public class ParticleDreadTorch extends IafParticle {
    private static final Identifier SNOWFLAKE = Identifier.parse("iceandfire_fabric:textures/particles/snowflake_0.png");
    private static final Identifier SNOWFLAKE_BIG = Identifier.parse("iceandfire_fabric:textures/particles/snowflake_1.png");

    private final boolean big;

    public ParticleDreadTorch(ClientLevel world, double x, double y, double z, double motX, double motY, double motZ, float size) {
        super(world, x, y, z, motX, motY, motZ, "snowflake_0");
        this.setPos(x, y, z);
        this.yd += 0.01D;
        big = random.nextBoolean();
    }



    @Override
    protected int getLightCoords(float partialTick) {
        return 240;
    }

    public int getFXLayer() {
        return 3;
    }


}
