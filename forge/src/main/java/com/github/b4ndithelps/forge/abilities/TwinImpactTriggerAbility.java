package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.events.TwinImpactEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * Triggers the stored Twin Impact mark. If a recent entity/block mark exists,
 * it applies a much stronger second hit at the marked location or on the marked entity.
 */
@SuppressWarnings("removal")
public class TwinImpactTriggerAbility extends Ability {

    public static final PalladiumProperty<Float> MULTIPLIER = new FloatProperty("multiplier").configurable("Damage/force multiplier for the second impact");
    public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Maximum distance from player to allow triggering the stored mark");

    public TwinImpactTriggerAbility() {
        super();
        this.withProperty(MULTIPLIER, 4.0F)
            .withProperty(MAX_DISTANCE, 64);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;

        float mult = Math.max(1.0F, entry.getProperty(MULTIPLIER));
        int maxDist = Math.max(1, entry.getProperty(MAX_DISTANCE));

        TwinImpactEvents.StoredMark mark = TwinImpactEvents.consumeMark(player, maxDist);
        if (mark == null) return;

        if (mark.entityId >= 0) {
            TwinImpactEvents.applySecondImpact(player, mark, mult);
        } else if (mark.blockPos != null) {
            TwinImpactEvents.applySecondImpact(player, mark, mult);
        } else if (mark.position != null) {
            // Fallback: apply AoE at position
            TwinImpactEvents.applySecondImpact(player, mark, mult);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Triggers the stored Twin Impact mark, applying a strong second impact at the marked spot or entity.";
    }
}


