package com.github.b4ndithelps.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * This is the file that builds the BQL specific config located in the root of the /config folder.
 */
public class BQLConfig {

    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final BQLConfig INSTANCE;
    public static final ForgeConfigSpec SPEC;

    static {
        final Pair<BQLConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(BQLConfig::new);
        INSTANCE = specPair.getLeft();
        SPEC = specPair.getRight();
    }

    // Body Constants
    public final ForgeConfigSpec.ConfigValue<List<? extends Double>> damageStagePercentages;
    public final ForgeConfigSpec.DoubleValue maxDamage;
    public final ForgeConfigSpec.DoubleValue staminaSleepRecoverPercent;

    // Stamina Constants
    public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> exhaustionLevels;
    public final ForgeConfigSpec.ConfigValue<List<? extends Double>> exhaustionMultipliers;
    public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> staminaRegenCooldowns;
    public final ForgeConfigSpec.ConfigValue<List<? extends Double>> staminaRegenRate;
    public final ForgeConfigSpec.DoubleValue staminaGainChance;
    public final ForgeConfigSpec.DoubleValue staminaGainExhaustedChance;
    public final ForgeConfigSpec.DoubleValue staminaEnablePercent;
    public final ForgeConfigSpec.IntValue staminaGainReq;
    public final ForgeConfigSpec.IntValue staminaMaxIncrease;
    public final ForgeConfigSpec.IntValue upgradePointCost;
    public final ForgeConfigSpec.IntValue pointsToUpgrade;
    public final ForgeConfigSpec.IntValue startingStaminaMin;
    public final ForgeConfigSpec.IntValue startingStaminaMax;

    // Creation Constants
    public final ForgeConfigSpec.DoubleValue creationStaminaCost;

    // Creation Shop Constants - We'll handle the maps dynamically
    public final ForgeConfigSpec.ConfigValue<String> creationShopDataPath;

    // Powerstock Constants
    public final ForgeConfigSpec.DoubleValue minorDamagePercentage;
    public final ForgeConfigSpec.DoubleValue majorDamagePercentage;
    public final ForgeConfigSpec.DoubleValue severeDamagePercentage;
    public final ForgeConfigSpec.DoubleValue strengthDivisor;
    public final ForgeConfigSpec.DoubleValue armorDivisor;
    public final ForgeConfigSpec.DoubleValue healthDivisor;
    public final ForgeConfigSpec.DoubleValue speedDivisor;
    public final ForgeConfigSpec.DoubleValue swimDivisor;
    public final ForgeConfigSpec.DoubleValue minorDamage;
    public final ForgeConfigSpec.DoubleValue majorDamage;
    public final ForgeConfigSpec.DoubleValue severeDamage;

    // Genetics / Extractor Constants
    public final ForgeConfigSpec.IntValue extractorMaxDurability;
    public final ForgeConfigSpec.IntValue extractorDurabilityCost;
    public final ForgeConfigSpec.BooleanValue extractorDamageTarget;
    public final ForgeConfigSpec.DoubleValue extractorDamageAmount;
    public final ForgeConfigSpec.ConfigValue<List<? extends String>> extractorValidEntityTypes;
    public final ForgeConfigSpec.IntValue seqLenPlayer;
    public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> seqLenVillagerRange;
    public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> seqLenZombieRange;
    public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> seqLenHuskRange;
    public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> seqLenDrownedRange;

    // Player Genome
    public final ForgeConfigSpec.IntValue playerMaxGenes;


