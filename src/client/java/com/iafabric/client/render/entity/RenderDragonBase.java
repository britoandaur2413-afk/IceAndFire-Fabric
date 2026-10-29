package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.render.entity.layer.LayerDragonArmor;
import com.iafabric.client.render.entity.layer.LayerDragonBanner;
import com.iafabric.client.render.entity.layer.LayerDragonEyes;
import com.iafabric.client.texture.ArrayLayeredTexture;
import com.iafabric.entity.DragonSaddle;
import com.iafabric.entity.DragonSeatPose;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.enums.EnumDragonTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A fire, ice or lightning dragon: its Tabula model, scaled to its age, with a texture per variant, stage and state. */
public class RenderDragonBase extends IafMobRenderer<EntityDragonBase, TabulaModel> {

    private final Map<String, ResourceLocation> layeredTextureCache = new HashMap<>();
    private final int dragonType;

    public RenderDragonBase(EntityRendererProvider.Context context, TabulaModel model, int dragonType) {
        super(context, model, 0.15F);
        this.addLayer(new LayerDragonEyes(this));
        this.addLayer(new LayerDragonBanner(this));
        this.addLayer(new LayerDragonArmor(this));
        this.addLayer(new com.iafabric.client.render.entity.layer.LayerDragonSaddle(this));
        this.dragonType = dragonType;
    }

    /** The spikes of its back where a double saddle sits (the saddle is over them): not drawn while it wears one. */
    private static final String[] UNDER_SADDLE = {"spike6", "spike7", "spike8"};

    @Override
    public void render(EntityDragonBase entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (!DragonSaddle.worn(entity)) {
            super.render(entity, entityYaw, partialTicks, poseStack, buffers, light);
            return;
        }
        this.showUnderSaddle(false);
        try {
            super.render(entity, entityYaw, partialTicks, poseStack, buffers, light);
        } finally {
            this.showUnderSaddle(true);
        }
    }

    private void showUnderSaddle(boolean show) {
        for (String name : UNDER_SADDLE) {
            var spike = this.model.getCube(name);
            if (spike != null) {
                spike.showModel = show;
            }
        }
    }

    /** A dragon ridden is always drawn: its rider is drawn with it (the rider may be in view when its body is not). */
    @Override
    public boolean shouldRender(EntityDragonBase entity, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(entity, frustum, x, y, z) || entity.getPassengers().stream().anyMatch(DragonSeat::seated);
    }

