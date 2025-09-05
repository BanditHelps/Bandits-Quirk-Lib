package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.effects.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class ResistanceSystem {
    private ResistanceSystem() {}

    public static void applyGenomeBasedEffects(ServerPlayer player) {
        ListTag genome = GenomeHelper.getGenome(player);

        int heatAmplifier = 0;
        int coldAmplifier = 0;
        int poisonAmplifier = 0;
        int hungerAmplifier = 0;
        boolean wantsWaterBreathing = false;
        boolean wantsNightVision = false;

        for (int i = 0; i < genome.size(); i++) {
            CompoundTag gene = genome.getCompound(i);
            String id = gene.getString("id");
            int quality = Math.max(0, Math.min(100, gene.getInt("quality")));

            switch (id) {
                case "bandits_quirk_lib:gene.heat_resistance" ->
                        heatAmplifier = Math.max(heatAmplifier, mapQualityToAmplifier(quality));
                case "bandits_quirk_lib:gene.cold_resistance" ->
                        coldAmplifier = Math.max(coldAmplifier, mapQualityToAmplifier(quality));
                case "bandits_quirk_lib:gene.poison_resistance" ->
                        poisonAmplifier = Math.max(poisonAmplifier, mapQualityToAmplifier(quality));
                case "bandits_quirk_lib:gene.hunger_resistance" ->
                        hungerAmplifier = Math.max(hungerAmplifier, mapQualityToAmplifier(quality));
            }
        }

        // Duration 40 ticks (2s), re-applied each tick to keep active without flicker
        int duration = 40;
        if (heatAmplifier > 0) player.addEffect(new MobEffectInstance(ModEffects.HEAT_RESISTANCE.get(), duration, heatAmplifier - 1, true, false, true));
        if (coldAmplifier > 0) player.addEffect(new MobEffectInstance(ModEffects.COLD_RESISTANCE.get(), duration, coldAmplifier - 1, true, false, true));
        if (poisonAmplifier > 0) player.addEffect(new MobEffectInstance(ModEffects.POISON_RESISTANCE.get(), duration, poisonAmplifier - 1, true, false, true));
        if (hungerAmplifier > 0) player.addEffect(new MobEffectInstance(ModEffects.HUNGER_RESISTANCE.get(), duration, hungerAmplifier - 1, true, false, true));
    }

    private static int mapQualityToAmplifier(int quality) {
        if (quality >= 85) return 4;
        if (quality >= 65) return 3;
        if (quality >= 45) return 2;
        if (quality >= 25) return 1;
        return 0;
    }
}