    public BQLConfig(ForgeConfigSpec.Builder builder) {
        // Body Constants Section
        builder.comment("Body System Configuration")
                .push("body");

        this.damageStagePercentages = builder
                .comment("Damage stage percentages for body parts (Healthy, Sprained, Broken, Destroyed)")
                .defineList("damage_stage_percentages", 
                    Arrays.asList(0.0, 0.4, 0.8, 1.0),
                    obj -> obj instanceof Double && (Double) obj >= 0.0 && (Double) obj <= 1.0);

        this.maxDamage = builder
                .comment("Maximum damage a body part can take")
                .defineInRange("max_damage", 100.0, 1.0, 1000.0);

        this.staminaSleepRecoverPercent = builder
                .comment("Amount of stamina regained when successfully sleeping")
                        .defineInRange("stamina_sleep_recover_percent", 0.75, 0.0, 1.0);

        builder.pop();

        // Stamina Constants Section
        builder.comment("Stamina System Configuration")
                .push("stamina");

        this.exhaustionLevels = builder
                .comment("Exhaustion level stamina range (max 5 elements)")
                .defineList("exhaustion_levels",
                    Arrays.asList(0, -10, -35, -60, -80),
                    obj -> obj instanceof Integer);

        this.exhaustionMultipliers = builder
                .comment("Percentage learn boost for each exhaustion level")
                .defineList("exhaustion_multipliers",
                    Arrays.asList(1.0, 1.2, 1.5, 2.0, 3.0),
                    obj -> obj instanceof Double && (Double) obj > 0.0);

        this.staminaRegenCooldowns = builder
                .comment("Default stamina regeneration cooldowns per exhaustion level")
                .defineList("stamina_regen_cooldowns",
                    Arrays.asList(3, 6, 8, 9, 10),
                    obj -> obj instanceof Integer && (Integer) obj > 0);

        this.staminaRegenRate = builder
                .comment("Stamina regeneration rates. (% chance stamina point is recovered on tick)")
                .defineList("stamina_regen_rate",
                    Arrays.asList(1.0, 0.5, 0.3, 0.2, 0.1),
                    obj -> obj instanceof Double && (Double) obj > 0.0);

        this.staminaGainChance = builder
                .comment("Chance for stamina maximum to increase when used")
                .defineInRange("stamina_gain_chance", 0.3, 0.0, 1.0);

        this.staminaGainExhaustedChance = builder
                .comment("Chance for stamina maximum to increase when exhausted")
                .defineInRange("stamina_gain_exhausted_chance", 0.45, 0.0, 1.0);

        this.staminaEnablePercent = builder
                .comment("Percentage stamina required to re-enable powers after exhaustion")
                .defineInRange("stamina_enable_percent", 0.02, 0.0, 1.0);

        this.staminaGainReq = builder
                .comment("Stamina usage required for chance at max increase")
                .defineInRange("stamina_gain_req", 100, 1, 10000);

        this.staminaMaxIncrease = builder
                .comment("Maximum stamina increase amount")
                .defineInRange("stamina_max_increase", 3, 1, 100);

        this.upgradePointCost = builder
                .comment("Progress required to redeem an upgrade point")
                .defineInRange("upgrade_point_cost", 500, 1, 10000);

        this.pointsToUpgrade = builder
                .comment("Stamina usage needed to get an upgrade point")
                .defineInRange("points_to_upgrade", 500, 1, 10000);

        this.startingStaminaMin = builder
                .comment("Minimum starting stamina")
                .defineInRange("starting_stamina_min", 50, 1, 1000);

        this.startingStaminaMax = builder
                .comment("Maximum starting stamina")
                .defineInRange("starting_stamina_max", 200, 1, 1000);

        builder.pop();

        // Creation Shop Section
        builder.comment("Creation Shop Configuration")
                .push("creation_shop");

        this.creationStaminaCost = builder
                .comment("Stamina use multiplier. (cost = creationStaminaCost * lipidsUsed)")
                .defineInRange("creation_stamina_cost", 0.8, 0.0, 10.0);

        this.creationShopDataPath = builder
                .comment("Path to creation shop data file (relative to config directory)")
                .define("creation_shop_data_path", "bql/creation_shop_data.json");

        builder.pop();

        // Powerstock section
        builder.comment("Powerstock Value Configuration")
                .push("powerstock");

        this.minorDamagePercentage = builder
                .comment("The percentage over the max power threshold considered to be 'Minor' (1.01 = 1% over max safety limit)")
                .defineInRange("minor_damage_percentage", 1.01, 0.0, 5.0);

        this.majorDamagePercentage = builder
                .comment("The percentage over the max power threshold considered to be 'Major' (2.0 = 100% over max safety limit)")
                .defineInRange("major_damage_percentage", 2.0, 0.0, 5.0);

        this.severeDamagePercentage = builder
                .comment("The percentage over the max power threshold considered to be 'Severe' (5.0 = 400% over max safety limit)")
                .defineInRange("severe_damage_percentage", 5.0, 0.0, 5.0);

        this.strengthDivisor = builder
                .comment("The amount of power it takes to increase FC strength by +1 damage.")
                .defineInRange("powerstock_strength_divisor", 10833, 1.0, 500000);

        this.armorDivisor = builder
                .comment("The amount of power it takes to increase FC armor by +0.5 points.")
                .defineInRange("powerstock_armor_divisor", 5000, 1.0, 500000);

        this.healthDivisor = builder
                .comment("The amount of power it takes to increase FC health by +0.5 points.")
                .defineInRange("powerstock_health_divisor", 10000, 1.0, 500000);

        this.speedDivisor = builder
                .comment("The amount of power it takes to increase FC speed. (Large values required as speed scales strangely)")
                .defineInRange("powerstock_speed_divisor", 3000, 1.0, 100000);

        this.swimDivisor = builder
                .comment("The amount of power it takes to increase FC swim speed. (Large values required as speed scales strangely)")
                .defineInRange("powerstock_swim_divisor", 3000, 1.0, 100000);

        this.minorDamage = builder
                .comment("The amount of damage overusing in the 'minor' level does to a limb")
                .defineInRange("powerstock_minor_damage", 25, 0.0, 1000);

        this.majorDamage = builder
                .comment("The amount of damage overusing in the 'major' level does to a limb")
                .defineInRange("powerstock_major_damage", 50, 0.0, 1000);

        this.severeDamage = builder
                .comment("The amount of damage overusing in the 'severe' level does to a limb")
                .defineInRange("powerstock_severe_damage", 75, 0.0, 1000);


        builder.pop();

        // Genetics / Extractor section
        builder.comment("Genetics & Tissue Extractor Configuration")
                .push("genetics");

        this.extractorMaxDurability = builder
                .comment("Maximum durability of the Tissue Extractor item")
                .defineInRange("extractor_max_durability", 128, 1, 4096);

        this.extractorDurabilityCost = builder
                .comment("Durability cost per successful use of the Tissue Extractor")
                .defineInRange("extractor_durability_cost", 1, 1, 64);

        this.extractorDamageTarget = builder
                .comment("Whether using the Tissue Extractor damages the target entity")
                .define("extractor_damage_target", false);

        this.extractorDamageAmount = builder
                .comment("Damage dealt to the target when extractor is used (in health points)")
                .defineInRange("extractor_damage_amount", 1.0, 0.0, 20.0);

        this.extractorValidEntityTypes = builder
                .comment("List of entity type IDs that can be swabbed (e.g., minecraft:player)")
                .defineList(
                        "extractor_valid_entity_types",
                        Arrays.asList(
                                "minecraft:player",
                                "minecraft:villager",
                                "minecraft:zombie",
                                "minecraft:husk",
                                "minecraft:drowned"
                        ),
                        obj -> obj instanceof String
                );

        this.seqLenPlayer = builder
                .comment("Exact number of genes for players")
                .defineInRange("sequence_len_player", 4, 1, 16);

        this.seqLenVillagerRange = builder
                .comment("Villager gene count range [min,max]")
                .defineList("sequence_len_villager", Arrays.asList(3,4), o -> o instanceof Integer);

        this.seqLenZombieRange = builder
                .comment("Zombie gene count range [min,max]")
                .defineList("sequence_len_zombie", Arrays.asList(2,3), o -> o instanceof Integer);

        this.seqLenHuskRange = builder
                .comment("Husk gene count range [min,max]")
                .defineList("sequence_len_husk", Arrays.asList(2,3), o -> o instanceof Integer);

        this.seqLenDrownedRange = builder
                .comment("Drowned gene count range [min,max]")
                .defineList("sequence_len_drowned", Arrays.asList(2,3), o -> o instanceof Integer);

        this.playerMaxGenes = builder
                .comment("Maximum number of genes a player can have at once")
                .defineInRange("player_max_genes", 12, 1, 64);

        builder.pop();
    }
} 