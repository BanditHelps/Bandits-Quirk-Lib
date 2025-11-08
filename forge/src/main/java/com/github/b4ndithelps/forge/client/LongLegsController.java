package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

public final class LongLegsController {

    private static final ThreadLocal<Boolean> HIDE_VANILLA_LEGS = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final String LONG_LEGS_GENE_ID = "bandits_quirk_lib:gene.long_legs";
    private static final String LONG_LEGS_GENE_ID_ALT = "bandits_quirk_lib:long_legs";

    private LongLegsController() {}

    public static boolean shouldRenderLongLegs(Player player) {
        if (player == null) return false;
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

    private static final Map<Player, LegPose> LEG_POSES = new WeakHashMap<>();

    public static void saveAndMoveLegsOffscreen(Player player, PlayerModel<?> model) {
        if (player == null || model == null) return;
        LegPose prev = new LegPose();
        if (model.rightLeg != null) { prev.rx = model.rightLeg.x; prev.ry = model.rightLeg.y; prev.rz = model.rightLeg.z; }
        if (model.leftLeg != null) { prev.lx = model.leftLeg.x; prev.ly = model.leftLeg.y; prev.lz = model.leftLeg.z; }
        if (model.rightPants != null) { prev.rpx = model.rightPants.x; prev.rpy = model.rightPants.y; prev.rpz = model.rightPants.z; }
        if (model.leftPants != null) { prev.lpx = model.leftPants.x; prev.lpy = model.leftPants.y; prev.lpz = model.leftPants.z; }
        LEG_POSES.put(player, prev);

        float off = 10000.0F;
        if (model.rightLeg != null) model.rightLeg.y += off;
        if (model.leftLeg != null) model.leftLeg.y += off;
        if (model.rightPants != null) model.rightPants.y += off;
        if (model.leftPants != null) model.leftPants.y += off;
    }

    public static void restoreLegPositions(Player player, PlayerModel<?> model) {
        LegPose prev = LEG_POSES.get(player);
        if (prev == null || model == null) return;
        if (model.rightLeg != null) { model.rightLeg.x = prev.rx; model.rightLeg.y = prev.ry; model.rightLeg.z = prev.rz; }
        if (model.leftLeg != null) { model.leftLeg.x = prev.lx; model.leftLeg.y = prev.ly; model.leftLeg.z = prev.lz; }
        if (model.rightPants != null) { model.rightPants.x = prev.rpx; model.rightPants.y = prev.rpy; model.rightPants.z = prev.rpz; }
        if (model.leftPants != null) { model.leftPants.x = prev.lpx; model.leftPants.y = prev.lpy; model.leftPants.z = prev.lpz; }
    }

    public static void clearLegPositions(Player player) { LEG_POSES.remove(player); }

    private static final class LegPose {
        float rx, ry, rz;
        float lx, ly, lz;
        float rpx, rpy, rpz;
        float lpx, lpy, lpz;
    }
}