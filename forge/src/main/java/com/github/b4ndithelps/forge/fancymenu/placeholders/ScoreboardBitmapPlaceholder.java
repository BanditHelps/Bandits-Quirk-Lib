package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.BanditsQuirkLib;
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

public class ScoreboardBitmapPlaceholder extends Placeholder {

    public ScoreboardBitmapPlaceholder() {
        super("bitmap_check");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String scoreboardName = dps.values.get("scoreboard");
        String bitPositionStr = dps.values.get("bit");
        String playerName = dps.values.get("player");

        if (scoreboardName == null || bitPositionStr == null) {
            BanditsQuirkLibForge.LOGGER.error("ScoreboardBitmap placeholder requires 'scoreboard' and 'bit' parameters");
            return "false";
        }

        try {
            int bitPosition = Integer.parseInt(bitPositionStr);

            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            LocalPlayer player = mc.player;

            if (level == null || player == null) {
                return "false";
            }

            Scoreboard scoreboard = level.getScoreboard();



            scoreboard.getObjectiveNames().forEach( (n) -> {
                BanditsQuirkLibForge.LOGGER.info("The dumb names are: " + n);
            });

            Objective objective = scoreboard.getObjective(scoreboardName);

            if (objective == null) {
                BanditsQuirkLibForge.LOGGER.warn("Scoreboard '{}' not found", scoreboardName);
                return "false";
            }

            // Determine what player to check
            String targetPlayer = playerName != null ? playerName : player.getGameProfile().getName();

            // Get score for the player
            Score score = scoreboard.getOrCreatePlayerScore(targetPlayer, objective);
            int scoreValue = score.getScore();

            // Check if the bit at the specified position is set using modulus
            // We use bit shifting to create a mask: 1 << bitPosition gives us the bit we want to check
            // Then we AND it with the score to see if that bit is set
            int bitMask  = 1 << bitPosition;
            boolean bitIsSet = (scoreValue & bitMask) != 0;

            return bitIsSet ? "true" : "false";
        } catch (NumberFormatException e) {
            BanditsQuirkLibForge.LOGGER.error("Invalid bit position '{}' in ScoreboardBitmap placeholder", bitPositionStr);
            return "false";
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in ScoreboardBitmap placeholder" + e.getMessage());
            return "false";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("scoreboard");
        values.add("bit");
        values.add("player"); // this one is optional

        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Scoreboard Bitmap Check";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Checks if a specific bit is set in a scoreboard value.",
                "Returns 'true' if the bit is set, 'false' otherwise.",
                "Parameters:",
                "- scoreboard: Name of the scoreboard objective",
                "- bit: Bit position to check (0-31)",
                "- player: Player name (optional, defaults to the current player)",
                "Example: %scoreboard_bitmap{scoreboard:\"scoreboard_name\";bit:\"3\"}%"
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
        defaultValues.put("bit", "0");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}
