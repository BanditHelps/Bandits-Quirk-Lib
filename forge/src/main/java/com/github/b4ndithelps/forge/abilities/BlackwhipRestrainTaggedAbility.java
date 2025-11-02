package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.effects.ModEffects;
import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

@SuppressWarnings("removal")
public class BlackwhipRestrainTaggedAbility extends Ability {

	public static final PalladiumProperty<Integer> DURATION_TICKS = new IntegerProperty("restrain_ticks").configurable("Duration (ticks) to restrain each tagged target");
	public static final PalladiumProperty<Integer> MAX_TARGETS = new IntegerProperty("max_targets").configurable("Maximum number of tagged targets to affect (0 = all)");
	public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Maximum distance to consider tags valid (0 = any)");

	public BlackwhipRestrainTaggedAbility() {
		super();
		this.withProperty(DURATION_TICKS, 100)
				.withProperty(MAX_TARGETS, 0)
				.withProperty(MAX_DISTANCE, 48);
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		int duration = Math.max(1, entry.getProperty(DURATION_TICKS));
		int maxTargets = Math.max(0, entry.getProperty(MAX_TARGETS));
		int maxDist = Math.max(0, entry.getProperty(MAX_DISTANCE));

		List<LivingEntity> targets = BlackwhipTags.consumeTags(player, maxTargets, maxDist);
		if (targets.isEmpty()) return;
        for (LivingEntity t : targets) {
			// Apply stun/immobilize
			t.addEffect(new MobEffectInstance(ModEffects.STUN_EFFECT.get(), duration, 0, false, false));
			// Small visual to acknowledge action
			if (t.level() instanceof ServerLevel sl) {
				AABB bb = t.getBoundingBox();
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
						(bb.minX + bb.maxX) * 0.5, t.getY(0.5), (bb.minZ + bb.maxZ) * 0.5,
						6, 0.15, 0.15, 0.15, 0.01);
			}
		}
        // visuals updated by consumeTags sync
	}
}


