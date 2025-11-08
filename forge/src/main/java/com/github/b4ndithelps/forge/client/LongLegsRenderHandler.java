package com.github.b4ndithelps.forge.client;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.systems.GenomeHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
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
public class LongLegsRenderHandler {
    private static final Map<Player, boolean[]> VISIBLE_CACHE = new WeakHashMap<>();

    private static final String LONG_LEGS_GENE_ID = "bandits_quirk_lib:gene.long_legs";
    private static final String LONG_LEGS_GENE_ID_ALT = "bandits_quirk_lib:long_legs";

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (!LongLegsController.shouldRenderLongLegs(player)) return;

        // Ensure vanilla legs are filtered/hidden for this frame
        LongLegsController.setHideVanillaLegs(true);

        int quality = getGeneQualityClient(player, LONG_LEGS_GENE_ID);
        if (quality < 0) quality = getGeneQualityClient(player, LONG_LEGS_GENE_ID_ALT);

        float factor = Math.max(0F, Math.min(1F, (quality < 0 ? 100 : quality) / 100F));
        float eased = factor * factor;
        float baseScale = 1.05F;
        float maxScale = 1.80F;
        float yScale = baseScale + (maxScale - baseScale) * eased;

        // Raise the whole model so extended legs don't sink below ground.
        float legLengthBlocks = 12.0F / 16.0F; // vanilla leg length below hip
        float offsetY = legLengthBlocks * (yScale - 1.0F);

        PoseStack poseStack = event.getPoseStack();
        poseStack.translate(0.0D, offsetY, 0.0D);

        // Additionally, hide and move vanilla leg parts offscreen for this render
        if (event.getRenderer().getModel() instanceof PlayerModel) {
            PlayerModel<?> pm = (PlayerModel<?>) event.getRenderer().getModel();
            storeAndHideLegs(player, pm);
            LongLegsController.saveAndMoveLegsOffscreen(player, pm);
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        if (event.getRenderer().getModel() instanceof PlayerModel) {
            PlayerModel<?> pm = (PlayerModel<?>) event.getRenderer().getModel();
            restoreLegs(player, pm);
            LongLegsController.restoreLegPositions(player, pm);
            LongLegsController.clearLegPositions(player);
        }
        LongLegsController.setHideVanillaLegs(false);
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
    private static void storeAndHideLegs(Player player, PlayerModel<?> pm) {
        boolean[] vis = new boolean[4];
        vis[0] = pm.rightLeg != null && pm.rightLeg.visible;
        vis[1] = pm.leftLeg != null && pm.leftLeg.visible;
        vis[2] = pm.rightPants != null && pm.rightPants.visible;
        vis[3] = pm.leftPants != null && pm.leftPants.visible;
        VISIBLE_CACHE.put(player, vis);

        if (pm.rightLeg != null) pm.rightLeg.visible = false;
        if (pm.leftLeg != null) pm.leftLeg.visible = false;
        if (pm.rightPants != null) pm.rightPants.visible = false;
        if (pm.leftPants != null) pm.leftPants.visible = false;
    }

    private static void restoreLegs(Player player, PlayerModel<?> pm) {
        boolean[] vis = VISIBLE_CACHE.remove(player);
        if (vis == null) return;
        if (pm.rightLeg != null) pm.rightLeg.visible = vis[0];
        if (pm.leftLeg != null) pm.leftLeg.visible = vis[1];
        if (pm.rightPants != null) pm.rightPants.visible = vis[2];
        if (pm.leftPants != null) pm.leftPants.visible = vis[3];
    }
}