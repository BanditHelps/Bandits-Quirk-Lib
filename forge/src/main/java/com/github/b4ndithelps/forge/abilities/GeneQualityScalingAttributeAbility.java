package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AttributeModifierAbility;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneQualityScalingAttributeAbility extends AttributeModifierAbility {

    public static final PalladiumProperty<String> GENE = new StringProperty("gene").configurable("The gene whose quality scales the attribute amount (0-100). Amount = base + base * (quality/100)");

    public GeneQualityScalingAttributeAbility() {
        super();
        this.withProperty(GENE, "bandits_quirk_lib:gene.example");
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled) {
            Attribute attribute = entry.getProperty(ATTRIBUTE);
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null || entity.level().isClientSide) {
                return;
            }

            double baseAmount = entry.getProperty(AMOUNT);

            double scaledAmount = baseAmount;
            boolean hasGene = false;
            if (entity instanceof ServerPlayer player) {
                int quality = getGeneQuality(player, entry.getProperty(GENE));
                if (quality >= 0) {
                    hasGene = true;
                    double factor = Math.max(0D, Math.min(1D, quality / 100D));
                    scaledAmount = baseAmount + (baseAmount * factor);
                }
            }

            UUID uuid = entry.getProperty(UUID);
            AttributeModifier modifier = instance.getModifier(uuid);
            int operationValue = entry.getProperty(OPERATION);

            if (!hasGene) {
                if (modifier != null) {
                    instance.removeModifier(uuid);
                }
                return;
            }

            if (modifier != null && (modifier.getAmount() != scaledAmount || modifier.getOperation().toValue() != operationValue)) {
                instance.removeModifier(uuid);
                modifier = null;
            }

            if (modifier == null) {
                modifier = new AttributeModifier(
                        uuid,
                        entry.getConfiguration().getDisplayName().getString(),
                        scaledAmount,
                        Operation.fromValue(operationValue)
                );
                instance.addTransientModifier(modifier);
            }
        } else {
            this.lastTick(entity, entry, holder, false);
        }
    }

    private int getGeneQuality(ServerPlayer player, String geneId) {
        Pattern idPattern = Pattern.compile("id:\"([^\"]+)\"");
        Pattern qualityPattern = Pattern.compile("quality:(\\d+)");
        ListTag genes = GenomeHelper.getGenome(player);

        for (int i = 0; i < genes.size(); i++) {
            Tag gene = genes.get(i);
            String asString = gene.getAsString();
            Matcher idMatcher = idPattern.matcher(asString);
            if (idMatcher.find() && geneId.equals(idMatcher.group(1))) {
                Matcher qualityMatcher = qualityPattern.matcher(asString);
                if (qualityMatcher.find()) {
                    try {
                        return Integer.parseInt(qualityMatcher.group(1));
                    } catch (NumberFormatException ignored) {
                        return -1;
                    }
                }
                return -1;
            }
        }
        return -1;
    }

    @Override
    public String getDocumentationDescription() {
        return "Adds an attribute modifier while enabled. The configured amount scales with the specified gene's quality: amount = base + base * (quality/100). Applies only if the player has the gene.";
    }
}




