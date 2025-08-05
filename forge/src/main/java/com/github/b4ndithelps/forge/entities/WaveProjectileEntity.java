package com.github.b4ndithelps.forge.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class WaveProjectileEntity extends Projectile {

    // Data synchronizers for client-server sync
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(WaveProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(WaveProjectileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPEED =
            SynchedEntityData.defineId(WaveProjectileEntity.class, EntityDataSerializers.FLOAT);

    // Blocks that this wave can destroy
    private Set<Block> destructibleBlocks;
    private int maxLifetime = 200; // 10 seconds at 20 TPS
    private int lifetime = 0;

    public WaveProjectileEntity(EntityType<? extends WaveProjectileEntity> entityType, Level level) {
        super(entityType, level);
        this.initializeDestructibleBlocks();
    }

    public WaveProjectileEntity(EntityType<? extends WaveProjectileEntity> entityType, Level level, LivingEntity owner) {
        super(entityType, level);
        this.setOwner(owner);
        this.initializeDestructibleBlocks();

        // Set initial rotation to match owner's look direction
        if (owner != null) {
            this.setYRot(owner.getYRot());
            this.setXRot(owner.getXRot());
        }
    }

    // Method to set movement and align rotation properly
    public void setMovementAndRotation(Vec3 movement) {
        this.setDeltaMovement(movement);
        this.alignRotationWithMovement();
    }

    // Align the entity's rotation with its movement direction
    private void alignRotationWithMovement() {
        Vec3 movement = this.getDeltaMovement();
        if (movement.lengthSqr() > 0) {
            // Calculate yaw from movement direction
            double yaw = Math.atan2(-movement.x, movement.z) * (180.0 / Math.PI);
            
            // Calculate pitch from movement direction
            double horizontalDistance = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
            double pitch = Math.atan2(-movement.y, horizontalDistance) * (180.0 / Math.PI);
            
            this.setYRot((float) yaw);
            this.setXRot((float) pitch);
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_WIDTH, 3.0F);
        this.entityData.define(DATA_HEIGHT, 3.0F);
        this.entityData.define(DATA_SPEED, 1.0F);
    }

    private void initializeDestructibleBlocks() {
        destructibleBlocks = new HashSet<>();
        // Add blocks that can be destroyed by the wave
        destructibleBlocks.add(Blocks.DIRT);
        destructibleBlocks.add(Blocks.STONE);
        destructibleBlocks.add(Blocks.COBBLESTONE);
        destructibleBlocks.add(Blocks.GRASS_BLOCK);
        // Add more blocks as needed
    }

    // Setters for wave properties
    public void setWaveWidth(float width) {
        this.entityData.set(DATA_WIDTH, width);
        this.refreshDimensions();
    }

    public void setWaveHeight(float height) {
        this.entityData.set(DATA_HEIGHT, height);
        this.refreshDimensions();
    }

    public void setWaveSpeed(float speed) {
        this.entityData.set(DATA_SPEED, speed);
    }

    // Getters
    public float getWaveWidth() {
        return this.entityData.get(DATA_WIDTH);
    }

    public float getWaveHeight() {
        return this.entityData.get(DATA_HEIGHT);
    }

    public float getWaveSpeed() {
        return this.entityData.get(DATA_SPEED);
    }

    @Override
    public void tick() {
        super.tick();

        // Ensure rotation is always aligned with movement direction
        this.alignRotationWithMovement();

        lifetime++;
        if (lifetime > maxLifetime) {
            this.discard();
            return;
        }

        // Move the wave forward
        // Movement direction is set during entity creation and should be preserved

        // Check for block collisions and destroy blocks
        this.checkBlockCollisions();

        // Move the entity
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private void checkBlockCollisions() {
        // Create a custom bounding box for collision detection
        AABB collisionBox = this.getCustomBoundingBox();

        // Get all block positions within the wave's bounding box
        int minX = (int) Math.floor(collisionBox.minX);
        int maxX = (int) Math.ceil(collisionBox.maxX);
        int minY = (int) Math.floor(collisionBox.minY);
        int maxY = (int) Math.ceil(collisionBox.maxY);
        int minZ = (int) Math.floor(collisionBox.minZ);
        int maxZ = (int) Math.ceil(collisionBox.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState blockState = this.level().getBlockState(pos);

                    if (canDestroyBlock(blockState.getBlock())) {
                        // Destroy the block
                        this.level().destroyBlock(pos, true);

                        // Optional: Add particles or sound effects
                        // this.level().addParticle(ParticleTypes.EXPLOSION, x + 0.5, y + 0.5, z + 0.5, 0, 0, 0);
                    }
                }
            }
        }
    }

    private boolean canDestroyBlock(Block block) {
        return destructibleBlocks.contains(block) && block != Blocks.AIR;
    }

    private AABB getCustomBoundingBox() {
        // Create a custom bounding box based on width and height
        float width = getWaveWidth();
        float height = getWaveHeight();

        Vec3 pos = this.position();

        // Calculate the wave's orientation based on its movement direction
        Vec3 movement = this.getDeltaMovement().normalize();

        // Create perpendicular vectors for the wave "wall"
        // If moving in X/Z plane, the wall should be perpendicular to movement
        double perpX = -movement.z; // Perpendicular X component
        double perpZ = movement.x;  // Perpendicular Z component

        // Calculate the corners of the wave wall
        double halfWidth = width / 2.0;
        double wallThickness = 0.5; // How thick the wave is in movement direction

        return new AABB(
                pos.x - perpX * halfWidth - movement.x * wallThickness,
                pos.y,
                pos.z - perpZ * halfWidth - movement.z * wallThickness,
                pos.x + perpX * halfWidth + movement.x * wallThickness,
                pos.y + height,
                pos.z + perpZ * halfWidth + movement.z * wallThickness
        );
    }

    @Override
    public EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        // Return custom dimensions based on wave size
        float width = getWaveWidth();
        float height = getWaveHeight();
        return EntityDimensions.scalable(width, height);
    }

    @Override
    protected float getEyeHeight(net.minecraft.world.entity.Pose pose, EntityDimensions dimensions) {
        return dimensions.height * 0.85F;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        // Handle what happens when the wave hits something
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            // You can add special behavior here if needed
        }
        // Don't call super.onHit() if you don't want the projectile to stop on collision
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("WaveWidth", getWaveWidth());
        compound.putFloat("WaveHeight", getWaveHeight());
        compound.putFloat("WaveSpeed", getWaveSpeed());
        compound.putInt("Lifetime", this.lifetime);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        setWaveWidth(compound.getFloat("WaveWidth"));
        setWaveHeight(compound.getFloat("WaveHeight"));
        setWaveSpeed(compound.getFloat("WaveSpeed"));
        this.lifetime = compound.getInt("Lifetime");
    }

    // Add method to configure destructible blocks
    public void addDestructibleBlock(Block block) {
        this.destructibleBlocks.add(block);
    }

    public void removeDestructibleBlock(Block block) {
        this.destructibleBlocks.remove(block);
    }

    public void setDestructibleBlocks(Set<Block> blocks) {
        this.destructibleBlocks = new HashSet<>(blocks);
    }
}
