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
            if (!event.getObject().getCapability(StaminaDataProvider.STAMINA_DATA).isPresent()) {
                event.addCapability(new ResourceLocation(BanditsQuirkLib.MOD_ID, "stamina_data"), new StaminaDataProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerCloned(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(oldStore -> {
                event.getEntity().getCapability(StaminaDataProvider.STAMINA_DATA).ifPresent(newStore -> {
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
                });
            });
        }
    }
}
