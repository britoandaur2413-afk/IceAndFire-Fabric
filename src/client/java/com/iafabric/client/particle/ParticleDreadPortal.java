package com.iafabric.client.particle;

import com.mojang.math.Axis;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ParticleDreadPortal extends IafParticle {
    private static final ResourceLocation SNOWFLAKE = ResourceLocation.parse("iceandfire_fabric:textures/particles/snowflake_0.png");
    private static final ResourceLocation SNOWFLAKE_BIG = ResourceLocation.parse("iceandfire_fabric:textures/particles/snowflake_1.png");

    private final boolean big;

    public ParticleDreadPortal(ClientLevel world, double x, double y, double z, double motX, double motY, double motZ, float size) {
        super(world, x, y, z, motX, motY, motZ, "snowflake_0");
        this.setPos(x, y, z);
        big = random.nextBoolean();
    }




    @Override
    protected int getLightColor(float partialTick) {
        return 240;
    }

    public int getFXLayer() {
        return 3;
    }


}
