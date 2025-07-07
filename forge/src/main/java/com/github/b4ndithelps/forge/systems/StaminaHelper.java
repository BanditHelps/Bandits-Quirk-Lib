package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.capabilities.IStaminaData;
import com.github.b4ndithelps.forge.capabilities.StaminaDataProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;

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
                staminaData.getCurrentStamina() + "/" + staminaData.getMaxStamina()
        ).orElse("0/100");
    }

    public static void setCurrentStamina(Player player, int amount) {
        getStaminaData(player).ifPresent(staminaData -> {
            staminaData.setCurrentStamina(amount);
        });
    }
}
