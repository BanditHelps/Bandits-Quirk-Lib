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
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("BQL-Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Dynamic config storage for KubeJS
    private static final Map<String, Object> dynamicConfigs = new ConcurrentHashMap<>();
    private static final Map<String, String> configDescriptions = new ConcurrentHashMap<>();
    
    // Creation shop data cache
    private static CreationShopData creationShopData;
    
    public static void initialize() {
        // Register the main config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BQLConfig.SPEC, "bql-common.toml");
        
        // Load creation shop data
        loadCreationShopData();
        
        // Update constants with config values
        updateConstants();
        
        LOGGER.info("Config system initialized");
    }
    
    public static void updateConstants() {
        // Update Body Constants
        List<? extends Double> damagePercentages = BQLConfig.INSTANCE.damageStagePercentages.get();
        if (damagePercentages.size() >= 4) {
            BodyConstants.DAMAGE_STAGE_PERCENTAGES = new float[]{
                damagePercentages.get(0).floatValue(),
                damagePercentages.get(1).floatValue(),
                damagePercentages.get(2).floatValue(),
                damagePercentages.get(3).floatValue()
            };
        }
        BodyConstants.MAX_DAMAGE = BQLConfig.INSTANCE.maxDamage.get().floatValue();
        
        // Update Stamina Constants
        List<? extends Integer> exhaustionLevels = BQLConfig.INSTANCE.exhaustionLevels.get();
        if (exhaustionLevels.size() == 5) {
            StaminaConstants.EXHAUSTION_LEVELS = exhaustionLevels.stream().mapToInt(Integer::intValue).toArray();
        }
        
        List<? extends Double> exhaustionMultipliers = BQLConfig.INSTANCE.exhaustionMultipliers.get();
        if (exhaustionMultipliers.size() == 5) {
            StaminaConstants.EXHAUSTION_MULTIPLIERS = exhaustionMultipliers.stream().mapToDouble(Double::doubleValue).toArray();
        }
        
        List<? extends Integer> regenCooldowns = BQLConfig.INSTANCE.staminaRegenCooldowns.get();
        if (regenCooldowns.size() == 5) {
            StaminaConstants.STAMINA_REGEN_COOLDOWNS = regenCooldowns.stream().mapToInt(Integer::intValue).toArray();
        }
        
        List<? extends Double> regenRates = BQLConfig.INSTANCE.staminaRegenRate.get();
        if (regenRates.size() == 5) {
            StaminaConstants.STAMINA_REGEN_RATE = regenRates.stream().mapToDouble(Double::doubleValue).toArray();
        }
        
        // Update other stamina constants using reflection to maintain compatibility
        try {
            java.lang.reflect.Field field;
            
            field = StaminaConstants.class.getDeclaredField("STAMINA_GAIN_CHANCE");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.staminaGainChance.get());
            
            field = StaminaConstants.class.getDeclaredField("STAMINA_GAIN_EXHAUSTED_CHANCE");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.staminaGainExhaustedChance.get());
            
            field = StaminaConstants.class.getDeclaredField("STAMINA_ENABLE_PERCENT");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.staminaEnablePercent.get());
            
            field = StaminaConstants.class.getDeclaredField("STAMINA_GAIN_REQ");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.staminaGainReq.get());
            
            field = StaminaConstants.class.getDeclaredField("STAMINA_MAX_INCREASE");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.staminaMaxIncrease.get());
            
            field = StaminaConstants.class.getDeclaredField("UPGRADE_POINT_COST");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.upgradePointCost.get());
            
            field = StaminaConstants.class.getDeclaredField("PLUS_ULTRA_TAG");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.plusUltraTag.get());
            
            field = StaminaConstants.class.getDeclaredField("POWERS_DISABLED_TAG");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.powersDisabledTag.get());
            
            field = StaminaConstants.class.getDeclaredField("STAMINA_PERCENT_SCOREBOARD");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.staminaPercentScoreboard.get());
            
            field = StaminaConstants.class.getDeclaredField("UPGRADE_POINTS_SCOREBOARD");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.upgradePointsScoreboard.get());
            
            field = StaminaConstants.class.getDeclaredField("POINTS_TO_UPGRADE");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.pointsToUpgrade.get());
            
            field = StaminaConstants.class.getDeclaredField("STARTING_STAMINA_MIN");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.startingStaminaMin.get());
            
            field = StaminaConstants.class.getDeclaredField("STARTING_STAMINA_MAX");
            field.setAccessible(true);
            field.set(null, BQLConfig.INSTANCE.startingStaminaMax.get());
            
        } catch (Exception e) {
            LOGGER.error("Failed to update stamina constants via reflection", e);
        }
        
        LOGGER.info("Constants updated from config");
    }
    
    private static void loadCreationShopData() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path creationShopFile = configDir.resolve(BQLConfig.INSTANCE.creationShopDataPath.get());
        
        try {
            if (!Files.exists(creationShopFile)) {
                // Create default creation shop data file
                createDefaultCreationShopData(creationShopFile);
            }
            
            String content = Files.readString(creationShopFile);
            creationShopData = GSON.fromJson(content, CreationShopData.class);
            
            // Update the constants class maps if needed
            updateCreationShopConstants();
            
            LOGGER.info("Loaded creation shop data from: " + creationShopFile);
            
        } catch (Exception e) {
            LOGGER.error("Failed to load creation shop data", e);
            creationShopData = createDefaultCreationShopDataObject();
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
        data.bitMap1Table = new HashMap<>(CreationShopConstants.BIT_MAP_1_TABLE);
        data.bitMap2Table = new HashMap<>(CreationShopConstants.BIT_MAP_2_TABLE);
        data.bitMap3Table = new HashMap<>(CreationShopConstants.BIT_MAP_3_TABLE);
        data.bitMap4Table = new HashMap<>(CreationShopConstants.BIT_MAP_4_TABLE);
        data.bitMap5Table = new HashMap<>(CreationShopConstants.BIT_MAP_5_TABLE);
        data.creationPriceTable = new HashMap<>(CreationShopConstants.CREATION_PRICE_TABLE);
        data.creationEnchantPriceTable = new HashMap<>(CreationShopConstants.CREATION_ENCHANT_PRICE_TABLE);
        return data;
    }
    
    private static void updateCreationShopConstants() {
        if (creationShopData == null) return;
        
        try {
            java.lang.reflect.Field field;
            
            field = CreationShopConstants.class.getDeclaredField("BIT_MAP_1_TABLE");
            field.setAccessible(true);
            field.set(null, creationShopData.bitMap1Table);
            
            field = CreationShopConstants.class.getDeclaredField("BIT_MAP_2_TABLE");
            field.setAccessible(true);
            field.set(null, creationShopData.bitMap2Table);
            
            field = CreationShopConstants.class.getDeclaredField("BIT_MAP_3_TABLE");
            field.setAccessible(true);
            field.set(null, creationShopData.bitMap3Table);
            
            field = CreationShopConstants.class.getDeclaredField("BIT_MAP_4_TABLE");
            field.setAccessible(true);
            field.set(null, creationShopData.bitMap4Table);
            
            field = CreationShopConstants.class.getDeclaredField("BIT_MAP_5_TABLE");
            field.setAccessible(true);
            field.set(null, creationShopData.bitMap5Table);
            
            field = CreationShopConstants.class.getDeclaredField("CREATION_PRICE_TABLE");
            field.setAccessible(true);
            field.set(null, creationShopData.creationPriceTable);
            
            field = CreationShopConstants.class.getDeclaredField("CREATION_ENCHANT_PRICE_TABLE");
            field.setAccessible(true);
            field.set(null, creationShopData.creationEnchantPriceTable);
            
        } catch (Exception e) {
            LOGGER.error("Failed to update creation shop constants", e);
        }
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
    
    public static class CreationShopData {
        public Map<String, Integer> bitMap1Table = new HashMap<>();
        public Map<String, Integer> bitMap2Table = new HashMap<>();
        public Map<String, Integer> bitMap3Table = new HashMap<>();
        public Map<String, Integer> bitMap4Table = new HashMap<>();
        public Map<String, Integer> bitMap5Table = new HashMap<>();
        public Map<String, Integer> creationPriceTable = new HashMap<>();
        public Map<String, Integer> creationEnchantPriceTable = new HashMap<>();
    }
} 