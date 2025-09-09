package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

public final class LongLegsController {

    private static final ThreadLocal<Boolean> HIDE_VANILLA_LEGS = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final String LONG_LEGS_GENE_ID = "bandits_quirk_lib:gene.long_legs";
    private static final String LONG_LEGS_GENE_ID_ALT = "bandits_quirk_lib:long_legs";

    private LongLegsController() {}

    public static boolean shouldRenderLongLegs(Player player) {
        if (player == null) return false;
        if (player.isCrouching()) return true; // debug force
        return hasGene(player, LONG_LEGS_GENE_ID) || hasGene(player, LONG_LEGS_GENE_ID_ALT);
    }

    public static boolean hasGene(Player player, String geneId) {
        ListTag genome = GenomeHelper.getGenome(player);
        for (int i = 0; i < genome.size(); i++) {
            CompoundTag gene = genome.getCompound(i);
            if (geneId.equals(gene.getString("id"))) {
                return true;
            }
        }
        return false;
    }

    public static void setHideVanillaLegs(boolean hide) { HIDE_VANILLA_LEGS.set(hide); }
    public static boolean isHideVanillaLegs() { return HIDE_VANILLA_LEGS.get(); }
}


