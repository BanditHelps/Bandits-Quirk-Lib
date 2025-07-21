package com.github.b4ndithelps.forge.abilities;

import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladiumcore.registry.DeferredRegister;
import net.threetag.palladiumcore.registry.RegistrySupplier;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

public class AbilityRegister {
    public static final DeferredRegister<Ability> ABILITIES;
    public static final RegistrySupplier<Ability> HAPPEN_ONCE;
    public static final RegistrySupplier<Ability> ROT_ABILITY;
    public static final RegistrySupplier<Ability> ENVIRONMENT_DECAY;

    public AbilityRegister() {

    }

    public static void init() {

    }

    static {
        ABILITIES = DeferredRegister.create(MOD_ID, Ability.REGISTRY);
        HAPPEN_ONCE = ABILITIES.register("happen_once", HappenOnceAbility::new);
        ROT_ABILITY = ABILITIES.register("rot_ability", RotAbility::new);
        ENVIRONMENT_DECAY = ABILITIES.register("environment_decay", EnvironmentDecayAbility::new);
    }
}
