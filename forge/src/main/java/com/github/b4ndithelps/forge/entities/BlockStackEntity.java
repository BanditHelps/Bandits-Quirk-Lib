package com.github.b4ndithelps.forge.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
	private boolean hasPlaced = false;

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
			// Hover at owner's main-shoulder: more side + less forward + slightly higher
			float yaw = owner.getYRot();
			Vec3 ownerPos = owner.position();
			double shoulderHeight = Math.max(0.45, Math.min(0.9, owner.getBbHeight() * 0.78));
			Vec3 up = new Vec3(0, 1, 0);
			Vec3 fwdYaw = Vec3.directionFromRotation(0, yaw).normalize();
			Vec3 rightYaw = fwdYaw.cross(up).normalize();
			float sideDir = owner.getMainArm() == HumanoidArm.RIGHT ? 1.0f : -1.0f;
			Vec3 target = ownerPos
					.add(0, shoulderHeight + 0.25, 0)
					.add(rightYaw.scale(1.10 * sideDir))
					.add(fwdYaw.scale(0.05));
			this.setPos(target.x, target.y - 0.20, target.z); // center roughly at middle block
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
			// Stop further motion/updates this tick to avoid visible drift before despawn
			return;
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
		if (this.level().isClientSide) {
			super.onHit(result);
			return;
		}
		if (this.hasPlaced) {
			return;
		}
		this.hasPlaced = true;
		super.onHit(result);
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
		// Place the pillar at/near impact and play small burst FX, then remove this entity
		if (this.level() instanceof ServerLevel sl) {
			Vec3 targetCenter = result.getEntity().getBoundingBox().getCenter();
			Vec3 impact = targetCenter.add(0, Math.max(0.4, result.getEntity().getBbHeight() * 0.25), 0);
			placeStackAtImpact(sl, impact);
			burstFX(sl, impact);
		}
		// Remove the entity after impact
		this.discard();
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		if (this.level().isClientSide) return;
		// Place the original stack as normal blocks adjacent to the hit face if space allows; otherwise just remove
		if (placeStackAsBlocks(result)) {
			this.shattered = true; // prevent any later shatter attempts
			this.discard();
		} else {
			this.discard();
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

	private boolean placeStackAsBlocks(BlockHitResult hit) {
		if (!(this.level() instanceof ServerLevel sl)) return false;
		Direction face = hit.getDirection();
		BlockPos anchor = hit.getBlockPos().relative(face);
		BlockPos b0 = anchor;
		BlockPos b1 = anchor.above();
		BlockPos b2 = anchor.above(2);
		if (!canOccupy(sl, b0) || !canOccupy(sl, b1) || !canOccupy(sl, b2)) {
			return false;
		}
		clearBlock(sl, b0);
		clearBlock(sl, b1);
		clearBlock(sl, b2);
		sl.setBlock(b0, getBottom(), 3);
		sl.setBlock(b1, getMiddle(), 3);
		sl.setBlock(b2, getTop(), 3);
		return true;
	}

	private boolean placeStackAtImpact(ServerLevel sl, Vec3 impact) {
		BlockPos ip = BlockPos.containing(impact);
		// Find ground within a few blocks below
		BlockPos probe = ip;
		for (int i = 0; i < 6; i++) {
			if (!sl.getBlockState(probe).isAir()) {
				break;
			}
			probe = probe.below();
		}
		BlockPos base = sl.getBlockState(probe).isAir() ? ip : probe.above();
		BlockPos b0 = base;
		BlockPos b1 = base.above();
		BlockPos b2 = base.above(2);
		if (!canOccupy(sl, b0) || !canOccupy(sl, b1) || !canOccupy(sl, b2)) {
			return false;
		}
		clearBlock(sl, b0);
		clearBlock(sl, b1);
		clearBlock(sl, b2);
		sl.setBlock(b0, getBottom(), 3);
		sl.setBlock(b1, getMiddle(), 3);
		sl.setBlock(b2, getTop(), 3);
		return true;
	}

	private void burstFX(ServerLevel sl, Vec3 where) {
		sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getMiddle()),
				where.x, where.y, where.z,
				20, 0.4, 0.3, 0.4, 0.12);
		sl.playSound(null, BlockPos.containing(where), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.2f);
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
		// Store full blockstates for fidelity
		tag.put("BotState", NbtUtils.writeBlockState(getBottom()));
		tag.put("MidState", NbtUtils.writeBlockState(getMiddle()));
		tag.put("TopState", NbtUtils.writeBlockState(getTop()));
		// Legacy ints for backward compatibility
		tag.putInt("Bot", BuiltInRegistries.BLOCK.getId(getBottom().getBlock()));
		tag.putInt("Mid", BuiltInRegistries.BLOCK.getId(getMiddle().getBlock()));
		tag.putInt("Top", BuiltInRegistries.BLOCK.getId(getTop().getBlock()));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setAttached(tag.getBoolean("Attached"));
		setDamageAmount(tag.getFloat("Damage"));
		setThrowSpeed(tag.getFloat("ThrowSpeed"));
		BlockState bot;
		BlockState mid;
		BlockState top;
		if (tag.contains("BotState") && tag.contains("MidState") && tag.contains("TopState")) {
			bot = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("BotState"));
			mid = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("MidState"));
			top = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("TopState"));
		} else {
			bot = BuiltInRegistries.BLOCK.byId(tag.getInt("Bot")).defaultBlockState();
			mid = BuiltInRegistries.BLOCK.byId(tag.getInt("Mid")).defaultBlockState();
			top = BuiltInRegistries.BLOCK.byId(tag.getInt("Top")).defaultBlockState();
		}
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

	private boolean canOccupy(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES);
	}

	private void clearBlock(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!state.isAir()) {
			level.destroyBlock(pos, true);
		}
	}
}