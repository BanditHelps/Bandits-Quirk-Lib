package com.github.b4ndithelps.forge.capabilities.Body;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Enhanced BodyStatusCapability that automatically syncs changes to the client.
 * This wrapper around BodyStatusCapability provides automatic synchronization
 * whenever data is modified on the server side.
 */
public class SyncedBodyStatusCapability implements IBodyStatusCapability {
    private final BodyStatusCapability baseCapability;
    private final Supplier<Player> playerSupplier;
    
    public SyncedBodyStatusCapability(Supplier<Player> playerSupplier) {
        this.baseCapability = new BodyStatusCapability();
        this.playerSupplier = playerSupplier;
    }
    
    private void triggerSync() {
        if (playerSupplier != null) {
            Player player = playerSupplier.get();
            if (player instanceof ServerPlayer serverPlayer) {
                BodyStatusHelper.syncToClient(serverPlayer);
            }
        }
    }

    // Delegate read-only methods directly (no sync needed)
    @Override
    public BodyStatusData getBodyPartData(BodyPart part) {
        return baseCapability.getBodyPartData(part);
    }

    @Override
    public float getDamage(BodyPart part) {
        return baseCapability.getDamage(part);
    }

    @Override
    public DamageStage getDamageStage(BodyPart part) {
        return baseCapability.getDamageStage(part);
    }

    @Override
    public float getDamagePercentage(BodyPart part) {
        return baseCapability.getDamagePercentage(part);
    }

    @Override
    public int getCustomStatus(BodyPart part, String statusName) {
        return baseCapability.getCustomStatus(part, statusName);
    }

    @Override
    public boolean hasCustomStatus(BodyPart part, String statusName) {
        return baseCapability.hasCustomStatus(part, statusName);
    }

    @Override
    public float getCustomFloat(BodyPart part, String key) {
        return baseCapability.getCustomFloat(part, key);
    }

    @Override
    public boolean hasCustomFloat(BodyPart part, String key) {
        return baseCapability.hasCustomFloat(part, key);
    }

    @Override
    public String getCustomString(BodyPart part, String key) {
        return baseCapability.getCustomString(part, key);
    }

    @Override
    public boolean hasCustomString(BodyPart part, String key) {
        return baseCapability.hasCustomString(part, key);
    }

    @Override
    public boolean isPartBroken(BodyPart part) {
        return baseCapability.isPartBroken(part);
    }

    @Override
    public boolean isPartDestroyed(BodyPart part) {
        return baseCapability.isPartDestroyed(part);
    }

    @Override
    public boolean isPartSprained(BodyPart part) {
        return baseCapability.isPartSprained(part);
    }

    // Delegate write methods and trigger sync
    @Override
    public void setDamage(BodyPart part, float damage) {
        baseCapability.setDamage(part, damage);
        triggerSync();
    }

    @Override
    public void addDamage(BodyPart part, float amount) {
        baseCapability.addDamage(part, amount);
        triggerSync();
    }

    @Override
    public void damageAll(float amount) {
        baseCapability.damageAll(amount);
        triggerSync();
    }

    @Override
    public void healDamage(BodyPart part, float amount) {
        baseCapability.healDamage(part, amount);
        triggerSync();
    }

    @Override
    public void setCustomStatus(BodyPart part, String statusName, int level) {
        baseCapability.setCustomStatus(part, statusName, level);
        triggerSync();
    }

    @Override
    public void setCustomFloat(BodyPart part, String key, float value) {
        baseCapability.setCustomFloat(part, key, value);
        triggerSync();
    }

    @Override
    public void setCustomString(BodyPart part, String key, String value) {
        baseCapability.setCustomString(part, key, value);
        triggerSync();
    }

    @Override
    public void resetPart(BodyPart part) {
        baseCapability.resetPart(part);
        triggerSync();
    }

    @Override
    public void resetAll() {
        baseCapability.resetAll();
        triggerSync();
    }

    // NBT serialization delegates directly
    @Override
    public CompoundTag serializeNBT() {
        return baseCapability.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        baseCapability.deserializeNBT(tag);
        // Note: Don't trigger sync on deserialize as this is used for loading/cloning
    }
}
