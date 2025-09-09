// BQL Config System Example
// This file demonstrates how to use the new config system in KubeJS

// ===== Creating Dynamic Configs =====

// Set a simple config value
//Config.setConfig("myAddon.damage_multiplier", 1.5, "Multiplier for all damage dealt");

// Set multiple config values for your addon
//let myAddonConfigs = {
//    "health_regen_rate": 0.1,
//    "max_energy": 1000,
//    "energy_regen_delay": 60,
//    "enable_special_abilities": true
//};

//let myAddonDescriptions = {
//    "health_regen_rate": "Health regeneration rate per tick",
//    "max_energy": "Maximum energy capacity",
//    "energy_regen_delay": "Ticks to wait before energy starts regenerating",
//    "enable_special_abilities": "Whether to enable special abilities"
//};

// Generate a config template for your addon
//Config.generateConfigTemplate("MyAwesomeAddon", myAddonConfigs, myAddonDescriptions);

// ===== Retrieving Config Values =====

// Get a dynamic config value with default fallback
//let damageMultiplier = Config.getConfig("myAddon.damage_multiplier", 1.0);
//let healthRegenRate = Config.getConfig("MyAwesomeAddon.health_regen_rate", 0.05);

//console.log("Damage Multiplier: " + damageMultiplier);
//console.log("Health Regen Rate: " + healthRegenRate);

// Check if a config exists
//if (Config.hasConfig("MyAwesomeAddon.enable_special_abilities")) {
//    let enableSpecial = Config.getConfig("MyAwesomeAddon.enable_special_abilities");
//    console.log("Special abilities enabled: " + enableSpecial);
//}

// ===== Retrieving BQL Constants =====

// Body constants
//let damageStages = Config.getDamageStagePercentages();
//let maxDamage = Config.getMaxDamage();
//console.log("Damage stage percentages: " + damageStages);
//console.log("Max damage: " + maxDamage);

// Stamina constants
//let exhaustionLevels = Config.getExhaustionLevels();
//let staminaGainChance = Config.getStaminaGainChance();
//console.log("Exhaustion levels: " + exhaustionLevels);
//console.log("Stamina gain chance: " + staminaGainChance);

// Creation shop constants
//let bitMap1 = Config.getBitMap1Table();
//let coalBitValue = Config.getBitValue(1, "minecraft:coal");
//let coalPrice = Config.getCreationPrice("minecraft:coal");

//console.log("Coal bit value from map 1: " + coalBitValue);
//console.log("Coal creation price: " + coalPrice);

// ===== Utility Functions =====

// Save all dynamic configs to file
//Config.saveConfigs();

// Get all config keys for debugging
//let allKeys = Config.getConfigKeys();
//console.log("All dynamic config keys: " + allKeys);

// Reload configs from files
//Config.reloadConfigs();

// Get all config values (for debugging)
//let allConfigs = Config.getAllConfigs();
//console.log("All configs: " + JSON.stringify(allConfigs, null, 2));

// ===== Example Usage in Event Handlers =====

// Example: Use config values in damage calculations
//ServerEvents.entityHurt(event => {
//    let damageMultiplier = Config.getConfig("myAddon.damage_multiplier", 1.0);
//    let originalDamage = event.getDamage();
//    event.setDamage(originalDamage * damageMultiplier);
//});

// Example: Use stamina constants for custom calculations
//PlayerEvents.tick(event => {
//    let player = event.player;
//    let staminaGainChance = Config.getStaminaGainChance();
//    let staminaGainReq = Config.getStaminaGainReq();
//
//    // Your custom stamina logic here using the configurable constants
//    // These values will automatically update when the config files change
//});

//console.log("BQL Config System Example loaded successfully!");