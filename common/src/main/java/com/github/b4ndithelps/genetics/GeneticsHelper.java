package com.github.b4ndithelps.genetics;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Utility methods for deterministic, entity-tied genetics.
 */
public final class GeneticsHelper {
    private GeneticsHelper() {}

    public static final String TAG_GENOME_SEED = "bqlGenomeSeed";

    private static final String[] TRAIT_POOL = new String[]{
            "StrongBones", "HeatResistance", "ColdAdaptation", "RapidHealing", "ToxicTolerance",
            "NightVision", "DenseMuscle", "ElasticTissue", "ClottingFactor", "EfficientLungs"
    };

    /**
     * Returns a stable genome seed derived from the entity UUID.
     * Avoids loader-specific persistent data in common code.
     */
    public static long getOrAssignGenomeSeed(LivingEntity entity) {
        return computeStableSeed(entity.getUUID());
    }

    /**
     * Deterministically compute a seed from UUID.
     */
    public static long computeStableSeed(UUID uuid) {
        return uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
    }

    /**
     * Deterministically generate a set of traits from a seed. Count is small for readability.
     */
    public static List<String> generateTraitsFromSeed(long seed) {
        Random rng = new Random(seed ^ 0x9E3779B97F4A7C15L);
        int traitCount = 2 + rng.nextInt(2); // 2-3 traits
        List<String> traits = new ArrayList<>();
        boolean[] used = new boolean[TRAIT_POOL.length];
        for (int i = 0; i < traitCount; i++) {
            int idx;
            do {
                idx = rng.nextInt(TRAIT_POOL.length);
            } while (used[idx]);
            used[idx] = true;
            traits.add(TRAIT_POOL[idx]);
        }
        return traits;
    }

    /**
     * Populates the provided tag with sample metadata.
     */
    public static void writeSampleNbt(CompoundTag tag, LivingEntity sourceEntity, long genomeSeed, List<String> traits, int quality, long timestamp) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(sourceEntity.getType());
        tag.putString("EntityType", typeId != null ? typeId.toString() : "");
        tag.putString("EntityName", sourceEntity.getName().getString());
        tag.putString("EntityUUID", sourceEntity.getUUID().toString());
        tag.putLong("GenomeSeed", genomeSeed);

        ListTag traitsList = new ListTag();
        for (String t : traits) {
            traitsList.add(StringTag.valueOf(t));
        }
        tag.put("Traits", traitsList);
        tag.putInt("Quality", quality);
        tag.putLong("Timestamp", timestamp);
    }

    public static boolean isEntityHumanoid(Entity entity) {
        return entity instanceof net.minecraft.world.entity.player.Player
                || entity instanceof net.minecraft.world.entity.npc.Villager
                || entity instanceof net.minecraft.world.entity.monster.Zombie
                || entity instanceof net.minecraft.world.entity.monster.Husk
                || entity instanceof net.minecraft.world.entity.monster.Drowned;
    }
}


