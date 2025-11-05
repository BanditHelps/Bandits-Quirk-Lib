package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

@SuppressWarnings("removal")
public class BlackwhipLiftAbility extends Ability {

	public static final PalladiumProperty<Integer> LEVITATION_LEVEL = new IntegerProperty("levitation_level").configurable("Levitation amplifier (1=Levitation II)");
	public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Maximum distance to consider tags valid (0 = any)");
	public static final PalladiumProperty<Float> INITIAL_UPWARD_FORCE = new FloatProperty("initial_upward_force").configurable("Instant upward push applied to start the lift");

	public BlackwhipLiftAbility() {
		super();
		this.withProperty(LEVITATION_LEVEL, 1)
				.withProperty(MAX_DISTANCE, 48)
				.withProperty(INITIAL_UPWARD_FORCE, 0.4F);
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		int duration = 20;
		int amp = Math.max(0, entry.getProperty(LEVITATION_LEVEL) - 1);
		int maxDist = Math.max(0, entry.getProperty(MAX_DISTANCE));
		float up = Math.max(0F, entry.getProperty(INITIAL_UPWARD_FORCE));

		List<LivingEntity> targets = BlackwhipTags.getTaggedEntities(player, maxDist);
		if (targets.isEmpty()) return;
		for (LivingEntity t : targets) {
			if (up > 0) {
				Vec3 dm = t.getDeltaMovement();
				t.setDeltaMovement(dm.x * 0.5, Math.max(dm.y, up), dm.z * 0.5);
			}
			t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, duration, amp, false, false));
			if (t.level() instanceof ServerLevel sl) {
				sl.sendParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + 0.1, t.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
			}
		}
		// visuals updated by consumeTags sync
	}
}


