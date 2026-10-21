package com.iafabric.client.render.entity;

import com.iafabric.IafMod;
import com.iafabric.client.render.IafRenderType;
import com.iafabric.client.render.RetexturingCollector;
import com.iafabric.entity.EntityStoneStatue;
import com.iafabric.entity.EntityTroll;
import com.iafabric.util.EntityNbt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * A creature turned to stone: the creature itself is drawn through its own renderer, posed still, with
 * everything it draws turned to stone (and cracks over it as it is broken).
 */
public class RenderStoneStatue extends IafEntityRenderer<EntityStoneStatue> {
    private static final Identifier STONE = Identifier.withDefaultNamespace("textures/block/stone.png");
    protected static final Identifier[] DESTROY_STAGES = new Identifier[10];

    static {
        for (int i = 0; i < 10; i++) {
            DESTROY_STAGES[i] = Identifier.withDefaultNamespace("textures/block/destroy_stage_" + i + ".png");
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
            if (type == EntityTypes.PLAYER) {
                // a player cannot be made up here; a zombie has the same shape
                type = EntityTypes.ZOMBIE;
            }
            entity = type.create(Minecraft.getInstance().level, EntitySpawnReason.LOAD);
            if (entity != null) {
                // Never added to the level, but 26.x renderers read its id (held items): one no real entity has.
                entity.setId(-1 - statue.getId());
                try {
                    if (type != EntityTypes.ZOMBIE || statue.getTrappedEntityType() != EntityTypes.PLAYER) {
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
    public void submit(State<EntityStoneStatue> state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        EntityStoneStatue statue = state.entity;
        if (statue == null) {
            return;
        }
        Entity inside = statueEntity(statue);
        if (inside == null) {
            return;
        }
        float yaw = Mth.lerp(state.partialTick, statue.yRotO, statue.getYRot());
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
        EntityRenderer<Entity, EntityRenderState> renderer = (EntityRenderer<Entity, EntityRenderState>) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(inside);
        EntityRenderState insideState = renderer.createRenderState(inside, 0.0F);
        insideState.lightCoords = state.lightCoords;
        insideState.nameTag = null;
        insideState.shadowPieces.clear();
        // Each part is drawn with its own texture turned to stone (transparent parts stay transparent); a troll has its stone skin.
        RenderType fallback = IafRenderType.getStoneMobRenderType(200, 200);
        java.util.function.Function<RenderType, RenderType> stone = inside instanceof EntityTroll troll
            ? type -> RenderTypes.entityCutout(troll.getTrollType().TEXTURE_STONE)
            : type -> com.iafabric.client.render.StoneTextures.of(type).map(RenderTypes::entityCutout).orElse(fallback);
        poseStack.pushPose();
        renderer.submit(insideState, poseStack, new RetexturingCollector(collector, stone), camera);
        if (statue.getCrackAmount() >= 1) {
            int stage = Mth.clamp(statue.getCrackAmount() - 1, 0, DESTROY_STAGES.length - 1);
            renderer.submit(insideState, poseStack, new RetexturingCollector(collector, IafRenderType.getStoneCrackRenderType(DESTROY_STAGES[stage])), camera);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
