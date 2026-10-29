package com.iafabric.client.render.entity;

import com.iafabric.IafMod;
import com.iafabric.client.render.IafRenderType;
import com.iafabric.client.render.RetexturingCollector;
import com.iafabric.entity.EntityStoneStatue;
import com.iafabric.entity.EntityTroll;
import com.iafabric.util.EntityNbt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * A creature turned to stone: the creature itself is drawn through its own renderer, posed still, with
 * everything it draws turned to stone (and cracks over it as it is broken).
 */
public class RenderStoneStatue extends IafEntityRenderer<EntityStoneStatue> {
    private static final ResourceLocation STONE = new ResourceLocation("textures/block/stone.png");
    protected static final ResourceLocation[] DESTROY_STAGES = new ResourceLocation[10];

    static {
        for (int i = 0; i < 10; i++) {
            DESTROY_STAGES[i] = new ResourceLocation("textures/block/destroy_stage_" + i + ".png");
        }
    }

    private final Map<String, Entity> hollowEntityMap = new HashMap<>();

    public RenderStoneStatue(EntityRendererProvider.Context context) {
        super(context);
    }

    private Entity statueEntity(EntityStoneStatue statue) {
        String key = statue.getTrappedEntityTypeString() + statue.getId();
        Entity entity = hollowEntityMap.get(key);
        if (entity == null && Minecraft.getInstance().level != null) {
            EntityType<?> type = statue.getTrappedEntityType();
            if (type == EntityType.PLAYER) {
                // a player cannot be made up here; a zombie has the same shape
                type = EntityType.ZOMBIE;
            }
            entity = type.create(Minecraft.getInstance().level);
            if (entity != null) {
                // Never added to the level: an id no real entity has.
                entity.setId(-1 - statue.getId());
                try {
                    if (type != EntityType.ZOMBIE || statue.getTrappedEntityType() != EntityType.PLAYER) {
                        EntityNbt.load(entity, statue.getTrappedTag());
                    }
                } catch (Exception e) {
                    IafMod.LOGGER.warn("Mob {} could not build statue NBT", statue.getTrappedEntityTypeString());
                }
                hollowEntityMap.put(key, entity);
            }
        }
        return entity;
    }

    @Override
    public void render(EntityStoneStatue statue, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        Entity inside = statueEntity(statue);
        if (inside == null) {
            return;
        }
        float yaw = Mth.lerp(partialTicks, statue.yRotO, statue.getYRot());
        inside.setYRot(yaw);
        inside.yRotO = yaw;
        inside.setXRot(0);
        inside.xRotO = 0;
        inside.setPos(statue.getX(), statue.getY(), statue.getZ());
        inside.xo = inside.getX();
        inside.yo = inside.getY();
        inside.zo = inside.getZ();
        inside.tickCount = 0;
        if (inside instanceof LivingEntity living) {
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
            living.hurtTime = 0;
            living.deathTime = 0;
        }
        @SuppressWarnings("unchecked")
        EntityRenderer<Entity> renderer = (EntityRenderer<Entity>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(inside);
        // Each part is drawn with its own texture turned to stone (transparent parts stay transparent); a troll has its stone skin.
        RenderType fallback = IafRenderType.getStoneMobRenderType(200, 200);
        java.util.function.Function<RenderType, RenderType> stone = inside instanceof EntityTroll troll
            ? type -> RenderType.entityCutoutNoCull(troll.getTrollType().TEXTURE_STONE)
            : type -> com.iafabric.client.render.StoneTextures.of(type).map(RenderType::entityCutoutNoCull).orElse(fallback);
        poseStack.pushPose();
        renderer.render(inside, yaw, 0.0F, poseStack, new RetexturingCollector(buffers, stone), light);
        if (statue.getCrackAmount() >= 1) {
            int stage = Mth.clamp(statue.getCrackAmount() - 1, 0, DESTROY_STAGES.length - 1);
            renderer.render(inside, yaw, 0.0F, poseStack, new RetexturingCollector(buffers, IafRenderType.getStoneCrackRenderType(DESTROY_STAGES[stage])), light);
        }
        poseStack.popPose();
        super.render(statue, entityYaw, partialTicks, poseStack, buffers, light);
    }
}
