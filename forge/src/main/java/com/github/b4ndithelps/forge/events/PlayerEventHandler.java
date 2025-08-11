package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.abilities.HappenOnceAbility;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.threetag.palladium.power.SuperpowerUtil;

import java.util.UUID;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();

        HappenOnceAbility.cleanupPlayerData(playerUUID);

        BanditsQuirkLibForge.LOGGER.info("Cleaned up player data");
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();

        // Only runs if they haven't been here before
        boolean wasInitialized = StaminaHelper.initializePlayerStamina(player);

        // Since I store a "was initialized" inside of the StaminaHelper, just use it to decide if we need these again
        if (wasInitialized) {
            SuperpowerUtil.addSuperpower(player, ResourceLocation.parse("bql:base_quirk"));
            SuperpowerUtil.addSuperpower(player, ResourceLocation.parse("bql:body_status"));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Client mirror: if permeation is active or rising, force spectator-like no-clip locally
        if (event.player.level().isClientSide) {
            boolean active = event.player.getTags().contains("Bql.PermeateActive");
            boolean rising = event.player.getTags().contains("Bql.PermeateRise");

            if (active || rising) {
                event.player.noPhysics = true;
                event.player.fallDistance = 0.0F;
            } else {
                event.player.noPhysics = false;
            }
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) return;
        
        // Handle permeation rise: push player upward until target Y or free space is reached
        if (player.getTags().contains("Bql.PermeateRise")) {
            double desiredUp = player.getPersistentData().getDouble("Bql.PermeateRiseSpeed");
            if (desiredUp <= 0.0D) desiredUp = 0.6D;
            double targetY = player.getPersistentData().getDouble("Bql.PermeateTargetY");

            // Keep phasing while rising
            player.noPhysics = true;
            player.fallDistance = 0.0F;

            // Move upward deterministically to avoid collision resolution fighting: teleport small step each tick
            double currentY = player.getY();
            double step = Math.max(0.05D, Math.min(desiredUp, Math.max(0.0D, targetY - currentY)));
            double nextY = currentY + step;
            player.teleportTo(player.getX(), nextY, player.getZ());
            // Keep a slight upward velocity for client smoothing
            var motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.8, Math.max(motion.y, 0.1D), motion.z * 0.8);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

            // Consider reached if player's feet are at/above target Y, or if player is in free space for a couple ticks
            boolean reachedTargetY = player.getY() >= targetY - 0.001;
            boolean free = player.level().noCollision(player, player.getBoundingBox().inflate(-0.02));
            int freeTicks = player.getPersistentData().getInt("Bql.PermeateFreeTicks");
            if (free) {
                freeTicks++;
                player.getPersistentData().putInt("Bql.PermeateFreeTicks", freeTicks);
            } else {
                player.getPersistentData().putInt("Bql.PermeateFreeTicks", 0);
            }

            // Debug tracing each tick of rise
            com.github.b4ndithelps.forge.BanditsQuirkLibForge.LOGGER.info(
                    "[Permeation] Rising tick for {}: y={}/target={} step={} free={} freeTicks={}",
                    player.getGameProfile().getName(),
                    String.format("%.2f", player.getY()),
                    String.format("%.2f", targetY),
                    String.format("%.2f", step),
                    free, freeTicks);

            // Stop when reached target Y or stable free space
            if (reachedTargetY || freeTicks >= 2) {
                // Stop rising and restore normal physics
                player.noPhysics = false;
                player.removeTag("Bql.PermeateActive");
                player.removeTag("Bql.PermeateRise");
                player.getPersistentData().remove("Bql.PermeateRiseSpeed");
                player.getPersistentData().remove("Bql.PermeateTargetY");
                player.getPersistentData().remove("Bql.PermeateFreeTicks");
                
                // Damp velocity to avoid overshoot bounce
                var endMotion = player.getDeltaMovement();
                player.setDeltaMovement(endMotion.x * 0.5, 0.0, endMotion.z * 0.5);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));

                // Debug stop
                com.github.b4ndithelps.forge.BanditsQuirkLibForge.LOGGER.info(
                        "[Permeation] Rising end for {} at y={}: reachedTargetY={} freeTicks={}",
                        player.getGameProfile().getName(), String.format("%.2f", player.getY()), reachedTargetY, freeTicks);
            }
        }
        
        // Auto-switch handedness based on destroyed arms for better visuals
        // Store the player's original handedness once, then flip when one arm is destroyed
        boolean storedOriginal = player.getPersistentData().contains("Bql.OrigLeftHanded");
        if (!storedOriginal) {
            player.getPersistentData().putBoolean("Bql.OrigLeftHanded", player.getMainArm() == HumanoidArm.LEFT);
        }

        boolean rightArmDestroyed = BodyStatusHelper.isPartDestroyed(player, "right_arm");
        boolean leftArmDestroyed = BodyStatusHelper.isPartDestroyed(player, "left_arm");

        boolean desiredLeftHanded;
        if (rightArmDestroyed && !leftArmDestroyed) {
            // Use left as main if right arm is destroyed
            desiredLeftHanded = true;
        } else if (leftArmDestroyed && !rightArmDestroyed) {
            // Use right as main if left arm is destroyed
            desiredLeftHanded = false;
        } else {
            // Neither or both destroyed: restore original preference
            desiredLeftHanded = player.getPersistentData().getBoolean("Bql.OrigLeftHanded");
        }

        if ((player.getMainArm() == HumanoidArm.LEFT) != desiredLeftHanded) {
            player.setMainArm(desiredLeftHanded ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
        }

        // Check if player has the movement restriction tag
        if (player.getTags().contains("Bql.RestrictMove")) {
            // Check if player still has any stun effect active
            boolean hasStunEffect = false;
            for (var effect : player.getActiveEffects()) {
                if (effect.getEffect().getClass().getSimpleName().equals("StunMobEffect")) {
                    hasStunEffect = true;
                    break;
                }
            }
            
            // If no stun effect, remove movement restriction and cleanup
            if (!hasStunEffect) {
                player.removeTag("Bql.RestrictMove");
                player.removeTag("Bql.RestrictMove.HasStoredPos");
                player.getPersistentData().remove("Bql.StoredX");
                player.getPersistentData().remove("Bql.StoredZ");
                player.getPersistentData().remove("Bql.StoredYaw");
                player.getPersistentData().remove("Bql.StoredPitch");
                return;
            }
            
            // Store current horizontal position and rotation (only once when tag is first detected)
            if (!player.getTags().contains("Bql.RestrictMove.HasStoredPos")) {
                player.getPersistentData().putDouble("Bql.StoredX", player.getX());
                player.getPersistentData().putDouble("Bql.StoredZ", player.getZ());
                player.getPersistentData().putFloat("Bql.StoredYaw", player.getYRot());
                player.getPersistentData().putFloat("Bql.StoredPitch", player.getXRot());
                player.addTag("Bql.RestrictMove.HasStoredPos");
            }
            
            // Get stored horizontal position and rotation
            double storedX = player.getPersistentData().getDouble("Bql.StoredX");
            double storedZ = player.getPersistentData().getDouble("Bql.StoredZ");
            float storedYaw = player.getPersistentData().getFloat("Bql.StoredYaw");
            float storedPitch = player.getPersistentData().getFloat("Bql.StoredPitch");
            
            // Check if player has moved horizontally from stored position
            double deltaX = Math.abs(player.getX() - storedX);
            double deltaZ = Math.abs(player.getZ() - storedZ);
            double deltaYaw = Math.abs(player.getYRot() - storedYaw);
            double deltaPitch = Math.abs(player.getXRot() - storedPitch);
            
            // Movement threshold (small tolerance for floating point precision)
            double movementThreshold = 0.01;
            double rotationThreshold = 0.5;
            
            // If player has moved horizontally beyond threshold or rotated, reset their position
            if (deltaX > movementThreshold || deltaZ > movementThreshold ||
                deltaYaw > rotationThreshold || deltaPitch > rotationThreshold) {
                
                // Reset player horizontal position and rotation to stored values, keep current Y
                player.teleportTo(storedX, player.getY(), storedZ);
                player.setYRot(storedYaw);
                player.setXRot(storedPitch);
                
                // Set horizontal velocity to zero but preserve downward velocity for falling
                Vec3 currentVelocity = player.getDeltaMovement();
                double yVelocity = Math.min(currentVelocity.y, 0.0); // Only preserve downward movement
                player.setDeltaMovement(0.0, yVelocity, 0.0);
            }
        }
    }
}
