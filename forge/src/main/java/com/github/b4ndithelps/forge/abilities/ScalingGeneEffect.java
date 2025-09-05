package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScalingGeneEffect extends Ability {

    // Configurable properties
    public static final PalladiumProperty<String> EFFECT = new StringProperty("effect").configurable("The effect to give the player, that scales with gene quality");
    public static final PalladiumProperty<String> GENE = new StringProperty("gene").configurable("The gene that scales the effect based on the quality.");

    public ScalingGeneEffect() {
        super();
        this.withProperty(EFFECT, "minecraft:water_breathing")
                .withProperty(GENE, "bandits_quirk_lib:gene.water_breathing");
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        Pattern idPattern = Pattern.compile("id:\"([^\"]+)\"");
        Pattern qualityPattern = Pattern.compile("quality:(\\d+)");
        if (entity instanceof ServerPlayer player) {
            ListTag genes = GenomeHelper.getGenome(player);
            String geneId = entry.getProperty(GENE);

            boolean found = false;
            int quality = -1;

            // Looks to see if the player has the gene
            for (int i = 0; i < genes.size() && !found; i++) {
                Tag gene = genes.get(i);
                String temp = gene.getAsString();
                Matcher idMatcher = idPattern.matcher(temp);
                if (idMatcher.find()) {
                    String id = idMatcher.group(1);
                    if (id.equals(geneId)) {
                        found = true;
                        Matcher qualityMatcher = qualityPattern.matcher(temp);
                        if (qualityMatcher.find()) quality = Integer.parseInt(qualityMatcher.group(1));
                    }
                }
            }

            if (found) {
                int duration = getDurationFromQuality(quality);
                String potionRegistryId = entry.getProperty(EFFECT);
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(potionRegistryId));
                if (effect == null) {
                    BanditsQuirkLibForge.LOGGER.error("Invalid name for potion effect: " + potionRegistryId);
                } else {
                    player.addEffect(new MobEffectInstance(effect, duration * 20, 0, true, false));
                }


            }
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (entity instanceof ServerPlayer player) {
            String potionRegistryId = entry.getProperty(EFFECT);
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(potionRegistryId));
            if (effect == null) {
                BanditsQuirkLibForge.LOGGER.error("Invalid name for potion effect: " + potionRegistryId);
            } else {
                player.removeEffect(effect);
            }
        }
    }

    private int getDurationFromQuality(int quality) {
        if (quality >= 100) return 3600; // 1 hour
        if (quality >= 85) return  120; // 2 minutes
        if (quality >= 50) return  60; // 1 minute
        if (quality >= 25) return  15; // 15 seconds
        return 5; // 30 seconds

    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!enabled) return;
    }

    @Override
    public String getDocumentationDescription() {
        return "Regeneration ability that slowly heals damaged body parts over time. " +
                "Randomly selects damaged (but not destroyed) body parts and reduces their damage by the specified heal amount. " +
                "Supports configurable healing intervals." +
                "Uses the BodyStatusHelper system for safe body part modification and automatic client synchronization.";
    }
}