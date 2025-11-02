package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipStatePacket;
import com.github.b4ndithelps.forge.systems.BlackwhipTags;
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
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;

import java.util.List;

@SuppressWarnings("removal")
public class BlackwhipTagAbility extends Ability {

	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum reach of the whip");
	public static final PalladiumProperty<Integer> TRAVEL_TICKS = new IntegerProperty("travel_ticks").configurable("Ticks the whip takes to reach a hit target");
	public static final PalladiumProperty<Integer> MISS_RETRACT_TICKS = new IntegerProperty("miss_retract_ticks").configurable("How many ticks the retract animation lasts on a miss");
	public static final PalladiumProperty<Float> WHIP_CURVE = new FloatProperty("whip_curve").configurable("How much the whip arcs (visual only)");
	public static final PalladiumProperty<Float> WHIP_PARTICLE_SIZE = new FloatProperty("whip_particle_size").configurable("Dust particle size for the whip visuals");
	public static final PalladiumProperty<Integer> TAG_EXPIRE_TICKS = new IntegerProperty("tag_expire_ticks").configurable("How long a tag remains usable");
	public static final PalladiumProperty<Boolean> PERSISTENT_TETHERS = new net.threetag.palladium.util.property.BooleanProperty("persistent_tethers").configurable("Render persistent tethers to tagged targets");
	public static final PalladiumProperty<Integer> MAX_PERSISTENT_TETHERS = new IntegerProperty("max_persistent_tethers").configurable("Max number of persistent tethers to keep (0 = unlimited)");

	public static final PalladiumProperty<Boolean> IS_ACTIVE = new net.threetag.palladium.util.property.BooleanProperty("is_active");
	public static final PalladiumProperty<String> TARGET_UUID = new net.threetag.palladium.util.property.StringProperty("target_uuid");
	public static final PalladiumProperty<Integer> TICKS_LEFT = new IntegerProperty("ticks_left");

	public BlackwhipTagAbility() {
		super();
		this.withProperty(RANGE, 18.0F)
				.withProperty(TRAVEL_TICKS, 6)
				.withProperty(MISS_RETRACT_TICKS, 8)
				.withProperty(WHIP_CURVE, 0.6F)
				.withProperty(WHIP_PARTICLE_SIZE, 1.0F)
				.withProperty(TAG_EXPIRE_TICKS, 200)
				.withProperty(PERSISTENT_TETHERS, true)
				.withProperty(MAX_PERSISTENT_TETHERS, 4);
	}

	@Override
	public void registerUniqueProperties(PropertyManager manager) {
		manager.register(IS_ACTIVE, false);
		manager.register(TARGET_UUID, "");
		manager.register(TICKS_LEFT, 0);
	}

	@Override
	public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!enabled) return;
		if (!(entity instanceof ServerPlayer player)) return;

		entry.setUniqueProperty(IS_ACTIVE, true);
		entry.setUniqueProperty(TARGET_UUID, "");
		entry.setUniqueProperty(TICKS_LEFT, 0);

		float range = Math.max(1.0F, entry.getProperty(RANGE));
		LivingEntity target = findLineTarget(player, range);
		if (target != null && target != player) {
			int travel = Math.max(1, entry.getProperty(TRAVEL_TICKS));
			entry.setUniqueProperty(TARGET_UUID, target.getUUID().toString());
			entry.setUniqueProperty(TICKS_LEFT, travel);

			// add tag immediately; visuals will travel
			BlackwhipTags.addTag(player, target, Math.max(1, entry.getProperty(TAG_EXPIRE_TICKS)), Math.max(0, entry.getProperty(MAX_PERSISTENT_TETHERS)));

			player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 1.35f);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipStatePacket(
							player.getId(), true, false, target.getId(),
							travel, entry.getProperty(MISS_RETRACT_TICKS),
							range, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));

			// update persistent multi-tethers if enabled
			if (entry.getProperty(PERSISTENT_TETHERS)) {
				BlackwhipTags.syncToClients(player, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE));
			}
		} else {
			// Miss
			entry.setUniqueProperty(TICKS_LEFT, Math.max(1, entry.getProperty(MISS_RETRACT_TICKS)));
			player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 1.1f);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipStatePacket(player.getId(), true, false, -1,
							entry.getProperty(TICKS_LEFT), entry.getProperty(MISS_RETRACT_TICKS),
							range, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
		}
	}

	@Override
	public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (!(entity instanceof ServerPlayer player)) return;
		if (!entry.getProperty(IS_ACTIVE)) return;
		int ticksLeft = entry.getProperty(TICKS_LEFT);
		if (ticksLeft <= 0) {
			finish(entry, player);
			return;
		}
		entry.setUniqueProperty(TICKS_LEFT, ticksLeft - 1);
	}

	@Override
	public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
		if (entity instanceof ServerPlayer player) {
			// end visuals quickly on clients
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipStatePacket(player.getId(), false, false, -1, 0, entry.getProperty(MISS_RETRACT_TICKS),
							entry.getProperty(RANGE), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
		}
		entry.setUniqueProperty(IS_ACTIVE, false);
		entry.setUniqueProperty(TARGET_UUID, "");
		entry.setUniqueProperty(TICKS_LEFT, 0);
	}

	private void finish(AbilityInstance entry, ServerPlayer player) {
		entry.setUniqueProperty(IS_ACTIVE, false);
		entry.setUniqueProperty(TARGET_UUID, "");
		entry.setUniqueProperty(TICKS_LEFT, 0);
		BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new BlackwhipStatePacket(player.getId(), false, false, -1, 0, entry.getProperty(MISS_RETRACT_TICKS),
						entry.getProperty(RANGE), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
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


