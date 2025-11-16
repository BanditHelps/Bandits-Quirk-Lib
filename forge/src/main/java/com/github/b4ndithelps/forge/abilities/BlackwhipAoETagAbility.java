package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipMultiBlockWhipPacket;
import com.github.b4ndithelps.forge.network.BlackwhipStatePacket;
import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Collections;

@SuppressWarnings("removal")
public class BlackwhipAoETagAbility extends Ability {

	public static final PalladiumProperty<Float> RANGE = new FloatProperty("range").configurable("Maximum radius to search for targets");
	public static final PalladiumProperty<Integer> TRAVEL_TICKS = new IntegerProperty("travel_ticks").configurable("Ticks the whips take to reach targets");
	public static final PalladiumProperty<Integer> MISS_RETRACT_TICKS = new IntegerProperty("miss_retract_ticks").configurable("How many ticks the retract animation lasts on a miss");
	public static final PalladiumProperty<Float> WHIP_CURVE = new FloatProperty("whip_curve").configurable("How much the whip arcs (visual only)");
	public static final PalladiumProperty<Float> WHIP_PARTICLE_SIZE = new FloatProperty("whip_particle_size").configurable("Dust particle size for the whip visuals");
	public static final PalladiumProperty<Integer> TAG_EXPIRE_TICKS = new IntegerProperty("tag_expire_ticks").configurable("How long a tag remains usable");
	public static final PalladiumProperty<Boolean> PERSISTENT_TETHERS = new BooleanProperty("persistent_tethers").configurable("Render persistent tethers to tagged targets");
	public static final PalladiumProperty<Integer> MAX_PERSISTENT_TETHERS = new IntegerProperty("max_persistent_tethers").configurable("Max number of persistent tethers to keep (0 = unlimited)");
	public static final PalladiumProperty<Integer> MAX_DISTANCE = new IntegerProperty("max_distance").configurable("Max distance before auto-break (0 = unlimited)");

	public static final PalladiumProperty<Boolean> IS_ACTIVE = new BooleanProperty("is_active");
	public static final PalladiumProperty<Integer> TICKS_LEFT = new IntegerProperty("ticks_left");

	public BlackwhipAoETagAbility() {
		super();
		this.withProperty(RANGE, 12.0F)
				.withProperty(TRAVEL_TICKS, 24)
				.withProperty(MISS_RETRACT_TICKS, 8)
				.withProperty(WHIP_CURVE, 0.6F)
				.withProperty(WHIP_PARTICLE_SIZE, 1.0F)
				.withProperty(TAG_EXPIRE_TICKS, 200)
				.withProperty(PERSISTENT_TETHERS, true)
				.withProperty(MAX_PERSISTENT_TETHERS, 4)
				.withProperty(MAX_DISTANCE, 48);
	}

	@Override
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
		int travel = Math.max(1, entry.getProperty(TRAVEL_TICKS));
		int expire = Math.max(1, entry.getProperty(TAG_EXPIRE_TICKS));
		int maxDist = Math.max(0, entry.getProperty(MAX_DISTANCE));

		// Determine max whips capacity including quirk factor bonus
		int maxKeep = Math.max(0, entry.getProperty(MAX_PERSISTENT_TETHERS));
		if (maxKeep > 0) {
			int quirkBonus = (int) Math.floor(QuirkFactorHelper.getQuirkFactor(player));
			if (quirkBonus > 0) {
				maxKeep += quirkBonus;
			}
		}
		// Per-cast visual whip limit should mirror the same cap as single-tag ability (including quirk bonus)
		int perCastLimit = (maxKeep > 0) ? maxKeep : Integer.MAX_VALUE;

		// Gather nearby candidates
		Vec3 pos = player.position();
		AABB box = new AABB(pos.x - range, pos.y - range, pos.z - range, pos.x + range, pos.y + range, pos.z + range);
		List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
		// Sort by distance ascending
		candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));

		// Enforce "up to the max number of whips" by limiting new tags based on existing count
		int currentCount = Math.max(0, (int) BodyStatusHelper.getCustomFloat(player, "chest", "blackwhip_connected_count"));
		int remainingSlots = maxKeep > 0 ? Math.max(0, maxKeep - currentCount) : Integer.MAX_VALUE;

		List<LivingEntity> selected = new ArrayList<>();
		for (LivingEntity c : candidates) {
			// Within sphere radius check
			if (c.distanceToSqr(pos) > (double)(range * range)) continue;
			if (remainingSlots <= 0) break;
			if (selected.size() >= perCastLimit) break;
			selected.add(c);
			if (remainingSlots != Integer.MAX_VALUE) remainingSlots -= 1;
		}

		if (!selected.isEmpty()) {
			// Tag all selected and enforce per-tag max distance; trimming to capacity is handled below if configured
			for (LivingEntity target : selected) {
				BlackwhipTags.addTagWithMaxDistance(player, target, expire, maxDist);
			}
			// If capacity is configured, use trimming logic by re-adding the last target with maxTags to prune oldest
			if (maxKeep > 0) {
				// Reuse trimming logic once; capacity was already respected, but this ensures hard cap if existing tags exceed
				LivingEntity last = selected.get(selected.size() - 1);
				BlackwhipTags.addTag(player, last, expire, maxKeep);
			}

			// Play visuals: multi-branch travel animation toward snapshot positions
			List<Double> xs = new ArrayList<>(selected.size());
			List<Double> ys = new ArrayList<>(selected.size());
			List<Double> zs = new ArrayList<>(selected.size());
			for (LivingEntity t : selected) {
				Vec3 p = t.position().add(0, t.getBbHeight() * 0.6, 0);
				xs.add(p.x);
				ys.add(p.y);
				zs.add(p.z);
			}
			player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 1.35f);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipMultiBlockWhipPacket(player.getId(), true, xs, ys, zs, travel, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));

			// Update persistent multi-tethers if enabled
			if (entry.getProperty(PERSISTENT_TETHERS)) {
				BlackwhipTags.syncToClients(player, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE));
			}

			entry.setUniqueProperty(TICKS_LEFT, travel);
		} else {
			// No targets — show a short miss retract for feedback
			int miss = Math.max(1, entry.getProperty(MISS_RETRACT_TICKS));
			entry.setUniqueProperty(TICKS_LEFT, miss);
			player.level().playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 1.1f);
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipStatePacket(player.getId(), true, false, -1,
							miss, miss,
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
			// End any client-side traveling/miss visuals quickly
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipMultiBlockWhipPacket(player.getId(), false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
							0, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
			BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
					new BlackwhipStatePacket(player.getId(), false, false, -1, 0, entry.getProperty(MISS_RETRACT_TICKS),
							entry.getProperty(RANGE), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
		}
		entry.setUniqueProperty(IS_ACTIVE, false);
		entry.setUniqueProperty(TICKS_LEFT, 0);
	}

	private void finish(AbilityInstance entry, ServerPlayer player) {
		entry.setUniqueProperty(IS_ACTIVE, false);
		entry.setUniqueProperty(TICKS_LEFT, 0);
		BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new BlackwhipMultiBlockWhipPacket(player.getId(), false, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
						0, entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
		BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
				new BlackwhipStatePacket(player.getId(), false, false, -1, 0, entry.getProperty(MISS_RETRACT_TICKS),
						entry.getProperty(RANGE), entry.getProperty(WHIP_CURVE), entry.getProperty(WHIP_PARTICLE_SIZE)));
	}
}