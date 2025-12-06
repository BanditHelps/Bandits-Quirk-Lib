package com.github.b4ndithelps.forge.abilities.frost;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.blocks.ModBlocks;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import com.github.b4ndithelps.forge.systems.TempHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("removal")
public class IcyTouchAbility extends Ability {

    private static final Direction[] SPREAD_DIRECTIONS = Direction.values();
    private static final float TEMP_DROP_PER_NEW_BLOCK = 0.12F;

    public static final PalladiumProperty<Float> TOUCH_RANGE =
            new FloatProperty("touch_range").configurable("Maximum distance to pick a target block");
    public static final PalladiumProperty<Integer> MAX_SPREAD_RADIUS =
            new IntegerProperty("max_spread_radius").configurable("Maximum radius (in blocks) that the spread can reach from the origin");
    public static final PalladiumProperty<Integer> SPREAD_ATTEMPTS =
            new IntegerProperty("spread_attempts").configurable("Number of frontier blocks processed per pulse");
    public static final PalladiumProperty<Integer> SPREAD_INTERVAL =
            new IntegerProperty("spread_interval").configurable("Ticks between each branching pulse before quirk scaling");
    public static final PalladiumProperty<Integer> BASE_CONVERT_COUNT =
            new IntegerProperty("base_convert_count").configurable("Baseline number of blocks converted per cast");
    public static final PalladiumProperty<Integer> CONVERTS_PER_QUIRK =
            new IntegerProperty("convert_per_quirk").configurable("Additional blocks converted per quirk factor point");
    public static final PalladiumProperty<Integer> ICE_DURATION =
            new IntegerProperty("ice_duration").configurable("Base lifetime in ticks before the ice reverts");
    public static final PalladiumProperty<Integer> QUIRK_DURATION_BONUS =
            new IntegerProperty("quirk_duration_bonus").configurable("Extra lifetime (ticks) per point of quirk factor");

    private static final SpreadTracker SPREAD_TRACKER = new SpreadTracker();
    private static final ConversionTracker CONVERSION_TRACKER = new ConversionTracker();

    public IcyTouchAbility() {
        this.withProperty(TOUCH_RANGE, 6.0F)
                .withProperty(MAX_SPREAD_RADIUS, 5)
                .withProperty(SPREAD_ATTEMPTS, 4)
                .withProperty(SPREAD_INTERVAL, 3)
                .withProperty(BASE_CONVERT_COUNT, 24)
                .withProperty(CONVERTS_PER_QUIRK, 8)
                .withProperty(ICE_DURATION, 160)
                .withProperty(QUIRK_DURATION_BONUS, 80);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (entity instanceof Player player && TempHelper.isOverheated(player)) {
            return;
        }

        double quirkFactor = resolveQuirkFactor(entity);
        int lifetime = computeLifetimeTicks(entry, quirkFactor);
        int maxBlocks = computeMaxBlocks(entry, quirkFactor);
        int radius = Math.max(1, entry.getProperty(MAX_SPREAD_RADIUS));
        int attemptsPerPulse = Math.max(1, entry.getProperty(SPREAD_ATTEMPTS));
        int interval = computeSpreadInterval(entry, quirkFactor);

        double range = Math.max(1.0D, entry.getProperty(TOUCH_RANGE));
        BlockPos target = resolveTargetBlock(entity, range);
        if (target == null) {
            return;
        }
        BlockState state = serverLevel.getBlockState(target);
        if (state.isAir()) {
            return;
        }

        SPREAD_TRACKER.startCascade(entity, serverLevel, target.immutable(), lifetime, maxBlocks, radius, attemptsPerPulse, interval);
    }

