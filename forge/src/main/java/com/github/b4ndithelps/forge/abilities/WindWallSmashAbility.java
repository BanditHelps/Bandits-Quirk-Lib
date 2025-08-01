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
    public static final PalladiumProperty<Integer> MAX_RADIUS = new IntegerProperty("max_radius").configurable("Maximum radius the air smash can reach");
    public static final PalladiumProperty<Float> SPEED = new FloatProperty("speed").configurable("Speed at which the air smash travels");
    public static final PalladiumProperty<Integer> MAX_CHARGE_TIME = new IntegerProperty("max_charge_time").configurable("Maximum charge time in ticks (20 ticks = 1 second)");

    // Unique values for charging and wave progression
    public static final PalladiumProperty<Integer> CHARGE_TICKS;
    public static final PalladiumProperty<Integer> DISTANCE;
    public static final PalladiumProperty<Integer> PREV_DISTANCE;
    public static final PalladiumProperty<Integer> TICKS_SINCE_RELEASE;
    public static final PalladiumProperty<Boolean> IS_CHARGING;
    public static final PalladiumProperty<Boolean> IS_RELEASED;

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
        this.withProperty(BASE_KNOCKBACK, 2.5F)
                .withProperty(MAX_RADIUS, 12)
                .withProperty(SPEED, 1.2F)
                .withProperty(MAX_CHARGE_TIME, 60); // 3 seconds max charge
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CHARGE_TICKS, 0);
        manager.register(DISTANCE, 0);
        manager.register(PREV_DISTANCE, 0);
        manager.register(TICKS_SINCE_RELEASE, 0);
        manager.register(IS_CHARGING, false);
        manager.register(IS_RELEASED, false);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {
            // Start charging
            entry.setUniqueProperty(CHARGE_TICKS, 0);
            entry.setUniqueProperty(DISTANCE, 0);
            entry.setUniqueProperty(PREV_DISTANCE, 0);
            entry.setUniqueProperty(TICKS_SINCE_RELEASE, 0);
            entry.setUniqueProperty(IS_CHARGING, true);
            entry.setUniqueProperty(IS_RELEASED, false);

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

        boolean isCharging = entry.getProperty(IS_CHARGING);
        boolean isReleased = entry.getProperty(IS_RELEASED);

        if (enabled) {
            if (isCharging && !isReleased) {
                // Handle charging phase
                handleChargingPhase(player, serverLevel, entry);
            } else if (isReleased) {
                // Handle air wave travel phase
                handleAirWavePhase(player, serverLevel, entry);
            }
        } else {
            // Ability key released - trigger the air smash if we were charging
            if (isCharging && !isReleased) {
                releaseAirSmash(player, serverLevel, entry);
            }
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        // Reset all properties when ability ends
        if (entity instanceof ServerPlayer player) {
            entry.setUniqueProperty(CHARGE_TICKS, 0);
            entry.setUniqueProperty(DISTANCE, 0);
            entry.setUniqueProperty(PREV_DISTANCE, 0);
            entry.setUniqueProperty(TICKS_SINCE_RELEASE, 0);
            entry.setUniqueProperty(IS_CHARGING, false);
            entry.setUniqueProperty(IS_RELEASED, false);

            if (entity.level() instanceof ServerLevel serverLevel) {
                // Play ending sound effect
                serverLevel.playSound(null, player.blockPosition(),
                        SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.3f, 0.8f);
            }
        }
    }

    private void handleChargingPhase(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        int chargeTicks = entry.getProperty(CHARGE_TICKS);
        int maxChargeTime = entry.getProperty(MAX_CHARGE_TIME);

        // Increment charge time
        chargeTicks++;
        entry.setUniqueProperty(CHARGE_TICKS, Math.min(chargeTicks, maxChargeTime));

        // Add charging particles and effects
        addChargingEffects(player, level, chargeTicks, maxChargeTime);

        // Play charging sounds
        if (chargeTicks % 10 == 0) {
            float pitch = 1.0f + (chargeTicks / (float)maxChargeTime) * 0.5f;
            level.playSound(null, player.blockPosition(),
                    SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.1f, pitch);
        }
    }

    private void handleAirWavePhase(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        int ticksSinceRelease = entry.getProperty(TICKS_SINCE_RELEASE);
        entry.setUniqueProperty(TICKS_SINCE_RELEASE, ticksSinceRelease + 1);

        try {
            executeAirWave(player, level, entry);
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error executing air smash ability: ", e);
        }
    }

    private void releaseAirSmash(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        entry.setUniqueProperty(IS_CHARGING, false);
        entry.setUniqueProperty(IS_RELEASED, true);
        entry.setUniqueProperty(TICKS_SINCE_RELEASE, 0);
        entry.setUniqueProperty(DISTANCE, 0);
        entry.setUniqueProperty(PREV_DISTANCE, 0);

        int chargeTicks = entry.getProperty(CHARGE_TICKS);

        // Play release sound with intensity based on charge
        float volume = 0.5f + (chargeTicks / (float)entry.getProperty(MAX_CHARGE_TIME)) * 0.8f;
        float pitch = 0.8f + (chargeTicks / (float)entry.getProperty(MAX_CHARGE_TIME)) * 0.4f;
        level.playSound(null, player.blockPosition(),
                SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, volume, pitch);

        // Add release particles
        addReleaseEffects(player, level, chargeTicks);
    }

    private void executeAirWave(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
        int chargeTicks = entry.getProperty(CHARGE_TICKS);
        int maxChargeTime = entry.getProperty(MAX_CHARGE_TIME);
        int maxRadius = entry.getProperty(MAX_RADIUS);
        float speed = entry.getProperty(SPEED);

        // Calculate charge factor (0.0 to 1.0)
        float chargeFactor = Math.min(chargeTicks / (float)maxChargeTime, 1.0f);

        // Get current distance and store previous
        int currentDistance = entry.getProperty(DISTANCE);
        entry.setUniqueProperty(PREV_DISTANCE, currentDistance);

        int ticksSinceRelease = entry.getProperty(TICKS_SINCE_RELEASE);

        // Apply quirk factor to speed and range
        float speedMultiplier = 1.0f + ((float)quirkFactor * QUIRK_SPEED_MULTIPLIER);
        float effectiveSpeed = Math.min(speed * speedMultiplier, MAX_EFFECTIVE_SPEED);

        int effectiveMaxRadius = maxRadius + (int)(quirkFactor * QUIRK_RANGE_MULTIPLIER * maxRadius);

        // Calculate expansion rate
        int ticksPerExpansion = Math.max(1, (int)(4 / effectiveSpeed));
        int expectedDistance = Math.min(1 + (ticksSinceRelease / ticksPerExpansion), effectiveMaxRadius);

        // Expand the air wave
        if (expectedDistance > currentDistance) {
            entry.setUniqueProperty(DISTANCE, expectedDistance);
            performAirWave(player, level, expectedDistance, currentDistance, chargeFactor, quirkFactor, entry.getProperty(BASE_KNOCKBACK));
        }

        // Add ongoing air effects
        if (currentDistance > 0) {
            addOngoingAirEffects(level, player.blockPosition(), currentDistance, player.getLookAngle());
        }

        // End the ability when max distance is reached
        if (currentDistance >= effectiveMaxRadius) {
            // The ability will naturally end when this method stops being called
        }
    }

    private void performAirWave(ServerPlayer player, ServerLevel level, int newDistance, int currentDistance, float chargeFactor, double quirkFactor, float knockback) {
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

        // Create cone shape for air wave
        double coneHalfAngle = Math.toRadians(35); // 70 degree total cone
        int startDistance = Math.max(1, currentDistance);

        // Process blocks in the expanding cone
        processAirWaveBlocks(level, centerPos, lookX, lookZ, newDistance, startDistance, coneHalfAngle, chargeFactor);

        // Apply wind effects to entities
        applyWindToEntities(player, level, centerPos, newDistance, lookX, lookZ, chargeFactor, quirkFactor, knockback);

        // Add visual effects
        addAirWaveVisualization(level, centerPos, newDistance, lookX, lookZ, chargeFactor);
    }

    private void processAirWaveBlocks(ServerLevel level, BlockPos centerPos, double lookX, double lookZ,
                                      int maxDistance, int startDistance, double coneHalfAngle, float chargeFactor) {
        int blocksProcessed = 0;
        int maxBlocksPerTick = 200;
        int searchSize = maxDistance + 2;

        for (int x = centerPos.getX() - searchSize; x <= centerPos.getX() + searchSize && blocksProcessed < maxBlocksPerTick; x++) {
            for (int z = centerPos.getZ() - searchSize; z <= centerPos.getZ() + searchSize && blocksProcessed < maxBlocksPerTick; z++) {
                double deltaX = x - centerPos.getX();
                double deltaZ = z - centerPos.getZ();
                double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                if (distance < startDistance || distance > maxDistance) continue;

                // Check cone angle
                if (distance > 0.5) {
                    double dotProduct = (deltaX * lookX + deltaZ * lookZ) / distance;
                    double angleFromLookDirection = Math.acos(Math.max(-1.0, Math.min(1.0, dotProduct)));
                    if (angleFromLookDirection > coneHalfAngle) continue;
                }

                BlockPos targetPos = new BlockPos(x, centerPos.getY(), z);
                if (processAirEffectColumn(level, targetPos, chargeFactor)) {
                    blocksProcessed++;
                }
            }
        }
    }

    private boolean processAirEffectColumn(ServerLevel level, BlockPos centerPos, float chargeFactor) {
        boolean processedAny = false;

        // Find terrain surface
        int surfaceY = findTerrainSurface(level, centerPos.getX(), centerPos.getZ(), centerPos.getY());
        int minY = Math.max(level.getMinBuildHeight(), surfaceY - 2);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, surfaceY + 12);

        for (int checkY = minY; checkY <= maxY; checkY++) {
            BlockPos checkPos = new BlockPos(centerPos.getX(), checkY, centerPos.getZ());
            BlockState blockState = level.getBlockState(checkPos);
            Block block = blockState.getBlock();

            if (block == Blocks.AIR) continue;

            // Light wind effects - always active
            if (LIGHT_BLOCKS.contains(block)) {
                // Create flying particles and destroy light blocks
                level.sendParticles(ParticleTypes.POOF,
                        checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5,
                        3, 0.3, 0.3, 0.3, 0.1);
                level.destroyBlock(checkPos, false);
                processedAny = true;
            }
            // Medium wind effects - require some charge
            else if (chargeFactor >= 0.3f && LEAF_BLOCKS.contains(block)) {
                // Scatter leaves
                level.sendParticles(ParticleTypes.POOF,
                        checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5,
                        2, 0.2, 0.2, 0.2, 0.05);

                // Randomly destroy leaves based on charge
                if (ThreadLocalRandom.current().nextFloat() < chargeFactor * 0.8f) {
                    scheduleBlockDestroy(level, checkPos, ThreadLocalRandom.current().nextInt(5) + 1);
                }
                processedAny = true;
            }
            // Strong wind effects - require high charge
            else if (chargeFactor >= 0.6f && ICE_SNOW_BLOCKS.contains(block)) {
                // Shatter ice and blow away snow
                level.sendParticles(ParticleTypes.CLOUD,
                        checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0.1);

                scheduleBlockDestroy(level, checkPos, ThreadLocalRandom.current().nextInt(3) + 1);
                processedAny = true;
            }
        }

        return processedAny;
    }

    private void applyWindToEntities(ServerPlayer caster, ServerLevel level, BlockPos centerPos, int radius,
                                     double lookX, double lookZ, float chargeFactor, double quirkFactor, float knockback) {
        AABB searchArea = new AABB(
                centerPos.getX() - radius, centerPos.getY() - 3, centerPos.getZ() - radius,
                centerPos.getX() + radius, centerPos.getY() + 6, centerPos.getZ() + radius
        );

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchArea);
        double coneHalfAngle = Math.toRadians(35);

        for (LivingEntity entity : entities) {
            if (entity == caster) continue;

            double entityX = entity.getX() - centerPos.getX();
            double entityZ = entity.getZ() - centerPos.getZ();
            double distance = Math.sqrt(entityX * entityX + entityZ * entityZ);

            if (distance > radius) continue;

            // Check cone angle
            if (distance > 1) {
                double entityAngle = Math.atan2(entityZ, entityX);
                double lookAngle = Math.atan2(lookZ, lookX);
                double angleDiff = Math.abs(entityAngle - lookAngle);
                if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                if (angleDiff > coneHalfAngle) continue;
            }

            // Calculate knockback strength
            double proximityFactor = 1.0 - (distance / radius);

            double knockbackStrength = knockback * chargeFactor * proximityFactor;
            knockbackStrength *= (1.0 + quirkFactor * QUIRK_KNOCKBACK_MULTIPLIER);

            // Apply knockback
            double normalizedDistance = Math.max(distance, 1.0);
            double pushX = (entityX / normalizedDistance) * knockbackStrength;
            double pushZ = (entityZ / normalizedDistance) * knockbackStrength;
            double pushY = Math.min(knockbackStrength * 0.3, 1.0); // Upward component

            entity.push(pushX, pushY, pushZ);
            entity.hurtMarked = true;

            // Add wind particles around entity
            level.sendParticles(ParticleTypes.CLOUD,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    3, 0.5, 0.5, 0.5, 0.1);
        }
    }

    private void addChargingEffects(ServerPlayer player, ServerLevel level, int chargeTicks, int maxChargeTime) {
        float chargeProgress = Math.min(chargeTicks / (float)maxChargeTime, 1.0f);

        // Swirling particles around player
        int particleCount = Math.max(2, (int)(chargeProgress * 8));

        for (int i = 0; i < particleCount; i++) {
            double angle = (chargeTicks + i * 45) * 0.1;
            double radius = 1.0 + chargeProgress;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + 0.5 + Math.sin(chargeTicks * 0.2) * 0.3;

            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
        }

        // Intensity particles when highly charged
        if (chargeProgress > 0.7f) {
            level.sendParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 1, player.getZ(),
                    2, 0.3, 0.3, 0.3, 0.05);
        }
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

    private void addAirWaveVisualization(ServerLevel level, BlockPos centerPos, int radius,
                                         double lookX, double lookZ, float chargeFactor) {
        if (radius <= 0) return;

        double coneHalfAngle = Math.toRadians(35);
        int numParticles = Math.min(Math.max(6, radius * 2), 20);

        for (int i = 0; i < numParticles; i++) {
            for (int side = -1; side <= 1; side += 2) {
                double angle = side * coneHalfAngle;
                double rayX = lookX * Math.cos(angle) - lookZ * Math.sin(angle);
                double rayZ = lookX * Math.sin(angle) + lookZ * Math.cos(angle);

                double distance = radius * (0.3 + (i / (double)numParticles) * 0.7);
                double x = centerPos.getX() + rayX * distance;
                double z = centerPos.getZ() + rayZ * distance;
                double y = centerPos.getY() + ThreadLocalRandom.current().nextDouble() * 2;

                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1,
                        rayX * 0.1, 0.1, rayZ * 0.1, 0.05);

                if (ThreadLocalRandom.current().nextDouble() < chargeFactor) {
                    level.sendParticles(ParticleTypes.POOF, x, y + 0.5, z, 1,
                            0.1, 0.2, 0.1, 0.02);
                }
            }
        }
    }

    private void addOngoingAirEffects(ServerLevel level, BlockPos centerPos, int currentRadius, Vec3 lookDirection) {
        if (currentRadius <= 0) return;

        double lookX = lookDirection.x;
        double lookZ = lookDirection.z;
        double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (lookLength > 0) {
            lookX /= lookLength;
            lookZ /= lookLength;
        }

        int numParticles = Math.min(currentRadius / 3, 6);

        for (int i = 0; i < numParticles; i++) {
            double randomRadius = ThreadLocalRandom.current().nextDouble() * currentRadius;
            double randomAngle = (ThreadLocalRandom.current().nextDouble() - 0.5) * Math.toRadians(70);

            double rayX = lookX * Math.cos(randomAngle) - lookZ * Math.sin(randomAngle);
            double rayZ = lookX * Math.sin(randomAngle) + lookZ * Math.cos(randomAngle);

            double x = centerPos.getX() + rayX * randomRadius;
            double z = centerPos.getZ() + rayZ * randomRadius;
            double y = centerPos.getY() + ThreadLocalRandom.current().nextDouble() * 2;

            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.1, 0.1, 0.1, 0.01);
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
        return "A charge-based wind ability that creates a cone-shaped air blast. Hold to charge for increased power. Blows back entities and destroys light blocks, leaves (medium charge), and ice/snow (high charge). Quirk factor increases speed (up to 3.5x), range (up to 3x), and knockback strength (up to 4x).";
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
        DISTANCE = (new IntegerProperty("distance")).sync(SyncType.NONE).disablePersistence();
        PREV_DISTANCE = (new IntegerProperty("prev_distance")).sync(SyncType.NONE).disablePersistence();
        TICKS_SINCE_RELEASE = (new IntegerProperty("ticks_since_release")).sync(SyncType.NONE).disablePersistence();
        IS_CHARGING = (new BooleanProperty("is_charging")).sync(SyncType.NONE).disablePersistence();
        IS_RELEASED = (new BooleanProperty("is_released")).sync(SyncType.NONE).disablePersistence();
    }
}
