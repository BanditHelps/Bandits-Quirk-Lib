package com.github.b4ndithelps.forge.abilities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.List;

public class ThrowHeldItemAbility extends Ability {

	public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage dealt on hit or proximity");
	public static final PalladiumProperty<Float> PROXIMITY_RADIUS = new FloatProperty("proximity_radius").configurable("Radius to still hit when near target");
	public static final PalladiumProperty<Integer> SPEED = new IntegerProperty("speed").configurable("Throw speed used for visuals (no entity)");
	public static final PalladiumProperty<Float> MAX_DISTANCE = new FloatProperty("max_distance").configurable("Max distance to travel while ray-stepping");

	public ThrowHeldItemAbility() {
		super();
		this.withProperty(DAMAGE, 6.0F)
				.withProperty(PROXIMITY_RADIUS, 0.6F)
				.withProperty(SPEED, 2)
				.withProperty(MAX_DISTANCE, 18.0F);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		if (!(player.level() instanceof ServerLevel level)) return;

		// Choose a hand with an item (prefer main hand)
		InteractionHand hand = InteractionHand.MAIN_HAND;
		ItemStack held = player.getItemInHand(hand);
		if (held.isEmpty()) {
			hand = InteractionHand.OFF_HAND;
			held = player.getItemInHand(hand);
		}
		if (held.isEmpty()) return; // nothing to throw

		// Copy one item to represent the thrown stack
		ItemStack one = held.copy();
		one.setCount(1);
		// Consume one from the hand immediately
		held.shrink(1);
		player.setItemInHand(hand, held);
		player.swing(hand, true);

		// Configs
		float damage = Math.max(0.0F, entry.getProperty(DAMAGE));
		float radius = Math.max(0.0F, entry.getProperty(PROXIMITY_RADIUS));
		int speed = Math.max(1, entry.getProperty(SPEED));

		Vec3 look = player.getLookAngle().normalize();
		Vec3 start = player.getEyePosition().add(look.scale(0.2));

		// Play throw sound
		level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.7F, 1.2F);

		// Spawn projectile entity
		com.github.b4ndithelps.forge.entities.ThrownHeldItemEntity proj =
				new com.github.b4ndithelps.forge.entities.ThrownHeldItemEntity(level, player, one, damage, radius);
		proj.setPos(start.x, start.y, start.z);
		proj.setDeltaMovement(look.scale(speed));
		level.addFreshEntity(proj);
	}

	@Override
	public String getDocumentationDescription() {
		return "Consumes one item from the player's hand and throws it in the look direction. "
			 + "Spawns a visible projectile of the item that damages on hit or near-miss.";
	}
}


