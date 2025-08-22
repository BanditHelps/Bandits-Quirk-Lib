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
import net.minecraft.world.InteractionHand;
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
    public static final PalladiumProperty<Float> MAX_DISTANCE = new FloatProperty("max_distance").configurable("Maximum ray distance");
    public static final PalladiumProperty<Float> BASE_DAMAGE = new FloatProperty("base_damage").configurable("Base damage for the attack");
    public static final PalladiumProperty<Float> BASE_KNOCKBACK = new FloatProperty("base_knockback").configurable("Base knockback for the attack");
    public static final PalladiumProperty<Float> BASE_RADIUS = new FloatProperty("base_radius").configurable("Base explosion radius");
    public static final PalladiumProperty<Float> MAX_POWER = new FloatProperty("max_power").configurable("Power value considered 100% scaling (e.g., 500000)");
    public static final PalladiumProperty<Float> MAX_DAMAGE = new FloatProperty("max_damage").configurable("Maximum damage when power used is 100%");
    public static final PalladiumProperty<Float> MAX_KNOCKBACK = new FloatProperty("max_knockback").configurable("Maximum knockback when power used is 100%");
    public static final PalladiumProperty<Float> MAX_RADIUS = new FloatProperty("max_radius").configurable("Maximum AoE radius when power used is 100%");
    public static final PalladiumProperty<Float> EXPLOSION_POWER_MAX = new FloatProperty("explosion_power_max").configurable("Max explosion strength at full power");
    public static final PalladiumProperty<Float> EXPLOSION_SPACING_MIN = new FloatProperty("explosion_spacing_min").configurable("Minimum spacing between explosions at high power");
    public static final PalladiumProperty<Integer> EXPLOSION_COUNT_MAX = new IntegerProperty("explosion_count_max").configurable("Maximum number of explosions along the path");

    // Unique properties for tracking state
    public static final PalladiumProperty<Integer> CHARGE_TICKS;

    public DetroitSmashAbility() {
        super();
        this.withProperty(MAX_CHARGE_TICKS, 100)
            .withProperty(MAX_DISTANCE, 30.0f)
            .withProperty(BASE_DAMAGE, 5.0f)
            .withProperty(BASE_KNOCKBACK, 1.0f)
            .withProperty(BASE_RADIUS, 2.0f)
            // Linear scaling maxima for damage/knockback/radius and the power cap
            .withProperty(MAX_POWER, 500000.0f)
            .withProperty(MAX_DAMAGE, 100.0f)
            .withProperty(MAX_KNOCKBACK, 5.0f)
            .withProperty(MAX_RADIUS, 10.0f)
            // Explosion tuning properties
            .withProperty(EXPLOSION_POWER_MAX, 8.0f)
            .withProperty(EXPLOSION_SPACING_MIN, 2.5f)
            .withProperty(EXPLOSION_COUNT_MAX, 36);
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
            player.swing(InteractionHand.MAIN_HAND, true);

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
        } else if (currentChargeTicks == 99) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 2.0f);
        }
    }

    private void addChargingParticles(ServerPlayer player, ServerLevel level, float chargePercent) {
        Vec3 lookDirection = player.getLookAngle();
        float storedPower = PowerStockHelper.getStoredPower(player);
         
        // Scale particle effects based on both charge and stored power (for 500k max)
        float powerFactor = storedPower > 0 ? Math.min(1.0f, (float)Math.sqrt(storedPower) / 707.0f) : 0.0f;
         
        // Stronger particle scaling for a flashy feel
        int baseParticleCount = (int) Math.ceil(chargePercent / 6.0);
        int powerParticleCount = (int) Math.ceil(powerFactor * powerFactor * 40);
        int particleCount = Math.max(0, baseParticleCount + powerParticleCount);
         
        // Add minimal "dud" particles for very low power levels
        if (particleCount == 0 && chargePercent > 30 && storedPower >= 0) {
            particleCount = 1; // At least one tiny particle to show something happened
        }
         
        // Preview distance also scales with power
        double baseDistance = 1 + (chargePercent / 20.0);
        double previewDistance = Math.min(20, baseDistance * (0.2f + powerFactor * 1.2f));
         
        // Show charging preview along look direction
        for (double i = 1; i < previewDistance; i += 0.8) {
            double px = player.getX() + (i * lookDirection.x);
            double py = player.getY() + (i * lookDirection.y) + player.getEyeHeight();
            double pz = player.getZ() + (i * lookDirection.z);
             
            // Only show particles if we actually have particles to show
            if (particleCount > 0) {
                // Scale particle intensity based on power
                int smokeParticles = Math.max(0, particleCount/2);
                int critParticles = Math.max(0, particleCount/3);
                 
                if (chargePercent < 30) {
                    if (smokeParticles > 0) {
                        // For very low power, show tiny "dud" particles
                        if (powerFactor < 0.1f) {
                            level.sendParticles(ParticleTypes.SMOKE, px, py, pz, 1, 0.02, 0.02, 0.02, 0.001);
                        } else {
                            level.sendParticles(ParticleTypes.SMOKE, px, py, pz, smokeParticles, 0.08, 0.08, 0.08, 0.01);
                        }
                    }
                } else if (chargePercent < 60) {
                    if (smokeParticles > 0) {
                        level.sendParticles(ParticleTypes.SMOKE, px, py, pz, smokeParticles, 0.15, 0.08, 0.15, 0.02);
                    }
                    if (critParticles > 0 && powerFactor > 0.15f) {
                        level.sendParticles(ParticleTypes.CRIT, px, py, pz, critParticles, 0.08, 0.08, 0.08, 0.008);
                    }
                } else if (chargePercent < 80) {
                    if (smokeParticles > 0) {
                        level.sendParticles(ParticleTypes.SMOKE, px, py, pz, smokeParticles, 0.2, 0.15, 0.2, 0.03);
                    }
                    if (critParticles > 0 && powerFactor > 0.15f) {
                        level.sendParticles(ParticleTypes.CRIT, px, py, pz, critParticles, 0.12, 0.08, 0.12, 0.012);
                    }
                    // Only show explosion particles if we have significant power
                    if ((int)chargePercent % 10 == 0 && powerFactor > 0.35f) {
                        level.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 2, 0.08, 0.08, 0.08, 0.01);
                    }
                } else {
                    if (smokeParticles > 0) {
                        level.sendParticles(ParticleTypes.SMOKE, px, py, pz, smokeParticles, 0.25, 0.2, 0.25, 0.04);
                    }
                    if (critParticles > 0 && powerFactor > 0.15f) {
                        level.sendParticles(ParticleTypes.CRIT, px, py, pz, critParticles, 0.18, 0.12, 0.18, 0.02);
                    }
                    // Enhanced effects only for higher power levels
                    if ((int)chargePercent % 10 == 0 && powerFactor > 0.45f) {
                        level.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 2, 0.12, 0.08, 0.12, 0.02);
                        if (powerFactor > 0.7f) level.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 3, 0.08, 0.08, 0.08, 0.008);
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
        float maxPower = Math.max(1.0f, entry.getProperty(MAX_POWER));
        float maxDamage = Math.max(baseDamage, entry.getProperty(MAX_DAMAGE));
        float maxKnockback = Math.max(baseKnockback, entry.getProperty(MAX_KNOCKBACK));
        float maxRadius = Math.max(baseRadius, entry.getProperty(MAX_RADIUS));
        
        float chargePercent = Math.min((chargeTicks / (float)maxChargeTicks) * 100.0f, 100.0f);
        float storedPower = PowerStockHelper.getStoredPower(player);
        float powerUsed = storedPower * chargePercent / 100.0f;
        
        // Linear power ratio based on configured max power
        float powerRatio = Math.max(0.0f, Math.min(1.0f, powerUsed / maxPower));
        
        // Final values map linearly to configured maxima using the power ratio
        float finalDamage = Math.max(0.0f, maxDamage * powerRatio);
        float finalKnockback = Math.max(0.0f, maxKnockback * powerRatio);
        float finalRadius = Math.max(0.0f, maxRadius * powerRatio);
        float finalDistance = Math.max(1.0f, entry.getProperty(MAX_DISTANCE) * powerRatio);

        // Execute the ray smash - sounds and initial particles (scale with power)
        // Only play sounds if we have decent power, start very quiet
        if (powerRatio > 0.1f) {
            float soundVolume = Math.max(0.1f, Math.min(3.0f, powerRatio * 3.0f));
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, soundVolume, 0.6f);
             
            if (powerRatio > 0.4f) {
                level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, soundVolume * 0.8f, 1.0f);
            }
            if (powerRatio > 0.6f) {
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

        // Spawn immediate swing visuals near the player
        Vec3 handPos = eyePosition.add(lookDirection.scale(1.5));
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, handPos.x, handPos.y - 0.2, handPos.z, 6, 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(ParticleTypes.CRIT, handPos.x, handPos.y, handPos.z, 12, 0.25, 0.25, 0.25, 0.05);
        level.sendParticles(ParticleTypes.CLOUD, handPos.x, handPos.y, handPos.z, 8, 0.25, 0.25, 0.25, 0.05);

        // Direct entity hit check along the ray for a satisfying punch
        LivingEntity directTarget = findTargetEntity(player, (float)finalDistance);
        if (directTarget != null && directTarget != player && directTarget.isAlive()) {
            impactPoint = directTarget.position();
            // Perform vanilla attack for animations/damage hooks
            player.attack(directTarget);
            // Add heavy extra damage scaling with power
            float bonus = finalDamage * 0.75f;
            directTarget.hurt(player.damageSources().playerAttack(player), bonus);
            // Strong knockback on direct hit
            Vec3 kbDir = directTarget.position().subtract(player.position()).normalize().add(0, 0.2, 0);
            directTarget.setDeltaMovement(directTarget.getDeltaMovement().add(kbDir.scale(finalKnockback * 0.6)));
        }

        // Show particle trail along the ray path
        addParticleTrail(level, eyePosition, impactPoint, chargePercent);

        // Create TNT explosions for mid to high power levels
        boolean shouldCreateExplosions = storedPower >= 50000 && chargePercent >= 50 && powerRatio > 0.3f;
        if (shouldCreateExplosions) {
            float configuredMaxPower = Math.max(1.0f, entry.getProperty(MAX_POWER));
            float explosionPowerMax = Math.max(1.0f, entry.getProperty(EXPLOSION_POWER_MAX));
            float explosionSpacingMin = Math.max(0.5f, entry.getProperty(EXPLOSION_SPACING_MIN));
            int explosionCountMax = Math.max(1, entry.getProperty(EXPLOSION_COUNT_MAX));
            createExplosionChain(level, player, eyePosition, impactPoint, finalRadius, powerUsed, chargePercent,
                    configuredMaxPower, explosionPowerMax, explosionSpacingMin, explosionCountMax);
        }

        // Enhanced visual effects at impact point
        addImpactEffects(level, impactPoint, finalRadius, chargePercent, powerRatio);

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
         
         // Get power scaling for trail effects - denser at shorter ranges
         float powerFactor = Math.max(0.0f, Math.min(1.0f, (float)(distance - 1.0) / 35.0f));
         
         // Stronger trail particles
         int trailParticleCount = Math.max(0, Math.round((chargePercent / 15.0f) * (powerFactor * powerFactor * 2.0f + 0.5f)));
         double stepSize = Math.max(0.35, 1.2 - powerFactor * 1.0);
         
         // Add minimal "dud" trail particles for very low power but decent charge
         if (trailParticleCount == 0 && chargePercent > 60 && distance > 3) {
             trailParticleCount = 1; // At least show something traveled
         }
         
         for (double i = 1; i < distance; i += stepSize) {
             Vec3 particlePos = startPos.add(direction.scale(i));
             
             if (trailParticleCount > 0) {
                 // Scale particle spread and intensity with power
                 double spread = Math.max(0.04, 0.25 * powerFactor);
                 double speed = Math.max(0.01, 0.12 * powerFactor);
                 
                 // For very low power (dud particles), use minimal effects
                 if (powerFactor < 0.1f) {
                     level.sendParticles(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 
                         1, 0.03, 0.03, 0.03, 0.006); // Tiny puff
                 } else {
                     level.sendParticles(ParticleTypes.CRIT, particlePos.x, particlePos.y, particlePos.z, 
                         trailParticleCount, spread, spread, spread, speed);
                     level.sendParticles(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 
                         Math.max(1, trailParticleCount * 2), spread * 0.9, spread * 0.9, spread * 0.9, speed * 0.6);
                     
                     // Only add explosion particles if we have significant power
                     if (powerFactor > 0.45f && trailParticleCount > 1) {
                         level.sendParticles(ParticleTypes.EXPLOSION, particlePos.x, particlePos.y, particlePos.z, 
                             Math.max(0, trailParticleCount/2), spread * 0.6, spread * 0.6, spread * 0.6, speed * 0.35);
                     }
                 }
             }
         }
    }

    private void createExplosionChain(ServerLevel level, ServerPlayer player, Vec3 startPos, Vec3 endPos, 
                                    float finalRadius, float powerUsed, float chargePercent,
                                    float configuredMaxPower, float explosionPowerMax, float explosionSpacingMin, int explosionCountMax) {
        Vec3 direction = endPos.subtract(startPos).normalize();
        double distance = startPos.distanceTo(endPos);
        
        // Scale explosions using the same linear power ratio configuration
        float powerRatio = Math.max(0.0f, Math.min(1.0f, powerUsed / configuredMaxPower));
        float explosionPower = Math.min(explosionPowerMax, 1.5f + (finalRadius * 0.4f * powerRatio));
        float explosionSpacing = Math.max(explosionSpacingMin, 10.0f - (chargePercent * 0.08f) - (powerRatio * 4.0f));
        int maxExplosions = Math.min(explosionCountMax, Math.round(10 + powerRatio * (explosionCountMax - 10)));
        int numExplosions = Math.min(maxExplosions, (int)Math.ceil(distance / explosionSpacing));

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
                currentExplosionPower, Level.ExplosionInteraction.BLOCK);
        }

        player.sendSystemMessage(Component.literal(String.format("High-power Detroit Smash with %d explosions!", numExplosions))
                .withStyle(ChatFormatting.RED));
    }

    // Simple entity raycast to find the closest living entity along the player's look within range
    private LivingEntity findTargetEntity(ServerPlayer player, float range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        LivingEntity best = null;
        double closest = range;

        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(eyePos, endPos).inflate(1.0),
                e -> e != player && e.isAlive());

        for (LivingEntity e : entities) {
            if (e.getBoundingBox().inflate(0.3).clip(eyePos, endPos).isPresent()) {
                double dist = eyePos.distanceTo(e.position());
                if (dist < closest) {
                    closest = dist;
                    best = e;
                }
            }
        }

        return best;
    }

    private void addImpactEffects(ServerLevel level, Vec3 impactPoint, float finalRadius, float chargePercent, float powerScalingFactor) {
         // Scale impact effects based on power and charge
         float effectIntensity = Math.max(0.0f, powerScalingFactor * powerScalingFactor * (chargePercent / 90.0f));
         
         // Only create major effects if we have very significant power
         if (powerScalingFactor > 0.6f) {
             level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
                 impactPoint.x, impactPoint.y, impactPoint.z, 1, 0, 0, 0, 0);
         }
         
         // Stronger particle counts
         int critCount = Math.max(0, Math.round((10 + chargePercent) * effectIntensity));
         int smokeCount = Math.max(0, Math.round((18 + chargePercent) * effectIntensity));
         int explosionCount = Math.max(0, Math.round((4 + chargePercent/5) * effectIntensity));
         
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

         // Shockwave ring around impact for flair
         if (powerScalingFactor > 0.3f) {
             int rings = Math.max(1, (int)(powerScalingFactor * 3));
             for (int r = 1; r <= rings; r++) {
                 double ringRadius = finalRadius * (0.5 + 0.3 * r);
                 for (int a = 0; a < 24; a++) {
                     double theta = (Math.PI * 2 * a) / 24.0;
                     double rx = impactPoint.x + ringRadius * Math.cos(theta);
                     double rz = impactPoint.z + ringRadius * Math.sin(theta);
                     level.sendParticles(ParticleTypes.CLOUD, rx, impactPoint.y + 0.2, rz, 1, 0.02, 0.02, 0.02, 0.02);
                 }
             }
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
