package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipMultiBlockWhipPacket;
import com.github.b4ndithelps.forge.systems.PowerStockHelper;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.SyncType;

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

    public static final PalladiumProperty<Integer> CHARGE_TICKS;

    public BlackwhipQuadZipAbility() {
        super();
        this.withProperty(MAX_CHARGE_TICKS, 50)
                .withProperty(MAX_RANGE, 24.0f)
                .withProperty(MIN_PULL, 0.9f)
                .withProperty(MAX_PULL, 3.2f)
                .withProperty(TRAVEL_TICKS, 7)
                .withProperty(CURVE, 0.6f)
                .withProperty(THICKNESS, 1.0f);
    }

    @Override
    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(CHARGE_TICKS, 0);
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
        java.util.List<Double> anglesDegList = new java.util.ArrayList<>(count);
        java.util.List<Double> distList = new java.util.ArrayList<>(count);
        java.util.List<Double> verticalList = new java.util.ArrayList<>(count);

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
                } else {
                    // Fallback: keep forwardEnd with vertical offset so it still looks natural
                    anchor = forwardEnd.add(trueUp.scale(verticalList.get(i)));
                }
            }

            xs.add(anchor.x);
            ys.add(anchor.y);
            zs.add(anchor.z);
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

        int maxTicks = Math.max(1, entry.getProperty(MAX_CHARGE_TICKS));
        int t = Math.min(maxTicks, entry.getProperty(CHARGE_TICKS) + 1);
        entry.setUniqueProperty(CHARGE_TICKS, t);

        if (t % 5 == 0) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 p = eye.add(look.scale(1.0 + (t / (float)maxTicks) * 0.8));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2 + (int)(t / (float)maxTicks * 4), 0.1, 0.1, 0.1, 0.02);

            // Action bar charging percentage with safety color coding (like ChargedPunch)
            float chargeRatio = t / (float) maxTicks;
            float storedPower = PowerStockHelper.getStoredPower(player);
            float chargePercent = chargeRatio * 100.0f;
            float powerUsed = storedPower * chargeRatio;
            PowerStockHelper.sendPlayerPercentageMessage(player, powerUsed, chargePercent, "Charging Quad Zip");
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        int chargeTicks = Math.max(0, entry.getProperty(CHARGE_TICKS));
        if (chargeTicks <= 0) return;

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

        Vec3 look = player.getLookAngle().normalize();
        Vec3 impulse = look.scale(impulseMag);

        // Apply strong forward impulse; slightly damp existing velocity for control
        player.setDeltaMovement(player.getDeltaMovement().scale(0.2).add(impulse));
        player.hasImpulse = true;
        player.fallDistance = 0.0F;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        // Clear visuals
        BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BlackwhipMultiBlockWhipPacket(player.getId(), false, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0,
                        entry.getProperty(CURVE), entry.getProperty(THICKNESS)));

        level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 1.35f);

        entry.setUniqueProperty(CHARGE_TICKS, 0);
    }

    static {
        CHARGE_TICKS = (new IntegerProperty("charge_ticks")).sync(SyncType.NONE).disablePersistence();
    }
}


