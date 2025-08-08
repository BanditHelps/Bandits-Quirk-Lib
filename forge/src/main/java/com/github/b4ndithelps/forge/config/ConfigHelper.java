package com.github.b4ndithelps.forge.config;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.values.BodyConstants;
import com.github.b4ndithelps.values.CreationShopConstants;
import com.github.b4ndithelps.values.StaminaConstants;
import dev.latvian.mods.kubejs.util.ConsoleJS;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * KubeJS helper class for config management
 * Provides methods for creating dynamic configs and retrieving constants
 */
public class ConfigHelper {

    // ===== Dynamic Config Management =====
    
    /**
     * Creates or updates a dynamic config value
     * @param key The config key
     * @param value The config value
     * @param description Optional description for the config
     */
    public static void setConfig(String key, Object value, String description) {
        ConfigManager.setDynamicConfig(key, value, description);
    }
    
    /**
     * Creates or updates a dynamic config value without description
     * @param key The config key
     * @param value The config value
     */
    public static void setConfig(String key, Object value) {
        ConfigManager.setDynamicConfig(key, value, null);
    }
    
    /**
     * Retrieves a dynamic config value
     * @param key The config key
     * @return The config value or null if not found
     */
    public static Object getConfig(String key) {
        return ConfigManager.getDynamicConfig(key);
    }
    
    /**
     * Retrieves a dynamic config value with a default fallback
     * @param key The config key
     * @param defaultValue The default value if config not found
     * @return The config value or default value
     */
    public static Object getConfig(String key, Object defaultValue) {
        return ConfigManager.getDynamicConfig(key, defaultValue);
    }
    
    /**
     * Checks if a dynamic config exists
     * @param key The config key
     * @return true if the config exists
     */
    public static boolean hasConfig(String key) {
        return ConfigManager.hasDynamicConfig(key);
    }
    
    /**
     * Gets all dynamic config keys
     * @return Set of all config keys
     */
    public static Set<String> getConfigKeys() {
        return ConfigManager.getDynamicConfigKeys();
    }
    
    /**
     * Saves all dynamic configs to file
     */
    public static void saveConfigs() {
        ConfigManager.saveDynamicConfigs();
        BanditsQuirkLibForge.LOGGER.info("Dynamic configs saved to file");
    }
    
