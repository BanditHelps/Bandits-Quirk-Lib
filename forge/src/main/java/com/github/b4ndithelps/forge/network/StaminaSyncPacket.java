package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.capabilities.stamina.StaminaDataProvider;
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
 * Packet for synchronizing Stamina capability data (including upgrade points) from server to client.
 */
public class StaminaSyncPacket {
    private final UUID playerUUID;
    private final CompoundTag staminaData;

    public StaminaSyncPacket(UUID playerUUID, CompoundTag staminaData) {
        this.playerUUID = playerUUID;
        this.staminaData = staminaData;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerUUID);
        buffer.writeNbt(staminaData);
    }

    public static StaminaSyncPacket decode(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        CompoundTag tag = buffer.readNbt();
        return new StaminaSyncPacket(id, tag);
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide()));
        return true;
    }

    private void handleClientSide() {
        Player player = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getPlayerByUUID(playerUUID) : null;
        if (player == null) return;

        player.getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(cap -> {
            // Apply full capability NBT; ensures upgradePoints and pointsProgress are in sync
            cap.loadNBTData(staminaData);
        });
    }

    public static StaminaSyncPacket fullSync(Player player) {
        CompoundTag tag = new CompoundTag();
        player.getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(cap -> cap.saveNBTData(tag));
        return new StaminaSyncPacket(player.getUUID(), tag);
    }
}