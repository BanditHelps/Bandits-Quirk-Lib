package com.github.b4ndithelps.genetics;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class Gene {
    public enum Category { resistance, cosmetic, builder, lowend, quirk }
    public enum Rarity { common, uncommon, rare, very_rare }

    private final ResourceLocation id;
    private final Category category;
    private final Rarity rarity;
    private final int qualityMin;
    private final int qualityMax;
    private final boolean combinable;
    private final String description;
    private final List<String> mobs;
    private final GeneCombinationRecipe combinationRecipe; // optional

    public Gene(ResourceLocation id,
                Category category,
                Rarity rarity,
                int qualityMin,
                int qualityMax,
                boolean combinable,
                String description,
                List<String> mobs,
                GeneCombinationRecipe combinationRecipe) {
        this.id = id;
        this.category = category;
        this.rarity = rarity;
        this.qualityMin = qualityMin;
        this.qualityMax = qualityMax;
        this.combinable = combinable;
        this.description = description;
        this.mobs = mobs;
        this.combinationRecipe = combinationRecipe;
    }

    public ResourceLocation getId() { return id; }
    public Category getCategory() { return category; }
    public Rarity getRarity() { return rarity; }
    public int getQualityMin() { return qualityMin; }
    public int getQualityMax() { return qualityMax; }
    public boolean isCombinable() { return combinable; }
    public String getDescription() { return description; }
    public List<String> getMobs() { return mobs; }
    public GeneCombinationRecipe getCombinationRecipe() { return combinationRecipe; }
}


