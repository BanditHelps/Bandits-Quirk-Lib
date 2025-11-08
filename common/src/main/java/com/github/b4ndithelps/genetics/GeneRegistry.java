package com.github.b4ndithelps.genetics;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GeneRegistry {
    private static final Map<ResourceLocation, Gene> idToGene = new HashMap<>();
    private static volatile List<Gene> cachedList = List.of();

    private GeneRegistry() {}

    public static void clear() {
        idToGene.clear();
        cachedList = List.of();
    }

    public static void register(Gene gene) {
        idToGene.put(gene.getId(), gene);
        cachedList = List.copyOf(idToGene.values());
    }

    public static Gene get(ResourceLocation id) { return idToGene.get(id); }

    public static List<Gene> all() { return cachedList; }

    public static List<Gene> getAllOfType(String type) {
        List<Gene> output = new ArrayList<>();
        for (Gene gene : cachedList) {
            if (gene.getMobs().contains(type)) {
                output.add(gene);
            }
        }

        return output;
    }

    public static boolean isEmpty() { return idToGene.isEmpty(); }
}


