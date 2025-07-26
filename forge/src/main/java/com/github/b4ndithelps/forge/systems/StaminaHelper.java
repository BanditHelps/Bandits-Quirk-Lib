package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.capabilities.IStaminaData;
import com.github.b4ndithelps.forge.capabilities.StaminaDataProvider;
import com.github.b4ndithelps.forge.damage.ModDamageTypes;
import com.github.b4ndithelps.forge.vfx.ParticleEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.common.util.LazyOptional;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.b4ndithelps.values.StaminaConstants.*;

public class StaminaHelper {
    
    /**
     * Safely checks if the stamina capability is available for the given player.
     * 
     * @param player The player to check
     * @return true if the capability is available, false otherwise
     */
    public static boolean isStaminaDataAvailable(Player player) {
        return player.getCapability(StaminaDataProvider.STAMINA_DATA, null).isPresent();
    }
    
    /**
     * Gets the stamina data capability for the given player.
     * Returns null if the capability is not available (e.g., during player death/respawn).
     * 
     * @param player The player to get the capability for
     * @return The IStaminaData instance, or null if not available
     */
    public static IStaminaData getStaminaDataSafe(Player player) {
        return player.getCapability(StaminaDataProvider.STAMINA_DATA, null).orElse(null);
    }
    
    public static LazyOptional<IStaminaData> getStaminaData(Player player) {
        return player.getCapability(StaminaDataProvider.STAMINA_DATA, null);
    }

    public static boolean hasStamina(Player player, int required) {
        return getStaminaData(player).map(staminaData ->
                staminaData.getCurrentStamina() >= required).orElse(false);
    }

