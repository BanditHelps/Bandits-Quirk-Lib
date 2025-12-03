package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
public class ThrownItemEvents {

	private static final Map<ResourceKey<Level>, Map<Integer, ActiveThrow>> ACTIVE_THROWS = new ConcurrentHashMap<>();

	public static void track(ItemEntity entity, ServerPlayer owner, float damage, float radius, double maxDistance) {
		if (entity == null || owner == null) return;
		if (!(entity.level() instanceof ServerLevel serverLevel)) return;

		ActiveThrow data = new ActiveThrow(owner.getId(), Math.max(0.0F, damage), Math.max(0.0F, radius),
				Math.max(1.0D, maxDistance), entity.position(), serverLevel.getGameTime());
		ACTIVE_THROWS.computeIfAbsent(serverLevel.dimension(), key -> new ConcurrentHashMap<>())
				.put(entity.getId(), data);
	}

	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (!(event.level instanceof ServerLevel level) || event.phase != TickEvent.Phase.END) return;

		Map<Integer, ActiveThrow> throwsForLevel = ACTIVE_THROWS.get(level.dimension());
		if (throwsForLevel == null || throwsForLevel.isEmpty()) return;

		Iterator<Map.Entry<Integer, ActiveThrow>> iterator = throwsForLevel.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, ActiveThrow> entry = iterator.next();
			Entity entity = level.getEntity(entry.getKey());
			if (!(entity instanceof ItemEntity item) || !item.isAlive()) {
				iterator.remove();
				continue;
			}

			ActiveThrow data = entry.getValue();
			Vec3 currentPos = item.position();
			Vec3 lastPos = data.lastPos;
			data.lastPos = currentPos;

			// Remove if exceeded range/time or already on ground
			if (level.getGameTime() - data.createdTick > 80
					|| currentPos.distanceToSqr(data.startPos) > data.maxDistanceSq
					|| item.onGround()) {
				item.discard();
				iterator.remove();
				continue;
			}

			if (lastPos == null || currentPos.distanceToSqr(lastPos) < 1.0e-6) {
				continue;
			}

			// Block collision along movement vector
			HitResult clipResult = level.clip(new ClipContext(lastPos, currentPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, item));
			if (clipResult.getType() == HitResult.Type.BLOCK && clipResult instanceof BlockHitResult) {
				handleBlockImpact(level, item);
				iterator.remove();
				continue;
			}

			// Entity collision / near miss
			AABB pathBounds = new AABB(lastPos, currentPos).inflate(Math.max(0.1F, data.nearRadius));
			Predicate<Entity> predicate = candidate -> candidate instanceof LivingEntity living
					&& living.isAlive()
					&& candidate.getId() != data.ownerEntityId;

			EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(level, item, lastPos, currentPos, pathBounds, predicate);
			if (entityHitResult != null && entityHitResult.getEntity() instanceof LivingEntity living) {
				handleEntityImpact(level, item, data, living);
				iterator.remove();
				continue;
			}

			if (data.nearRadius > 1.0e-3F) {
				for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, item.getBoundingBox().inflate(data.nearRadius), e -> e.isAlive() && e.getId() != data.ownerEntityId)) {
					handleEntityImpact(level, item, data, living);
					iterator.remove();
					break;
				}
			}
		}

		if (throwsForLevel.isEmpty()) {
			ACTIVE_THROWS.remove(level.dimension());
		}
	}

	private static void handleEntityImpact(ServerLevel level, ItemEntity item, ActiveThrow data, LivingEntity target) {
		LivingEntity owner = level.getEntity(data.ownerEntityId) instanceof LivingEntity livingOwner ? livingOwner : null;
		DamageSource source = owner != null ? level.damageSources().thrown(item, owner) : level.damageSources().generic();

		if (data.damage > 0.0F) {
			target.hurt(source, data.damage);
		}

		level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 1.0F);
		level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
				8, 0.3, 0.3, 0.3, 0.1);

		item.discard();
	}

	private static void handleBlockImpact(ServerLevel level, ItemEntity item) {
		level.playSound(null, item.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.6F, 1.2F);
		level.sendParticles(ParticleTypes.POOF, item.getX(), item.getY(), item.getZ(), 5,
				0.2, 0.2, 0.2, 0.02);
		item.discard();
	}

	private static class ActiveThrow {
		private final int ownerEntityId;
		private final float damage;
		private final float nearRadius;
		private final double maxDistanceSq;
		private final Vec3 startPos;
		private final long createdTick;
		private Vec3 lastPos;

		private ActiveThrow(int ownerEntityId, float damage, float nearRadius, double maxDistance, Vec3 startPos, long createdTick) {
			this.ownerEntityId = ownerEntityId;
			this.damage = damage;
			this.nearRadius = nearRadius;
			this.maxDistanceSq = maxDistance * maxDistance;
			this.startPos = startPos;
			this.createdTick = createdTick;
			this.lastPos = startPos;
		}
	}
}





