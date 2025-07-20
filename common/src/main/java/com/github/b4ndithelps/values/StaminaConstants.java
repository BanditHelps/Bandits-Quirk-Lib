package com.github.b4ndithelps.values;

public class StaminaConstants {
    // ONLY CODED TO HAVE A MAX OF 5 ELEMENTS (4 Levels)
    public static int[] EXHAUSTION_LEVELS = {0, -10, -35, -60, -80};
    public static double[] EXHAUSTION_MULTIPLIERS = {1.0, 1.2, 1.5, 2.0, 3.0};
    public static int[] STAMINA_REGEN_COOLDOWNS = {3, 6, 8, 9, 10};
    public static double[] STAMINA_REGEN_RATE = {1, 0.5, 0.3, 0.2, 0.1};

    // The percent chance that stamina maximum increases when used
    public static double STAMINA_GAIN_CHANCE = 0.05;
    public static double STAMINA_GAIN_EXHAUSTED_CHANCE = 0.1;

    // The percentage stamina required to re-enable powers after losing them due to exhaustion
    public static double STAMINA_ENABLE_PERCENT = 0.02;

    // How much stamina needs to be used in order to have a chance at increasing the max value
    public static int STAMINA_GAIN_REQ = 100;

    // The maximum amount that the stamina can increase after using STAMINA_GAIN_REQ total stamina
    public static int STAMINA_MAX_INCREASE = 3;

    // The amount of "progress" required to redeem an upgrade point
    public static int UPGRADE_POINT_COST = 500;

    // Tags
    public static final String PLUS_ULTRA_TAG = "MineHa.PlusUltra";
    public static final String POWERS_DISABLED_TAG = "MineHa.PowersDisabled";

    // Scoreboards
    public static final String STAMINA_PERCENT_SCOREBOARD = "MineHa.StaminaPercentage";
    public static final String UPGRADE_POINTS_SCOREBOARD = "MineHa.UpgradePoints";

    public static int POINTS_TO_UPGRADE = 500;
    public static int STARTING_STAMINA_MIN = 50;
    public static int STARTING_STAMINA_MAX = 200;
}
