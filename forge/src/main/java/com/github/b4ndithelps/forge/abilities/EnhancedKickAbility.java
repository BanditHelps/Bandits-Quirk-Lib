package com.github.b4ndithelps.forge.abilities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

public class EnhancedKickAbility extends Ability {

	public static final PalladiumProperty<Float> BONUS_DAMAGE = new FloatProperty("bonus_damage").configurable("Extra damage added on top of normal attack");
	public static final PalladiumProperty<Float> EXTRA_KNOCKBACK = new FloatProperty("extra_knockback").configurable("Additional knockback strength");

	public EnhancedKickAbility() {
		super();
		this.withProperty(BONUS_DAMAGE, 1.5F)
				.withProperty(EXTRA_KNOCKBACK, 0.6F);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		if (!(player.level() instanceof ServerLevel)) return;

		// Swing main hand for animation (reuse arm swing)
		player.swing(InteractionHand.MAIN_HAND);

		float reach = player.getAbilities().instabuild ? 5.0F : 3.0F;
		LivingEntity target = findTargetEntity(player, reach);
		if (target == null || target == player || !target.isAlive()) return;

		// Perform normal attack first
		player.attack(target);

		// Apply a bit of bonus damage
		float extra = Math.max(0.0F, entry.getProperty(BONUS_DAMAGE));
		if (extra > 0.0F && target.isAlive()) {
			target.hurt(player.damageSources().playerAttack(player), extra);
		}

		// Add extra knockback
		if (target.isAlive()) {
			float kb = Math.max(0.0F, entry.getProperty(EXTRA_KNOCKBACK));
			if (kb > 0.0F) {
				Vec3 dir = target.position().subtract(player.position());
				if (dir.lengthSqr() > 1.0E-4) {
					Vec3 knock = dir.normalize().scale(kb * 0.5).add(0, 0.1F * kb, 0);
					target.setDeltaMovement(target.getDeltaMovement().add(knock));
				}
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
		return "Performs a normal kick (standard attack and swing) then applies small bonus damage and configurable extra knockback. No stun.";
	}
}


