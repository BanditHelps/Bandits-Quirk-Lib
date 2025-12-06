package com.github.b4ndithelps.forge.systems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Lightweight facade around the body status capability for tracking a player's internal temperature.
 * Abilities can call {@link #raiseInnerTemp(Player, float)} or {@link #lowerInnerTemp(Player, float)}
 * without worrying about the underlying storage details.
 */
public final class TempHelper {

    private static final String STORAGE_PART = "chest";
    private static final String INTERNAL_TEMP_KEY = "internal_temp";

    private static final float BASELINE_TEMP = 0.0F;
    private static final float MIN_TEMP = -300.0F;
    private static final float MAX_TEMP = 100.0F;

    private static final float BASE_HEAT_DISABLE = 15.0F;
    private static final float HEAT_TOLERANCE_PER_QUIRK = 0.35F;

    private static final float BASE_HEAT_COOL_RATE = 0.04F;
    private static final float BASE_COLD_WARM_RATE = 0.015F;
    private static final float MAX_NATURAL_RATE = 0.18F;
    private static final float QUIRK_COOLING_SCALE = 0.01F;
    private static final float TEMP_DROP_EPSILON = 0.05F;
    private static final int NORMALIZE_DELAY_TICKS = 80;
    private static final String TEMP_LAST_KEY = "Bql.TempLast";
    private static final String TEMP_DELAY_KEY = "Bql.TempNormalizeDelay";

    private TempHelper() {
    }

    /**
     * @return true when the player can safely read the stored temperature.
     */
    public static boolean canAccess(Player player) {
        return player != null && BodyStatusHelper.isBodyStatusAvailable(player);
    }

    private static boolean canMutate(Player player) {
        return canAccess(player) && !player.level().isClientSide();
    }

    /**
     * Gets the current stored inner temperature or the baseline if unavailable.
     */
    public static float getInnerTemp(Player player) {
        if (!canAccess(player)) {
            return BASELINE_TEMP;
        }
        try {
            return BodyStatusHelper.getCustomFloat(player, STORAGE_PART, INTERNAL_TEMP_KEY);
        } catch (RuntimeException ignored) {
            return BASELINE_TEMP;
        }
    }

    /**
     * @return dynamic heat threshold where ice powers shut off, adjusted by quirk factor.
     */
    public static float getHeatDisableThreshold(Player player) {
        float threshold = BASE_HEAT_DISABLE;
        if (player instanceof ServerPlayer serverPlayer) {
            double factor = QuirkFactorHelper.getQuirkFactor(serverPlayer);
            threshold += (float) (factor * HEAT_TOLERANCE_PER_QUIRK);
        }
        return Mth.clamp(threshold, BASELINE_TEMP, MAX_TEMP);
    }

    public static boolean isOverheated(Player player) {
        return getInnerTemp(player) > getHeatDisableThreshold(player);
    }

    /**
     * Raises internal temperature by the provided amount (clamped to the configured bounds).
     */
    public static float raiseInnerTemp(Player player, float amount) {
        return adjustInternalTemp(player, Math.max(0.0F, amount));
    }

    /**
     * Lowers internal temperature by the provided amount (clamped to the configured bounds).
     */
    public static float lowerInnerTemp(Player player, float amount) {
        return adjustInternalTemp(player, -Math.max(0.0F, amount));
    }

    /**
     * Moves the stored value toward the baseline by the provided rate.
     */
    public static float settleTowardsBaseline(Player player, float rate) {
        float current = getInnerTemp(player);
        if (rate <= 0.0F || Math.abs(current - BASELINE_TEMP) < 1.0e-3F) {
            return current;
        }
        float direction = current > BASELINE_TEMP ? -1.0F : 1.0F;
        return adjustInternalTemp(player, direction * Math.min(Math.abs(current - BASELINE_TEMP), rate));
    }

    /**
     * Applies gentle passive cooling/warming every player tick.
     */
    public static void tickNaturalCooling(ServerPlayer player) {
        if (!canMutate(player)) {
            return;
        }
        float temp = getInnerTemp(player);

        if (suppressNormalization(player, temp)) {
            return;
        }
        if (Math.abs(temp - BASELINE_TEMP) < 1.0e-3F) {
            return;
        }

        double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
        float quirkBonus = (float) Math.min(quirkFactor, 50.0) * QUIRK_COOLING_SCALE;
        if (temp > BASELINE_TEMP) {
            float rate = Mth.clamp(BASE_HEAT_COOL_RATE + quirkBonus, BASE_HEAT_COOL_RATE, MAX_NATURAL_RATE);
            settleTowardsBaseline(player, rate);
        } else {
            settleTowardsBaseline(player, BASE_COLD_WARM_RATE);
        }
    }

    private static boolean suppressNormalization(ServerPlayer player, float currentTemp) {
        CompoundTag tag = player.getPersistentData();
        float lastTemp = tag.contains(TEMP_LAST_KEY) ? tag.getFloat(TEMP_LAST_KEY) : currentTemp;
        int delay = tag.getInt(TEMP_DELAY_KEY);

        if (currentTemp < lastTemp - TEMP_DROP_EPSILON) {
            delay = NORMALIZE_DELAY_TICKS;
        } else if (delay > 0) {
            delay--;
        }

        tag.putFloat(TEMP_LAST_KEY, currentTemp);
        if (delay > 0) {
            tag.putInt(TEMP_DELAY_KEY, delay);
            return true;
        }
        if (tag.contains(TEMP_DELAY_KEY)) {
            tag.remove(TEMP_DELAY_KEY);
        }
        return false;
    }

    /**
     * Directly sets the internal temperature to the provided value (after clamping).
     */
    public static float setInnerTemp(Player player, float value) {
        if (!canMutate(player)) {
            return BASELINE_TEMP;
        }
        float clamped = Mth.clamp(value, MIN_TEMP, MAX_TEMP);
        BodyStatusHelper.setCustomFloat(player, STORAGE_PART, INTERNAL_TEMP_KEY, clamped);
        return clamped;
    }

    private static float adjustInternalTemp(Player player, float delta) {
        if (!canMutate(player)) {
            return BASELINE_TEMP;
        }
        float current = getInnerTemp(player);
        float updated = Mth.clamp(current + delta, MIN_TEMP, MAX_TEMP);
        BodyStatusHelper.setCustomFloat(player, STORAGE_PART, INTERNAL_TEMP_KEY, updated);
        return updated;
    }
}
