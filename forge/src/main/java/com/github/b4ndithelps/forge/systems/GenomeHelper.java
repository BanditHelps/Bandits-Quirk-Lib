package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.capabilities.genome.GenomeCapabilityProvider;
import com.github.b4ndithelps.forge.capabilities.genome.IGenomeCapability;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.GenomeSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public class GenomeHelper {
    public static boolean isAvailable(Player player) {
        return player.getCapability(GenomeCapabilityProvider.GENOME_CAPABILITY).isPresent();
    }

    public static IGenomeCapability get(Player player) {
        return player.getCapability(GenomeCapabilityProvider.GENOME_CAPABILITY).orElse(null);
    }

    public static ListTag getGenome(Player player) {
        IGenomeCapability cap = get(player);
        return cap != null ? cap.getGenome() : new ListTag();
    }

    public static int size(Player player) {
        IGenomeCapability cap = get(player);
        return cap != null ? cap.size() : 0;
    }

    public static boolean addGene(Player player, CompoundTag gene) {
        IGenomeCapability cap = get(player);
        if (cap == null) return false;
        boolean added = cap.addGene(gene);
        if (added && player instanceof ServerPlayer sp) syncToClient(sp);
        return added;
    }

    public static boolean removeGeneById(Player player, String geneId) {
        IGenomeCapability cap = get(player);
        if (cap == null) return false;
        boolean removed = cap.removeGeneById(geneId);
        if (removed && player instanceof ServerPlayer sp) syncToClient(sp);
        return removed;
    }

    public static void clear(Player player) {
        IGenomeCapability cap = get(player);
        if (cap == null) return;
        cap.clear();
        if (player instanceof ServerPlayer sp) syncToClient(sp);
    }

    public static void setGenome(Player player, ListTag genome) {
        IGenomeCapability cap = get(player);
        if (cap == null) return;
        cap.setGenome(genome);
        if (player instanceof ServerPlayer sp) syncToClient(sp);
    }

    public static void syncToClient(ServerPlayer serverPlayer) {
        serverPlayer.getCapability(GenomeCapabilityProvider.GENOME_CAPABILITY)
                .ifPresent(cap -> {
                    CompoundTag data = cap.serializeNBT();
                    BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new GenomeSyncPacket(serverPlayer.getUUID(), data));
                });
    }
}