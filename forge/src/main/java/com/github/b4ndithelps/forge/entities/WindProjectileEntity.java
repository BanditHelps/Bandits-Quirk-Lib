package com.github.b4ndithelps.forge.entities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class WindProjectileEntity extends AbstractArrow {
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(WindProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> KNOCKBACK_FORCE = SynchedEntityData.defineId(WindProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> VISUAL_RATIO = SynchedEntityData.defineId(WindProjectileEntity.class, EntityDataSerializers.FLOAT);
    
    private int particleTimer = 0;
    private Vec3 startPosition;
    private final double maxRange;

    public WindProjectileEntity(EntityType<? extends WindProjectileEntity> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true); // Wind projectiles aren't affected by gravity
        this.maxRange = 3;
    }

    public WindProjectileEntity(Level level, LivingEntity shooter, float damage, float knockback, float visualRatio, double range) {
        super(ModEntities.WIND_PROJECTILE.get(), shooter, level);
        this.setNoGravity(true);
        this.entityData.set(DAMAGE, damage);
        this.entityData.set(KNOCKBACK_FORCE, knockback);
        this.entityData.set(VISUAL_RATIO, visualRatio);
        this.maxRange = range;
        this.startPosition = this.position();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DAMAGE, 0.0F);
        this.entityData.define(KNOCKBACK_FORCE, 0.3F);
        this.entityData.define(VISUAL_RATIO, 0.0F);
    }

    @Override
    public void tick() {
        super.tick();

        if (startPosition != null && this.position().distanceTo(startPosition) > maxRange) {
            this.discard();
            return;
        }
        
        // Create particle trail every few ticks
        if (this.level() instanceof ServerLevel serverLevel && particleTimer++ % 2 == 0) {
            createParticleRing(serverLevel);
        }

        // Check if projectile is in water and moving too slowly
        if (this.isInWater() && this.getDeltaMovement().lengthSqr() < 0.01) { // Very slow movement
            this.discard();
            return;
        }
        
        // Remove projectile after certain time to prevent infinite travel. This is a backup in case max range fails
        if (this.tickCount > 100) { // 5 seconds max lifetime
            this.discard();
        }
    }

    private void createParticleRing(ServerLevel level) {
        float visualRatio = this.entityData.get(VISUAL_RATIO);
        Vec3 position = this.position();
        Vec3 velocity = this.getDeltaMovement();

        // Normalize velocity to get direction
        Vec3 direction = velocity.normalize();

        // Create two perpendicular vectors to the direction for ring orientation
        Vec3 perpendicular1, perpendicular2;

        // Find a vector that's not parallel to direction
        if (Math.abs(direction.y) < 0.9) {
            perpendicular1 = new Vec3(0, 1, 0).cross(direction).normalize();
        } else {
            perpendicular1 = new Vec3(1, 0, 0).cross(direction).normalize();
        }

        // Second perpendicular vector
        perpendicular2 = direction.cross(perpendicular1).normalize();

        // Ring parameters - fewer particles for cleaner outline
        int particleCount = (int) (8 + visualRatio * 8); // Fewer particles
        double ringRadius = 0.5 + visualRatio * 0.5;

        // Create just one main ring at the projectile position
        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i) / particleCount;

            // Calculate position on ring using perpendicular vectors
            Vec3 ringOffset = perpendicular1.scale(Math.cos(angle) * ringRadius)
                    .add(perpendicular2.scale(Math.sin(angle) * ringRadius));

            Vec3 particlePos = position.add(ringOffset);

            // Main ring outline particles
            level.sendParticles(ParticleTypes.CLOUD,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.02, 0.02, 0.02, 0.01);
        }

        // Optional: Single trailing ring for higher visual ratios
        if (visualRatio > 0.6F) {
            Vec3 trailCenter = position.subtract(direction.scale(0.4));
            double trailRadius = ringRadius * 0.7;
            int trailParticles = particleCount / 2;

            for (int i = 0; i < trailParticles; i++) {
                double angle = (2 * Math.PI * i) / trailParticles;

                Vec3 trailOffset = perpendicular1.scale(Math.cos(angle) * trailRadius)
                        .add(perpendicular2.scale(Math.sin(angle) * trailRadius));

                Vec3 trailPos = trailCenter.add(trailOffset);

                level.sendParticles(ParticleTypes.POOF,
                        trailPos.x, trailPos.y, trailPos.z,
                        1, 0.01, 0.01, 0.01, 0.005);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        float damage = this.entityData.get(DAMAGE);
        float knockback = this.entityData.get(KNOCKBACK_FORCE);
        
        if (target instanceof LivingEntity livingTarget && this.getOwner() != target) {
            // Apply knockback
            Vec3 knockbackDirection = this.getDeltaMovement().normalize();
            livingTarget.push(
                knockbackDirection.x * knockback, 
                Math.max(knockbackDirection.y * knockback, knockback * 0.3), // Minimum upward knockback
                knockbackDirection.z * knockback
            );
            
            // Apply damage if enabled
            if (damage > 0 && this.getOwner() instanceof LivingEntity owner) {
                livingTarget.hurt(this.damageSources().thrown(this, owner), damage);
            }
            
            // Visual feedback for hit
            if (this.level() instanceof ServerLevel serverLevel) {
                Vec3 entityPos = target.position().add(0, target.getBbHeight() / 2, 0);
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        entityPos.x, entityPos.y, entityPos.z,
                        5, 0.3, 0.3, 0.3, 0.1);
                
                // Sound feedback for hit
                serverLevel.playSound(null, target.blockPosition(), 
                              damage > 0 ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_KNOCKBACK, 
                              SoundSource.PLAYERS, 0.8F, 1.2F);
            }
        }
        
        // Remove projectile after hit
        this.discard();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        
        // Create impact particles on any hit
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 hitPos = result.getLocation();
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    hitPos.x, hitPos.y, hitPos.z,
                    3, 0.3, 0.3, 0.3, 0.1);
        }
        
        // Remove projectile
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Don't call super.onHitBlock() to avoid arrow-specific behavior

        // Add your own wind-specific block hit effects here
        if (this.level() instanceof ServerLevel serverLevel) {
            Vec3 hitPos = result.getLocation();

            // Wind-specific particles
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    hitPos.x, hitPos.y, hitPos.z,
                    10, 0.5, 0.5, 0.5, 0.1);

            // Wind-specific sound
            serverLevel.playSound(null, result.getBlockPos(),
                    SoundEvents.BRUSH_GENERIC, // Or another wind-like sound
                    SoundSource.PLAYERS, 0.8F, 1.0F);
        }

        // Remove projectile
        this.discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY; // Wind projectiles can't be picked up
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.entityData.get(DAMAGE));
        tag.putFloat("KnockbackForce", this.entityData.get(KNOCKBACK_FORCE));
        tag.putFloat("VisualRatio", this.entityData.get(VISUAL_RATIO));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DAMAGE, tag.getFloat("Damage"));
        this.entityData.set(KNOCKBACK_FORCE, tag.getFloat("KnockbackForce"));
        this.entityData.set(VISUAL_RATIO, tag.getFloat("VisualRatio"));
    }

    @Override
    public boolean isPickable() {
        return false; // Wind projectiles pass through most interactions
    }

    @Override
    public boolean isPushable() {
        return false; // Wind projectiles can't be pushed
    }
}