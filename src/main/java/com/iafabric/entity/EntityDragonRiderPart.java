package com.iafabric.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/** The hitbox of the rider drawn on a dragon's back (DragonRiders): hit apart from the dragon, with its own health. */
public class EntityDragonRiderPart extends EntityDragonPart {
	private final EntityDragonBase mount;

	public EntityDragonRiderPart(EntityDragonBase dragon) {
		super(dragon, 0F, 0F, 0F, 0.9F, 1.5F, 1F);
		this.mount = dragon;
		// (it keeps to the seat on its own, on the server and on each player's game: EntityDragonPart)
		this.setPart(RIDER);
		Vec3 seat = DragonRiders.seat(dragon);
		this.setPos(seat.x, seat.y + 0.1, seat.z);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		// only a creature's blow or shot (no fire, falls, walls or blasts of its own)
		if (this.isInvulnerableTo(level, source) || source.getEntity() == null && source.getDirectEntity() == null) {
			return false;
		}
		return this.mount != null && DragonRiders.hurtRider(this.mount, level, source, damage);
	}
}
