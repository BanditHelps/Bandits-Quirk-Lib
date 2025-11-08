package com.github.b4ndithelps.forge.capabilities.body;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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
        if (event.getObject() instanceof Player player) {
            // Create provider with player supplier for auto-sync functionality
            event.addCapability(new ResourceLocation(MOD_ID, "body_status"),
                    new BodyStatusCapabilityProvider(() -> player));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                .ifPresent(oldBodyStatus -> {
                    event.getEntity().getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                            .ifPresent(newBodyStatus -> {
                                newBodyStatus.deserializeNBT(oldBodyStatus.serializeNBT());
                                newBodyStatus.setDamage(BodyPart.HEAD, 0);
                                newBodyStatus.setDamage(BodyPart.CHEST, 0);
                                newBodyStatus.setDamage(BodyPart.LEFT_ARM, 0);
                                newBodyStatus.setDamage(BodyPart.RIGHT_ARM, 0);
                                newBodyStatus.setDamage(BodyPart.LEFT_LEG, 0);
                                newBodyStatus.setDamage(BodyPart.RIGHT_LEG, 0);
                                newBodyStatus.setDamage(BodyPart.LEFT_FOOT, 0);
                                newBodyStatus.setDamage(BodyPart.RIGHT_FOOT, 0);
                                newBodyStatus.setDamage(BodyPart.RIGHT_HAND, 0);
                                newBodyStatus.setDamage(BodyPart.LEFT_HAND, 0);
                            });
                });
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Initialize any default values and sync to client
        Player player = event.getEntity();
        
        // Only sync for server players
        if (player instanceof ServerPlayer serverPlayer) {
            player.getCapability(BodyStatusCapabilityProvider.BODY_STATUS_CAPABILITY)
                    .ifPresent(bodyStatus -> {
                        // Sync the current body status data to the client
                        // This ensures client has the same data as server
                        BodyStatusHelper.syncToClient(serverPlayer);
                    });
        }
    }
}