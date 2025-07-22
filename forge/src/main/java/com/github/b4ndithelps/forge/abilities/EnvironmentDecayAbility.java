package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
import net.threetag.palladium.util.property.StringProperty;
import net.threetag.palladium.util.property.SyncType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class EnvironmentDecayAbility extends Ability {

    // Quirk Factor Scaling Constants
    private static final float QUIRK_INTENSITY_MULTIPLIER = 1.0f;
    private static final float QUIRK_BLOCKS_MULTIPLIER = 1.5f;
    private static final float QUIRK_RANGE_MULTIPLIER = 1.2f;
    private static final float QUIRK_SPEED_MULTIPLIER = 0.8f; // Lower = faster spread

    // Configurable properties
    public static final PalladiumProperty<Integer> DAMAGE = new IntegerProperty("damage").configurable("Base damage value for held items");
    public static final PalladiumProperty<Integer> MAX_BLOCKS = new IntegerProperty("max_blocks").configurable("Maximum number of blocks to decay");
    public static final PalladiumProperty<Integer> BASE_INTENSITY = new IntegerProperty("base_intensity").configurable("Base intensity level (1-4) determining which blocks can be decayed");
    public static final PalladiumProperty<String> DECAY_TYPE = new StringProperty("decay_type").configurable("Type of decay pattern: horizontal, vertical, circular");
    public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Range for targeting blocks");
    public static final PalladiumProperty<Float> SPREAD_SPEED = new FloatProperty("spread_speed").configurable("How fast the decay spreads (ticks between waves)");

    // Wave tracking properties
    public static final PalladiumProperty<Integer> CURRENT_WAVE;
    public static final PalladiumProperty<Integer> BLOCKS_DECAYED;
    public static final PalladiumProperty<Integer> TARGET_X;
    public static final PalladiumProperty<Integer> TARGET_Y;
    public static final PalladiumProperty<Integer> TARGET_Z;
    public static final PalladiumProperty<String> CURRENT_WAVE_BLOCKS; // Blocks in current wave front
    public static final PalladiumProperty<String> PROCESSED_BLOCKS; // All processed blocks
    public static final PalladiumProperty<Float> WAVE_TIMER; // Timer for wave progression

    public EnvironmentDecayAbility() {
        this.withProperty(DAMAGE, 15)
                .withProperty(MAX_BLOCKS, 50)
                .withProperty(BASE_INTENSITY, 2)
                .withProperty(DECAY_TYPE, "circular")
                .withProperty(RANGE, 8.0F)
                .withProperty(SPREAD_SPEED, 4.0F); // Ticks between waves
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CURRENT_WAVE, 0);
        manager.register(BLOCKS_DECAYED, 0);
        manager.register(TARGET_X, 0);
        manager.register(TARGET_Y, 0);
        manager.register(TARGET_Z, 0);
        manager.register(CURRENT_WAVE_BLOCKS, "");
        manager.register(PROCESSED_BLOCKS, "");
        manager.register(WAVE_TIMER, 0.0F);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {
            // Reset all tracking properties
            entry.setUniqueProperty(CURRENT_WAVE, 0);
            entry.setUniqueProperty(BLOCKS_DECAYED, 0);
            entry.setUniqueProperty(CURRENT_WAVE_BLOCKS, "");
            entry.setUniqueProperty(PROCESSED_BLOCKS, "");
            entry.setUniqueProperty(WAVE_TIMER, 0.0F);

            // Damage held item
            damageHeldItem(player, entry.getProperty(DAMAGE));

            // Find target block
            BlockPos targetPos = findTargetBlock(player, entry.getProperty(RANGE));
            if (targetPos != null) {
                entry.setUniqueProperty(TARGET_X, targetPos.getX());
                entry.setUniqueProperty(TARGET_Y, targetPos.getY());
                entry.setUniqueProperty(TARGET_Z, targetPos.getZ());

                // Initialize first wave with the target block
                Set<BlockPos> firstWave = new HashSet<>();
                firstWave.add(targetPos);
                entry.setUniqueProperty(CURRENT_WAVE_BLOCKS, serializeBlockPositions(firstWave));

                if (entity.level() instanceof ServerLevel serverLevel) {
                    // Play initial sound and effect
                    serverLevel.playSound(null, targetPos, SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 0.8f);
                }
            }
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        if (enabled) {
            try {
                executeWaveDecay(player, serverLevel, entry);
            } catch (Exception e) {
                BanditsQuirkLibForge.LOGGER.error("Error executing wave decay: ", e);
            }
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {

    }

    private void executeWaveDecay(ServerPlayer player, ServerLevel level, AbilityInstance entry) {
        // Get current state
        float waveTimer = entry.getProperty(WAVE_TIMER);
        int blocksDecayed = entry.getProperty(BLOCKS_DECAYED);
        int currentWave = entry.getProperty(CURRENT_WAVE);

        // Apply quirk factor scaling
        double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
        int effectiveMaxBlocks = entry.getProperty(MAX_BLOCKS) + (int)(quirkFactor * QUIRK_BLOCKS_MULTIPLIER * entry.getProperty(MAX_BLOCKS));
        int effectiveIntensity = Math.min(5, entry.getProperty(BASE_INTENSITY) + (int)(quirkFactor * QUIRK_INTENSITY_MULTIPLIER));
        float effectiveSpreadSpeed = entry.getProperty(SPREAD_SPEED) * (1.0f - (float)(quirkFactor * QUIRK_SPEED_MULTIPLIER));

        // Stop if we've hit the block limit
        if (blocksDecayed >= effectiveMaxBlocks) return;

        // Update wave timer
        waveTimer += 1.0F;
        entry.setUniqueProperty(WAVE_TIMER, waveTimer);

        // Check if it's time for the next wave
        if (waveTimer >= effectiveSpreadSpeed) {
            processCurrentWave(level, entry, effectiveIntensity, effectiveMaxBlocks);
            entry.setUniqueProperty(WAVE_TIMER, 0.0F);
            entry.setUniqueProperty(CURRENT_WAVE, currentWave + 1);
        }
    }

    private void processCurrentWave(ServerLevel level, AbilityInstance entry, int intensity, int maxBlocks) {
        Set<BlockPos> currentWaveBlocks = parseBlockPositions(entry.getProperty(CURRENT_WAVE_BLOCKS));
        Set<BlockPos> processedBlocks = parseBlockPositions(entry.getProperty(PROCESSED_BLOCKS));
        int blocksDecayed = entry.getProperty(BLOCKS_DECAYED);
        String decayType = entry.getProperty(DECAY_TYPE);

        if (currentWaveBlocks.isEmpty() || blocksDecayed >= maxBlocks) return;

        Set<BlockPos> nextWaveBlocks = new HashSet<>();
        int blocksProcessedThisWave = 0;

        // Process current wave blocks
        for (BlockPos pos : currentWaveBlocks) {
            if (blocksDecayed + blocksProcessedThisWave >= maxBlocks) break;

            // Destroy the block if it's decayable
            if (isDecayable(level.getBlockState(pos), intensity)) {
                level.destroyBlock(pos, false);

                // Only add particles 40% of the time to reduce lag
                if (level.random.nextFloat() < 0.2f) {
                    addDecayParticles(level, pos, intensity);
                }

                blocksProcessedThisWave++;

                // Play sound for each destroyed block
                if (blocksProcessedThisWave % 3 == 0) { // Don't spam sounds
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.PLAYERS, 0.2f, 1.0f + (blocksProcessedThisWave * 0.05f));
                }
            }

            // Add this position to processed blocks
            processedBlocks.add(pos);

            // Find connected blocks for next wave
            Set<BlockPos> connectedBlocks = findConnectedBlocks(level, pos, decayType, intensity, processedBlocks);
            nextWaveBlocks.addAll(connectedBlocks);
        }

        // Update properties
        entry.setUniqueProperty(BLOCKS_DECAYED, blocksDecayed + blocksProcessedThisWave);
        entry.setUniqueProperty(PROCESSED_BLOCKS, serializeBlockPositions(processedBlocks));
        entry.setUniqueProperty(CURRENT_WAVE_BLOCKS, serializeBlockPositions(nextWaveBlocks));
    }

    private Set<BlockPos> findConnectedBlocks(ServerLevel level, BlockPos center, String decayType,
                                              int intensity, Set<BlockPos> alreadyProcessed) {
        Set<BlockPos> connectedBlocks = new HashSet<>();

        switch (decayType.toLowerCase()) {
            case "horizontal":
                connectedBlocks.addAll(getHorizontalConnectedBlocks(level, center, intensity, alreadyProcessed));
                break;
            case "vertical":
                connectedBlocks.addAll(getVerticalConnectedBlocks(level, center, intensity, alreadyProcessed));
                break;
            case "circular":
            default:
                connectedBlocks.addAll(getCircularConnectedBlocks(level, center, intensity, alreadyProcessed));
                break;
        }

        return connectedBlocks;
    }

    private Set<BlockPos> getHorizontalConnectedBlocks(ServerLevel level, BlockPos center,
                                                       int intensity, Set<BlockPos> alreadyProcessed) {
        Set<BlockPos> blocks = new HashSet<>();

        // Check 4 horizontal directions (North, South, East, West)
        BlockPos[] directions = {
                center.north(),
                center.south(),
                center.east(),
                center.west()
        };

        for (BlockPos pos : directions) {
            if (!alreadyProcessed.contains(pos) && isDecayable(level.getBlockState(pos), intensity)) {
                blocks.add(pos);
            }
        }

        return blocks;
    }

    private Set<BlockPos> getVerticalConnectedBlocks(ServerLevel level, BlockPos center,
                                                     int intensity, Set<BlockPos> alreadyProcessed) {
        Set<BlockPos> blocks = new HashSet<>();

        // Check vertical directions (Up, Down) and horizontal at same level
        BlockPos[] directions = {
                center.above(),
                center.below(),
                center.north(),
                center.south(),
                center.east(),
                center.west()
        };

        for (BlockPos pos : directions) {
            if (!alreadyProcessed.contains(pos) && isDecayable(level.getBlockState(pos), intensity)) {
                blocks.add(pos);
            }
        }

        return blocks;
    }

    private Set<BlockPos> getCircularConnectedBlocks(ServerLevel level, BlockPos center,
                                                     int intensity, Set<BlockPos> alreadyProcessed) {
        Set<BlockPos> blocks = new HashSet<>();

        // Check all 6 directions (including up/down) for true 3D spread
        BlockPos[] directions = {
                center.north(),
                center.south(),
                center.east(),
                center.west(),
                center.above(),
                center.below()
        };

        for (BlockPos pos : directions) {
            if (!alreadyProcessed.contains(pos) && isDecayable(level.getBlockState(pos), intensity)) {
                blocks.add(pos);
            }
        }

        // Also check diagonal horizontal connections for more natural spread
        BlockPos[] diagonals = {
                center.north().east(),
                center.north().west(),
                center.south().east(),
                center.south().west()
        };

        for (BlockPos pos : diagonals) {
            if (!alreadyProcessed.contains(pos) && isDecayable(level.getBlockState(pos), intensity)) {
                blocks.add(pos);
            }
        }

        return blocks;
    }

    private boolean isDecayable(BlockState blockState, int intensity) {
        Block block = blockState.getBlock();

        // Always skip air
        if (block == Blocks.AIR || block == Blocks.BEDROCK) return false;

        // Get the block's destroy time with different tools
        float destroyTime = blockState.getDestroySpeed(null, null);

        // Level 0: Instant break blocks and shovel blocks
        if (destroyTime < 0.1f || isIntensityZero(blockState)) {
            return intensity >= 1;
        }

        // Level 1: Axe Materials
        if (isIntensityOne(blockState)) {
            return intensity >= 2;
        }

        // Level 2: Stone Pickaxe Materials
        if (isIntensityTwo(blockState)) {
            return intensity >= 3;
        }

        // Level 3: Iron Pickaxe Materials
        if (isIntensityThree(blockState)) {
            return intensity >= 4;
        }

        // Level 4: Diamond Pickaxe Materials
        if (isIntensityFour(blockState)) {
            return intensity >= 5;
        }

        // Default
        return intensity >= 3 && destroyTime > 0 && destroyTime < 50.0f;
    }

    // The first intensity is used for basic blocks, and wooden shovel blocks
    private boolean isIntensityZero(BlockState blockState) {
        return blockState.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL);
    }

    // This intensity is used for axe related blocks. Pretty much all wood
    private boolean isIntensityOne(BlockState blockState) {
        return blockState.is(BlockTags.MINEABLE_WITH_AXE);
    }

    // Intensity for stone pickaxes and below
    private boolean isIntensityTwo(BlockState blockState) {
        return blockState.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) &&
                blockState.is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL) &&
                !blockState.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL) &&
                !blockState.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL);
    }

    // Intensity for iron pickaxes and below
    private boolean isIntensityThree(BlockState blockState) {
        return blockState.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) &&
                (blockState.is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL) ||
                blockState.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL)) &&
                !blockState.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL);
    }

    // Intensity for diamond pickaxes and below
    private boolean isIntensityFour(BlockState blockState) {
        return blockState.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) &&
                (blockState.is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL) ||
                        blockState.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL) ||
                blockState.is(net.minecraft.tags.BlockTags.NEEDS_DIAMOND_TOOL));
    }

    private BlockPos findTargetBlock(ServerPlayer player, float range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookVec.scale(range));

        BlockHitResult result = player.level().clip(new net.minecraft.world.level.ClipContext(
                eyePos, endPos,
                net.minecraft.world.level.ClipContext.Block.OUTLINE,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                player
        ));

        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getBlockPos();
        }

        return null;
    }

    private void addDecayParticles(ServerLevel level, BlockPos pos, int intensity) {
        // Main decay effect
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                2 + intensity, 0.3, 0.3, 0.3, 0.05);

        // Intensity-based additional effects
        if (intensity >= 2) {
            level.sendParticles(ParticleTypes.ASH,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    2, 0.2, 0.2, 0.2, 0.02);
        }

        if (intensity >= 3) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1, 0.1, 0.1, 0.1, 0.01);
        }
    }

    private void damageHeldItem(ServerPlayer player, int damage) {
        ItemStack mainHand = player.getMainHandItem();

        if (!mainHand.isEmpty() && mainHand.isDamageableItem()) {
            double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
            int totalDamage = damage + (int)(damage * quirkFactor * 0.5);

            mainHand.hurtAndBreak(totalDamage, player, (p) -> {
                p.broadcastBreakEvent(net.minecraft.world.InteractionHand.MAIN_HAND);
            });
        }
    }

    // Utility methods for serializing/deserializing BlockPos sets
    private Set<BlockPos> parseBlockPositions(String positions) {
        Set<BlockPos> result = new HashSet<>();
        if (positions != null && !positions.isEmpty()) {
            String[] parts = positions.split(";");
            for (String part : parts) {
                if (!part.trim().isEmpty()) {
                    String[] coords = part.trim().split(",");
                    if (coords.length == 3) {
                        try {
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            int z = Integer.parseInt(coords[2]);
                            result.add(new BlockPos(x, y, z));
                        } catch (NumberFormatException e) {
                            // Skip invalid positions
                        }
                    }
                }
            }
        }
        return result;
    }

    private String serializeBlockPositions(Set<BlockPos> positions) {
        return positions.stream()
                .map(pos -> pos.getX() + "," + pos.getY() + "," + pos.getZ())
                .collect(Collectors.joining(";"));
    }

    @Override
    public String getDocumentationDescription() {
        return "Creates a wave of destruction that spreads from a target block to connected blocks. " +
                "Supports horizontal (4-directional), vertical (6-directional), and circular (8-directional) spread patterns. " +
                "Intensity determines tool requirements: 1=instant break, 2=shovel materials, 3=wood pickaxe materials, " +
                "4=iron pickaxe materials, 5=diamond pickaxe materials. " +
                "Quirk factor increases intensity, block count, and spread speed for more devastating wave effects. " +
                "The wave spreads outward one layer at a time, only affecting blocks connected to previously destroyed blocks.";
    }

    static {
        CURRENT_WAVE = (new IntegerProperty("current_wave")).sync(SyncType.NONE).disablePersistence();
        BLOCKS_DECAYED = (new IntegerProperty("blocks_decayed")).sync(SyncType.NONE).disablePersistence();
        TARGET_X = (new IntegerProperty("target_x")).sync(SyncType.NONE).disablePersistence();
        TARGET_Y = (new IntegerProperty("target_y")).sync(SyncType.NONE).disablePersistence();
        TARGET_Z = (new IntegerProperty("target_z")).sync(SyncType.NONE).disablePersistence();
        CURRENT_WAVE_BLOCKS = (new StringProperty("current_wave_blocks")).sync(SyncType.NONE).disablePersistence();
        PROCESSED_BLOCKS = (new StringProperty("processed_blocks")).sync(SyncType.NONE).disablePersistence();
        WAVE_TIMER = (new FloatProperty("wave_timer")).sync(SyncType.NONE).disablePersistence();
    }
} 