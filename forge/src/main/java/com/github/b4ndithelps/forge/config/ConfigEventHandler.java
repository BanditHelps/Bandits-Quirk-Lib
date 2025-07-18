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
        LOGGER.info("Config loaded: {}", event.getConfig().getFileName());
        ConfigManager.updateConstants();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        LOGGER.info("Config reloaded: {}", event.getConfig().getFileName());
        ConfigManager.updateConstants();
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