package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.entities.ModEntities;
import com.github.b4ndithelps.forge.entities.WaveProjectileEntity;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
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

public class WindWallSmashAbility extends Ability {

    // Quirk Factor Scaling Constants
    private static final float QUIRK_SPEED_MULTIPLIER = 2.5f; // How much quirk factor affects speed
    private static final float QUIRK_RANGE_MULTIPLIER = 2.0f; // How much quirk factor affects range
    private static final float QUIRK_KNOCKBACK_MULTIPLIER = 3.0f; // How much quirk factor affects knockback strength
    private static final float MAX_EFFECTIVE_SPEED = 12.0f; // Maximum speed cap

    // Configurable properties
    public static final PalladiumProperty<Float> BASE_WAVE_WIDTH = new FloatProperty("base_wave_width").configurable("Base width of the wave projectile");
    public static final PalladiumProperty<Float> BASE_WAVE_HEIGHT = new FloatProperty("base_wave_height").configurable("Base height of the wave projectile");
    public static final PalladiumProperty<Float> BASE_WAVE_SPEED = new FloatProperty("base_wave_speed").configurable("Base speed of the wave projectile");
    public static final PalladiumProperty<Integer> MAX_CHARGE_TIME = new IntegerProperty("max_charge_time").configurable("Maximum charge time in ticks (20 ticks = 1 second)");

    // Unique values for charging
    public static final PalladiumProperty<Integer> CHARGE_TICKS;



    public WindWallSmashAbility() {
        this.withProperty(BASE_WAVE_WIDTH, 3.0F)
                .withProperty(BASE_WAVE_HEIGHT, 3.0F)
                .withProperty(BASE_WAVE_SPEED, 1.0F)
                .withProperty(MAX_CHARGE_TIME, 60); // 3 seconds max charge
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
            
            // If user released the key and we have charge, spawn the wave projectile
            if (chargeTicks > 0) {
                spawnWaveProjectile(player, (ServerLevel) entity.level(), entry, chargeTicks);
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

        // Play charging sounds periodically (but no particles)
        if (chargeTicks % 15 == 0) {
            float pitch = 1.0f + (chargeTicks / (float)maxChargeTime) * 0.5f;
            level.playSound(null, player.blockPosition(),
                    SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.05f, pitch);
        }
    }

    private void spawnWaveProjectile(ServerPlayer player, ServerLevel level, AbilityInstance entry, int chargeTicks) {
        double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
        int maxChargeTime = entry.getProperty(MAX_CHARGE_TIME);
        float baseWidth = entry.getProperty(BASE_WAVE_WIDTH);
        float baseHeight = entry.getProperty(BASE_WAVE_HEIGHT);
        float baseSpeed = entry.getProperty(BASE_WAVE_SPEED);

        // Calculate charge factor (0.0 to 1.0)
        float chargeFactor = Math.min(chargeTicks / (float)maxChargeTime, 1.0f);

        // Apply quirk factor and charge factor to wave properties
        float effectiveWidth = baseWidth * (1.0f + chargeFactor * 1.5f) * (1.0f + (float)quirkFactor * 0.5f);
        float effectiveHeight = baseHeight * (1.0f + chargeFactor * 1.5f) * (1.0f + (float)quirkFactor * 0.5f);
        float effectiveSpeed = Math.min(baseSpeed * (1.0f + chargeFactor * 2.0f) * (1.0f + (float)quirkFactor * QUIRK_SPEED_MULTIPLIER), MAX_EFFECTIVE_SPEED);

        // Get player's look direction
        Vec3 lookDirection = player.getLookAngle();
        float playerYaw = player.getYRot();
        float playerPitch = player.getXRot();

        // Create the wave projectile entity - use player's yaw directly for movement direction
        WaveProjectileEntity wave = new WaveProjectileEntity(ModEntities.WAVE_PROJECTILE.get(), level, player);

        // Position the wave slightly in front of the player
        Vec3 playerPos = player.position().add(0, player.getEyeHeight(), 0);
        Vec3 spawnPos = playerPos.add(lookDirection.scale(1.5)); // Spawn 1.5 blocks in front

        wave.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        wave.setWaveWidth(effectiveWidth);
        wave.setWaveHeight(effectiveHeight);
        wave.setWaveSpeed(effectiveSpeed);

        // Set the wave's movement direction and ensure proper rotation alignment
        wave.setMovementAndRotation(lookDirection.scale(effectiveSpeed));

        // If your wave entity needs to know its "wall orientation" (perpendicular to movement),
        // you can store that as a separate property in the entity
        // wave.setWallOrientation(playerYaw + 90.0F); // If you have this method

        // Spawn the entity into the world
        level.addFreshEntity(wave);

        // Play release sound and effects
        BlockPos centerPos = player.blockPosition();
        float volume = 0.5f + chargeFactor * 0.8f;
        float pitch = 0.8f + chargeFactor * 0.4f;
        level.playSound(null, centerPos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, volume, pitch);

        // Add burst particles at release point
        addReleaseEffects(player, level, chargeTicks);
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
        return "A charge-based wind ability that spawns a destructive wave projectile. Hold to charge for increased power, then release to launch a wave that automatically destroys blocks in its path. Charge time and quirk factor increase the wave's width, height, and speed.";
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
    }
}
