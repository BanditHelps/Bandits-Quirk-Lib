package com.github.b4ndithelps.forge;

import com.github.b4ndithelps.forge.fancymenu.ForgeFancyMenuIntegration;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.github.b4ndithelps.BanditsQuirkLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(BanditsQuirkLib.MOD_ID)
public final class BanditsQuirkLibForge {
    public static final String LOGGING_ID = "Bandit's Quirk Lib | Forge";

    public static final Logger LOGGER = LoggerFactory.getLogger(LOGGING_ID);

    public BanditsQuirkLibForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(BanditsQuirkLib.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        BanditsQuirkLib.init();

        ForgeFancyMenuIntegration.init();
    }


}
