package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;

public final class LongArmsController {

    private static final ThreadLocal<Boolean> HIDE_VANILLA_ARMS = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final String LONG_ARMS_GENE_ID = "bandits_quirk_lib:gene.long_arms";
    private static final String LONG_ARMS_GENE_ID_ALT = "bandits_quirk_lib:long_arms";
    private static final java.util.Map<Player, ArmPose> ARM_POSES = new java.util.WeakHashMap<>();

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

    public static void saveAndMoveArmsOffscreen(Player player, PlayerModel<?> model) {
        if (player == null || model == null) return;
        ArmPose prev = new ArmPose();
        if (model.rightArm != null) {
            prev.rx = model.rightArm.x;
            prev.ry = model.rightArm.y;
            prev.rz = model.rightArm.z;
        }
        if (model.leftArm != null) {
            prev.lx = model.leftArm.x;
            prev.ly = model.leftArm.y;
            prev.lz = model.leftArm.z;
        }
        if (model.rightSleeve != null) {
            prev.rsx = model.rightSleeve.x;
            prev.rsy = model.rightSleeve.y;
            prev.rsz = model.rightSleeve.z;
        }
        if (model.leftSleeve != null) {
            prev.lsx = model.leftSleeve.x;
            prev.lsy = model.leftSleeve.y;
            prev.lsz = model.leftSleeve.z;
        }
        ARM_POSES.put(player, prev);

        // Move far out of view so any vanilla/layers that force visibility won't be seen
        float off = 10000.0F;
        if (model.rightArm != null) model.rightArm.y += off;
        if (model.leftArm != null) model.leftArm.y += off;
        if (model.rightSleeve != null) model.rightSleeve.y += off;
        if (model.leftSleeve != null) model.leftSleeve.y += off;
    }

    public static void restoreArmPositions(Player player, PlayerModel<?> model) {
        ArmPose prev = ARM_POSES.get(player);
        if (prev == null || model == null) return;
        if (model.rightArm != null) { model.rightArm.x = prev.rx; model.rightArm.y = prev.ry; model.rightArm.z = prev.rz; }
        if (model.leftArm != null) { model.leftArm.x = prev.lx; model.leftArm.y = prev.ly; model.leftArm.z = prev.lz; }
        if (model.rightSleeve != null) { model.rightSleeve.x = prev.rsx; model.rightSleeve.y = prev.rsy; model.rightSleeve.z = prev.rsz; }
        if (model.leftSleeve != null) { model.leftSleeve.x = prev.lsx; model.leftSleeve.y = prev.lsy; model.leftSleeve.z = prev.lsz; }
    }

    public static void clearArmPositions(Player player) { ARM_POSES.remove(player); }

    private static final class ArmPose {
        float rx, ry, rz;
        float lx, ly, lz;
        float rsx, rsy, rsz;
        float lsx, lsy, lsz;
    }
}


