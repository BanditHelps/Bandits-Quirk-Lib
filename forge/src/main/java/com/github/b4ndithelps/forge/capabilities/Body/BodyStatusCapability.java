package com.github.b4ndithelps.forge.capabilities.Body;

import net.minecraft.nbt.CompoundTag;

import java.util.EnumMap;
import java.util.Map;

public class BodyStatusCapability implements IBodyStatusCapability {
    private final Map<BodyPart, BodyStatusData> bodyParts;

    public BodyStatusCapability() {
        this.bodyParts = new EnumMap<>(BodyPart.class);

        // Initialize all body parts
        for (BodyPart part : BodyPart.values()) {
            bodyParts.put(part, new BodyStatusData());
        }
    }

    @Override
    public BodyStatusData getBodyPartData(BodyPart part) {
        return bodyParts.get(part);
    }

    @Override
    public float getDamage(BodyPart part) {
        return bodyParts.get(part).getDamage();
    }

    @Override
    public void setDamage(BodyPart part, float damage) {
        bodyParts.get(part).setDamage(damage);
    }

    @Override
    public void addDamage(BodyPart part, float amount) {
        bodyParts.get(part).addDamage(amount);
    }

    @Override
    public void damageAll(float amount) {
        bodyParts.forEach((bodyPart, bodyStatusData) -> {
            bodyStatusData.addDamage(amount);
        });
    }

    @Override
    public void healDamage(BodyPart part, float amount) {
        BodyStatusData data = bodyParts.get(part);
        data.setDamage(data.getDamage() - amount);
    }

    @Override
    public DamageStage getDamageStage(BodyPart part) {
        return bodyParts.get(part).getStage();
    }

    @Override
    public float getDamagePercentage(BodyPart part) {
        return bodyParts.get(part).getDamagePercentage();
    }

    @Override
    public int getCustomStatus(BodyPart part, String statusName) {
        return bodyParts.get(part).getCustomStatus(statusName);
    }

    @Override
    public void setCustomStatus(BodyPart part, String statusName, int level) {
        bodyParts.get(part).setCustomStatus(statusName, level);
    }

    @Override
    public boolean hasCustomStatus(BodyPart part, String statusName) {
        return bodyParts.get(part).hasCustomStatus(statusName);
    }

    @Override
    public float getCustomFloat(BodyPart part, String key) {
        return bodyParts.get(part).getCustomFloat(key);
    }

    @Override
    public void setCustomFloat(BodyPart part, String key, float value) {
        bodyParts.get(part).setCustomFloat(key, value);
    }

    @Override
    public boolean hasCustomFloat(BodyPart part, String key) {
        return bodyParts.get(part).hasCustomFloat(key);
    }

    @Override
    public String getCustomString(BodyPart part, String key) {
        return bodyParts.get(part).getCustomString(key);
    }

    @Override
    public void setCustomString(BodyPart part, String key, String value) {
        bodyParts.get(part).setCustomString(key, value);
    }

    @Override
    public boolean hasCustomString(BodyPart part, String key) {
        return bodyParts.get(part).hasCustomString(key);
    }

    @Override
    public boolean isPartBroken(BodyPart part) {
        return getDamageStage(part) == DamageStage.BROKEN;
    }

    @Override
    public boolean isPartDestroyed(BodyPart part) {
        return getDamageStage(part) == DamageStage.DESTROYED;
    }

    @Override
    public boolean isPartSprained(BodyPart part) {
        return getDamageStage(part) == DamageStage.SPRAINED;
    }

    @Override
    public void resetPart(BodyPart part) {
        bodyParts.put(part, new BodyStatusData());
    }

    @Override
    public void resetAll() {
        for (BodyPart part : BodyPart.values()) {
            resetPart(part);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<BodyPart, BodyStatusData> entry : bodyParts.entrySet()) {
            tag.put(entry.getKey().name(), entry.getValue().serializeNBT());
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        for (BodyPart part : BodyPart.values()) {
            if (tag.contains(part.name())) {
                bodyParts.get(part).deserializeNBT(tag.getCompound(part.name()));
            }
        }
    }
}