    /**
     * Its riders, in the seat on its back bone (BodyUpper, the root of its body in the three kinds) as it is posed this
     * frame: the bone's place and its turn, so the rider rises, sinks, tilts and rolls with its back (less the bone's
     * own tilt at rest, so a dragon at rest carries its rider upright); at the rider's own size, its seat (0.6 over its
     * feet, where a rider sits) on the seat. They are the one drawn with it (a kingdom's pillager, a dread knight) and
     * those really on it (its owner, Cryona on Black Frost), not drawn where the game would draw them (DragonSeatMixin);
     * a friend in a double saddle sits in its seat behind, on its lower back.
     */
    @Override
    protected void renderInPose(EntityDragonBase entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        var back = this.model.getCube("BodyUpper");
        if (back == null) {
            return;
        }
        List<LivingEntity> seated = new ArrayList<>();
        List<LivingEntity> behind = new ArrayList<>();
        LivingEntity drawn = DragonRiderRender.prepare(entity, bodyRot(entity, partialTicks), partialTicks);
        if (drawn != null) {
            seated.add(drawn);
        }
        for (Entity passenger : entity.getPassengers()) {
            if (DragonSeat.seated(passenger) && !DragonSeat.inOwnEyes(passenger)) {
                (DragonSaddle.inBackSeat(entity, passenger) ? behind : seated).add((LivingEntity) passenger);
            }
        }
        if (seated.isEmpty() && behind.isEmpty()) {
            return;
        }
        float size = entity.getRenderSize() / 3F;
        poseStack.pushPose();
        back.translateRotate(poseStack);
        if (!seated.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0F, DragonSeatPose.seatY(entity) / 16F, SEAT_Z / 16F);
            // the rider at its own size and upright in the model's frame again (the model is drawn upside down and scaled)
            poseStack.scale(-1F / size, -1F / size, 1F / size);
            // less the bone's tilt at rest (flipped back, its turn about x is the other way)
            poseStack.mulPose(Axis.XP.rotation(back.defaultRotationX));
            poseStack.mulPose(Axis.YP.rotation(back.defaultRotationY));
            poseStack.mulPose(Axis.ZP.rotation(-back.defaultRotationZ));
            for (LivingEntity rider : seated) {
                this.renderSeated(rider, rider == drawn, partialTicks, poseStack, buffers);
            }
            poseStack.popPose();
        }
        var lower = this.model.getCube("BodyLower");
        if (!behind.isEmpty() && lower != null) {
            // the friend behind, in the double saddle's seat on its lower back, likewise (less both bones' tilt at rest)
            lower.translateRotate(poseStack);
            poseStack.translate(0F, (DragonSaddle.LOWER_TOP - DragonSaddle.CUSHION) / 16F, DragonSaddle.backZ(entity) / 16F);
            poseStack.scale(-1F / size, -1F / size, 1F / size);
            poseStack.mulPose(Axis.XP.rotation(back.defaultRotationX + lower.defaultRotationX));
            for (LivingEntity rider : behind) {
                this.renderSeated(rider, false, partialTicks, poseStack, buffers);
            }
        }
        poseStack.popPose();
    }

    /**
     * A rider drawn in its seat, facing the way the seat faces (its renderer turns it by its body's turn: that turn is
     * taken off first), its head turned from there as the rider looks; lit as where it is. One drawn with the dragon
     * (not in the world) is drawn seated as a passenger is (VirtualRiders).
     */
    private void renderSeated(LivingEntity rider, boolean virtual, float partialTicks, PoseStack poseStack, MultiBufferSource buffers) {
        @SuppressWarnings("unchecked")
        EntityRenderer<LivingEntity> renderer = (EntityRenderer<LivingEntity>) this.entityRenderDispatcher.getRenderer(rider);
        float body = bodyRot(rider, partialTicks);
        Vec3 offset = renderer.getRenderOffset(rider, partialTicks);
        int light = this.entityRenderDispatcher.getPackedLightCoords(rider, partialTicks);
        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y - DragonSeatPose.RIDER_SEAT, offset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(body - 180F));
        if (virtual) {
            VirtualRiders.SEATED.add(rider);
        }
        try {
            renderer.render(rider, body, partialTicks, poseStack, buffers, light);
        } finally {
            if (virtual) {
                VirtualRiders.SEATED.remove(rider);
            }
            poseStack.popPose();
        }
    }

    @Override
    protected void scale(EntityDragonBase entity, PoseStack poseStack, float partialTickTime) {
        this.shadowRadius = entity.getRenderSize() / 3;
        float pitch = entity.prevDragonPitch + (entity.getDragonPitch() - entity.prevDragonPitch) * partialTickTime;
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(shadowRadius, shadowRadius, shadowRadius);
    }

    /**
     * Where its rider sits this frame: the top of its back bone (BodyUpper, the root of its body in all three kinds),
     * a little behind the shoulders, as the posed model is drawn (its beat in the air, its tilt, its size). Kept on the
     * dragon in its own frame (before its turn), so its rider is set on it exactly (EntityDragonBase.positionRider).
     */
    @Override
    protected void renderBody(EntityDragonBase entity, PoseStack poseStack, com.iafabric.client.render.BufferSource buffers, int light, int overlay, float partialTicks) {
        var back = this.model.getCube("BodyUpper");
        if (back != null) {
            PoseStack seat = new PoseStack();
            seat.scale(-1.0F, -1.0F, 1.0F);
            this.scale(entity, seat, partialTicks);
            seat.translate(0.0D, -1.501F, 0.0D);
            back.translateRotate(seat);
            Vector3f at = seat.last().pose().transformPosition(new Vector3f(0F, DragonSeatPose.seatY(entity) / 16F, SEAT_Z / 16F));
            entity.seatOnBack = new Vec3(at.x(), at.y(), at.z());
            entity.seatOnBackTick = entity.tickCount;
            var lower = this.model.getCube("BodyLower");
            if (lower != null && DragonSaddle.worn(entity)) {
                // and the double saddle's seat behind, on its lower back
                lower.translateRotate(seat);
                at = seat.last().pose().transformPosition(new Vector3f(0F, (DragonSaddle.LOWER_TOP - DragonSaddle.CUSHION) / 16F, DragonSaddle.backZ(entity) / 16F));
                entity.seatBehind = new Vec3(at.x(), at.y(), at.z());
                entity.seatBehindTick = entity.tickCount;
            }
        }
        this.keepMouth(entity, partialTicks);
        super.renderBody(entity, poseStack, buffers, light, overlay, partialTicks);
    }

    /**
     * Where its mouth is this frame, as the posed model is drawn (its body, its neck, its head, then each jaw: every bone
     * on the way, as it moves and is animated), in its own frame (before its turn): the middle of the gap between its
     * jaws (the underside of its snout and the top of its jaw), at their tips and at their hinge. The breath leaves just
     * inside the tips, along the line from the hinge to the tips: two points of the model.
     */
    private void keepMouth(EntityDragonBase entity, float partialTicks) {
        var snout = this.model.getCube("HeadFront");
        var jaw = this.model.getCube("Jaw");
        if (snout == null || jaw == null || snout.cubeList.isEmpty() || jaw.cubeList.isEmpty() || snout.parent == null) {
            return;
        }
        PoseStack at = new PoseStack();
        at.scale(-1.0F, -1.0F, 1.0F);
        this.scale(entity, at, partialTicks);
        at.translate(0.0D, -1.501F, 0.0D);
        down(at, snout.parent);
        var upper = snout.cubeList.get(0);
        var lower = jaw.cubeList.get(0);
        at.pushPose();
        snout.translateRotate(at);
        float x = (upper.minX + upper.maxX) / 32F;
        Vector3f upperTip = at.last().pose().transformPosition(new Vector3f(x, upper.maxY / 16F, upper.minZ / 16F));
        Vector3f upperHinge = at.last().pose().transformPosition(new Vector3f(x, upper.maxY / 16F, upper.maxZ / 16F));
        at.popPose();
        jaw.translateRotate(at);
        x = (lower.minX + lower.maxX) / 32F;
        Vector3f lowerTip = at.last().pose().transformPosition(new Vector3f(x, lower.minY / 16F, lower.minZ / 16F));
        Vector3f lowerHinge = at.last().pose().transformPosition(new Vector3f(x, lower.minY / 16F, lower.maxZ / 16F));
        Vector3f tip = upperTip.add(lowerTip).mul(0.5F);
        Vector3f hinge = upperHinge.add(lowerHinge).mul(0.5F);
        Vector3f way = new Vector3f(tip).sub(hinge).normalize();
        Vector3f mouth = new Vector3f(hinge).lerp(tip, 0.85F);
        entity.mouthAt = new Vec3(mouth.x(), mouth.y(), mouth.z());
        entity.mouthDir = new Vec3(way.x(), way.y(), way.z());
        entity.mouthTick = entity.tickCount;
    }

    /** Down its bones to this one, from its body: each as posed. */
    private static void down(PoseStack at, com.iafabric.citadel.client.model.basic.BasicModelPart part) {
        if (part.parent != null) {
            down(at, part.parent);
        }
        part.translateRotate(at);
    }

    /** The seat along the back bone, in model units (DragonSeatPose: the same on the server; its height there too). */
    private static final float SEAT_Z = DragonSeatPose.SEAT_Z;

    public static final ResourceLocation BLACK_FROST = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/black_frost.png");
    public static final ResourceLocation DREAD_DRAGON = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_dragon.png");

    @Override
    public ResourceLocation getTextureLocation(EntityDragonBase entity) {
        // the dread's undead dragons wear their own skin
        if (entity.getDreadLook() == 2) {
            return BLACK_FROST;
        }
        if (entity.getDreadLook() == 1) {
            return DREAD_DRAGON;
        }
        String baseTexture = entity.getVariantName(entity.getVariant()) + entity.getDragonStage() + entity.isModelDead() + entity.isMale() + entity.isSkeletal()
            + entity.isSleeping() + entity.isBlinking();
        ResourceLocation location = layeredTextureCache.get(baseTexture);
        if (location == null) {
            location = ResourceLocation.fromNamespaceAndPath("iceandfire_fabric", "dragon_texture_" + baseTexture.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"));
            List<String> layers = new ArrayList<>();
            layers.add(EnumDragonTextures.getTextureFromDragon(entity).toString());
            if (entity.isMale() && !entity.isSkeletal()) {
                if (dragonType == 0) {
                    layers.add(EnumDragonTextures.getDragonEnum(entity).FIRE_MALE_OVERLAY.toString());
                } else if (dragonType == 1) {
                    layers.add(EnumDragonTextures.getDragonEnum(entity).ICE_MALE_OVERLAY.toString());
                } else if (dragonType == 2) {
                    layers.add(EnumDragonTextures.getDragonEnum(entity).LIGHTNING_MALE_OVERLAY.toString());
                }
            } else {
                layers.add(EnumDragonTextures.Armor.EMPTY.FIRETEXTURE.toString());
            }
            ArrayLayeredTexture.register(location, layers);
            layeredTextureCache.put(baseTexture, location);
        }
        return location;
    }
}
