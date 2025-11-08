package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("removal")
public class BlackwhipMoveTaggedAbility extends Ability {

	public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Max control distance to the player (0 = unlimited)");
	public static final PalladiumProperty<Float> BASE_DISTANCE = new FloatProperty("base_distance").configurable("Base distance in front of the player");
	public static final PalladiumProperty<Float> CARRY_HEIGHT = new FloatProperty("carry_height").configurable("Vertical offset for the controlled target");
	public static final PalladiumProperty<Float> SPRING_STIFFNESS = new FloatProperty("spring_stiffness").configurable("Pull strength toward desired position");
	public static final PalladiumProperty<Float> DAMPING = new FloatProperty("damping").configurable("Velocity damping (0..1)");
	public static final PalladiumProperty<Float> MAX_PULL_BIAS = new FloatProperty("max_pull_bias").configurable("Max absolute scroll bias applied to distance");
	public static final PalladiumProperty<Boolean> FACE_PLAYER = new BooleanProperty("face_player").configurable("Make target face the player");
	public static final PalladiumProperty<Float> HEALTH_SLOW_STRENGTH = new FloatProperty("health_slow_strength").configurable("How much higher-health targets resist being moved (0 = disabled)");
	public static final PalladiumProperty<Integer> RAMP_TICKS = new IntegerProperty("ramp_ticks").configurable("Ease-in time on activation for smoother start");

	// Runtime state
	public static final PalladiumProperty<Boolean> IS_ACTIVE = new BooleanProperty("is_active");
	public static final PalladiumProperty<String> CONTROLLED_TARGET = new StringProperty("controlled_target");
	public static final PalladiumProperty<Integer> ACTIVE_TICKS = new IntegerProperty("active_ticks");

	public BlackwhipMoveTaggedAbility() {
		super();
		this.withProperty(MAX_DISTANCE, 48)
				.withProperty(BASE_DISTANCE, 4.5F)
				.withProperty(CARRY_HEIGHT, 1.25F)
				.withProperty(SPRING_STIFFNESS, 0.45F)
				.withProperty(DAMPING, 0.75F)
				.withProperty(MAX_PULL_BIAS, 12.0F)
				.withProperty(FACE_PLAYER, true)
				.withProperty(HEALTH_SLOW_STRENGTH, 1.0F)
				.withProperty(RAMP_TICKS, 8);
	}

