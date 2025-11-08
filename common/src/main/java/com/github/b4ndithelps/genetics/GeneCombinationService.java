package com.github.b4ndithelps.genetics;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Resolves per-world builder gene selections and validates combination inputs.
 */
public final class GeneCombinationService {
    private GeneCombinationService() {}

    private static final Map<ResourceKey<Level>, List<ResourceLocation>> worldToBuilderGenes = new HashMap<>();

    /**
     * Returns the deterministic builder gene set for the given world.
     * Selection is stable per world key and changes across different worlds.
     */
    public static List<ResourceLocation> getOrCreateBuilderGenes(MinecraftServer server) {
        ResourceKey<Level> key = Level.OVERWORLD;
        // If called on logical server, we can vary by actual world keys if needed; keep it simple for now
        return worldToBuilderGenes.computeIfAbsent(key, k -> computeBuilderGenes(server));
    }

    private static List<ResourceLocation> computeBuilderGenes(MinecraftServer server) {
        long seed = server.getWorldData().worldGenOptions().seed();
        Random rng = new Random(seed ^ 0x9E3779B97F4A7C15L);
        // Pick from all registered builder category genes
        List<Gene> candidates = new ArrayList<>();
        for (Gene g : GeneRegistry.all()) {
            if (g.getCategory() == Gene.Category.builder) candidates.add(g);
        }
        candidates.sort(Comparator.comparing(g -> g.getId().toString()));
        Collections.shuffle(candidates, rng);
        // We'll keep an ordered list; the actual count needed comes from the recipe
        List<ResourceLocation> ids = new ArrayList<>();
        for (Gene g : candidates) ids.add(g.getId());
        return List.copyOf(ids);
    }

    /**
     * Verifies that provided ingredients satisfy the target gene's recipe.
     * Does not consume items; only checks IDs and qualities. Builder gene choices are interpreted
     * using the per-world selection order.
     */
    public static boolean matchesRecipe(MinecraftServer server,
                                        Gene target,
                                        List<GeneIngredient> provided) {
        GeneCombinationRecipe recipe = target.getCombinationRecipe();
        if (recipe == null) return false;
        if (recipe.totalIngredientCount() > GeneCombinationRecipe.MAX_TOTAL_INGREDIENTS) return false;

        // Count explicit matches
        Map<ResourceLocation, Integer> explicitNeeded = new HashMap<>();
        Map<ResourceLocation, Integer> minQuality = new HashMap<>();
        for (GeneCombinationRecipe.Requirement req : recipe.getExplicitRequirements()) {
            explicitNeeded.merge(req.getGeneId(), 1, Integer::sum);
            minQuality.merge(req.getGeneId(), req.getMinQuality(), Math::max);
        }

        List<GeneIngredient> remaining = new ArrayList<>();
        for (GeneIngredient ing : provided) {
            Integer needed = explicitNeeded.get(ing.id);
            if (needed != null && needed > 0 && ing.quality >= minQuality.getOrDefault(ing.id, 0)) {
                explicitNeeded.put(ing.id, needed - 1);
            } else {
                remaining.add(ing);
            }
        }
        for (Integer left : explicitNeeded.values()) {
            if (left != 0) return false;
        }

        // Check builder requirements
        int buildersNeeded = recipe.getBuilderCount();
        if (buildersNeeded == 0) return remaining.isEmpty();

        List<ResourceLocation> builderOrder = getOrCreateBuilderGenes(server);
        int satisfied = 0;
        for (GeneIngredient ing : remaining) {
            if (ing.quality < recipe.getBuilderMinQuality()) continue;
            // Accept if this ingredient is one of the allowed builder genes by order
            if (builderOrder.contains(ing.id)) {
                satisfied++;
            } else {
                return false; // non-builder ingredient present
            }
        }
        return satisfied == buildersNeeded;
    }

    /** Simple value holder for id+quality during checks. */
    public static final class GeneIngredient {
        public final ResourceLocation id;
        public final int quality;

        public GeneIngredient(ResourceLocation id, int quality) {
            this.id = id;
            this.quality = quality;
        }
    }
}


