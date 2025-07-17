package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.capabilities.Body.BodyPart;
import com.github.b4ndithelps.forge.capabilities.Body.BodyStatusCapabilityProvider;
import com.github.b4ndithelps.forge.capabilities.Body.CustomStatus;
import com.github.b4ndithelps.forge.capabilities.Body.IBodyStatusCapability;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class BodyStatusHelper {
    private static final Map<String, CustomStatus> registeredStatuses = new HashMap<>();

    /**
     * Registers a custom status for use with body parts.
     * This allows validation of levels and provides stage names.
     * 
     * @param status The custom status to register
     */
    public static void registerCustomStatus(CustomStatus status) {
        registeredStatuses.put(status.getName(), status);
    }

    /**
     * Gets a registered custom status by name.
     * 
     * @param name The name of the custom status
     * @return The CustomStatus object, or null if not registered
     */
    public static CustomStatus getCustomStatus(String name) {
        return registeredStatuses.get(name);
    }

    /**
     * Gets all registered custom statuses.
     * 
     * @return A copy of the registered statuses map
     */
    public static Map<String, CustomStatus> getAllRegisteredStatuses() {
        return new HashMap<>(registeredStatuses);
    }

    public static IBodyStatusCapability getBodyStatus(Player player) {
        return player.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                .orElseThrow(() -> new RuntimeException("Body status capability not found"));
    }

    // Convenience methods for KubeJS
    public static float getDamage(Player player, String bodyPartName) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).getDamage(part);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static void setDamage(Player player, String bodyPartName, float damage) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            getBodyStatus(player).setDamage(part, damage);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static void addDamage(Player player, String bodyPartName, float amount) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            getBodyStatus(player).addDamage(part, amount);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static String getDamageStage(Player player, String bodyPartName) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).getDamageStage(part).getName();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static int getCustomStatus(Player player, String bodyPartName, String statusName) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).getCustomStatus(part, statusName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static void setCustomStatus(Player player, String bodyPartName, String statusName, int level) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            CustomStatus status = getCustomStatus(statusName);
            
            // If status is registered, validate the level. Otherwise, allow any positive level (backwards compatibility)
            if (status != null) {
                if (!status.isValidLevel(level)) {
                    throw new RuntimeException("Invalid level " + level + " for status " + statusName + 
                                             ". Valid range: 0-" + status.getMaxLevel());
                }
            } else if (level < 0) {
                throw new RuntimeException("Level cannot be negative for status " + statusName);
            }
            
            getBodyStatus(player).setCustomStatus(part, statusName, level);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    /**
     * Initializes a custom status for a body part only if it doesn't already exist.
     * This is safe to call during player login events as it won't overwrite existing data.
     * 
     * @param player The player to initialize the status for
     * @param bodyPartName The name of the body part (case insensitive)
     * @param statusName The name of the custom status
     * @param defaultLevel The default level to set (only if status doesn't exist)
     * @return true if the status was initialized, false if it already existed
     */
    public static boolean initializeNewStatus(Player player, String bodyPartName, String statusName, int defaultLevel) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            
            // Check if the status already exists (level > 0)
            if (bodyStatus.hasCustomStatus(part, statusName)) {
                return false; // Status already exists, don't overwrite
            }
            
            // Validate the default level if status is registered
            CustomStatus status = getCustomStatus(statusName);
            if (status != null) {
                if (!status.isValidLevel(defaultLevel)) {
                    throw new RuntimeException("Invalid default level " + defaultLevel + " for status " + statusName + 
                                             ". Valid range: 0-" + status.getMaxLevel());
                }
            } else if (defaultLevel < 0) {
                throw new RuntimeException("Default level cannot be negative for status " + statusName);
            }
            
            // Set the status since it doesn't exist
            bodyStatus.setCustomStatus(part, statusName, defaultLevel);
            return true;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    /**
     * Initializes a custom status for all body parts only if they don't already exist.
     * This is safe to call during player login events as it won't overwrite existing data.
     * 
     * @param player The player to initialize the status for
     * @param statusName The name of the custom status
     * @param defaultLevel The default level to set (only if status doesn't exist)
     * @return the number of body parts that had the status initialized
     */
    public static int initializeNewStatusForAllParts(Player player, String statusName, int defaultLevel) {
        int initializedCount = 0;
        
        for (BodyPart part : BodyPart.values()) {
            if (initializeNewStatus(player, part.getName(), statusName, defaultLevel)) {
                initializedCount++;
            }
        }
        
        return initializedCount;
    }

    public static boolean isPartBroken(Player player, String bodyPartName) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).isPartBroken(part);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static boolean isPartDestroyed(Player player, String bodyPartName) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).isPartDestroyed(part);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static boolean isPartSprained(Player player, String bodyPartName) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).isPartSprained(part);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    // Enhanced custom data methods for KubeJS
    public static float getCustomFloat(Player player, String bodyPartName, String key) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).getCustomFloat(part, key);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static void setCustomFloat(Player player, String bodyPartName, String key, float value) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            getBodyStatus(player).setCustomFloat(part, key, value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static String getCustomString(Player player, String bodyPartName, String key) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).getCustomString(part, key);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static void setCustomString(Player player, String bodyPartName, String key, String value) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            getBodyStatus(player).setCustomString(part, key, value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static boolean hasCustomFloat(Player player, String bodyPartName, String key) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).hasCustomFloat(part, key);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }

    public static boolean hasCustomString(Player player, String bodyPartName, String key) {
        try {
            BodyPart part = BodyPart.valueOf(bodyPartName.toUpperCase());
            return getBodyStatus(player).hasCustomString(part, key);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName);
        }
    }
}
