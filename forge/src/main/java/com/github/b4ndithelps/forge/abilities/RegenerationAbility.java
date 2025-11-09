package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;

public class RegenerationAbility extends Ability {

    // Configurable properties
    public static final PalladiumProperty<Float> HEAL_AMOUNT = new FloatProperty("heal_amount").configurable("Amount of damage to heal per regeneration tick");
    public static final PalladiumProperty<Integer> TICK_INTERVAL = new IntegerProperty("tick_interval").configurable("Ticks between regeneration attempts (20 = every second)");

    // Unique properties for tracking state
    public static final PalladiumProperty<Integer> TICK_COUNTER;

    public RegenerationAbility() {
        super();
        this.withProperty(HEAL_AMOUNT, 0.5f)
                .withProperty(TICK_INTERVAL, 40); // Every 2 seconds by default
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(TICK_COUNTER, 0);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (entity instanceof ServerPlayer) {
            // Reset tick counter when ability starts
            entry.setUniqueProperty(TICK_COUNTER, 0);
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!enabled) return;

        // Check if the player has the body status capability
        if (!BodyStatusHelper.isBodyStatusAvailable(player)) return;

        // Check if there are any damaged parts to heal
        if (!BodyStatusHelper.hasDamagedParts(player)) return;

        // Handle tick interval
        int currentTicks = entry.getProperty(TICK_COUNTER);
        int tickInterval = entry.getProperty(TICK_INTERVAL);
        
        entry.setUniqueProperty(TICK_COUNTER, currentTicks + 1);

        // Apply regeneration at specified intervals
        if (currentTicks % tickInterval == 0) {
            performRegeneration(player, entry);
        }
    }

    private void performRegeneration(ServerPlayer player, AbilityInstance entry) {
        try {
            float healAmount = entry.getProperty(HEAL_AMOUNT);
            
            // Attempt to heal a random damaged part
            String healedPart = BodyStatusHelper.healRandomDamagedPart(player, healAmount);
            
            if (healedPart != null) {
                BanditsQuirkLibForge.LOGGER.debug("Regeneration healed {} by {} damage for player {}", 
                    healedPart, healAmount, player.getName().getString());
            } else {
                // No parts were healed (shouldn't happen due to hasDamagedParts check, but just in case)
                BanditsQuirkLibForge.LOGGER.debug("Regeneration found no damaged parts to heal for player {}", 
                    player.getName().getString());
            }
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Failed to perform regeneration for player {}: {}", 
                player.getName().getString(), e.getMessage());
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Regeneration ability that slowly heals damaged body parts over time. " +
               "Randomly selects damaged (but not destroyed) body parts and reduces their damage by the specified heal amount. " +
               "Supports configurable healing intervals." +
               "Uses the BodyStatusHelper system for safe body part modification and automatic client synchronization.";
    }

    static {
        TICK_COUNTER = new IntegerProperty("tick_counter");
    }
}
