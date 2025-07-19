package com.github.b4ndithelps.forge.config;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("BQL-ConfigEvents");

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        LOGGER.info("Config load event triggered for: {}", event.getConfig().getFileName());
        if (event.getConfig().getSpec() == BQLConfig.SPEC) {
            LOGGER.info("BQL config loaded, updating constants...");
            // Config is now loaded, we can safely access config values
            ConfigManager.loadCreationShopData();
            ConfigManager.updateConstants();
        } else {
            LOGGER.info("Config loaded was not BQL config: {}", event.getConfig().getSpec());
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        LOGGER.info("Config reload event triggered for: {}", event.getConfig().getFileName());
        if (event.getConfig().getSpec() == BQLConfig.SPEC) {
            LOGGER.info("BQL config reloaded, updating constants...");
            // Config is reloaded, reload creation shop data and update constants
            ConfigManager.loadCreationShopData();
            ConfigManager.updateConstants();
        } else {
            LOGGER.info("Config reloaded was not BQL config: {}", event.getConfig().getSpec());
        }
    }

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            LOGGER.info("Server starting - loading dynamic configs");
            ConfigManager.loadDynamicConfigs();
        }
    }
} 