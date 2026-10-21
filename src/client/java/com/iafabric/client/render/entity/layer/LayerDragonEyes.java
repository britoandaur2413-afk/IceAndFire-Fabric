package com.iafabric.client.render.entity.layer;

import com.iafabric.citadel.client.model.AdvancedModelBox;
import com.iafabric.citadel.client.model.TabulaModel;
import com.iafabric.citadel.client.model.basic.BasicModelPart;
import com.iafabric.client.model.util.TabulaModelHandlerHelper;
import com.iafabric.client.render.TabulaModelAccessor;
import com.iafabric.client.render.entity.IafMobRenderer;
import com.iafabric.client.render.entity.IafRenderLayer;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityIceDragon;
import com.iafabric.entity.EntityLightningDragon;
import com.iafabric.enums.EnumDragonTextures;
import com.iafabric.IafMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.iafabric.client.render.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.util.ArrayList;
import java.util.List;

/** A dragon's eyes, drawn bright (they glow in the dark) on a copy of the head that follows the body's pose. */
public class LayerDragonEyes extends IafRenderLayer<EntityDragonBase, TabulaModel> {
    private TabulaModel fireHead;
    private TabulaModel iceHead;
    private TabulaModel lightningHead;

    public LayerDragonEyes(IafMobRenderer<EntityDragonBase, TabulaModel> renderer) {
        super(renderer);
        try {
            fireHead = onlyKeepCubes(new TabulaModelAccessor(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/firedragon/firedragon_ground")), List.of("HeadFront"));
            iceHead = onlyKeepCubes(new TabulaModelAccessor(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/icedragon/icedragon_ground")), List.of("HeadFront"));
            lightningHead = onlyKeepCubes(new TabulaModelAccessor(TabulaModelHandlerHelper.loadTabulaModel("/assets/iceandfire_fabric/models/tabula/lightningdragon/lightningdragon_ground")), List.of("HeadFront"));
        } catch (Exception ex) {
            IafMod.LOGGER.error("Couldn't load the dragon eye models", ex);
        }
    }

    @Override
    public void render(PoseStack poseStack, BufferSource buffers, int light, EntityDragonBase dragon, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!dragon.shouldRenderEyes()) {
            return;
        }
        TabulaModel head = dragon instanceof EntityLightningDragon ? lightningHead : dragon instanceof EntityIceDragon ? iceHead : fireHead;
        TabulaModel model = head != null ? head : getParentModel();
        if (head != null) {
            copyPositions(head, getParentModel());
        }
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderTypes.eyes(EnumDragonTextures.getEyeTextureFromDragon(dragon))), light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }

    /** Keeps only the named cubes and their parents (the parents place the head). */
    private static TabulaModel onlyKeepCubes(TabulaModelAccessor model, List<String> names) {
        List<AdvancedModelBox> keep = new ArrayList<>();
        for (String name : names) {
            AdvancedModelBox cube = model.getCube(name);
            keep.add(cube);
            while (cube.getParent() != null) {
                keep.add(cube.getParent());
                cube = cube.getParent();
            }
        }
        for (AdvancedModelBox root : model.getRootBox()) {
            removeChildren(root, keep);
        }
        model.getRootBox().removeIf(box -> !keep.contains(box));
        model.getCubes().values().removeIf(box -> !keep.contains(box));
        return model;
    }

    private static void removeChildren(BasicModelPart box, List<AdvancedModelBox> keep) {
        box.childModels.removeIf(child -> !keep.contains(child));
        for (BasicModelPart child : box.childModels) {
            removeChildren(child, keep);
        }
    }

    private static void copyPositions(TabulaModel model, TabulaModel from) {
        for (AdvancedModelBox cube : model.getCubes().values()) {
            AdvancedModelBox source = from.getCube(cube.boxName);
            if (source == null) {
                continue;
            }
            cube.rotateAngleX = source.rotateAngleX;
            cube.rotateAngleY = source.rotateAngleY;
            cube.rotateAngleZ = source.rotateAngleZ;
            cube.rotationPointX = source.rotationPointX;
            cube.rotationPointY = source.rotationPointY;
            cube.rotationPointZ = source.rotationPointZ;
        }
    }
}
