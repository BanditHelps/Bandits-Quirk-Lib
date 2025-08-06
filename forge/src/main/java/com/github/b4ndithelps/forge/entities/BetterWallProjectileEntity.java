package com.github.b4ndithelps.forge.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;

/**
 * This entity is going to be used as a wall of air that flys out and interacts with certain blocks
 */
public class BetterWallProjectileEntity extends Projectile {
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(BetterWallProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(BetterWallProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME = SynchedEntityData.defineId(BetterWallProjectileEntity.class, EntityDataSerializers.INT);

    private Vec3 direction;
    private int maxLifetime = 100; // ticks
    private int particleTimer = 0;

    public BetterWallProjectileEntity(EntityType<? extends BetterWallProjectileEntity> entityType, Level level) {
        super(entityType, level);
        this.direction = Vec3.ZERO;
        this.setWidth(1.0f);
        this.setHeight(1.0f);
        this.setLifetime(100);
        this.noPhysics = true;
    }

    public BetterWallProjectileEntity(EntityType<? extends BetterWallProjectileEntity> entityType, Level level, Player shooter, float width, float height) {
        super(entityType, level);
        this.setOwner(shooter);
        this.setWidth(1.0f);
        this.setHeight(1.0f);
        this.setPos(shooter.getX(), shooter.getY() - 0.1, shooter.getZ());

        // Calculate the direction based on the player's look direction
        Vec3 lookDir = shooter.getLookAngle();
        this.direction = lookDir.normalize();

        // Set the rotation to match direction
        this.setYRot((float) (Mth.atan2(lookDir.x, lookDir.z) * 180.0 / Math.PI));
        this.setXRot((float) (Mth.atan2(-lookDir.y, Math.sqrt(lookDir.x * lookDir.x + lookDir.z * lookDir.z)) * 190.0 / Math.PI));

        // Update the hitbox
        this.refreshDimensions();
    }

    public BetterWallProjectileEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.BETTER_WALL_PROJECTILE.get(), level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_WIDTH, 1.0f);
        this.entityData.define(DATA_HEIGHT, 1.0f);
        this.entityData.define(DATA_LIFETIME, 100);
    }

    public void setWidth(float width) {
        this.entityData.set(DATA_WIDTH, Math.max(0.1f, width));
        this.refreshDimensions();
    }

    public void setHeight(float height) {
        this.entityData.set(DATA_HEIGHT, Math.max(0.1f, height));
        this.refreshDimensions();
    }

    public void setLifetime(int lifetime) {
        this.entityData.set(DATA_LIFETIME, lifetime);
        this.maxLifetime = lifetime;
    }

    public float getWallWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getWallHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    public int getLifetime() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public void setDirection(Vec3 direction) {
        this.direction = direction.normalize();
    }

    // Use "theDirection" because the regular getDirection is being used in the super
    public Vec3 getTheDirection() {
        return this.direction;
    }

    @Override
    public void tick() {
        super.tick();

        // Check lifetime
        if (this.tickCount >= this.maxLifetime) {
            this.discard();
            return;
        }

        // Move in the set direction (no gravity gets applied here)
        if (!this.direction.equals(Vec3.ZERO)) {
            double speed = 0.8; // May need to adjust this, or make it a variable
            Vec3 motion = this.direction.scale(speed);
            this.setDeltaMovement(motion);
            this.move(MoverType.SELF, motion);
        }

        this.destroyBlocks();
        this.spawnParticles();

        // Update the position
        this.setPos(this.getX() + this.getDeltaMovement().x,
                this.getY() + this.getDeltaMovement().y,
                this.getZ() + this.getDeltaMovement().z);
    }

