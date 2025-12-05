package com.github.b4ndithelps.forge.entities;

import com.github.b4ndithelps.forge.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class FrostKnockbackSnowballEntity extends Snowball {

    private static final EntityDataAccessor<Float> DATA_KNOCKBACK =
            SynchedEntityData.defineId(FrostKnockbackSnowballEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_REINFORCED_DAMAGE =
            SynchedEntityData.defineId(FrostKnockbackSnowballEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_STONE_CORE =
            SynchedEntityData.defineId(FrostKnockbackSnowballEntity.class, EntityDataSerializers.BOOLEAN);

    public FrostKnockbackSnowballEntity(EntityType<? extends Snowball> entityType, Level level) {
        super(entityType, level);
    }

    public FrostKnockbackSnowballEntity(Level level, LivingEntity shooter) {
        super(level, shooter);
    }

    public FrostKnockbackSnowballEntity(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_KNOCKBACK, 0.9F);
        this.entityData.define(DATA_REINFORCED_DAMAGE, 0.0F);
        this.entityData.define(DATA_STONE_CORE, false);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FROST_KNOCKBACK_SNOWBALL.get();
    }

    public void setKnockbackStrength(float amount) {
        this.entityData.set(DATA_KNOCKBACK, amount);
    }

    public float getKnockbackStrength() {
        return this.entityData.get(DATA_KNOCKBACK);
    }

    public void setReinforcedDamage(float amount) {
        this.entityData.set(DATA_REINFORCED_DAMAGE, amount);
    }

    public float getReinforcedDamage() {
        return this.entityData.get(DATA_REINFORCED_DAMAGE);
    }

    public void setHasStoneCore(boolean flag) {
        this.entityData.set(DATA_STONE_CORE, flag);
    }

    public boolean hasStoneCore() {
        return this.entityData.get(DATA_STONE_CORE);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide()) {
            return;
        }

        Vec3 velocity = this.getDeltaMovement();
        Vec3 knockback = velocity.lengthSqr() < 1.0e-4
                ? new Vec3(0.0, 0.25, 0.0)
                : velocity.normalize().scale(Math.max(0.1F, getKnockbackStrength()));

        result.getEntity().push(knockback.x, Math.max(0.1, knockback.y + 0.1), knockback.z);
        result.getEntity().hurtMarked = true;

        if (hasStoneCore() && result.getEntity() instanceof LivingEntity living) {
            float damage = Math.max(0.0F, getReinforcedDamage());
            if (damage > 0.0F) {
                living.hurt(this.damageSources().thrown(this, this.getOwner()), damage);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Knockback", getKnockbackStrength());
        tag.putFloat("ReinforcedDamage", getReinforcedDamage());
        tag.putBoolean("HasStoneCore", hasStoneCore());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Knockback")) {
            setKnockbackStrength(tag.getFloat("Knockback"));
        }

        if (tag.contains("ReinforcedDamage")) {
            setReinforcedDamage(tag.getFloat("ReinforcedDamage"));
        }

        setHasStoneCore(tag.getBoolean("HasStoneCore"));
    }
}
