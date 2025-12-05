package com.github.b4ndithelps.forge.abilities.frost;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
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
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("removal")
public class SnowWallAbility extends Ability {
    public static final PalladiumProperty<Integer> PATH_LENGTH =
            new IntegerProperty("path_length").configurable("Blocks ahead to coat in snow each pulse");
    public static final PalladiumProperty<Integer> PATH_WIDTH =
            new IntegerProperty("path_width").configurable("Half-width (in blocks) of the snow path");
    public static final PalladiumProperty<Integer> PATH_LAYERS =
            new IntegerProperty("path_layers").configurable("Number of snow layers to attempt per placement");
    public static final PalladiumProperty<Integer> PATH_INTERVAL =
            new IntegerProperty("path_interval").configurable("Ticks between each path placement pulse");
    public static final PalladiumProperty<Integer> PATH_HEIGHT_CAP =
            new IntegerProperty("path_height_cap").configurable("Maximum block height for built-up snow");

    public static final PalladiumProperty<Integer> WALL_WIDTH =
            new IntegerProperty("wall_width").configurable("Half-width of the final powdered snow wall");
    public static final PalladiumProperty<Integer> WALL_HEIGHT =
            new IntegerProperty("wall_height").configurable("Vertical height of the powdered snow wall");
    public static final PalladiumProperty<Integer> WALL_THICKNESS =
            new IntegerProperty("wall_thickness").configurable("Thickness (in blocks) of the wall");
    public static final PalladiumProperty<Float> WALL_DISTANCE =
            new FloatProperty("wall_distance").configurable("Distance in front of the caster where the wall spawns");
    public static final PalladiumProperty<Integer> WALL_LIFETIME =
            new IntegerProperty("wall_lifetime").configurable("Lifetime in ticks before the wall melts");

    public SnowWallAbility() {
        this.withProperty(PATH_LENGTH, 6)
                .withProperty(PATH_WIDTH, 2)
                .withProperty(PATH_LAYERS, 4)
                .withProperty(PATH_INTERVAL, 3)
                .withProperty(PATH_HEIGHT_CAP, 4)
                .withProperty(WALL_WIDTH, 3)
                .withProperty(WALL_HEIGHT, 4)
                .withProperty(WALL_THICKNESS, 1)
                .withProperty(WALL_DISTANCE, 4.0F)
                .withProperty(WALL_LIFETIME, 200);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int interval = Math.max(1, entry.getProperty(PATH_INTERVAL));
        long offset = (serverLevel.getGameTime() + entity.getId()) % interval;
        if (offset != 0) {
            return;
        }
        laySnowPath(serverLevel, entity, entry);
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        buildPowderSnowWall(serverLevel, entity, entry);
    }

    private void laySnowPath(ServerLevel level, LivingEntity entity, AbilityInstance entry) {
        Vec3 forward = entity.getLookAngle();
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0e-4) {
            float yaw = entity.getYRot();
            horizontalForward = Vec3.directionFromRotation(0.0F, yaw);
        }
        if (horizontalForward.lengthSqr() < 1.0e-4) {
            return;
        }

        Vec3 dir = horizontalForward.normalize();
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
        int length = Math.max(1, entry.getProperty(PATH_LENGTH));
        int halfWidth = Math.max(0, entry.getProperty(PATH_WIDTH));
        int layers = Mth.clamp(entry.getProperty(PATH_LAYERS), 1, 8);
        int heightCap = Math.max(1, entry.getProperty(PATH_HEIGHT_CAP));

