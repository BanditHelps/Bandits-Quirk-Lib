package com.github.b4ndithelps.forge.abilities;


import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.PowerStockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.SyncType;
import net.minecraft.ChatFormatting;

import java.util.List;

import static com.github.b4ndithelps.forge.systems.PowerStockHelper.sendPlayerPercentageMessage;

public class DetroitSmashAbility extends Ability {
    // Configurable properties
    public static final PalladiumProperty<Integer> MAX_CHARGE_TICKS = new IntegerProperty("max_charge_ticks").configurable("Maximum charge time in ticks");
    public static final PalladiumProperty<Float> DAMAGE_THRESHOLD = new FloatProperty("damage_threshold").configurable("Percentage threshold where player starts taking damage");
    public static final PalladiumProperty<Float> MAX_DISTANCE = new FloatProperty("max_distance").configurable("Maximum ray distance");
    public static final PalladiumProperty<Float> BASE_DAMAGE = new FloatProperty("base_damage").configurable("Base damage for the attack");
    public static final PalladiumProperty<Float> BASE_KNOCKBACK = new FloatProperty("base_knockback").configurable("Base knockback for the attack");
    public static final PalladiumProperty<Float> BASE_RADIUS = new FloatProperty("base_radius").configurable("Base explosion radius");

    // Unique properties for tracking state
    public static final PalladiumProperty<Integer> CHARGE_TICKS;

    public DetroitSmashAbility() {
        super();
        this.withProperty(MAX_CHARGE_TICKS, 100)
            .withProperty(DAMAGE_THRESHOLD, 60.0f)
            .withProperty(MAX_DISTANCE, 30.0f)
            .withProperty(BASE_DAMAGE, 5.0f)
            .withProperty(BASE_KNOCKBACK, 1.0f)
            .withProperty(BASE_RADIUS, 2.0f);
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CHARGE_TICKS, 0);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {
            // Initialize charge
            entry.setUniqueProperty(CHARGE_TICKS, 0);
            BodyStatusHelper.setCustomFloat(player, "left_arm", "pstock_smash_charge", 0.0f);

            if (entity.level() instanceof ServerLevel serverLevel) {
                // Play charging start sound
                serverLevel.playSound(null, player.blockPosition(),
                        SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.3f, 1.2f);
            }
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        if (enabled) {
            handleChargingPhase(player, serverLevel, entry);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (entity instanceof ServerPlayer player && entity.level() instanceof ServerLevel serverLevel) {
            int chargeTicks = entry.getProperty(CHARGE_TICKS);
            
            // Prevent bug where it forces a smash on reload
            if (chargeTicks == 0) return;

            // Trigger arm swing animation
            player.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND);

            // Execute the Detroit Smash
            executeDetroitSmash(player, serverLevel, entry, chargeTicks);
            
            // Reset charge
            entry.setUniqueProperty(CHARGE_TICKS, 0);
            BodyStatusHelper.setCustomFloat(player, "left_arm", "pstock_smash_charge", 0.0f);
        }
    }

