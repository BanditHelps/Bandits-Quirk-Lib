package com.github.b4ndithelps.forge.entities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

import java.util.List;

public class ThrownHeldItemEntity extends Projectile {

	private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(ThrownHeldItemEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(ThrownHeldItemEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_NEAR_RADIUS = SynchedEntityData.defineId(ThrownHeldItemEntity.class, EntityDataSerializers.FLOAT);

	private int lifeTicks = 0;

	public ThrownHeldItemEntity(EntityType<? extends ThrownHeldItemEntity> type, Level level) {
		super(type, level);
		this.noPhysics = false;
	}

	public ThrownHeldItemEntity(Level level, LivingEntity owner, ItemStack stack, float damage, float nearRadius) {
		super(ModEntities.THROWN_HELD_ITEM.get(), level);
		this.setOwner(owner);
		this.setItem(stack);
		this.setDamage(damage);
		this.setNearRadius(nearRadius);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
		this.entityData.define(DATA_DAMAGE, 4.0F);
		this.entityData.define(DATA_NEAR_RADIUS, 0.6F);
	}

	public void setItem(ItemStack stack) {
		this.entityData.set(DATA_ITEM, stack.copy());
	}

	public ItemStack getItem() {
		return this.entityData.get(DATA_ITEM);
	}

	public void setDamage(float damage) {
		this.entityData.set(DATA_DAMAGE, Math.max(0.0F, damage));
	}

	public float getDamage() {
		return this.entityData.get(DATA_DAMAGE);
	}

	public void setNearRadius(float radius) {
		this.entityData.set(DATA_NEAR_RADIUS, Math.max(0.0F, radius));
	}

	public float getNearRadius() {
		return this.entityData.get(DATA_NEAR_RADIUS);
	}

	@Override
	public void tick() {
		super.tick();
		lifeTicks++;

		// Despawn after a short lifetime to avoid lingering objects
		if (!this.level().isClientSide && lifeTicks > 100) {
			this.discard();
			return;
		}

		// Move and check collision along path this tick
		HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hit.getType() != HitResult.Type.MISS) {
			this.onHit(hit);
			return;
		}

		// Advance position
		Vec3 motion = this.getDeltaMovement();
		if (motion.lengthSqr() > 1.0e-8) {
			this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
			this.setBoundingBox(this.makeBoundingBox());
		}

		// Simple gravity + slight drag
		if (!this.isNoGravity()) {
			this.setDeltaMovement(motion.x * 0.99, motion.y - 0.05, motion.z * 0.99);
		}

		// Near-miss proximity damage: if any living entity is within near radius
		if (!this.level().isClientSide) {
			float r = getNearRadius();
			AABB box = this.getBoundingBox().inflate(r, r, r);
			List<Entity> nearby = this.level().getEntities(this, box, e -> e instanceof LivingEntity && e.isAlive() && e != this.getOwner());
			if (!nearby.isEmpty()) {
				Entity first = nearby.get(0);
				this.onProximity(first);
				return;
			}
		}

		// Client-side small trail
		if (this.level().isClientSide && this.random.nextFloat() < 0.2F) {
			this.level().addParticle(ParticleTypes.ITEM_SNOWBALL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
		}
	}

	protected void onProximity(Entity target) {
		if (this.level().isClientSide) return;
		if (target instanceof LivingEntity living) {
			float dmg = getDamage();
			Entity owner = this.getOwner();
			DamageSource source = owner instanceof LivingEntity lo ? this.damageSources().thrown(this, lo) : this.damageSources().generic();
			if (dmg > 0) {
				living.hurt(source, dmg);
			}
		}
		if (this.level() instanceof ServerLevel sl) {
			sl.playSound(null, this.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.6F, 1.2F);
			sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
		}
		this.discard();
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (this.level().isClientSide) return;
		if (result instanceof EntityHitResult ehr) {
			onHitEntity(ehr);
		} else if (result instanceof BlockHitResult) {
			onHitBlock();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		Entity target = result.getEntity();
		if (target instanceof LivingEntity living) {
			float dmg = getDamage();
			Entity owner = this.getOwner();
			DamageSource source = owner instanceof LivingEntity lo ? this.damageSources().thrown(this, lo) : this.damageSources().generic();
			if (dmg > 0) {
				living.hurt(source, dmg);
			}
		}
		if (this.level() instanceof ServerLevel sl) {
			sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 1.0F);
			sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
		}
		this.discard();
	}

	protected void onHitBlock() {
		if (this.level() instanceof ServerLevel sl) {
			sl.playSound(null, this.blockPosition(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.6F, 1.2F);
			sl.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
		}
		// Consumed by the throw
		this.discard();
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		ItemStack stack = getItem();
		if (!stack.isEmpty()) {
			tag.put("Item", stack.save(new CompoundTag()));
		}
		tag.putFloat("Damage", getDamage());
		tag.putFloat("NearRadius", getNearRadius());
		tag.putInt("Life", lifeTicks);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Item")) {
			this.setItem(ItemStack.of(tag.getCompound("Item")));
		}
		this.setDamage(tag.getFloat("Damage"));
		this.setNearRadius(tag.getFloat("NearRadius"));
		this.lifeTicks = tag.getInt("Life");
	}
}


