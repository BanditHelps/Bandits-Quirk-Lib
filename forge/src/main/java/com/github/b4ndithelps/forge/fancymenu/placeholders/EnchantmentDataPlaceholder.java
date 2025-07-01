package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EnchantmentDataPlaceholder extends Placeholder {

    public EnchantmentDataPlaceholder() {
        super("get_enchant_data");
    }

    @SuppressWarnings("removal")
    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String enchantId = dps.values.get("enchant");
        String data = dps.values.get("data");

        if (enchantId == null || data == null) {
            BanditsQuirkLibForge.LOGGER.error("Enchantment placeholder requires 'enchant' and 'data' parameters");
            return "0";
        }

        try {
            ResourceLocation location = new ResourceLocation(enchantId);
            Optional<Enchantment> possibleEnchant = Optional.ofNullable(ForgeRegistries.ENCHANTMENTS.getValue(location));

            if (possibleEnchant.isPresent()) {
                Enchantment enchant = possibleEnchant.get();

                // Depending on the data type, return that information
                switch (data) {
                    case "max_level":
                        return String.valueOf(enchant.getMaxLevel());
                    default:
                        return "INVALID data type!";
                }
            }


            return "ENCHANTMENT_NOT_FOUND";
        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in Scoreboard placeholder" + e.getMessage());
            return "0";
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("enchant");
        values.add("data"); // this one is optional

        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Enchantment Data";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Retrieves information about a specific enchantment.",
                "Parameters:",
                "- enchant: Minecraft id of the enchantment. i.e minecraft:sharpness",
                "- data: What data to receive. Accepts max_level"
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
        defaultValues.put("enchant", "minecraft:sharpness");
        defaultValues.put("data", "max_level");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}
