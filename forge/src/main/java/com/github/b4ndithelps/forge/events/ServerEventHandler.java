package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID)
public class ServerEventHandler {

    /**
     * Currently used to initialize the following scoreboards:
     * - MineHa.StaminaPercentage -> Used for the visualization of stamina on the HUD
     * - MineHa.QuirkFactor -> The overall power level each player has
     * @param event
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Scoreboard scoreboard = server.getScoreboard();

        String staminaObj = "MineHa.StaminaPercentage";
        String quirkFactorObj = "MineHa.QuirkFactor";

        // Check if the objective already exists to avoid duplicates
        if (scoreboard.getObjective(staminaObj) == null) {
            scoreboard.addObjective(
                    staminaObj,
                    ObjectiveCriteria.DUMMY,
                    Component.literal("MineHa.StaminaPercentage"),
                    ObjectiveCriteria.RenderType.INTEGER
            );
        }

        if (scoreboard.getObjective(quirkFactorObj) == null) {
            scoreboard.addObjective(
                    quirkFactorObj,
                    ObjectiveCriteria.DUMMY,
                    Component.literal("MineHa.QuirkFactor"),
                    ObjectiveCriteria.RenderType.INTEGER
            );
        }

        BanditsQuirkLibForge.LOGGER.info("Server started!");
    }
}
