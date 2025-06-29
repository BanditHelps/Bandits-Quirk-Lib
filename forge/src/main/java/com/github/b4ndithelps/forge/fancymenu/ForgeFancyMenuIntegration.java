package com.github.b4ndithelps.forge.fancymenu;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.fancymenu.elements.IconButtonElementBuilder;
import com.github.b4ndithelps.forge.fancymenu.placeholders.ScoreboardBitmapPlaceholder;
import com.github.b4ndithelps.forge.fancymenu.placeholders.ScoreboardPlaceholder;
import de.keksuccino.fancymenu.customization.element.ElementRegistry;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ForgeFancyMenuIntegration {

    public static final ScoreboardBitmapPlaceholder SCOREBOARD_BITMAP_PLACEHOLDER = new ScoreboardBitmapPlaceholder();
    public static final ScoreboardPlaceholder SCOREBOARD_PLACEHOLDER = new ScoreboardPlaceholder();

    @SuppressWarnings("removal")
    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(ForgeFancyMenuIntegration::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("fancymenu")) {
            event.enqueueWork(() -> {
                try {
                    // Register placeholders
                    PlaceholderRegistry.register(SCOREBOARD_BITMAP_PLACEHOLDER);
                    PlaceholderRegistry.register(SCOREBOARD_PLACEHOLDER);

//                    ElementRegistry.register(ICON_BUTTON);

                    BanditsQuirkLibForge.LOGGER.info("Fancy Menu Integration loaded successfully!");
                } catch (Exception e) {
                    BanditsQuirkLibForge.LOGGER.error("Failed to register FancyMenu integration: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}