    private double resolveQuirkFactor(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return Math.max(0.0, QuirkFactorHelper.getQuirkFactor(player));
        }
        return 0.0;
    }

    private int computeLifetimeTicks(AbilityInstance entry, double quirkFactor) {
        int base = Math.max(40, entry.getProperty(ICE_DURATION));
        int perQuirk = Math.max(0, entry.getProperty(QUIRK_DURATION_BONUS));
        int bonus = (int) Math.round(perQuirk * quirkFactor);
        return Math.max(20, base + bonus);
    }

    private int computeMaxBlocks(AbilityInstance entry, double quirkFactor) {
        int base = Math.max(1, entry.getProperty(BASE_CONVERT_COUNT));
        int perQuirk = Math.max(0, entry.getProperty(CONVERTS_PER_QUIRK));
        int bonus = (int) Math.round(perQuirk * quirkFactor);
        return Math.max(1, base + bonus);
    }

    private int computeSpreadInterval(AbilityInstance entry, double quirkFactor) {
        int configured = Math.max(1, entry.getProperty(SPREAD_INTERVAL));
        double speedMultiplier = 1.0 + (quirkFactor * 2.5);
        return Math.max(1, (int) Math.round(configured / speedMultiplier));
    }

    private BlockPos resolveTargetBlock(LivingEntity entity, double range) {
        Vec3 direction = entity.getLookAngle();
        if (direction.lengthSqr() < 1.0e-6D) {
            return null;
        }
        Vec3 start = entity.getEyePosition();
        Vec3 end = start.add(direction.normalize().scale(range));
        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity);
        BlockHitResult result = entity.level().clip(context);
        if (result == null || result.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return result.getBlockPos();
    }

    @Override
    public String getDocumentationDescription() {
        return "Tap the key to flash-freeze the block you're looking at, then watch the cold radiate outward in even waves. "
                + "The cascade converts a quirk-scaled number of solid blocks into permafrost ice before the chill wears off.";
    }

    private static class SpreadTracker {
        private final Map<ServerLevel, List<ActiveCascade>> cascades = new HashMap<>();

        void startCascade(LivingEntity owner, ServerLevel level, BlockPos origin, int lifetimeTicks,
                          int maxBlocks, int maxRadius, int spreadAttempts, int interval) {
            if (maxBlocks <= 0) {
                return;
            }
            ConvertResult result = CONVERSION_TRACKER.freezeBlock(level, origin, lifetimeTicks);
            if (result == ConvertResult.FAILED) {
                return;
            }
            ActiveCascade cascade = new ActiveCascade(owner.getUUID(), origin, lifetimeTicks, maxBlocks,
                    Math.max(1, maxRadius) * Math.max(1, maxRadius), Math.max(1, spreadAttempts), Math.max(1, interval));
            cascade.initialize(level, origin, result);
            cascades.computeIfAbsent(level, key -> new ArrayList<>()).add(cascade);
        }

        void tick(ServerLevel level) {
            List<ActiveCascade> active = cascades.get(level);
            if (active == null || active.isEmpty()) {
                return;
            }
            Iterator<ActiveCascade> iterator = active.iterator();
            while (iterator.hasNext()) {
                ActiveCascade cascade = iterator.next();
                if (!cascade.tick(level)) {
                    iterator.remove();
                }
            }
            if (active.isEmpty()) {
                cascades.remove(level);
            }
        }
    }

    private static class ActiveCascade {
        private final UUID ownerId;
        private final BlockPos origin;
        private final int lifetimeTicks;
        private final int maxBlocks;
        private final int radiusSq;
        private final int spreadAttempts;
        private final int spreadInterval;
        private final Set<BlockPos> visited = new HashSet<>();
        private final ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        private int convertedBlocks = 0;
        private int ticksUntilNextSpread = 0;

        private ActiveCascade(UUID ownerId, BlockPos origin, int lifetimeTicks, int maxBlocks, int radiusSq,
                              int spreadAttempts, int spreadInterval) {
            this.ownerId = ownerId;
            this.origin = origin;
            this.lifetimeTicks = lifetimeTicks;
            this.maxBlocks = maxBlocks;
            this.radiusSq = radiusSq;
            this.spreadAttempts = spreadAttempts;
            this.spreadInterval = spreadInterval;
        }

        void initialize(ServerLevel level, BlockPos origin, ConvertResult result) {
            visited.add(origin);
            handleConversionResult(level, origin, result);
            enqueueNeighbors(level, origin);
        }

        boolean tick(ServerLevel level) {
            if (convertedBlocks >= maxBlocks || frontier.isEmpty()) {
                return false;
            }

            if (ticksUntilNextSpread > 0) {
                ticksUntilNextSpread--;
                return true;
            }

            int processed = 0;
            while (processed < spreadAttempts && convertedBlocks < maxBlocks && !frontier.isEmpty()) {
                BlockPos next = frontier.poll();
                convertNode(level, next);
                processed++;
            }

            ticksUntilNextSpread = spreadInterval;
            return convertedBlocks < maxBlocks && !frontier.isEmpty();
        }

        private void convertNode(ServerLevel level, BlockPos pos) {
            if (!level.hasChunkAt(pos)) {
                return;
            }
            if (pos.distSqr(origin) > radiusSq) {
                return;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                return;
            }

            ConvertResult result = CONVERSION_TRACKER.freezeBlock(level, pos, lifetimeTicks);
            if (result == ConvertResult.FAILED) {
                return;
            }

            handleConversionResult(level, pos, result);
            enqueueNeighbors(level, pos);
        }

        private void enqueueNeighbors(ServerLevel level, BlockPos from) {
            for (Direction direction : SPREAD_DIRECTIONS) {
                BlockPos candidate = from.relative(direction).immutable();
                if (visited.contains(candidate)) {
                    continue;
                }
                if (!level.hasChunkAt(candidate)) {
                    continue;
                }
                if (candidate.distSqr(origin) > radiusSq) {
                    continue;
                }
                BlockState state = level.getBlockState(candidate);
                if (state.isAir()) {
                    continue;
                }
                visited.add(candidate);
                frontier.add(candidate);
            }
        }

        private void handleConversionResult(ServerLevel level, BlockPos pos, ConvertResult result) {
            if (result != ConvertResult.CREATED) {
                return;
            }
            convertedBlocks++;
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.7;
            double z = pos.getZ() + 0.5;
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 4, 0.15, 0.25, 0.15, 0.0);
            level.sendParticles(ParticleTypes.CLOUD, x, y - 0.2, z, 1, 0.05, 0.05, 0.05, 0.0);

            Player owner = level.getPlayerByUUID(ownerId);
            if (owner != null) {
                TempHelper.lowerInnerTemp(owner, TEMP_DROP_PER_NEW_BLOCK);
            }
        }
    }

    private static class ConversionTracker {
        private final Map<ServerLevel, Map<BlockPos, TrackedConversion>> activeConversions = new HashMap<>();

        ConvertResult freezeBlock(ServerLevel level, BlockPos pos, int lifetimeTicks) {
            if (!level.hasChunkAt(pos)) {
                return ConvertResult.FAILED;
            }

            Map<BlockPos, TrackedConversion> conversions =
                    activeConversions.computeIfAbsent(level, key -> new HashMap<>());
            TrackedConversion existing = conversions.get(pos);
            long expiry = level.getGameTime() + lifetimeTicks;

            if (existing != null) {
                existing.expiryTick = expiry;
                return ConvertResult.EXTENDED;
            }

            BlockState current = level.getBlockState(pos);
            if (!canConvert(current, level, pos)) {
                return ConvertResult.FAILED;
            }

            BlockPos immutable = pos.immutable();
            conversions.put(immutable, new TrackedConversion(immutable, current, expiry));
            level.setBlockAndUpdate(immutable, ModBlocks.PERMAFROST_ICE.get().defaultBlockState());
            return ConvertResult.CREATED;
        }

        boolean isTracked(ServerLevel level, BlockPos pos) {
            Map<BlockPos, TrackedConversion> conversions = activeConversions.get(level);
            return conversions != null && conversions.containsKey(pos);
        }

        void tick(ServerLevel level) {
            Map<BlockPos, TrackedConversion> conversions = activeConversions.get(level);
            if (conversions == null || conversions.isEmpty()) {
                return;
            }

            long now = level.getGameTime();
            Iterator<Map.Entry<BlockPos, TrackedConversion>> iterator = conversions.entrySet().iterator();
            while (iterator.hasNext()) {
                TrackedConversion conversion = iterator.next().getValue();
                if (conversion.expiryTick > now) {
                    continue;
                }
                if (!level.hasChunkAt(conversion.pos)) {
                    continue;
                }
                BlockState current = level.getBlockState(conversion.pos);
                if (current.is(ModBlocks.PERMAFROST_ICE.get())) {
                    BlockState original = conversion.originalState;
                    if (original != null && original.canSurvive(level, conversion.pos)) {
                        level.setBlockAndUpdate(conversion.pos, original);
                    } else {
                        level.setBlockAndUpdate(conversion.pos, Blocks.AIR.defaultBlockState());
                    }
                }
                iterator.remove();
            }

            if (conversions.isEmpty()) {
                activeConversions.remove(level);
            }
        }

        private boolean canConvert(BlockState state, ServerLevel level, BlockPos pos) {
            if (state.isAir()) {
                return false;
            }
            if (state.is(ModBlocks.PERMAFROST_ICE.get())) {
                return false;
            }
            if (state.hasBlockEntity()) {
                return false;
            }
            return state.getDestroySpeed(level, pos) >= 0.0F;
        }
    }

    private static class TrackedConversion {
        private final BlockPos pos;
        private final BlockState originalState;
        private long expiryTick;

        private TrackedConversion(BlockPos pos, BlockState originalState, long expiryTick) {
            this.pos = pos;
            this.originalState = originalState;
            this.expiryTick = expiryTick;
        }
    }

    private enum ConvertResult {
        FAILED,
        CREATED,
        EXTENDED
    }

    @Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
    public static class TemporaryIceTicker {
        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
                return;
            }
            for (ServerLevel level : event.getServer().getAllLevels()) {
                CONVERSION_TRACKER.tick(level);
                SPREAD_TRACKER.tick(level);
            }
        }
    }
}

