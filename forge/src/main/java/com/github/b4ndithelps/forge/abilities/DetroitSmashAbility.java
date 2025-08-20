package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.capabilities.Body.BodyPart;
import com.github.b4ndithelps.forge.capabilities.Body.IBodyStatusCapability;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
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
import java.util.concurrent.ThreadLocalRandom;

public class DetroitSmashAbility extends Ability {

    // Safety threshold multipliers
    private static final float MINOR_DAMAGE_THRESHOLD = 1.2f; // 120% of safe threshold
    private static final float MAJOR_DAMAGE_THRESHOLD = 1.5f; // 150% of safe threshold  
    private static final float SEVERE_DAMAGE_THRESHOLD = 2.0f; // 200% of safe threshold

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

    /**
     * Get the current stored power from the body system
     * @param player The player entity
     * @return Current stored power
     */
    private float getStoredPower(ServerPlayer player) {
        IBodyStatusCapability bodyStatus = BodyStatusHelper.getBodyStatus(player);
        return bodyStatus != null ? bodyStatus.getCustomFloat(BodyPart.CHEST, "pstock_stored") : 0.0f;
    }

    /**
     * Get the current safe power threshold from the body system
     * @param player The player entity
     * @return The amount of power that can be used before it becomes unsafe
     */
    private float getSafePowerThreshold(ServerPlayer player) {
        IBodyStatusCapability bodyStatus = BodyStatusHelper.getBodyStatus(player);
        return bodyStatus != null ? bodyStatus.getCustomFloat(BodyPart.CHEST, "pstock_max_safe") : 0.0f;
    }

    /**
     * Safety check results container
     */
    private static class SafetyCheckResult {
        public final boolean isSafe;
        public final String damageLevel;
        public final String color;
        
        public SafetyCheckResult(boolean isSafe, String damageLevel, String color) {
            this.isSafe = isSafe;
            this.damageLevel = damageLevel;
            this.color = color;
        }
    }

    /**
     * Check if the player's current power usage is safe and determine damage level
     * @param player The player entity
     * @param powerUsed Amount of power used
     * @return Safety check results
     */
    private SafetyCheckResult getOverUseDamageLevel(ServerPlayer player, float powerUsed) {
        IBodyStatusCapability bodyStatus = BodyStatusHelper.getBodyStatus(player);
        float safeThreshold = bodyStatus != null ? bodyStatus.getCustomFloat(BodyPart.CHEST, "pstock_max_safe") : 0.0f;
        
        if (powerUsed <= safeThreshold) {
            return new SafetyCheckResult(true, "none", "green");
        }
        
        float overagePercentage = powerUsed / safeThreshold;
        
        // Determine damage level based on how much over threshold
        if (overagePercentage >= SEVERE_DAMAGE_THRESHOLD) {
            return new SafetyCheckResult(false, "severe", "dark_red");
        } else if (overagePercentage >= MAJOR_DAMAGE_THRESHOLD) {
            return new SafetyCheckResult(false, "major", "red");
        } else if (overagePercentage >= MINOR_DAMAGE_THRESHOLD) {
            return new SafetyCheckResult(false, "minor", "gold");
        }
        
        return new SafetyCheckResult(true, "none", "green");
    }

