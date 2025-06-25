package com.github.b4ndithelps.fabric;

import net.fabricmc.api.ModInitializer;

import com.github.b4ndithelps.BanditsQuirkLib;

public final class BanditsQuirkLibFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        BanditsQuirkLib.init();
    }
}
