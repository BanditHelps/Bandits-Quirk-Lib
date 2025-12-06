package com.github.b4ndithelps.forge.abilities.frost;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.TempHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

import java.util.List;

/**
 * Continuous beam that freezes targets and piles up snow where it contacts the terrain.
 */
@SuppressWarnings("removal")
public class SnowBeamAbility extends Ability {

    private static final float TEMP_DROP_PER_ACTIVE_TICK = 0.5F;

    public static final PalladiumProperty<Float> RANGE = new FloatProperty("beam_range")
            .configurable("Maximum reach of the snow beam in blocks");
    public static final PalladiumProperty<Float> BEAM_RADIUS = new FloatProperty("beam_radius")
            .configurable("Radius used for entity collision along the beam path");
    public static final PalladiumProperty<Integer> SNOW_BUILDUP = new IntegerProperty("snow_layers_per_tick")
            .configurable("How many snow layers are added each tick while the beam touches a surface");
    public static final PalladiumProperty<Integer> FROST_DURATION = new IntegerProperty("frost_duration")
            .configurable("Duration (in ticks) of the frost slow applied to targets");
    public static final PalladiumProperty<Integer> FROST_AMPLIFIER = new IntegerProperty("frost_amplifier")
            .configurable("Amplifier level for the slowness effect (1 = Slowness I)");
    public static final PalladiumProperty<Integer> FREEZE_BUILDUP = new IntegerProperty("freeze_buildup")
            .configurable("Ticks of freeze progress added to hit targets");
    public static final PalladiumProperty<Integer> SNOW_HEIGHT_CAP = new IntegerProperty("snow_height_cap")
            .configurable("Maximum height in blocks that snow piles created by the beam may reach");
    public static final PalladiumProperty<String> CONE_BODY_PART = new StringProperty("cone_body_part")
            .configurable("Body part storing the cone control value (e.g., 'chest')");
    public static final PalladiumProperty<String> CONE_BODY_KEY = new StringProperty("cone_body_key")
            .configurable("Custom float key (0-1) that determines spread from point to cone");

    private static final float MAX_CONE_EXPANSION = 3.0F;
    private static final int MAX_SNOW_RADIUS = 6;

    public SnowBeamAbility() {
        super();
        this.withProperty(RANGE, 14.0F)
                .withProperty(BEAM_RADIUS, 0.65F)
                .withProperty(SNOW_BUILDUP, 2)
                .withProperty(FROST_DURATION, 80)
                .withProperty(FROST_AMPLIFIER, 1)
                .withProperty(FREEZE_BUILDUP, 12)
                .withProperty(SNOW_HEIGHT_CAP, 6)
                .withProperty(CONE_BODY_PART, "chest")
                .withProperty(CONE_BODY_KEY, "frost_beam_shape");
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
            return;
        }

        Vec3 direction = entity.getLookAngle();
        if (direction.lengthSqr() < 1.0e-6) {
            return;
        }
        Vec3 normalized = direction.normalize();
        Vec3 start = entity.getEyePosition();

        float configuredRange = Math.max(0.5F, entry.getProperty(RANGE));
        Vec3 end = start.add(normalized.scale(configuredRange));
        float baseRadius = Math.max(0.2F, entry.getProperty(BEAM_RADIUS));
        float coneFactor = getConeFactor(entity, entry);

