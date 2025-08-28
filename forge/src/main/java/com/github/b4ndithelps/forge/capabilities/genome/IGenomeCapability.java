package com.github.b4ndithelps.forge.capabilities.genome;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public interface IGenomeCapability {
    /**
     * @return the genome NBT list (list of compound tags with id, quality, name)
     */
    ListTag getGenome();

    /** Replace the entire genome list. */
    void setGenome(ListTag genome);

    /** Adds a gene entry; returns true if added, false if limit reached. */
    boolean addGene(CompoundTag gene);

    /** Removes first gene with matching id; returns true if removed. */
    boolean removeGeneById(String geneId);

    /** Clears all genes. */
    void clear();

    /** @return current count */
    int size();

    /** Serialization */
    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);
}


