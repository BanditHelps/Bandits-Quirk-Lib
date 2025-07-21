package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.SyncType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RotAbility extends Ability {

    // Quirk Factor Scaling Constants - Adjust these to balance the ability
    private static final float QUIRK_SPEED_MULTIPLIER = 3.0f; // How much quirk factor affects speed (3x at 100% quirk)
    private static final float QUIRK_RANGE_MULTIPLIER = 4.0f; // How much quirk factor affects range (2x at 100% quirk)
    private static final float QUIRK_DECAY_MULTIPLIER = 4.0f; // How much quirk factor affects decay strength (4 extra levels at 100% quirk)
    private static final float MAX_EFFECTIVE_SPEED = 15.0f; // Maximum speed cap to prevent crashes
    
    // Configurable properties
    public static final PalladiumProperty<Integer> DAMAGE = new IntegerProperty("damage").configurable("Base damage value for the rot ability");
    public static final PalladiumProperty<Integer> MAX_RADIUS = new IntegerProperty("max_radius").configurable("Maximum radius the rot can reach");
    public static final PalladiumProperty<Float> SPEED = new FloatProperty("speed").configurable("Speed at which the rot ability activates. Use 0 for instant activation.");

    // Unique values for "faux animation" and timing
    public static final PalladiumProperty<Integer> DISTANCE;
    public static final PalladiumProperty<Integer> PREV_DISTANCE;
    public static final PalladiumProperty<Integer> TICKS_SINCE_START;


    
    // Plant blocks that can be affected by rot
    private static final Set<Block> PLANT_BLOCKS = Set.of(
        Blocks.GRASS, Blocks.TALL_GRASS, Blocks.FERN, Blocks.LARGE_FERN,
        Blocks.DANDELION, Blocks.POPPY, Blocks.BLUE_ORCHID, Blocks.ALLIUM,
        Blocks.AZURE_BLUET, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.WHITE_TULIP,
        Blocks.PINK_TULIP, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.LILY_OF_THE_VALLEY,
        Blocks.SUNFLOWER, Blocks.LILAC, Blocks.ROSE_BUSH, Blocks.PEONY,
        Blocks.DEAD_BUSH, Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES,
        Blocks.BEETROOTS, Blocks.SWEET_BERRY_BUSH, Blocks.BAMBOO, Blocks.SUGAR_CANE,
        Blocks.CACTUS, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM,
        Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
        Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
        Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
        Blocks.OAK_SAPLING, Blocks.SPRUCE_SAPLING, Blocks.BIRCH_SAPLING,
        Blocks.JUNGLE_SAPLING, Blocks.ACACIA_SAPLING, Blocks.DARK_OAK_SAPLING,
        Blocks.KELP, Blocks.KELP_PLANT, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS,
        Blocks.VINE, Blocks.GLOW_LICHEN, Blocks.MOSS_CARPET, Blocks.MOSS_BLOCK
    );

    // Tree blocks for propagation
    private static final Set<Block> WOOD_BLOCKS = Set.of(
        Blocks.OAK_LOG, Blocks.SPRUCE_LOG, Blocks.BIRCH_LOG,
        Blocks.JUNGLE_LOG, Blocks.ACACIA_LOG, Blocks.DARK_OAK_LOG,
        Blocks.CRIMSON_STEM, Blocks.WARPED_STEM,
        Blocks.STRIPPED_OAK_LOG, Blocks.STRIPPED_SPRUCE_LOG, Blocks.STRIPPED_BIRCH_LOG,
        Blocks.STRIPPED_JUNGLE_LOG, Blocks.STRIPPED_ACACIA_LOG, Blocks.STRIPPED_DARK_OAK_LOG,
        Blocks.STRIPPED_CRIMSON_STEM, Blocks.STRIPPED_WARPED_STEM,
        Blocks.OAK_WOOD, Blocks.SPRUCE_WOOD, Blocks.BIRCH_WOOD,
        Blocks.JUNGLE_WOOD, Blocks.ACACIA_WOOD, Blocks.DARK_OAK_WOOD,
        Blocks.CRIMSON_HYPHAE, Blocks.WARPED_HYPHAE,
        Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_BIRCH_WOOD,
        Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_DARK_OAK_WOOD,
        Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_WARPED_HYPHAE
    );

    private static final Set<Block> LEAF_BLOCKS = Set.of(
        Blocks.OAK_LEAVES, Blocks.SPRUCE_LEAVES, Blocks.BIRCH_LEAVES,
        Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
        Blocks.AZALEA_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
        Blocks.NETHER_WART_BLOCK, Blocks.WARPED_WART_BLOCK
    );

    public RotAbility() {
        this.withProperty(DAMAGE, 10)
            .withProperty(MAX_RADIUS, 15)
            .withProperty(SPEED, 1.5F);
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(DISTANCE, 0);
        manager.register(PREV_DISTANCE, 0);
        manager.register(TICKS_SINCE_START, 0);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {
                // Reset timer when ability starts
                entry.setUniqueProperty(TICKS_SINCE_START, 0);
                entry.setUniqueProperty(DISTANCE, 0);
                entry.setUniqueProperty(PREV_DISTANCE, 0);
                
                // Start the rot effect - damage held item and play initial sound
                damageHeldItem(player, entry.getProperty(DAMAGE));
                
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, player.blockPosition(),
                            SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.3f, 1.5f);
                }
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        // Execute rot effect when fully active
        if (enabled) {
            // Increment timer
            int currentTicks = entry.getProperty(TICKS_SINCE_START);
            entry.setUniqueProperty(TICKS_SINCE_START, currentTicks + 1);
            
            try {
                executeRotEffect(player, serverLevel, entry);
            } catch (Exception e) {
                BanditsQuirkLibForge.LOGGER.error("Error executing rot ability: ", e);
            }
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        // Reset the properties when the ability ends
        if (entity instanceof ServerPlayer player) {
            entry.setUniqueProperty(DISTANCE, 0);
            entry.setUniqueProperty(PREV_DISTANCE, 0);
            entry.setUniqueProperty(TICKS_SINCE_START, 0);
            
            if (entity.level() instanceof ServerLevel serverLevel) {
                // Play ending sound effect
                serverLevel.playSound(null, player.blockPosition(),
                        SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.5f, 1.2f);
            }
        }
    }



    private void executeRotEffect(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
        int baseDamage = entry.getProperty(DAMAGE);
        int maxPossibleRadius = entry.getProperty(MAX_RADIUS);
        float speed = entry.getProperty(SPEED);

        // Get current radius from property and store previous
        int currentRadius = entry.getProperty(DISTANCE);
        entry.setUniqueProperty(PREV_DISTANCE, currentRadius);
        
        // Get current ticks from property
        int ticksSinceStart = entry.getProperty(TICKS_SINCE_START);
        
        // Apply quirk factor to speed - higher quirk factor = much faster expansion
        float speedMultiplier = 1.0f + ((float)quirkFactor * QUIRK_SPEED_MULTIPLIER);
        float effectiveSpeed = Math.min(speed * speedMultiplier, MAX_EFFECTIVE_SPEED);
        
        // Apply quirk factor to range - higher quirk factor = larger max radius
        int effectiveMaxRadius = maxPossibleRadius + (int)(quirkFactor * QUIRK_RANGE_MULTIPLIER * maxPossibleRadius);
        
        // Calculate how many ticks should pass before each radius expansion  
        int ticksPerExpansion = Math.max(1, (int)(5 / effectiveSpeed));
        
        // Calculate expected radius based on time elapsed
        int expectedRadius = Math.min(1 + (ticksSinceStart / ticksPerExpansion), effectiveMaxRadius);
        
        // Add warning particles before expansion
        if (expectedRadius > currentRadius) {
            // Update the distance property to new radius
            entry.setUniqueProperty(DISTANCE, expectedRadius);
            
            // Execute the actual rot effect for the new area
            performRotWave(player, level, expectedRadius, currentRadius, quirkFactor);
        }
        
        // Always show some ongoing effect particles if we have any radius
        if (currentRadius > 0) {
            addOngoingEffectParticles(level, player.blockPosition(), currentRadius, player.getLookAngle());
        }
    }

    private void performRotWave(ServerPlayer player, ServerLevel level, int newRadius, int currentRadius, double quirkFactor) {
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
        int maxBlocksPerTick = 300; // Increased to handle larger cone areas properly
        Set<BlockPos> processedPositions = new HashSet<>();
        
        // Create a cone shape by checking all blocks in expanding squares
        int coneLength = newRadius;
        double coneHalfAngle = Math.toRadians(30); // 60 degree total cone (30 degrees each side)
        
        // Process blocks in expanding area from current radius to new radius
        int startRadius = Math.max(1, currentRadius);
        int searchSize = coneLength + 2; // Add buffer for edge cases
        
        // Check all blocks in a square area, but only process those in the cone and within the expanding radius
        for (int x = centerPos.getX() - searchSize; x <= centerPos.getX() + searchSize && blocksProcessed < maxBlocksPerTick; x++) {
            for (int z = centerPos.getZ() - searchSize; z <= centerPos.getZ() + searchSize && blocksProcessed < maxBlocksPerTick; z++) {
                // Calculate distance from center
                double deltaX = x - centerPos.getX();
                double deltaZ = z - centerPos.getZ();
                double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
                
                // Skip blocks that are too close (already processed) or too far
                if (distance < startRadius || distance > coneLength) continue;
                
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
                if (processRotBlockColumn(level, targetPos, processedPositions)) {
                    blocksProcessed++;
                }
            }
        }

        // Apply decay effect to entities within the rot cone
        applyDecayToEntities(player, level, centerPos, newRadius, lookX, lookZ, quirkFactor);

        // Add visual and audio effects
        addRotVisualization(level, centerPos, newRadius, lookX, lookZ);
        
        if (newRadius > currentRadius) {
            // Enhanced effects when radius increases
            level.sendParticles(ParticleTypes.LARGE_SMOKE, 
                centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                15, 1, 0.5, 1, 0.1);
                
            level.sendParticles(ParticleTypes.ASH, 
                centerPos.getX(), centerPos.getY() + 1, centerPos.getZ(), 
                20, 2, 1, 2, 0.1);
                
            level.playSound(null, centerPos, SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 0.7f, 0.8f);
        }
    }

    private boolean processRotBlockColumn(ServerLevel level, BlockPos centerPos, Set<BlockPos> processedPositions) {
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
            
            // Convert grass blocks to dirt
            if (block == Blocks.GRASS_BLOCK) {
                level.setBlock(checkPos, Blocks.DIRT.defaultBlockState(), 3);
                
                // Add decay particles
                level.sendParticles(ParticleTypes.LARGE_SMOKE, 
                    checkPos.getX() + 0.5, checkPos.getY() + 1, checkPos.getZ() + 0.5, 
                    3, 0.3, 0.3, 0.3, 0.05);
                processedAny = true;
            }
            // Destroy plant life and propagate to connected trees
            else if (PLANT_BLOCKS.contains(block) || LEAF_BLOCKS.contains(block)) {
                // If it's a leaf block, propagate decay to connected tree parts
                if (LEAF_BLOCKS.contains(block)) {
                    propagateTreeDecay(level, checkPos, processedPositions);
                }
                
                // Schedule destruction with delay for visual effect
                int delay = ThreadLocalRandom.current().nextInt(10) + 1;
                scheduleBlockDestroy(level, checkPos, delay);
                
                // Add withering particles
                level.sendParticles(ParticleTypes.SMOKE, 
                    checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 
                    2, 0.2, 0.2, 0.2, 0.02);
                processedAny = true;
            }
            // If it's a wood block, decay it and connected tree parts
            else if (WOOD_BLOCKS.contains(block)) {
                propagateTreeDecay(level, checkPos, processedPositions);
                
                // Schedule destruction with delay
                int delay = ThreadLocalRandom.current().nextInt(15) + 5;
                scheduleBlockDestroy(level, checkPos, delay);
                
                // Add decay particles
                level.sendParticles(ParticleTypes.LARGE_SMOKE, 
                    checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 
                    4, 0.3, 0.3, 0.3, 0.08);
                processedAny = true;
            }
        }
        
        return processedAny;
    }

    /**
     * Finds the terrain surface at the given X,Z coordinate by scanning from above.
     * Returns the Y coordinate of the highest solid, non-plant block (the "ground").
     */
    private int findTerrainSurface(ServerLevel level, int x, int z, int startY) {
        // Start scanning from above the player position
        int scanStartY = Math.min(level.getMaxBuildHeight() - 1, startY + 20);
        int scanEndY = Math.max(level.getMinBuildHeight(), startY - 40);
        
        // Scan downward to find the terrain surface
        for (int y = scanStartY; y >= scanEndY; y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockState blockState = level.getBlockState(checkPos);
            Block block = blockState.getBlock();
            
            // Skip air and plant-like blocks to find actual terrain
            if (block == Blocks.AIR || 
                PLANT_BLOCKS.contains(block) || 
                LEAF_BLOCKS.contains(block) ||
                block == Blocks.SNOW ||
                block == Blocks.POWDER_SNOW) {
                continue;
            }
            
            // Found a solid block - this is our terrain surface
            return y;
        }
        
        // If no solid block found, return the start Y as fallback
        return startY;
    }

    private void propagateTreeDecay(ServerLevel level, BlockPos startPos, Set<BlockPos> processedPositions) {
        Queue<BlockPos> toProcess = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        toProcess.offer(startPos);
        visited.add(startPos);
        
        int maxBlocks = 50; // Limit to prevent lag
        int processedBlocks = 0;
        
        while (!toProcess.isEmpty() && processedBlocks < maxBlocks) {
            BlockPos currentPos = toProcess.poll();
            processedBlocks++;
            
            // Check all 26 adjacent positions (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        
                        BlockPos neighborPos = currentPos.offset(dx, dy, dz);
                        
                        // Skip if already visited or processed
                        if (visited.contains(neighborPos) || processedPositions.contains(neighborPos)) continue;
                        
                        BlockState neighborState = level.getBlockState(neighborPos);
                        Block neighborBlock = neighborState.getBlock();
                        
                        // If it's a tree-related block, add it to the decay queue
                        if (WOOD_BLOCKS.contains(neighborBlock) || LEAF_BLOCKS.contains(neighborBlock)) {
                            visited.add(neighborPos);
                            toProcess.offer(neighborPos);
                            processedPositions.add(neighborPos);
                            
                            // Schedule destruction
                            int delay = ThreadLocalRandom.current().nextInt(20) + processedBlocks;
                            scheduleBlockDestroy(level, neighborPos, delay);
                            
                            // Add particles
                            if (WOOD_BLOCKS.contains(neighborBlock)) {
                                level.sendParticles(ParticleTypes.LARGE_SMOKE, 
                                    neighborPos.getX() + 0.5, neighborPos.getY() + 0.5, neighborPos.getZ() + 0.5, 
                                    3, 0.3, 0.3, 0.3, 0.05);
                            } else {
                                level.sendParticles(ParticleTypes.SMOKE, 
                                    neighborPos.getX() + 0.5, neighborPos.getY() + 0.5, neighborPos.getZ() + 0.5, 
                                    2, 0.2, 0.2, 0.2, 0.02);
                            }
                        }
                    }
                }
            }
        }
    }

    private void applyDecayToEntities(ServerPlayer caster, ServerLevel level, BlockPos centerPos, int radius, double lookX, double lookZ, double quirkFactor) {
        // Get entities in a wider area but filter to cone in front of player
        AABB searchArea = new AABB(
            centerPos.getX() - radius, centerPos.getY() - 4, centerPos.getZ() - radius,
            centerPos.getX() + radius, centerPos.getY() + 8, centerPos.getZ() + radius
        );
        
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchArea);
        double coneHalfAngle = Math.toRadians(30); // 30 degrees each side
        
        for (LivingEntity entity : entities) {
            // Skip the caster
            if (entity == caster) continue;
            
            // Calculate position relative to caster
            double entityX = entity.getX() - centerPos.getX();
            double entityZ = entity.getZ() - centerPos.getZ();
            double distance = Math.sqrt(entityX * entityX + entityZ * entityZ);
            
            // Only affect entities within radius
            if (distance > radius) continue;
            
            // Check if entity is within the cone
            if (distance > 1) {
                double entityAngle = Math.atan2(entityZ, entityX);
                double lookAngle = Math.atan2(lookZ, lookX);
                double angleDiff = Math.abs(entityAngle - lookAngle);
                
                // Normalize angle difference
                if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;
                
                if (angleDiff > coneHalfAngle) continue;
            }
            
            // Calculate effect duration and amplifier based on proximity and quirk factor
            double proximityFactor = 1.0 - (distance / radius);
            int effectDuration = (int) (80 + (proximityFactor * 160)); // 4-12 seconds
            
            // Scale amplifier with both proximity and quirk factor
            int baseAmplifier = (int) Math.floor(proximityFactor * 2); // 0-2 amplifier
            int quirkBonus = (int) Math.floor(quirkFactor * QUIRK_DECAY_MULTIPLIER); // Additional levels from quirk factor
            int effectAmplifier = Math.min(baseAmplifier + quirkBonus, 6); // Cap at level 6 to prevent excessive damage
            
            // Apply wither effect as decay
            entity.addEffect(new MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WITHER, 
                effectDuration, 
                effectAmplifier, 
                false, 
                true
            ));

            // Add extra ominous particles for high quirk factor
            if (quirkFactor > 0.5) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, 
                    entity.getX(), entity.getY() + 0.5, entity.getZ(), 
                    2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    private void addRotVisualization(ServerLevel level, BlockPos centerPos, int radius, double lookX, double lookZ) {
        if (radius <= 0) return;
        
        double coneHalfAngle = Math.toRadians(30);
        
        // Create particle cone outline
        int numParticles = Math.min(Math.max(8, radius * 2), 24);
        
        for (int i = 0; i < numParticles; i++) {
            // Create particles along both edges of the cone
            for (int side = -1; side <= 1; side += 2) {
                double angle = side * coneHalfAngle;
                double rayX = lookX * Math.cos(angle) - lookZ * Math.sin(angle);
                double rayZ = lookX * Math.sin(angle) + lookZ * Math.cos(angle);
                
                double distance = radius * (0.5 + (i / (double)numParticles) * 0.5);
                double x = centerPos.getX() + rayX * distance;
                double z = centerPos.getZ() + rayZ * distance;
                double y = centerPos.getY() - 0.5 + ThreadLocalRandom.current().nextDouble() * 1.5;
                
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.1, 0.2, 0.1, 0.02);
                
                if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                    level.sendParticles(ParticleTypes.ASH, x, y + 0.3, z, 1, 0.1, 0.3, 0.1, 0.01);
                }
            }
        }
    }

    private void addOngoingEffectParticles(ServerLevel level, BlockPos centerPos, int currentRadius, Vec3 lookDirection) {
        if (currentRadius <= 0) return;
        
        double lookX = lookDirection.x;
        double lookZ = lookDirection.z;
        double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (lookLength > 0) {
            lookX /= lookLength;
            lookZ /= lookLength;
        }
        
        // Subtle ongoing effect particles throughout the affected area
        int numParticles = Math.min(currentRadius / 2, 8);
        
        for (int i = 0; i < numParticles; i++) {
            double randomRadius = ThreadLocalRandom.current().nextDouble() * currentRadius;
            double randomAngle = (ThreadLocalRandom.current().nextDouble() - 0.5) * Math.toRadians(60);
            
            double rayX = lookX * Math.cos(randomAngle) - lookZ * Math.sin(randomAngle);
            double rayZ = lookX * Math.sin(randomAngle) + lookZ * Math.cos(randomAngle);
            
            double x = centerPos.getX() + rayX * randomRadius;
            double z = centerPos.getZ() + rayZ * randomRadius;
            double y = centerPos.getY() + ThreadLocalRandom.current().nextDouble() * 1.5;
            
            level.sendParticles(ParticleTypes.ASH, x, y, z, 1, 0.1, 0.2, 0.1, 0.01);
        }
    }

    private void scheduleBlockDestroy(ServerLevel level, BlockPos pos, int delayTicks) {
        level.getServer().tell(new net.minecraft.server.TickTask(
            level.getServer().getTickCount() + delayTicks,
            () -> {
                level.destroyBlock(pos, false);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, 
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                    3, 0.3, 0.3, 0.3, 0.1);
            }
        ));
    }

    private void damageHeldItem(ServerPlayer player, int damage) {
        ItemStack mainHand = player.getMainHandItem();
        
        if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
            double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
            int totalDamage = damage + (int)(damage * quirkFactor);
            
            mainHand.hurtAndBreak(totalDamage, player, (p) -> {
                p.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND);
            });
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Sends out a cone-shaped wave of rot in front of the player, destroying plant life and applying decay effects to entities. Decay spreads to connected tree parts. Quirk factor significantly increases expansion speed (up to 4x), range (up to 3x), and decay effect strength (up to +4 levels).";
    }

    static {
        DISTANCE = (new IntegerProperty("distance")).sync(SyncType.NONE).disablePersistence();
        PREV_DISTANCE = (new IntegerProperty("prev_distance")).sync(SyncType.NONE).disablePersistence();
        TICKS_SINCE_START = (new IntegerProperty("ticks_since_start")).sync(SyncType.NONE).disablePersistence();
    }
} 