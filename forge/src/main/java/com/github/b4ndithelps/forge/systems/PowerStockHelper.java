package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.capabilities.Body.BodyPart;
import com.github.b4ndithelps.forge.capabilities.Body.IBodyStatusCapability;
import com.github.b4ndithelps.forge.effects.ModEffects;
import com.github.b4ndithelps.forge.utils.ActionBarHelper;
import com.github.b4ndithelps.values.BodyConstants;
import com.github.b4ndithelps.values.QuirkConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import static com.github.b4ndithelps.values.QuirkConstants.*;

/**
 * PowerStockHelper is used in order to help calculate and manage values related to PowerStock.
 * This includes scaling, power thresholds, etc.
 */
public class PowerStockHelper {

    public static final String MAXIMUM_SAFE_POWER_KEY = "pstock_max_safe";
    public static final String STORED_POWER_KEY = "pstock_stored";
    public static final String FC_PERCENTAGE_KEY = "pstock_fc_percent";

    // Class that stores information about the safe power use.
    public static class SafetyInfo {
        private int damageLevel;
        private boolean isSafe;

        public SafetyInfo(boolean isSafe, int damageLevel) {
            this.isSafe = isSafe;
            this.damageLevel = damageLevel;
        }

        public int getDamageLevel() {
            return damageLevel;
        }

        public boolean isSafe() {
            return isSafe;
        }
    }

    // Queries the player's BodyStatus to see what the max stored powerstock energy they can have is
    // This is a total amount of energy they can use, based on the current stamina of the player
    public static float getSafePowerThreshold(ServerPlayer player) {
        IBodyStatusCapability bodyStatus = BodyStatusHelper.getBodyStatus(player);
        return bodyStatus == null ? 0.0f : bodyStatus.getCustomFloat(BodyPart.CHEST, MAXIMUM_SAFE_POWER_KEY);
    }

    // Returns the player's current stored powerstock energy
    public static float getStoredPower(ServerPlayer player) {
        IBodyStatusCapability bodyStatus = BodyStatusHelper.getBodyStatus(player);
        return bodyStatus == null ? 0.0f : bodyStatus.getCustomFloat(BodyPart.CHEST, STORED_POWER_KEY);
    }

    // Returns the player's selected Full Cowling Level
    // Converts to a percentage < 1 as the minecraft function sets it to an integer from 0-100
    public static float getFCPercentage(ServerPlayer player) {
        IBodyStatusCapability bodyStatus = BodyStatusHelper.getBodyStatus(player);
        return bodyStatus == null ? 0.0f :bodyStatus.getCustomFloat(BodyPart.CHEST, FC_PERCENTAGE_KEY) / 100;
    }

    public static SafetyInfo getOverUseDamageLevel(ServerPlayer player, float powerUsed) {
        float maxSafePower = getSafePowerThreshold(player);

        int damageLevel = 0; // 0 = No Damage
                             // 1 = Minor Damage
                             // 2 = Major Damage
                             // 3 = Severe Damage

        // If we are not using over the max allowed, just return.
        if (powerUsed <= maxSafePower) {
            return new SafetyInfo(true, damageLevel);
        }

        float percentOverPowered = powerUsed / maxSafePower;

        if (percentOverPowered >= QuirkConstants.PSTOCK_SEVERE_DAMAGE_PERCENTAGE) {
            damageLevel = 3;
        } else if (percentOverPowered >= QuirkConstants.PSTOCK_MAJOR_DAMAGE_PERCENTAGE) {
            damageLevel = 2;
        } else if (percentOverPowered >= QuirkConstants.PSTOCK_MINOR_DAMAGE_PERCENTAGE) {
            damageLevel = 1;
        }

        return new SafetyInfo(false, damageLevel);
    }

    // This method checks to see if the player is using too much power with full cowling.
    // If they are, we give them the special overuse effect to strain their body.
    public static void applyFullCowlDamage(ServerPlayer player) {
        float currentPowerUsage = getStoredPower(player) * getFCPercentage(player);

        SafetyInfo safetyInfo = getOverUseDamageLevel(player, currentPowerUsage);

        if (!safetyInfo.isSafe()) {
            player.addEffect(new MobEffectInstance(ModEffects.PSTOCK_OVERUSE.get(), 999999, safetyInfo.getDamageLevel(), false, false));
        } else {
            player.removeEffect(ModEffects.PSTOCK_OVERUSE.get());
        }
    }

