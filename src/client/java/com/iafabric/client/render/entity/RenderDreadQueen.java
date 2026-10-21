package com.iafabric.client.render.entity;

import com.iafabric.client.model.ModelDreadQueen;
import com.iafabric.client.render.entity.layer.LayerGenericGlowing;
import com.iafabric.client.render.entity.layer.LayerHeldItem;
import com.iafabric.entity.EntityDreadQueen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** Cryona, the Dread Queen: her armor cracked when she is badly hurt (asleep on her throne she is RenderCryonaAsleep). */
public class RenderDreadQueen extends IafMobRenderer<EntityDreadQueen, ModelDreadQueen<EntityDreadQueen>> {
	public static final Identifier TEXTURE = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_queen.png");
	public static final Identifier TEXTURE_INACTIVE = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_queen_inactive.png");
	public static final Identifier TEXTURE_INJURED = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_queen_injured.png");
	public static final Identifier TEXTURE_EYES = Identifier.parse("iceandfire_fabric:textures/models/dread/dread_queen_eyes.png");
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
	public void extractRenderState(EntityDreadQueen queen, State<EntityDreadQueen> state, float partialTicks) {
		super.extractRenderState(queen, state, partialTicks);
		if (queen.getVehicle() instanceof com.iafabric.entity.EntityDragonBase dragon && dragon.carriesRider(queen)) {
			float body = net.minecraft.util.Mth.rotLerp(partialTicks, dragon.yBodyRotO, dragon.yBodyRot);
			net.minecraft.world.phys.Vec3 seat = dragon.drawnSeat(partialTicks, body);
			state.x = seat.x;
			state.y = seat.y;
			state.z = seat.z;
			float head = state.bodyRot + state.yRot;
			state.bodyRot = body;
			state.yRot = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(head - body), -60F, 60F);
		}
	}

	@Override
	public Identifier getTextureLocation(EntityDreadQueen queen) {
		return queen.getHealth() < queen.getMaxHealth() * 0.35F ? TEXTURE_INJURED : TEXTURE;
	}
}