    /**
     * This function is the main one for determining stamina percentage, regeneration, and exhaustion effects.
     * @param player
     */
    public static void handleStaminaTick(Player player) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData == null) {
            return; // Silently return if capability not available
        }

        int curStamina = staminaData.getCurrentStamina();
        int maxStamina = staminaData.getMaxStamina();
        int exhaustLevel = staminaData.getExhaustionLevel();

        // First things first. Decide whether to regenerate stamina
        if (staminaData.getRegenCooldown() > 0) {
           staminaData.setRegenCooldown(staminaData.getRegenCooldown() - 1);

        } else if (curStamina <= maxStamina) {
            // Regeneration rate is dependent on exhaustion level
            double regenRate = STAMINA_REGEN_RATE[staminaData.getExhaustionLevel()];

            // If not exhausted, just linearly increase. Else use a bit of randomness to slow it down
            if (regenRate >= 1 || Math.random() < regenRate) {
                int newCurrent = curStamina + 1;
                if (newCurrent > maxStamina) {
                    newCurrent = maxStamina;
                }

                int newExhaustLevel = calculateExhaustionLevel(newCurrent);

                // Send a message if the exhaustion has updated
                if (newExhaustLevel != exhaustLevel) {
                    sendExhaustionMessage(player, newExhaustLevel, exhaustLevel);
                }

                staminaData.setCurrentStamina(newCurrent);
                updateStaminaPercentage(player, newCurrent, maxStamina);
                staminaData.setExhaustionLevel(newExhaustLevel);
            }

        }

        // It could have updated above, so re-get it
        exhaustLevel = staminaData.getExhaustionLevel();

        // Time to apply the exhaustion effects
        if (exhaustLevel > 0) {
            applyExhaustionEffects(player, exhaustLevel);
        }

        // Re-enable powers based on PlusUltra status
        if (staminaData.isPowersDisabled()) {
            if (player.getTags().contains(PLUS_ULTRA_TAG)) {
                // PlusUltra users: re-enable powers when not at death level with last hurrah used
                if (!(exhaustLevel == 4 && getLastHurrahUsed(player))) {
                    enablePowers(player);
                }
            } else {
                // Normal users: re-enable powers when stamina is above 2% of max and not exhausted
                if (curStamina > (maxStamina * STAMINA_ENABLE_PERCENT) && exhaustLevel == 0) {
                    enablePowers(player);
                }
            }
        }
    }

    /**
     * This function is the one called whenever someone uses any of their stamina.
     * Handles the exhaustion levels as well as the level accumulation
     * @param player
     * @param amount
     */
    public static void useStamina(Player player, int amount) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (amount == 0) {
            return; // using no stamina
        }

        // Check if the player is at death level, and if the last hurrah has been used
        if (staminaData.getExhaustionLevel() == 4 && staminaData.getLastHurrahUsed()) {
            player.sendSystemMessage(Component.literal("§4§lYour body has reached its absolute limit. You collapse from exhaustion."));
            player.kill();
        }

        // Apply damage to the player if using stamina while exhausted
        if (staminaData.getExhaustionLevel() > 1) {
            applyExhaustionDamage(player, staminaData.getExhaustionLevel());
        }

        int newCurrent = staminaData.getCurrentStamina() - amount;
        int newUsageTotal = staminaData.getUsageTotal() + amount;
        int newExhaustionLevel = calculateExhaustionLevel(newCurrent);
        int oldExhaustionLevel = staminaData.getExhaustionLevel(); // needed for sendExhaustionMessage()

        awardUpgradeProgress(player, amount, staminaData.getExhaustionLevel());

        // Handle the highest level of exhaustion. If it is maxed out, it means death for the player
        if (newExhaustionLevel == 4 && player.getTags().contains(PLUS_ULTRA_TAG)) {
            if (!staminaData.getLastHurrahUsed()) {
                // First time reaching death leve, so give them one last chance
                player.sendSystemMessage(Component.literal("§b§lPLUS ULTRA! §4This is your final stand! Quirk Factor multiplied by 2! §4§lDANGER! Next use is fatal!"));

                staminaData.setCurrentStamina(newCurrent);
                staminaData.setUsageTotal(newUsageTotal);
                staminaData.setRegenCooldown(STAMINA_REGEN_COOLDOWNS[newExhaustionLevel]);
                staminaData.setExhaustionLevel(newExhaustionLevel);
                staminaData.setLastHurrahUsed(true);

                ParticleEffects.plusUltraVfx(player);
            } else {
                player.sendSystemMessage(Component.literal("§4§lYour body gives out completely. The overexertion was too much."));
                player.kill();
            }
        } else {
            // If it is normal exhaustion though, just update the values
            staminaData.setCurrentStamina(newCurrent);
            staminaData.setUsageTotal(newUsageTotal);
            staminaData.setRegenCooldown(STAMINA_REGEN_COOLDOWNS[newExhaustionLevel]);
            staminaData.setExhaustionLevel(newExhaustionLevel);
        }

        handleStaminaMaxIncrease(player, newExhaustionLevel, staminaData.getMaxStamina(), newUsageTotal);

        // Check if we need to disable powers due to having no stamina
        if (newCurrent <= 0 && !player.getTags().contains(PLUS_ULTRA_TAG)) {
            disablePowers(player, newExhaustionLevel);
        }

        updateStaminaPercentage(player, newCurrent, staminaData.getMaxStamina());
        sendExhaustionMessage(player, newExhaustionLevel, oldExhaustionLevel);
    }

    // For every STAMINA_GAIN_REQUIREMENT points of stamina used, have a small chance to increase the maximum
    // stamina by some random amount. Will be configurable.
    private static void handleStaminaMaxIncrease(Player player, int exhaustLevel, int maxStamina, int newUsageTotal) {
        double chanceToIncrease = exhaustLevel > 0 ? STAMINA_GAIN_EXHAUSTED_CHANCE : STAMINA_GAIN_CHANCE;

        if (newUsageTotal >= STAMINA_GAIN_REQ && Math.random() < chanceToIncrease) {
            int maxIncrease = (int) Math.floor(Math.random() * STAMINA_MAX_INCREASE) + 1;
            player.sendSystemMessage(Component.literal("§6Your training pays off! Maximum stamina increased by " + maxIncrease + "!"));

            int newMax = getMaxStamina(player) + maxIncrease;

            IStaminaData staminaData = getStaminaDataSafe(player);
            staminaData.setMaxStamina(newMax);
            staminaData.setUsageTotal(0);
        }
    }

    // Function that harms the player depending on the exhaustion they are at. Only called by useStamina
    // Damage occurs between level 2 and 3
    private static void applyExhaustionDamage(Player player, int level) {

        switch(level) {
            case 2: // Moderate overexertion
                ModDamageTypes.applyExhaustionDamage(player, 2.0f);
                player.sendSystemMessage(Component.literal("§6Pushing through exhaustion harms your body!"));
                break;
            case 3: // Severe overexertion
                ModDamageTypes.applyExhaustionDamage(player, 4.0f);
                player.sendSystemMessage(Component.literal("§cForcing your exhausted body causes significant damage!"));
                break;
        }
    }

    // Function that gives the player a variety of negative effects, dependent on the level of exhaustion
    private static void applyExhaustionEffects(Player player, int exhaustLevel) {
        switch(exhaustLevel) {
            case 1:
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, true));
                break;

            case 2:
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 1, false, false, true));
                break;

            case 3:
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 2, false, false, true));
                break;

            case 4:
                if (player.getTags().contains(PLUS_ULTRA_TAG)) {
                    // Plus ultra users don't get (most) the effects at the final stage.
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.removeEffect(MobEffects.WEAKNESS);
                    player.removeEffect(MobEffects.DIG_SLOWDOWN);
                } else {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 2, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 2, false, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 4, false, false, true));
                }

                // Visual warning
                ParticleEffects.finalExhaustionLevel(player);
                break;
        }
    }

    // Since EXHAUSTION_LEVELS is expected to be of length 5, range of return value is 0-4
    private static int calculateExhaustionLevel(int stamina) {
        for (int i = 0; i < EXHAUSTION_LEVELS.length - 1; i++) {
            if (stamina >= EXHAUSTION_LEVELS[i]) {
                return i;
            }
        }

        // If it is not any of the above, assume the max (which should only be 4 levels)
        return EXHAUSTION_LEVELS.length - 1;
    }

    // Based on the amount of stamina used and the level of exhaustion, award them progress towards an upgrade point
    private static void awardUpgradeProgress(Player player, int staminaUsed, int exhaustLevel) {
        double exhaustionMultiplier = EXHAUSTION_MULTIPLIERS[exhaustLevel];

        int progressGain = (int) Math.floor(staminaUsed * exhaustionMultiplier);
        int newProgress = getProgressPoints(player) + progressGain;

        // Convert the new progress to actual upgrade points
        int pointsToAward = newProgress / UPGRADE_POINT_COST;
        int finalProgress = newProgress % UPGRADE_POINT_COST;

        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            staminaData.setPointsProgress(finalProgress);
        }

        // Give and notify the player of new upgrade points
        if (pointsToAward > 0) {
            player.sendSystemMessage(Component.literal("§a+" + pointsToAward + " Upgrade Point" + (pointsToAward > 1 ? "s" : "") + "!"));
            int newUpgradePointTotal = getUpgradePoints(player) + pointsToAward;
            setUpgradePoints(player, newUpgradePointTotal);
        }

    }

    // Disables powers if they have any exhaustion. TODO Add compat for Hertz power thing
    public static void disablePowers(Player player, int exhaustLevel) {
        if (exhaustLevel == 0) {
            player.sendSystemMessage(Component.literal("§cYou're exhausted! Your quirk is temporarily disabled."));
        } else {
            player.sendSystemMessage(Component.literal("§4Your body is pushed beyond its limits! Powers disabled from overexertion!"));
        }

        player.getTags().add(POWERS_DISABLED_TAG);
        setPowersDisabled(player, true);
        ParticleEffects.powersDisabledVfx(player);
    }

    public static void enablePowers(Player player) {
        player.sendSystemMessage(Component.literal("§aYou've recovered from your exhaustion."));
        player.sendSystemMessage(Component.literal("§aYour quirk is recharged and ready to use!"));
        player.getTags().remove(POWERS_DISABLED_TAG);
        setPowersDisabled(player, false);

        // Reset the last hurrah
        int currentStamina = getCurrentStamina(player);
        boolean lastHurrahUsed = getLastHurrahUsed(player);

        if (currentStamina > 0 && lastHurrahUsed) {
            IStaminaData staminaData = getStaminaDataSafe(player);
            if (staminaData != null) {
                staminaData.setLastHurrahUsed(false);
            }
        }

        ParticleEffects.powersEnabledVfx(player);
    }

    // This one is specifically used to update the scoreboard for stamina percentage.
    // A scoreboard is needed because it is tied to a Palladium HUD.
    private static void updateStaminaPercentage(Player player, int current, int max) {
        Scoreboard scoreboard = player.level().getScoreboard();
        Objective objective = scoreboard.getObjective(STAMINA_PERCENT_SCOREBOARD);
        int score;

        if (objective != null) {
            if (max == 0 || current < 0) {
                score = 0;
            } else if (current == max) {
                score = 100;
            } else {
                double percentage = ((double) current / max) * 100;

                // Round to the nearest 14% increment
                score = (int) Math.round(percentage / 7) * 7;
            }

            scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), objective).setScore(score);
        }
    }

    private static void setPowersDisabled(Player player, boolean isDisabled) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            staminaData.setPowersDisabled(isDisabled);
        }
    }

    // Sends the player a message that changes based on the current level of exhaustion, but only if it increases
    // Might get replaced in the future with just the stamina HUD changing
    private static void sendExhaustionMessage(Player player, int newExhaustLevel, int oldExhaustLevel) {
        if (newExhaustLevel > oldExhaustLevel) {
            switch (newExhaustLevel) {
                case 1:
                    player.sendSystemMessage(Component.literal("§eYou're feeling winded from overexertion..."));
                    break;
                case 2:
                    player.sendSystemMessage(Component.literal("§6Your body is struggling from severe overexertion!"));
                    break;
                case 3:
                    player.sendSystemMessage(Component.literal("§cCritical overexertion! Your body is breaking down!"));
                    break;
            }
        }
    }

    /**
     * Returns a random amount of stamina. Ranges defined in StaminaConstants.
     * This is a more biased random generation towards lower values. Used in player init
     * @param player
     * @return
     */
    public static int getRandomStamina(Player player) {
        double biasedRandom = Math.min(Math.random(), Math.random());

        return (int) Math.floor(STARTING_STAMINA_MIN + (biasedRandom * STARTING_STAMINA_MAX));
    }

    public static int getMaxStamina(Player player) {
        return getStaminaData(player).map(IStaminaData::getMaxStamina).orElse(0);
    }

    public static int getCurrentStamina(Player player) {
        return getStaminaData(player).map(IStaminaData::getCurrentStamina).orElse(0);
    }

    public static int getProgressPoints(Player player) {
        return getStaminaData(player).map(IStaminaData::getPointsProgress).orElse(0);
    }

    public static int getUpgradePoints(Player player) {
        return getStaminaData(player).map(IStaminaData::getUpgradePoints).orElse(0);
    }

    public static boolean getLastHurrahUsed(Player player) {
        return getStaminaData(player).map(IStaminaData::getLastHurrahUsed).orElse(false);
    }

    public static String getStaminaInfo(Player player) {
        return getStaminaData(player).map(staminaData ->
                staminaData.getCurrentStamina() + " / " + staminaData.getMaxStamina()
        ).orElse("0 / 100");
    }

    public static void setCurrentStamina(Player player, int amount) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            staminaData.setCurrentStamina(amount);
            updateStaminaPercentage(player, amount, staminaData.getMaxStamina());
        }
    }

    public static void addMaxStamina(Player player, int amount) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            staminaData.setMaxStamina(amount + staminaData.getMaxStamina());
            updateStaminaPercentage(player, staminaData.getCurrentStamina(), staminaData.getMaxStamina());
        }
    }

    public static void addCurrentStamina(Player player, int amount) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            staminaData.setCurrentStamina(amount + staminaData.getCurrentStamina());
            updateStaminaPercentage(player, staminaData.getCurrentStamina(), staminaData.getMaxStamina());
        }
    }

    public static void setUpgradePoints(Player player, int amount) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            staminaData.setUpgradePoints(amount);
