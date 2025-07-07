package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.capabilities.IStaminaData;
import com.github.b4ndithelps.forge.capabilities.StaminaDataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;

import static com.github.b4ndithelps.values.StaminaConstants.EXHAUSTION_LEVELS;
import static com.github.b4ndithelps.values.StaminaConstants.POINTS_TO_UPGRADE;

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

    public static boolean useStamina(Player player, int amount) {
        if (!hasStamina(player, amount)) return false;

        getStaminaData(player).ifPresent(staminaData -> {
           staminaData.setCurrentStamina(staminaData.getCurrentStamina() - amount);
           staminaData.setUsageTotal(staminaData.getUsageTotal() + amount);
        });

        return true;
    }

    public static int getMaxStamina(Player player) {
        return getStaminaData(player).map(IStaminaData::getMaxStamina).orElse(0);
    }

    public static int getCurrentStamina(Player player) {
        return getStaminaData(player).map(IStaminaData::getCurrentStamina).orElse(0);
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

    public static void getUpgradePoints(Player player) {
        getStaminaData(player).ifPresent(staminaData -> {
            player.sendSystemMessage(Component.literal("§6=== " + player.getGameProfile().getName() + "'s Upgrade Points ==="));
            player.sendSystemMessage(Component.literal("§aTotal Points: " + staminaData.getUpgradePoints()));
            player.sendSystemMessage(Component.literal("§7Progress to next point: " + staminaData.getPointsProgress() + "/" + POINTS_TO_UPGRADE));
            player.sendSystemMessage(Component.literal("§eUse these points to unlock new abilities!"));
            player.sendSystemMessage(Component.literal("§7Tip: Using abilities while exhausted gives bonus points!"));
        });
    }
}
