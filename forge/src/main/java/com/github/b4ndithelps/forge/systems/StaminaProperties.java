package com.github.b4ndithelps.forge.systems;

import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

// This class is used to store the new Palladium Properties that I will be injecting into the palladium ability class
public class StaminaProperties {
    public static final PalladiumProperty<Integer> STAMINA_COST =
            (new IntegerProperty("stamina_cost")).configurable("Determines the stamina cost for using this ability");

    public static final PalladiumProperty<Integer> STAMINA_DRAIN_INTERVAL =
            (new IntegerProperty("stamina_interval")).configurable("How often (in ticks) stamina is drained. Default is every tick. 0 Means first tick only");

} 