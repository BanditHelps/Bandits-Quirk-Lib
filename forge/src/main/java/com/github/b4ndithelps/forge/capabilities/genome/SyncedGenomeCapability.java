package com.github.b4ndithelps.forge.capabilities.genome;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class SyncedGenomeCapability implements IGenomeCapability {
    private final GenomeCapability base = new GenomeCapability();
    private final Supplier<Player> playerSupplier;

    public SyncedGenomeCapability(Supplier<Player> playerSupplier) { this.playerSupplier = playerSupplier; }

    private void triggerSync() {
        Player p = playerSupplier == null ? null : playerSupplier.get();
        if (p instanceof ServerPlayer sp) GenomeHelper.syncToClient(sp);
    }

    @Override public ListTag getGenome() { return base.getGenome(); }
    @Override public void setGenome(ListTag genome) { base.setGenome(genome); triggerSync(); }
    @Override public boolean addGene(CompoundTag gene) { boolean r = base.addGene(gene); if (r) triggerSync(); return r; }
    @Override public boolean removeGeneById(String geneId) { boolean r = base.removeGeneById(geneId); if (r) triggerSync(); return r; }
    @Override public void clear() { base.clear(); triggerSync(); }
    @Override public int size() { return base.size(); }
    @Override public CompoundTag serializeNBT() { return base.serializeNBT(); }
    @Override public void deserializeNBT(CompoundTag tag) { base.deserializeNBT(tag); }
}


