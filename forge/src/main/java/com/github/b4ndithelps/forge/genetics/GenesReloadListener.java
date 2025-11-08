package com.github.b4ndithelps.forge.genetics;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneRegistry;
import com.github.b4ndithelps.genetics.GeneCombinationRecipe;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
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

                List<String> mobList = new ArrayList<>();
                // List of mob types. Defaults to ALL when not defined
                if (json.has("mobs")) {
                    JsonArray elementList = json.get("mobs").getAsJsonArray();

                    for (int x = 0; x < elementList.size(); x++ ) {
                        mobList.add(elementList.get(x).toString().replace("\"", ""));
                        System.out.println(elementList.get(x).toString());
                    }
                } else {
                    mobList = new ArrayList<>();
                    mobList.add("minecraft:player");
                    mobList.add("minecraft:drowned");
                    mobList.add("minecraft:zombie");
                    mobList.add("minecraft:villager");
                    mobList.add("minecraft:husk");
                }

                GeneCombinationRecipe combinationRecipe = null;
                if (json.has("combination")) {
                    JsonObject comb = json.getAsJsonObject("combination");
                    List<GeneCombinationRecipe.Requirement> reqs = new ArrayList<>();
                    int builderCount = 0;
                    int builderMinQuality = 0;

                    if (comb.has("requires")) {
                        JsonArray arr = comb.getAsJsonArray("requires");
                        for (JsonElement el : arr) {
                            JsonObject reqObj = el.getAsJsonObject();
                            String reqIdStr = reqObj.get("id").getAsString();
                            int reqMinQ = reqObj.has("min_quality") ? reqObj.get("min_quality").getAsInt() : 0;
                            reqs.add(new GeneCombinationRecipe.Requirement(new ResourceLocation(reqIdStr), reqMinQ));
                        }
                    }
                    if (comb.has("builder")) {
                        JsonObject b = comb.getAsJsonObject("builder");
                        builderCount = Math.max(0, b.has("count") ? b.get("count").getAsInt() : 0);
                        builderMinQuality = Math.max(0, Math.min(100, b.has("min_quality") ? b.get("min_quality").getAsInt() : 0));
                    }

                    GeneCombinationRecipe tmp = new GeneCombinationRecipe(reqs, builderCount, builderMinQuality);
                    if (tmp.totalIngredientCount() > GeneCombinationRecipe.MAX_TOTAL_INGREDIENTS) {
                        // clamp silently to obey max of 4
                        tmp = tmp.clampedToMax();
                    }
                    if (tmp.totalIngredientCount() > 0) {
                        combinationRecipe = tmp;
                    }
                }

                Gene gene = new Gene(id, category, rarity, qMin, qMax, combinable, description, mobList, combinationRecipe);
                GeneRegistry.register(gene);
                loaded++;
            } catch (Exception ex) {
                BanditsQuirkLib.LOGGER.error("Failed to parse gene json {}: {}", e.getKey(), ex.toString());
            }
        }
        BanditsQuirkLib.LOGGER.info("Loaded {} genes", loaded);
    }
}