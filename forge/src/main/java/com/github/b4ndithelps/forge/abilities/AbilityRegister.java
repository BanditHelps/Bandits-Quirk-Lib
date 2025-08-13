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
    public static final RegistrySupplier<Ability> GRAB_ABILITY;
    public static final RegistrySupplier<Ability> BODY_STATUS_MODIFIER;
    public static final RegistrySupplier<Ability> WIND_PROJECTILE;
    public static final RegistrySupplier<Ability> WIND_WALL_SMASH;
    public static final RegistrySupplier<Ability> PERMEATION;
    public static final RegistrySupplier<Ability> PERMEATION_RISE;
    public static final RegistrySupplier<Ability> QUIRK_FACTOR_SCALING_ATTRIBUTE;
    public static final RegistrySupplier<Ability> LIMITED_AIR_SUPPLY;


    public AbilityRegister() {

    }

    public static void init() {

    }

    static {
        ABILITIES = DeferredRegister.create(MOD_ID, Ability.REGISTRY);
        HAPPEN_ONCE = ABILITIES.register("happen_once", HappenOnceAbility::new);
        ROT_ABILITY = ABILITIES.register("rot_ability", RotAbility::new);
        ENVIRONMENT_DECAY = ABILITIES.register("environment_decay", EnvironmentDecayAbility::new);
        GRAB_ABILITY = ABILITIES.register("grab_ability", GrabAbility::new);
        BODY_STATUS_MODIFIER = ABILITIES.register("body_status_modifier", BodyStatusModifierAbility::new);
        WIND_PROJECTILE = ABILITIES.register("wind_projectile", WindProjectileAbility::new);
        WIND_WALL_SMASH = ABILITIES.register("wind_wall_smash", WindWallSmashAbility::new);
        PERMEATION = ABILITIES.register("permeation", PermeationAbility::new);
        PERMEATION_RISE = ABILITIES.register("permeation_rise", PermeationRiseAbility::new);
        QUIRK_FACTOR_SCALING_ATTRIBUTE = ABILITIES.register("quirk_factor_scaling_attribute", QuirkFactorScalingAttributeAbility::new);
        LIMITED_AIR_SUPPLY = ABILITIES.register("limited_air_supply", LimitedAirAbility::new);
    }
}
