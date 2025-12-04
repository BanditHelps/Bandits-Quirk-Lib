package com.github.b4ndithelps.forge.abilities.blackwhip;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipMultiBlockWhipPacket;
import com.github.b4ndithelps.forge.network.PlayerVelocityS2CPacket;
import com.github.b4ndithelps.forge.utils.ActionBarHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("removal")
public class BlackwhipQuadZipAbility extends Ability {

    public static final PalladiumProperty<Integer> MAX_CHARGE_TICKS = new IntegerProperty("max_charge_ticks").configurable("Maximum charge time in ticks");
    public static final PalladiumProperty<Float> MAX_RANGE = new FloatProperty("max_range").configurable("Maximum reach for tendril anchors");
    public static final PalladiumProperty<Float> MIN_PULL = new FloatProperty("min_pull").configurable("Base pull impulse at minimal charge");
    public static final PalladiumProperty<Float> MAX_PULL = new FloatProperty("max_pull").configurable("Maximum pull impulse at full charge");
    public static final PalladiumProperty<Integer> TRAVEL_TICKS = new IntegerProperty("travel_ticks").configurable("Ticks for tendrils to travel to anchors");
    public static final PalladiumProperty<Float> CURVE = new FloatProperty("curve").configurable("Visual curve of tendrils");
    public static final PalladiumProperty<Float> THICKNESS = new FloatProperty("thickness").configurable("Visual thickness of tendrils");
    public static final PalladiumProperty<Float> BASE_DAMAGE = new FloatProperty("base_damage").configurable("Base damage when hitting an entity on release");
    public static final PalladiumProperty<Float> BASE_KNOCKBACK = new FloatProperty("base_knockback").configurable("Base knockback when hitting an entity on release");

    public static final PalladiumProperty<Integer> CHARGE_TICKS;
    public static final PalladiumProperty<Boolean> HAS_ANCHORS;
    public static final PalladiumProperty<Integer> ANCHOR_COUNT;
    private static final int MAX_ANCHORS = 12;
    public static final PalladiumProperty<Double>[] ANCHOR_X = new PalladiumProperty[MAX_ANCHORS];
    public static final PalladiumProperty<Double>[] ANCHOR_Y = new PalladiumProperty[MAX_ANCHORS];
    public static final PalladiumProperty<Double>[] ANCHOR_Z = new PalladiumProperty[MAX_ANCHORS];
    public static final PalladiumProperty<Double>[] ANCHOR_MAXLEN = new PalladiumProperty[MAX_ANCHORS];

    public BlackwhipQuadZipAbility() {
        super();
        this.withProperty(MAX_CHARGE_TICKS, 50)
                .withProperty(MAX_RANGE, 24.0f)
                .withProperty(MIN_PULL, 0.9f)
                .withProperty(MAX_PULL, 3.2f)
                .withProperty(TRAVEL_TICKS, 7)
                .withProperty(CURVE, 0.6f)
                .withProperty(THICKNESS, 1.0f)
                .withProperty(BASE_DAMAGE, 4.0f)
                .withProperty(BASE_KNOCKBACK, 0.6f);
    }

