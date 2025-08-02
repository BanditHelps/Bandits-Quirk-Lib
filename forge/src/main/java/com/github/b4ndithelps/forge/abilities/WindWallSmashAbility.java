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

        BlockPos centerPos = player.blockPosition();
        Vec3 lookDirection = player.getLookAngle();
        
        // Normalize look direction to horizontal only
        double lookX = lookDirection.x;
        double lookZ = lookDirection.z;
        double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (lookLength > 0) {
            lookX /= lookLength;
            lookZ /= lookLength;
        }

        // Calculate how many ticks should pass before each radius expansion
        int ticksPerExpansion = Math.max(3, (int)(20 / effectiveSpeed));
        
        // Schedule the expansion over time
        for (int radius = 1; radius <= maxEffectiveRadius; radius++) {
            final int currentRadius = radius;
            final int previousRadius = radius - 1;
            int delay = radius * ticksPerExpansion;
            
            level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + delay,
                () -> {
                    try {
                        executeWindWaveRing(player, level, currentRadius, previousRadius, chargeFactor, quirkFactor, baseKnockback);
                    } catch (Exception e) {
                        BanditsQuirkLibForge.LOGGER.error("Error executing wind wave ring at radius " + currentRadius + ": ", e);
                    }
                }
            ));
        }
    }



    private void executeWindWaveRing(ServerPlayer player, ServerLevel level, int newRadius, int currentRadius, float chargeFactor, double quirkFactor, float baseKnockback) {
        BlockPos centerPos = player.blockPosition();
        Vec3 lookDirection = player.getLookAngle();
        
        // Normalize look direction to horizontal only
        double lookX = lookDirection.x;
        double lookZ = lookDirection.z;
        double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (lookLength > 0) {
            lookX /= lookLength;
            lookZ /= lookLength;
        }

        int blocksProcessed = 0;
        int maxBlocksPerTick = 100; // Limit to prevent lag
        Set<BlockPos> processedPositions = new HashSet<>();
        
        // Create a cone shape by checking all blocks in the new ring area
        double coneHalfAngle = Math.toRadians(45); // 90 degree total cone (45 degrees each side)
        
        // Process blocks in expanding area from current radius to new radius
        int startRadius = Math.max(0, currentRadius);
        
        // Check all blocks in the new ring area
        for (int x = centerPos.getX() - newRadius; x <= centerPos.getX() + newRadius && blocksProcessed < maxBlocksPerTick; x++) {
            for (int z = centerPos.getZ() - newRadius; z <= centerPos.getZ() + newRadius && blocksProcessed < maxBlocksPerTick; z++) {
                // Calculate distance from center
                double deltaX = x - centerPos.getX();
                double deltaZ = z - centerPos.getZ();
                double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                
                // Skip blocks that are too close (already processed) or too far
                if (distance <= startRadius || distance > newRadius) continue;
                
                // Check if this block is within the cone angle
                if (distance > 0.5) { // Skip center to avoid division by zero
                    // Calculate dot product to check if block is in front of player
                    double dotProduct = (deltaX * lookX + deltaZ * lookZ) / distance;
                    
                    // Convert dot product to angle (dot product = cos(angle))
                    double angleFromLookDirection = Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct)));
                    
                    // Skip blocks outside the cone angle
                    if (angleFromLookDirection > coneHalfAngle) continue;
                }
                
                // This block is within the cone and distance range - process it
                BlockPos targetPos = new BlockPos(x, centerPos.getY(), z);
                if (processWindBlockColumn(level, targetPos, processedPositions, chargeFactor)) {
                    blocksProcessed++;
                }
            }
        }

        // Apply knockback effect to entities within the wind cone
        applyKnockbackToEntities(player, level, centerPos, newRadius, currentRadius, lookX, lookZ, quirkFactor, baseKnockback, chargeFactor);

        // Add visual effects
        addWindWaveVisualization(level, centerPos, newRadius, lookX, lookZ, chargeFactor);
        
        // Play wave sound effects periodically
        if (newRadius % 3 == 0) {
            level.playSound(null, centerPos, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.15f, 1.2f + (newRadius * 0.05f));
        }
    }

    private void applyKnockbackToEntities(ServerPlayer caster, ServerLevel level, BlockPos centerPos, int newRadius, int currentRadius, double lookX, double lookZ, double quirkFactor, float baseKnockback, float chargeFactor) {
        // Get entities in the expanding area
        AABB searchArea = new AABB(
            centerPos.getX() - newRadius, centerPos.getY() - 4, centerPos.getZ() - newRadius,
            centerPos.getX() + newRadius, centerPos.getY() + 8, centerPos.getZ() + newRadius
        );
        
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchArea);
        double coneHalfAngle = Math.toRadians(45); // 45 degrees each side
        
        for (LivingEntity entity : entities) {
            // Skip the caster
            if (entity == caster) continue;
            
            // Calculate position relative to caster
            double entityX = entity.getX() - centerPos.getX();
            double entityZ = entity.getZ() - centerPos.getZ();
            double distance = Math.sqrt(entityX * entityX + entityZ * entityZ);
            
            // Only affect entities within the new ring (not already processed)
            if (distance <= currentRadius || distance > newRadius) continue;
            
            // Check if entity is within the cone
            if (distance > 1) {
                double entityAngle = Math.atan2(entityZ, entityX);
                double lookAngle = Math.atan2(lookZ, lookX);
                double angleDiff = Math.abs(entityAngle - lookAngle);
                
                // Normalize angle difference
                if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                
                if (angleDiff > coneHalfAngle) continue;
            }
            
            // Calculate knockback strength based on proximity, charge, and quirk factor
            double proximityFactor = 1.0 - (distance / newRadius);
            double knockbackStrength = baseKnockback * chargeFactor * proximityFactor;
            
            // Scale knockback with quirk factor
            knockbackStrength *= (1.0 + quirkFactor * QUIRK_KNOCKBACK_MULTIPLIER);
            
            // Calculate direction for knockback (away from player in cone direction)
            double normalizedDistance = Math.max(distance, 0.1);
            double pushX = (entityX / normalizedDistance) * knockbackStrength;
            double pushZ = (entityZ / normalizedDistance) * knockbackStrength;
            double pushY = Math.min(knockbackStrength * 0.4, 1.5); // Upward component for wind effect
            
            // Apply the knockback
            entity.push(pushX, pushY, pushZ);
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

    private void addWindWaveVisualization(ServerLevel level, BlockPos centerPos, int radius, double lookX, double lookZ, float chargeFactor) {
        if (radius <= 0) return;
        
        double coneHalfAngle = Math.toRadians(45);
        
        // Create a proper wall of particles across the entire cone width
        int raysCount = Math.min(Math.max(8, radius * 2), 24); // More rays for denser wall
        
        // Create particles across the entire cone width at this radius
        for (int ray = 0; ray < raysCount; ray++) {
            // Calculate angle across the cone (from -45° to +45°)
            double angleStep = (2 * coneHalfAngle) / (raysCount - 1);
            double angle = -coneHalfAngle + (ray * angleStep);
            
            // Calculate ray direction
            double rayX = lookX * Math.cos(angle) - lookZ * Math.sin(angle);
            double rayZ = lookX * Math.sin(angle) + lookZ * Math.cos(angle);
            
            // Create particles along this ray at the current radius
            double x = centerPos.getX() + rayX * radius;
            double z = centerPos.getZ() + rayZ * radius;
            double y = centerPos.getY() + ThreadLocalRandom.current().nextDouble() * 2;
            
            // Main wall particles
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 2, 
                rayX * 0.3, 0.2, rayZ * 0.3, 0.15);
            
            // Additional detail particles for higher charge
            if (ThreadLocalRandom.current().nextDouble() < chargeFactor * 0.6) {
                level.sendParticles(ParticleTypes.POOF, x, y + 0.5, z, 1, 
                    0.1, 0.2, 0.1, 0.02);
            }
            
            // Add depth to the wall by creating particles slightly behind the main radius
            if (radius > 2 && ThreadLocalRandom.current().nextDouble() < 0.4) {
                double depthOffset = 0.3 + ThreadLocalRandom.current().nextDouble() * 0.4;
                double backX = centerPos.getX() + rayX * (radius - depthOffset);
                double backZ = centerPos.getZ() + rayZ * (radius - depthOffset);
                double backY = centerPos.getY() + ThreadLocalRandom.current().nextDouble() * 1.8;
                
                level.sendParticles(ParticleTypes.CLOUD, backX, backY, backZ, 1, 
                    rayX * 0.2, 0.15, rayZ * 0.2, 0.1);
            }
        }
        
        // Add interior volume particles to fill the cone space
        int volumeParticles = Math.min((int)(radius * chargeFactor * 1.2), 16); // More volume particles
        for (int i = 0; i < volumeParticles; i++) {
            // Random angle within the cone
            double randomAngle = (ThreadLocalRandom.current().nextDouble() - 0.5) * Math.toRadians(90);
            // Random distance within the radius
            double randomDistance = ThreadLocalRandom.current().nextDouble() * radius * 0.9;
            
            double rayX = lookX * Math.cos(randomAngle) - lookZ * Math.sin(randomAngle);
            double rayZ = lookX * Math.sin(randomAngle) + lookZ * Math.cos(randomAngle);
            
            double x = centerPos.getX() + rayX * randomDistance;
            double z = centerPos.getZ() + rayZ * randomDistance;
            double y = centerPos.getY() + ThreadLocalRandom.current().nextDouble() * 1.5;
            
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 
                rayX * 0.1, 0.05, rayZ * 0.1, 0.05);
        }
        
        // Add edge definition particles for visual clarity
        for (int side = -1; side <= 1; side += 2) {
            double edgeAngle = side * coneHalfAngle;
            double rayX = lookX * Math.cos(edgeAngle) - lookZ * Math.sin(edgeAngle);
            double rayZ = lookX * Math.sin(edgeAngle) + lookZ * Math.cos(edgeAngle);
            
            double x = centerPos.getX() + rayX * radius;
            double z = centerPos.getZ() + rayZ * radius;
            double y = centerPos.getY() + ThreadLocalRandom.current().nextDouble() * 2;
            
            // Slightly more intense particles on the edges for definition
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 3, 
                rayX * 0.4, 0.25, rayZ * 0.4, 0.2);
        }
    }

    private boolean processWindBlockColumn(ServerLevel level, BlockPos centerPos, Set<BlockPos> processedPositions, float chargeFactor) {
        boolean processedAny = false;
        
        // Find the terrain surface at this X,Z coordinate
        int surfaceY = findTerrainSurface(level, centerPos.getX(), centerPos.getZ(), centerPos.getY());
        
        // Process blocks around the surface level
        // Go from a few blocks below surface to well above for trees
        int minY = Math.max(level.getMinBuildHeight(), surfaceY - 3);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, surfaceY + 15);
        
        for (int checkY = minY; checkY <= maxY; checkY++) {
            BlockPos checkPos = new BlockPos(centerPos.getX(), checkY, centerPos.getZ());
            
            // Skip if already processed
            if (processedPositions.contains(checkPos)) continue;
            processedPositions.add(checkPos);
            
            BlockState blockState = level.getBlockState(checkPos);
            Block block = blockState.getBlock();
            
            // Skip air blocks
            if (block == Blocks.AIR) continue;
            
            // Light wind effects - always active
            if (LIGHT_BLOCKS.contains(block)) {
                level.destroyBlock(checkPos, false);
                
                // Add wind particles
                level.sendParticles(ParticleTypes.POOF, 
                    checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 
                    3, 0.3, 0.3, 0.3, 0.1);
                processedAny = true;
            }
            // Medium wind effects - require some charge
            else if (chargeFactor >= 0.3f && LEAF_BLOCKS.contains(block)) {
                // Scatter leaves - destroy chance based on charge
                if (ThreadLocalRandom.current().nextFloat() < chargeFactor * 0.8f) {
                    scheduleBlockDestroy(level, checkPos, ThreadLocalRandom.current().nextInt(10) + 1);
                }
                
                // Add scattering particles
                level.sendParticles(ParticleTypes.POOF, 
                    checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 
                    2, 0.2, 0.2, 0.2, 0.05);
                processedAny = true;
            }
            // Strong wind effects - require high charge
            else if (chargeFactor >= 0.6f && ICE_SNOW_BLOCKS.contains(block)) {
                // Shatter ice and blow away snow
                scheduleBlockDestroy(level, checkPos, ThreadLocalRandom.current().nextInt(5) + 1);
                
                // Add shattering particles
                level.sendParticles(ParticleTypes.CLOUD, 
                    checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 
                    4, 0.3, 0.3, 0.3, 0.1);
                processedAny = true;
            }
        }
        
        return processedAny;
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

    private int findTerrainSurface(ServerLevel level, int x, int z, int startY) {
        int scanStartY = Math.min(level.getMaxBuildHeight() - 1, startY + 20);
        int scanEndY = Math.max(level.getMinBuildHeight(), startY - 40);

        for (int y = scanStartY; y >= scanEndY; y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState blockState = level.getBlockState(checkPos);
            Block block = blockState.getBlock();

            if (block == Blocks.AIR ||
                    LIGHT_BLOCKS.contains(block) ||
                    LEAF_BLOCKS.contains(block) ||
                    ICE_SNOW_BLOCKS.contains(block)) {
                continue;
            }

            return y;
        }

        return startY;
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