    /**
     * This function facilitates the power scaling that powerstock has on the player's attributes. There are different
     * equations, as each type needs to scale a bit differently. May need to adjust this later.
     * @param player
     * @param baseAmount - the minimum that the scaled value can be
     * @param scalingType - Can be strength, armor, health, speed, swim, logarithmic, and leaving it default use a sqrt function
     * @param max - The max that the scaled value can become
     * @param percentUsed - The percentage of power currently being used.
     * @return - A value bounded between 0 and max. Used for an attribute modifier
     */
    public static float calculateScaledAttribute(ServerPlayer player, float baseAmount, String scalingType, float max, float percentUsed) {
        float totalPower = getStoredPower(player);

        double scaledValue = baseAmount;

        switch(scalingType) {
            case "strength":
                // Linear growth: adding +1 strength every "PSTOCK_STRENGTH_ADDITION" energy
                scaledValue += ((totalPower * percentUsed) / PSTOCK_STRENGTH_DIVISOR);
                break;
            case "armor":
                // Linear growth: adding 0.5 armor per "PSTOCK_ARMOR_ADDITION" energy.
                scaledValue += ((totalPower * percentUsed) / PSTOCK_ARMOR_DIVISOR);
                break;
            case "health":
                // Linear growth: adding 0.5 armor per "PSTOCK_HEALTH_ADDITION" energy.
                scaledValue += (float) ((totalPower * percentUsed) / PSTOCK_HEALTH_DIVISOR);
                break;
            case "speed":
                // Not 100% sure what this one does anymore. I do know speed scaling is way different from health
                scaledValue += (float) ((Math.sqrt(totalPower) * percentUsed) / PSTOCK_SPEED_DIVISOR);
                break;
            case "swim":
                // Not 100% sure what this one does either.
                scaledValue += (float) ((Math.sqrt(totalPower) * percentUsed) / PSTOCK_SWIM_DIVISOR);
                break;
            case "logarithmic":
                // Generic logarithmic
                scaledValue += (float) ((Math.log10(totalPower) * percentUsed) / 100);
                break;
            default:
                // Default: Don't expect this to be used.
                scaledValue += (float) ((Math.sqrt(totalPower) * percentUsed) / 100);
                break;
        }

        return (float) Math.min(max, Math.max(0, scaledValue));
    }

    // This is used to send a consistent "Charging: X% / 100%" message to the player that is color coded to the damage level
    public static void sendPlayerPercentageMessage(ServerPlayer player, float powerUsed, float chargePercent, String label) {
        SafetyInfo safetyInfo = getOverUseDamageLevel(player, powerUsed);
        String statusText = safetyInfo.isSafe() ? "Safe": "Overuse";

        ChatFormatting color = switch (safetyInfo.getDamageLevel()) {
            case 1 -> ChatFormatting.GOLD;
            case 2 -> ChatFormatting.RED;
            case 3 -> ChatFormatting.DARK_RED;
            default -> ChatFormatting.GREEN;
        };

        ActionBarHelper.sendPercentageDisplay(player, label, chargePercent, color, color, statusText);
    }

    // This function will damage a certain body part based on how much overcharge the ability is using.
    public static void applyLimbDamage(ServerPlayer player, float powerUsed, String bodyPart) {
        SafetyInfo safetyInfo = getOverUseDamageLevel(player, powerUsed);
        if (safetyInfo.isSafe()) return;

        double damage = switch (safetyInfo.getDamageLevel()) {
            case 1 -> PSTOCK_MINOR_DAMAGE;
            case 2 -> PSTOCK_MAJOR_DAMAGE;
            case 3 -> PSTOCK_SEVERE_DAMAGE;
            default -> 0;
        };

        // If full cowling is activated, check to see
        if (player.getTags().contains("MineHa.PowerStock.FullCowl")) {
            float fcPercentage = getFCPercentage(player);
            // Check if the full cowling percent is "safe". If so, give a decrease to the damage taken equal to
            // 1 damage per percent used
            if (getOverUseDamageLevel(player, (fcPercentage * getStoredPower(player))).isSafe()) {
                damage = Math.max(damage - (fcPercentage * 100), 0);
            }
        }

        // If the player is using full cowling,

        // Do some extra checks to make sure that damage does reach the final stage.
        float currentDamage = BodyStatusHelper.getDamage(player, bodyPart);
        float currentDamagePercent = currentDamage / BodyConstants.MAX_DAMAGE;

        // If we are going to break the arm, use logic to split it into 3 stages
        if (currentDamage + damage >= BodyConstants.MAX_DAMAGE) {
            // If damage% < 90%, set damage to 92%
            // If damage% > 90% && <= 92%, set damage to 97%
            // If damage% > 92%, set damage to 100%

            if (currentDamagePercent < 0.9) {
                // Set the damage to 92%
                BodyStatusHelper.setDamage(player, bodyPart, (BodyConstants.MAX_DAMAGE * 0.92f));
            } else if (currentDamagePercent <= 0.92){
                // Set damage to 97%
                BodyStatusHelper.setDamage(player, bodyPart, (BodyConstants.MAX_DAMAGE * 0.97f));
            } else {
                // Set damage to 100%
                BodyStatusHelper.setDamage(player, bodyPart, BodyConstants.MAX_DAMAGE);
            }
        } else {
            BodyStatusHelper.addDamage(player, bodyPart, (float) damage);
        }
    }


}