    @Override
    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CHARGE_TICKS, 0);
        manager.register(HAS_ANCHORS, false);
        manager.register(ANCHOR_COUNT, 0);
        for (int i = 0; i < MAX_ANCHORS; i++) {
            manager.register(ANCHOR_X[i], 0.0);
            manager.register(ANCHOR_Y[i], 0.0);
            manager.register(ANCHOR_Z[i], 0.0);
            manager.register(ANCHOR_MAXLEN[i], 0.0);
        }
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        entry.setUniqueProperty(CHARGE_TICKS, 0);

        float range = Math.max(4.0f, entry.getProperty(MAX_RANGE));
        int travel = Math.max(1, entry.getProperty(TRAVEL_TICKS));
        float curve = Math.max(0.0f, entry.getProperty(CURVE));
        float thickness = Math.max(0.05f, entry.getProperty(THICKNESS));

        // Determine tendril count based on quirk factor (ensure even: split across hands)
        int baseCount = 4;
        int maxCount = 12;
        int count = baseCount;
        if (player != null) {
            double qf = Math.max(0.0, QuirkFactorHelper.getQuirkFactor(player));
            // Scales as base + floor(base * qf), clamped and forced even
            count = baseCount + (int)Math.floor(baseCount * qf);
            if (count > maxCount) count = maxCount;
            if ((count & 1) == 1) count += 1; // make even
            if (count < 2) count = 2; // minimum even
        }
        int perSide = Math.max(1, count / 2);

        // Build 4 anchors in the look direction with a wider, symmetric left/right spread (2 per side)
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 up = new Vec3(0, 1, 0);
        // Use right-handed basis consistent with renderer: right = look x up
        Vec3 right = look.cross(up);
        if (right.lengthSqr() < 1.0e-6) right = new Vec3(1, 0, 0);
        right = right.normalize();
        Vec3 trueUp = up;

        // Build angled directions: perSide to the right (positive angles), then perSide to the left (negative angles)
        double minAngle = 12.0, maxAngle = 35.0;
        List<Double> anglesDegList = new ArrayList<>(count);
        List<Double> distList = new ArrayList<>(count);
        List<Double> verticalList = new ArrayList<>(count);

        for (int i = 0; i < perSide; i++) {
            double t = perSide == 1 ? 0.5 : (i + 0.5) / (double) perSide;
            double ang = minAngle + t * (maxAngle - minAngle);
            // right side
            anglesDegList.add(ang);
            // slight distance variance per index
            double distScale = 0.9 + 0.2 * t; // 0.9..1.1
            distList.add(range * distScale);
            // alternate vertical offsets up/down
            double v = (i % 2 == 0 ? 0.75 : -0.75);
            verticalList.add(v);
        }

        for (int i = 0; i < perSide; i++) {
            double tVar = perSide == 1 ? 0.5 : (i + 0.5) / (double) perSide;
            double ang = -(minAngle + tVar * (maxAngle - minAngle));
            // left side
            anglesDegList.add(ang);
            double distScale = 0.9 + 0.2 * tVar;
            distList.add(range * distScale);
            double v = (i % 2 == 0 ? 0.75 : -0.75);
            verticalList.add(v);
        }

        List<Double> xs = new ArrayList<>(count);
        List<Double> ys = new ArrayList<>(count);
        List<Double> zs = new ArrayList<>(count);
        boolean anySolidAnchor = false;
        for (int i = 0; i < count; i++) {
            double ang = Math.toRadians(anglesDegList.get(i));
            double c = Math.cos(ang);
            double s = Math.sin(ang);
            // Yaw around world up: rotate look toward left/right using right axis
            Vec3 dir = look.scale(c).add(right.scale(s)).normalize();

            // Raycast forward to try to hit a block
            Vec3 forwardEnd = eye.add(dir.scale(distList.get(i)));
            BlockHitResult forwardHit = player.level().clip(new ClipContext(
                    eye,
                    forwardEnd,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player));

            Vec3 anchor;
            if (forwardHit.getType() == HitResult.Type.BLOCK) {
                anchor = Vec3.atCenterOf(forwardHit.getBlockPos());
                anySolidAnchor = true;
            } else {
                // No hit: apply simple gravity by dropping from the forwardEnd point down to ground
                double drop = Math.max(4.0, range);
                BlockHitResult downHit = player.level().clip(new ClipContext(
                        forwardEnd,
                        forwardEnd.add(0, -drop, 0),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        player));
                if (downHit.getType() == HitResult.Type.BLOCK) {
                    anchor = Vec3.atCenterOf(downHit.getBlockPos());
                    anySolidAnchor = true;
                } else {
                    // Fallback: keep forwardEnd with vertical offset so it still looks natural
                    anchor = forwardEnd.add(trueUp.scale(verticalList.get(i)));
                }
            }

            xs.add(anchor.x);
            ys.add(anchor.y);
            zs.add(anchor.z);
        }

        // If none of the tendrils latched onto a real block, do not arm the ability
        if (!anySolidAnchor) {
            entry.setUniqueProperty(CHARGE_TICKS, 0);
            return;
        }

        // Persist anchors for rope constraint
        entry.setUniqueProperty(HAS_ANCHORS, true);
        entry.setUniqueProperty(ANCHOR_COUNT, Math.min(count, MAX_ANCHORS));
        int nStore = Math.min(count, MAX_ANCHORS);
        for (int i = 0; i < nStore; i++) {
            double ax = xs.get(i), ay = ys.get(i), az = zs.get(i);
            entry.setUniqueProperty(ANCHOR_X[i], ax);
            entry.setUniqueProperty(ANCHOR_Y[i], ay);
            entry.setUniqueProperty(ANCHOR_Z[i], az);
            double maxLen = Math.sqrt(player.position().distanceToSqr(ax, ay, az));
            entry.setUniqueProperty(ANCHOR_MAXLEN[i], maxLen);
        }

        // Send visuals to all tracking and self
        BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BlackwhipMultiBlockWhipPacket(player.getId(), true, xs, ys, zs, travel, curve, thickness));

        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 1.0f);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // If we have no valid anchors, don't charge or show any feedback
        if (!Boolean.TRUE.equals(entry.getProperty(HAS_ANCHORS))) {
            entry.setUniqueProperty(CHARGE_TICKS, 0);
            return;
        }

        int maxTicks = Math.max(1, entry.getProperty(MAX_CHARGE_TICKS));
        int t = Math.min(maxTicks, entry.getProperty(CHARGE_TICKS) + 1);
        entry.setUniqueProperty(CHARGE_TICKS, t);

        // Enforce rope constraints while anchors are attached
        if (Boolean.TRUE.equals(entry.getProperty(HAS_ANCHORS))) {
            int n = Math.max(0, Math.min(MAX_ANCHORS, entry.getProperty(ANCHOR_COUNT)));
            Vec3 pos = player.position();
            Vec3 velocity = player.getDeltaMovement();
            boolean adjusted = false;
            for (int i = 0; i < n; i++) {
                Vec3 anchor = new Vec3(entry.getProperty(ANCHOR_X[i]), entry.getProperty(ANCHOR_Y[i]), entry.getProperty(ANCHOR_Z[i]));
                double maxLen = Math.max(0.25, entry.getProperty(ANCHOR_MAXLEN[i]));
                Vec3 toPlayer = pos.subtract(anchor);
                double dist = toPlayer.length();
                if (dist > maxLen) {
                    Vec3 dir = toPlayer.scale(1.0 / dist);
                    Vec3 boundary = anchor.add(dir.scale(maxLen));
                    player.setPos(boundary.x, boundary.y, boundary.z);
                    // Remove radial velocity component to simulate taut rope; keep tangential motion slight damping
                    double vRad = velocity.dot(dir);
                    Vec3 vTangential = velocity.subtract(dir.scale(vRad));
                    double tangentialDamping = 0.0001;
                    Vec3 newV = vTangential.scale(1.0 - tangentialDamping);
                    player.setDeltaMovement(newV);
                    adjusted = true;
                    pos = boundary;
                    velocity = newV;
                }
            }
            if (adjusted) {
                player.hasImpulse = true;
                player.fallDistance = 0.0F;
                BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new PlayerVelocityS2CPacket(velocity.x, velocity.y, velocity.z, 0.0f));
            }
        }

        if (t % 5 == 0) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 p = eye.add(look.scale(1.0 + (t / (float)maxTicks) * 0.8));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2 + (int)(t / (float)maxTicks * 4), 0.1, 0.1, 0.1, 0.02);

            // Action bar charging percentage without safety status label
            float chargeRatio = t / (float) maxTicks;
            float chargePercent = chargeRatio * 100.0f;
            MutableComponent message = Component.literal("Charging Quad Zip")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(": ").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.format("%.0f%%", chargePercent)).withStyle(ChatFormatting.GREEN));
            ActionBarHelper.sendActionBar(player, message);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        // No valid anchors => do nothing on release
        if (!Boolean.TRUE.equals(entry.getProperty(HAS_ANCHORS))) {
            entry.setUniqueProperty(CHARGE_TICKS, 0);
            return;
        }

        int chargeTicks = Math.max(0, entry.getProperty(CHARGE_TICKS));
        if (chargeTicks == 0) return;

        int maxTicks = Math.max(1, entry.getProperty(MAX_CHARGE_TICKS));
        float ratio = Math.min(1.0f, chargeTicks / (float)maxTicks);
        float minPull = Math.max(0.0f, entry.getProperty(MIN_PULL));
        float maxPull = Math.max(minPull, entry.getProperty(MAX_PULL));
        // Scale max pull with quirk factor (base + base * qf)
        double qf = 0.0;
        if (player != null) {
            qf = Math.max(0.0, QuirkFactorHelper.getQuirkFactor(player));
        }
        float maxPullScaled = (float)(maxPull + (maxPull * qf));
        // Ease-out cubic for a snappy release
        float eased = 1.0f - (float)Math.pow(1.0 - ratio, 3.0);
        float impulseMag = minPull + (maxPullScaled - minPull) * eased;

        double configuredRange = Math.max(6.0, entry.getProperty(MAX_RANGE));
        double rayDistance = Math.max(6.0, configuredRange * (1.0 + qf));
        Vec3 desiredVelocity = null;
        boolean lockedTarget = false;

        // Try to fling toward a looked-at entity within range scaled by quirk factor
        LivingEntity target = raycastLivingTarget(player, rayDistance);
        if (target != null && target != player && target.isAlive()) {
            Vec3 toTarget = target.getEyePosition().subtract(player.getEyePosition());
            double dist = Math.sqrt(toTarget.lengthSqr());
            if (dist > 1.0E-4) {
                lockedTarget = true;
                Vec3 dir = toTarget.normalize();
                double speed = computeQuadZipSpeed(impulseMag * 1.05F, dist, ratio, qf, rayDistance, true);
                desiredVelocity = dir.scale(speed);

                // Arm a brief post-release collision hit window; damage will apply only if player flies through an entity
                float baseDamage = Math.max(0.0f, entry.getProperty(BASE_DAMAGE));
                float finalDamage = baseDamage * (1.0f + (float) qf);
                float baseKb = Math.max(0.0f, entry.getProperty(BASE_KNOCKBACK));
                float finalKb = baseKb * (1.0f + (float) qf);
                player.getPersistentData().putDouble("Bql.QZipDamage", finalDamage);
                player.getPersistentData().putDouble("Bql.QZipKnock", finalKb);
                player.getPersistentData().putInt("Bql.QZipTicks", 8);
                // Initialize per-window hit tracking
                player.getPersistentData().put("Bql.QZipHits", new CompoundTag());
            }
        }

        if (desiredVelocity == null) {
            // Apply a more horizontally biased impulse when no target is found
            Vec3 dir = player.getLookAngle().normalize();
            Vec3 biasedDir = new Vec3(dir.x, dir.y * 0.35, dir.z);
            if (biasedDir.lengthSqr() < 1.0E-5) {
                biasedDir = dir;
            }
            Vec3 normalized = biasedDir.lengthSqr() > 1.0E-5 ? biasedDir.normalize() : new Vec3(0, 0, 1);
            double travelBudget = Math.max(3.0, computeAnchorTravelBudget(entry));
            travelBudget = Math.max(travelBudget, configuredRange * 0.7);
            double maxBudget = configuredRange * (1.35 + qf * 0.25);
            travelBudget = Math.min(maxBudget, travelBudget);
            double speed = computeQuadZipSpeed(impulseMag, travelBudget, ratio, qf, configuredRange, false);
            desiredVelocity = normalized.scale(speed);
        }

        float damping = lockedTarget ? 0.22f : 0.3f;
        Vec3 prevVelocity = player.getDeltaMovement();
        Vec3 damped = prevVelocity.scale(damping);
        Vec3 adjustment = desiredVelocity.subtract(damped);
        // Override previous velocity to ensure straight flight toward the chosen direction
        player.setDeltaMovement(desiredVelocity);
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PlayerVelocityS2CPacket(adjustment.x, adjustment.y, adjustment.z, damping));

        // Clear visuals
        BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BlackwhipMultiBlockWhipPacket(player.getId(), false, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0,
                        entry.getProperty(CURVE), entry.getProperty(THICKNESS)));

        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 1.35f);

        entry.setUniqueProperty(CHARGE_TICKS, 0);
        entry.setUniqueProperty(HAS_ANCHORS, false);
        entry.setUniqueProperty(ANCHOR_COUNT, 0);
    }

    private double computeAnchorTravelBudget(AbilityInstance entry) {
        if (!Boolean.TRUE.equals(entry.getProperty(HAS_ANCHORS))) {
            return 0.0;
        }
        int n = Math.max(0, Math.min(MAX_ANCHORS, entry.getProperty(ANCHOR_COUNT)));
        if (n <= 0) {
            return Math.max(0.0f, entry.getProperty(MAX_RANGE));
        }
        double total = 0.0;
        double max = 0.0;
        for (int i = 0; i < n; i++) {
            double len = Math.max(0.0, entry.getProperty(ANCHOR_MAXLEN[i]));
            total += len;
            if (len > max) {
                max = len;
            }
        }
        double avg = total / n;
        return Math.max(max, avg);
    }

    private double computeQuadZipSpeed(double baseStrength, double travelDistance, double chargeRatio, double quirkFactor, double rangeHint, boolean targetLocked) {
        double distance = Math.max(0.25, travelDistance);
        double normalizedRange = Math.min(1.0, distance / Math.max(4.0, rangeHint));
        double statsBonus = 1.15 + Math.min(1.75, quirkFactor * 0.25 + chargeRatio * 0.9);
        double rangeBonus = targetLocked ? (0.9 + normalizedRange * 1.05) : (0.8 + normalizedRange * 1.25);
        double targetSpeed = Math.max(0.5, baseStrength) * statsBonus * rangeBonus;
        double minSpeed = Math.max(0.8, Math.min(distance, baseStrength * 0.75));
        double distanceCap = distance * (targetLocked ? 1.35 : 1.5) + quirkFactor * 0.8;
        double hardCap = targetLocked ? 13.5 + quirkFactor * 1.6 : 12.0 + quirkFactor * 1.4;
        double maxSpeed = Math.max(minSpeed + 0.2, Math.min(distanceCap, hardCap));
        return Math.max(minSpeed, Math.min(maxSpeed, targetSpeed));
    }

    private LivingEntity raycastLivingTarget(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, end).inflate(1.0), e -> e != null && e != player && e.isAlive());

        LivingEntity best = null;
        double bestDist = range;
        for (LivingEntity e : candidates) {
            var hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceTo(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = e;
                }
            }
        }
        return best;
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
        HAS_ANCHORS = (new BooleanProperty("qzip_has_anchors")).sync(SyncType.NONE).disablePersistence();
        ANCHOR_COUNT = (new IntegerProperty("qzip_anchor_count")).sync(SyncType.NONE).disablePersistence();
        for (int i = 0; i < MAX_ANCHORS; i++) {
            ANCHOR_X[i] = (new DoubleProperty("qzip_anchor_x_" + i)).sync(SyncType.NONE).disablePersistence();
            ANCHOR_Y[i] = (new DoubleProperty("qzip_anchor_y_" + i)).sync(SyncType.NONE).disablePersistence();
            ANCHOR_Z[i] = (new DoubleProperty("qzip_anchor_z_" + i)).sync(SyncType.NONE).disablePersistence();
            ANCHOR_MAXLEN[i] = (new DoubleProperty("qzip_anchor_maxlen_" + i)).sync(SyncType.NONE).disablePersistence();
        }
    }
}