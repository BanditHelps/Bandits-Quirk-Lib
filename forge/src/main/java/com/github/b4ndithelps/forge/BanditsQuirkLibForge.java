package com.github.b4ndithelps.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.github.b4ndithelps.BanditsQuirkLib;

@Mod(BanditsQuirkLib.MOD_ID)
public final class BanditsQuirkLibForge {
    public BanditsQuirkLibForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(BanditsQuirkLib.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        BanditsQuirkLib.init();
    }
}
