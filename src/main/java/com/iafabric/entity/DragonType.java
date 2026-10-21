package com.iafabric.entity;


import net.minecraft.world.entity.EntityTypes;
import com.iafabric.util.EntityUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import com.iafabric.util.BlockMaterial;
import com.iafabric.IafConfig;
import com.iafabric.block.BlockEggInIce;
import com.iafabric.block.IafBlockRegistry;
import com.iafabric.entity.tile.TileEntityEggInIce;
import com.iafabric.misc.IafSoundRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.Blocks;

public class DragonType {

    public static final DragonType FIRE = new DragonType("fire");
    public static final DragonType ICE = new DragonType("ice").setPiscivore();
    public static final DragonType LIGHTNING = new DragonType("lightning");

    private String name;
    private boolean piscivore;

    public DragonType(String name) {
        this.name = name;
    }

    public static String getNameFromInt(int type){
        if(type == 2){
            return "lightning";
        }else if (type == 1){
            return "ice";
        }else{
            return "fire";
        }
    }

    public static int getIntFromType(DragonType type){
        if(type == LIGHTNING){
            return 2;
        }else if (type == ICE){
            return 1;
        }else{
            return 0;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPiscivore() {
        return piscivore;
    }

    public DragonType setPiscivore() {
        piscivore = true;
        return this;
    }

    public void updateEggCondition(EntityDragonEgg egg) {
        BlockPos pos = BlockPos.containing(egg.position());
        if (this == FIRE) {
            if (BlockMaterial.FIRE.is(egg.level().getBlockState(pos))) {
                egg.setDragonAge(egg.getDragonAge() + 1);
            }
            if (egg.getDragonAge() > IafConfig.dragonEggTime) {
                egg.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                EntityFireDragon dragon = new EntityFireDragon(egg.level());
                if (egg.hasCustomName()) {
                    dragon.setCustomName(egg.getCustomName());
                }
                dragon.setVariant(egg.getEggType().ordinal());
                dragon.setGender(egg.getRandom().nextBoolean());
                dragon.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                dragon.setHunger(50);
                if (!egg.level().isClientSide()) {
                    egg.level().addFreshEntity(dragon);
                }
                if (egg.hasCustomName()) {
                    dragon.setCustomName(egg.getCustomName());
                }
                dragon.setTame(true, true);
                EntityUtil.setOwnerUUID(dragon, egg.getOwnerId());
                egg.level().playLocalSound(egg.getX(), egg.getY() + egg.getEyeHeight(), egg.getZ(), SoundEvents.FIRE_EXTINGUISH, egg.getSoundSource(), 2.5F, 1.0F, false);
                egg.level().playLocalSound(egg.getX(), egg.getY() + egg.getEyeHeight(), egg.getZ(), IafSoundRegistry.EGG_HATCH, egg.getSoundSource(), 2.5F, 1.0F, false);
                egg.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        if (this == ICE) {
            if (BlockMaterial.WATER.is(egg.level().getBlockState(pos)) && egg.getRandom().nextInt(500) == 0) {
                egg.level().setBlockAndUpdate(pos, IafBlockRegistry.EGG_IN_ICE.defaultBlockState());
                egg.level().playLocalSound(egg.getX(), egg.getY() + egg.getEyeHeight(), egg.getZ(), SoundEvents.GLASS_BREAK, egg.getSoundSource(), 2.5F, 1.0F, false);
                if (egg.level().getBlockState(pos).getBlock() instanceof BlockEggInIce) {
                    ((TileEntityEggInIce) egg.level().getBlockEntity(pos)).type = egg.getEggType();
                    ((TileEntityEggInIce) egg.level().getBlockEntity(pos)).ownerUUID = egg.getOwnerId();
                }
                egg.remove(Entity.RemovalReason.DISCARDED);
            }
        }
        if (this == LIGHTNING) {
            boolean flag;
            BlockPos.MutableBlockPos blockpos$pooledmutable = new BlockPos.MutableBlockPos(egg.getX(), egg.getY(), egg.getZ());
            flag = egg.level().isRainingAt(blockpos$pooledmutable) || egg.level().isRainingAt(blockpos$pooledmutable.set(egg.getX(), egg.getY() + (double) egg.getBbHeight(), egg.getZ()));
            if (egg.level().canSeeSky(egg.blockPosition().above()) && flag) {
                egg.setDragonAge(egg.getDragonAge() + 1);
            }
            if (egg.getDragonAge() > IafConfig.dragonEggTime) {
                EntityLightningDragon dragon = new EntityLightningDragon(egg.level());
                if (egg.hasCustomName()) {
                    dragon.setCustomName(egg.getCustomName());
                }
                dragon.setVariant(egg.getEggType().ordinal() - 8);
                dragon.setGender(egg.getRandom().nextBoolean());
                dragon.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                dragon.setHunger(50);
                if (!egg.level().isClientSide()) {
                    egg.level().addFreshEntity(dragon);
                }
                if (egg.hasCustomName()) {
                    dragon.setCustomName(egg.getCustomName());
                }
                dragon.setTame(true, true);
                EntityUtil.setOwnerUUID(dragon, egg.getOwnerId());
                LightningBolt lightningboltentity = EntityTypes.LIGHTNING_BOLT.create(egg.level(), EntitySpawnReason.TRIGGERED);
                lightningboltentity.setPos(egg.getX(), egg.getY(), egg.getZ());
                lightningboltentity.setVisualOnly(true);
                if (!egg.level().isClientSide()) {
                    egg.level().addFreshEntity(lightningboltentity);
                }
                egg.level().playLocalSound(egg.getX(), egg.getY() + egg.getEyeHeight(), egg.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, egg.getSoundSource(), 2.5F, 1.0F, false);
                egg.level().playLocalSound(egg.getX(), egg.getY() + egg.getEyeHeight(), egg.getZ(), IafSoundRegistry.EGG_HATCH, egg.getSoundSource(), 2.5F, 1.0F, false);
                egg.remove(Entity.RemovalReason.DISCARDED);


            }
        }
    }
}
