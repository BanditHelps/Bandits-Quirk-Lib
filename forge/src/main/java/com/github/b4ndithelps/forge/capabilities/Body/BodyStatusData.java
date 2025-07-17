package com.github.b4ndithelps.forge.capabilities.Body;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.github.b4ndithelps.values.BodyConstants.DAMAGE_STAGE_PERCENTAGES;

/**
 * Stores damage information as well as custom status values for each body part
 */
public class BodyStatusData {
    private float damage;
    private float maxDamage;
    private DamageStage stage;
    private Map<String, Integer> customStatuses;
    private Map<String, Float> customFloatData;
    private Map<String, String> customStringData;

    public BodyStatusData(float maxDamage) {
        this.damage = 0;
        this.maxDamage = maxDamage;
        this.stage = DamageStage.HEALTHY;
        this.customStatuses = new HashMap<>();
        this.customFloatData = new HashMap<>();
        this.customStringData = new HashMap<>();
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = Math.max(0, Math.min(damage, maxDamage));
        updateDamageStage();
    }

    public void addDamage(float amount) {
        setDamage(this.damage + amount);
    }

    public float getMaxDamage() {
        return maxDamage;
    }

    public DamageStage getStage() {
        return stage;
    }

    public float getDamagePercentage() {
        return maxDamage > 0 ? damage / maxDamage : 0;
    }

    // Determines the stage based on the defined Stage Percentages.
    // DAMAGE_STAGE_PERCENTAGES = [HEALTHY, SPRAINED, BROKEN, DESTROYED]
    private void updateDamageStage() {
        float percentage = getDamagePercentage();
        if (percentage >= DAMAGE_STAGE_PERCENTAGES[3]) {
            stage = DamageStage.DESTROYED;
        } else if (percentage >= DAMAGE_STAGE_PERCENTAGES[2]) {
            stage = DamageStage.BROKEN;
        } else if (percentage >= DAMAGE_STAGE_PERCENTAGES[1]) {
            stage = DamageStage.SPRAINED;
        } else {
            stage = DamageStage.HEALTHY;
        }
    }

    public int getCustomStatus(String statusName) {
        return customStatuses.getOrDefault(statusName, 0);
    }

    public void setCustomStatus(String statusName, int level) {
        if (level <= 0) {
            customStatuses.remove(statusName);
        } else {
            customStatuses.put(statusName, level);
        }
    }

    public boolean hasCustomStatus(String statusName) {
        return customStatuses.containsKey(statusName) && customStatuses.get(statusName) > 0;
    }

    public Set<String> getActiveCustomStatuses() {
        return customStatuses.keySet();
    }

    // Float data methods
    public float getCustomFloat(String key) {
        return customFloatData.getOrDefault(key, 0.0f);
    }

    public void setCustomFloat(String key, float value) {
        if (value == 0.0f) {
            customFloatData.remove(key);
        } else {
            customFloatData.put(key, value);
        }
    }

    public boolean hasCustomFloat(String key) {
        return customFloatData.containsKey(key);
    }

    // String data methods
    public String getCustomString(String key) {
        return customStringData.getOrDefault(key, "");
    }

    public void setCustomString(String key, String value) {
        if (value == null || value.isEmpty()) {
            customStringData.remove(key);
        } else {
            customStringData.put(key, value);
        }
    }

    public boolean hasCustomString(String key) {
        return customStringData.containsKey(key) && !customStringData.get(key).isEmpty();
    }

    // Get all custom data keys
    public Set<String> getAllCustomKeys() {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(customStatuses.keySet());
        allKeys.addAll(customFloatData.keySet());
        allKeys.addAll(customStringData.keySet());
        return allKeys;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("damage", damage);
        tag.putFloat("maxDamage", maxDamage);
        tag.putString("stage", stage.name());

        CompoundTag customTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : customStatuses.entrySet()) {
            customTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("customStatuses", customTag);

        CompoundTag floatTag = new CompoundTag();
        for (Map.Entry<String, Float> entry : customFloatData.entrySet()) {
            floatTag.putFloat(entry.getKey(), entry.getValue());
        }
        tag.put("customFloatData", floatTag);

        CompoundTag stringTag = new CompoundTag();
        for (Map.Entry<String, String> entry : customStringData.entrySet()) {
            stringTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("customStringData", stringTag);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        damage = tag.getFloat("damage");
        maxDamage = tag.getFloat("maxDamage");
        stage = DamageStage.valueOf(tag.getString("stage"));

        customStatuses.clear();
        if (tag.contains("customStatuses")) {
            CompoundTag customTag = tag.getCompound("customStatuses");
            for (String key : customTag.getAllKeys()) {
                customStatuses.put(key, customTag.getInt(key));
            }
        }

        customFloatData.clear();
        if (tag.contains("customFloatData")) {
            CompoundTag floatTag = tag.getCompound("customFloatData");
            for (String key : floatTag.getAllKeys()) {
                customFloatData.put(key, floatTag.getFloat(key));
            }
        }

        customStringData.clear();
        if (tag.contains("customStringData")) {
            CompoundTag stringTag = tag.getCompound("customStringData");
            for (String key : stringTag.getAllKeys()) {
                customStringData.put(key, stringTag.getString(key));
            }
        }
    }
}
