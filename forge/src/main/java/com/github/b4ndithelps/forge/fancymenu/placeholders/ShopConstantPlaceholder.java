package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.config.ConfigHelper;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ShopConstantPlaceholder extends Placeholder {

    public ShopConstantPlaceholder() { super("shop_constant"); }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String key = dps.values.get("key");
        String type = dps.values.get("type");

        if (key == null || type == null) {
            BanditsQuirkLibForge.LOGGER.error("Shop Constant Placeholder requires 'key' and 'type' values");
        }

        try {

            return switch (type) {
                case "item_learn" -> String.valueOf(ConfigHelper.getItemLearnCost(key));
                case "enchant_learn" -> String.valueOf(ConfigHelper.getEnchantLearnCost(key));
                case "item_buy" -> String.valueOf(ConfigHelper.getItemBuyCost(key));
                case "enchant_buy" -> String.valueOf(ConfigHelper.getEnchantBuyCost(key));
                default -> "error";
            };

        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in ShopConstant placeholder" + e.getMessage());
            return "error";
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
        return "Get Shop value from config";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Gets a value from the shop constants",
                "Returns the value of the as a String, or 'error'",
                "Parameters:",
                "- key: The minecraft item key",
                "- type: 'item_buy', 'item_learn', 'enchant_buy', 'enchant_learn'"
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
        defaultValues.put("key", "minecraft:coal");
        defaultValues.put("type",  "item_learn");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}