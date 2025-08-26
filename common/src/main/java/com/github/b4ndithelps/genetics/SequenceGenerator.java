package com.github.b4ndithelps.genetics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class SequenceGenerator {
    private SequenceGenerator() {}

    public static final class GeneInstance {
        public final ResourceLocation id;
        public final int quality;

        public GeneInstance(ResourceLocation id, int quality) {
            this.id = id;
            this.quality = quality;
        }
    }

    public static List<GeneInstance> generateForEntity(LivingEntity entity, int minCount, int maxCount) {
        long seed = GeneticsHelper.computeStableSeed(entity.getUUID());
        Random rng = new Random(seed ^ 0xD1B54A32D192ED03L);

        int countRange = Math.max(0, maxCount - minCount);
        int count = minCount + (countRange > 0 ? rng.nextInt(countRange + 1) : 0);
        if (count <= 0) return List.of();

        List<Gene> available = new ArrayList<>(GeneRegistry.getAllOfType(entity.getType().arch$registryName().toString()));

        for (Gene g : available) {
            System.out.println(g.getId());
        }


        available.sort(Comparator.comparing(g -> g.getId().toString()));

        List<GeneInstance> out = new ArrayList<>();
        for (int i = 0; i < count && !available.isEmpty(); i++) {
            Gene chosen = chooseWeighted(rng, available);
            int qMin = Math.max(1, chosen.getQualityMin());
            int qMax = Math.max(qMin, chosen.getQualityMax());
            int quality = qMin + rng.nextInt(qMax - qMin + 1);
            out.add(new GeneInstance(chosen.getId(), quality));
            available.remove(chosen);
        }

        return out;
    }

    private static Gene chooseWeighted(Random rng, List<Gene> pool) {
        int total = 0;
        for (Gene g : pool) total += weightForRarity(g.getRarity());
        int roll = rng.nextInt(Math.max(1, total));
        int acc = 0;
        for (Gene g : pool) {
            acc += weightForRarity(g.getRarity());
            if (roll < acc) return g;
        }
        return pool.get(rng.nextInt(pool.size()));
    }

    private static int weightForRarity(Gene.Rarity rarity) {
        return switch (rarity) {
            case common -> 100;
            case uncommon -> 40;
            case rare -> 10;
            case very_rare -> 2;
        };
    }
}


