package com.github.b4ndithelps.forge.capabilities.stamina;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.StaminaSyncPacket;
import com.github.b4ndithelps.values.BodyConstants;
import com.github.b4ndithelps.values.StaminaConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.level.SleepFinishedTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
public class StaminaCapabilityHandler {

    private static final Set<UUID> sleepingPlayers = new HashSet<>();

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(BanditsQuirkLib.MOD_ID, "stamina_data"), new StaminaDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        if (event.isWasDeath()) {
            // Get the old player's capability data
            event.getOriginal().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(oldStore -> {
                // Get the new player's capability
                event.getEntity().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(newStore -> {
                    // Copy all the data from old to new
                    newStore.setMaxStamina(oldStore.getMaxStamina());
                    newStore.setUsageTotal(oldStore.getUsageTotal());
                    newStore.setPowersDisabled(oldStore.isPowersDisabled());
                    newStore.setInitialized(oldStore.isInitialized());
                    newStore.setUpgradePoints(oldStore.getUpgradePoints());

                    // Special Values that should not be the same on death ie. exhaustion, lastHurrah, and current Stamina
                    newStore.setPointsProgress(0);
                    newStore.setCurrentStamina((int) Math.floor( (double) oldStore.getMaxStamina() / 2));
                    newStore.setExhaustionLevel(0);
                    newStore.setLastHurrahUsed(false);
                    newStore.setRegenCooldown(StaminaConstants.STAMINA_REGEN_COOLDOWNS[0]);
                });
            });

        } else {
            event.getOriginal().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(oldStore -> {
                // Get the new player's capability
                event.getEntity().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(newStore -> {
                    // Copy all the data from old to new
                    newStore.setMaxStamina(oldStore.getMaxStamina());
                    newStore.setUsageTotal(oldStore.getUsageTotal());
                    newStore.setPowersDisabled(oldStore.isPowersDisabled());
                    newStore.setInitialized(oldStore.isInitialized());
                    newStore.setUpgradePoints(oldStore.getUpgradePoints());
                    newStore.setPointsProgress(oldStore.getPointsProgress());
                    newStore.setCurrentStamina(oldStore.getCurrentStamina());
                    newStore.setExhaustionLevel(oldStore.getExhaustionLevel());
                    newStore.setLastHurrahUsed(oldStore.getLastHurrahUsed());
                    newStore.setRegenCooldown(oldStore.getRegenCooldown());
                });
            });
        }

        if (event.getEntity() instanceof ServerPlayer sp) {
            BQLNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    StaminaSyncPacket.fullSync(sp)
            );
        }

        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Force save capability data when player logs out
        event.getEntity().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(staminaData -> {
            // The capability system should handle this automatically, but we can force it
//            BanditsQuirkLib.LOGGER.debug("Player {} logged out with stamina: {}",
//                    event.getEntity().getName().getString(), staminaData.getCurrentStamina());
        });
    }

    @SubscribeEvent
    public static void onPlayerSleepInBed(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide) {
            sleepingPlayers.add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onSleepFinished(SleepFinishedTimeEvent event) {
        // This event fires when players successfully sleep through the night
        ServerLevel level = (ServerLevel) event.getLevel();

        // Get all players who were sleeping in beds
        for (ServerPlayer player : level.getPlayers(serverPlayer ->
                sleepingPlayers.contains(serverPlayer.getUUID()))) {

            handlePlayerSleptThroughNight(player);
        }

        // Clear the sleeping players set
        sleepingPlayers.clear();
    }

    private static void handlePlayerSleptThroughNight(Player player) {
        player.getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(staminaData -> {
            // When they wake up, give them a configurable amount of their stamina back
            staminaData.setCurrentStamina((int) (staminaData.getCurrentStamina() + staminaData.getMaxStamina() * BodyConstants.STAMINA_SLEEP_RECOVERY_PERCENTAGE));
        });
    }
}