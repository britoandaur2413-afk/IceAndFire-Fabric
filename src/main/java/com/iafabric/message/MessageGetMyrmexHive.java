package com.iafabric.message;

import com.iafabric.IafMod;
import com.iafabric.entity.util.MyrmexHive;
import com.iafabric.world.MyrmexWorldData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;


public class MessageGetMyrmexHive {

    public CompoundTag hive;

    public MessageGetMyrmexHive(CompoundTag hive) {
        this.hive = hive;
    }

    public MessageGetMyrmexHive() {
    }

    public static MessageGetMyrmexHive read(FriendlyByteBuf buf) {
        return new MessageGetMyrmexHive(buf.readNbt());
    }

    public static void write(MessageGetMyrmexHive message, FriendlyByteBuf buf) {
        buf.writeNbt(message.hive);
    }

    public static class Handler {
        public Handler() {
        }

        public static void handle(MessageGetMyrmexHive message, Player player) {
            MyrmexHive serverHive = MyrmexHive.fromNBT(message.hive);
            CompoundTag tag = new CompoundTag();
            serverHive.writeVillageDataToNBT(tag);
            serverHive.readVillageDataFromNBT(tag);
            IafMod.PROXY.setReferencedHive(serverHive);
            if (player == null || player.level().isClientSide) {
                player = IafMod.PROXY.getClientSidePlayer();
            }else {
                if (MyrmexWorldData.get(player.level()) != null) {
                    MyrmexHive realHive = MyrmexWorldData.get(player.level()).getHiveFromUUID(serverHive.hiveUUID);
                    // a hive gone since the staff screen opened (its queen died) has nothing to update
                    if (realHive != null) {
                        realHive.readVillageDataFromNBT(serverHive.toNBT());
                    }
                }
            }

        }
    }
}