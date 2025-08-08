package com.github.b4ndithelps.forge.fancymenu;

import com.github.b4ndithelps.BanditsQuirkLib;
import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.fancymenu.elements.IconButtonElementBuilder;
import com.github.b4ndithelps.forge.fancymenu.placeholders.*;
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
    public static final NBTPlaceholder NBT_PLACEHOLDER = new NBTPlaceholder();
    public static final PersistentDataPlaceholder PERSISTENT_DATA_PLACEHOLDER = new PersistentDataPlaceholder();
    public static final EnchantmentDataPlaceholder ENCHANTMENT_DATA_PLACEHOLDER = new EnchantmentDataPlaceholder();
    public static final DynamicConfigPlaceholder DYNAMIC_CONFIG_PLACEHOLDER = new DynamicConfigPlaceholder();
    public static final ShopConstantPlaceholder SHOP_CONSTANT_PLACEHOLDER = new ShopConstantPlaceholder();
    public static final BodyStatusFloatPlaceholder BODY_STATUS_FLOAT_PLACEHOLDER = new BodyStatusFloatPlaceholder();


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
                    PlaceholderRegistry.register(NBT_PLACEHOLDER);
                    PlaceholderRegistry.register(PERSISTENT_DATA_PLACEHOLDER);
                    PlaceholderRegistry.register(DYNAMIC_CONFIG_PLACEHOLDER);
                    PlaceholderRegistry.register(SHOP_CONSTANT_PLACEHOLDER);
                    PlaceholderRegistry.register(BODY_STATUS_FLOAT_PLACEHOLDER);

                    BanditsQuirkLibForge.LOGGER.info("Fancy Menu Integration loaded successfully!");
                } catch (Exception e) {
                    BanditsQuirkLibForge.LOGGER.error("Failed to register FancyMenu integration: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}
