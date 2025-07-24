package com.github.b4ndithelps.forge.systems;

import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

// This class is used to store the new Palladium Properties that I will be injecting into the palladium ability class
public class StaminaProperties {

    public static final PalladiumProperty<Integer> STAMINA_FIRST_TICK_COST =
            (new IntegerProperty("stamina_first_cost")).configurable("Determines the stamina cost ran the first tick the power is activated");

    public static final PalladiumProperty<Integer> STAMINA_INTERVAL_COST =
            (new IntegerProperty("stamina_interval_cost")).configurable("Determines the stamina cost every interval defined by \"stamina_interval\"");

    public static final PalladiumProperty<Integer> STAMINA_TICK_INTERVAL =
            (new IntegerProperty("stamina_interval")).configurable("How often (in ticks) stamina is drained. Default is every tick. Uses \"stamina_interval_cost\"");

} 