    /**
     * Generates a config file template for the specified addon
     * Only runs if .delete_to_reload_dynamic_configs file does NOT exist in /config/bql folder
     * Creates the flag file after successful generation to prevent future regenerations
     * @param addonName The name of the addon
     * @param configs Map of config key -> default value
     * @param descriptions Map of config key -> description (optional)
     */
    public static void generateConfigTemplate(String addonName, Map<String, Object> configs, Map<String, String> descriptions) {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path reloadFlagFile = configDir.resolve("bql/.delete_to_reload_dynamic_configs");
            
            if (Files.exists(reloadFlagFile)) {
                BanditsQuirkLibForge.LOGGER.info("Skipping config template generation for addon '{}' - reload flag file exists. Delete '/config/bql/.delete_to_reload_dynamic_configs' to allow regeneration.", addonName);
                return;
            }
            
            for (Map.Entry<String, Object> entry : configs.entrySet()) {
                String key = addonName + "." + entry.getKey();
                String description = descriptions != null ? descriptions.get(entry.getKey()) : null;
                ConfigManager.setDynamicConfig(key, entry.getValue(), description);
            }
            ConfigManager.saveDynamicConfigs();
            BanditsQuirkLibForge.LOGGER.info("Generated config template for addon: " + addonName);
            
            // Create the flag file to prevent future regenerations
            try {
                Files.createDirectories(reloadFlagFile.getParent());
                if (!Files.exists(reloadFlagFile)) {
                    Files.createFile(reloadFlagFile);
                    BanditsQuirkLibForge.LOGGER.info("Created reload flag file to prevent future config regenerations. Delete '/config/bql/.delete_to_reload_dynamic_configs' to allow regeneration.");
                } else {
                    BanditsQuirkLibForge.LOGGER.info("Reload flag file already exists at '/config/bql/.delete_to_reload_dynamic_configs'.");
                }
            } catch (Exception flagFileException) {
                BanditsQuirkLibForge.LOGGER.warn("Failed to create reload flag file, config may regenerate on next startup: " + flagFileException.getMessage());
            }
            
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Failed to check for reload flag file, proceeding with config generation for addon: " + addonName);
            // Fall back to original behavior if file check fails
            for (Map.Entry<String, Object> entry : configs.entrySet()) {
                String key = addonName + "." + entry.getKey();
                String description = descriptions != null ? descriptions.get(entry.getKey()) : null;
                ConfigManager.setDynamicConfig(key, entry.getValue(), description);
            }
            ConfigManager.saveDynamicConfigs();
            BanditsQuirkLibForge.LOGGER.info("Generated config template for addon: " + addonName);
        }
    }
    
    /**
     * Forces generation of a config file template for the specified addon
     * Ignores the presence of .delete_to_reload_dynamic_configs file
     * Creates the flag file after successful generation to prevent future regenerations
     * @param addonName The name of the addon
     * @param configs Map of config key -> default value
     * @param descriptions Map of config key -> description (optional)
     */
    public static void forceGenerateConfigTemplate(String addonName, Map<String, Object> configs, Map<String, String> descriptions) {
        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            String key = addonName + "." + entry.getKey();
            String description = descriptions != null ? descriptions.get(entry.getKey()) : null;
            ConfigManager.setDynamicConfig(key, entry.getValue(), description);
        }
        ConfigManager.saveDynamicConfigs();
        BanditsQuirkLibForge.LOGGER.info("Force generated config template for addon: " + addonName);
        
        // Create the flag file to prevent future regenerations
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path reloadFlagFile = configDir.resolve("bql/.delete_to_reload_dynamic_configs");
            Files.createDirectories(reloadFlagFile.getParent());
            if (!Files.exists(reloadFlagFile)) {
                Files.createFile(reloadFlagFile);
                BanditsQuirkLibForge.LOGGER.info("Created reload flag file to prevent future config regenerations. Delete '/config/bql/.delete_to_reload_dynamic_configs' to allow regeneration.");
            } else {
                BanditsQuirkLibForge.LOGGER.info("Reload flag file already exists at '/config/bql/.delete_to_reload_dynamic_configs'.");
            }
        } catch (Exception flagFileException) {
            BanditsQuirkLibForge.LOGGER.warn("Failed to create reload flag file, config may regenerate on next startup: " + flagFileException.getMessage());
        }
    }
    
    // ===== Body Constants Retrieval =====
    
    /**
     * Gets the damage stage percentages array
     * @return Array of damage stage percentages
     */
    public static float[] getDamageStagePercentages() {
        return BodyConstants.DAMAGE_STAGE_PERCENTAGES.clone();
    }
    
    /**
     * Gets the maximum damage value
     * @return Maximum damage value
     */
    public static float getMaxDamage() {
        return BodyConstants.MAX_DAMAGE;
    }
    
    // ===== Stamina Constants Retrieval =====
    
    /**
     * Gets the exhaustion levels array
     * @return Array of exhaustion levels
     */
    public static int[] getExhaustionLevels() {
        return StaminaConstants.EXHAUSTION_LEVELS.clone();
    }
    
    /**
     * Gets the exhaustion multipliers array
     * @return Array of exhaustion multipliers
     */
    public static double[] getExhaustionMultipliers() {
        return StaminaConstants.EXHAUSTION_MULTIPLIERS.clone();
    }
    
    /**
     * Gets the stamina regeneration cooldowns array
     * @return Array of stamina regen cooldowns
     */
    public static int[] getStaminaRegenCooldowns() {
        return StaminaConstants.STAMINA_REGEN_COOLDOWNS.clone();
    }
    
    /**
     * Gets the stamina regeneration rates array
     * @return Array of stamina regen rates
     */
    public static double[] getStaminaRegenRates() {
        return StaminaConstants.STAMINA_REGEN_RATE.clone();
    }
    
    /**
     * Gets the stamina gain chance
     * @return Stamina gain chance
     */
    public static double getStaminaGainChance() {
        return StaminaConstants.STAMINA_GAIN_CHANCE;
    }
    
    /**
     * Gets the stamina gain exhausted chance
     * @return Stamina gain exhausted chance
     */
    public static double getStaminaGainExhaustedChance() {
        return StaminaConstants.STAMINA_GAIN_EXHAUSTED_CHANCE;
    }
    
    /**
     * Gets the stamina enable percent
     * @return Stamina enable percent
     */
    public static double getStaminaEnablePercent() {
        return StaminaConstants.STAMINA_ENABLE_PERCENT;
    }
    
    /**
     * Gets the stamina gain requirement
     * @return Stamina gain requirement
     */
    public static int getStaminaGainReq() {
        return StaminaConstants.STAMINA_GAIN_REQ;
    }
    
    /**
     * Gets the stamina max increase
     * @return Stamina max increase
     */
    public static int getStaminaMaxIncrease() {
        return StaminaConstants.STAMINA_MAX_INCREASE;
    }
    
    /**
     * Gets the upgrade point cost
     * @return Upgrade point cost
     */
    public static int getUpgradePointCost() {
        return StaminaConstants.UPGRADE_POINT_COST;
    }
    
    /**
     * Gets the Plus Ultra tag
     * @return Plus Ultra tag
     */
    public static String getPlusUltraTag() {
        return StaminaConstants.PLUS_ULTRA_TAG;
    }
    
    /**
     * Gets the Powers Disabled tag
     * @return Powers Disabled tag
     */
    public static String getPowersDisabledTag() {
        return StaminaConstants.POWERS_DISABLED_TAG;
    }
    
    /**
     * Gets the stamina percent scoreboard name
     * @return Stamina percent scoreboard
     */
    public static String getStaminaPercentScoreboard() {
        return StaminaConstants.STAMINA_PERCENT_SCOREBOARD;
    }
    
    /**
     * Gets the upgrade points scoreboard name
     * @return Upgrade points scoreboard
     */
    public static String getUpgradePointsScoreboard() {
        return StaminaConstants.UPGRADE_POINTS_SCOREBOARD;
    }
    
    /**
     * Gets the points to upgrade
     * @return Points to upgrade
     */
    public static int getPointsToUpgrade() {
        return StaminaConstants.POINTS_TO_UPGRADE;
    }
    
    /**
     * Gets the starting stamina minimum
     * @return Starting stamina minimum
     */
    public static int getStartingStaminaMin() {
        return StaminaConstants.STARTING_STAMINA_MIN;
    }
    
    /**
     * Gets the starting stamina maximum
     * @return Starting stamina maximum
     */
    public static int getStartingStaminaMax() {
        return StaminaConstants.STARTING_STAMINA_MAX;
    }
    
    // ===== Creation Shop Constants Retrieval =====

    /**
     * Gets the multiplier of stamina that creating an item will use
     */
    public static double getCreationStaminaCost() { return CreationShopConstants.CREATION_STAMINA_COST; }

    /**
     * Gets a copy of the Bit Map 1 Table
     * @return Map of item -> bit value
     */
    public static Map<String, Integer> getBitMap1Table() {
        return new HashMap<>(CreationShopConstants.BIT_MAP_1_TABLE);
    }
    
    /**
     * Gets a copy of the Bit Map 2 Table
     * @return Map of item -> bit value
     */
    public static Map<String, Integer> getBitMap2Table() {
        return new HashMap<>(CreationShopConstants.BIT_MAP_2_TABLE);
    }
    
    /**
     * Gets a copy of the Bit Map 3 Table
     * @return Map of item -> bit value
     */
    public static Map<String, Integer> getBitMap3Table() {
        return new HashMap<>(CreationShopConstants.BIT_MAP_3_TABLE);
    }
    
    /**
     * Gets a copy of the Bit Map 4 Table
     * @return Map of item -> bit value
     */
    public static Map<String, Integer> getBitMap4Table() {
        return new HashMap<>(CreationShopConstants.BIT_MAP_4_TABLE);
    }
    
    /**
     * Gets a copy of the Bit Map 5 Table
     * @return Map of item -> bit value
     */
    public static Map<String, Integer> getBitMap5Table() {
        return new HashMap<>(CreationShopConstants.BIT_MAP_5_TABLE);
    }
    
    /**
     * Gets a copy of the Item Buy Cost Table
     * @return Map of item -> buy cost
     */
    public static Map<String, Integer> getItemBuyCostTable() {
        return new HashMap<>(CreationShopConstants.ITEM_BUY_COST_TABLE);
    }
    
    /**
     * Gets a copy of the Enchant Buy Cost Table
     * @return Map of enchantment -> buy cost
     */
    public static Map<String, Integer> getEnchantBuyCostTable() {
        return new HashMap<>(CreationShopConstants.ENCHANT_BUY_COST_TABLE);
    }
    
    /**
     * Gets a copy of the Item Learn Cost Table
     * @return Map of item -> learn cost
     */
    public static Map<String, Integer> getItemLearnCostTable() {
        return new HashMap<>(CreationShopConstants.ITEM_LEARN_COST_TABLE);
    }
    
    /**
     * Gets a copy of the Enchant Learn Cost Table
     * @return Map of enchantment -> learn cost
     */
    public static Map<String, Integer> getEnchantLearnCostTable() {
        return new HashMap<>(CreationShopConstants.ENCHANT_LEARN_COST_TABLE);
    }
    
    /**
     * Gets a specific bit value from any bit map
     * @param mapNumber The map number (1-5)
     * @param itemId The item ID
     * @return The bit value or 0 if not found
     */
    public static int getBitValue(int mapNumber, String itemId) {
        Map<String, Integer> map;
        switch (mapNumber) {
            case 1: map = CreationShopConstants.BIT_MAP_1_TABLE; break;
            case 2: map = CreationShopConstants.BIT_MAP_2_TABLE; break;
            case 3: map = CreationShopConstants.BIT_MAP_3_TABLE; break;
            case 4: map = CreationShopConstants.BIT_MAP_4_TABLE; break;
            case 5: map = CreationShopConstants.BIT_MAP_5_TABLE; break;
            default: return 0;
        }
        return map.getOrDefault(itemId, 0);
    }
    
    /**
     * Gets the buy cost for an item
     * @param itemId The item ID
     * @return The buy cost or 0 if not found
     */
    public static int getItemBuyCost(String itemId) {
        return CreationShopConstants.ITEM_BUY_COST_TABLE.getOrDefault(itemId, 0);
    }
    
    /**
     * Gets the learn cost for an item
     * @param itemId The item ID
     * @return The learn cost or 0 if not found
     */
    public static int getItemLearnCost(String itemId) {
        return CreationShopConstants.ITEM_LEARN_COST_TABLE.getOrDefault(itemId, 0);
    }
    
    /**
     * Gets the buy cost for an enchantment
     * @param enchantId The enchantment ID
     * @return The buy cost or 0 if not found
     */
    public static int getEnchantBuyCost(String enchantId) {
        return CreationShopConstants.ENCHANT_BUY_COST_TABLE.getOrDefault(enchantId, 0);
    }
    
    /**
     * Gets the learn cost for an enchantment
     * @param enchantId The enchantment ID
     * @return The learn cost or 0 if not found
     */
    public static int getEnchantLearnCost(String enchantId) {
        return CreationShopConstants.ENCHANT_LEARN_COST_TABLE.getOrDefault(enchantId, 0);
    }
    
    // ===== Utility Methods =====
    
    /**
     * Reloads all configs from files
     */
    public static void reloadConfigs() {
        ConfigManager.loadDynamicConfigs();
        ConfigManager.updateConstants();
        BanditsQuirkLibForge.LOGGER.info("Configs reloaded from files");
    }
    
    /**
     * Forces a complete reload of all config systems (for testing/debugging)
     */
    public static void forceReloadAll() {
        ConfigManager.forceReloadAll();
        BanditsQuirkLibForge.LOGGER.info("Forced complete config reload");
    }
    
    /**
     * Gets all current config values as a map (for debugging/inspection)
     * @return Map of all config values
     */
    public static Map<String, Object> getAllConfigs() {
        Map<String, Object> allConfigs = new HashMap<>();
        
        // Add static configs
        allConfigs.put("body.damage_stage_percentages", Arrays.toString(BodyConstants.DAMAGE_STAGE_PERCENTAGES));
        allConfigs.put("body.max_damage", BodyConstants.MAX_DAMAGE);
        
        allConfigs.put("stamina.exhaustion_levels", Arrays.toString(StaminaConstants.EXHAUSTION_LEVELS));
        allConfigs.put("stamina.exhaustion_multipliers", Arrays.toString(StaminaConstants.EXHAUSTION_MULTIPLIERS));
        allConfigs.put("stamina.stamina_gain_chance", StaminaConstants.STAMINA_GAIN_CHANCE);
        
        // Add dynamic configs
        for (String key : ConfigManager.getDynamicConfigKeys()) {
            allConfigs.put("dynamic." + key, ConfigManager.getDynamicConfig(key));
        }
        
        return allConfigs;
    }
} 