package com.github.b4ndithelps.forge.capabilities.Body;

/**
 * The CustomStatus is to be used via KubeJS developers in order to add values to each body part.
 * For example, you could store a power level in each limb, for Fajin, or frost level for HHHC
 */
public class CustomStatus {
    private final String name;
    private final int maxLevel;
    private final String[] stageNames;

    public CustomStatus(String name, int maxLevel, String... stageNames) {
        this.name = name;
        this.maxLevel = maxLevel;
        this.stageNames = stageNames;
    }

    public String getName() {
        return name;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public String getStageName(int level) {
        if (level < 0 || level >= stageNames.length) {
            return "unknown";
        }
        return stageNames[level];
    }

    public boolean isValidLevel(int level) {
        return level >= 0 && level <= maxLevel;
    }
}
