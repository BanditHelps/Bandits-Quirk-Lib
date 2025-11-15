package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.particle.ModParticles;
import com.github.b4ndithelps.forge.particle.custom.ChargingDustParticle;
import com.github.b4ndithelps.forge.particle.custom.RisingDustParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void registerParticle(RegisterParticleProvidersEvent  event) {
        Minecraft.getInstance().particleEngine.register(
                ModParticles.RISING_DUST.get(), RisingDustParticle.Provider::new
        );
        Minecraft.getInstance().particleEngine.register(
                ModParticles.CHARGE_DUST_PARTICLE.get(), ChargingDustParticle.Provider::new
        );
    }
}
