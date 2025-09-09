package com.github.b4ndithelps.forge.compat;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
public final class PehkuiLongLegsScaler {

    private static final String LONG_LEGS_GENE_ID = "bandits_quirk_lib:gene.long_legs";
    private static final String LONG_LEGS_GENE_ID_ALT = "bandits_quirk_lib:long_legs";

    private static volatile boolean pehkuiChecked = false;
    private static volatile boolean pehkuiPresent = false;

    // Reflection cache
    private static Class<?> scaleTypesClass;
    private static Object scaleTypeHitboxHeight;
    private static Object scaleTypeEyeHeight;
    private static Method scaleType_getScaleData;
    private static Class<?> scaleDataClass;
    private static Method scaleData_setTargetScale;

    private PehkuiLongLegsScaler() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;

        if (!ensurePehkui()) return;

        float heightScale = computeDesiredHeightScale(player);
        if (Float.isNaN(heightScale) || heightScale <= 0.0F) return;

        applyPehkuiScales(player, heightScale);
    }

    private static boolean ensurePehkui() {
        if (pehkuiChecked) return pehkuiPresent;
        pehkuiChecked = true;
        try {
            if (!ModList.get().isLoaded("pehkui")) {
                pehkuiPresent = false;
                return false;
            }
            scaleTypesClass = Class.forName("virtuoel.pehkui.api.ScaleTypes");
            Class<?> scaleTypeClass = Class.forName("virtuoel.pehkui.api.ScaleType");
            scaleDataClass = Class.forName("virtuoel.pehkui.api.ScaleData");

            Field fHitboxHeight = scaleTypesClass.getField("HITBOX_HEIGHT");
            Field fEyeHeight = scaleTypesClass.getField("EYE_HEIGHT");
            scaleType_getScaleData = scaleTypeClass.getMethod("getScaleData", net.minecraft.world.entity.Entity.class);
            scaleData_setTargetScale = scaleDataClass.getMethod("setTargetScale", float.class);

            scaleTypeHitboxHeight = fHitboxHeight.get(null);
            scaleTypeEyeHeight = fEyeHeight.get(null);

            pehkuiPresent = scaleTypeHitboxHeight != null && scaleTypeEyeHeight != null;
        } catch (Throwable t) {
            pehkuiPresent = false;
        }
        return pehkuiPresent;
    }

    private static float computeDesiredHeightScale(Player player) {
        int quality = getGeneQuality(player, LONG_LEGS_GENE_ID);
        if (quality < 0) quality = getGeneQuality(player, LONG_LEGS_GENE_ID_ALT);
        if (quality < 0) return 1.0F; // no change

        float factor = Math.max(0F, Math.min(1F, quality / 100F));
        float eased = factor * factor; // quadratic ease-in
        float baseLegScale = 1.05F;
        float maxLegScale = 1.80F;
        float legYScale = baseLegScale + (maxLegScale - baseLegScale) * eased;

        // Total model height increases with leg extension; use a stronger contribution to match visuals.
        float legPortion = 0.52F; // tuned higher for larger hitbox at high leg scales
        float heightScale = 1.0F + (legYScale - 1.0F) * legPortion;
        return heightScale;
    }

    private static int getGeneQuality(Player player, String geneId) {
        ListTag genome = GenomeHelper.getGenome(player);
        for (int i = 0; i < genome.size(); i++) {
            CompoundTag gene = genome.getCompound(i);
            if (geneId.equals(gene.getString("id"))) {
                return gene.contains("quality", 3) ? gene.getInt("quality") : 100;
            }
        }
        return -1;
    }

    private static void applyPehkuiScales(Player player, float heightScale) {
        try {
            Object hitboxHeightData = scaleType_getScaleData.invoke(scaleTypeHitboxHeight, player);
            Object eyeHeightData = scaleType_getScaleData.invoke(scaleTypeEyeHeight, player);
            scaleData_setTargetScale.invoke(hitboxHeightData, heightScale);
            scaleData_setTargetScale.invoke(eyeHeightData, heightScale);
        } catch (Throwable ignored) {
        }
    }
}


