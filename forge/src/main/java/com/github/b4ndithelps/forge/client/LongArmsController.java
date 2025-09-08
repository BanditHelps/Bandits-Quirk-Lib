package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

public final class LongArmsController {

    private static final ThreadLocal<Boolean> HIDE_VANILLA_ARMS = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final String LONG_ARMS_GENE_ID = "bandits_quirk_lib:gene.long_arms";
    private static final String LONG_ARMS_GENE_ID_ALT = "bandits_quirk_lib:long_arms";

    private LongArmsController() {}

    public static boolean shouldRenderLongArms(Player player) {
        if (player == null) return false;
        // Allow crouch to force-enable for debugging
        if (player.isCrouching()) return true;
        return hasGene(player, LONG_ARMS_GENE_ID) || hasGene(player, LONG_ARMS_GENE_ID_ALT);
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

    public static void setHideVanillaArms(boolean hide) { HIDE_VANILLA_ARMS.set(hide); }
    public static boolean isHideVanillaArms() { return HIDE_VANILLA_ARMS.get(); }
}


