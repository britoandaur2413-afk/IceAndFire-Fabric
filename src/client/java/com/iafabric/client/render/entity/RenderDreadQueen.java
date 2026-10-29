package com.iafabric.client.render.entity;

import com.iafabric.client.model.ModelDreadQueen;
import com.iafabric.client.render.entity.layer.LayerGenericGlowing;
import com.iafabric.client.render.entity.layer.LayerHeldItem;
import com.iafabric.entity.EntityDreadQueen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Cryona, the Dread Queen: her armor cracked when she is badly hurt (asleep on her throne she is RenderCryonaAsleep). */
public class RenderDreadQueen extends IafMobRenderer<EntityDreadQueen, ModelDreadQueen<EntityDreadQueen>> {
	public static final ResourceLocation TEXTURE = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_queen.png");
	public static final ResourceLocation TEXTURE_INACTIVE = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_queen_inactive.png");
	public static final ResourceLocation TEXTURE_INJURED = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_queen_injured.png");
	public static final ResourceLocation TEXTURE_EYES = ResourceLocation.parse("iceandfire_fabric:textures/models/dread/dread_queen_eyes.png");
	public final LayerHeldItem<EntityDreadQueen, ModelDreadQueen<EntityDreadQueen>> itemLayer;

	public RenderDreadQueen(EntityRendererProvider.Context context) {
		super(context, new ModelDreadQueen<>(0.0F), 0.6F);
		this.addLayer(new LayerGenericGlowing<>(this, TEXTURE_EYES));
		this.itemLayer = new LayerHeldItem<>(this);
		this.addLayer(this.itemLayer);
	}

	@Override
	protected void scale(EntityDreadQueen entity, PoseStack poseStack, float partialTickTime) {
		poseStack.scale(1.1F, 1.1F, 1.1F);
	}

	/**
	 * On Black Frost she is drawn on its back as it is drawn (its turn, its tilt, the beat of its body in the air), not
	 * where her own place catches up with it a tick later: she sat beside its back or floated over it as it moved.
	 */
	@Override
	public void render(EntityDreadQueen queen, float entityYaw, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffers, int light) {
		if (queen.getVehicle() instanceof com.iafabric.entity.EntityDragonBase dragon && dragon.carriesRider(queen)) {
			float body = net.minecraft.util.Mth.rotLerp(partialTicks, dragon.yBodyRotO, dragon.yBodyRot);
			net.minecraft.world.phys.Vec3 seat = dragon.drawnSeat(partialTicks, body);
			net.minecraft.world.phys.Vec3 at = queen.getPosition(partialTicks);
			poseStack.pushPose();
			poseStack.translate(seat.x - at.x, seat.y - at.y, seat.z - at.z);
			super.render(queen, entityYaw, partialTicks, poseStack, buffers, light);
			poseStack.popPose();
			return;
		}
		super.render(queen, entityYaw, partialTicks, poseStack, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityDreadQueen queen) {
		return queen.getHealth() < queen.getMaxHealth() * 0.35F ? TEXTURE_INJURED : TEXTURE;
	}
}
