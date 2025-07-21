package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AnimationTimer;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.SyncType;
import net.threetag.palladiumcore.util.Platform;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RotAbility extends Ability implements AnimationTimer {

    // Configurable properties
    public static final PalladiumProperty<Integer> DAMAGE = new IntegerProperty("damage").configurable("Base damage value for the rot ability");
    public static final PalladiumProperty<Integer> MAX_RADIUS = new IntegerProperty("max_radius").configurable("Maximum radius the rot can reach");
    public static final PalladiumProperty<Float> SPEED = new FloatProperty("speed").configurable("Speed at which the rot ability activates. Use 0 for instant activation.");
    
    // Animation properties
    public static final PalladiumProperty<Float> VALUE;
    public static final PalladiumProperty<Float> PREV_VALUE;
    
    private static final String ROT_RADIUS_OBJECTIVE = "MineHa.Decay.RotRadius";
    
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

    static {
        VALUE = new FloatProperty("value").sync(SyncType.NONE).disablePersistence();
        PREV_VALUE = new FloatProperty("prev_value").sync(SyncType.NONE).disablePersistence();
    }

    public RotAbility() {
        super();
        this.withProperty(DAMAGE, 10)
            .withProperty(MAX_RADIUS, 15)
            .withProperty(SPEED, 0.1F); // Slower activation for dramatic effect
    }

    @Override
    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(VALUE, 0.0F);
        manager.register(PREV_VALUE, 0.0F);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {
            if (entry.getProperty(VALUE) <= 0.0F) {
                // Start the rot effect - damage held item and play initial sound
                damageHeldItem(player, entry.getProperty(DAMAGE));
                
                if (entity.level() instanceof ServerLevel serverLevel) {
                    serverLevel.playSound(null, player.blockPosition(), 
                        SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.3f, 1.5f);
                }
            }
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        float speed = entry.getProperty(SPEED);
        float value = entry.getProperty(VALUE);
        entry.setUniqueProperty(PREV_VALUE, value);

        boolean active = enabled;
        
        if (speed > 0.0F) {
            // Handle animation progression
            if (entry.isEnabled() && value < 1.0F) {
                entry.setUniqueProperty(VALUE, value = Math.min(value + speed, 1.0F));
            } else if (!entry.isEnabled() && value > 0.0F) {
                entry.setUniqueProperty(VALUE, value = Math.max(value - speed, 0.0F));
            }
            
            active = value >= 1.0F;
        }

        // Execute rot effect when fully active
        if (active) {
            try {
                executeRotEffect(player, serverLevel, entry);
            } catch (Exception e) {
                BanditsQuirkLibForge.LOGGER.error("Error executing rot ability: ", e);
            }
        }
        
        // Add continuous visual effects while charging up
        if (value > 0.0F && value < 1.0F) {
            addChargingEffects(serverLevel, player, value);
        }
    }

    private void executeRotEffect(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
        int baseDamage = entry.getProperty(DAMAGE);
        int maxPossibleRadius = entry.getProperty(MAX_RADIUS);

        // Get or create rot radius scoreboard
        Scoreboard scoreboard = player.getServer().getScoreboard();
        Objective rotRadiusObj = scoreboard.getObjective(ROT_RADIUS_OBJECTIVE);
        if (rotRadiusObj == null) {
            rotRadiusObj = scoreboard.addObjective(ROT_RADIUS_OBJECTIVE, 
                net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY, 
                net.minecraft.network.chat.Component.literal("Rot Radius"), 
                net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER);
        }

        int currentRadius = scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), rotRadiusObj).getScore();
        
        // Calculate new radius based on damage and quirk factor
        int radiusIncrease = Math.max(1, baseDamage / 5);
        int newRadius = Math.min(currentRadius + radiusIncrease, maxPossibleRadius + (int)(quirkFactor * 10));
        
        // Update scoreboard
        scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), rotRadiusObj).setScore(newRadius);

        // Execute the actual rot effect
        performRotWave(player, level, newRadius, currentRadius, quirkFactor);
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
        int maxBlocksPerTick = 60; // Reduced for performance
        
        // Create a cone-shaped rot pattern extending forward from the player
        int coneLength = newRadius;
        double coneAngle = Math.toRadians(60); // Wider 60 degree cone
        
        // Process blocks in a cone shape in front of the player
        for (int distance = Math.max(0, currentRadius); distance <= coneLength && blocksProcessed < maxBlocksPerTick; distance++) {
            // Calculate cone width at this distance
            double coneWidth = Math.tan(coneAngle) * distance;
            int maxWidth = Math.max(1, (int) Math.floor(coneWidth));
            
            // Include center area for close distances
            boolean includeCenter = distance <= 3;
            
            for (int side = -maxWidth; side <= maxWidth; side++) {
                // Calculate perpendicular direction
                double perpX = -lookZ;
                double perpZ = lookX;
                
                // Calculate position in the cone
                double baseX = lookX * distance;
                double baseZ = lookZ * distance;
                double offsetX = perpX * side;
                double offsetZ = perpZ * side;
                
                int x = (int) Math.round(baseX + offsetX);
                int z = (int) Math.round(baseZ + offsetZ);
                
                // Process center area for close distances
                if (includeCenter && distance <= 2) {
                    for (int centerX = -1; centerX <= 1; centerX++) {
                        for (int centerZ = -1; centerZ <= 1; centerZ++) {
                            if (Math.abs(centerX) + Math.abs(centerZ) <= distance) {
                                if (processRotBlock(level, centerPos, centerX, centerZ, distance)) {
                                    blocksProcessed++;
                                }
                                if (blocksProcessed >= maxBlocksPerTick) break;
                            }
                        }
                        if (blocksProcessed >= maxBlocksPerTick) break;
                    }
                }
                
                // Process the main cone block
                if (blocksProcessed < maxBlocksPerTick) {
                    if (processRotBlock(level, centerPos, x, z, distance)) {
                        blocksProcessed++;
                    }
                }
            }
        }

        // Apply decay effect to entities within the rot cone
        applyDecayToEntities(player, level, centerPos, newRadius, lookX, lookZ);

        // Add visual and audio effects
        addRotVisualization(level, centerPos, newRadius);
        
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

    private void addChargingEffects(ServerLevel level, ServerPlayer player, float chargeProgress) {
        BlockPos pos = player.blockPosition();
        
        // Charging particles at player's feet
        if (ThreadLocalRandom.current().nextFloat() < chargeProgress * 0.5f) {
            level.sendParticles(ParticleTypes.SMOKE, 
                pos.getX() + ThreadLocalRandom.current().nextFloat() - 0.5f, 
                pos.getY() - 0.2, 
                pos.getZ() + ThreadLocalRandom.current().nextFloat() - 0.5f, 
                1, 0.1, 0.1, 0.1, 0.01);
        }
        
        // Play charging sound
        if (chargeProgress > 0.8f && ThreadLocalRandom.current().nextFloat() < 0.1f) {
            level.playSound(null, pos, SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 0.2f, 1.8f);
        }
    }

    private boolean processRotBlock(ServerLevel level, BlockPos centerPos, int offsetX, int offsetZ, int distance) {
        boolean processedAny = false;
        
        // Check blocks in a vertical range
        for (int y = -4; y <= 3; y++) {
            int checkY = centerPos.getY() + y;
            
            // Don't go too deep underground or too high
            if (checkY < level.getMinBuildHeight() || checkY > centerPos.getY() + 8) continue;
            
            BlockPos checkPos = new BlockPos(centerPos.getX() + offsetX, checkY, centerPos.getZ() + offsetZ);
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
            // Destroy plant life
            else if (PLANT_BLOCKS.contains(block)) {
                // Schedule destruction with delay for visual effect
                int delay = ThreadLocalRandom.current().nextInt(10) + distance;
                scheduleBlockDestroy(level, checkPos, delay);
                
                // Add withering particles
                level.sendParticles(ParticleTypes.SMOKE, 
                    checkPos.getX() + 0.5, checkPos.getY() + 0.5, checkPos.getZ() + 0.5, 
                    2, 0.2, 0.2, 0.2, 0.02);
                processedAny = true;
            }
        }
        
        return processedAny;
    }

    private void applyDecayToEntities(ServerPlayer caster, ServerLevel level, BlockPos centerPos, int radius, double lookX, double lookZ) {
        // Get entities in a wider area but filter to cone in front of player
        AABB searchArea = new AABB(
            centerPos.getX() - radius, centerPos.getY() - 4, centerPos.getZ() - radius,
            centerPos.getX() + radius, centerPos.getY() + 8, centerPos.getZ() + radius
        );
        
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchArea);
        
        for (LivingEntity entity : entities) {
            // Skip the caster
            if (entity == caster) continue;
            
            // Calculate position relative to caster
            double entityX = entity.getX() - centerPos.getX();
            double entityZ = entity.getZ() - centerPos.getZ();
            double distance = Math.sqrt(entityX * entityX + entityZ * entityZ);
            
            // Only affect entities within radius
            if (distance > radius) continue;
            
            // Check if entity is in front of the player (cone check)
            if (distance > 1) {
                double dotProduct = (entityX * lookX + entityZ * lookZ) / distance;
                double coneThreshold = Math.cos(Math.toRadians(60)); // 60 degree cone
                if (dotProduct < coneThreshold) continue;
            }
            
            // Calculate effect duration and amplifier based on proximity
            double proximityFactor = 1.0 - (distance / radius);
            int effectDuration = (int) (80 + (proximityFactor * 160)); // 4-12 seconds
            int effectAmplifier = (int) Math.floor(proximityFactor * 2); // 0-2 amplifier
            
            // Apply wither effect as decay
            entity.addEffect(new MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WITHER, 
                effectDuration, 
                effectAmplifier, 
                false, 
                true
            ));
            
            // Add visual indication
            level.sendParticles(ParticleTypes.SOUL, 
                entity.getX(), entity.getY() + 1, entity.getZ(), 
                5, 0.5, 0.5, 0.5, 0.05);
        }
    }

    private void addRotVisualization(ServerLevel level, BlockPos centerPos, int radius) {
        if (radius <= 0) return;
        
        // Create particle ring around the perimeter
        double circumference = 2 * Math.PI * radius;
        int numPerimeterParticles = Math.min(Math.max(6, (int)(circumference / 3)), 20);
        
        for (int i = 0; i < numPerimeterParticles; i++) {
            double angle = (i / (double)numPerimeterParticles) * 2 * Math.PI;
            double x = centerPos.getX() + Math.cos(angle) * radius;
            double z = centerPos.getZ() + Math.sin(angle) * radius;
            double y = centerPos.getY() - 0.5 + ThreadLocalRandom.current().nextDouble() * 1.5;
            
            level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 1, 0.1, 0.2, 0.1, 0.02);
            
            if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                level.sendParticles(ParticleTypes.ASH, x, y + 0.3, z, 1, 0.1, 0.3, 0.1, 0.01);
            }
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
    public float getAnimationValue(AbilityInstance entry, float partialTick) {
        return Mth.lerp(partialTick, entry.getProperty(PREV_VALUE), entry.getProperty(VALUE));
    }

    @Override
    public float getAnimationTimer(AbilityInstance entry, float partialTick, boolean maxedOut) {
        return maxedOut ? 1.0F : Mth.lerp(partialTick, entry.getProperty(PREV_VALUE), entry.getProperty(VALUE));
    }

    @Override
    public String getDocumentationDescription() {
        return "Sends out a cone-shaped wave of rot in front of the player, destroying plant life and applying decay effects to entities. Hold the activation key to charge up the ability.";
    }
} 