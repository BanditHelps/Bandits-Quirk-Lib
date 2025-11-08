package com.github.b4ndithelps.genetics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Utility methods for deterministic, entity-tied genetics.
 */
public final class GeneticsHelper {
    private GeneticsHelper() {}

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
     * Generates a stable, pseudo-random gene display name like "gene_ab12" based on UUID, gene id, and index.
     */
    public static String generateStableGeneName(UUID uuid, ResourceLocation geneId, int index) {
        long seed = computeStableSeed(uuid) ^ geneId.toString().hashCode() ^ (long)(index + 1) * 0x9E3779B97F4A7C15L;
        int code = (int)(seed & 0xFFFF);
        String suffix = String.format("%04x", code);
        return "gene_" + suffix;
    }

    public static boolean isEntityHumanoid(Entity entity) {
        return entity instanceof Player
                || entity instanceof Villager
                || entity instanceof Zombie
                || entity instanceof Husk
                || entity instanceof Drowned;
    }
}


