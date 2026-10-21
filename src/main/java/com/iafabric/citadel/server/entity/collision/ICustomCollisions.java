package com.iafabric.citadel.server.entity.collision;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** An entity that moves through some blocks (the death worm swims through sand). EntityMixin routes Entity#collide here. */
public interface ICustomCollisions {

    boolean canPassThrough(BlockPos pos, BlockState state, VoxelShape shape);

    static Vec3 getAllowedMovementForEntity(Entity entity, Vec3 movement) {
        if (movement.lengthSqr() == 0.0D) {
            return movement;
        }
        ICustomCollisions custom = (ICustomCollisions) entity;
        AABB box = entity.getBoundingBox();
        AABB area = box.expandTowards(movement);
        ImmutableList.Builder<VoxelShape> shapes = ImmutableList.builder();
        shapes.addAll(entity.level().getEntityCollisions(entity, area));
        CollisionContext context = CollisionContext.of(entity);
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(area.minX - 1.0E-7D) - 1, Mth.floor(area.minY - 1.0E-7D) - 1, Mth.floor(area.minZ - 1.0E-7D) - 1,
            Mth.floor(area.maxX + 1.0E-7D) + 1, Mth.floor(area.maxY + 1.0E-7D) + 1, Mth.floor(area.maxZ + 1.0E-7D) + 1)) {
            BlockState state = entity.level().getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape(entity.level(), pos, context);
            if (!shape.isEmpty() && !custom.canPassThrough(pos, state, shape)) {
                shapes.add(shape.move(pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        List<VoxelShape> list = shapes.build();
        Vec3 resolved = Vec3.ZERO;
        for (Direction.Axis axis : Direction.axisStepOrder(movement)) {
            double along = movement.get(axis);
            if (along != 0.0D) {
                resolved = resolved.with(axis, Shapes.collide(axis, box.move(resolved), list, along));
            }
        }
        return resolved;
    }
}
