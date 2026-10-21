package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.item.IafItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;


public class MessageSpawnParticleAt {
    private double x;
    private double y;
    private double z;
    private int particleType;

    public MessageSpawnParticleAt() {
    }

    public MessageSpawnParticleAt(double x, double y, double z, int particleType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.particleType = particleType;
    }

    public static MessageSpawnParticleAt read(FriendlyByteBuf buf) {
        return new MessageSpawnParticleAt(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt());
    }

    public static void write(MessageSpawnParticleAt message, FriendlyByteBuf buf) {
        buf.writeDouble(message.x);
        buf.writeDouble(message.y);
        buf.writeDouble(message.z);
        buf.writeInt(message.particleType);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageSpawnParticleAt message, Player player) {
            if (player != null) {
                if (!player.getMainHandItem().isEmpty() && player.getMainHandItem().getItem() == IafItemRegistry.DRAGON_DEBUG_STICK) {
                    player.level().addParticle(ParticleTypes.SMOKE, message.x, message.y, message.z, 0, 0, 0);
                }
            }
        }
    }
}