    /**
     * Send charging percentage message to player via actionbar
     * @param player The player entity
     * @param powerUsed Amount of power being used
     * @param chargePercent Charge percentage (0-100)
     */
    private void sendPlayerPercentageMessage(ServerPlayer player, float powerUsed, float chargePercent) {
        SafetyCheckResult safetyInfo = getOverUseDamageLevel(player, powerUsed);
        
        String statusText = safetyInfo.isSafe ? "Safe" : "Overuse";
        String message = String.format("Charging Detroit Smash: %.0f%% (%s)", chargePercent, statusText);
        
        // Send title command to display actionbar message
        Component actionBarComponent = Component.literal(message)
                .withStyle(ChatFormatting.valueOf(safetyInfo.color.toUpperCase()));

        ClientboundSetActionBarTextPacket packet = new ClientboundSetActionBarTextPacket(actionBarComponent);
        player.connection.send(packet);
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
        float storedPower = getStoredPower(player);
        float powerUsed = storedPower * chargePercent / 100.0f;

        // Show charge message every 5 ticks
        if (currentChargeTicks % 5 == 0) {
            sendPlayerPercentageMessage(player, powerUsed, chargePercent);
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
        int particleCount = (int) Math.max(1, Math.floor(chargePercent / 10));
        
        double previewDistance = Math.min(10, 2 + (chargePercent / 10));
        
        // Show charging preview along look direction
        for (double i = 1; i < previewDistance; i += 0.8) {
            double px = player.getX() + (i * lookDirection.x);
            double py = player.getY() + (i * lookDirection.y) + player.getEyeHeight();
            double pz = player.getZ() + (i * lookDirection.z);
            
            if (chargePercent < 30) {
                level.sendParticles(ParticleTypes.SMOKE, px, py, pz, Math.max(1, particleCount/2), 0.1, 0.1, 0.1, 0.01);
            } else if (chargePercent < 60) {
                level.sendParticles(ParticleTypes.SMOKE, px, py, pz, Math.max(1, particleCount/2), 0.15, 0.1, 0.15, 0.02);
                level.sendParticles(ParticleTypes.CRIT, px, py, pz, Math.max(1, particleCount/3), 0.1, 0.1, 0.1, 0.01);
            } else if (chargePercent < 80) {
                level.sendParticles(ParticleTypes.SMOKE, px, py, pz, Math.max(1, particleCount/2), 0.2, 0.15, 0.2, 0.03);
                level.sendParticles(ParticleTypes.CRIT, px, py, pz, Math.max(1, particleCount/3), 0.15, 0.1, 0.15, 0.02);
                if ((int)chargePercent % 20 == 0) {
                    level.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 1, 0.1, 0.1, 0.1, 0.01);
                }
            } else {
                level.sendParticles(ParticleTypes.SMOKE, px, py, pz, Math.max(1, particleCount/2), 0.25, 0.2, 0.25, 0.04);
                level.sendParticles(ParticleTypes.CRIT, px, py, pz, Math.max(1, particleCount/3), 0.2, 0.15, 0.2, 0.03);
                if ((int)chargePercent % 15 == 0) {
                    level.sendParticles(ParticleTypes.EXPLOSION, px, py, pz, 1, 0.15, 0.1, 0.15, 0.02);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0.1, 0.1, 0.1, 0.01);
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
        float storedPower = getStoredPower(player);
        float powerUsed = storedPower * chargePercent / 100.0f;
        
        // Calculate scaled attack values using the power system
        float scaledDamage = Math.min(400.0f, Math.max(baseDamage, (float)Math.sqrt(powerUsed) * 0.04f));
        float scaledKnockback = Math.min(12.0f, baseKnockback + (float)Math.sqrt(powerUsed) * 0.01f);
        float scaledRadius = Math.min(8.0f, baseRadius + (float)Math.sqrt(powerUsed) * 0.005f);
        float scaledDistance = Math.min(maxDistance + (float)Math.sqrt(powerUsed) * 0.01f, 45.0f);
        
        // Charge percentage bonuses
        float chargeDamageBonus = (chargePercent / 100.0f) * 8.0f;
        float chargeKnockbackBonus = (chargePercent / 100.0f) * 1.5f;
        float chargeRadiusBonus = (chargePercent / 100.0f) * 2.0f;
        float chargeDistanceBonus = (chargePercent / 100.0f) * 5.0f;
        
        float finalDamage = Math.max(baseDamage, scaledDamage + chargeDamageBonus);
        float finalKnockback = Math.max(baseKnockback, scaledKnockback + chargeKnockbackBonus);
        float finalRadius = Math.max(baseRadius, scaledRadius + chargeRadiusBonus);
        float finalDistance = Math.max(10, scaledDistance + chargeDistanceBonus);

        // Execute the ray smash - sounds and initial particles
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.5f, 0.6f);
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.5f, 0.8f);

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

        // Create TNT explosions for high power levels
        boolean shouldCreateExplosions = storedPower >= 50000 && chargePercent >= 50;
        if (shouldCreateExplosions) {
            createExplosionChain(level, player, eyePosition, impactPoint, finalRadius, powerUsed, chargePercent);
        }

        // Enhanced visual effects at impact point
        addImpactEffects(level, impactPoint, finalRadius, chargePercent);

        // Area of Effect Damage and Knockback
        applyAreaDamage(level, player, impactPoint, finalRadius, finalDamage, finalKnockback);

        // Apply overuse damage to arms
        applyOveruseDamage(player, powerUsed);

        // Show completion message
        player.sendSystemMessage(Component.literal(String.format("Detroit Smash executed! %.0f damage, %.0f radius, %.0f range", 
            finalDamage, finalRadius, actualDistance)).withStyle(ChatFormatting.GREEN));
    }

