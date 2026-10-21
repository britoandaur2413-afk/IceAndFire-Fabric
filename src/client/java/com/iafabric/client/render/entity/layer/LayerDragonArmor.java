package com.iafabric.client.render.entity.layer;

import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.client.texture.ArrayLayeredTexture;
import com.iafabric.entity.DragonType;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.enums.EnumDragonTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import com.iafabric.client.render.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A dragon's armor: the four armor pieces' textures made into one, drawn on the dragon's model. */
public class LayerDragonArmor extends IafRenderLayer<EntityDragonBase, TabulaModel> {
    private static final Map<String, Identifier> LAYERED_ARMOR_CACHE = new HashMap<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public LayerDragonArmor(IafMobRenderer<EntityDragonBase, TabulaModel> renderer) {
        super(renderer);
    }

    public static void clearCache(String key) {
        LAYERED_ARMOR_CACHE.remove(key);
    }

    @Override
    public void render(PoseStack poseStack, BufferSource buffers, int light, EntityDragonBase dragon, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        int armorHead = dragon.getArmorOrdinal(dragon.getItemBySlot(EquipmentSlot.HEAD));
        int armorNeck = dragon.getArmorOrdinal(dragon.getItemBySlot(EquipmentSlot.CHEST));
        int armorLegs = dragon.getArmorOrdinal(dragon.getItemBySlot(EquipmentSlot.LEGS));
        int armorFeet = dragon.getArmorOrdinal(dragon.getItemBySlot(EquipmentSlot.FEET));
        String key = dragon.dragonType.getName() + "_" + armorHead + "_" + armorNeck + "_" + armorLegs + "_" + armorFeet;
        if (key.equals(dragon.dragonType.getName() + "_0_0_0_0")) {
            return;
        }
        Identifier location = LAYERED_ARMOR_CACHE.get(key);
        if (location == null) {
            location = Identifier.fromNamespaceAndPath("iceandfire_fabric", "dragon_armor_" + key);
            List<String> layers = new ArrayList<>();
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                EnumDragonTextures.Armor armor = EnumDragonTextures.Armor.getArmorForDragon(dragon, slot);
                if (dragon.dragonType == DragonType.FIRE) {
                    layers.add(armor.FIRETEXTURE.toString());
                } else if (dragon.dragonType == DragonType.ICE) {
                    layers.add(armor.ICETEXTURE.toString());
                } else {
                    layers.add(armor.LIGHTNINGTEXTURE.toString());
                }
            }
            ArrayLayeredTexture.register(location, layers);
            LAYERED_ARMOR_CACHE.put(key, location);
        }
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderTypes.entityCutoutZOffset(location)), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
