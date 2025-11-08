package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class NBTPlaceholder extends Placeholder {

    public NBTPlaceholder() {
        super("get_nbt");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String key = dps.values.get("key");
        String type = dps.values.get("type");


        if (key == null || type == null) {
            BanditsQuirkLibForge.LOGGER.error("NBT placeholder requires 'key' and 'type' parameters");
            return "Error";
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

            if (mc.player == null) {
                return "Error";
            }

            ServerPlayer player = server.getPlayerList().getPlayerByName(mc.player.getGameProfile().getName());

            if (level == null || player == null) {
                return "Error";
            }

            CompoundTag persistentData = player.getPersistentData();

            BanditsQuirkLibForge.LOGGER.info("Checking Player: '{}'", persistentData.getAllKeys());

            return switch (type) {
                case "string" -> persistentData.getString(key);
                case "boolean" -> persistentData.getBoolean(key) ? "true" : "false";
                case "int" -> String.valueOf(persistentData.getInt(key));
                default -> {
                    BanditsQuirkLibForge.LOGGER.error("Invalid type selected for NBT Placeholder!");
                    yield "Error";
                }
            };
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in NBT placeholder" + e.getMessage());
            return "Error";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("key");
        values.add("type");

        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "NBT Value";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Retrieves a specific NBT Tag value as a String. Doesn't handle arrays",
                "Parameters:",
                "- key: Key of the NBT data to retrieve",
                "- type: Type of data to return as a string (string | int | boolean)"
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
        defaultValues.put("key", "example_key");
        defaultValues.put("type", "string");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}