    private void handleChargingPhase(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        int maxChargeTicks = entry.getProperty(MAX_CHARGE_TICKS);
        int currentChargeTicks = entry.getProperty(CHARGE_TICKS);
        
        // Increment charge if not at max
        if (currentChargeTicks < maxChargeTicks) {
            currentChargeTicks++;
            entry.setUniqueProperty(CHARGE_TICKS, currentChargeTicks);
            BodyStatusHelper.setCustomFloat(player, "left_arm", "pstock_smash_charge", currentChargeTicks);
        }
        
        // Calculate charge percentage
        float chargePercent = Math.min((currentChargeTicks / (float)maxChargeTicks) * 100.0f, 100.0f);
        float storedPower = PowerStockHelper.getStoredPower(player);
        float powerUsed = storedPower * chargePercent / 100.0f;

        // Show charge message every 5 ticks
        if (currentChargeTicks % 5 == 0) {
            sendPlayerPercentageMessage(player, powerUsed, chargePercent, "Charging Smash");
        }

        // Charge-based particle effects along player's look direction
        if (currentChargeTicks % 5 == 0) {
            addChargingParticles(player, level, chargePercent);
        }
        
        // Sound effects at certain charge levels
        if (currentChargeTicks == 20) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.3f, 1.2f);
        } else if (currentChargeTicks == 50) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.5f, 1.5f);
        } else if (currentChargeTicks == 80) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7f, 1.8f);
        } else if (currentChargeTicks == maxChargeTicks) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 2.0f);
        }
    }

    private void addChargingParticles(ServerPlayer player, ServerLevel level, float chargePercent) {
        Vec3 lookDirection = player.getLookAngle();
        float storedPower = PowerStockHelper.getStoredPower(player);
         
        // Scale particle effects based on both charge and stored power (for 500k max)
        float powerFactor = storedPower > 0 ? Math.min(1.0f, (float)Math.sqrt(storedPower) / 707.0f) : 0.0f;
         
        // Much more aggressive particle scaling - very few particles at low power
        int baseParticleCount = (int) Math.floor(chargePercent / 15); // Base particles from charge
        int powerParticleCount = (int) Math.floor(powerFactor * powerFactor * 10); // Square scaling for power
        int particleCount = Math.max(0, baseParticleCount + powerParticleCount);
         
        // Add minimal "dud" particles for very low power levels
        if (particleCount == 0 && chargePercent > 30 && storedPower >= 0) {
            particleCount = 1; // At least one tiny particle to show something happened
        }
         
        // Preview distance also scales heavily with power
        double baseDistance = 1 + (chargePercent / 30);
        double previewDistance = Math.min(15, baseDistance * (0.1f + powerFactor * 0.9f));
         
        // Show charging preview along look direction
        for (double i = 1; i < previewDistance; i += 0.8) {
            double px = player.getX() + (i * lookDirection.x);
            double py = player.getY() + (i * lookDirection.y) + player.getEyeHeight();
            double pz = player.getZ() + (i * lookDirection.z);
             
            // Only show particles if we actually have particles to show
            if (particleCount > 0) {
                // Scale particle intensity based on power - much more conservative
                int smokeParticles = Math.max(0, particleCount/3);
                int critParticles = Math.max(0, particleCount/5);
                 
                if (chargePercent < 30) {
                    if (smokeParticles > 0) {
                        // For very low power, show tiny "dud" particles
                        if (powerFactor < 0.1f) {
                            level.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0.02, 0.02, 0.02, 0.001);
                        } else {
                            level.sendParticles(ParticleTypes.SMOKE, px, py, pz, smokeParticles, 0.05, 0.05, 0.05, 0.005);
                        }
                    }
                } else if (chargePercent < 60) {
                    if (smokeParticles > 0) {
                        level.sendParticles(ParticleTypes.SMOKE, px, py, pz, smokeParticles, 0.1, 0.05, 0.1, 0.01);
                    }
                    if (critParticles > 0 && powerFactor > 0.2f) {
                        level.sendParticles(ParticleTypes.CRIT, px, py, pz, critParticles, 0.05, 0.05, 0.05, 0.005);
                    }
                } else if (chargePercent < 80) {
                    if (smokeParticles > 0) {
                        level.sendParticles(ParticleTypes.SMOKE, px, py, pz, smokeParticles, 0.15, 0.1, 0.15, 0.02);
                    }
                    if (critParticles > 0 && powerFactor > 0.2f) {
                        level.sendParticles(ParticleTypes.CRIT, px, py, pz, critParticles, 0.1, 0.05, 0.1, 0.01);
                    }
                    // Only show explosion particles if we have significant power
                    if ((int)chargePercent % 20 == 0 && powerFactor > 0.4f) {
                        level.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 1, 0.05, 0.05, 0.05, 0.005);
                    }
                } else {
                    if (smokeParticles > 0) {
                        level.sendParticles(ParticleTypes.SMOKE, px, py, pz, smokeParticles, 0.2, 0.15, 0.2, 0.03);
                    }
                    if (critParticles > 0 && powerFactor > 0.2f) {
                        level.sendParticles(ParticleTypes.CRIT, px, py, pz, critParticles, 0.15, 0.1, 0.15, 0.02);
                    }
                    // Enhanced effects only for higher power levels
                    if ((int)chargePercent % 15 == 0 && powerFactor > 0.5f) {
                        level.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 1, 0.1, 0.05, 0.1, 0.01);
                        if (powerFactor > 0.7f) {
                            level.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0.05, 0.05, 0.05, 0.005);
                        }
                    }
                }
            }
        }
    }

    private void executeDetroitSmash(ServerPlayer player, ServerLevel level, AbilityInstance entry, int chargeTicks) {
        int maxChargeTicks = entry.getProperty(MAX_CHARGE_TICKS);
        float maxDistance = entry.getProperty(MAX_DISTANCE);
        float baseDamage = entry.getProperty(BASE_DAMAGE);
        float baseKnockback = entry.getProperty(BASE_KNOCKBACK);
        float baseRadius = entry.getProperty(BASE_RADIUS);
        
        float chargePercent = Math.min((chargeTicks / (float)maxChargeTicks) * 100.0f, 100.0f);
        float storedPower = PowerStockHelper.getStoredPower(player);
        float powerUsed = storedPower * chargePercent / 100.0f;
        
        // Power scaling factor - designed for 0 to 500,000 power range
        float powerScalingFactor = powerUsed > 0 ? Math.min(1.0f, (float)Math.sqrt(powerUsed) / 707.0f) : 0.0f; // sqrt(500000) ≈ 707
         
        // Calculate scaled attack values - heavily dependent on stored power (designed for 500k max)
        float scaledDamage = baseDamage * (0.05f + powerScalingFactor * 19.95f); // 5% base, up to 2000% at max power
        float scaledKnockback = baseKnockback * (0.1f + powerScalingFactor * 14.9f); // 10% base, up to 1500% at max power
        float scaledRadius = baseRadius * (0.2f + powerScalingFactor * 9.8f); // 20% base, up to 1000% at max power
        float scaledDistance = maxDistance * (0.15f + powerScalingFactor * 4.85f); // 15% base, up to 500% at max power

        // Charge percentage multipliers (these are multiplicative, not additive)
        float chargeMultiplier = 0.3f + (chargePercent / 100.0f) * 0.7f; // 30% to 100% based on charge
         
        // Apply charge multiplier to all scaled values (allow very small minimums for zero power)
        float finalDamage = Math.max(0.1f, scaledDamage * chargeMultiplier);
        float finalKnockback = Math.max(0.05f, scaledKnockback * chargeMultiplier);
        float finalRadius = Math.max(0.2f, scaledRadius * chargeMultiplier);
        float finalDistance = Math.max(1.0f, scaledDistance * chargeMultiplier);

        // Execute the ray smash - sounds and initial particles (scale with power)
        // Only play sounds if we have decent power, start very quiet
        if (powerScalingFactor > 0.1f) {
            float soundVolume = Math.max(0.1f, Math.min(3.0f, powerScalingFactor * 3.0f));
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, soundVolume, 0.6f);
             
            if (powerScalingFactor > 0.4f) {
                level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, soundVolume * 0.8f, 1.0f);
            }
            if (powerScalingFactor > 0.6f) {
                level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, soundVolume * 0.6f, 0.8f);
            }
        }

        // Ray-based targeting system
        Vec3 eyePosition = player.getEyePosition();
        Vec3 lookDirection = player.getLookAngle();
        Vec3 endPosition = eyePosition.add(lookDirection.scale(finalDistance));

        // Perform ray trace
        BlockHitResult hitResult = level.clip(new ClipContext(eyePosition, endPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        Vec3 impactPoint = hitResult.getType() == HitResult.Type.BLOCK ? hitResult.getLocation() : endPosition;
        double actualDistance = eyePosition.distanceTo(impactPoint);

        // Give the user temporary resistance to prevent self-death from explosions
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4, true, false));

        // Show particle trail along the ray path
        addParticleTrail(level, eyePosition, impactPoint, chargePercent);

        // Create TNT explosions for mid to high power levels
        boolean shouldCreateExplosions = storedPower >= 50000 && chargePercent >= 50 && powerScalingFactor > 0.3f;
        if (shouldCreateExplosions) {
            createExplosionChain(level, player, eyePosition, impactPoint, finalRadius, powerUsed, chargePercent);
        }

        // Enhanced visual effects at impact point
        addImpactEffects(level, impactPoint, finalRadius, chargePercent, powerScalingFactor);

        // Area of Effect Damage and Knockback
        applyAreaDamage(level, player, impactPoint, finalRadius, finalDamage, finalKnockback);

        // Apply overuse damage to arms
        PowerStockHelper.applyLimbDamage(player, powerUsed, "arm");

        // Show completion message
        player.sendSystemMessage(Component.literal(String.format("Detroit Smash executed! %.0f damage, %.0f radius, %.0f range", 
            finalDamage, finalRadius, actualDistance)).withStyle(ChatFormatting.GREEN));
    }

    private void addParticleTrail(ServerLevel level, Vec3 startPos, Vec3 endPos, float chargePercent) {
         Vec3 direction = endPos.subtract(startPos).normalize();
         double distance = startPos.distanceTo(endPos);
         
         // Get power scaling for trail effects - estimate from distance
         float powerFactor = Math.max(0.0f, Math.min(1.0f, (float)(distance - 1.0) / 50.0f)); // Estimate power from range
         
         // Much more conservative trail particles
         int trailParticleCount = Math.max(0, Math.round((chargePercent / 30.0f) * powerFactor * powerFactor));
         double stepSize = Math.max(0.5, 1.5 - powerFactor * 1.0); // Less dense particles for lower power
         
         // Add minimal "dud" trail particles for very low power but decent charge
         if (trailParticleCount == 0 && chargePercent > 60 && distance > 3) {
             trailParticleCount = 1; // At least show something traveled
         }
         
         for (double i = 1; i < distance; i += stepSize) {
             Vec3 particlePos = startPos.add(direction.scale(i));
             
             if (trailParticleCount > 0) {
                 // Scale particle spread and intensity with power
                 double spread = Math.max(0.02, 0.2 * powerFactor); // Even smaller spread for dud particles
                 double speed = Math.max(0.005, 0.1 * powerFactor); // Even slower speed for dud particles
                 
                 // For very low power (dud particles), use minimal effects
                 if (powerFactor < 0.1f) {
                     level.sendParticles(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 
                         1, 0.02, 0.02, 0.02, 0.005); // Tiny puff
                 } else {
                     level.sendParticles(ParticleTypes.CRIT, particlePos.x, particlePos.y, particlePos.z, 
                         trailParticleCount, spread, spread, spread, speed);
                     level.sendParticles(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 
                         Math.max(1, trailParticleCount), spread * 0.75, spread * 0.75, spread * 0.75, speed * 0.5);
                     
                     // Only add explosion particles if we have significant power
                     if (powerFactor > 0.5f && trailParticleCount > 1) {
                         level.sendParticles(ParticleTypes.EXPLOSION, particlePos.x, particlePos.y, particlePos.z, 
                             Math.max(0, trailParticleCount/3), spread * 0.5, spread * 0.5, spread * 0.5, speed * 0.3);
                     }
                 }
             }
         }
    }

    private void createExplosionChain(ServerLevel level, ServerPlayer player, Vec3 startPos, Vec3 endPos, 
                                    float finalRadius, float powerUsed, float chargePercent) {
        Vec3 direction = endPos.subtract(startPos).normalize();
        double distance = startPos.distanceTo(endPos);
        
                 float powerRatio = Math.min(1.0f, powerUsed / 200000.0f); // Scale for higher power levels
        float explosionPower = Math.min(4.0f, 1.0f + (finalRadius * 0.2f * powerRatio));
        float explosionSpacing = Math.max(4.0f, 12.0f - (chargePercent / 10.0f));
        int maxExplosions = Math.min(15, Math.round(powerRatio * 25));
        int numExplosions = Math.min(maxExplosions, (int)(distance / explosionSpacing));

        // Create explosions along the ray path
        for (int i = 0; i <= numExplosions; i++) {
            double explosionDistance = Math.min(i * explosionSpacing, distance);
            Vec3 explosionPos = startPos.add(direction.scale(explosionDistance));
            
            // Vary explosion power slightly along the path (stronger in middle)
            double distanceRatio = explosionDistance / distance;
            double powerMultiplier = 0.6 + (0.4 * Math.sin(distanceRatio * Math.PI));
            float currentExplosionPower = Math.max(0.5f, explosionPower * (float)powerMultiplier);
            
            // Create explosion that damages entities
            level.explode(player, explosionPos.x, explosionPos.y, explosionPos.z, 
                currentExplosionPower, Level.ExplosionInteraction.TNT);
        }

        player.sendSystemMessage(Component.literal(String.format("High-power Detroit Smash with %d explosions!", numExplosions))
                .withStyle(ChatFormatting.RED));
    }

    private void addImpactEffects(ServerLevel level, Vec3 impactPoint, float finalRadius, float chargePercent, float powerScalingFactor) {
         // Scale impact effects based on power and charge - much more conservative
         float effectIntensity = Math.max(0.0f, powerScalingFactor * powerScalingFactor * (chargePercent / 100.0f));
         
         // Only create major effects if we have very significant power
         if (powerScalingFactor > 0.6f) {
             level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
                 impactPoint.x, impactPoint.y, impactPoint.z, 1, 0, 0, 0, 0);
         }
         
         // Much more conservative particle counts
         int critCount = Math.max(0, Math.round((5 + chargePercent/2) * effectIntensity));
         int smokeCount = Math.max(0, Math.round((10 + chargePercent/2) * effectIntensity));
         int explosionCount = Math.max(0, Math.round((2 + chargePercent/8) * effectIntensity));
         
         // Add minimal "dud" impact particles for very low power levels
         if (critCount == 0 && smokeCount == 0 && chargePercent > 20) {
             smokeCount = 1; // At least a tiny puff of smoke for the impact
             if (chargePercent > 50) {
                 critCount = 1; // Maybe one crit particle if they charged a bit
             }
         }
         
         // Send particles - even minimal ones for dud effects
         if (critCount > 0) {
             // For dud effects, use very small radius and speed
             float effectiveRadius = powerScalingFactor < 0.1f ? 0.1f : finalRadius;
             level.sendParticles(ParticleTypes.CRIT, 
                 impactPoint.x, impactPoint.y, impactPoint.z, 
                 critCount, effectiveRadius, effectiveRadius, effectiveRadius, Math.max(0.005, 0.15 * powerScalingFactor));
         }
         if (smokeCount > 0) {
             // For dud effects, use very small radius and speed
             float effectiveRadius = powerScalingFactor < 0.1f ? 0.15f : finalRadius * 1.5f;
             level.sendParticles(ParticleTypes.SMOKE, 
                 impactPoint.x, impactPoint.y, impactPoint.z, 
                 smokeCount, effectiveRadius, effectiveRadius, effectiveRadius, Math.max(0.01, 0.2 * powerScalingFactor));
         }
         if (explosionCount > 0) {
             level.sendParticles(ParticleTypes.EXPLOSION, 
                 impactPoint.x, impactPoint.y, impactPoint.z, 
                 explosionCount, finalRadius * 0.8f, finalRadius * 0.8f, finalRadius * 0.8f, Math.max(0.005, 0.1 * powerScalingFactor));
         }

         // Scale sound effects with power - much quieter at low levels
         if (powerScalingFactor > 0.1f) {
             float impactSoundVolume = Math.max(0.1f, Math.min(4.0f, powerScalingFactor * 4.0f));
             level.playSound(null, BlockPos.containing(impactPoint), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, impactSoundVolume, 0.4f);
             
             if (powerScalingFactor > 0.5f) {
                 level.playSound(null, BlockPos.containing(impactPoint), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, impactSoundVolume * 0.7f, 0.8f);
             }
         }
     }

    private void applyAreaDamage(ServerLevel level, ServerPlayer caster, Vec3 impactPoint, 
                               float finalRadius, float finalDamage, float finalKnockback) {
        // Find entities in radius
        AABB searchArea = new AABB(
            impactPoint.x - finalRadius, impactPoint.y - finalRadius, impactPoint.z - finalRadius,
            impactPoint.x + finalRadius, impactPoint.y + finalRadius, impactPoint.z + finalRadius
        );

        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, searchArea);

        for (LivingEntity entity : nearbyEntities) {
            if (entity == caster) continue; // Don't damage the caster
            
            double distance = entity.position().distanceTo(impactPoint);
            if (distance <= finalRadius) {
                // Calculate damage and knockback based on distance (closer = more damage)
                double damageMultiplier = Math.max(0.3, 1.0 - (distance / finalRadius));
                double knockbackMultiplier = Math.max(0.2, 1.0 - (distance / finalRadius));
                
                float actualDamage = (float)(finalDamage * damageMultiplier);
                float actualKnockback = (float)(finalKnockback * knockbackMultiplier);

                // Apply damage using explosion damage source
                DamageSource explosionDamage = level.damageSources().explosion(caster, caster);
                entity.hurt(explosionDamage, actualDamage);
                
                // Apply knockback effect
                if (distance > 0.1) { // Avoid division by zero
                    Vec3 knockbackDirection = entity.position().subtract(impactPoint).normalize();
                    
                    // Add upward component for more realistic knockback
                    knockbackDirection = new Vec3(
                        knockbackDirection.x, 
                        Math.max(0.2, knockbackDirection.y + 0.3), 
                        knockbackDirection.z
                    );
                    
                    Vec3 knockbackVelocity = knockbackDirection.scale(actualKnockback * 0.25);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(knockbackVelocity));
                }
            }
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "A charging punch ability based on My Hero Academia's Detroit Smash. Scales damage, range, and effects based on stored power in the user's chest. Creates ray-cast explosions along the punch trajectory with proper damage and knockback for all entities including players.";
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
    }
}
