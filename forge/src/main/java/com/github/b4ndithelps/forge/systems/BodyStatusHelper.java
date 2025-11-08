package com.github.b4ndithelps.forge.systems;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.capabilities.body.BodyPart;
import com.github.b4ndithelps.forge.capabilities.body.BodyStatusCapabilityProvider;
import com.github.b4ndithelps.forge.capabilities.body.CustomStatus;
import com.github.b4ndithelps.forge.capabilities.body.IBodyStatusCapability;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BodyStatusSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
     * Also resolves "arm" and "leg" do damage first the right, then the left arm.
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
        } else if ("leg".equals(lowerName)) {
            // First try the right leg, then the left
            return isPartDestroyed(player, "right_leg") ? "left_leg" : "right_leg";
        } else if ("arm".equals(lowerName)) {
            // First try the right arm, then the left
            return isPartDestroyed(player, "right_arm") ? "left_arm" : "right_arm";
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
            
            // Auto-sync to client if this is a server player
            if (player instanceof ServerPlayer serverPlayer) {
                syncToClient(serverPlayer);
            }
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

            if (!bodyStatus.isPartDestroyed(part)) {
                bodyStatus.addDamage(part, amount);
                
                // Auto-sync to client if this is a server player
                if (player instanceof ServerPlayer serverPlayer) {
                    syncToClient(serverPlayer);
                }
            }

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
            
            // Auto-sync to client if this is a server player
            if (player instanceof ServerPlayer serverPlayer) {
                syncToClient(serverPlayer);
            }
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
            
            // Auto-sync to client if this is a server player
            if (player instanceof ServerPlayer serverPlayer) {
                syncToClient(serverPlayer);
            }
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
            
            // Auto-sync to client if this is a server player
            if (player instanceof ServerPlayer serverPlayer) {
                syncToClient(serverPlayer);
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    public static void addToCustomFloat(Player player, String bodyPartName, String key, float value) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }

            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            float previousValue = bodyStatus.getCustomFloat(part, key);
            bodyStatus.setCustomFloat(part, key, previousValue + value);
            
            // Auto-sync to client if this is a server player
            if (player instanceof ServerPlayer serverPlayer) {
                syncToClient(serverPlayer);
                BanditsQuirkLibForge.LOGGER.info("Synced Value to client");
            }
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
            
            // Auto-sync to client if this is a server player
            if (player instanceof ServerPlayer serverPlayer) {
                syncToClient(serverPlayer);
            }
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

    // === SYNCHRONIZATION METHODS ===

    /**
     * Synchronizes the BodyStatus capability data from server to a specific client.
     * This should be called whenever the server-side data changes and needs to be
     * reflected on the client.
     * 
     * @param serverPlayer The server player whose data should be synced
     */
    public static void syncToClient(ServerPlayer serverPlayer) {
        serverPlayer.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                .ifPresent(bodyStatus -> {
                    CompoundTag data = bodyStatus.serializeNBT();
                    BodyStatusSyncPacket packet = new BodyStatusSyncPacket(serverPlayer.getUUID(), data);
                    BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
                });
    }

    /**
     * Synchronizes the BodyStatus capability data from server to all tracking players.
     * This is useful when the body status changes and should be visible to other players
     * (e.g., for visual effects, render layers).
     * 
     * @param serverPlayer The server player whose data should be synced
     */
    public static void syncToTrackingPlayers(ServerPlayer serverPlayer) {
        serverPlayer.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                .ifPresent(bodyStatus -> {
                    CompoundTag data = bodyStatus.serializeNBT();
                    BodyStatusSyncPacket packet = new BodyStatusSyncPacket(serverPlayer.getUUID(), data);
                    BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer), packet);
                });
    }

    /**
     * Synchronizes the BodyStatus capability data to a specific target player.
     * This is useful for sending one player's body status to another specific player.
     * 
     * @param sourcePlayer The server player whose data should be synced
     * @param targetPlayer The server player who should receive the sync
     */
    public static void syncToPlayer(ServerPlayer sourcePlayer, ServerPlayer targetPlayer) {
        sourcePlayer.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                .ifPresent(bodyStatus -> {
                    CompoundTag data = bodyStatus.serializeNBT();
                    BodyStatusSyncPacket packet = new BodyStatusSyncPacket(sourcePlayer.getUUID(), data);
                    BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> targetPlayer), packet);
                });
    }

    // === BULK OPERATION METHODS (NO AUTO-SYNC) ===

    /**
     * Sets damage for a body part WITHOUT syncing to the client.
     * Use this for bulk operations where you want to sync manually at the end.
     * 
     * @param player The player to modify
     * @param bodyPartName The name of the body part
     * @param damage The damage amount to set
     */
    public static void setDamageNoSync(Player player, String bodyPartName, float damage) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            bodyStatus.setDamage(part, damage);
            // No sync - caller must handle sync manually
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    /**
     * Adds damage to a body part WITHOUT syncing to the client.
     * Use this for bulk operations where you want to sync manually at the end.
     * 
     * @param player The player to modify
     * @param bodyPartName The name of the body part
     * @param amount The damage amount to add
     */
    public static void addDamageNoSync(Player player, String bodyPartName, float amount) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());

            if (!bodyStatus.isPartDestroyed(part)) {
                bodyStatus.addDamage(part, amount);
                // No sync - caller must handle sync manually
            }

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    /**
     * Damages all body parts WITHOUT syncing to the client.
     * Use this for bulk operations where you want to sync manually at the end.
     * 
     * @param player The player to modify
     * @param amount The damage amount to add to all parts
     */
    public static void damageAllNoSync(Player player, float amount) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            bodyStatus.damageAll(amount);
            // No sync - caller must handle sync manually
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets a custom status WITHOUT syncing to the client.
     * Use this for bulk operations where you want to sync manually at the end.
     * 
     * @param player The player to modify
     * @param bodyPartName The name of the body part
     * @param statusName The name of the custom status
     * @param level The status level to set
     */
    public static void setCustomStatusNoSync(Player player, String bodyPartName, String statusName, int level) {
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
            // No sync - caller must handle sync manually
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    /**
     * Sets a custom float value WITHOUT syncing to the client.
     * Use this for bulk operations where you want to sync manually at the end.
     * 
     * @param player The player to modify
     * @param bodyPartName The name of the body part
     * @param key The custom data key
     * @param value The float value to set
     */
    public static void setCustomFloatNoSync(Player player, String bodyPartName, String key, float value) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            bodyStatus.setCustomFloat(part, key, value);
            // No sync - caller must handle sync manually
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    /**
     * Sets a custom string value WITHOUT syncing to the client.
     * Use this for bulk operations where you want to sync manually at the end.
     * 
     * @param player The player to modify
     * @param bodyPartName The name of the body part
     * @param key The custom data key
     * @param value The string value to set
     */
    public static void setCustomStringNoSync(Player player, String bodyPartName, String key, String value) {
        try {
            IBodyStatusCapability bodyStatus = getBodyStatus(player);
            if (bodyStatus == null) {
                return; // Silently fail if capability not available
            }
            
            String resolvedName = resolveBodyPartName(player, bodyPartName);
            BodyPart part = BodyPart.valueOf(resolvedName.toUpperCase());
            bodyStatus.setCustomString(part, key, value);
            // No sync - caller must handle sync manually
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid body part: " + bodyPartName + " (resolved to: " + resolveBodyPartName(player, bodyPartName) + ")");
        }
    }

    // === REGENERATION HELPER METHODS ===

    /**
     * Gets all body parts that are damaged (damage > 0) but not destroyed.
     * This is useful for regeneration abilities that want to heal damaged parts.
     * 
     * @param player The player to check
     * @return An array of BodyPart values that are damaged but not destroyed
     */
    public static BodyPart[] getDamagedParts(Player player) {
        IBodyStatusCapability bodyStatus = getBodyStatus(player);
        if (bodyStatus == null) {
            return new BodyPart[0]; // Return empty array if capability not available
        }
        
        return Arrays.stream(BodyPart.values())
                .filter(part -> {
                    float damage = bodyStatus.getDamage(part);
                    boolean isDestroyed = bodyStatus.isPartDestroyed(part);
                    return damage > 0.0f && !isDestroyed;
                })
                .toArray(BodyPart[]::new);
    }

    /**
     * Gets all body part names that are damaged (damage > 0) but not destroyed.
     * This is useful for KubeJS and other scripting systems.
     * 
     * @param player The player to check
     * @return An array of body part names that are damaged but not destroyed
     */
    public static String[] getDamagedPartNames(Player player) {
        BodyPart[] damagedParts = getDamagedParts(player);
        return Arrays.stream(damagedParts)
                .map(part -> part.getName())
                .toArray(String[]::new);
    }

    /**
     * Heals a random damaged body part by the specified amount.
     * Only heals parts that are damaged (damage > 0) but not destroyed.
     * 
     * @param player The player to heal
     * @param healAmount The amount to heal (reduces damage)
     * @return The name of the body part that was healed, or null if no parts could be healed
     */
    public static String healRandomDamagedPart(Player player, float healAmount) {
        BodyPart[] damagedParts = getDamagedParts(player);
        if (damagedParts.length == 0) {
            return null; // No damaged parts to heal
        }

        // Pick a random damaged part
        BodyPart randomPart = damagedParts[new Random().nextInt(damagedParts.length)];
        
        // Get current damage and calculate new damage
        IBodyStatusCapability bodyStatus = getBodyStatus(player);
        if (bodyStatus == null) {
            return null; // Capability not available
        }
        
        float currentDamage = bodyStatus.getDamage(randomPart);
        float newDamage = Math.max(0.0f, currentDamage - healAmount);
        
        // Apply the healing
        bodyStatus.setDamage(randomPart, newDamage);
        
        // Auto-sync to client if this is a server player
        if (player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
        
        return randomPart.getName();
    }

    /**
     * Heals a random damaged body part by the specified amount WITHOUT syncing.
     * Use this for bulk operations where you want to sync manually at the end.
     * 
     * @param player The player to heal
     * @param healAmount The amount to heal (reduces damage)
     * @return The name of the body part that was healed, or null if no parts could be healed
     */
    public static String healRandomDamagedPartNoSync(Player player, float healAmount) {
        BodyPart[] damagedParts = getDamagedParts(player);
        if (damagedParts.length == 0) {
            return null; // No damaged parts to heal
        }

        // Pick a random damaged part
        BodyPart randomPart = damagedParts[new Random().nextInt(damagedParts.length)];
        
        // Get current damage and calculate new damage
        IBodyStatusCapability bodyStatus = getBodyStatus(player);
        if (bodyStatus == null) {
            return null; // Capability not available
        }
        
        float currentDamage = bodyStatus.getDamage(randomPart);
        float newDamage = Math.max(0.0f, currentDamage - healAmount);
        
        // Apply the healing without syncing
        bodyStatus.setDamage(randomPart, newDamage);
        
        return randomPart.getName();
    }

    /**
     * Checks if a player has any damaged body parts (excluding destroyed parts).
     * 
     * @param player The player to check
     * @return true if the player has any damaged but not destroyed parts
     */
    public static boolean hasDamagedParts(Player player) {
        return getDamagedParts(player).length > 0;
    }

    /**
     * Gets the total damage across all body parts (excluding destroyed parts).
     * 
     * @param player The player to check
     * @return The total damage amount
     */
    public static float getTotalDamage(Player player) {
        IBodyStatusCapability bodyStatus = getBodyStatus(player);
        if (bodyStatus == null) {
            return 0.0f; // Return 0 if capability not available
        }
        
        return Arrays.stream(BodyPart.values())
                .filter(part -> !bodyStatus.isPartDestroyed(part))
                .map(part -> bodyStatus.getDamage(part))
                .reduce(0.0f, Float::sum);
    }
}