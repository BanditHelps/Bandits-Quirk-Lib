package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.capabilities.body.BodyStatusCapabilityProvider;
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
 * Packet for synchronizing BodyStatus capability data from server to client.
 * This ensures that client-side access to BodyStatus data returns the same values as server-side.
 */
public class BodyStatusSyncPacket {
    private final UUID playerUUID;
    private final CompoundTag bodyStatusData;

    public BodyStatusSyncPacket(UUID playerUUID, CompoundTag bodyStatusData) {
        this.playerUUID = playerUUID;
        this.bodyStatusData = bodyStatusData;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerUUID);
        buffer.writeNbt(bodyStatusData);
    }

    public static BodyStatusSyncPacket decode(FriendlyByteBuf buffer) {
        UUID playerUUID = buffer.readUUID();
        CompoundTag bodyStatusData = buffer.readNbt();
        return new BodyStatusSyncPacket(playerUUID, bodyStatusData);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // This runs on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide());
        });
        return true;
    }

    private void handleClientSide() {
        Player player = Minecraft.getInstance().level.getPlayerByUUID(playerUUID);
        if (player != null) {
            player.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                    .ifPresent(bodyStatus -> {
                        // Deserialize the NBT data to sync the capability
                        bodyStatus.deserializeNBT(bodyStatusData);
                    });
        }
    }
}