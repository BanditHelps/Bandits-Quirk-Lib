package com.github.b4ndithelps.genetics;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable description of how a gene can be crafted by combining other genes.
 * A recipe may include explicit gene requirements and a number of builder genes
 * which are determined per-world using {@link GeneCombinationService}.
 */
public final class GeneCombinationRecipe {
    public static final int MAX_TOTAL_INGREDIENTS = 4;

    public static final class Requirement {
        private final ResourceLocation geneId;
        private final int minQuality;

        public Requirement(ResourceLocation geneId, int minQuality) {
            this.geneId = Objects.requireNonNull(geneId, "geneId");
            this.minQuality = Math.max(0, Math.min(100, minQuality));
        }

        public ResourceLocation getGeneId() { return geneId; }
        public int getMinQuality() { return minQuality; }
    }

    private final List<Requirement> explicitRequirements;
    private final int builderCount;
    private final int builderMinQuality;

    public GeneCombinationRecipe(List<Requirement> explicitRequirements, int builderCount, int builderMinQuality) {
        List<Requirement> reqs = explicitRequirements == null ? List.of() : new ArrayList<>(explicitRequirements);
        this.explicitRequirements = List.copyOf(reqs);
        int clampedBuilderCount = Math.max(0, builderCount);
        this.builderCount = clampedBuilderCount;
        this.builderMinQuality = Math.max(0, Math.min(100, builderMinQuality));
    }

    public List<Requirement> getExplicitRequirements() { return explicitRequirements; }
    public int getBuilderCount() { return builderCount; }
    public int getBuilderMinQuality() { return builderMinQuality; }

    public int totalIngredientCount() {
        return explicitRequirements.size() + builderCount;
    }

    public GeneCombinationRecipe clampedToMax() {
        int allowedBuilders = Math.max(0, MAX_TOTAL_INGREDIENTS - explicitRequirements.size());
        if (builderCount <= allowedBuilders) return this;
        return new GeneCombinationRecipe(explicitRequirements, allowedBuilders, builderMinQuality);
    }
}


