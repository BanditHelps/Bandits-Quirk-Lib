package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.util.property.BooleanProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.StringProperty;

@SuppressWarnings("removal")
public class BodyStatusModifierAbility extends Ability {

    // Configurable properties
    public static final PalladiumProperty<String> VALUE_TYPE = new StringProperty("value_type").configurable("Type of value to modify: 'float', 'string', or 'status'");
    public static final PalladiumProperty<String> BODY_PART = new StringProperty("body_part").configurable("Body part to modify (e.g., 'head', 'main_arm', 'off_arm', 'torso', 'left_leg', 'right_leg')");
    public static final PalladiumProperty<String> KEY = new StringProperty("key").configurable("Key name for the custom value");
    public static final PalladiumProperty<String> OPERATION = new StringProperty("operation").configurable("Operation type: 'set', 'add', 'subtract', 'multiply', 'divide' (float/status only)");
    public static final PalladiumProperty<Float> FLOAT_VALUE = new FloatProperty("float_value").configurable("Float value to use in operation (for float type)");
    public static final PalladiumProperty<String> STRING_VALUE = new StringProperty("string_value").configurable("String value to set (for string type)");
    public static final PalladiumProperty<Integer> STATUS_VALUE = new IntegerProperty("status_value").configurable("Status value to use in operation (for status type)");
    public static final PalladiumProperty<Float> MIN_VALUE = new FloatProperty("min_value").configurable("Minimum value limit (float/status only, -1 = no limit)");
    public static final PalladiumProperty<Float> MAX_VALUE = new FloatProperty("max_value").configurable("Maximum value limit (float/status only, -1 = no limit)");
    public static final PalladiumProperty<Integer> TICK_INTERVAL = new IntegerProperty("tick_interval").configurable("Ticks between operations (1 = every tick, 20 = every second)");
    public static final PalladiumProperty<Boolean> APPLY_ONCE = new BooleanProperty("apply_once").configurable("Apply operation only once when ability activates");
    public static final PalladiumProperty<Boolean> SEND_MESSAGE = new BooleanProperty("send_message").configurable("Send message to player when modifying values");

    // Unique properties for tracking state
    public static final PalladiumProperty<Boolean> HAS_APPLIED_ONCE;
    public static final PalladiumProperty<Integer> TICK_COUNTER;
    public static final PalladiumProperty<Float> PREVIOUS_FLOAT_VALUE;
    public static final PalladiumProperty<String> PREVIOUS_STRING_VALUE;
    public static final PalladiumProperty<Integer> PREVIOUS_STATUS_VALUE;

    public BodyStatusModifierAbility() {
        super();
        this.withProperty(VALUE_TYPE, "float")
                .withProperty(BODY_PART, "torso")
                .withProperty(KEY, "custom_value")
                .withProperty(OPERATION, "add")
                .withProperty(FLOAT_VALUE, 1.0f)
                .withProperty(STRING_VALUE, "")
                .withProperty(STATUS_VALUE, 1)
                .withProperty(MIN_VALUE, -1.0f)
                .withProperty(MAX_VALUE, -1.0f)
                .withProperty(TICK_INTERVAL, 20)
                .withProperty(APPLY_ONCE, false)
                .withProperty(SEND_MESSAGE, false);
    }

    public void registerUniqueProperties(PropertyManager manager) {
        manager.register(HAS_APPLIED_ONCE, false);
        manager.register(TICK_COUNTER, 0);
        manager.register(PREVIOUS_FLOAT_VALUE, 0.0f);
        manager.register(PREVIOUS_STRING_VALUE, "");
        manager.register(PREVIOUS_STATUS_VALUE, 0);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled && entity instanceof ServerPlayer player) {
            // Reset state
            entry.setUniqueProperty(HAS_APPLIED_ONCE, false);
            entry.setUniqueProperty(TICK_COUNTER, 0);
            
            // Store previous values for potential restoration
            storePreviousValues(player, entry);
            
            // Apply immediately if apply_once is true
            if (entry.getProperty(APPLY_ONCE)) {
                System.out.println("first tick");
                applyModification(player, entry);
                entry.setUniqueProperty(HAS_APPLIED_ONCE, true);
            }
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!enabled) return;