        ClipContext context = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, entity);
        BlockHitResult blockHit = entity.level().clip(context);
        double beamLength = configuredRange;

        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            beamLength = Math.min(beamLength, blockHit.getLocation().distanceTo(start));
            handleSnowImpact(serverLevel, blockHit, entry, beamLength, configuredRange, baseRadius, coneFactor);
        }

        applyBeamToEntities(serverLevel, entity, start, normalized, beamLength, entry, baseRadius, coneFactor);
        spawnBeamParticles(serverLevel, start, normalized, beamLength, baseRadius, coneFactor);

        if (entity instanceof Player player) {
            TempHelper.lowerInnerTemp(player, TEMP_DROP_PER_ACTIVE_TICK);
        }
    }

    private void applyBeamToEntities(ServerLevel level, LivingEntity caster, Vec3 start, Vec3 direction, double beamLength,
                                     AbilityInstance entry, float baseRadius, float coneFactor) {
        Vec3 end = start.add(direction.scale(beamLength));
        float maxRadius = computeRadiusAtFraction(baseRadius, coneFactor, 1.0D);
        AABB searchBox = new AABB(start, end).inflate(maxRadius + 0.5);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                target -> target.isAlive() && target != caster && !target.isSpectator());

        for (LivingEntity target : targets) {
            if (!shouldAffect(caster, target, start, direction, beamLength, baseRadius, coneFactor)) {
                continue;
            }
            applyFrost(target, entry);
        }
    }

    private boolean shouldAffect(LivingEntity caster, LivingEntity target, Vec3 start, Vec3 direction, double beamLength,
                                 float baseRadius, float coneFactor) {
        if (caster.isAlliedTo(target)) {
            return false;
        }

        Vec3 targetCenter = target.getBoundingBox().getCenter();
        Vec3 toTarget = targetCenter.subtract(start);
        double projection = toTarget.dot(direction);
        double min = -target.getBbWidth();
        double max = beamLength + target.getBbWidth();
        if (projection < min || projection > max) {
            return false;
        }

        double clampedProjection = Mth.clamp(projection, 0.0, beamLength);
        double fraction = beamLength <= 1.0e-3 ? 0.0 : clampedProjection / beamLength;
        float dynamicRadius = computeRadiusAtFraction(baseRadius, coneFactor, fraction);

        Vec3 closestPoint = start.add(direction.scale(clampedProjection));
        double allowed = dynamicRadius + target.getBbWidth() * 0.5;
        return closestPoint.distanceToSqr(targetCenter) <= allowed * allowed;
    }

    private void applyFrost(LivingEntity target, AbilityInstance entry) {
        int duration = Math.max(0, entry.getProperty(FROST_DURATION));
        int amplifier = Math.max(0, entry.getProperty(FROST_AMPLIFIER) - 1);
        if (duration > 0) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, amplifier, false, true, true));
        }

        int freezeBuildup = Math.max(0, entry.getProperty(FREEZE_BUILDUP));
        if (freezeBuildup > 0) {
            int required = target.getTicksRequiredToFreeze();
            int current = target.getTicksFrozen();
            target.setTicksFrozen(Math.min(required, current + freezeBuildup));
        }
    }

    private void handleSnowImpact(ServerLevel level, BlockHitResult hitResult, AbilityInstance entry, double beamLength,
                                  float configuredRange, float baseRadius, float coneFactor) {
        int layersPerTick = Math.max(0, entry.getProperty(SNOW_BUILDUP));
        if (layersPerTick <= 0) {
            return;
        }
        int capBlocks = Math.max(1, entry.getProperty(SNOW_HEIGHT_CAP));

        Direction face = hitResult.getDirection();
        if (face == Direction.DOWN) {
            return; // We only pile snow on top or on the sides
        }

        BlockPos basePos = hitResult.getBlockPos();
        BlockState baseState = level.getBlockState(basePos);

        BlockPos targetPos = face == Direction.UP
                ? basePos.above()
                : basePos.relative(face);

        // If we are already intersecting snow, keep stacking on that column instead of moving upward
        if (isSnowMaterial(baseState)) {
            targetPos = basePos;
        }

        if (!level.hasChunkAt(targetPos)) {
            return;
        }

        double normalization = configuredRange <= 1.0e-3F ? 0.0 : Math.min(1.0, beamLength / configuredRange);
        float spreadRadius = computeRadiusAtFraction(baseRadius, coneFactor, normalization);
        int horizontalRadius = Math.min(MAX_SNOW_RADIUS, Math.max(0, Mth.floor(spreadRadius)));

        if (horizontalRadius <= 0) {
            BlockPos columnBase = findColumnBase(level, targetPos);
            addSnowLayers(level, targetPos, layersPerTick, columnBase, capBlocks);
            return;
        }

        int radiusSq = horizontalRadius * horizontalRadius;
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                if (dx * dx + dz * dz > radiusSq) continue;

                BlockPos areaPos = targetPos.offset(dx, 0, dz);
                if (!level.hasChunkAt(areaPos)) continue;

                BlockPos columnBase = findColumnBase(level, areaPos);
                addSnowLayers(level, areaPos, layersPerTick, columnBase, capBlocks);
            }
        }
    }

    private BlockPos findColumnBase(ServerLevel level, BlockPos start) {
        BlockPos current = start;
        while (current.getY() > level.getMinBuildHeight() && isSnowMaterial(level.getBlockState(current.below()))) {
            current = current.below();
        }
        return current;
    }

    private void addSnowLayers(ServerLevel level, BlockPos pos, int layersToAdd, BlockPos basePos, int capBlocks) {
        if (layersToAdd <= 0) {
            return;
        }
        if (pos.getY() - basePos.getY() >= capBlocks) {
            return;
        }

        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            placeSnowState(level, pos, layersToAdd, basePos, capBlocks);
            return;
        }

        if (state.is(Blocks.SNOW)) {
            int existing = state.getValue(SnowLayerBlock.LAYERS);
            int total = existing + layersToAdd;
            if (total <= 8) {
                level.setBlockAndUpdate(pos, state.setValue(SnowLayerBlock.LAYERS, total));
            } else {
                level.setBlockAndUpdate(pos, Blocks.SNOW_BLOCK.defaultBlockState());
                addSnowLayers(level, pos.above(), total - 8, basePos, capBlocks);
            }
            return;
        }

        if (state.is(Blocks.SNOW_BLOCK)) {
            addSnowLayers(level, pos.above(), layersToAdd, basePos, capBlocks);
            return;
        }

        if (state.canBeReplaced()) {
            placeSnowState(level, pos, layersToAdd, basePos, capBlocks);
        }
    }

    private void placeSnowState(ServerLevel level, BlockPos pos, int layers, BlockPos basePos, int capBlocks) {
        if (pos.getY() - basePos.getY() >= capBlocks) {
            return;
        }
        BlockState snow = Blocks.SNOW.defaultBlockState();
        if (!snow.canSurvive(level, pos)) {
            return;
        }

        if (layers >= 8) {
            level.setBlockAndUpdate(pos, Blocks.SNOW_BLOCK.defaultBlockState());
            addSnowLayers(level, pos.above(), layers - 8, basePos, capBlocks);
        } else {
            level.setBlockAndUpdate(pos, snow.setValue(SnowLayerBlock.LAYERS, Math.max(1, Math.min(7, layers))));
        }
    }

    private boolean isSnowMaterial(BlockState state) {
        return state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK);
    }

    private float getConeFactor(LivingEntity entity, AbilityInstance entry) {
        if (!(entity instanceof Player player)) {
            return 0.0F;
        }
        if (!BodyStatusHelper.isBodyStatusAvailable(player)) {
            return 0.0F;
        }

        String part = entry.getProperty(CONE_BODY_PART);
        String key = entry.getProperty(CONE_BODY_KEY);
        if (part == null || part.isEmpty() || key == null || key.isEmpty()) {
            return 0.0F;
        }

        try {
            float value = BodyStatusHelper.getCustomFloat(player, part, key);
            return Mth.clamp(value, 0.0F, 1.0F);
        } catch (RuntimeException ignored) {
            return 0.0F;
        }
    }

    private float computeRadiusAtFraction(float baseRadius, float coneFactor, double fraction) {
        float clampedFactor = Mth.clamp(coneFactor, 0.0F, 1.0F);
        float normalized = (float) Mth.clamp(fraction, 0.0, 1.0);
        float expansionMultiplier = 1.0F + normalized * MAX_CONE_EXPANSION;
        float lerpedMultiplier = (float) Mth.lerp(clampedFactor, 1.0F, expansionMultiplier);
        return baseRadius * lerpedMultiplier;
    }

    private void spawnBeamParticles(ServerLevel level, Vec3 start, Vec3 direction, double beamLength,
                                    float baseRadius, float coneFactor) {
        if (beamLength <= 0.0D) {
            return;
        }

        double spacing = 0.55D;
        int steps = Math.max(1, (int) Math.ceil(beamLength / spacing));
        Vec3 unit = direction.normalize();

        double viewBuffer = 1.1D; // keep immediate area near player clear
        RandomSource random = level.getRandom();

        for (int i = 0; i <= steps; i++) {
            double distance = Math.min(beamLength, i * spacing);
            if (distance < viewBuffer) {
                continue;
            }

            double fraction = beamLength <= 1.0e-3 ? 0.0 : distance / beamLength;
            float visualRadius = computeRadiusAtFraction(baseRadius, coneFactor, fraction) * 0.45F;
            double angle = random.nextDouble() * (Math.PI * 2);
            double radial = coneFactor <= 0.0F ? 0.0 : random.nextDouble() * visualRadius;
            Vec3 offset = new Vec3(Math.cos(angle) * radial, 0.0, Math.sin(angle) * radial);

            Vec3 pos = start.add(unit.scale(distance)).add(offset);
            level.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 1, 0.02, 0.02, 0.02, 0.0);
            if (i % 4 == 0 && coneFactor > 0.0F) {
                level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 1, 0.015, 0.01, 0.015, 0.0);
            }
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Fires a continuous stream of snow that freezes targets, slows them, and piles up snow wherever the beam meets the ground.";
    }
}

