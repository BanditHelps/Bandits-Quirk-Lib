package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.mojang.brigadier.ParseResults;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class PersistentDataPlaceholder extends Placeholder {

    public PersistentDataPlaceholder() {
        super("get_persistent");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String dataKey = dps.values.get("key");

        if (dataKey == null) {
            BanditsQuirkLibForge.LOGGER.error("Command Result placeholder requires 'key' parameter");
            return "";
        }

        try {
            // Get the client player
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                BanditsQuirkLibForge.LOGGER.error("Player is not available");
                return "";
            }

            return minecraft.player.getPersistentData().getString(dataKey);


        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in Command result placeholder" + e.getMessage());
            return "ERROR";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("key");

        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Persistent Data";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Grabs persistent data stored by the library mod. (and the library mod only)",
                "Parameters:",
                "- key: The key of the data"
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
        defaultValues.put("key", "Some.Data.Key");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}
