package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.capabilities.Body.BodyPart;
import com.github.b4ndithelps.forge.capabilities.Body.BodyStatusCapabilityProvider;
import com.github.b4ndithelps.forge.capabilities.Body.CustomStatus;
import com.github.b4ndithelps.forge.capabilities.Body.IBodyStatusCapability;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
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

    /**
     * Safely checks if the body status capability is available for the given player.
     * 
     * @param player The player to check
     * @return true if the capability is available, false otherwise
     */
    public static boolean isBodyStatusAvailable(Player player) {
        return player.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY).isPresent();
    }

    /**
     * Gets the body status capability for the given player.
     * Returns null if the capability is not available (e.g., during player death/respawn).
     * 
     * @param player The player to get the capability for
     * @return The IBodyStatusCapability instance, or null if not available
     */
    public static IBodyStatusCapability getBodyStatus(Player player) {
        return player.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY).orElse(null);
    }

    /**
     * Resolves special body part names (main_arm, off_arm) to actual body parts based on player's main hand setting.
     * If the body part name is not special, returns the original name.
     * 
     * @param player The player to check main hand setting for
     * @param bodyPartName The body part name to resolve
     * @return The resolved body part name
     */
    private static String resolveBodyPartName(Player player, String bodyPartName) {
        String lowerName = bodyPartName.toLowerCase();
        
        if ("main_arm".equals(lowerName)) {
            // Get the player's main hand setting
            HumanoidArm mainArm = player.getMainArm();
            return mainArm == HumanoidArm.RIGHT ? "right_arm" : "left_arm";
        } else if ("off_arm".equals(lowerName)) {
            // Get the opposite of the player's main hand setting
            HumanoidArm mainArm = player.getMainArm();
            return mainArm == HumanoidArm.RIGHT ? "left_arm" : "right_arm";
        }
        
        // Return original name if not a special case
        return bodyPartName;
    }

    // Convenience methods for KubeJS
    public static float getDamage(Player player, String bodyPartName) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return 0.0f; // Return default value if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.getDamage(part);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static void setDamage(Player player, String bodyPartName, float damage) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            bodyStatus.setDamage(part, damage);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static void addDamage(Player player, String bodyPartName, float amount) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            bodyStatus.addDamage(part, amount);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static void damageAll(Player player, float amount) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            bodyStatus.damageAll(amount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDamageStage(Player player, String bodyPartName) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return "healthy"; // Return default stage if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.getDamageStage(part).getName();
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static int getCustomStatus(Player player, String bodyPartName, String statusName) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return 0; // Return default value if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.getCustomStatus(part, statusName);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static void setCustomStatus(Player player, String bodyPartName, String statusName, int level) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
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
            
            bodyStatus.setCustomStatus(part, statusName, level);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    /**
     * Initializes a custom status for a body part only if it doesn't already exist.
     * This is safe to call during player login events as it won't overwrite existing data.
     * 
     * @param player The player to initialize the status for
     * @param bodyPartName The name of the body part (case insensitive, supports main_arm/off_arm)
     * @param statusName The name of the custom status
     * @param defaultLevel The default level to set (only if status doesn't exist)
     * @return true if the status was initialized, false if it already existed or capability not available
     */
    public static boolean initializeNewStatus(Player player, String bodyPartName, String statusName, int defaultLevel) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return false; // Return false if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            
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
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
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
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return false; // Return false if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.isPartBroken(part);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static boolean isPartDestroyed(Player player, String bodyPartName) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return false; // Return false if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.isPartDestroyed(part);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static boolean isPartSprained(Player player, String bodyPartName) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return false; // Return false if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.isPartSprained(part);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    // Enhanced custom data methods for KubeJS
    public static float getCustomFloat(Player player, String bodyPartName, String key) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return 0.0f; // Return default value if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.getCustomFloat(part, key);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static void setCustomFloat(Player player, String bodyPartName, String key, float value) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            bodyStatus.setCustomFloat(part, key, value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static void addToCustomFloat(Player player, String bodyPartName, String key, float value) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);

            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            float previousValue = bodyStatus.getCustomFloat(part, key);
            bodyStatus.setCustomFloat(part, key, previousValue + value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static String getCustomString(Player player, String bodyPartName, String key) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return ""; // Return empty string if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.getCustomString(part, key);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static void setCustomString(Player player, String bodyPartName, String key, String value) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            bodyStatus.setCustomString(part, key, value);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static boolean hasCustomFloat(Player player, String bodyPartName, String key) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return false; // Return false if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.hasCustomFloat(part, key);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static boolean hasCustomString(Player player, String bodyPartName, String key) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return false; // Return false if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            return bodyStatus.hasCustomString(part, key);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    /**
     * Utility method to get the actual body part name after resolving main_arm/off_arm.
     * Useful for debugging or displaying which arm is actually being targeted.
     * 
     * @param player The player to resolve for
     * @param bodyPartName The body part name (may include main_arm/off_arm)
     * @return The resolved body part name
     */
    public static String getResolvedBodyPartName(Player player, String bodyPartName) {
        return resolveBodyPartName(player, bodyPartName);
    }
}
