package com.github.b4ndithelps.forge.fancymenu.placeholders;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import com.github.b4ndithelps.forge.config.ConfigHelper;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static dev.latvian.mods.rhino.TopLevel.Builtins.Array;

public class DynamicConfigPlaceholder extends Placeholder {

    public DynamicConfigPlaceholder() { super("dyn_config"); }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String configKey = dps.values.get("key");

        if (configKey == null) {
            BanditsQuirkLibForge.LOGGER.error("Dynamic Config Placeholder requires 'key' value");
        }

        try {
            return ConfigHelper.getConfig(configKey).toString();

        } catch (Exception e) {
            BanditsQuirkLibForge.LOGGER.error("Error in DynConfig placeholder" + e.getMessage());
            return "error";
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
        return "Get Dynamic Config";
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(
                "Gets a value from the dynamic configs",
                "Returns the value of the config key as a String, or 'error'",
                "Parameters:",
                "- key: The Key located inside of the dynamic config file"
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
        defaultValues.put("key", "Bql.Example");
        return DeserializedPlaceholderString.build(this.getIdentifier(), defaultValues);
    }
}
