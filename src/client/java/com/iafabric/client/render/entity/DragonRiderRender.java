package com.iafabric.client.render.entity;

import com.iafabric.entity.DragonRiders;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.entity.EntityDreadKnight;
import com.iafabric.entity.IafEntityRegistry;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The rider drawn on a kingdom's or the dread's dragon (DragonRiders): a pillager with its crossbow, the warlord with
 * the illagers' banner, or a dread knight with its sword, seated on the dragon's back and turned with it. Each dragon
 * keeps its own rider to draw (never in the world), and the rider shows red when hit.
 */
public final class DragonRiderRender {
	private static final Map<EntityDragonBase, Drawn> DRAWN = new WeakHashMap<>();

	private DragonRiderRender() {
	}

	private record Drawn(LivingEntity entity, int kind) {
	}

	/**
	 * A dragon's virtual rider, set where it sits and turned with its body as it is drawn this frame (none if it has
	 * none): its dragon draws it in its seat (RenderDragonBase).
	 */
	static @Nullable LivingEntity prepare(EntityDragonBase dragon, float body, float partialTicks) {
		int kind = dragon.getVirtualRider();
		if (kind == DragonRiders.NONE || dragon.isModelDead() || !(dragon.level() instanceof ClientLevel level)) {
			return null;
		}
		Drawn drawn = DRAWN.get(dragon);
		if (drawn == null || drawn.kind != kind || drawn.entity.level() != level) {
			LivingEntity rider = make(dragon, kind, level);
			if (rider == null) {
				return null;
			}
			// (an id no entity of the world has)
			rider.setId(-1000 - dragon.getId());
			drawn = new Drawn(rider, kind);
			DRAWN.put(dragon, drawn);
		}
		LivingEntity rider = drawn.entity;
		// where it sits, turned with the dragon's body as it is drawn
		float yaw = dragon.getYRot() * Mth.DEG_TO_RAD;
		Vec3 seat = DragonRiders.seat(dragon).subtract(dragon.position());
		double along = seat.x * -Mth.sin(yaw) + seat.z * Mth.cos(yaw);
		float turn = body * Mth.DEG_TO_RAD;
		Vec3 offset = new Vec3(-Mth.sin(turn) * along, seat.y, Mth.cos(turn) * along);
		double x = Mth.lerp(partialTicks, dragon.xo, dragon.getX()) + offset.x;
		double y = Mth.lerp(partialTicks, dragon.yo, dragon.getY()) + offset.y;
		double z = Mth.lerp(partialTicks, dragon.zo, dragon.getZ()) + offset.z;
		float head = Mth.rotLerp(partialTicks, dragon.yHeadRotO, dragon.yHeadRot);
		head = body + Mth.clamp(Mth.wrapDegrees(head - body), -50F, 50F);
		rider.setPos(x, y, z);
		rider.setYRot(body);
		rider.setXRot(0F);
		rider.setOldPosAndRot();
		rider.yBodyRot = body;
		rider.yBodyRotO = body;
		rider.yHeadRot = head;
		rider.yHeadRotO = head;
		rider.tickCount = dragon.tickCount;
		rider.hurtTime = Math.max(0, dragon.riderHurtTime);
		return rider;
	}

	private static LivingEntity make(EntityDragonBase dragon, int kind, ClientLevel level) {
		if (kind == DragonRiders.KNIGHT) {
			EntityDreadKnight knight = IafEntityRegistry.DREAD_KNIGHT.create(level);
			if (knight == null) {
				return null;
			}
			int look = Math.floorMod(dragon.getUUID().hashCode(), 6);
			knight.seatedOnDragon = true;
			knight.setArmorVariant(look % 3);
			knight.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(IafItemRegistry.DREAD_KNIGHT_SWORD));
			if (look >= 3) {
				knight.setItemSlot(EquipmentSlot.OFFHAND, knight.generateShield());
			}
			return knight;
		}
		LivingEntity pillager = EntityType.PILLAGER.create(level);
		if (pillager == null) {
			return null;
		}
		pillager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
		if (kind == DragonRiders.WARLORD) {
			pillager.setItemSlot(EquipmentSlot.HEAD, Raid.getLeaderBannerInstance());
		}
		return pillager;
	}
}
