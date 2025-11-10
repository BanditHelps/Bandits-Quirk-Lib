package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("removal")
public class BlackwhipRestrictAbility extends Ability {

	public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Max distance to consider tags valid (0 = any)");
	public static final PalladiumProperty<Float> SPRING_STIFFNESS = new FloatProperty("spring_stiffness").configurable("How strongly the target is pulled toward the anchor");
	public static final PalladiumProperty<Float> DAMPING = new FloatProperty("damping").configurable("Velocity damping while controlled (0..1, higher = more damping)");
    public static final PalladiumProperty<Float> UPWARD_BIAS = new FloatProperty("upward_bias").configurable("Extra vertical bias when looking up");

	public BlackwhipRestrictAbility() {
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

		// Choose the tagged entity most aligned with the player's look, fallback to closest
		List<LivingEntity> tagged = BlackwhipTags.getTaggedEntities(player, maxDist);
		if (tagged.isEmpty()) return;

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

			// Explicitly sync motion to player clients; mob motion syncs automatically, but players need a packet
			if (t instanceof ServerPlayer targetPlayer && t.level() instanceof ServerLevel targetLevel) {
				targetLevel.getChunkSource().broadcastAndSend(targetPlayer, new ClientboundSetEntityMotionPacket(targetPlayer));
			}

			// Small server-side particles to provide feedback while controlling (lightweight)
			if (player.level() instanceof ServerLevel sl && player.tickCount % 12 == 0) {
				Vec3 p = t.position().add(0, t.getBbHeight() * 0.6, 0);
				sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
		}
	}

	@Override
	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		// When puppet control ends: clear tethers so whips can extend again; reset any UI bias
		if (entity instanceof ServerPlayer player) {
			PLAYER_TETHER_MAXLEN.remove(player.getUUID());
		}
	}

	private static float clamp01(float v) {
		if (v < 0f) return 0f;
		if (v > 1f) return 1f;
		return v;
	}
}