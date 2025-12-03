package com.github.b4ndithelps.forge.abilities.blackwhip;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipBlockWhipPacket;
import com.github.b4ndithelps.forge.network.PlayerVelocityS2CPacket;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

@SuppressWarnings("removal")
public class BlackwhipZipAbility extends Ability {

	public static final PalladiumProperty<Float> MAX_RANGE = new FloatProperty("max_range").configurable("Max grapple range");
	public static final PalladiumProperty<Float> RANGE_BASE = new FloatProperty("range_base").configurable("Base grapple range");
	public static final PalladiumProperty<Float> RANGE_QUIRK_BOOST = new FloatProperty("range_quirk_boost").configurable("Additional range per quirk factor");
	public static final PalladiumProperty<Float> PULL_SPEED = new FloatProperty("pull_speed").configurable("Pull speed toward anchor");
	public static final PalladiumProperty<Integer> MAX_TICKS = new IntegerProperty("max_ticks").configurable("Max duration to apply pull (0 = instant impulse)");
	public static final PalladiumProperty<Float> PULL_QUIRK_BOOST = new FloatProperty("pull_quirk_boost").configurable("Additional pull speed multiplier per quirk factor");

    // Unique runtime state: stored anchor and armed flag
    public static final PalladiumProperty<Boolean> HAS_ANCHOR = new BooleanProperty("has_anchor");
    public static final PalladiumProperty<Double> ANCHOR_X = new DoubleProperty("anchor_x");
    public static final PalladiumProperty<Double> ANCHOR_Y = new DoubleProperty("anchor_y");
    public static final PalladiumProperty<Double> ANCHOR_Z = new DoubleProperty("anchor_z");
	public static final PalladiumProperty<Double> ANCHOR_MAXLEN = new DoubleProperty("anchor_maxlen");

	public BlackwhipZipAbility() {
		super();
		this.withProperty(MAX_RANGE, 0.0F)
				.withProperty(RANGE_BASE, 24.0F)
				.withProperty(RANGE_QUIRK_BOOST, 6.0F)
				.withProperty(PULL_SPEED, 1.2F)
				.withProperty(PULL_QUIRK_BOOST, 0.35F)
				.withProperty(MAX_TICKS, 0);
	}

