package com.iafabric.client.render.entity;

import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.model.ModelDreadThrall;
import com.iafabric.client.render.entity.layer.LayerHeldItem;
import com.iafabric.client.render.entity.layer.IHasArmorVariantResource;
import com.iafabric.client.render.entity.layer.LayerBipedArmorMultiple;
import com.iafabric.client.render.entity.layer.LayerGenericGlowing;
import com.iafabric.entity.EntityDreadThrall;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

public class RenderDreadThrall extends IafMobRenderer<EntityDreadThrall, ModelDreadThrall> implements IHasArmorVariantResource {
    public static final ResourceLocation TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_thrall.png");
    public static final ResourceLocation TEXTURE_EYES = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_thrall_eyes.png");
    public static final ResourceLocation TEXTURE_LEG_ARMOR = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_legs.png");
    public static final ResourceLocation TEXTURE_ARMOR_0 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_chest_1.png");
    public static final ResourceLocation TEXTURE_ARMOR_1 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_chest_2.png");
    public static final ResourceLocation TEXTURE_ARMOR_2 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_chest_3.png");
    public static final ResourceLocation TEXTURE_ARMOR_3 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_chest_4.png");
    public static final ResourceLocation TEXTURE_ARMOR_4 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_chest_5.png");
    public static final ResourceLocation TEXTURE_ARMOR_5 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_chest_6.png");
    public static final ResourceLocation TEXTURE_ARMOR_6 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_chest_7.png");
    public static final ResourceLocation TEXTURE_ARMOR_7 = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/thrall_chest_8.png");
    public final LayerHeldItem<EntityDreadThrall, ModelDreadThrall> itemLayer;

    public RenderDreadThrall(EntityRendererProvider.Context context) {
        super(context, new ModelDreadThrall(0.0F, false), 0.6F);

        this.addLayer(new LayerGenericGlowing<>(this, TEXTURE_EYES));
        this.itemLayer = new LayerHeldItem<>(this);
        this.addLayer(this.itemLayer);
        this.addLayer(new LayerBipedArmorMultiple<>(this,
            new ModelDreadThrall(0.5F, true), new ModelDreadThrall(1.0F, true),
            TEXTURE_ARMOR_0, TEXTURE_LEG_ARMOR));
    }

    @Override
    public ResourceLocation getArmorResource(int variant, EquipmentSlot equipmentSlotType) {
        if (equipmentSlotType == EquipmentSlot.LEGS)
            return TEXTURE_LEG_ARMOR;
        switch (variant) {
            case 0:
                return TEXTURE_ARMOR_0;
            case 1:
                return TEXTURE_ARMOR_1;
            case 2:
                return TEXTURE_ARMOR_2;
            case 3:
                return TEXTURE_ARMOR_3;
            case 4:
                return TEXTURE_ARMOR_4;
            case 5:
                return TEXTURE_ARMOR_5;
            case 6:
                return TEXTURE_ARMOR_6;
            case 7:
                return TEXTURE_ARMOR_7;
            default:
                return TEXTURE_ARMOR_0;
        }
    }

    @Override
    public void scale(EntityDreadThrall livingEntityIn, PoseStack stack, float partialTickTime) {
        stack.scale(0.95F, 0.95F, 0.95F);
        if (livingEntityIn.getAnimation() == this.getModel().getSpawnAnimation()) {
            itemLayer.hidden = livingEntityIn.getAnimationTick() <= this.getModel().getSpawnAnimation().getDuration() - 10;
            return;
        }
        itemLayer.hidden = false;

    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EntityDreadThrall entity) {
        return TEXTURE;
    }


}
