package com.github.b4ndithelps.forge.capabilities.genome;

import com.github.b4ndithelps.values.GeneticsConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class GenomeCapability implements IGenomeCapability {
    private final ListTag genome = new ListTag();

    @Override
    public ListTag getGenome() { return genome; }

    @Override
    public void setGenome(ListTag newGenome) {
        genome.clear();
        if (newGenome != null) {
            for (int i = 0; i < newGenome.size(); i++) {
                if (genome.size() >= GeneticsConstants.PLAYER_MAX_GENES) break;
                Tag t = newGenome.get(i);
                if (t instanceof CompoundTag ct) genome.add(ct.copy());
            }
        }
    }

    @Override
    public boolean addGene(CompoundTag gene) {
        if (gene == null) return false;
        if (genome.size() >= GeneticsConstants.PLAYER_MAX_GENES) return false;
        genome.add(gene.copy());
        return true;
    }

    @Override
    public boolean removeGeneById(String geneId) {
        if (geneId == null) return false;
        for (int i = 0; i < genome.size(); i++) {
            CompoundTag g = genome.getCompound(i);
            if (geneId.equals(g.getString("id"))) {
                genome.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() { genome.clear(); }

    @Override
    public int size() { return genome.size(); }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("genome", genome.copy());
        tag.putInt("max", GeneticsConstants.PLAYER_MAX_GENES);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        genome.clear();
        if (tag != null && tag.contains("genome", 9)) {
            setGenome(tag.getList("genome", 10));
        }
    }
}