        // If apply_once is true and we've already applied, don't do anything
        if (entry.getProperty(APPLY_ONCE) && entry.getProperty(HAS_APPLIED_ONCE)) return;

        int currentTicks = entry.getProperty(TICK_COUNTER);
        int tickInterval = entry.getProperty(TICK_INTERVAL);
        
        entry.setUniqueProperty(TICK_COUNTER, currentTicks + 1);

        // Apply modification at specified intervals
        if (currentTicks % tickInterval == 0) {
            System.out.println("real tick");
            applyModification(player, entry);
        }
    }

    private void storePreviousValues(ServerPlayer player, AbilityInstance entry) {
        if (!BodyStatusHelper.isBodyStatusAvailable(player)) return;

        String valueType = entry.getProperty(VALUE_TYPE).toLowerCase();
        String bodyPart = entry.getProperty(BODY_PART);
        String key = entry.getProperty(KEY);

        try {
            switch (valueType) {
                case "float" -> {
                    float prevValue = BodyStatusHelper.getCustomFloat(player, bodyPart, key);
                    entry.setUniqueProperty(PREVIOUS_FLOAT_VALUE, prevValue);
                }
                case "string" -> {
                    String prevValue = BodyStatusHelper.getCustomString(player, bodyPart, key);
                    entry.setUniqueProperty(PREVIOUS_STRING_VALUE, prevValue);
                }
                case "status" -> {
                    int prevValue = BodyStatusHelper.getCustomStatus(player, bodyPart, key);
                    entry.setUniqueProperty(PREVIOUS_STATUS_VALUE, prevValue);
                }
            }
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.warn("Failed to store previous value: " + e.getMessage());
        }
    }

    private void applyModification(ServerPlayer player, AbilityInstance entry) {
        if (!BodyStatusHelper.isBodyStatusAvailable(player)) return;

        String valueType = entry.getProperty(VALUE_TYPE).toLowerCase();
        String bodyPart = entry.getProperty(BODY_PART);
        String key = entry.getProperty(KEY);
        String operation = entry.getProperty(OPERATION).toLowerCase();

        try {
            boolean success = false;
            
            switch (valueType) {
                case "float" -> success = applyFloatModification(player, entry, bodyPart, key, operation);
                case "string" -> success = applyStringModification(player, entry, bodyPart, key);
                case "status" -> success = applyStatusModification(player, entry, bodyPart, key, operation);
                default -> BanditsQuirkLibForge.LOGGER.warn("Invalid value type: " + valueType);
            }

            if (success) {
                if (entry.getProperty(SEND_MESSAGE)) {
                    sendMessage(player, entry, valueType, bodyPart, key);
                }
            }
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Failed to apply body status modification: " + e.getMessage());
            if (entry.getProperty(SEND_MESSAGE)) {
                player.sendSystemMessage(Component.literal("§cFailed to modify " + key + ": " + e.getMessage()));
            }
        }
    }

    private boolean applyFloatModification(ServerPlayer player, AbilityInstance entry, String bodyPart, String key, String operation) {
        float operationValue = entry.getProperty(FLOAT_VALUE);
        float currentValue = BodyStatusHelper.getCustomFloat(player, bodyPart, key);
        float newValue = currentValue;

        switch (operation) {
            case "set" -> newValue = operationValue;
            case "add" -> newValue = currentValue + operationValue;
            case "subtract" -> newValue = currentValue - operationValue;
            case "multiply" -> newValue = currentValue * operationValue;
            case "divide" -> {
                if (operationValue != 0) {
                    newValue = currentValue / operationValue;
                } else {
                    BanditsQuirkLibForge.LOGGER.warn("Division by zero attempted in float operation");
                    return false;
                }
            }
            default -> {
                BanditsQuirkLibForge.LOGGER.warn("Invalid operation for float: " + operation);
                return false;
            }
        }

        // Apply bounds checking
        newValue = applyBounds(newValue, entry);
        
        BodyStatusHelper.setCustomFloat(player, bodyPart, key, newValue);
        return true;
    }

    private boolean applyStringModification(ServerPlayer player, AbilityInstance entry, String bodyPart, String key) {
        String newValue = entry.getProperty(STRING_VALUE);
        BodyStatusHelper.setCustomString(player, bodyPart, key, newValue);
        return true;
    }

    private boolean applyStatusModification(ServerPlayer player, AbilityInstance entry, String bodyPart, String key, String operation) {
        int operationValue = entry.getProperty(STATUS_VALUE);
        int currentValue = BodyStatusHelper.getCustomStatus(player, bodyPart, key);
        int newValue = currentValue;

        switch (operation) {
            case "set" -> newValue = operationValue;
            case "add" -> newValue = currentValue + operationValue;
            case "subtract" -> newValue = currentValue - operationValue;
            case "multiply" -> newValue = currentValue * operationValue;
            case "divide" -> {
                if (operationValue != 0) {
                    newValue = currentValue / operationValue;
                } else {
                    BanditsQuirkLibForge.LOGGER.warn("Division by zero attempted in status operation");
                    return false;
                }
            }
            default -> {
                BanditsQuirkLibForge.LOGGER.warn("Invalid operation for status: " + operation);
                return false;
            }
        }

        // Apply bounds checking (convert to float for bounds check, then back to int)
        float boundedValue = applyBounds((float) newValue, entry);
        newValue = Math.round(boundedValue);
        
        BodyStatusHelper.setCustomStatus(player, bodyPart, key, newValue);
        return true;
    }

    private float applyBounds(float value, AbilityInstance entry) {
        float minValue = entry.getProperty(MIN_VALUE);
        float maxValue = entry.getProperty(MAX_VALUE);

        if (minValue >= 0 && value < minValue) {
            value = minValue;
        }
        if (maxValue >= 0 && value > maxValue) {
            value = maxValue;
        }

        return value;
    }

    private void sendMessage(ServerPlayer player, AbilityInstance entry, String valueType, String bodyPart, String key) {
        String resolvedBodyPart = BodyStatusHelper.getResolvedBodyPartName(player, bodyPart);
        String operation = entry.getProperty(OPERATION);
        
        Component message = Component.literal(String.format("§a%s %s on %s: %s", 
                operation.substring(0, 1).toUpperCase() + operation.substring(1),
                key, resolvedBodyPart, 
                getCurrentValueString(player, entry, valueType, bodyPart, key)));
        
        player.sendSystemMessage(message);
    }

    private String getCurrentValueString(ServerPlayer player, AbilityInstance entry, String valueType, String bodyPart, String key) {
        try {
            return switch (valueType) {
                case "float" -> String.format("%.2f", BodyStatusHelper.getCustomFloat(player, bodyPart, key));
                case "string" -> "\"" + BodyStatusHelper.getCustomString(player, bodyPart, key) + "\"";
                case "status" -> String.valueOf(BodyStatusHelper.getCustomStatus(player, bodyPart, key));
                default -> "unknown";
            };
        } catch (Exception e) {
            return "error";
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Modifies custom float, string, or status values stored in body parts through the BodyStatusHelper system. " +
               "Supports various operations (set, add, subtract, multiply, divide) with configurable bounds, timing, and feedback. " +
               "Can apply modifications once or continuously at specified intervals. Supports main_arm/off_arm resolution and quirk factor integration.";
    }

    static {
        HAS_APPLIED_ONCE = new BooleanProperty("has_applied_once");
        TICK_COUNTER = new IntegerProperty("tick_counter");
        PREVIOUS_FLOAT_VALUE = new FloatProperty("previous_float_value");
        PREVIOUS_STRING_VALUE = new StringProperty("previous_string_value");
        PREVIOUS_STATUS_VALUE = new IntegerProperty("previous_status_value");
    }
}