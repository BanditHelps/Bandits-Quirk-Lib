package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class WindWallSmashAbility extends Ability {

    // Quirk Factor Scaling Constants
    private static final float QUIRK_SPEED_MULTIPLIER = 2.5f; // How much quirk factor affects speed
    private static final float QUIRK_RANGE_MULTIPLIER = 2.0f; // How much quirk factor affects range
    private static final float QUIRK_KNOCKBACK_MULTIPLIER = 3.0f; // How much quirk factor affects knockback strength
    private static final float MAX_EFFECTIVE_SPEED = 12.0f; // Maximum speed cap

    // Configurable properties
    public static final PalladiumProperty<Float> BASE_KNOCKBACK = new FloatProperty("base_knockback").configurable("Base knockback strength for entities");
    public static final PalladiumProperty<Integer> MAX_RADIUS = new IntegerProperty("max_radius").configurable("Maximum radius the wind wall can reach");
    public static final PalladiumProperty<Float> SPEED = new FloatProperty("speed").configurable("Speed at which the wind wall expands");
    public static final PalladiumProperty<Integer> MAX_CHARGE_TIME = new IntegerProperty("max_charge_time").configurable("Maximum charge time in ticks (20 ticks = 1 second)");

    // Unique values for charging
    public static final PalladiumProperty<Integer> CHARGE_TICKS;

    // Blocks affected by wind at different charge levels
    private static final Set<Block> LIGHT_BLOCKS = Set.of(
            Blocks.DEAD_BUSH, Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
            Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
            Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY,
            Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY
    );

    private static final Set<Block> LEAF_BLOCKS = Set.of(
            Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
            Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
            Blocks.NETHER_WART_BLOCK, Blocks.WARPED_WART_BLOCK
    );

    private static final Set<Block> ICE_SNOW_BLOCKS = Set.of(
            Blocks.ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.FROSTED_ICE,
            Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.POWDER_SNOW
    );

    public WindWallSmashAbility() {
        this.withProperty(BASE_KNOCKBACK, 3.0F)
                .withProperty(MAX_RADIUS, 15)
                .withProperty(SPEED, 1.5F)
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
            
            // If user released the key and we have charge, start the wind wave
            if (chargeTicks > 0) {
                releaseWindWave(player, (ServerLevel) entity.level(), entry, chargeTicks);
                // Start a server task to handle the expansion over time
                startWindWaveExpansion(player, (ServerLevel) entity.level(), entry);
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

    private void releaseWindWave(ServerPlayer player, ServerLevel level, AbilityInstance entry, int chargeTicks) {
        BlockPos centerPos = player.blockPosition();
        
        // Calculate charge factor for sound effects
        int maxChargeTime = entry.getProperty(MAX_CHARGE_TIME);
        float chargeFactor = Math.min(chargeTicks / (float)maxChargeTime, 1.0f);

        // Play release sound with intensity based on charge
        float volume = 0.5f + chargeFactor * 0.8f;
        float pitch = 0.8f + chargeFactor * 0.4f;
        level.playSound(null, centerPos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, volume, pitch);

        // Add burst particles at release point
        addReleaseEffects(player, level, chargeTicks);
    }

    private void startWindWaveExpansion(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
        int chargeTicks = entry.getProperty(CHARGE_TICKS);
        int maxChargeTime = entry.getProperty(MAX_CHARGE_TIME);
        int maxRadius = entry.getProperty(MAX_RADIUS);
        float speed = entry.getProperty(SPEED);
        float baseKnockback = entry.getProperty(BASE_KNOCKBACK);

        // Calculate charge factor (0.0 to 1.0)
        float chargeFactor = Math.min(chargeTicks / (float)maxChargeTime, 1.0f);

        // Apply quirk factor to speed and range
        float speedMultiplier = 1.0f + ((float)quirkFactor * QUIRK_SPEED_MULTIPLIER);
        float effectiveSpeed = Math.min(speed * speedMultiplier, MAX_EFFECTIVE_SPEED);
        int effectiveMaxRadius = maxRadius + (int)(quirkFactor * QUIRK_RANGE_MULTIPLIER * maxRadius);
        int maxEffectiveRadius = Math.max(1, (int)(effectiveMaxRadius * chargeFactor));

        Vec3 playerPos = player.position().add(0, player.getEyeHeight(), 0); // Start from eye level
        Vec3 lookDirection = player.getLookAngle();
        
        // Use full 3D look direction for proper directional movement
        double lookX = lookDirection.x;
        double lookY = lookDirection.y;
        double lookZ = lookDirection.z;

        // Calculate how many ticks should pass before each radius expansion (slower progression)
        int ticksPerExpansion = Math.max(8, (int)(40 / effectiveSpeed));
        
        // Schedule the expansion over time
        for (int radius = 1; radius <= maxEffectiveRadius; radius++) {
            final int currentRadius = radius;
            final int previousRadius = radius - 1;
            int delay = radius * ticksPerExpansion;
            
            level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + delay,
                () -> {
                    try {
                        executeWindWaveRing(player, level, playerPos, currentRadius, previousRadius, chargeFactor, quirkFactor, baseKnockback, lookX, lookY, lookZ);
                    } catch (Exception e) {
                        BanditsQuirkLibForge.LOGGER.error("Error executing wind wave ring at radius " + currentRadius + ": ", e);
                    }
                }
            ));
        }
    }



    private void executeWindWaveRing(ServerPlayer player, ServerLevel level, Vec3 centerPos, int newRadius, int currentRadius, float chargeFactor, double quirkFactor, float baseKnockback, double lookX, double lookY, double lookZ) {
        
        int blocksProcessed = 0;
        int maxBlocksPerTick = 150; // Increased limit since we're not doing surface scanning
        Set<BlockPos> processedPositions = new HashSet<>();
        
        // Create a cone shape using the actual 3D look direction
        double coneHalfAngle = Math.toRadians(45); // 90 degree total cone (45 degrees each side)
        
        // Process blocks in a systematic cone pattern (simplified for better shape)
        // Create perpendicular vectors for the cone spread
        Vec3 forward = new Vec3(lookX, lookY, lookZ);
        Vec3 right = new Vec3(-lookZ, 0, lookX).normalize();
        Vec3 up = forward.cross(right).normalize();
        
        // Check blocks in a grid pattern within the expanding cone ring
        double ringWidth = Math.max(0.5, newRadius * 0.2); // Width of the ring to process
        double minDistance = Math.max(0, currentRadius - ringWidth * 0.5);
        double maxDistance = newRadius + ringWidth * 0.5;
        
        // Sample points more systematically
        int samplesPerRadius = Math.min(12, Math.max(6, newRadius * 2));
        
        for (double distance = minDistance; distance <= maxDistance && blocksProcessed < maxBlocksPerTick; distance += 0.5) {
            // Skip if too close to previous radius
            if (distance <= currentRadius * 0.8) continue;
            
            double coneRadiusAtDistance = Math.sin(coneHalfAngle) * distance;
            
            // Sample around the cone perimeter
            for (int sample = 0; sample < samplesPerRadius; sample++) {
                double angle = (sample * 2.0 * Math.PI) / samplesPerRadius;
                
                // Calculate position on cone surface
                double rightComp = Math.cos(angle) * coneRadiusAtDistance;
                double upComp = Math.sin(angle) * coneRadiusAtDistance;
                
                Vec3 position = centerPos.add(
                    forward.scale(distance).add(
                        right.scale(rightComp).add(up.scale(upComp))
                    )
                );
                
                BlockPos blockPos = new BlockPos(
                    (int)Math.floor(position.x), 
                    (int)Math.floor(position.y), 
                    (int)Math.floor(position.z)
                );
                
                if (!processedPositions.contains(blockPos)) {
                    processedPositions.add(blockPos);
                    if (processWindBlock(level, blockPos, chargeFactor)) {
                        blocksProcessed++;
                    }
                }
            }
            
            // Add some interior volume sampling (fewer samples)
            if (distance > 1.0) {
                int interiorSamples = Math.min(4, (int)(distance * 0.5));
                for (int i = 0; i < interiorSamples; i++) {
                    double interiorAngle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                    double interiorRadius = ThreadLocalRandom.current().nextDouble() * coneRadiusAtDistance * 0.7;
                    
                    double rightComp = Math.cos(interiorAngle) * interiorRadius;
                    double upComp = Math.sin(interiorAngle) * interiorRadius;
                    
                    Vec3 position = centerPos.add(
                        forward.scale(distance).add(
                            right.scale(rightComp).add(up.scale(upComp))
                        )
                    );
                    
                    BlockPos blockPos = new BlockPos(
                        (int)Math.floor(position.x), 
                        (int)Math.floor(position.y), 
                        (int)Math.floor(position.z)
                    );
                    
                    if (!processedPositions.contains(blockPos)) {
                        processedPositions.add(blockPos);
                        if (processWindBlock(level, blockPos, chargeFactor)) {
                            blocksProcessed++;
                        }
                    }
                }
            }
        }

        // Apply knockback effect to entities within the wind cone
        applyKnockbackToEntities(player, level, centerPos, newRadius, currentRadius, lookX, lookY, lookZ, quirkFactor, baseKnockback, chargeFactor);

        // Add visual effects
        addWindWaveVisualization(level, centerPos, newRadius, lookX, lookY, lookZ, chargeFactor);
        
        // Play wave sound effects periodically
        if (newRadius % 3 == 0) {
            BlockPos soundPos = new BlockPos((int)centerPos.x, (int)centerPos.y, (int)centerPos.z);
            level.playSound(null, soundPos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.15f, 1.2f + (newRadius * 0.05f));
        }
    }

    private void applyKnockbackToEntities(ServerPlayer caster, ServerLevel level, Vec3 centerPos, int newRadius, int currentRadius, double lookX, double lookY, double lookZ, double quirkFactor, float baseKnockback, float chargeFactor) {
        // Get entities in the expanding area
        AABB searchArea = new AABB(
            centerPos.x - newRadius, centerPos.y - 4, centerPos.z - newRadius,
            centerPos.x + newRadius, centerPos.y + 8, centerPos.z + newRadius
        );
        
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchArea);
        double coneHalfAngle = Math.toRadians(45); // 45 degrees each side
        Vec3 lookDirection = new Vec3(lookX, lookY, lookZ);
        
        for (LivingEntity entity : entities) {
            // Skip the caster
            if (entity == caster) continue;
            
            // Calculate position relative to caster
            Vec3 entityPos = entity.position();
            Vec3 toEntity = entityPos.subtract(centerPos);
            double distance = toEntity.length();
            
            // Only affect entities within the new ring (not already processed)
            if (distance <= currentRadius || distance > newRadius) continue;
            
            // Check if entity is within the 3D cone using proper cone geometry
            if (distance > 0.1) {
                Vec3 toEntityNormalized = toEntity.normalize();
                double dotProduct = lookDirection.dot(toEntityNormalized);
                double angleFromLookDirection = Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct)));
                
                // Skip entities outside the cone angle
                if (angleFromLookDirection > coneHalfAngle) continue;
                
                // Additional check: ensure entity is within the actual cone volume
                // Calculate the cross product to get the perpendicular distance
                Vec3 crossProduct = lookDirection.cross(toEntityNormalized);
                double perpendicularDistance = crossProduct.length() * distance;
                double maxPerpendicularAtDistance = Math.sin(coneHalfAngle) * distance;
                
                // Skip if too far from the cone center line
                if (perpendicularDistance > maxPerpendicularAtDistance) continue;
            }
            
            // Calculate knockback strength based on proximity, charge, and quirk factor
            double proximityFactor = 1.0 - (distance / newRadius);
            double knockbackStrength = baseKnockback * chargeFactor * proximityFactor;
            
            // Scale knockback with quirk factor
            knockbackStrength *= (1.0 + quirkFactor * QUIRK_KNOCKBACK_MULTIPLIER);
            
            // Calculate direction for knockback (away from caster in 3D direction)
            Vec3 pushDirection = toEntity.normalize().scale(knockbackStrength);
            
            // Apply the knockback
            entity.push(pushDirection.x, Math.min(pushDirection.y + knockbackStrength * 0.3, 1.5), pushDirection.z);
            entity.hurtMarked = true;
            
            // Add wind effect particles around the entity
            level.sendParticles(ParticleTypes.CLOUD, 
                entity.getX(), entity.getY() + 0.5, entity.getZ(), 
                4, 0.5, 0.3, 0.5, 0.2);
                
            // Extra particles for high charge/quirk factor
            if (quirkFactor > 0.5 || chargeFactor > 0.7) {
                level.sendParticles(ParticleTypes.POOF, 
                    entity.getX(), entity.getY() + 0.5, entity.getZ(), 
                    2, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    private void addWindWaveVisualization(ServerLevel level, Vec3 centerPos, int radius, double lookX, double lookY, double lookZ, float chargeFactor) {
        if (radius <= 0) return;
        
        double coneHalfAngle = Math.toRadians(45);
        
        // Create a proper 3D cone wall of particles (reduced density)
        int raysCount = Math.min(Math.max(6, radius), 16); // Fewer rays for cleaner visual
        
        // Create perpendicular vectors for the cone spread (3D)
        Vec3 forward = new Vec3(lookX, lookY, lookZ);
        Vec3 right = new Vec3(-lookZ, 0, lookX).normalize(); // Horizontal right vector
        Vec3 up = forward.cross(right).normalize(); // Up vector
        
        // Create particles in a circular pattern around the cone at this radius
        for (int ray = 0; ray < raysCount; ray++) {
            // Create rays in a circular pattern around the cone edge
            double circleAngle = (ray * 2 * Math.PI) / raysCount;
            double coneRadius = Math.sin(coneHalfAngle) * radius;
            
            // Calculate position on the circle at this distance
            double rightComponent = Math.cos(circleAngle) * coneRadius;
            double upComponent = Math.sin(circleAngle) * coneRadius;
            
            // Calculate ray direction in 3D
            Vec3 rayDirection = forward.scale(radius)
                .add(right.scale(rightComponent))
                .add(up.scale(upComponent))
                .normalize();
            
            // Create particles along this ray at the current radius
            double x = centerPos.x + rayDirection.x * radius;
            double y = centerPos.y + rayDirection.y * radius + ThreadLocalRandom.current().nextDouble() * 1.5;
            double z = centerPos.z + rayDirection.z * radius;
            
            // Main wall particles (reduced count)
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 
                rayDirection.x * 0.3, 0.2, rayDirection.z * 0.3, 0.15);
            
            // Additional detail particles for higher charge
            if (ThreadLocalRandom.current().nextDouble() < chargeFactor * 0.6) {
                level.sendParticles(ParticleTypes.POOF, x, y + 0.5, z, 1, 
                    0.1, 0.2, 0.1, 0.02);
            }
            
            // Add depth to the wall by creating particles slightly behind the main radius (reduced chance)
            if (radius > 3 && ThreadLocalRandom.current().nextDouble() < 0.2) {
                double depthOffset = 0.3 + ThreadLocalRandom.current().nextDouble() * 0.4;
                Vec3 backDirection = rayDirection.scale(radius - depthOffset);
                double backX = centerPos.x + backDirection.x;
                double backY = centerPos.y + backDirection.y + ThreadLocalRandom.current().nextDouble() * 1.8;
                double backZ = centerPos.z + backDirection.z;
                
                level.sendParticles(ParticleTypes.CLOUD, backX, backY, backZ, 1, 
                    rayDirection.x * 0.2, 0.15, rayDirection.z * 0.2, 0.1);
            }
        }
        
        // Add interior cross-section particles at different radii for 3D volume effect (reduced)
        if (radius > 2) {
            int crossSectionSamples = Math.min((int)(radius * 0.5), 6);
            for (int i = 0; i < crossSectionSamples; i++) {
                double interiorRadius = ThreadLocalRandom.current().nextDouble() * radius * 0.8;
                double circleAngle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
                double coneRadiusAtDistance = Math.sin(coneHalfAngle) * interiorRadius;
                
                double rightComponent = Math.cos(circleAngle) * coneRadiusAtDistance * ThreadLocalRandom.current().nextDouble();
                double upComponent = Math.sin(circleAngle) * coneRadiusAtDistance * ThreadLocalRandom.current().nextDouble();
                
                Vec3 rayDirection = forward.scale(interiorRadius)
                    .add(right.scale(rightComponent))
                    .add(up.scale(upComponent))
                    .normalize();
                
                double x = centerPos.x + rayDirection.x * interiorRadius;
                double y = centerPos.y + rayDirection.y * interiorRadius + ThreadLocalRandom.current().nextDouble() * 1.0;
                double z = centerPos.z + rayDirection.z * interiorRadius;
                
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 
                    rayDirection.x * 0.1, 0.05, rayDirection.z * 0.1, 0.05);
            }
        }
        
        // Add additional volume particles to fill the cone space (reduced density)
        int volumeParticles = Math.min((int)(radius * chargeFactor * 0.5), 8); // Fewer volume particles
        for (int i = 0; i < volumeParticles; i++) {
            // Random position within the cone
            double randomDistance = ThreadLocalRandom.current().nextDouble() * radius * 0.9;
            double randomCircleAngle = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            double randomConeRadius = ThreadLocalRandom.current().nextDouble() * Math.sin(coneHalfAngle) * randomDistance;
            
            double rightComponent = Math.cos(randomCircleAngle) * randomConeRadius;
            double upComponent = Math.sin(randomCircleAngle) * randomConeRadius;
            
            Vec3 rayDirection = forward.scale(randomDistance)
                .add(right.scale(rightComponent))
                .add(up.scale(upComponent))
                .normalize();
            
            double x = centerPos.x + rayDirection.x * randomDistance;
            double y = centerPos.y + rayDirection.y * randomDistance + ThreadLocalRandom.current().nextDouble() * 1.0;
            double z = centerPos.z + rayDirection.z * randomDistance;
            
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 
                rayDirection.x * 0.1, 0.05, rayDirection.z * 0.1, 0.05);
        }
    }

    private boolean processWindBlock(ServerLevel level, BlockPos blockPos, float chargeFactor) {
        BlockState blockState = level.getBlockState(blockPos);
        Block block = blockState.getBlock();
        
        // Skip air blocks
        if (block == Blocks.AIR) return false;
        
        // Light wind effects - always active
        if (LIGHT_BLOCKS.contains(block)) {
            level.destroyBlock(blockPos, false);
            
            // Add wind particles
            level.sendParticles(ParticleTypes.POOF, 
                blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 
                3, 0.3, 0.3, 0.3, 0.1);
            return true;
        }
        // Medium wind effects - require some charge
        else if (chargeFactor >= 0.3f && LEAF_BLOCKS.contains(block)) {
            // Scatter leaves - destroy chance based on charge
            if (ThreadLocalRandom.current().nextFloat() < chargeFactor * 0.8f) {
                scheduleBlockDestroy(level, blockPos, ThreadLocalRandom.current().nextInt(10) + 1);
            }
            
            // Add scattering particles
            level.sendParticles(ParticleTypes.POOF, 
                blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 
                2, 0.2, 0.2, 0.2, 0.05);
            return true;
        }
        // Strong wind effects - require high charge
        else if (chargeFactor >= 0.6f && ICE_SNOW_BLOCKS.contains(block)) {
            // Shatter ice and blow away snow
            scheduleBlockDestroy(level, blockPos, ThreadLocalRandom.current().nextInt(5) + 1);
            
            // Add shattering particles
            level.sendParticles(ParticleTypes.CLOUD, 
                blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 
                4, 0.3, 0.3, 0.3, 0.1);
            return true;
        }
        
        return false;
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



    private void scheduleBlockDestroy(ServerLevel level, BlockPos pos, int delayTicks) {
        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + delayTicks,
                () -> {
                    level.destroyBlock(pos, false);
                    level.sendParticles(ParticleTypes.POOF,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            3, 0.3, 0.3, 0.3, 0.1);
                }
        ));
    }

    @Override
    public String getDocumentationDescription() {
        return "A charge-based wind ability that creates a cone-shaped air blast. Hold to charge for increased power, then release to send out a devastating wind wave. Blows back entities and destroys light blocks, leaves (medium charge), and ice/snow (high charge). Quirk factor increases range (up to 3x), and knockback strength (up to 4x).";
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
    }
}