	@Override
	public void registerUniqueProperties(PropertyManager manager) {
		manager.register(IS_ACTIVE, false);
		manager.register(CONTROLLED_TARGET, "");
		manager.register(ACTIVE_TICKS, 0);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		entry.setUniqueProperty(IS_ACTIVE, true);
		entry.setUniqueProperty(CONTROLLED_TARGET, "");
		entry.setUniqueProperty(ACTIVE_TICKS, 0);

		// Pick best currently tagged target to begin controlling
		LivingEntity best = chooseBestTagged(player, Math.max(0, entry.getProperty(MAX_DISTANCE)));
		if (best != null) {
			entry.setUniqueProperty(CONTROLLED_TARGET, best.getUUID().toString());
		}
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;
		if (!enabled || !Boolean.TRUE.equals(entry.getProperty(IS_ACTIVE))) return;

		// advance active ticks for ramp-in
		int activeTicks = Math.max(0, entry.getProperty(ACTIVE_TICKS));
		entry.setUniqueProperty(ACTIVE_TICKS, activeTicks + 1);

		int maxDist = Math.max(0, entry.getProperty(MAX_DISTANCE));

		// Resolve current controlled target; if missing, reacquire
		LivingEntity target = resolveControlled(player, entry, maxDist);
		if (target == null) return;

		// Respect external immunity during control
		if (target.getTags().contains("MineHa.GrabProof")) {
			entry.setUniqueProperty(CONTROLLED_TARGET, "");
			return;
		}

		// Break if too far
		if (maxDist > 0 && target.position().distanceTo(player.position()) > maxDist) {
			entry.setUniqueProperty(CONTROLLED_TARGET, "");
			return;
		}

		// Read scroll bias from body status; clamp to configured max
		float maxBias = Math.max(0.0F, entry.getProperty(MAX_PULL_BIAS));
		float bias = BodyStatusHelper.getCustomFloat(player, "chest", "blackwhip_move_bias");
		if (bias > maxBias) bias = maxBias;
		if (bias < -maxBias) bias = -maxBias;

		// Desired anchor in front of player
		Vec3 playerPos = player.position();
		Vec3 look = player.getLookAngle();
		float desiredDistance = Math.max(0.5F, entry.getProperty(BASE_DISTANCE) + bias);
		float height = entry.getProperty(CARRY_HEIGHT);
		Vec3 desired = playerPos.add(look.scale(desiredDistance)).add(0, height, 0);

		// Spring-damper toward desired
		Vec3 toDesired = desired.subtract(target.position());
		double dist = toDesired.length();
		if (dist > 1.0e-6) {
			Vec3 dir = toDesired.scale(1.0 / dist);
			float k = Math.max(0.0F, entry.getProperty(SPRING_STIFFNESS));

			// Slowdown factor based on target max health (higher HP => lower k)
			float healthStrength = Math.max(0.0F, entry.getProperty(HEALTH_SLOW_STRENGTH));
			if (healthStrength > 0.0F && target.getMaxHealth() > 0.0F) {
				float scale = (float) (target.getMaxHealth() / 20.0F); // 20 = baseline (player/zombie)
				float slow = 1.0F / (1.0F + healthStrength * scale);
				k *= clamp01(slow);
			}

			// Ease-in ramp on activation
			int rampTicks = Math.max(1, entry.getProperty(RAMP_TICKS));
			float ramp = clamp01((float) Math.min(activeTicks, rampTicks) / (float) rampTicks);
			k *= ramp;

			float damping = clamp01(entry.getProperty(DAMPING));
			Vec3 vel = target.getDeltaMovement();
			Vec3 accel = dir.scale(dist * k);
			Vec3 newVel = new Vec3(
					vel.x * damping + accel.x,
					vel.y * damping + accel.y,
					vel.z * damping + accel.z
			);
			target.setDeltaMovement(newVel);
			target.fallDistance = 0.0F;
			target.setOnGround(false);
			if (Boolean.TRUE.equals(entry.getProperty(FACE_PLAYER))) {
				target.setYRot(player.getYRot());
				target.setXRot(0);
			}
			// Lightweight feedback
			if (player.level() instanceof ServerLevel sl && player.tickCount % 6 == 0) {
				Vec3 p = target.position().add(0, target.getBbHeight() * 0.6, 0);
				sl.sendParticles(ParticleTypes.ASH, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
			}
		}

		// Keep tag refreshed while actively controlling
		BlackwhipTags.addTagWithMaxDistance(player, target, 20, maxDist);
	}

	@Override
	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		entry.setUniqueProperty(IS_ACTIVE, false);
		entry.setUniqueProperty(CONTROLLED_TARGET, "");
		entry.setUniqueProperty(ACTIVE_TICKS, 0);
		if (entity instanceof ServerPlayer player) {
			try {
				BodyStatusHelper.setCustomFloat(player, "chest", "blackwhip_move_bias", 0.0F);
			} catch (Exception ignored) {}
		}
	}

	private LivingEntity chooseBestTagged(ServerPlayer player, int maxDist) {
		List<LivingEntity> tagged = BlackwhipTags.getTaggedEntities(player, maxDist);
		if (tagged.isEmpty()) return null;
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		return tagged.stream().min(Comparator.comparingDouble(t -> {
			Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
			double len = Math.max(1.0e-6, to.length());
			double cos = look.dot(to) / len;
			// Prefer high alignment and nearness
			double alignScore = 1.0 - Math.max(-1.0, Math.min(1.0, cos));
			double near = eye.distanceTo(t.position());
			return alignScore * 2.0 + near * 0.02;
		})).orElse(null);
	}

	private LivingEntity resolveControlled(ServerPlayer player, AbilityInstance entry, int maxDist) {
		String id = entry.getProperty(CONTROLLED_TARGET);
		if (id != null && !id.isEmpty()) {
			try {
				UUID uuid = UUID.fromString(id);
				List<LivingEntity> candidates = BlackwhipTags.getTaggedEntities(player, maxDist);
				for (LivingEntity e : candidates) {
					if (uuid.equals(e.getUUID())) return e;
				}
			} catch (IllegalArgumentException ignored) {
				entry.setUniqueProperty(CONTROLLED_TARGET, "");
			}
		}
		LivingEntity reacquired = chooseBestTagged(player, maxDist);
		if (reacquired != null) {
			entry.setUniqueProperty(CONTROLLED_TARGET, reacquired.getUUID().toString());
		}
		return reacquired;
	}

	private static float clamp01(float v) {
		if (v < 0f) return 0f;
		if (v > 1f) return 1f;
		return v;
	}
}