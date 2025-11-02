package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.effects.ModEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.BooleanProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;

import java.util.List;
import java.util.UUID;

/**
 * Blackwhip Restrain: Shoots a green-blue tendril in the look direction.
 * If it hits a living target, applies a strong restraining effect and keeps
 * a visible whip tether for the duration. If it misses, the whip visually
 * extends to max range and retracts.
 */
@SuppressWarnings("removal")
public class BlackwhipRestrainAbility extends Ability {

	// Configurable properties
	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum reach of the whip");
	public static final PalladiumProperty<Integer> RESTRAIN_TICKS = new IntegerProperty("restrain_ticks").configurable("Duration (ticks) to restrain the target on hit");
	public static final PalladiumProperty<Integer> MISS_RETRACT_TICKS = new IntegerProperty("miss_retract_ticks").configurable("How many ticks the retract animation lasts on a miss");
	public static final PalladiumProperty<Float> WHIP_CURVE = new FloatProperty("whip_curve").configurable("How much the whip arcs (visual only)");
	public static final PalladiumProperty<Float> WHIP_PARTICLE_SIZE = new FloatProperty("whip_particle_size").configurable("Dust particle size for the whip visuals");

	// Runtime state (unique properties)
	public static final PalladiumProperty<String> TARGET_UUID = new net.threetag.palladium.util.property.StringProperty("target_uuid");
	public static final PalladiumProperty<Boolean> IS_ACTIVE = new net.threetag.palladium.util.property.BooleanProperty("is_active");
	public static final PalladiumProperty<Boolean> IS_RESTRAINING = new net.threetag.palladium.util.property.BooleanProperty("is_restraining");
	public static final PalladiumProperty<Integer> TICKS_LEFT = new IntegerProperty("ticks_left");

	public BlackwhipRestrainAbility() {
		super();
		this.withProperty(RANGE, 16.0F)
 				.withProperty(RESTRAIN_TICKS, 100)
 				.withProperty(MISS_RETRACT_TICKS, 8)
 				.withProperty(WHIP_CURVE, 0.6F)
 				.withProperty(WHIP_PARTICLE_SIZE, 1.0F);
	}

