package com.github.b4ndithelps.forge.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

    // Creation Shop Constants - We'll handle the maps dynamically
    public final ForgeConfigSpec.ConfigValue<String> creationShopDataPath;

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

        this.creationShopDataPath = builder
                .comment("Path to creation shop data file (relative to config directory)")
                .define("creation_shop_data_path", "bql/creation_shop_data.json");

        builder.pop();
    }
} 