//            syncUpgradePointsToBoard(player, amount);
        }
    }

    public static void setMaxStamina(Player player, int amount) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            staminaData.setMaxStamina(amount);
            updateStaminaPercentage(player, staminaData.getCurrentStamina(), amount);
        }
    }

    // Not only does this set the level of exhaustion, it also adjusts the current stamina to
    // reflect the level. This makes it equivalent to draining stamina
    public static void setExhaustionLevel(Player player, int level) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            staminaData.setExhaustionLevel(level);
            staminaData.setCurrentStamina(EXHAUSTION_LEVELS[level]);
        }
    }

    /**
     * This function is meant to be ran on a player's first login. Initializes all stamina variables.
     * @param player
     * @return True if the player was just initialized. False otherwise
     */
    public static boolean initializePlayerStamina(Player player) {
        AtomicBoolean retValue = new AtomicBoolean(false);
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null && !staminaData.isInitialized()) {
            int maxStamina = getRandomStamina(player);
            staminaData.setMaxStamina(maxStamina);
            staminaData.setCurrentStamina(maxStamina);
            staminaData.setUsageTotal(0);
            staminaData.setRegenCooldown(0);
            staminaData.setExhaustionLevel(0);
            staminaData.setLastHurrahUsed(false);
            staminaData.setInitialized(true);
            staminaData.setPointsProgress(0);

            setUpgradePoints(player, 0);

            player.sendSystemMessage(Component.literal("§6Your quirk manifests with " + maxStamina + " stamina capacity!"));
            retValue.set(true);
        }

        return retValue.get();
    }

    public static void debugStamina(Player player) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            player.sendSystemMessage(Component.literal("§6=== " + player.getGameProfile().getName() + "'s Stamina Status ==="));
            player.sendSystemMessage(Component.literal("§eCurrent: " + getStaminaInfo(player)));
            player.sendSystemMessage(Component.literal("§eExhaustion Level: " + staminaData.getExhaustionLevel()));
            player.sendSystemMessage(Component.literal("§eRegen Cooldown: " + staminaData.getRegenCooldown() + " ticks"));
            player.sendSystemMessage(Component.literal("§ePower Disabled: " + (staminaData.isPowersDisabled() ? "Yes" : "No")));
            player.sendSystemMessage(Component.literal("§eLast Hurrah Used: " + (staminaData.getLastHurrahUsed() ? "Yes" : "No")));
            player.sendSystemMessage(Component.literal("§ePlus Ultra: " + (player.getTags().contains("MineHa.PlusUltra") ? "§bYes" : "§cNo")));
            player.sendSystemMessage(Component.literal("§eUsage Total: " + staminaData.getUsageTotal()));
            player.sendSystemMessage(Component.literal("§aUpgrade Points: " + staminaData.getUpgradePoints()));
            player.sendSystemMessage(Component.literal("§7Points Progress: " + staminaData.getPointsProgress() + "/" + POINTS_TO_UPGRADE));
        }
    }

    public static void getUpgradePointsInfo(Player player) {
        IStaminaData staminaData = getStaminaDataSafe(player);
        if (staminaData != null) {
            player.sendSystemMessage(Component.literal("§6=== " + player.getGameProfile().getName() + "'s Upgrade Points ==="));
            player.sendSystemMessage(Component.literal("§aTotal Points: " + staminaData.getUpgradePoints()));
            player.sendSystemMessage(Component.literal("§7Progress to next point: " + staminaData.getPointsProgress() + "/" + POINTS_TO_UPGRADE));
            player.sendSystemMessage(Component.literal("§eUse these points to unlock new abilities!"));
            player.sendSystemMessage(Component.literal("§7Tip: Using abilities while exhausted gives bonus points!"));
        }
    }
}
