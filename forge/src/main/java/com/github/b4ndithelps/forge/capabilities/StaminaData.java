package com.github.b4ndithelps.forge.capabilities;

import net.minecraft.nbt.CompoundTag;

/**
 * An implementation of the IStaminaData interface. Handles the storage and retrieval of all stamina data.
 * Also includes access to upgrade points
 */
public class StaminaData implements IStaminaData{
    private int currentStamina = 100;
    private int maxStamina = 100;
    private int usageTotal = 0;
    private int regenCooldown = 0;
    private int exhaustionLevel = 0;
    private boolean lastHurrahUsed = false;
    private boolean powersDisabled = false;
    private boolean initialized = false;
    private int upgradePoints = 0;
    private int pointsProgress = 0;

    @Override
    public int getCurrentStamina() {
        return currentStamina;
    }

    @Override
    public void setCurrentStamina(int stamina) {
        this.currentStamina = Math.min(stamina, this.maxStamina);
    }

    @Override
    public int getMaxStamina() {
        return maxStamina;
    }

    @Override
    public void setMaxStamina(int stamina) {
        // Reset the current stamina first to ensure current is never greater than max
        if (this.currentStamina > stamina) {
            this.currentStamina = stamina;
        }
        this.maxStamina = stamina;
    }

    @Override
    public int getUsageTotal() {
        return usageTotal;
    }

    @Override
    public void setUsageTotal(int usage) {
        this.usageTotal = usage;
    }

    @Override
    public int getRegenCooldown() {
        return regenCooldown;
    }

    @Override
    public void setRegenCooldown(int cooldown) {
        this.regenCooldown = cooldown;
    }

    @Override
    public int getExhaustionLevel() {
        return exhaustionLevel;
    }

    @Override
    public void setExhaustionLevel(int level) {
        this.exhaustionLevel = level;
    }

    @Override
    public boolean getLastHurrahUsed() {
        return lastHurrahUsed;
    }

    @Override
    public void setLastHurrahUsed(boolean used) {
        this.lastHurrahUsed = used;
    }

    @Override
    public boolean isPowersDisabled() {
        return powersDisabled;
    }

    @Override
    public void setPowersDisabled(boolean disabled) {
        this.powersDisabled = disabled;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    @Override
    public int getUpgradePoints() {
        return upgradePoints;
    }

    @Override
    public void setUpgradePoints(int points) {
        this.upgradePoints = points;
    }

    @Override
    public int getPointsProgress() {
        return pointsProgress;
    }

    @Override
    public void setPointsProgress(int progress) {
        this.pointsProgress = progress;
    }

    @Override
    public void saveNBTData(CompoundTag nbt) {
        nbt.putInt("currentStamina", currentStamina);
        nbt.putInt("maxStamina", maxStamina);
        nbt.putInt("usageTotal", usageTotal);
        nbt.putInt("regenCooldown", regenCooldown);
        nbt.putInt("exhaustionLevel", exhaustionLevel);
        nbt.putBoolean("lastHurrahUsed", lastHurrahUsed);
        nbt.putBoolean("powersDisabled", powersDisabled);
        nbt.putBoolean("initialized", initialized);
        nbt.putInt("upgradePoints", upgradePoints);
        nbt.putInt("pointsProgress", pointsProgress);
    }

    @Override
    public void loadNBTData(CompoundTag nbt) {
        currentStamina = nbt.getInt("currentStamina");
        maxStamina = nbt.getInt("maxStamina");
        usageTotal = nbt.getInt("usageTotal");
        regenCooldown = nbt.getInt("regenCooldown");
        exhaustionLevel = nbt.getInt("exhaustionLevel");
        lastHurrahUsed = nbt.getBoolean("lastHurrahUsed");
        powersDisabled = nbt.getBoolean("powersDisabled");
        initialized = nbt.getBoolean("initialized");
        upgradePoints = nbt.getInt("upgradePoints");
        pointsProgress = nbt.getInt("pointsProgress");
    }
}