    @Override
    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(HAS_ANCHOR, false);
        manager.register(ANCHOR_X, 0.0);
        manager.register(ANCHOR_Y, 0.0);
        manager.register(ANCHOR_Z, 0.0);
		manager.register(ANCHOR_MAXLEN, 0.0);
    }

	@Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;

        boolean armed = Boolean.TRUE.equals(entry.getProperty(HAS_ANCHOR));
        // Compute effective range: max(legacy range, base + quirkFactor * boost)
        float maxRange = Math.max(0.0F, entry.getProperty(MAX_RANGE));
        float baseRange = Math.max(0.0F, entry.getProperty(RANGE_BASE));
        float quirkBoostPerPoint = Math.max(0.0F, entry.getProperty(RANGE_QUIRK_BOOST));
        double quirkFactor = Math.max(0.0, QuirkFactorHelper.getQuirkFactor(player));
        float range = Math.max(1.0F, Math.max(maxRange, baseRange + (float) (quirkFactor * quirkBoostPerPoint)));
        float basePullSpeed = Math.max(0.1F, entry.getProperty(PULL_SPEED));
        float quirkPullBoost = Math.max(0.0F, entry.getProperty(PULL_QUIRK_BOOST));
        double pullSpeed = basePullSpeed * (1.0 + quirkFactor * quirkPullBoost);

        if (armed) {
            // Second press: apply zip toward stored anchor and clear it
            Vec3 anchor = new Vec3(entry.getProperty(ANCHOR_X), entry.getProperty(ANCHOR_Y), entry.getProperty(ANCHOR_Z));
            Vec3 toAnchor = anchor.subtract(player.position());
			if (toAnchor.lengthSqr() > 1.0e-3) {
				double dist = Math.sqrt(toAnchor.lengthSqr());
				double normalizedRange = Math.min(1.0, dist / Math.max(1.0, range));
				double statsBonus = 1.0 + Math.min(1.5, quirkFactor * Math.max(0.02, quirkPullBoost * 0.15));
				double targetSpeed = Math.max(0.25, pullSpeed) * (0.6 + normalizedRange * 0.9) * statsBonus;
				double minSpeed = Math.max(0.3, Math.min(dist, 0.35 + pullSpeed * 0.25));
				double maxSpeed = Math.max(minSpeed, Math.min(dist, 5.5 + quirkFactor * 0.35));
				double clampedSpeed = Math.max(minSpeed, Math.min(maxSpeed, targetSpeed));
				Vec3 desiredVelocity = toAnchor.normalize().scale(clampedSpeed);
				float damping = 0.35f;
				Vec3 damped = player.getDeltaMovement().scale(damping);
				Vec3 adjustment = desiredVelocity.subtract(damped);
                player.setDeltaMovement(desiredVelocity);
                player.hasImpulse = true;
                player.fallDistance = 0.0F;
                // Client motion for instant feedback
                BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlackwhipBlockWhipPacket(player.getId(), false, anchor.x, anchor.y, anchor.z, 0, 0.6f, 1.0f));
                BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerVelocityS2CPacket(adjustment.x, adjustment.y, adjustment.z, damping));
                player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 1.35f);
            }
            entry.setUniqueProperty(HAS_ANCHOR, false);
            // Retract visual for others as well
            BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new BlackwhipBlockWhipPacket(player.getId(), false, anchor.x, anchor.y, anchor.z, 0,
                            0.6f, 1.0f));
            return;
        }

        // First press: raycast entity and block. If entity is hit first, pull entity toward player; otherwise anchor to block
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));

        // Block hit
        BlockHitResult blockHit = player.level().clip(new ClipContext(
                eye,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player));

        if (blockHit.getType() != HitResult.Type.BLOCK) return;

        Vec3 anchor = Vec3.atCenterOf(blockHit.getBlockPos());
        entry.setUniqueProperty(HAS_ANCHOR, true);
        entry.setUniqueProperty(ANCHOR_X, anchor.x);
        entry.setUniqueProperty(ANCHOR_Y, anchor.y);
        entry.setUniqueProperty(ANCHOR_Z, anchor.z);
		// Store rope maximum length at moment of attachment so it cannot grow longer
        double maxLen = anchor.distanceTo(player.position());
        entry.setUniqueProperty(ANCHOR_MAXLEN, maxLen);

        // Send client visuals: tendril traveling to the block anchor
        BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BlackwhipBlockWhipPacket(player.getId(), true, anchor.x, anchor.y, anchor.z, 6,
                        0.6f, 1.0f));
        player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 1.0f);
    }

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;
		boolean anchored = Boolean.TRUE.equals(entry.getProperty(HAS_ANCHOR));
		if (!enabled && !anchored) return;

		if (anchored) {
			Vec3 anchor = new Vec3(entry.getProperty(ANCHOR_X), entry.getProperty(ANCHOR_Y), entry.getProperty(ANCHOR_Z));
			double maxLen = Math.max(0.25, entry.getProperty(ANCHOR_MAXLEN));
			Vec3 toPlayer = player.position().subtract(anchor);
			double dist = toPlayer.length();
			if (dist > maxLen) {
				Vec3 dir = toPlayer.scale(1.0 / dist);
				// Hard clamp to rope length to enforce non-extendable constraint
				Vec3 boundary = anchor.add(dir.scale(maxLen));
				player.setPos(boundary.x, boundary.y, boundary.z);
				// Project velocity onto tangent (remove all radial component) for pendulum-like motion
				Vec3 v = player.getDeltaMovement();
				double vRad = v.dot(dir);
				Vec3 vTangential = v.subtract(dir.scale(vRad));
				// Slight tangential damping to reduce jitter while keeping swing
				double tangentialDamping = 0.0001;
				Vec3 newVelocity = vTangential.scale(1.0 - tangentialDamping);
				player.setDeltaMovement(newVelocity);
				player.hasImpulse = true;
				player.fallDistance = 0.0F;
				// Sync client velocity for immediate local feel
				BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
						new PlayerVelocityS2CPacket(newVelocity.x, newVelocity.y, newVelocity.z, 0.0f));
			}
		}
	}
}