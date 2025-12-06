package com.github.b4ndithelps.forge.abilities.frost;

import com.github.b4ndithelps.forge.effects.ModEffects;
import com.github.b4ndithelps.forge.systems.TempHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.SyncType;

import java.util.List;

@SuppressWarnings("removal")
public class WhiteoutAbility extends Ability {

    private static final float TEMP_DROP_PER_STORM_TICK = 0.18F;

    public static final PalladiumProperty<Float> MAX_RADIUS =
            new FloatProperty("max_radius").configurable("Maximum storm radius in blocks");
    public static final PalladiumProperty<Float> RADIUS_GROWTH =
            new FloatProperty("radius_growth").configurable("Radius growth per tick");
    public static final PalladiumProperty<Integer> SNOW_BLINDNESS_DURATION =
            new IntegerProperty("snow_blindness_duration").configurable("Duration (ticks) of snow blindness applied");
    public static final PalladiumProperty<Integer> SNOW_BLINDNESS_AMPLIFIER =
            new IntegerProperty("snow_blindness_amp").configurable("Amplifier applied to snow blindness (0 = level I)");
    public static final PalladiumProperty<Integer> PARTICLE_DENSITY =
            new IntegerProperty("particle_density").configurable("Number of flakes spawned per tick");
    public static final PalladiumProperty<Integer> SNOW_PILE_ATTEMPTS =
            new IntegerProperty("snow_pile_attempts").configurable("Random snow pile attempts per tick while active");

    private static final PalladiumProperty<Float> CURRENT_RADIUS =
            new FloatProperty("whiteout_internal_radius").sync(SyncType.NONE).disablePersistence();
    private static final float SNOW_PILE_CHANCE = 0.35F;

    public WhiteoutAbility() {
        this.withProperty(MAX_RADIUS, 10.0F)
                .withProperty(RADIUS_GROWTH, 0.25F)
                .withProperty(SNOW_BLINDNESS_DURATION, 80)
                .withProperty(SNOW_BLINDNESS_AMPLIFIER, 0)
                .withProperty(PARTICLE_DENSITY, 30)
                .withProperty(SNOW_PILE_ATTEMPTS, 1);
    }

