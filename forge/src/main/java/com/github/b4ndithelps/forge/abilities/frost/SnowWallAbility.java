package com.github.b4ndithelps.forge.abilities.frost;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import com.github.b4ndithelps.forge.systems.TempHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.BooleanProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.SyncType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class SnowWallAbility extends Ability {
    public static final PalladiumProperty<Integer> PATH_LENGTH =
            new IntegerProperty("path_length").configurable("Blocks of distance the trail can extend");
    public static final PalladiumProperty<Integer> PATH_LAYERS =
            new IntegerProperty("path_layers").configurable("Blocks the trail advances per pulse");
    public static final PalladiumProperty<Integer> PATH_INTERVAL =
            new IntegerProperty("path_interval").configurable("Ticks between each path placement pulse");

    public static final PalladiumProperty<Integer> WALL_WIDTH =
            new IntegerProperty("wall_width").configurable("Half-width of the powdered snow wall");
    public static final PalladiumProperty<Integer> WALL_HEIGHT =
            new IntegerProperty("wall_height").configurable("Vertical height of the powdered snow wall");
    public static final PalladiumProperty<Integer> WALL_THICKNESS =
            new IntegerProperty("wall_thickness").configurable("Base depth of the wall before quirk scaling");
    public static final PalladiumProperty<Integer> WALL_LIFETIME =
            new IntegerProperty("wall_lifetime").configurable("Lifetime in ticks before the wall melts");

    private static final PalladiumProperty<Boolean> HAS_MARKER =
            new BooleanProperty("snow_wall_has_marker").sync(SyncType.NONE).disablePersistence();
    private static final PalladiumProperty<Float> MARKER_X =
            new FloatProperty("snow_wall_marker_x").sync(SyncType.NONE).disablePersistence();
    private static final PalladiumProperty<Float> MARKER_Y =
            new FloatProperty("snow_wall_marker_y").sync(SyncType.NONE).disablePersistence();
    private static final PalladiumProperty<Float> MARKER_Z =
            new FloatProperty("snow_wall_marker_z").sync(SyncType.NONE).disablePersistence();
    private static final PalladiumProperty<Float> MARKER_DIR_X =
            new FloatProperty("snow_wall_marker_dir_x").sync(SyncType.NONE).disablePersistence();
    private static final PalladiumProperty<Float> MARKER_DIR_Z =
            new FloatProperty("snow_wall_marker_dir_z").sync(SyncType.NONE).disablePersistence();
    private static final PalladiumProperty<Integer> CURRENT_STEPS =
            new IntegerProperty("snow_wall_trail_steps").sync(SyncType.NONE).disablePersistence();

    private static final double DIRECTION_EPSILON = 1.0e-4D;
    private static final int TRAIL_HALF_WIDTH = 1;
    private static final int SURFACE_SEARCH_RANGE = 5;
    private static final float FALLBACK_WALL_DISTANCE = 4.0F;
    private static final int WALL_BASE_DELAY = 6;
    private static final int WALL_VERTICAL_INCREMENT = 4;
    private static final int WALL_LATERAL_INCREMENT = 2;
    private static final int WALL_DEPTH_INCREMENT = 3;

    private static final float TEMP_DROP_PER_TRAIL_STEP = 0.15F;
    private static final float TEMP_WALL_SPIKE_DROP = 2.0F;

    public SnowWallAbility() {
        this.withProperty(PATH_LENGTH, 6)
                .withProperty(PATH_LAYERS, 1)
                .withProperty(PATH_INTERVAL, 3)
                .withProperty(WALL_WIDTH, 3)
                .withProperty(WALL_HEIGHT, 4)
                .withProperty(WALL_THICKNESS, 1)
                .withProperty(WALL_LIFETIME, 200);
    }

    @Override
    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(HAS_MARKER, false);
        manager.register(MARKER_X, 0.0F);
        manager.register(MARKER_Y, 0.0F);
        manager.register(MARKER_Z, 0.0F);
        manager.register(MARKER_DIR_X, 0.0F);
        manager.register(MARKER_DIR_Z, 0.0F);
        manager.register(CURRENT_STEPS, 0);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        resetTrailState(entity, entry);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof Player player && TempHelper.isOverheated(player)) {
            resetTrailState(entity, entry);
            return;
        }

        advanceSnowTrail(serverLevel, entity, entry);
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            resetTrailState(entity, entry);
            return;
        }
        if (entity instanceof Player player && TempHelper.isOverheated(player)) {
            resetTrailState(entity, entry);
            return;
        }
        buildPowderSnowWall(serverLevel, entity, entry);
        resetTrailState(entity, entry);
    }

    private void advanceSnowTrail(ServerLevel level, LivingEntity entity, AbilityInstance entry) {
        int interval = Math.max(1, entry.getProperty(PATH_INTERVAL));
        long offset = Math.floorMod(level.getGameTime() + entity.getId(), interval);
        if (offset != 0) {
            return;
        }

        int maxSteps = Math.max(1, entry.getProperty(PATH_LENGTH));
        int currentSteps = entry.getProperty(CURRENT_STEPS);
        if (currentSteps >= maxSteps) {
            return;
        }

        Vec3 dir = resolveHorizontalDirection(entity, entry);
        if (dir == null) {
            return;
        }
        double advance = Mth.clamp(entry.getProperty(PATH_LAYERS), 1, 6);
        Vec3 marker = getMarkerPosition(entity, entry);
        Vec3 nextMarker = marker.add(dir.scale(advance));

        BlockPos anchorPlacement = computeTrailPlacement(level, nextMarker, SURFACE_SEARCH_RANGE);
        if (anchorPlacement == null) {
            return;
        }

        boolean placedSnow = false;
        placeSingleLayer(level, anchorPlacement);
        placedSnow = true;
        if (TRAIL_HALF_WIDTH > 0) {
            Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
            for (int tempOffset = -TRAIL_HALF_WIDTH; tempOffset <= TRAIL_HALF_WIDTH; tempOffset++) {
                if (tempOffset == 0) {
                    continue;
                }
                Vec3 lateral = nextMarker.add(side.scale(tempOffset));
                BlockPos placement = computeTrailPlacement(level, lateral, SURFACE_SEARCH_RANGE);
                if (placement != null) {
                    placeSingleLayer(level, placement);
                    placedSnow = true;
                }
            }
        }

        Vec3 anchorCenter = Vec3.atCenterOf(anchorPlacement);
        entry.setUniqueProperty(MARKER_X, (float) anchorCenter.x);
        entry.setUniqueProperty(MARKER_Y, (float) anchorCenter.y);
        entry.setUniqueProperty(MARKER_Z, (float) anchorCenter.z);
        entry.setUniqueProperty(MARKER_DIR_X, (float) dir.x);
        entry.setUniqueProperty(MARKER_DIR_Z, (float) dir.z);
        entry.setUniqueProperty(CURRENT_STEPS, currentSteps + 1);
        entry.setUniqueProperty(HAS_MARKER, true);

        if (placedSnow && entity instanceof Player player) {
            TempHelper.lowerInnerTemp(player, TEMP_DROP_PER_TRAIL_STEP);
        }
    }

    private Vec3 getMarkerPosition(LivingEntity entity, AbilityInstance entry) {
        if (!entry.getProperty(HAS_MARKER)) {
            return entity.position();
        }
        return new Vec3(entry.getProperty(MARKER_X), entry.getProperty(MARKER_Y), entry.getProperty(MARKER_Z));
    }

    private Vec3 resolveHorizontalDirection(LivingEntity entity, AbilityInstance entry) {
        Vec3 forward = entity.getLookAngle();
        Vec3 horizontal = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontal.lengthSqr() < DIRECTION_EPSILON) {
            Vec3 stored = getStoredDirection(entry);
            if (stored != null) {
                return stored;
            }
            float yaw = entity.getYRot();
            horizontal = Vec3.directionFromRotation(0.0F, yaw);
        }
        if (horizontal.lengthSqr() < DIRECTION_EPSILON) {
            return null;
        }
        return horizontal.normalize();
    }

    private Vec3 getStoredDirection(AbilityInstance entry) {
        double dx = entry.getProperty(MARKER_DIR_X);
        double dz = entry.getProperty(MARKER_DIR_Z);
        Vec3 stored = new Vec3(dx, 0.0, dz);
        if (stored.lengthSqr() < DIRECTION_EPSILON) {
            return null;
        }
        return stored.normalize();
    }

    private BlockPos computeTrailPlacement(ServerLevel level, Vec3 sample, int searchRange) {
        BlockPos candidate = BlockPos.containing(sample.x, sample.y, sample.z);
        BlockPos surface = findSurface(level, candidate, searchRange);
        if (surface == null) {
            return null;
        }
        BlockPos placement = surface.above();
        if (placement.getY() >= level.getMaxBuildHeight() || placement.getY() <= level.getMinBuildHeight()) {
            return null;
        }
        return placement;
    }

    private void placeSingleLayer(ServerLevel level, BlockPos pos) {
        BlockState existing = level.getBlockState(pos);
        if (existing.is(Blocks.SNOW) || existing.is(Blocks.SNOW_BLOCK)) {
            return;
        }
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return;
        }
        BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
        if (!snow.canSurvive(level, pos)) {
            return;
        }
        level.setBlockAndUpdate(pos, snow);
    }

    private void resetTrailState(LivingEntity entity, AbilityInstance entry) {
        entry.setUniqueProperty(HAS_MARKER, false);
        entry.setUniqueProperty(CURRENT_STEPS, 0);
        entry.setUniqueProperty(MARKER_X, (float) entity.getX());
        entry.setUniqueProperty(MARKER_Y, (float) entity.getY());
        entry.setUniqueProperty(MARKER_Z, (float) entity.getZ());
        entry.setUniqueProperty(MARKER_DIR_X, 0.0F);
        entry.setUniqueProperty(MARKER_DIR_Z, 0.0F);
    }

    private void buildPowderSnowWall(ServerLevel level, LivingEntity entity, AbilityInstance entry) {
        Vec3 dir = getStoredDirection(entry);
        if (dir == null) {
            Vec3 fallback = resolveHorizontalDirection(entity, entry);
            dir = fallback == null ? new Vec3(0.0, 0.0, 1.0) : fallback;
        }
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
        Vec3 centerVec = entry.getProperty(HAS_MARKER)
                ? getMarkerPosition(entity, entry)
                : entity.position().add(dir.scale(FALLBACK_WALL_DISTANCE));

        int halfWidth = Math.max(1, entry.getProperty(WALL_WIDTH));
        int height = Math.max(1, entry.getProperty(WALL_HEIGHT));
        int baseThickness = Math.max(1, entry.getProperty(WALL_THICKNESS));
        int lifetime = Math.max(40, entry.getProperty(WALL_LIFETIME));
        RandomSource random = level.getRandom();

        double quirkFactor = entity instanceof ServerPlayer player
                ? Math.max(0.0, QuirkFactorHelper.getQuirkFactor(player))
                : 0.0;
        int thickness = (int) Math.max(1, baseThickness + quirkFactor);

        boolean builtWall = false;
        for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
            for (int depth = 0; depth < thickness; depth++) {
                Vec3 offsetVec = centerVec.add(side.scale(lateral)).add(dir.scale(depth));
                BlockPos baseCandidate = BlockPos.containing(offsetVec.x, centerVec.y, offsetVec.z);
                BlockPos surface = findSurface(level, baseCandidate, SURFACE_SEARCH_RANGE);
                if (surface == null) {
                    continue;
                }
                BlockPos top = surface.above();
                for (int h = 0; h < height; h++) {
                    BlockPos place = top.above(h);
                    int delay = WALL_BASE_DELAY
                            + h * WALL_VERTICAL_INCREMENT
                            + Math.abs(lateral) * WALL_LATERAL_INCREMENT
                            + depth * WALL_DEPTH_INCREMENT
                            + random.nextInt(4);
                    schedulePowderSnowGrowth(level, place, delay, lifetime + random.nextInt(20));
                    builtWall = true;
                }
            }
        }

        if (builtWall && entity instanceof Player player) {
            TempHelper.lowerInnerTemp(player, TEMP_WALL_SPIKE_DROP);
        }
    }

    private BlockPos findSurface(ServerLevel level, BlockPos start, int searchRange) {
        BlockPos.MutableBlockPos cursor = start.mutable();
        for (int i = 0; i < searchRange; i++) {
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.getCollisionShape(level, cursor).isEmpty()) {
                return cursor.immutable();
            }
            cursor.move(Direction.DOWN);
            if (cursor.getY() < level.getMinBuildHeight()) {
                break;
            }
        }
        return null;
    }

    private void schedulePowderSnowGrowth(ServerLevel level, BlockPos pos, int delayTicks, int lifetimeTicks) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return;
        }
        WallTracker.enqueueGrowth(level, pos, delayTicks, lifetimeTicks);
    }

    @Override
    public String getDocumentationDescription() {
        return "Hold to paint a path of single-layer snow piles along your gaze, marking where the wall should form. "
                + "Release to raise a short-lived powdered snow wall at the trail's end, with thickness scaling off quirk factor and growing upward block by block.";
    }

    private record TrackedBlock(BlockPos pos, long expiryTick) {}

    private static class WallTracker {
        private static final Map<ServerLevel, List<TrackedBlock>> ACTIVE_BLOCKS = new HashMap<>();
        private static final Map<ServerLevel, List<PendingPlacement>> PENDING_BLOCKS = new HashMap<>();

        private static void track(ServerLevel level, BlockPos pos, int lifetimeTicks) {
            ACTIVE_BLOCKS
                    .computeIfAbsent(level, key -> new ArrayList<>())
                    .add(new TrackedBlock(pos.immutable(), level.getGameTime() + lifetimeTicks));
        }

        private static void enqueueGrowth(ServerLevel level, BlockPos pos, int delayTicks, int lifetimeTicks) {
            PENDING_BLOCKS
                    .computeIfAbsent(level, key -> new ArrayList<>())
                    .add(new PendingPlacement(pos.immutable(), Math.max(0, delayTicks), Math.max(1, lifetimeTicks)));
        }

        private static void tick(ServerLevel level) {
            processPending(level);
            cleanup(level);
        }

        private static void processPending(ServerLevel level) {
            List<PendingPlacement> pending = PENDING_BLOCKS.get(level);
            if (pending == null || pending.isEmpty()) {
                return;
            }
            pending.removeIf(entry -> {
                if (entry.ticksUntilPlace > 0) {
                    entry.ticksUntilPlace--;
                    return false;
                }
                if (canPlacePowderSnow(level, entry.pos)) {
                    level.setBlockAndUpdate(entry.pos, Blocks.POWDER_SNOW.defaultBlockState());
                    track(level, entry.pos, entry.lifetimeTicks);
                }
                return true;
            });
            if (pending.isEmpty()) {
                PENDING_BLOCKS.remove(level);
            }
        }

        private static boolean canPlacePowderSnow(ServerLevel level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
            return Blocks.POWDER_SNOW.defaultBlockState().canSurvive(level, pos);
        }

        private static void cleanup(ServerLevel level) {
            List<TrackedBlock> tracked = ACTIVE_BLOCKS.get(level);
            if (tracked == null || tracked.isEmpty()) {
                return;
            }
            long now = level.getGameTime();
            tracked.removeIf(block -> {
                if (block.expiryTick <= now) {
                    if (level.getBlockState(block.pos).is(Blocks.POWDER_SNOW)) {
                        level.setBlockAndUpdate(block.pos, Blocks.AIR.defaultBlockState());
                    }
                    return true;
                }
                return false;
            });
            if (tracked.isEmpty()) {
                ACTIVE_BLOCKS.remove(level);
            }
        }

        private static class PendingPlacement {
            private final BlockPos pos;
            private int ticksUntilPlace;
            private final int lifetimeTicks;

            private PendingPlacement(BlockPos pos, int ticksUntilPlace, int lifetimeTicks) {
                this.pos = pos;
                this.ticksUntilPlace = ticksUntilPlace;
                this.lifetimeTicks = lifetimeTicks;
            }
        }
    }

    @Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
    public static class WallCleanupTicker {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
                return;
            }
            for (ServerLevel level : event.getServer().getAllLevels()) {
                WallTracker.tick(level);
            }
        }
    }
}
