package com.github.b4ndithelps.forge.capabilities;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.capabilities.Body.IBodyStatusCapability;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CapabilityRegistration {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IStaminaData.class);
        event.register(IBodyStatusCapability.class);
    }
}
