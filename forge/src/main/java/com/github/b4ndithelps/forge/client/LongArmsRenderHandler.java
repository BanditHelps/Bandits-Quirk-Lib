package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class LongArmsRenderHandler {
    private static boolean IN_CUSTOM_RENDER = false;

    private static final String LONG_ARMS_GENE_ID = "bandits_quirk_lib:gene.long_arms";
    private static final String LONG_ARMS_GENE_ID_ALT = "bandits_quirk_lib:long_arms"; // fallback if added without "gene." prefix

    private static final Map<Player, ArmScaleState> PREV_SCALES = new WeakHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();

        boolean active = hasLongArms(player) || player.isCrouching();
        if (!active) {
            return;
        }

        HumanoidModel<?> model = event.getRenderer().getModel();
        if (model == null) return;

        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        ModelPart rightSleeve = (model instanceof PlayerModel<?> pm) ? pm.rightSleeve : null;
        ModelPart leftSleeve = (model instanceof PlayerModel<?> pm2) ? pm2.leftSleeve : null;

        ArmScaleState prev = new ArmScaleState();
        prev.rightArmVisible = rightArm == null || rightArm.visible;
        prev.leftArmVisible = leftArm == null || leftArm.visible;
        prev.rightSleeveVisible = rightSleeve == null || rightSleeve.visible;
        prev.leftSleeveVisible = leftSleeve == null || leftSleeve.visible;
        PREV_SCALES.put(player, prev);

        if (rightArm != null) rightArm.visible = false;
        if (leftArm != null) leftArm.visible = false;
        if (rightSleeve != null) rightSleeve.visible = false;
        if (leftSleeve != null) leftSleeve.visible = false;
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        ArmScaleState prev = PREV_SCALES.remove(player);
        if (prev == null) return;

        HumanoidModel<?> model = event.getRenderer().getModel();
        if (model == null) return;
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        if (rightArm != null) rightArm.visible = prev.rightArmVisible;
        if (leftArm != null) leftArm.visible = prev.leftArmVisible;
        if (model instanceof PlayerModel<?> pm) {
            if (pm.rightSleeve != null) pm.rightSleeve.visible = prev.rightSleeveVisible;
            if (pm.leftSleeve != null) pm.leftSleeve.visible = prev.leftSleeveVisible;
        }
    }

    private static boolean hasLongArms(Player player) {
        int quality = getGeneQualityClient(player, LONG_ARMS_GENE_ID);
        if (quality < 0) quality = getGeneQualityClient(player, LONG_ARMS_GENE_ID_ALT);
        return quality >= 0;
    }

    private static void restoreArmScale(HumanoidModel<?> model, ArmScaleState prev) {
        ModelPart rightArm = model.rightArm;
        ModelPart leftArm = model.leftArm;
        if (rightArm != null) rightArm.visible = prev.rightArmVisible;
        if (leftArm != null) leftArm.visible = prev.leftArmVisible;
        if (model instanceof PlayerModel<?> pm) {
            if (pm.rightSleeve != null) pm.rightSleeve.visible = prev.rightSleeveVisible;
            if (pm.leftSleeve != null) pm.leftSleeve.visible = prev.leftSleeveVisible;
        }
    }

    private static int getGeneQualityClient(Player player, String geneId) {
        ListTag genome = GenomeHelper.getGenome(player);
        for (int i = 0; i < genome.size(); i++) {
            CompoundTag gene = genome.getCompound(i);
            if (geneId.equals(gene.getString("id"))) {
                return gene.contains("quality", 3) ? gene.getInt("quality") : 100;
            }
        }
        return -1;
    }

    private static class ArmScaleState {
        boolean rightArmVisible;
        boolean leftArmVisible;
        boolean rightSleeveVisible;
        boolean leftSleeveVisible;

        float rightArmX, rightArmY, rightArmZ;
        float leftArmX, leftArmY, leftArmZ;
        float rightSleeveX, rightSleeveY, rightSleeveZ;
        float leftSleeveX, leftSleeveY, leftSleeveZ;

        ArmScaleState() {}
    }
}