    private void destroyBlocks() {
        if (this.level().isClientSide) return;

        AABB boundingBox = this.getBoundingBox();

        // Get all the block positions within the entity's hitbox
        int minX = Mth.floor(boundingBox.minX);
        int minY = Mth.floor(boundingBox.minY);
        int minZ = Mth.floor(boundingBox.minZ);
        int maxX = Mth.floor(boundingBox.maxX);
        int maxY = Mth.floor(boundingBox.maxY);
        int maxZ = Mth.floor(boundingBox.maxX);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState blockState = this.level().getBlockState(pos);
                    Block block = blockState.getBlock();

                    if (shouldDestroyBlock(block)) {
                        this.level().destroyBlock(pos, true);
                    }
                }
            }
        }
    }

    private boolean shouldDestroyBlock(Block block) {
        return block == Blocks.ICE ||
                block == Blocks.PACKED_ICE ||
                block == Blocks.BLUE_ICE ||
                block == Blocks.FROSTED_ICE ||
                block.defaultBlockState().is(BlockTags.LEAVES);
    }

    private void spawnParticles() {
        if (this.level().isClientSide) return;

        this.particleTimer++;
        if (this.particleTimer % 2 != 0) return; // Spawn particles every 2 ticks

        if (this.level() instanceof ServerLevel serverLevel) {
            AABB boundingBox = this.getBoundingBox();

            // Spawn particles along the edges of the wall to show its dimensions
            double particleSpacing = 0.3;

            // Front and back faces
            for (double x = boundingBox.minX; x <= boundingBox.maxX; x += particleSpacing) {
                for (double y = boundingBox.minY; y <= boundingBox.maxY; y += particleSpacing) {
                    // Front face
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            x, y, boundingBox.minZ, 1, 0.05, 0.05, 0.05, 0.01);
                    // Back face
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            x, y, boundingBox.maxZ, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Side faces
            for (double z = boundingBox.minZ; z <= boundingBox.maxZ; z += particleSpacing) {
                for (double y = boundingBox.minY; y <= boundingBox.maxY; y += particleSpacing) {
                    // Left face
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            boundingBox.minX, y, z, 1, 0.05, 0.05, 0.05, 0.01);
                    // Right face
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            boundingBox.maxX, y, z, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }

            // Top and bottom faces
            for (double x = boundingBox.minX; x <= boundingBox.maxX; x += particleSpacing) {
                for (double z = boundingBox.minZ; z <= boundingBox.maxZ; z += particleSpacing) {
                    // Top face
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            x, boundingBox.maxY, z, 1, 0.05, 0.05, 0.05, 0.01);
                    // Bottom face
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            x, boundingBox.minY, z, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }
    }

    @Override
    public void refreshDimensions() {
        float width = this.getWallWidth();
        float height = this.getWallHeight();

        // Set entity dimensions - depth is always small (like a wall)
        EntityDimensions dimensions = EntityDimensions.fixed(width, height);
        super.refreshDimensions();
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.fixed(this.getWallWidth(), this.getWallHeight());
    }

    @Override
    protected AABB makeBoundingBox() {
        float width = this.getWallWidth();
        float height = this.getWallHeight();
        float depth = 0.5f; // Thin wall depth

        // Create bounding box centered on the entity
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        return new AABB(
                x - width / 2, y - height / 2, z - depth / 2,
                x + width / 2, y + height / 2, z + depth / 2
        );
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("WallWidth", this.getWallWidth());
        compound.putFloat("WallHeight", this.getWallHeight());
        compound.putInt("Lifetime", this.getLifetime());
        compound.putDouble("DirectionX", this.direction.x);
        compound.putDouble("DirectionY", this.direction.y);
        compound.putDouble("DirectionZ", this.direction.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setWidth(compound.getFloat("WallWidth"));
        this.setHeight(compound.getFloat("WallHeight"));
        this.setLifetime(compound.getInt("Lifetime"));
        this.direction = new Vec3(
                compound.getDouble("DirectionX"),
                compound.getDouble("DirectionY"),
                compound.getDouble("DirectionZ")
        );
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        // Override to prevent default projectile behavior
        // Add custom entity interaction logic here when needed
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Override to prevent default projectile behavior
        // Blocks are handled in destroyBlocks() method instead
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float amount) {
        return false; // Make invulnerable
    }

}
