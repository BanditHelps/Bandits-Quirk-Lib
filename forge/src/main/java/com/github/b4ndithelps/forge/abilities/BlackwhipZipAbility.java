package com.github.b4ndithelps.forge.abilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

@SuppressWarnings("removal")
public class BlackwhipZipAbility extends Ability {

	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Max grapple range");
	public static final PalladiumProperty<Float> PULL_SPEED = new FloatProperty("pull_speed").configurable("Pull speed toward anchor");
	public static final PalladiumProperty<Integer> MAX_TICKS = new IntegerProperty("max_ticks").configurable("Max duration to apply pull (0 = instant impulse)");

	public BlackwhipZipAbility() {
		super();
		this.withProperty(RANGE, 24.0F)
				.withProperty(PULL_SPEED, 1.2F)
				.withProperty(MAX_TICKS, 0);
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		float range = Math.max(1.0F, entry.getProperty(RANGE));
		float speed = Math.max(0.1F, entry.getProperty(PULL_SPEED));
		int ticks = Math.max(0, entry.getProperty(MAX_TICKS));

		BlockHitResult hit = player.level().clip(new ClipContext(
				player.getEyePosition(),
				player.getEyePosition().add(player.getLookAngle().scale(range)),
				ClipContext.Block.OUTLINE,
				ClipContext.Fluid.NONE,
				player));
		if (hit.getType() != HitResult.Type.BLOCK) return;

		Vec3 anchor = Vec3.atCenterOf(hit.getBlockPos());
		Vec3 dir = anchor.subtract(player.position());
		if (dir.lengthSqr() < 1.0e-3) return;
		dir = dir.normalize();
		if (ticks <= 0) {
			Vec3 impulse = dir.scale(speed);
			player.setDeltaMovement(player.getDeltaMovement().scale(0.2).add(impulse));
		} else {
			Vec3 dm = player.getDeltaMovement();
			player.setDeltaMovement(dm.add(dir.scale(speed * 0.1)));
		}
		player.fallDistance = 0.0F;
		player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8f, 1.35f);
	}
}


