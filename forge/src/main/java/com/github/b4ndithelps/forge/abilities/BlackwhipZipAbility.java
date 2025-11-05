package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipBlockWhipPacket;
import net.minecraft.core.particles.ParticleTypes;
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
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;

@SuppressWarnings("removal")
public class BlackwhipZipAbility extends Ability {

	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Max grapple range");
	public static final PalladiumProperty<Float> PULL_SPEED = new FloatProperty("pull_speed").configurable("Pull speed toward anchor");
	public static final PalladiumProperty<Integer> MAX_TICKS = new IntegerProperty("max_ticks").configurable("Max duration to apply pull (0 = instant impulse)");
    // Unique runtime state: stored anchor and armed flag
    public static final PalladiumProperty<Boolean> HAS_ANCHOR = new net.threetag.palladium.util.property.BooleanProperty("has_anchor");
    public static final PalladiumProperty<Double> ANCHOR_X = new net.threetag.palladium.util.property.DoubleProperty("anchor_x");
    public static final PalladiumProperty<Double> ANCHOR_Y = new net.threetag.palladium.util.property.DoubleProperty("anchor_y");
    public static final PalladiumProperty<Double> ANCHOR_Z = new net.threetag.palladium.util.property.DoubleProperty("anchor_z");
	public static final PalladiumProperty<Double> ANCHOR_MAXLEN = new net.threetag.palladium.util.property.DoubleProperty("anchor_maxlen");
	public static final PalladiumProperty<Boolean> DEBUG = new net.threetag.palladium.util.property.BooleanProperty("debug").configurable("Enable debug telemetry for zip");

	public BlackwhipZipAbility() {
		super();
		this.withProperty(RANGE, 24.0F)
				.withProperty(PULL_SPEED, 1.2F)
				.withProperty(MAX_TICKS, 0)
				.withProperty(DEBUG, false);
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
        float range = Math.max(1.0F, entry.getProperty(RANGE));
        float speed = Math.max(0.1F, entry.getProperty(PULL_SPEED));

        if (armed) {
            // Second press: apply zip toward stored anchor and clear it
            Vec3 anchor = new Vec3(entry.getProperty(ANCHOR_X), entry.getProperty(ANCHOR_Y), entry.getProperty(ANCHOR_Z));
            Vec3 toAnchor = anchor.subtract(player.position());
            if (toAnchor.lengthSqr() > 1.0e-3) {
                Vec3 impulse = toAnchor.normalize().scale(speed);
                player.setDeltaMovement(player.getDeltaMovement().scale(0.2).add(impulse));
                player.hasImpulse = true;
                player.fallDistance = 0.0F;
                // Client motion for instant feedback
                BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BlackwhipBlockWhipPacket(player.getId(), false, anchor.x, anchor.y, anchor.z, 0, 0.6f, 1.0f));
                BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new com.github.b4ndithelps.forge.network.PlayerVelocityS2CPacket(impulse.x, impulse.y, impulse.z, 0.2f));
                player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 1.35f);
            }
            entry.setUniqueProperty(HAS_ANCHOR, false);
            // Retract visual for others as well
            BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                    new BlackwhipBlockWhipPacket(player.getId(), false, anchor.x, anchor.y, anchor.z, 0,
                            0.6f, 1.0f));
            return;
        }

        // First press: raycast to a block and store anchor + play visuals
        BlockHitResult hit = player.level().clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(range)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player));
        if (hit.getType() != HitResult.Type.BLOCK) return;

        Vec3 anchor = Vec3.atCenterOf(hit.getBlockPos());
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

		// Debug marker at anchor
		if (Boolean.TRUE.equals(entry.getProperty(DEBUG)) && player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
			sl.sendParticles(ParticleTypes.END_ROD, anchor.x, anchor.y, anchor.z, 8, 0.05, 0.05, 0.05, 0.0);
		}
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
				// Remove outward radial velocity so motion becomes tangential (swing-like)
				Vec3 vNow = player.getDeltaMovement();
				double vOut = vNow.dot(dir);
				if (vOut > 0) {
					vNow = vNow.subtract(dir.scale(vOut));
				}
				// Spring toward the boundary with damping
				double overshoot = dist - maxLen;
				double stiffness = 1.4; // firm tug
				double damping = 0.08;  // stabilize
				Vec3 spring = dir.scale(-overshoot * stiffness);
				Vec3 newVel = vNow.scale(1.0 - damping).add(spring);
				player.setDeltaMovement(newVel);
				player.hasImpulse = true;
				player.fallDistance = 0.0F;
				// Hard clamp position to exact rope boundary to prevent growth
				Vec3 boundary = anchor.add(dir.scale(maxLen));
				player.setPos(boundary.x, boundary.y, boundary.z);
				// Send client immediate velocity so the tug is felt locally
				BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
						new com.github.b4ndithelps.forge.network.PlayerVelocityS2CPacket(newVel.x, newVel.y, newVel.z, 0.0f));

				if (Boolean.TRUE.equals(entry.getProperty(DEBUG)) && player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
					boundary = anchor.add(dir.scale(maxLen));
					sl.sendParticles(ParticleTypes.CRIT, boundary.x, boundary.y, boundary.z, 6, 0.03, 0.03, 0.03, 0.0);
				}
			}
		}
	}
}






