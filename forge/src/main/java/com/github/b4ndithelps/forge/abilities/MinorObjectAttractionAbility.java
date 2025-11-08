package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

/**
 * Pulls ground items that are along the player's look direction toward the player.
 * Only items near the look ray within a configurable width are affected.
 */
@SuppressWarnings("removal")
public class MinorObjectAttractionAbility extends Ability {

	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum distance to search along look direction");
	public static final PalladiumProperty<Float> BEAM_WIDTH = new FloatProperty("beam_width").configurable("Half-width of look ray influence (meters)");
	public static final PalladiumProperty<Float> PULL_STRENGTH = new FloatProperty("pull_strength").configurable("Added velocity toward player each tick");

	public MinorObjectAttractionAbility() {
		super();
		this.withProperty(RANGE, 6.0F)
				.withProperty(BEAM_WIDTH, 0.75F)
				.withProperty(PULL_STRENGTH, 0.25F);
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) {
			return;
		}
		if (!(entity instanceof ServerPlayer player)) {
			return;
		}
		if (!(player.level() instanceof ServerLevel)) {
			return;
		}

		float range = entry.getProperty(RANGE);
		float beamWidth = Math.max(0.05F, entry.getProperty(BEAM_WIDTH));
		float pullStrength = Math.max(0.01F, entry.getProperty(PULL_STRENGTH));

		range += (float) QuirkFactorHelper.getQuirkFactor(player);
		
		Vec3 eyePos = player.getEyePosition();
		Vec3 look = player.getLookAngle().normalize();
		Vec3 endPos = eyePos.add(look.scale(range));

		// Broad-phase: items in a capsule-like AABB along the look ray
		AABB searchBox = new AABB(eyePos, endPos).inflate(beamWidth + 0.5);
		List<ItemEntity> candidates = player.level().getEntitiesOfClass(ItemEntity.class, searchBox, item -> item.isAlive());

		if (candidates.isEmpty()) {
			return;
		}

		Vec3 pullTarget = player.position().add(0, 1.0, 0); // aim near player's torso

		for (ItemEntity item : candidates) {
			Vec3 toItem = item.position().subtract(eyePos);
			double along = toItem.dot(look);
			if (along <= 0 || along > range) {
				continue; // behind player or beyond range
			}

			// Lateral distance from the look line
			double lateralSqr = toItem.lengthSqr() - (along * along);
			if (lateralSqr < 0) {
				lateralSqr = 0; // numerical safety
			}
			double lateral = Math.sqrt(lateralSqr);

			// Allow small extra based on item size
			double allowed = beamWidth + (item.getBbWidth() * 0.5);
			if (lateral > allowed) {
				continue; // not near the look ray
			}

			// Apply pull toward player
			Vec3 toPlayer = pullTarget.subtract(item.position());
			if (toPlayer.lengthSqr() < 1.0e-6) {
				continue;
			}
			Vec3 desired = toPlayer.normalize().scale(pullStrength);
			// damp existing motion a bit to keep it controllable
			Vec3 newDelta = item.getDeltaMovement().scale(0.85).add(desired);
			item.setDeltaMovement(newDelta);
			item.hasImpulse = true; // mark moved so client updates promptly
		}
	}

	@Override
	public String getDocumentationDescription() {
		return "Attracts ground items that lie along the player's look direction within a narrow beam, pulling only those the player is aiming at toward them.";
	}
}