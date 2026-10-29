package com.iafabric.client.render.entity;

import com.iafabric.entity.DragonSaddle;
import com.iafabric.entity.EntityDragonBase;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A dragon's rider sits in a seat on its back bone: the dragon draws it there, in its own pose (RenderDragonBase), and
 * the game does not draw it where it would (DragonSeatMixin).
 */
public final class DragonSeat {
	private DragonSeat() {
	}

	/**
	 * Whether this is a rider sitting on a grown dragon's back: the one steering it, one it carries (Cryona on Black
	 * Frost, a pillager) or a friend behind in a double saddle; not its prey in its jaws, not a small one on a player's
	 * shoulder.
	 */
	public static boolean seated(@Nullable Entity rider) {
		return rider instanceof LivingEntity && rider.getVehicle() instanceof EntityDragonBase dragon && dragon.getDragonStage() > 2
			&& (dragon.getControllingPassenger() == rider || dragon.carriesRider(rider) || DragonSaddle.inBackSeat(dragon, rider));
	}

	/** Whether this rider is the one whose eyes the game is seen through (in first person it is not drawn at all). */
	public static boolean inOwnEyes(@Nullable Entity rider) {
		Minecraft mc = Minecraft.getInstance();
		return rider == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
	}
}
