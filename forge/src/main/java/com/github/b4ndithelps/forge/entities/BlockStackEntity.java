package com.github.b4ndithelps.forge.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;

/**
 * Carries three captured blocks in a 1x3 vertical stack, can hover near the owner, then be thrown to deal damage.
 */
public class BlockStackEntity extends Projectile {

	private static final EntityDataAccessor<BlockState> DATA_BOT = SynchedEntityData.defineId(BlockStackEntity.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<BlockState> DATA_MID = SynchedEntityData.defineId(BlockStackEntity.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<BlockState> DATA_TOP = SynchedEntityData.defineId(BlockStackEntity.class, EntityDataSerializers.BLOCK_STATE);
	private static final EntityDataAccessor<Boolean> DATA_ATTACHED = SynchedEntityData.defineId(BlockStackEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> DATA_DAMAGE = SynchedEntityData.defineId(BlockStackEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_THROW_SPEED = SynchedEntityData.defineId(BlockStackEntity.class, EntityDataSerializers.FLOAT);

	private int lifeTicks = 0;
	private boolean shattered = false;

	public BlockStackEntity(EntityType<? extends BlockStackEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true; // while attached
		this.setBoundingBox(this.makeBoundingBox());
	}

	public static BlockStackEntity create(ServerLevel level, LivingEntity owner, BlockState bottom, BlockState middle, BlockState top, float hoverDamage, float throwSpeed) {
		BlockStackEntity e = new BlockStackEntity(ModEntities.BLOCK_STACK.get(), level);
		e.setOwner(owner);
		e.setStackStates(bottom, middle, top);
		e.setAttached(true);
		e.setDamageAmount(hoverDamage);
		e.setThrowSpeed(throwSpeed);
		return e;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(DATA_BOT, Blocks.STONE.defaultBlockState());
		this.entityData.define(DATA_MID, Blocks.STONE.defaultBlockState());
		this.entityData.define(DATA_TOP, Blocks.STONE.defaultBlockState());
		this.entityData.define(DATA_ATTACHED, true);
		this.entityData.define(DATA_DAMAGE, 8.0F);
		this.entityData.define(DATA_THROW_SPEED, 1.5F);
	}

	@Override
	public void tick() {
		super.tick();
		lifeTicks++;

		if (this.level().isClientSide) return;

		if (isAttached()) {
			LivingEntity owner = getOwnerAsLiving();
			if (owner == null || !owner.isAlive()) {
				this.discard();
				return;
			}
			// Hover at owner's main-shoulder: slight side + forward + shoulder height
			float yaw = owner.getYRot();
			Vec3 ownerPos = owner.position();
			double shoulderHeight = Math.max(0.45, Math.min(0.9, owner.getBbHeight() * 0.78));
			Vec3 up = new Vec3(0, 1, 0);
			Vec3 fwdYaw = Vec3.directionFromRotation(0, yaw).normalize();
			Vec3 rightYaw = fwdYaw.cross(up).normalize();
			float sideDir = owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1.0f : -1.0f;
			Vec3 target = ownerPos
					.add(0, shoulderHeight, 0)
					.add(rightYaw.scale(0.85 * sideDir)) // push further out from shoulder
					.add(fwdYaw.scale(0.15)); // a bit less forward to avoid overlap
			this.setPos(target.x, target.y - 0.5, target.z); // center roughly at middle block
			this.setDeltaMovement(Vec3.ZERO);
			this.noPhysics = true;
			// avoid entity collisions while attached
			return;
		}

		// Thrown state: enable collisions and lifetime
		this.noPhysics = false;

		// Check collisions along current motion, then advance
		HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hit.getType() != HitResult.Type.MISS) {
			this.onHit(hit);
		} else {
			Vec3 motion = this.getDeltaMovement();
			if (motion.lengthSqr() > 1.0e-8) {
				this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
				this.setBoundingBox(this.makeBoundingBox());
			}
		}
		// Apply gravity and light drag when thrown
		Vec3 vel = this.getDeltaMovement();
		this.setDeltaMovement(vel.x * 0.99, vel.y - 0.045, vel.z * 0.99);

		if (lifeTicks > 200) { // fallback lifetime
			this.discard();
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (this.level().isClientSide) return;
		if (result instanceof EntityHitResult ehr) {
			onHitEntity(ehr);
		} else if (result instanceof BlockHitResult bhr) {
			onHitBlock(bhr);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		if (this.level().isClientSide) return;
		if (result.getEntity() instanceof LivingEntity living && this.getOwner() instanceof LivingEntity owner) {
			float dmg = Math.max(0.0F, this.getDamageAmount());
			if (dmg > 0.0F) {
				living.hurt(this.damageSources().thrown(this, owner), dmg);
			}
			// small knockback
			Vec3 kb = this.getDeltaMovement().normalize().scale(0.65);
			living.push(kb.x, Math.max(0.25, kb.y), kb.z);
		}
		spawnFallingBlocksAndDiscard();
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (this.level().isClientSide) return;
		// Place the original stack as normal blocks adjacent to the hit face if space allows; otherwise fallback to shatter
		if (placeStackAsBlocks(result)) {
			this.shattered = true; // prevent any later shatter attempts
			this.discard();
		} else {
			spawnFallingBlocksAndDiscard();
		}
	}

	public void throwForward(LivingEntity owner) {
		this.setOwner(owner);
		setAttached(false);
		this.lifeTicks = 0;
		this.shattered = false;
		Vec3 dir = owner.getLookAngle().normalize();
		double speed = Math.max(0.1, getThrowSpeed());
		this.setDeltaMovement(dir.scale(speed));
		this.hasImpulse = true;
		this.noPhysics = false;
	}

	private void spawnFallingBlocksAndDiscard() {
		if (this.shattered) {
			this.discard();
			return;
		}
		this.shattered = true;
		if (!(this.level() instanceof ServerLevel sl)) {
			this.discard();
			return;
		}
		// Spawn three falling blocks at current position with slight random offsets and outward velocities
		java.util.Random rng = new java.util.Random(this.getUUID().getMostSignificantBits() ^ this.tickCount ^ System.nanoTime());
		Vec3 base = this.position();
		Vec3 v = this.getDeltaMovement();
		spawnOneFalling(sl, getBottom(), base, v, rng, 0);
		spawnOneFalling(sl, getMiddle(), base.add(0, 1, 0), v, rng, 1);
		spawnOneFalling(sl, getTop(), base.add(0, 2, 0), v, rng, 2);
		this.discard();
	}

	private boolean placeStackAsBlocks(BlockHitResult hit) {
		if (!(this.level() instanceof ServerLevel sl)) return false;
		Direction face = hit.getDirection();
		BlockPos anchor = hit.getBlockPos().relative(face);
		BlockPos b0 = anchor;
		BlockPos b1 = anchor.above();
		BlockPos b2 = anchor.above(2);
		// Require air for all three positions
		if (!sl.getBlockState(b0).isAir() || !sl.getBlockState(b1).isAir() || !sl.getBlockState(b2).isAir()) {
			return false;
		}
		sl.setBlock(b0, getBottom(), 3);
		sl.setBlock(b1, getMiddle(), 3);
		sl.setBlock(b2, getTop(), 3);
		return true;
	}

	private void spawnOneFalling(ServerLevel sl, BlockState state, Vec3 pos, Vec3 baseVel, java.util.Random rng, int index) {
		// Use vanilla factory, then adjust precise position and motion
		FallingBlockEntity e = FallingBlockEntity.fall(sl, BlockPos.containing(pos), state);
		// Random horizontal burst direction
		double angle = rng.nextDouble() * Math.PI * 2.0;
		double radius = 0.15 + rng.nextDouble() * 0.35; // small spread offset
		double offX = Math.cos(angle) * radius;
		double offZ = Math.sin(angle) * radius;
		e.setPos(pos.x + offX, pos.y + 0.01 + rng.nextDouble() * 0.06, pos.z + offZ);

		// Base forward velocity + random burst + upward kick
		Vec3 forward = baseVel.lengthSqr() > 1.0e-6 ? baseVel.normalize() : new Vec3(Math.cos(angle), 0, Math.sin(angle));
		double baseScale = 0.6 + rng.nextDouble() * 0.3;
		double burstX = (rng.nextDouble() - 0.5) * 0.6;
		double burstY = 0.18 + rng.nextDouble() * 0.22;
		double burstZ = (rng.nextDouble() - 0.5) * 0.6;
		Vec3 vel = new Vec3(
				forward.x * baseVel.length() * baseScale + burstX,
				Math.max(0.12, forward.y * baseVel.length() * 0.25) + burstY,
				forward.z * baseVel.length() * baseScale + burstZ
		);
		e.setDeltaMovement(vel);
		sl.addFreshEntity(e);
	}

	public void setStackStates(BlockState bottom, BlockState middle, BlockState top) {
		this.entityData.set(DATA_BOT, bottom);
		this.entityData.set(DATA_MID, middle);
		this.entityData.set(DATA_TOP, top);
		this.refreshDimensions();
	}

	public BlockState getBottom() { return this.entityData.get(DATA_BOT); }
	public BlockState getMiddle() { return this.entityData.get(DATA_MID); }
	public BlockState getTop() { return this.entityData.get(DATA_TOP); }

	public void setAttached(boolean attached) { this.entityData.set(DATA_ATTACHED, attached); }
	public boolean isAttached() { return this.entityData.get(DATA_ATTACHED); }

	public void setDamageAmount(float amount) { this.entityData.set(DATA_DAMAGE, amount); }
	public float getDamageAmount() { return this.entityData.get(DATA_DAMAGE); }

	public void setThrowSpeed(float speed) { this.entityData.set(DATA_THROW_SPEED, speed); }
	public float getThrowSpeed() { return this.entityData.get(DATA_THROW_SPEED); }

	@Nullable
	private LivingEntity getOwnerAsLiving() {
		return this.getOwner() instanceof LivingEntity l ? l : null;
	}

	@Override
	public void refreshDimensions() {
		this.setBoundingBox(this.makeBoundingBox());
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		// Roughly 1x3 stack
		return EntityDimensions.fixed(0.99F, 2.99F);
	}

	@Override
	protected AABB makeBoundingBox() {
		double w = 0.99;
		double h = 2.99;
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		return new AABB(
				x - w * 0.5, y, z - w * 0.5,
				x + w * 0.5, y + h, z + w * 0.5
		);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Attached", isAttached());
		tag.putFloat("Damage", getDamageAmount());
		tag.putFloat("ThrowSpeed", getThrowSpeed());
		tag.putInt("Bot", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(getBottom().getBlock()));
		tag.putInt("Mid", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(getMiddle().getBlock()));
		tag.putInt("Top", net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(getTop().getBlock()));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setAttached(tag.getBoolean("Attached"));
		setDamageAmount(tag.getFloat("Damage"));
		setThrowSpeed(tag.getFloat("ThrowSpeed"));
		BlockState bot = net.minecraft.core.registries.BuiltInRegistries.BLOCK.byId(tag.getInt("Bot")).defaultBlockState();
		BlockState mid = net.minecraft.core.registries.BuiltInRegistries.BLOCK.byId(tag.getInt("Mid")).defaultBlockState();
		BlockState top = net.minecraft.core.registries.BuiltInRegistries.BLOCK.byId(tag.getInt("Top")).defaultBlockState();
		setStackStates(bot, mid, top);
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// ignore damage
		return false;
	}
}


