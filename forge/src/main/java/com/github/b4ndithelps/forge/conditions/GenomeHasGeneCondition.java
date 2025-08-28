package com.github.b4ndithelps.forge.conditions;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionEnvironment;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

public class GenomeHasGeneCondition extends Condition {

    private final String geneId;
    private final int minQuality;

    public GenomeHasGeneCondition(String geneId, int minQuality) {
        this.geneId = geneId;
        this.minQuality = minQuality;
    }

    @Override
    public boolean active(DataContext dataContext) {
        LivingEntity entity = dataContext.getLivingEntity();
        if (!(entity instanceof Player player)) {
            return false;
        }

        ListTag genome = GenomeHelper.getGenome(player);
        for (int i = 0; i < genome.size(); i++) {
            CompoundTag gene = genome.getCompound(i);
            if (geneId.equals(gene.getString("id")) && gene.getInt("quality") >= minQuality) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ConditionSerializer getSerializer() {
        return CustomConditionSerializers.GENOME_HAS_GENE.get();
    }

    public static class Serializer extends ConditionSerializer {

        public static final PalladiumProperty<String> GENE_ID = (new StringProperty("gene_id")).configurable("The gene id to check for");
        public static final PalladiumProperty<Integer> MIN_QUALITY = (new IntegerProperty("min_quality")).configurable("Minimum required quality (inclusive)");

        public Serializer() {
            this.withProperty(GENE_ID, "minecraft:example_gene");
            this.withProperty(MIN_QUALITY, 0);
        }

        public ConditionEnvironment getContextEnvironment() {
            return ConditionEnvironment.ALL;
        }

        @Override
        public Condition make(JsonObject jsonObject) {
            return new GenomeHasGeneCondition(this.getProperty(jsonObject, GENE_ID), this.getProperty(jsonObject, MIN_QUALITY));
        }

        public String getDocumentationDescription() {
            return "Checks if the player has a specific gene with at least the minimum quality.";
        }
    }
}


