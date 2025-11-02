package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipStatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;

import java.util.List;

@SuppressWarnings("removal")
public class BlackwhipLashAbility extends Ability {

	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum lash reach");
	public static final PalladiumProperty<Integer> TRAVEL_TICKS = new IntegerProperty("travel_ticks").configurable("Ticks for lash to reach target");
	public static final PalladiumProperty<Integer> MISS_RETRACT_TICKS = new IntegerProperty("miss_retract_ticks").configurable("Ticks for miss retract visuals");
	public static final PalladiumProperty<Float> DAMAGE = new FloatProperty("damage").configurable("Damage on hit");
	public static final PalladiumProperty<Float> KNOCKBACK = new FloatProperty("knockback").configurable("Knockback strength");
	public static final PalladiumProperty<Float> WHIP_CURVE = new FloatProperty("whip_curve").configurable("Visual arc amount");
	public static final PalladiumProperty<Float> WHIP_PARTICLE_SIZE = new FloatProperty("whip_particle_size").configurable("Visual thickness");

	public static final PalladiumProperty<Boolean> IS_ACTIVE = new net.threetag.palladium.util.property.BooleanProperty("is_active");
	public static final PalladiumProperty<Integer> TICKS_LEFT = new IntegerProperty("ticks_left");

	public BlackwhipLashAbility() {
		super();
		this.withProperty(RANGE, 14.0F)
				.withProperty(TRAVEL_TICKS, 4)
				.withProperty(MISS_RETRACT_TICKS, 8)
				.withProperty(DAMAGE, 6.0F)
				.withProperty(KNOCKBACK, 0.6F)
				.withProperty(WHIP_CURVE, 0.5F)
				.withProperty(WHIP_PARTICLE_SIZE, 1.0F);
	}

	public void registerUniqueProperties(PropertyManager manager) {
		manager.register(IS_ACTIVE, false);
		manager.register(TICKS_LEFT, 0);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;
		entry.setUniqueProperty(IS_ACTIVE, true);
		entry.setUniqueProperty(TICKS_LEFT, 0);
		float range = Math.max(1.0F, entry.getProperty(RANGE));
		LivingEntity target = findLineTarget(player, range);
		if (target != null && target.isAlive()) {
			// visuals
			int travel = Math.max(1, entry.getProperty(TRAVEL_TICKS));
			entry.setUniqueProperty(TICKS_LEFT, travel);
			player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.2f);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipStatePacket(player.getId(), true, false, target.getId(), travel, entry.getProperty(MISS_RETRACT_TICKS),
							entry.getProperty(RANGE), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
			// apply impact immediately
			target.hurt(target.damageSources().playerAttack(player), Math.max(0F, entry.getProperty(DAMAGE)));
			Vec3 push = target.position().subtract(player.position()).normalize().scale(Math.max(0F, entry.getProperty(KNOCKBACK)));
			target.push(push.x, Math.min(0.5, push.length() * 0.5), push.z);
		} else {
			entry.setUniqueProperty(TICKS_LEFT, Math.max(1, entry.getProperty(MISS_RETRACT_TICKS)));
			player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.6f, 1.0f);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipStatePacket(player.getId(), true, false, -1, entry.getProperty(TICKS_LEFT), entry.getProperty(MISS_RETRACT_TICKS),
							entry.getProperty(RANGE), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
		}
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;
		if (!enabled && !entry.getProperty(IS_ACTIVE)) return;
		int t = entry.getProperty(TICKS_LEFT);
		if (t <= 0) {
			entry.setUniqueProperty(IS_ACTIVE, false);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipStatePacket(player.getId(), false, false, -1, 0, entry.getProperty(MISS_RETRACT_TICKS),
							entry.getProperty(RANGE), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
			return;
		}
		entry.setUniqueProperty(TICKS_LEFT, t - 1);
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


