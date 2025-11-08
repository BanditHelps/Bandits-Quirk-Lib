package com.github.b4ndithelps.forge;

import com.github.b4ndithelps.forge.abilities.AbilityRegister;
import com.github.b4ndithelps.forge.conditions.CustomConditionSerializers;
import com.github.b4ndithelps.forge.effects.ModEffects;
import com.github.b4ndithelps.forge.blocks.ModBlocks;
import com.github.b4ndithelps.forge.entities.ModEntities;
import com.github.b4ndithelps.forge.blocks.ModBlockEntities;
import com.github.b4ndithelps.forge.item.ModCreativeTabs;
import com.github.b4ndithelps.forge.item.ModItems;
import com.github.b4ndithelps.forge.fancymenu.ForgeFancyMenuIntegration;
import com.github.b4ndithelps.forge.blocks.ModMenus;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.config.ConfigManager;
import com.github.b4ndithelps.forge.config.ModGameRules;
import com.github.b4ndithelps.forge.genetics.GenesReloadListener;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(BanditsQuirkLib.MOD_ID, modEventBus);

        // Initialize config system
        ConfigManager.initialize();
        
        // Register gamerules
        ModGameRules.register();

        // Run our common setup.
        BanditsQuirkLib.init();

        // Forge Specific
        ForgeFancyMenuIntegration.init();
        CustomConditionSerializers.CUSTOM_SERIALIZERS.register();
        AbilityRegister.ABILITIES.register();
        ModEffects.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntities.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);

        BQLNetwork.register();

        // Register for global (Forge) event bus
        MinecraftForge.EVENT_BUS.register(ReloadHandlers.class);
    }

    @Mod.EventBusSubscriber(modid = BanditsQuirkLib.MOD_ID)
    public static class ReloadHandlers {
        @SubscribeEvent
        public static void onAddReloadListeners(AddReloadListenerEvent event) {
            event.addListener(new GenesReloadListener());
        }
    }
}