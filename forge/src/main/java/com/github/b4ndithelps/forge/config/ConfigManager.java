package com.github.b4ndithelps.forge.config;

import com.github.b4ndithelps.values.BodyConstants;
import com.github.b4ndithelps.values.CreationShopConstants;
import com.github.b4ndithelps.values.StaminaConstants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("BQL-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Dynamic config storage for KubeJS
    private static final Map<String, Object> dynamicConfigs = new ConcurrentHashMap<>();
    private static final Map<String, String> configDescriptions = new ConcurrentHashMap<>();
    
    // Creation shop data cache (only price tables now)
    private static CreationShopData creationShopData;
    
    public static void initialize() {
        // Register the main config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BQLConfig.SPEC, "bql-common.toml");
        
        LOGGER.info("Config system initialized - waiting for config to load");
    }
    
    public static void updateConstants() {
        LOGGER.info("Updating constants from config...");
        
        // Update Body Constants
        List<? extends Double> damagePercentages = BQLConfig.INSTANCE.damageStagePercentages.get();
        if (damagePercentages.size() >= 4) {
            BodyConstants.DAMAGE_STAGE_PERCENTAGES = new float[]{
                damagePercentages.get(0).floatValue(),
                damagePercentages.get(1).floatValue(),
                damagePercentages.get(2).floatValue(),
                damagePercentages.get(3).floatValue()
            };
            LOGGER.info("Updated damage stage percentages: {}", Arrays.toString(BodyConstants.DAMAGE_STAGE_PERCENTAGES));
        }
        BodyConstants.MAX_DAMAGE = BQLConfig.INSTANCE.maxDamage.get().floatValue();
        LOGGER.info("Updated max damage: {}", BodyConstants.MAX_DAMAGE);
        
        // Update Stamina Constants
        List<? extends Integer> exhaustionLevels = BQLConfig.INSTANCE.exhaustionLevels.get();
        if (exhaustionLevels.size() == 5) {
            StaminaConstants.EXHAUSTION_LEVELS = exhaustionLevels.stream().mapToInt(Integer::intValue).toArray();
            LOGGER.info("Updated exhaustion levels: {}", Arrays.toString(StaminaConstants.EXHAUSTION_LEVELS));
        }
        
        List<? extends Double> exhaustionMultipliers = BQLConfig.INSTANCE.exhaustionMultipliers.get();
        if (exhaustionMultipliers.size() == 5) {
            StaminaConstants.EXHAUSTION_MULTIPLIERS = exhaustionMultipliers.stream().mapToDouble(Double::doubleValue).toArray();
            LOGGER.info("Updated exhaustion multipliers: {}", Arrays.toString(StaminaConstants.EXHAUSTION_MULTIPLIERS));
        }
        
        List<? extends Integer> regenCooldowns = BQLConfig.INSTANCE.staminaRegenCooldowns.get();
        if (regenCooldowns.size() == 5) {
            StaminaConstants.STAMINA_REGEN_COOLDOWNS = regenCooldowns.stream().mapToInt(Integer::intValue).toArray();
            LOGGER.info("Updated stamina regen cooldowns: {}", Arrays.toString(StaminaConstants.STAMINA_REGEN_COOLDOWNS));
        }
        
        List<? extends Double> regenRates = BQLConfig.INSTANCE.staminaRegenRate.get();
        if (regenRates.size() == 5) {
            StaminaConstants.STAMINA_REGEN_RATE = regenRates.stream().mapToDouble(Double::doubleValue).toArray();
            LOGGER.info("Updated stamina regen rates: {}", Arrays.toString(StaminaConstants.STAMINA_REGEN_RATE));
        }
        
        // Update other stamina constants directly (no reflection needed)
        StaminaConstants.STAMINA_GAIN_CHANCE = BQLConfig.INSTANCE.staminaGainChance.get();
        StaminaConstants.STAMINA_GAIN_EXHAUSTED_CHANCE = BQLConfig.INSTANCE.staminaGainExhaustedChance.get();
        StaminaConstants.STAMINA_ENABLE_PERCENT = BQLConfig.INSTANCE.staminaEnablePercent.get();
        StaminaConstants.STAMINA_GAIN_REQ = BQLConfig.INSTANCE.staminaGainReq.get();
        StaminaConstants.STAMINA_MAX_INCREASE = BQLConfig.INSTANCE.staminaMaxIncrease.get();
        StaminaConstants.UPGRADE_POINT_COST = BQLConfig.INSTANCE.upgradePointCost.get();
        StaminaConstants.POINTS_TO_UPGRADE = BQLConfig.INSTANCE.pointsToUpgrade.get();
        StaminaConstants.STARTING_STAMINA_MIN = BQLConfig.INSTANCE.startingStaminaMin.get();
        StaminaConstants.STARTING_STAMINA_MAX = BQLConfig.INSTANCE.startingStaminaMax.get();
        
        LOGGER.info("Updated stamina constants - gain chance: {}, max damage: {}", 
                   StaminaConstants.STAMINA_GAIN_CHANCE, BodyConstants.MAX_DAMAGE);
        
        LOGGER.info("Constants updated from config");
    }
    
    public static void loadCreationShopData() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            String configPath = BQLConfig.INSTANCE.creationShopDataPath.get();
            Path creationShopFile = configDir.resolve(configPath);
        
            try {
                if (!Files.exists(creationShopFile)) {
                    // Create default creation shop data file
                    createDefaultCreationShopData(creationShopFile);
                }
                
                String content = Files.readString(creationShopFile);
                creationShopData = GSON.fromJson(content, CreationShopData.class);
                
                // Update the constants class price tables if needed
                updateCreationShopConstants();
                
                LOGGER.info("Loaded creation shop data from: " + creationShopFile);
                
            } catch (Exception e) {
                LOGGER.error("Failed to load creation shop data", e);
                creationShopData = createDefaultCreationShopDataObject();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to access config value for creation shop data path, using default", e);
            // Use default path when config is not available
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path creationShopFile = configDir.resolve("bql/creation_shop_data.json");
            
            try {
                if (!Files.exists(creationShopFile)) {
                    createDefaultCreationShopData(creationShopFile);
                }
                
                String content = Files.readString(creationShopFile);
                creationShopData = GSON.fromJson(content, CreationShopData.class);
                updateCreationShopConstants();
                
                LOGGER.info("Loaded creation shop data from default path: " + creationShopFile);
            } catch (Exception e2) {
                LOGGER.error("Failed to load creation shop data from default path", e2);
                creationShopData = createDefaultCreationShopDataObject();
            }
        }
    }
    
    private static void createDefaultCreationShopData(Path file) throws IOException {
        creationShopData = createDefaultCreationShopDataObject();
        
        Files.createDirectories(file.getParent());
        Files.writeString(file, GSON.toJson(creationShopData));
        
        LOGGER.info("Created default creation shop data file: " + file);
    }
    
    private static CreationShopData createDefaultCreationShopDataObject() {
        CreationShopData data = new CreationShopData();
        // Initialize all configurable cost tables
        data.creationPriceTable = new HashMap<>(CreationShopConstants.ITEM_BUY_COST_TABLE);
        data.creationEnchantPriceTable = new HashMap<>(CreationShopConstants.ENCHANT_BUY_COST_TABLE);
        data.creationItemLearnCostTable = new HashMap<>(CreationShopConstants.ITEM_LEARN_COST_TABLE);
        data.creationEnchantLearnCostTable = new HashMap<>(CreationShopConstants.ENCHANT_LEARN_COST_TABLE);
        return data;
    }
    
    private static void updateCreationShopConstants() {
        if (creationShopData == null) {
            LOGGER.warn("Creation shop data is null, cannot update constants");
            return;
        }
        
        // Update buy cost tables
        if (creationShopData.creationPriceTable != null) {
            CreationShopConstants.ITEM_BUY_COST_TABLE.clear();
            CreationShopConstants.ITEM_BUY_COST_TABLE.putAll(creationShopData.creationPriceTable);
        }
        
        if (creationShopData.creationEnchantPriceTable != null) {
            CreationShopConstants.ENCHANT_BUY_COST_TABLE.clear();
            CreationShopConstants.ENCHANT_BUY_COST_TABLE.putAll(creationShopData.creationEnchantPriceTable);
        }
        
        // Update learn cost tables
        if (creationShopData.creationItemLearnCostTable != null) {
            CreationShopConstants.ITEM_LEARN_COST_TABLE.clear();
            CreationShopConstants.ITEM_LEARN_COST_TABLE.putAll(creationShopData.creationItemLearnCostTable);
        }
        
        if (creationShopData.creationEnchantLearnCostTable != null) {
            CreationShopConstants.ENCHANT_LEARN_COST_TABLE.clear();
            CreationShopConstants.ENCHANT_LEARN_COST_TABLE.putAll(creationShopData.creationEnchantLearnCostTable);
        }
        
        LOGGER.info("Updated creation shop cost tables - Item Buy: {}, Enchant Buy: {}, Item Learn: {}, Enchant Learn: {}", 
                   CreationShopConstants.ITEM_BUY_COST_TABLE.size(), 
                   CreationShopConstants.ENCHANT_BUY_COST_TABLE.size(),
                   CreationShopConstants.ITEM_LEARN_COST_TABLE.size(),
                   CreationShopConstants.ENCHANT_LEARN_COST_TABLE.size());
    }
    
    // Dynamic config methods for KubeJS
    public static void setDynamicConfig(String key, Object value, String description) {
        dynamicConfigs.put(key, value);
        if (description != null) {
            configDescriptions.put(key, description);
        }
        LOGGER.info("Set dynamic config: {} = {}", key, value);
    }
    
    public static Object getDynamicConfig(String key) {
        return dynamicConfigs.get(key);
    }
    
    public static Object getDynamicConfig(String key, Object defaultValue) {
        return dynamicConfigs.getOrDefault(key, defaultValue);
    }
    
    public static boolean hasDynamicConfig(String key) {
        return dynamicConfigs.containsKey(key);
    }
    
    public static Set<String> getDynamicConfigKeys() {
        return new HashSet<>(dynamicConfigs.keySet());
    }
    
    public static void saveDynamicConfigs() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path dynamicConfigFile = configDir.resolve("bql/dynamic_configs.json");
            
            Files.createDirectories(dynamicConfigFile.getParent());
            
            JsonObject root = new JsonObject();
            JsonObject configs = new JsonObject();
            JsonObject descriptions = new JsonObject();
            
            for (Map.Entry<String, Object> entry : dynamicConfigs.entrySet()) {
                configs.add(entry.getKey(), GSON.toJsonTree(entry.getValue()));
            }
            
            for (Map.Entry<String, String> entry : configDescriptions.entrySet()) {
                descriptions.addProperty(entry.getKey(), entry.getValue());
            }
            
            root.add("configs", configs);
            root.add("descriptions", descriptions);
            
            Files.writeString(dynamicConfigFile, GSON.toJson(root));
            LOGGER.info("Saved dynamic configs to: " + dynamicConfigFile);
            
        } catch (Exception e) {
            LOGGER.error("Failed to save dynamic configs", e);
        }
    }
    
    public static void loadDynamicConfigs() {
        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path dynamicConfigFile = configDir.resolve("bql/dynamic_configs.json");
            
            if (!Files.exists(dynamicConfigFile)) {
                return;
            }
            
            String content = Files.readString(dynamicConfigFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            
            if (root.has("configs")) {
                JsonObject configs = root.getAsJsonObject("configs");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : configs.entrySet()) {
                    dynamicConfigs.put(entry.getKey(), GSON.fromJson(entry.getValue(), Object.class));
                }
            }
            
            if (root.has("descriptions")) {
                JsonObject descriptions = root.getAsJsonObject("descriptions");
                for (Map.Entry<String, com.google.gson.JsonElement> entry : descriptions.entrySet()) {
                    configDescriptions.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            
            LOGGER.info("Loaded dynamic configs from: " + dynamicConfigFile);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load dynamic configs", e);
        }
    }
    
    // Getters for creation shop data
    public static CreationShopData getCreationShopData() {
        return creationShopData;
    }
    
    /**
     * Manually reload and update all configs (useful for testing)
     */
    public static void forceReloadAll() {
        LOGGER.info("Forcing manual reload of all configs...");
        try {
            loadCreationShopData();
            updateConstants();
            loadDynamicConfigs();
            LOGGER.info("Manual config reload completed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to manually reload configs", e);
        }
    }
    
    public static class CreationShopData {
        public Map<String, Integer> creationPriceTable = new HashMap<>();
        public Map<String, Integer> creationEnchantPriceTable = new HashMap<>();
        public Map<String, Integer> creationItemLearnCostTable = new HashMap<>();
        public Map<String, Integer> creationEnchantLearnCostTable = new HashMap<>();
    }
} 