package com.github.b4ndithelps.forge.capabilities.Body;

import net.minecraft.nbt.CompoundTag;

public interface IBodyStatusCapability {
    BodyStatusData getBodyPartData(BodyPart part);

    float getDamage(BodyPart part);
    void setDamage(BodyPart part, float damage);
    void addDamage(BodyPart part, float amount);
    void damageAll(float amount);
    void healDamage(BodyPart part, float amount);

    DamageStage getDamageStage(BodyPart part);
    float getDamagePercentage(BodyPart part);

    int getCustomStatus(BodyPart part, String statusName);
    void setCustomStatus(BodyPart part, String statusName, int level);
    boolean hasCustomStatus(BodyPart part, String statusName);

    // Enhanced custom data methods
    float getCustomFloat(BodyPart part, String key);
    void setCustomFloat(BodyPart part, String key, float value);
    boolean hasCustomFloat(BodyPart part, String key);

    String getCustomString(BodyPart part, String key);
    void setCustomString(BodyPart part, String key, String value);
    boolean hasCustomString(BodyPart part, String key);

    boolean isPartBroken(BodyPart part);
    boolean isPartDestroyed(BodyPart part);
    boolean isPartSprained(BodyPart part);

    void resetPart(BodyPart part);
    void resetAll();

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);
}
