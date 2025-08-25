package com.github.b4ndithelps.forge.genetics;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class GenesReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    public GenesReloadListener() {
        super(GSON, "genes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        GeneRegistry.clear();
        int loaded = 0;
        for (Map.Entry<ResourceLocation, JsonElement> e : elements.entrySet()) {
            try {
                JsonObject json = e.getValue().getAsJsonObject();
                String idStr = json.has("id") ? json.get("id").getAsString() : new ResourceLocation(e.getKey().getNamespace(), e.getKey().getPath()).toString();
                ResourceLocation id = new ResourceLocation(idStr);

                Gene.Category category = Gene.Category.valueOf(json.get("category").getAsString());
                Gene.Rarity rarity = Gene.Rarity.valueOf(json.get("rarity").getAsString());
                int qMin = Math.max(0, json.getAsJsonArray("quality_range").get(0).getAsInt());
                int qMax = Math.max(qMin, json.getAsJsonArray("quality_range").get(1).getAsInt());
                boolean combinable = json.get("combinable").getAsBoolean();
                String description = json.has("description") ? json.get("description").getAsString() : "";

                Gene gene = new Gene(id, category, rarity, qMin, qMax, combinable, description);
                GeneRegistry.register(gene);
                loaded++;
            } catch (Exception ex) {
                BanditsQuirkLib.LOGGER.error("Failed to parse gene json {}: {}", e.getKey(), ex.toString());
            }
        }
        BanditsQuirkLib.LOGGER.info("Loaded {} genes", loaded);
    }
}


