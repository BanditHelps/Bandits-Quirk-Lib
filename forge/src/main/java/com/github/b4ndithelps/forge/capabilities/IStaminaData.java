package com.github.b4ndithelps.forge.capabilities;

import net.minecraft.nbt.CompoundTag;

public interface IStaminaData {
    int getCurrentStamina();
    void setCurrentStamina(int stamina);

    int getMaxStamina();
    void setMaxStamina(int stamina);

    int getUsageTotal();
    void setUsageTotal(int usage);

    int getRegenCooldown();
    void setRegenCooldown(int cooldown);

    int getExhaustionLevel();
    void setExhaustionLevel(int level);

    boolean getLastHurrahUsed();
    void setLastHurrahUsed(boolean used);

    boolean isPowersDisabled();
    void setPowersDisabled(boolean disabled);

    boolean isInitialized();
    void setInitialized(boolean initialized);

    int getUpgradePoints();
    void setUpgradePoints(int points);

    int getPointsProgress();
    void setPointsProgress(int progress);

    void saveNBTData(CompoundTag nbt);
    void loadNBTData(CompoundTag nbt);
}
