package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScalingGeneAmpEffect extends Ability {

    // Configurable properties
    public static final PalladiumProperty<String> EFFECT = new StringProperty("effect").configurable("The effect to give the player, that scales with gene quality");
    public static final PalladiumProperty<String> GENE = new StringProperty("gene").configurable("The gene that scales the effect based on the quality.");

    public ScalingGeneAmpEffect() {
        super();
        this.withProperty(EFFECT, "minecraft:water_breathing")
                .withProperty(GENE, "bandits_quirk_lib:gene.water_breathing");
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (entity instanceof ServerPlayer player) {
            Pattern idPattern = Pattern.compile("id:\"([^\"]+)\"");
            Pattern qualityPattern = Pattern.compile("quality:(\\d+)");
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
                int amplification = getAmplificationFromQuality(quality);
                String potionRegistryId = entry.getProperty(EFFECT);
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse(potionRegistryId));
                if (effect == null) {
                    BanditsQuirkLibForge.LOGGER.error("Invalid name for potion effect: " + potionRegistryId);
                } else {
                    player.addEffect(new MobEffectInstance(effect, 20, amplification, true, false));
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

    private int getAmplificationFromQuality(int quality) {
        if (quality >= 100) return 4;
        if (quality >= 85) return  3;
        if (quality >= 50) return  2;
        if (quality >= 25) return  1;
        return 0;

    }

    @Override
    public String getDocumentationDescription() {
        return "Regeneration ability that slowly heals damaged body parts over time. " +
                "Randomly selects damaged (but not destroyed) body parts and reduces their damage by the specified heal amount. " +
                "Supports configurable healing intervals." +
                "Uses the BodyStatusHelper system for safe body part modification and automatic client synchronization.";
    }
}