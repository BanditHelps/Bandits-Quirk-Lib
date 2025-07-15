package com.github.b4ndithelps.forge.capabilities;

import com.github.b4ndithelps.BanditsQuirkLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
public class StaminaCapabilityHandler {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(BanditsQuirkLib.MOD_ID, "stamina_data"), new StaminaDataProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            // Get the old player's capability data
            event.getOriginal().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(oldStore -> {
                // Get the new player's capability
                event.getEntity().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(newStore -> {
                    // Copy all the data from old to new
                    newStore.setCurrentStamina(oldStore.getCurrentStamina());
                    newStore.setMaxStamina(oldStore.getMaxStamina());
                    newStore.setUsageTotal(oldStore.getUsageTotal());
                    newStore.setRegenCooldown(oldStore.getRegenCooldown());
                    newStore.setExhaustionLevel(oldStore.getExhaustionLevel());
                    newStore.setLastHurrahUsed(oldStore.getLastHurrahUsed());
                    newStore.setPowersDisabled(oldStore.isPowersDisabled());
                    newStore.setInitialized(oldStore.isInitialized());
                    newStore.setUpgradePoints(oldStore.getUpgradePoints());
                    newStore.setPointsProgress(oldStore.getPointsProgress());

                    // Debug logging
                    BanditsQuirkLib.LOGGER.info("Cloned stamina data - Old: {}, New: {}",
                            oldStore.getCurrentStamina(), newStore.getCurrentStamina());
                });
            });
            event.getOriginal().invalidateCaps();
        }
    }

    // Optional: Add this event to handle player logout/login persistence
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // Force save capability data when player logs out
        event.getEntity().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(staminaData -> {
            // The capability system should handle this automatically, but we can force it
            BanditsQuirkLib.LOGGER.debug("Player {} logged out with stamina: {}",
                    event.getEntity().getName().getString(), staminaData.getCurrentStamina());
        });
    }
}