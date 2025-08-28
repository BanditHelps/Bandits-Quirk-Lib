package com.github.b4ndithelps.forge.network;

import com.github.b4ndithelps.forge.capabilities.genome.GenomeCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class GenomeSyncPacket {
    private final UUID playerUUID;
    private final CompoundTag genomeData;

    public GenomeSyncPacket(UUID playerUUID, CompoundTag genomeData) {
        this.playerUUID = playerUUID;
        this.genomeData = genomeData;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(playerUUID);
        buffer.writeNbt(genomeData);
    }

    public static GenomeSyncPacket decode(FriendlyByteBuf buffer) {
        return new GenomeSyncPacket(buffer.readUUID(), buffer.readNbt());
    }

    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide()));
        return true;
    }

    private void handleClientSide() {
        Player player = Minecraft.getInstance().level.getPlayerByUUID(this.playerUUID);
        if (player != null) {
            player.getCapability(GenomeCapabilityProvider.GENOME_CAPABILITY).ifPresent(cap -> cap.deserializeNBT(this.genomeData));
        }
    }
}


