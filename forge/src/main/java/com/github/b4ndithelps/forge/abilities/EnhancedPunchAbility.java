package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.effects.ModEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

public class EnhancedPunchAbility extends Ability {

	public static final PalladiumProperty<Float> BONUS_DAMAGE = new FloatProperty("bonus_damage").configurable("Extra damage added on top of normal attack");
	public static final PalladiumProperty<Float> STUN_CHANCE = new FloatProperty("stun_chance").configurable("Chance (0.0-1.0) to apply stun on hit");
	public static final PalladiumProperty<Integer> STUN_DURATION = new IntegerProperty("stun_duration").configurable("Stun duration in ticks when applied");

	public EnhancedPunchAbility() {
		super();
		this.withProperty(BONUS_DAMAGE, 2.0F)
				.withProperty(STUN_CHANCE, 0.15F)
				.withProperty(STUN_DURATION, 40);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		if (!(player.level() instanceof ServerLevel)) return;



		// Swing main hand for animation (force client sync)
		player.swing(InteractionHand.MAIN_HAND, true);
		// Play vanilla attack sound
		player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.0f);

		// Use vanilla-like reach (approx.): survival 3.0, creative 5.0
		float reach = player.getAbilities().instabuild ? 5.0F : 3.0F;
		LivingEntity target = findTargetEntity(player, reach);
		if (target == null || target == player || !target.isAlive()) return;

		// Perform normal player attack
		player.attack(target);

		// Apply bonus damage on top
		float extra = Math.max(0.0F, entry.getProperty(BONUS_DAMAGE));
		if (extra > 0.0F && target.isAlive()) {
			target.hurt(player.damageSources().playerAttack(player), extra);
		}

		// Roll stun chance
		float chance = Math.max(0.0F, Math.min(1.0F, entry.getProperty(STUN_CHANCE)));
		int duration = Math.max(0, entry.getProperty(STUN_DURATION));
		if (chance > 0.0F && duration > 0 && target.isAlive() && player.getRandom().nextFloat() < chance) {
			if (ModEffects.STUN_EFFECT.get() != null) {
				target.addEffect(new MobEffectInstance(ModEffects.STUN_EFFECT.get(), duration, 0, false, true));
			}
		}
	}

	private LivingEntity findTargetEntity(Player player, float range) {
		Vec3 eyePos = player.getEyePosition();
		Vec3 lookVec = player.getLookAngle();
		Vec3 endPos = eyePos.add(lookVec.scale(range));

		EntityHitResult hit = null;
		double closest = range;

		List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class,
				new AABB(eyePos, endPos).inflate(1.0),
				e -> e != player && e.isAlive());

		for (LivingEntity e : entities) {
			if (e.getBoundingBox().inflate(0.3).clip(eyePos, endPos).isPresent()) {
				double dist = eyePos.distanceTo(e.position());
				if (dist < closest) {
					closest = dist;
					hit = new EntityHitResult(e);
				}
			}
		}

		return hit == null ? null : (LivingEntity) hit.getEntity();
	}

	@Override
	public String getDocumentationDescription() {
		return "Performs a normal punch using standard reach and swing, then adds configurable bonus damage with a chance to apply a short stun.";
	}
}


