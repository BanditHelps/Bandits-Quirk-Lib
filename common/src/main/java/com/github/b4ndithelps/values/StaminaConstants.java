package com.github.b4ndithelps.values;

public class StaminaConstants {
    // ONLY CODED TO HAVE A MAX OF 5 ELEMENTS (4 Levels)
    public static final int[] EXHAUSTION_LEVELS = {0, -10, -35, -60, -80};
    public static final double[] EXHAUSTION_MULTIPLIERS = {1.0, 1.2, 1.5, 2.0, 3.0};
    public static final int[] STAMINA_REGEN_COOLDOWNS = {3, 6, 8, 9, 10};

    // The percent chance that stamina maximum increases when used
    public static final double STAMINA_GAIN_CHANCE = 0.05;
    public static final double STAMINA_GAIN_EXHAUSTED_CHANCE = 0.1;

    // How much stamina needs to be used in order to have a chance at increasing the max value
    public static final int STAMINA_GAIN_REQ = 100;

    // The maximum amount that the stamina can increase after using STAMINA_GAIN_REQ total stamina
    public static final int STAMINA_MAX_INCREASE = 3;

    // The amount of "progress" required to redeem an upgrade point
    public static final int UPGRADE_POINT_COST = 500;

    // Tags
    public static final String PLUS_ULTRA_TAG = "MineHa.PlusUltra";
    public static final String POWERS_DISABLED_TAG = "MineHa.PowersDisabled";

    public static final int POINTS_TO_UPGRADE = 500;
}