        Vec3 start = entity.position().add(0.0, -0.5, 0.0);
        for (int step = 0; step < length; step++) {
            Vec3 stepCenter = start.add(dir.scale(step));
            for (int offset = -halfWidth; offset <= halfWidth; offset++) {
                Vec3 lateral = stepCenter.add(side.scale(offset));
                BlockPos candidate = BlockPos.containing(lateral.x, lateral.y, lateral.z);
                BlockPos surface = findSurface(level, candidate, 6);
                if (surface == null) {
                    continue;
                }
                addSnowLayers(level, surface.above(), layers, heightCap);
            }
        }
    }

    private void buildPowderSnowWall(ServerLevel level, LivingEntity entity, AbilityInstance entry) {
        Vec3 forward = entity.getLookAngle();
        Vec3 flat = new Vec3(forward.x, 0.0D, forward.z);
        if (flat.lengthSqr() < 1.0e-4) {
            float yaw = entity.getYRot();
            flat = Vec3.directionFromRotation(0.0F, yaw);
        }
        if (flat.lengthSqr() < 1.0e-4) {
            flat = new Vec3(0.0, 0.0, 1.0);
        }

        Vec3 dir = flat.normalize();
        Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
        float distance = Math.max(1.0F, entry.getProperty(WALL_DISTANCE));
        Vec3 center = entity.position().add(dir.scale(distance));

        int halfWidth = Math.max(0, entry.getProperty(WALL_WIDTH));
        int height = Math.max(1, entry.getProperty(WALL_HEIGHT));
        int thickness = Math.max(1, entry.getProperty(WALL_THICKNESS));
        int lifetime = Math.max(40, entry.getProperty(WALL_LIFETIME));
        RandomSource random = level.getRandom();

        for (int lateral = -halfWidth; lateral <= halfWidth; lateral++) {
            for (int depth = 0; depth < thickness; depth++) {
                Vec3 offsetVec = center.add(side.scale(lateral)).add(dir.scale(depth));
                BlockPos baseCandidate = BlockPos.containing(offsetVec.x, entity.getY(), offsetVec.z);
                BlockPos surface = findSurface(level, baseCandidate, 4);
                if (surface == null) {
                    continue;
                }
                BlockPos top = surface.above();
                for (int h = 0; h < height; h++) {
                    BlockPos place = top.above(h);
                    placeTemporaryPowderSnow(level, place, lifetime + random.nextInt(20));
                }
            }
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

    private void addSnowLayers(ServerLevel level, BlockPos pos, int layersToAdd, int heightCap) {
        if (layersToAdd <= 0) {
            return;
        }
        BlockPos base = pos;
        int climbed = 0;
        while (climbed <= heightCap) {
            if (base.getY() >= level.getMaxBuildHeight()) {
                return;
            }
            BlockState state = level.getBlockState(base);

            if (state.isAir()) {
                placeSnowState(level, base, layersToAdd);
                return;
            }

            if (state.is(Blocks.SNOW)) {
                int existing = state.getValue(SnowLayerBlock.LAYERS);
                int total = existing + layersToAdd;
                if (total <= 8) {
                    level.setBlockAndUpdate(base, state.setValue(SnowLayerBlock.LAYERS, Math.min(8, total)));
                    return;
                } else {
                    level.setBlockAndUpdate(base, Blocks.SNOW_BLOCK.defaultBlockState());
                    layersToAdd = total - 8;
                    base = base.above();
                    climbed++;
                    continue;
                }
            }

            if (state.is(Blocks.SNOW_BLOCK)) {
                base = base.above();
                climbed++;
                continue;
            }

            if (state.canBeReplaced()) {
                placeSnowState(level, base, layersToAdd);
            }
            return;
        }
    }

    private void placeSnowState(ServerLevel level, BlockPos pos, int layers) {
        int clamped = Mth.clamp(layers, 1, 7);
        BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, clamped);
        if (snow.canSurvive(level, pos)) {
            level.setBlockAndUpdate(pos, snow);
        }
    }

    private void placeTemporaryPowderSnow(ServerLevel level, BlockPos pos, int lifetimeTicks) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) {
            return;
        }
        level.setBlockAndUpdate(pos, Blocks.POWDER_SNOW.defaultBlockState());
        WallTracker.track(level, pos, lifetimeTicks);
    }

    @Override
    public String getDocumentationDescription() {
        return "While held, trails snow in the direction you're facing. Upon release the stored cold snaps into a short-lived "
                + "powdered snow wall, trapping anything inside for the block lifetime.";
    }

    private record TrackedBlock(BlockPos pos, long expiryTick) {}

    private static class WallTracker {
        private static final Map<ServerLevel, List<TrackedBlock>> ACTIVE_BLOCKS = new HashMap<>();

        private static void track(ServerLevel level, BlockPos pos, int lifetimeTicks) {
            ACTIVE_BLOCKS
                    .computeIfAbsent(level, key -> new ArrayList<>())
                    .add(new TrackedBlock(pos.immutable(), level.getGameTime() + lifetimeTicks));
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
    }

    @Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
    public static class WallCleanupTicker {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
                return;
            }
            for (ServerLevel level : event.getServer().getAllLevels()) {
                WallTracker.cleanup(level);
            }
        }
    }
}
