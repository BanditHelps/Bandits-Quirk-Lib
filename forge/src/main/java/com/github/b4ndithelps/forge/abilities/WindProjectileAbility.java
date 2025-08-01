package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

public class WindProjectileAbility extends Ability {

    // Configurable properties
    public static final PalladiumProperty<Float> MAX_RANGE = new FloatProperty("max_range").configurable("Maximum range of the air projectile");
    public static final PalladiumProperty<Float> MAX_DAMAGE = new FloatProperty("max_damage").configurable("Maximum damage at full power");
    public static final PalladiumProperty<Float> MAX_KNOCKBACK = new FloatProperty("max_knockback").configurable("Maximum knockback force at full power");
    public static final PalladiumProperty<Float> DAMAGE_THRESHOLD = new FloatProperty("damage_threshold").configurable("Power stock threshold where damage starts (0-10000000)");
    public static final PalladiumProperty<Integer> PROJECTILE_SPEED = new IntegerProperty("projectile_speed").configurable("Speed of the projectile in blocks per tick");
    public static final PalladiumProperty<Float> PROJECTILE_WIDTH = new FloatProperty("projectile_width").configurable("Width of the projectile hitbox");

    public WindProjectileAbility() {
        super();
        this.withProperty(MAX_RANGE, 30.0F)
                .withProperty(MAX_DAMAGE, 12.0F)
                .withProperty(MAX_KNOCKBACK, 4.0F)
                .withProperty(DAMAGE_THRESHOLD, 1000000.0F) // Start doing damage at 1 million power stock
                .withProperty(PROJECTILE_SPEED, 2)
                .withProperty(PROJECTILE_WIDTH, 1.5F);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {
            executeAirForce(player, entry);
        }
    }

    private void executeAirForce(ServerPlayer player, AbilityInstance entry) {
        ServerLevel level = player.serverLevel();
        
        // Get power stock from chest
        float powerStock = BodyStatusHelper.getCustomFloat(player, "chest", "pstock_stored");
        
        // Calculate power scaling (0.0 to 1.0)
        float powerRatio = Math.min(powerStock / 10000000.0F, 1.0F);
        
        // Get configurable values
        float maxRange = entry.getProperty(MAX_RANGE);
        float maxDamage = entry.getProperty(MAX_DAMAGE);
        float maxKnockback = entry.getProperty(MAX_KNOCKBACK);
        float damageThreshold = entry.getProperty(DAMAGE_THRESHOLD);
        int projectileSpeed = entry.getProperty(PROJECTILE_SPEED);
        float projectileWidth = entry.getProperty(PROJECTILE_WIDTH);
        
        // Calculate actual values based on power
        float currentRange = maxRange * Math.max(0.2F, powerRatio); // Minimum 20% range
        float currentKnockback = maxKnockback * Math.max(0.3F, powerRatio); // Minimum 30% knockback
        float currentDamage = powerStock >= damageThreshold ? maxDamage * powerRatio : 0.0F;
        
        // Get player look direction
        Vec3 startPos = player.getEyePosition();
        Vec3 lookDirection = player.getLookAngle();
        
        // Play sound effect
        float pitch = 0.8F + (powerRatio * 0.4F); // Higher pitch for more power
        float volume = 0.5F + (powerRatio * 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, volume, pitch);
        
        // Create the projectile
        createWindProjectile(level, startPos, lookDirection, currentRange, currentDamage, currentKnockback, 
                           projectileSpeed, projectileWidth, powerRatio, player);
        
        BanditsQuirkLibForge.LOGGER.info("Wind projectile fired with power stock: {}, damage: {}, knockback: {}", 
                                       powerStock, currentDamage, currentKnockback);
    }

    private void createWindProjectile(ServerLevel level, Vec3 startPos, Vec3 direction, float range, 
                                    float damage, float knockback, int speed, float width, 
                                    float powerRatio, ServerPlayer shooter) {
        
        // Schedule projectile movement over multiple ticks
        final int totalTicks = (int) (range / speed);
        
        for (int tick = 0; tick < totalTicks; tick++) {
            final int currentTick = tick;
            
            level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + currentTick,
                () -> {
                    // Calculate current position
                    double distance = currentTick * speed;
                    Vec3 currentPos = startPos.add(direction.scale(distance));
                    
                    // Create particle ring effect
                    createParticleRing(level, currentPos, powerRatio);
                    
                    // Check for entity hits
                    checkEntityHits(level, currentPos, width, damage, knockback, direction, shooter);
                }
            ));
        }
    }

    private void createParticleRing(ServerLevel level, Vec3 position, float powerRatio) {
        // Ring of particles around the projectile
        int particleCount = (int) (8 + powerRatio * 12); // More particles at higher power
        double ringRadius = 0.5 + powerRatio * 0.5; // Larger ring at higher power
        
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;
            double offsetX = Math.cos(angle) * ringRadius;
            double offsetY = Math.sin(angle) * ringRadius;
            double offsetZ = Math.cos(angle + Math.PI/4) * ringRadius * 0.5; // Some Z variation
            
            // Main wind particles
            level.sendParticles(ParticleTypes.CLOUD,
                    position.x + offsetX, position.y + offsetY, position.z + offsetZ,
                    1, 0.1, 0.1, 0.1, 0.02);
            
            // Additional pressure wave particles at higher power
            if (powerRatio > 0.5F) {
                level.sendParticles(ParticleTypes.POOF,
                        position.x + offsetX * 1.5, position.y + offsetY * 1.5, position.z + offsetZ * 1.5,
                        1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        
        // Central impact particles for visual feedback
        if (powerRatio > 0.3F) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    position.x, position.y, position.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void checkEntityHits(ServerLevel level, Vec3 position, float width, float damage, 
                               float knockback, Vec3 direction, ServerPlayer shooter) {
        
        // Create hitbox around current position
        AABB hitBox = new AABB(position.subtract(width/2, width/2, width/2), 
                              position.add(width/2, width/2, width/2));
        
        // Find entities in the hitbox
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, hitBox,
                entity -> entity != shooter && entity.isAlive());
        
        for (LivingEntity entity : entities) {
            // Apply knockback
            Vec3 knockbackDirection = direction.normalize();
            entity.push(knockbackDirection.x * knockback, 
                       Math.max(knockbackDirection.y * knockback, knockback * 0.3), // Minimum upward knockback
                       knockbackDirection.z * knockback);
            
            // Apply damage if enabled
            if (damage > 0) {
                entity.hurt(level.damageSources().playerAttack(shooter), damage);
            }
            
            // Visual feedback for hit
            Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0);
            level.sendParticles(ParticleTypes.CRIT,
                    entityPos.x, entityPos.y, entityPos.z,
                    5, 0.3, 0.3, 0.3, 0.1);
            
            // Sound feedback for hit
            level.playSound(null, entity.blockPosition(), 
                          damage > 0 ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_KNOCKBACK, 
                          SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Creates a wind projectile (air force) that travels in the direction the player is looking. " +
               "Power is determined by the 'pstock_stored' value in the chest body part (0-10,000,000). " +
               "At low power levels, only provides knockback. At higher power levels (above damage threshold), " +
               "also deals damage. The projectile creates a ring of particles as it travels and is not affected by gravity.";
    }
}