    @Override
    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CURRENT_RADIUS, 0.0F);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        entry.setUniqueProperty(CURRENT_RADIUS, 0.0F);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) {
            return;
        }
        Level level = entity.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (entity instanceof Player player && TempHelper.isOverheated(player)) {
            return;
        }

        float maxRadius = Math.max(1.0F, entry.getProperty(MAX_RADIUS));
        float growth = Math.max(0.01F, entry.getProperty(RADIUS_GROWTH));
        float currentRadius = entry.getProperty(CURRENT_RADIUS);
        float newRadius = Math.min(maxRadius, currentRadius + growth);
        entry.setUniqueProperty(CURRENT_RADIUS, newRadius);

        spawnWhiteoutParticles(serverLevel, entity, newRadius, entry.getProperty(PARTICLE_DENSITY));
        applySnowBlindness(serverLevel, entity, entry, newRadius);
        scatterSnowPiles(serverLevel, entity, newRadius, entry.getProperty(SNOW_PILE_ATTEMPTS));

        if (entity instanceof Player player) {
            TempHelper.lowerInnerTemp(player, TEMP_DROP_PER_STORM_TICK);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            collapseBurst(serverLevel, entity.position());
        }
        entry.setUniqueProperty(CURRENT_RADIUS, 0.0F);
    }

    private void spawnWhiteoutParticles(ServerLevel level, LivingEntity entity, float radius, int density) {
        if (radius <= 0.1F || density <= 0) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 origin = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        int flakes = Mth.clamp(density, 5, 120);
        for (int i = 0; i < flakes; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * radius;
            double height = (random.nextDouble() - 0.5) * radius * 0.8 + origin.y;
            double x = origin.x + Math.cos(angle) * distance;
            double z = origin.z + Math.sin(angle) * distance;
            level.sendParticles(ParticleTypes.SNOWFLAKE, x, height, z, 1, 0.05, 0.05, 0.05, 0.0);
            if (random.nextFloat() < 0.15F) {
                level.sendParticles(ParticleTypes.CLOUD, x, height, z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    private void applySnowBlindness(ServerLevel level, LivingEntity caster, AbilityInstance entry, float radius) {
        if (radius <= 0.75F) {
            return;
        }
        int duration = Math.max(20, entry.getProperty(SNOW_BLINDNESS_DURATION));
        int amplifier = Math.max(0, entry.getProperty(SNOW_BLINDNESS_AMPLIFIER));
        double radiusSq = radius * radius;

        AABB box = caster.getBoundingBox().inflate(radius);
        List<Player> players = level.getEntitiesOfClass(Player.class, box,
                target -> target.isAlive() && target != caster && !target.isSpectator());
        for (Player target : players) {
            if (caster.isAlliedTo(target)) {
                continue;
            }
            if (target.distanceToSqr(caster) > radiusSq) {
                continue;
            }
            MobEffectInstance existing = target.getEffect(ModEffects.SNOW_BLINDNESS.get());
            if (existing != null && existing.getDuration() > duration - 20 && existing.getAmplifier() >= amplifier) {
                continue;
            }
            target.addEffect(new MobEffectInstance(ModEffects.SNOW_BLINDNESS.get(), duration, amplifier, false, true, true));
        }
    }

    private void scatterSnowPiles(ServerLevel level, LivingEntity entity, float radius, int configuredAttempts) {
        int attempts = Mth.clamp(configuredAttempts, 0, 24);
        if (radius <= 1.0F || attempts <= 0) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 origin = entity.position();

        for (int i = 0; i < attempts; i++) {
            if (random.nextFloat() > SNOW_PILE_CHANCE) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * radius;
            double sampleX = origin.x + Math.cos(angle) * distance;
            double sampleZ = origin.z + Math.sin(angle) * distance;
            BlockPos column = BlockPos.containing(sampleX, origin.y, sampleZ);
            if (!level.hasChunkAt(column)) {
                continue;
            }
            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            BlockPos placement = resolveSnowPlacement(level, surface);
            if (placement == null) {
                continue;
            }
            placeSnowLayer(level, placement);
        }
    }

    private BlockPos resolveSnowPlacement(ServerLevel level, BlockPos surface) {
        if (surface.getY() <= level.getMinBuildHeight() || surface.getY() >= level.getMaxBuildHeight()) {
            return null;
        }
        BlockState stateAtSurface = level.getBlockState(surface);
        if (isSnowMaterial(stateAtSurface)) {
            return null;
        }
        if (!stateAtSurface.isAir()) {
            BlockPos above = surface.above();
            if (above.getY() >= level.getMaxBuildHeight()) {
                return null;
            }
            surface = above;
        }
        BlockPos below = surface.below();
        if (below.getY() < level.getMinBuildHeight()) {
            return null;
        }
        BlockState belowState = level.getBlockState(below);
        if (isSnowMaterial(belowState)) {
            return null;
        }
        BlockState snow = Blocks.SNOW.defaultBlockState();
        if (!snow.canSurvive(level, surface)) {
            return null;
        }
        return surface;
    }

    private void placeSnowLayer(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight()) {
            return;
        }
        BlockState snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1);
        if (!snow.canSurvive(level, pos)) {
            return;
        }
        level.setBlockAndUpdate(pos, snow);
    }

    private boolean isSnowMaterial(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }

    private void collapseBurst(ServerLevel level, Vec3 position) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * 2.0;
            double x = position.x + Math.cos(angle) * distance;
            double z = position.z + Math.sin(angle) * distance;
            double y = position.y + random.nextDouble() * 1.8;
            level.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Toggles a growing whiteout that blinds other players within range. The longer it stays active, "
                + "the larger the storm grows until it reaches its configured maximum radius, scattering light snow piles.";
    }
}
