package com.github.b4ndithelps.forge.fancymenu;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.fancymenu.placeholders.ScoreboardBitmapPlaceholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeFancyMenuIntegration {

    @SuppressWarnings("removal")
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeFancyMenuIntegration::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("fancymenu")) {
            event.enqueueWork(() -> {
                try {
                    // Register placeholders
                    PlaceholderRegistry.register(new ScoreboardBitmapPlaceholder());

                    BanditsQuirkLibForge.LOGGER.info("Fancy Menu Integration loaded successfully!");
                } catch (Exception e) {
                    BanditsQuirkLibForge.LOGGER.error("Failed to register FancyMenu integration: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}
