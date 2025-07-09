package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.capabilities.IStaminaData;
import com.github.b4ndithelps.forge.capabilities.StaminaDataProvider;
import com.github.b4ndithelps.forge.damage.ModDamageTypes;
import com.github.b4ndithelps.forge.vfx.ParticleEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;

import static com.github.b4ndithelps.values.StaminaConstants.*;

public class StaminaHelper {
    public static LazyOptional<IStaminaData> getStaminaData(Player player) {
        return player.getCapability(StaminaDataProvider.STAMINA_DATA, null);
    }

    public static void modifyStamina(Player player, int amount) {
        getStaminaData(player).ifPresent(staminaData -> {
            int newStamina = Math.max(0, Math.min(staminaData.getMaxStamina(),
                    staminaData.getCurrentStamina() + amount));
            staminaData.setCurrentStamina(newStamina);
        });
    }

    public static boolean hasStamina(Player player, int required) {
        return getStaminaData(player).map(staminaData ->
                staminaData.getCurrentStamina() >= required).orElse(false);
    }

    /**
     * This function is the one called whenever someone uses any of their stamina.
     * Handles the exhaustion levels as well as the level accumulation
     * @param player
     * @param amount
     */
    public static void useStamina(Player player, int amount) {
        getStaminaData(player).ifPresent(staminaData -> {

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

            sendExhaustionMessage(player, newExhaustionLevel, oldExhaustionLevel);


        });
    }

    // For every STAMINA_GAIN_REQUIREMENT points of stamina used, have a small chance to increase the maximum
    // stamina by some random amount. Will be configurable.
    private static void handleStaminaMaxIncrease(Player player, int exhaustLevel, int maxStamina, int newUsageTotal) {
        double chanceToIncrease = exhaustLevel > 0 ? STAMINA_GAIN_EXHAUSTED_CHANCE : STAMINA_GAIN_CHANCE;

        if (newUsageTotal >= STAMINA_GAIN_REQ && Math.random() < chanceToIncrease) {
            int maxIncrease = (int) Math.floor(Math.random() * STAMINA_MAX_INCREASE) + 1;
            player.sendSystemMessage(Component.literal("§6Your training pays off! Maximum stamina increased by " + maxIncrease + "!"));

            int newMax = getMaxStamina(player) + maxIncrease;

            getStaminaData(player).ifPresent(staminaData -> {
                staminaData.setMaxStamina(newMax);
            });
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

        getStaminaData(player).ifPresent(staminaData -> {
            staminaData.setPointsProgress(finalProgress);
        });

        // Give and notify the player of new upgrade points
        if (pointsToAward > 0) {
            player.sendSystemMessage(Component.literal("§a+" + pointsToAward + " Upgrade Point" + (pointsToAward > 1 ? "s" : "") + "!"));
            int newUpgradePointTotal = getUpgradePoints(player) + pointsToAward;
            getStaminaData(player).ifPresent(staminaData -> {
                staminaData.setUpgradePoints(newUpgradePointTotal);
            });
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
        ParticleEffects.powersDisabledVfx(player);
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

    public static String getStaminaInfo(Player player) {
        return getStaminaData(player).map(staminaData ->
                staminaData.getCurrentStamina() + " / " + staminaData.getMaxStamina()
        ).orElse("0 / 100");
    }

    public static void setCurrentStamina(Player player, int amount) {
        getStaminaData(player).ifPresent(staminaData -> {
            staminaData.setCurrentStamina(amount);
        });
    }

    public static void setUpgradePoints(Player player, int amount) {
        getStaminaData(player).ifPresent(staminaData -> {
            staminaData.setUpgradePoints(amount);
        });
    }

    public static void setMaxStamina(Player player, int amount) {
        getStaminaData(player).ifPresent(staminaData -> {
            staminaData.setMaxStamina(amount);
        });
    }

    // Not only does this set the level of exhaustion, it also adjusts the current stamina to
    // reflect the level. This makes it equivalent to draining stamina
    public static void setExhaustionLevel(Player player, int level) {
        getStaminaData(player).ifPresent(staminaData -> {
            staminaData.setExhaustionLevel(level);
            staminaData.setCurrentStamina(EXHAUSTION_LEVELS[level]);
        });
    }

    public static void debugStamina(Player player) {
        getStaminaData(player).ifPresent(staminaData -> {
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
        });
    }

    public static void getUpgradePointsInfo(Player player) {
        getStaminaData(player).ifPresent(staminaData -> {
            player.sendSystemMessage(Component.literal("§6=== " + player.getGameProfile().getName() + "'s Upgrade Points ==="));
            player.sendSystemMessage(Component.literal("§aTotal Points: " + staminaData.getUpgradePoints()));
            player.sendSystemMessage(Component.literal("§7Progress to next point: " + staminaData.getPointsProgress() + "/" + POINTS_TO_UPGRADE));
            player.sendSystemMessage(Component.literal("§eUse these points to unlock new abilities!"));
            player.sendSystemMessage(Component.literal("§7Tip: Using abilities while exhausted gives bonus points!"));
        });
    }
}
