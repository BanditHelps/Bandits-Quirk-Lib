package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BodyStatusFloatPlaceholder extends Placeholder {

    public BodyStatusFloatPlaceholder() {
        super("get_body_float");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String bodyPartName = dps.values.get("body_part");
        String key = dps.values.get("key");

        if (bodyPartName == null || key == null) {
            BanditsQuirkLibForge.LOGGER.error("Body placeholder requires 'body_part' and 'key' parameters");
            return "0";
        }

        try {
            Minecraft mc = Minecraft.getInstance();

//            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
//            if (server == null) {
//                BanditsQuirkLibForge.LOGGER.error("Server is not available");
//                return "0";
//            }

            if (mc.player == null) {
                BanditsQuirkLibForge.LOGGER.error("Player is not available");
                return "0";
            }

//            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(mc.player.getUUID());
//            if (serverPlayer == null) {
//                BanditsQuirkLibForge.LOGGER.error("ServerPlayer is not found");
//                return "0";
//            }

            return String.valueOf((int)BodyStatusHelper.getCustomFloat(mc.player, bodyPartName, key));
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in Body placeholder" + e.getMessage());
            return "0";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("body_part");
        values.add("key");

        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Get Body Float Value";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Retrieves a specific float from the BodyStatus system as a String.",
                "Parameters:",
                "- body_part: The corresponding part to query",
                "- key: The key value storing the information"
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
        defaultValues.put("body_part", "chest");
        defaultValues.put("key", "key_name");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}
