package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
public class BlackwhipSlamAbility extends Ability {

	public static final PalladiumProperty<Float> LIFT_FORCE = new FloatProperty("lift_force").configurable("Initial upward force before slam");
	public static final PalladiumProperty<Float> DOWN_FORCE = new FloatProperty("down_force").configurable("Downward slam force");
	public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage dealt on slam impact");
	public static final PalladiumProperty<Integer> MAX_TARGETS = new IntegerProperty("max_targets").configurable("Maximum number of tagged targets to affect (0 = all)");
	public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Maximum distance to consider tags valid (0 = any)");

	public BlackwhipSlamAbility() {
		super();
		this.withProperty(LIFT_FORCE, 0.35F)
				.withProperty(DOWN_FORCE, 1.2F)
				.withProperty(DAMAGE, 6.0F)
				.withProperty(MAX_TARGETS, 0)
				.withProperty(MAX_DISTANCE, 48);
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		float up = Math.max(0F, entry.getProperty(LIFT_FORCE));
		float down = Math.max(0.1F, entry.getProperty(DOWN_FORCE));
		float damage = Math.max(0F, entry.getProperty(DAMAGE));
		int maxTargets = Math.max(0, entry.getProperty(MAX_TARGETS));
		int maxDist = Math.max(0, entry.getProperty(MAX_DISTANCE));

		List<LivingEntity> targets = BlackwhipTags.consumeTags(player, maxTargets, maxDist);
		if (targets.isEmpty()) return;
		for (LivingEntity t : targets) {
			Vec3 dm = t.getDeltaMovement();
			// pop up slightly, then slam downward
			t.setDeltaMovement(dm.x * 0.5, Math.max(dm.y, up), dm.z * 0.5);
			// apply a brief delay then strong downward force by scheduling next tick movement — approximate by immediate extra downward push
			t.setDeltaMovement(t.getDeltaMovement().x * 0.7, t.getDeltaMovement().y - down, t.getDeltaMovement().z * 0.7);
			t.hurt(t.damageSources().playerAttack(player), damage);
			if (t.level() instanceof ServerLevel sl) {
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, t.getX(), t.getY(0.1), t.getZ(), 10, 0.25, 0.1, 0.25, 0.05);
				sl.playSound(null, t.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.2f);
			}
		}
		// visuals updated by consumeTags sync
	}
}


