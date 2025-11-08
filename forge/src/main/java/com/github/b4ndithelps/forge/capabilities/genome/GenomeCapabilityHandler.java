package com.github.b4ndithelps.forge.capabilities.genome;

import com.github.b4ndithelps.forge.systems.GenomeHelper;
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
public class GenomeCapabilityHandler {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player player) {
            event.addCapability(new ResourceLocation(MOD_ID, "genome"), new GenomeCapabilityProvider(() -> player));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Mirror BodyStatus behavior: always copy capability data on clone (death or dimension change)
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(GenomeCapabilityProvider.GENOME_CAPABILITY).ifPresent(oldCap -> {
            event.getEntity().getCapability(GenomeCapabilityProvider.GENOME_CAPABILITY).ifPresent(newCap -> {
                newCap.deserializeNBT(oldCap.serializeNBT());
            });
        });
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            GenomeHelper.syncToClient(sp);
        }
    }
}