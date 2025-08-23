package com.github.b4ndithelps.forge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Syncs custom MineHa slot persistent data from server to client.
 *
 * We mirror existing sync packets and store the values inside the player's persistent data on the client.
 */
public class MineHaSlotSyncPacket {
    private final UUID playerUUID;
    private final int slotIndex; // -1 indicates full sync, otherwise single slot index
    private final String slotValue; // only used when slotIndex >= 0
    private final CompoundTag fullTag; // only used for full sync

    public MineHaSlotSyncPacket(UUID playerUUID, int slotIndex, String slotValue) {
        this.playerUUID = playerUUID;
        this.slotIndex = slotIndex;
        this.slotValue = slotValue;
        this.fullTag = null;
    }

    public MineHaSlotSyncPacket(UUID playerUUID, CompoundTag fullTag) {
        this.playerUUID = playerUUID;
        this.slotIndex = -1;
        this.slotValue = "";
        this.fullTag = fullTag;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerUUID);
        buffer.writeVarInt(slotIndex);
        if (slotIndex >= 0) {
            buffer.writeUtf(slotValue);
        } else {
            buffer.writeNbt(fullTag);
        }
    }

    public static MineHaSlotSyncPacket decode(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        int slot = buffer.readVarInt();
        if (slot >= 0) {
            String value = buffer.readUtf();
            return new MineHaSlotSyncPacket(id, slot, value);
        } else {
            CompoundTag tag = buffer.readNbt();
            return new MineHaSlotSyncPacket(id, tag);
        }
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide()));
        return true;
    }

    private void handleClientSide() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Player player = mc.level.getPlayerByUUID(playerUUID);
        if (player == null) return;

        if (slotIndex >= 0) {
            String key = "MineHa.Slot." + slotIndex;
            player.getPersistentData().putString(key, slotValue);
        } else if (fullTag != null) {
            // Expect keys like MineHa.Slot.1..N in fullTag
            for (String k : fullTag.getAllKeys()) {
                if (k.startsWith("MineHa.Slot.")) {
                    player.getPersistentData().putString(k, fullTag.getString(k));
                }
            }
        }
    }

    /**
     * Helper: build a full sync tag containing all slot keys from the server-side player's persistent data.
     */
    public static MineHaSlotSyncPacket fullSync(Player player, int maxSlotsInclusive) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i <= maxSlotsInclusive; i++) {
            String key = "MineHa.Slot." + i;
            String v = player.getPersistentData().getString(key);
            // Always include the key to ensure client clears stale values
            tag.putString(key, v);
        }
        return new MineHaSlotSyncPacket(player.getUUID(), tag);
    }
}