    private void addParticleTrail(ServerLevel level, Vec3 startPos, Vec3 endPos, float chargePercent) {
        Vec3 direction = endPos.subtract(startPos).normalize();
        double distance = startPos.distanceTo(endPos);
        
        int trailParticleCount = 3 + Math.round(chargePercent / 20.0f);
        
        for (double i = 1; i < distance; i += 0.3) {
            Vec3 particlePos = startPos.add(direction.scale(i));
            
            level.sendParticles(ParticleTypes.CRIT, particlePos.x, particlePos.y, particlePos.z, 
                trailParticleCount, 0.2, 0.2, 0.2, 0.1);
            level.sendParticles(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 
                trailParticleCount, 0.15, 0.15, 0.15, 0.05);
            level.sendParticles(ParticleTypes.EXPLOSION, particlePos.x, particlePos.y, particlePos.z, 
                Math.max(1, trailParticleCount/2), 0.1, 0.1, 0.1, 0.03);
        }
    }

    private void createExplosionChain(ServerLevel level, ServerPlayer player, Vec3 startPos, Vec3 endPos, 
                                    float finalRadius, float powerUsed, float chargePercent) {
        Vec3 direction = endPos.subtract(startPos).normalize();
        double distance = startPos.distanceTo(endPos);
        
        float powerRatio = Math.min(1.0f, powerUsed / 50000.0f);
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

    private void addImpactEffects(ServerLevel level, Vec3 impactPoint, float finalRadius, float chargePercent) {
        // Enhanced visual effects on top of the explosions
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, 
            impactPoint.x, impactPoint.y, impactPoint.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CRIT, 
            impactPoint.x, impactPoint.y, impactPoint.z, 
            50 + Math.round(chargePercent), finalRadius, finalRadius, finalRadius, 0.15);
        level.sendParticles(ParticleTypes.SMOKE, 
            impactPoint.x, impactPoint.y, impactPoint.z, 
            100 + Math.round(chargePercent), finalRadius * 1.5f, finalRadius * 1.5f, finalRadius * 1.5f, 0.2);
        level.sendParticles(ParticleTypes.EXPLOSION, 
            impactPoint.x, impactPoint.y, impactPoint.z, 
            20 + Math.round(chargePercent/2), finalRadius * 0.8f, finalRadius * 0.8f, finalRadius * 0.8f, 0.1);

        // Enhanced explosion sound at impact point
        level.playSound(null, BlockPos.containing(impactPoint), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.4f);
        level.playSound(null, BlockPos.containing(impactPoint), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 2.0f, 0.8f);
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

    private void applyOveruseDamage(ServerPlayer player, float powerUsed) {
        SafetyCheckResult safetyCheck = getOverUseDamageLevel(player, powerUsed);
        
        int armDamage = 0;
        
        if (!safetyCheck.isSafe) {
            switch (safetyCheck.damageLevel) {
                case "minor":
                    armDamage += 70;
                    break;
                case "major":
                    armDamage += 130;
                    break;
                case "severe":
                    armDamage += 200;
                    break;
            }
        }
        
        if (armDamage > 0) {
            BodyStatusHelper.addDamage(player, "arm", armDamage);
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