	@Override
	public void registerUniqueProperties(PropertyManager manager) {
		manager.register(TARGET_UUID, "");
		manager.register(IS_ACTIVE, false);
		manager.register(IS_RESTRAINING, false);
		manager.register(TICKS_LEFT, 0);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		// Start the whip action on activation
		entry.setUniqueProperty(IS_ACTIVE, true);
		entry.setUniqueProperty(IS_RESTRAINING, false);
		entry.setUniqueProperty(TARGET_UUID, "");

		float range = Math.max(1.0F, entry.getProperty(RANGE));

		// Raycast-like entity search along look direction
		LivingEntity target = findLineTarget(player, range);

		if (target != null && target != player) {
 			// Respect generic grab-proof tags if present
 			if (target.getTags().contains("MineHa.GrabProof")) {
 				playWhipMiss(player, entry, range);
 				return;
 			}

 			// Apply restrain effect and store state
 			int duration = Math.max(1, entry.getProperty(RESTRAIN_TICKS));
 			applyRestrain(target, duration);

 			entry.setUniqueProperty(IS_RESTRAINING, true);
 			entry.setUniqueProperty(TICKS_LEFT, duration);
 			entry.setUniqueProperty(TARGET_UUID, target.getUUID().toString());

 			// Audio feedback on successful tether
			player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 0.5f, 1.4f);
 		} else {
 			// Miss: play retract visuals for a short time
 			playWhipMiss(player, entry, range);
	}

	}

 	@Override
 	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
 		if (!(entity instanceof ServerPlayer player)) return;
 		if (!entry.getProperty(IS_ACTIVE)) return;

 		ServerLevel level = player.serverLevel();

 		String uuidStr = entry.getProperty(TARGET_UUID);
 		boolean restraining = entry.getProperty(IS_RESTRAINING);
 		int ticksLeft = entry.getProperty(TICKS_LEFT);

 		if (restraining && !uuidStr.isEmpty()) {
 			LivingEntity target = findEntityByUuid(player, uuidStr);
 			if (target == null || !target.isAlive()) {
 				// Target lost or dead: stop
 				finish(entry);
 				return;
 			}


			// Keep the tether visuals each tick from player's hand to target
			Vec3 hand = getHandPosition(player);
			spawnWhipParticles(level, hand, getAttachPoint(target), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE));

			// Wrapped helical effect around the restrained target
			spawnWrappedEffect(level, target, entry.getProperty(WHIP_PARTICLE_SIZE));

 			// Refresh restrain a little to keep effect consistent (not extending duration)
 			if (ticksLeft % 20 == 0) {
 				target.addEffect(new MobEffectInstance(ModEffects.STUN_EFFECT.get(), 25, 0, false, false));
 			}

 			if (ticksLeft <= 0) {
 				finish(entry);
 				return;
 			}

 			entry.setUniqueProperty(TICKS_LEFT, ticksLeft - 1);
 		} else {
 			// Miss retract animation: TICKS_LEFT counts down visual frames
 			if (ticksLeft <= 0) {
 				finish(entry);
 				return;
 			}

 			float range = Math.max(1.0F, entry.getProperty(RANGE));
 			Vec3 start = player.getEyePosition();
 			Vec3 end = start.add(player.getLookAngle().scale(range));

 			// Shorten the whip over time for retract effect
 			float retractRatio = Math.max(0.0F, Math.min(1.0F, (float) ticksLeft / Math.max(1, entry.getProperty(MISS_RETRACT_TICKS))));
 			Vec3 currentEnd = start.add(end.subtract(start).scale(retractRatio));

 			spawnWhipParticles(level, start, currentEnd, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE));
 			entry.setUniqueProperty(TICKS_LEFT, ticksLeft - 1);
 		}
 	}

 	@Override
 	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
 		finish(entry);
 	}

 	private void finish(AbilityInstance entry) {
 		entry.setUniqueProperty(IS_ACTIVE, false);
 		entry.setUniqueProperty(IS_RESTRAINING, false);
 		entry.setUniqueProperty(TICKS_LEFT, 0);
 		entry.setUniqueProperty(TARGET_UUID, "");
 	}

 	private void playWhipMiss(ServerPlayer player, AbilityInstance entry, float range) {
 		entry.setUniqueProperty(IS_RESTRAINING, false);
 		entry.setUniqueProperty(TICKS_LEFT, Math.max(1, entry.getProperty(MISS_RETRACT_TICKS)));
 		entry.setUniqueProperty(TARGET_UUID, "");

 		// Sound and initial burst
		player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 1.2f);

 		// Show the fully extended whip for one frame on miss
 		ServerLevel level = player.serverLevel();
 		Vec3 start = player.getEyePosition();
 		Vec3 end = start.add(player.getLookAngle().scale(range));
 		spawnWhipParticles(level, start, end, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE));
 	}

 	private void applyRestrain(LivingEntity target, int duration) {
 		// Core immobilization via our Stun effect
 		target.addEffect(new MobEffectInstance(ModEffects.STUN_EFFECT.get(), duration, 0, false, false));

 		// Small impact burst where hit occurs
 		if (target.level() instanceof ServerLevel serverLevel) {
 			Vec3 p = getAttachPoint(target);
 			serverLevel.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 8, 0.1, 0.1, 0.1, 0.02);
 		}
 	}

	private Vec3 getHandPosition(ServerPlayer player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 up = new Vec3(0, 1, 0);
		Vec3 right = look.cross(up);
		if (right.lengthSqr() < 1.0e-6) {
			right = new Vec3(1, 0, 0);
		}
		right = right.normalize();
		float sideDir = player.getMainArm() == HumanoidArm.RIGHT ? 1.0f : -1.0f;
		// Slightly below eyes and offset sideways to approximate hand position
		return eye.add(0, -0.2, 0).add(right.scale(0.35 * sideDir));
	}

 	private LivingEntity findLineTarget(ServerPlayer player, float range) {
 		Vec3 eye = player.getEyePosition();
 		Vec3 look = player.getLookAngle();
 		Vec3 end = eye.add(look.scale(range));

 		LivingEntity best = null;
 		double bestDist = range + 1.0;

 		// Check entities along a swept AABB corridor
 		AABB corridor = new AABB(eye, end).inflate(1.0);
 		List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, corridor, e -> e != player && e.isAlive());
 		for (LivingEntity e : candidates) {
 			AABB bb = e.getBoundingBox().inflate(0.25);
 			Vec3 hit = bb.clip(eye, end).orElse(null);
 			if (hit != null) {
 				double d = eye.distanceTo(hit);
 				if (d < bestDist) {
 					bestDist = d;
 					best = e;
 				}
 			}
 		}
 		return best;
 	}

 	private LivingEntity findEntityByUuid(ServerPlayer player, String uuidStr) {
 		try {
 			UUID uuid = UUID.fromString(uuidStr);
 			List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64), e -> e.getUUID().equals(uuid));
 			return list.isEmpty() ? null : list.get(0);
 		} catch (IllegalArgumentException ex) {
 			return null;
 		}
 	}

 	private Vec3 getAttachPoint(LivingEntity target) {
 		return target.position().add(0, target.getBbHeight() * 0.6, 0);
 	}

 	private void spawnWhipParticles(ServerLevel level, Vec3 start, Vec3 end, float curveAmount, float particleSize) {
 		// Color: vibrant teal-blue-green similar to Blackwhip
 		org.joml.Vector3f color = new org.joml.Vector3f(0.1f, 0.95f, 0.85f);
 		DustParticleOptions dust = new DustParticleOptions(color, Math.max(0.2f, particleSize));

 		Vec3 dir = end.subtract(start);
 		double length = dir.length();
 		if (length < 1.0e-4) return;

 		dir = dir.scale(1.0 / length);

 		// Build a simple arcing Bezier: start -> control -> end
 		// Control point offset: some sideways + upward relative to direction
 		Vec3 up = new Vec3(0, 1, 0);
 		Vec3 side = up.cross(dir);
 		if (side.lengthSqr() < 1.0e-6) {
 			side = new Vec3(1, 0, 0).cross(dir);
 		}
 		side = side.normalize();

 		double arc = length * curveAmount * 0.25;
 		Vec3 control = start.add(dir.scale(length * 0.5)).add(up.scale(arc * 0.6)).add(side.scale(arc * 0.4));

 		int segments = Math.max(10, (int) Math.min(48, length * 2.0));
 		for (int i = 0; i <= segments; i++) {
 			double t = i / (double) segments;
 			Vec3 p = cubicBezier(start, control, control, end, t);
 			// Main strand
 			level.sendParticles(dust, p.x, p.y, p.z, 1, 0, 0, 0, 0.0);

 			// Subtle glow and spark for energy look
			if ((i % 6) == 0) {
				level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.01, 0.01, 0.01, 0.01);
			}
 		}

	}

	private void spawnWrappedEffect(ServerLevel level, LivingEntity target, float particleSize) {
		// Teal dust color consistent with the whip
		org.joml.Vector3f color = new org.joml.Vector3f(0.1f, 0.95f, 0.85f);
		DustParticleOptions dust = new DustParticleOptions(color, Math.max(0.2f, particleSize * 0.9f));

		double height = target.getBbHeight();
		double radius = Math.max(target.getBbWidth() * 0.5, 0.4) + 0.25;
		int points = 24;
		double wraps = 2.5; // number of turns around the target
		double timePhase = (target.tickCount % 20) / 20.0 * (2 * Math.PI);

		Vec3 base = target.position();
		for (int i = 0; i < points; i++) {
			double t = i / (double) (points - 1);
			double angle = t * (2 * Math.PI) * wraps + timePhase;
			double y = base.y + (0.15 * height) + t * (0.7 * height);
			double x = base.x + Math.cos(angle) * radius;
			double z = base.z + Math.sin(angle) * radius;
			level.sendParticles(dust, x, y, z, 1, 0, 0, 0, 0.0);
			if ((i % 8) == 0) {
				level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.005, 0.005, 0.005, 0.005);
			}
		}

 	}

 	private Vec3 cubicBezier(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
 		double it = 1.0 - t;
 		double b0 = it * it * it;
 		double b1 = 3 * it * it * t;
 		double b2 = 3 * it * t * t;
 		double b3 = t * t * t;
 		double x = b0 * p0.x + b1 * p1.x + b2 * p2.x + b3 * p3.x;
 		double y = b0 * p0.y + b1 * p1.y + b2 * p2.y + b3 * p3.y;
 		double z = b0 * p0.z + b1 * p1.z + b2 * p2.z + b3 * p3.z;
 		return new Vec3(x, y, z);
 	}

 	@Override
 	public String getDocumentationDescription() {
 		return "Shoots a teal-green Blackwhip tendril; on hit restrains a living target with a stun-like immobilization and renders a tether between the player and target for the duration. On miss, the whip visually extends and retracts.";
 	}
}


