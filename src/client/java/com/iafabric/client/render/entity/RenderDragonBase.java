package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.render.entity.layer.LayerDragonArmor;
import com.iafabric.client.render.entity.layer.LayerDragonBanner;
import com.iafabric.client.render.entity.layer.LayerDragonEyes;
import com.iafabric.client.texture.ArrayLayeredTexture;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.enums.EnumDragonTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A fire, ice or lightning dragon: its Tabula model, scaled to its age, with a texture per variant, stage and state. */
public class RenderDragonBase extends IafMobRenderer<EntityDragonBase, TabulaModel> {

    private final Map<String, Identifier> layeredTextureCache = new HashMap<>();
    private final int dragonType;

    public RenderDragonBase(EntityRendererProvider.Context context, TabulaModel model, int dragonType) {
        super(context, model, 0.15F);
        this.addLayer(new LayerDragonEyes(this));
        this.addLayer(new LayerDragonBanner(this));
        this.addLayer(new LayerDragonArmor(this));
        this.dragonType = dragonType;
    }

    @Override
    public void extractRenderState(EntityDragonBase entity, IafMobRenderer.State<EntityDragonBase> state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // the rider drawn on a kingdom's or the dread's dragon
        state.extra = null;
        DragonRiderRender.extract(entity, state, partialTicks, this.entityRenderDispatcher);
    }

    @Override
    protected void scale(EntityDragonBase entity, PoseStack poseStack, float partialTickTime) {
        this.shadowRadius = entity.getRenderSize() / 3;
        float pitch = entity.prevDragonPitch + (entity.getDragonPitch() - entity.prevDragonPitch) * partialTickTime;
        poseStack.rotateDegrees(Axis.XP, pitch);
        poseStack.scale(shadowRadius, shadowRadius, shadowRadius);
    }

    public static final Identifier BLACK_FROST = Identifier.parse("iceandfire_fabric:textures/models/dread/black_frost.png");
    public static final Identifier DREAD_DRAGON = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_dragon.png");

    @Override
    public Identifier getTextureLocation(EntityDragonBase entity) {
        // the dread's undead dragons wear their own skin
        if (entity.getDreadLook() == 2) {
            return BLACK_FROST;
        }
        if (entity.getDreadLook() == 1) {
            return DREAD_DRAGON;
        }
        String baseTexture = entity.getVariantName(entity.getVariant()) + entity.getDragonStage() + entity.isModelDead() + entity.isMale() + entity.isSkeletal()
            + entity.isSleeping() + entity.isBlinking();
        Identifier location = layeredTextureCache.get(baseTexture);
        if (location == null) {
            location = Identifier.fromNamespaceAndPath("iceandfire_fabric", "dragon_texture_" + baseTexture.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_"));
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
