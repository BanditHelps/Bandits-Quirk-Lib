package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("removal")
public class BlackwhipPuppetAbility extends Ability {

	public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Max distance to consider tags valid (0 = any)");
	public static final PalladiumProperty<Float> MAX_LENGTH = new FloatProperty("max_length").configurable("Max whip length before dragging like a lead");
	public static final PalladiumProperty<Float> SPRING_STIFFNESS = new FloatProperty("spring_stiffness").configurable("How strongly the target is pulled toward the anchor");
	public static final PalladiumProperty<Float> DAMPING = new FloatProperty("damping").configurable("Velocity damping while controlled (0..1, higher = more damping)");
    public static final PalladiumProperty<Float> UPWARD_BIAS = new FloatProperty("upward_bias").configurable("Extra vertical bias when looking up");

	public BlackwhipPuppetAbility() {
		super();
		this.withProperty(MAX_DISTANCE, 48)
				.withProperty(SPRING_STIFFNESS, 0.35F)
				.withProperty(DAMPING, 0.8F)
				.withProperty(UPWARD_BIAS, 0.6F);
	}

	// Stores, per player, the fixed maximum tether length for each currently attached target
	private static final Map<UUID, Map<UUID, Double>> PLAYER_TETHER_MAXLEN = new ConcurrentHashMap<>();

	// Lead behavior does not need per-target offsets, heights, or bias smoothing

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		int maxDist = Math.max(0, entry.getProperty(MAX_DISTANCE));
		float k = Math.max(0.0F, entry.getProperty(SPRING_STIFFNESS));
		float damping = clamp01(entry.getProperty(DAMPING));
        float upBias = Math.max(0.0F, entry.getProperty(UPWARD_BIAS));

		// Choose the tagged entity most aligned with the player's look, fallback to closest
		List<LivingEntity> tagged = BlackwhipTags.getTaggedEntities(player, maxDist);
		if (tagged.isEmpty()) return;

        // We intentionally avoid coupling to player look or scroll; pure lead behavior

		// Periodic debug header
        if (player.tickCount % 40 == 0) {
			BanditsQuirkLibForge.LOGGER.info("[PUPPET] player={} tagged={} k={} damp={}",
					player.getGameProfile().getName(), tagged.size(),
					String.format("%.2f", k), String.format("%.2f", damping));
        }

		// No look-based anchor; behave like a slack lead

        // No per-target offset or height memory needed

		Map<UUID, Double> tetherMap = PLAYER_TETHER_MAXLEN.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());

		for (LivingEntity t : tagged) {
			// Refresh tag TTL while controlling so it doesn't expire mid-control
			BlackwhipTags.addTagWithMaxDistance(player, t, 20, maxDist);

			// Per-entity fixed tether: capture current distance the first time this target is processed
			Vec3 toPlayer = player.position().subtract(t.position());
			double dist = toPlayer.length();
			UUID tid = t.getUUID();
			double maxLenForTarget = tetherMap.computeIfAbsent(tid, k2 -> Math.max(1.0, dist));

			// Only pull when stretched beyond stored length
			Vec3 vel = t.getDeltaMovement();
			Vec3 newVel = vel;
			if (dist > maxLenForTarget && dist > 1.0e-6) {
				double excess = dist - maxLenForTarget;
				Vec3 pullDir = toPlayer.scale(1.0 / dist);
				Vec3 accel = pullDir.scale(excess * k);
				newVel = new Vec3(
						vel.x * damping + accel.x,
						vel.y * damping + accel.y,
						vel.z * damping + accel.z
				);
			}

			t.setDeltaMovement(newVel);
			t.fallDistance = 0.0F;

			// Small server-side particles to provide feedback while controlling (lightweight)
			if (player.level() instanceof ServerLevel sl && player.tickCount % 12 == 0) {
				Vec3 p = t.position().add(0, t.getBbHeight() * 0.6, 0);
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
		}

		// If player stops controlling (handled upstream), offsets remain; they'll be rebuilt next time
	}

	@Override
	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		// When puppet control ends: clear tethers so whips can extend again; reset any UI bias
		if (entity instanceof ServerPlayer player) {
			PLAYER_TETHER_MAXLEN.remove(player.getUUID());
			try {
				BodyStatusHelper.setCustomFloat(player, "chest", "blackwhip_puppet_bias", 0.0F);
			} catch (Exception ignored) {}
		}
	}

	private static double dotSafe(Vec3 a, Vec3 b) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}

	private static float clamp01(float v) {
		if (v < 0f) return 0f;
		if (v > 1f) return 1f;
		return v;
	}
}
