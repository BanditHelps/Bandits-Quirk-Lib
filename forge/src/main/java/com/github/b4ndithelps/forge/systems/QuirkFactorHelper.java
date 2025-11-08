package com.github.b4ndithelps.forge.systems;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class QuirkFactorHelper {
    
    private static final String QUIRK_FACTOR_OBJECTIVE = "MineHa.QuirkFactor";
    
    /**
     * Gets the quirk factor for a player from the scoreboard.
     * Quirk factor is a multiplier that affects ability potency and effects.
     * 
     * @param player The player to get the quirk factor for
     * @return The quirk factor as a decimal (e.g., 0.5 for 50% boost)
     */
    public static double getQuirkFactor(ServerPlayer player) {
        try {
            Scoreboard scoreboard = player.getServer().getScoreboard();
            Objective quirkFactorObj = scoreboard.getObjective(QUIRK_FACTOR_OBJECTIVE);
            
            if (quirkFactorObj == null) {
                // If no objective exists, return 0 (no quirk factor)
                return 0.0;
            }

            return scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), quirkFactorObj).getScore();
            
        } catch (Exception e) {
            // If anything goes wrong, return 0
            return 0.0;
        }
    }
    
    /**
     * Sets the quirk factor for a player on the scoreboard.
     * 
     * @param player The player to set the quirk factor for
     * @param quirkFactor The quirk factor as a decimal (will be converted to score)
     */
    public static void setQuirkFactor(ServerPlayer player, double quirkFactor) {
        try {
            Scoreboard scoreboard = player.getServer().getScoreboard();
            Objective quirkFactorObj = scoreboard.getObjective(QUIRK_FACTOR_OBJECTIVE);
            
            if (quirkFactorObj == null) {
                // Create the objective if it doesn't exist
                quirkFactorObj = scoreboard.addObjective(QUIRK_FACTOR_OBJECTIVE, 
                    ObjectiveCriteria.DUMMY,
                    Component.literal("Quirk Factor"),
                    ObjectiveCriteria.RenderType.INTEGER);
            }
            
            // Convert decimal to score (e.g., 0.1 multiplier = score of 1)
            int score = (int) quirkFactor;
            scoreboard.getOrCreatePlayerScore(player.getGameProfile().getName(), quirkFactorObj).setScore(score);
            
        } catch (Exception e) {
            // Silent fail - scoreboard operations are not critical
        }
    }
} 