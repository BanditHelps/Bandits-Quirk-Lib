package com.github.b4ndithelps.forge.capabilities.Body;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = MOD_ID)
public class BodyStatusCapabilityHandler {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(MOD_ID, "body_status"),
                    new BodyStatusCapabilityProvider());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                    .ifPresent(oldBodyStatus -> {
                        event.getEntity().getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                                .ifPresent(newBodyStatus -> {
                                    newBodyStatus.deserializeNBT(oldBodyStatus.serializeNBT());
                                });
                    });
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Initialize any default values or sync to client if needed
        Player player = event.getEntity();
        player.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                .ifPresent(bodyStatus -> {
                    // Initialization goes here
                });
    }
}
