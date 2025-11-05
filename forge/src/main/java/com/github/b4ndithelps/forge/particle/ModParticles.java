package com.github.b4ndithelps.forge.particle;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.particle.custom.RisingDustParticle;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BanditsQuirkLib.MOD_ID);

    public static final RegistryObject<SimpleParticleType> RISING_DUST = PARTICLES.register("rising_dust", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> CHARGE_DUST_PARTICLE =
            PARTICLES.register("charging_dust_particle", () -> new SimpleParticleType(true));

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}