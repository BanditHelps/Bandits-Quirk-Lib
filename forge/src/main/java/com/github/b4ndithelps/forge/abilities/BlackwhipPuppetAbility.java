package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.BlackwhipTags;
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
				.withProperty(MAX_LENGTH, 12.0F)
				.withProperty(SPRING_STIFFNESS, 0.35F)
				.withProperty(DAMPING, 0.8F)
				.withProperty(UPWARD_BIAS, 0.6F);
	}

	private static final Map<UUID, Map<Integer, Vec3>> PLAYER_TO_OFFSETS = new ConcurrentHashMap<>();

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		int maxDist = Math.max(0, entry.getProperty(MAX_DISTANCE));
		float maxLen = Math.max(1.0F, entry.getProperty(MAX_LENGTH));
		float k = Math.max(0.0F, entry.getProperty(SPRING_STIFFNESS));
		float damping = clamp01(entry.getProperty(DAMPING));
		float upBias = Math.max(0.0F, entry.getProperty(UPWARD_BIAS));

		// Choose the tagged entity most aligned with the player's look, fallback to closest
		List<LivingEntity> tagged = BlackwhipTags.getTaggedEntities(player, maxDist);
		if (tagged.isEmpty()) return;

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();

		// Anchor point: ahead of the player, clamped to max length, with extra lift if looking up
		double pitchRad = Math.toRadians(player.getXRot()); // positive pitch is looking down in MC, negative up
		float lookUpFactor = clamp01((float) -Math.sin(pitchRad)); // 0 when level/down, up to 1 when looking straight up
		Vec3 baseAnchor = eye.add(look.scale(maxLen));
		Vec3 anchorUp = new Vec3(0, lookUpFactor * upBias * maxLen * 0.25, 0);

		Map<Integer, Vec3> offsets = PLAYER_TO_OFFSETS.computeIfAbsent(player.getUUID(), uuidKey -> new ConcurrentHashMap<>());
		// prune offsets of non-tagged ids
		offsets.keySet().removeIf(id -> tagged.stream().noneMatch(le -> le.getId() == id));

		for (LivingEntity t : tagged) {
			// Refresh tag TTL while controlling so it doesn't expire mid-control
			BlackwhipTags.addTag(player, t, 20);

			// Compute a per-target desired anchor based on its captured offset relative to the player
			Vec3 rel = offsets.computeIfAbsent(t.getId(), id -> t.position().subtract(player.position()));
			Vec3 desired = player.position().add(rel).add(anchorUp);
			// Clamp desired within leash radius from player
			Vec3 fromPlayer = desired.subtract(player.position());
			double d = fromPlayer.length();
			if (d > maxLen && d > 1.0e-6) desired = player.position().add(fromPlayer.scale(maxLen / d));

			// Pull each target toward its own desired anchor using spring + damping
			Vec3 targetPos = t.position().add(0, t.getBbHeight() * 0.5, 0);
			Vec3 toAnchor = desired.subtract(targetPos);
			Vec3 vel = t.getDeltaMovement();

			// Apply leash constraint if beyond max length relative to player
			Vec3 leashVec = t.position().subtract(player.position());
			double leashLen = leashVec.length();
			if (leashLen > maxLen) {
				Vec3 pullDir = leashVec.normalize().scale(-(leashLen - maxLen));
				toAnchor = toAnchor.add(pullDir);
			}

			Vec3 accel = toAnchor.scale(k);
			Vec3 newVel = new Vec3(
					vel.x * damping + accel.x,
					vel.y * damping + accel.y,
					vel.z * damping + accel.z
			);
			// Nudge along player's motion a bit so moving player drags the target more convincingly
			newVel = newVel.add(player.getDeltaMovement().scale(0.15));

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

	private static double dotSafe(Vec3 a, Vec3 b) {
		return a.x * b.x + a.y * b.y + a.z * b.z;
	}

	private static float clamp01(float v) {
		if (v < 0f) return 0f;
		if (v > 1f) return 1f;
		return v;
	}
}
