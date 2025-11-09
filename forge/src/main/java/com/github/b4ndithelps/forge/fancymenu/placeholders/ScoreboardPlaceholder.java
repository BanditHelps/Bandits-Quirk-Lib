package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ScoreboardPlaceholder extends Placeholder {

    public ScoreboardPlaceholder() {
        super("get_scoreboard");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String scoreboardName = dps.values.get("scoreboard");
        String playerName = dps.values.get("player");

        if (scoreboardName == null) {
            BanditsQuirkLibForge.LOGGER.error("Scoreboard placeholder requires 'scoreboard' parameter");
            return "0";
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            LocalPlayer player = mc.player;

            if (level == null || player == null) {
                return "0";
            }

            Scoreboard scoreboard = level.getScoreboard();

            Objective objective = scoreboard.getObjective(scoreboardName);

            if (objective == null) {
                BanditsQuirkLibForge.LOGGER.warn("Scoreboard '{}' not found", scoreboardName);
                return "0";
            }

            // Determine what player to check
            String targetPlayer = playerName != null ? playerName : player.getGameProfile().getName();

            // Get score for the player
            Score score = scoreboard.getOrCreatePlayerScore(targetPlayer, objective);
            int scoreValue = score.getScore();

            return String.valueOf(scoreValue);
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in Scoreboard placeholder" + e.getMessage());
            return "0";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("scoreboard");
        values.add("player"); // this one is optional

        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Scoreboard Value";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Retrieves a specific scoreboard value as a String.",
                "Parameters:",
                "- scoreboard: Name of the scoreboard objective",
                "- player: Player name (optional, defaults to the current player)",
                "Example: %scoreboard{scoreboard:\"scoreboard_name\"}%"
        );
    }

    @Override
    public String getCategory() {
        return "BanditsQuirkLib";
    }

    @SuppressWarnings("removal")
    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> defaultValues = new HashMap<>();
        defaultValues.put("scoreboard", "scoreboard_name");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}