package com.github.b4ndithelps.forge.abilities.powerstock;

import com.github.b4ndithelps.forge.systems.PowerStockHelper;
import com.github.b4ndithelps.forge.systems.ProjectileSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

import java.util.concurrent.ThreadLocalRandom;

import static com.github.b4ndithelps.forge.systems.PowerStockHelper.sendPlayerPercentageMessage;

public class WindWallSmashAbility extends Ability {

    // Power Stock Scaling Constants
    private static final float BASE_LIFETIME = 20; // Base lifetime in ticks (2 seconds)
    private static final float MAX_LIFETIME = 150; // Max lifetime in ticks (7 seconds)

    // Configurable properties
    public static final PalladiumProperty<Float> BASE_WALL_WIDTH = new FloatProperty("base_wall_width").configurable("Base width of the wall projectile");
    public static final PalladiumProperty<Float> BASE_WALL_HEIGHT = new FloatProperty("base_wall_height").configurable("Base height of the wall projectile");
    public static final PalladiumProperty<Float> BASE_KNOCKBACK = new FloatProperty("base_knockback").configurable("Base knockback the projectile does to entities");
    public static final PalladiumProperty<Integer> MAX_CHARGE_TIME = new IntegerProperty("max_charge_time").configurable("Maximum charge time in ticks (20 ticks = 1 second)");

    // Unique values for charging
    public static final PalladiumProperty<Integer> CHARGE_TICKS;

    public WindWallSmashAbility() {
        this.withProperty(BASE_WALL_WIDTH, 3.0F)
                .withProperty(BASE_WALL_HEIGHT, 3.0F)
                .withProperty(MAX_CHARGE_TIME, 60)
                .withProperty(BASE_KNOCKBACK, 1.0F); // 3 seconds max charge
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CHARGE_TICKS, 0);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {
            // Start charging
            entry.setUniqueProperty(CHARGE_TICKS, 0);

            if (entity.level() instanceof ServerLevel serverLevel) {
                // Play charging start sound
                serverLevel.playSound(null, player.blockPosition(),
                        SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.2f, 1.8f);
            }
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        if (enabled) {
            // Charge up while enabled
            handleChargingPhase(player, serverLevel, entry);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (entity instanceof ServerPlayer player) {
            int chargeTicks = entry.getProperty(CHARGE_TICKS);
            
            // If user released the key, and we have charge, spawn the wall projectile
            if (chargeTicks > 0) {
                spawnWallProjectile(player, (ServerLevel) entity.level(), entry, chargeTicks);
            }
            
            // Reset charge when ability ends
            entry.setUniqueProperty(CHARGE_TICKS, 0);
        }
    }

    private void handleChargingPhase(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        int chargeTicks = entry.getProperty(CHARGE_TICKS);
        int maxChargeTime = entry.getProperty(MAX_CHARGE_TIME);

        // Increment charge time
        chargeTicks++;
        entry.setUniqueProperty(CHARGE_TICKS, Math.min(chargeTicks, maxChargeTime));

        // Calculate charge percentage
        float chargePercent = Math.min(chargeTicks / (float)maxChargeTime, 1.0f) * 100.0f;
        
        // Get current power stock
        float powerStock = PowerStockHelper.getStoredPower(player);
        
        // Calculate power that will be used at current charge level
        float powerToUse = (chargePercent / 100.0f) * powerStock;
        
        // Show charge message every 5 ticks (quarter second for smoother updates)
        if (chargeTicks % 5 == 0) {
            sendPlayerPercentageMessage(player, powerToUse, chargePercent, "Charging Smash");
        }

        // Play charging sounds periodically
        if (chargeTicks % 15 == 0) {
            float pitch = 1.0f + (chargeTicks / (float)maxChargeTime) * 0.5f;
            level.playSound(null, player.blockPosition(),
                    SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.05f, pitch);
        }
    }

    private void spawnWallProjectile(ServerPlayer player, ServerLevel level, AbilityInstance entry, int chargeTicks) {
        // Get power stock from chest
        float powerStock = PowerStockHelper.getStoredPower(player);
        
        int maxChargeTime = entry.getProperty(MAX_CHARGE_TIME);
        float baseWidth = entry.getProperty(BASE_WALL_WIDTH);
        float baseHeight = entry.getProperty(BASE_WALL_HEIGHT);
        float baseKnockback = entry.getProperty(BASE_KNOCKBACK);

        // Calculate charge factor (0.0 to 1.0)
        float chargeFactor = Math.min(chargeTicks / (float)maxChargeTime, 1.0f);
        
        // Calculate the actual amount of power being used for this attack
        float powerBeingUsed = chargeFactor * powerStock;
        
        // Scale based on the power being used (scale down since power values can be large)
        float powerScaling = Math.min(powerBeingUsed / 10000.0f, 50.0f); // Cap at 5x scaling for 50k+ power usage

        // Apply charge factor and power usage to wall properties
        float effectiveWidth = baseWidth * (1.0f + powerScaling * 0.2f);
        float effectiveHeight = baseHeight * (1.0f + powerScaling * 0.2f);
        int effectiveLifetime = (int)(BASE_LIFETIME * (1.0f + powerScaling * 0.3f));
        float effectiveKnockback = baseKnockback * (1.0f + powerScaling * 0.2f);
        effectiveLifetime = Math.min(effectiveLifetime, (int)MAX_LIFETIME);

        // Spawn the wall projectile using the new system
        ProjectileSpawner.spawnWallProjectile(level, player, effectiveWidth, effectiveHeight, effectiveLifetime, effectiveKnockback);

        // Play release sound and effects
        BlockPos centerPos = player.blockPosition();
        float volume = 0.5f + chargeFactor * 0.8f;
        float pitch = 0.8f + chargeFactor * 0.4f;
        level.playSound(null, centerPos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, volume, pitch);

        // Add burst particles at release point
        addReleaseEffects(player, level, chargeTicks);

        PowerStockHelper.applyLimbDamage(player, powerBeingUsed, "leg");
    }

    private void addReleaseEffects(ServerPlayer player, ServerLevel level, int chargeTicks) {
        float chargeStrength = Math.min(chargeTicks / 60.0f, 1.0f);
        Vec3 lookDirection = player.getLookAngle();

        // Burst of particles in front of player
        int particleCount = Math.max(5, (int)(chargeStrength * 15));

        for (int i = 0; i < particleCount; i++) {
            double spread = 0.3 * chargeStrength;
            double x = player.getX() + lookDirection.x + (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
            double y = player.getY() + 1 + (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
            double z = player.getZ() + lookDirection.z + (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;

            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1,
                    lookDirection.x * 0.5, 0.1, lookDirection.z * 0.5, 0.2);
        }
    }
    
    @Override
    public String getDocumentationDescription() {
        return "A charge-based wind ability that spawns a destructive wall projectile. Hold to charge from 0-100%, then release to launch a wall that destroys blocks in its path. The charge percentage determines what percentage of your stored power (pstock_stored from chest) is consumed for the attack. Higher charge levels and more power consumption result in larger, longer-lasting walls.";
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
    }
}