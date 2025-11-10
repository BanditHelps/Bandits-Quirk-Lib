package com.github.b4ndithelps.forge.events;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.NoShadowTagPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        // When any entity is added server-side, notify all tracking players of current no-shadow state
        boolean noShadow = entity.getTags().contains("Bql.NoShadow");
        if (event.getLevel() instanceof ServerLevel level) {
            // Always send current state to new trackers (covers both true/false for late joiners)
            BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), new NoShadowTagPacket(entity.getId(), noShadow));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Send initial snapshot for all players' no-shadow state to the joining player
        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e.getTags().contains("Bql.NoShadow")) {
                    BQLNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new NoShadowTagPacket(e.getId(), true));
                }
            }
        }
    }
}