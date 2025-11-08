package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

@SuppressWarnings("removal")
public class BlackwhipDetachAbility extends Ability {

	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum reach to select a tagged target");
	public static final PalladiumProperty<Float> WHIP_CURVE = new FloatProperty("whip_curve").configurable("How much the whip arcs (visual only)");
	public static final PalladiumProperty<Float> WHIP_PARTICLE_SIZE = new FloatProperty("whip_particle_size").configurable("Dust particle size for the whip visuals");

	public BlackwhipDetachAbility() {
		super();
		this.withProperty(RANGE, 18.0F)
				.withProperty(WHIP_CURVE, 0.6F)
				.withProperty(WHIP_PARTICLE_SIZE, 1.0F);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		float range = Math.max(1.0F, entry.getProperty(RANGE));
		LivingEntity target = findLineTarget(player, range);
		if (target == null || target == player) return;

		if (BlackwhipTags.removeTag(player, target)) {
			player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.7f, 1.2f);
			// visuals update via BlackwhipTags.syncToClients inside removeTag
		}
	}

	private LivingEntity findLineTarget(ServerPlayer player, float range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = eye.add(look.scale(range));
		LivingEntity best = null;
		double bestDist = range + 1.0;
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
}