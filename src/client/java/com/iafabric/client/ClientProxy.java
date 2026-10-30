package com.iafabric.client;

import com.iafabric.CommonProxy;
import com.iafabric.entity.util.MyrmexHive;
import com.iafabric.client.gui.GuiMyrmexAddRoom;
import com.iafabric.client.gui.GuiMyrmexStaff;
import com.iafabric.client.gui.bestiary.GuiBestiary;
import com.iafabric.client.particle.*;
import com.iafabric.entity.EntityDragonBase;
import com.iafabric.enums.EnumParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** What only the client can do: its player, its view, the screens it opens. */
public class ClientProxy extends CommonProxy {

    public static Set<UUID> currentDragonRiders = new HashSet<>();
    private static MyrmexHive referedClientHive = null;
    private int previousViewType = 0;
    private int thirdPersonViewDragon = 0;
    private volatile Entity referencedMob = null;
    private BlockEntity referencedTE = null;

    public static MyrmexHive getReferedClientHive() {
        return referedClientHive;
    }

    @Override
    public void setReferencedHive(MyrmexHive hive) {
        referedClientHive = hive;
    }

    @Override
    public Object getFontRenderer() {
        return Minecraft.getInstance().font;
    }

    @Override
    public int getDragon3rdPersonView() {
        return thirdPersonViewDragon;
    }

    @Override
    public void setDragon3rdPersonView(int view) {
        thirdPersonViewDragon = view;
    }

    @Override
    public int getPreviousViewType() {
        return previousViewType;
    }

    @Override
    public void setPreviousViewType(int view) {
        previousViewType = view;
    }

    @Override
    public boolean shouldSeeBestiaryContents() {
        return Minecraft.getInstance().hasShiftDown();
    }

    /**
     * The mob whose menu is open, as the player's game has it. In a world of one's own the server sets it too (one game
     * runs both): its own copy is never given out, the game's copy of the same mob is (the server's copy has the
     * server's players as riders, which nothing can draw: riding a dragon, its menu crashed the game).
     */
    @Override
    public Entity getReferencedMob() {
        Entity mob = referencedMob;
        if (mob != null && !mob.level().isClientSide()) {
            ClientLevel level = Minecraft.getInstance().level;
            return level == null ? null : level.getEntity(mob.getId());
        }
        return mob;
    }

    @Override
    public void setReferencedMob(Entity dragonBase) {
        referencedMob = dragonBase;
    }

    @Override
    public BlockEntity getRefrencedTE() {
        return referencedTE;
    }

    @Override
    public void setRefrencedTE(BlockEntity tileEntity) {
        referencedTE = tileEntity;
    }

    @Override
    public Player getClientSidePlayer() {
        return Minecraft.getInstance().player;
    }

    @Override
    public void spawnDragonParticle(EnumParticles name, double x, double y, double z, double motX, double motY, double motZ, EntityDragonBase dragon) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        if (name == EnumParticles.DragonFire) {
            Minecraft.getInstance().particleEngine.add(new ParticleDragonFlame(world, x, y, z, motX, motY, motZ, dragon, 0));
        } else if (name == EnumParticles.DragonIce) {
            Minecraft.getInstance().particleEngine.add(new ParticleDragonFrost(world, x, y, z, motX, motY, motZ, dragon, 0));
        }
    }

    @Override
    public void tickDragonBreath(EntityDragonBase dragon) {
        DragonBreathFx.tick(dragon);
    }

    @Override
    public void dragonCough(EntityDragonBase dragon) {
        DragonBreathFx.cough(dragon);
    }

    @Override
    public void spawnParticle(EnumParticles name, double x, double y, double z, double motX, double motY, double motZ) {
        spawnParticle(name, x, y, z, motX, motY, motZ, 1.0F);
    }

    @Override
    public void spawnParticle(EnumParticles name, double x, double y, double z, double motX, double motY, double motZ, float size) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) {
            return;
        }
        Particle particle = switch (name) {
            case DragonFire -> new ParticleDragonFlame(world, x, y, z, motX, motY, motZ, size);
            case DragonIce -> new ParticleDragonFrost(world, x, y, z, motX, motY, motZ, size);
            case Dread_Torch -> new ParticleDreadTorch(world, x, y, z, motX, motY, motZ, size);
            case Dread_Portal -> new ParticleDreadPortal(world, x, y, z, motX, motY, motZ, size);
            case Blood -> new ParticleBlood(world, x, y, z);
            case If_Pixie -> new ParticlePixieDust(world, x, y, z, (float) motX, (float) motY, (float) motZ);
            case Siren_Appearance -> new ParticleSirenAppearance(world, x, y, z, (int) motX);
            case Ghost_Appearance -> new ParticleGhostAppearance(world, x, y, z, (int) motX);
            case Siren_Music -> new ParticleSirenMusic(world, x, y, z, motX, motY, motZ, 1);
            case Serpent_Bubble -> new ParticleSerpentBubble(world, x, y, z, motX, motY, motZ, 1);
            case Hydra -> new ParticleHydraBreath(world, x, y, z, (float) motX, (float) motY, (float) motZ);
            default -> null;
        };
        if (particle != null) {
            Minecraft.getInstance().particleEngine.add(particle);
        }
    }

    @Override
    public void openBestiaryGui(ItemStack book) {
        Minecraft.getInstance().gui.setScreen(new GuiBestiary(book));
    }

    @Override
    public void openMyrmexStaffGui(ItemStack staff) {
        Minecraft.getInstance().gui.setScreen(new GuiMyrmexStaff(staff));
    }

    @Override
    public void openMyrmexAddRoomGui(ItemStack staff, BlockPos pos, Direction facing) {
        Minecraft.getInstance().gui.setScreen(new GuiMyrmexAddRoom(staff, pos, facing));
    }
}
