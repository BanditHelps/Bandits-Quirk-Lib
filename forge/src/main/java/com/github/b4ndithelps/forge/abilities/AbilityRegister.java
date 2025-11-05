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
    public static final RegistrySupplier<Ability> REGENERATION;
    public static final RegistrySupplier<Ability> DETROIT_SMASH;
    public static final RegistrySupplier<Ability> ENHANCED_PUNCH;
    public static final RegistrySupplier<Ability> ENHANCED_KICK;
    public static final RegistrySupplier<Ability> CHARGED_PUNCH;
    public static final RegistrySupplier<Ability> SCALING_GENE_DURATION_EFFECT;
    public static final RegistrySupplier<Ability> SCALING_GENE_AMP_EFFECT;
    public static final RegistrySupplier<Ability> MINOR_OBJECT_ATTRACTION;
    public static final RegistrySupplier<Ability> ZOOM;
    public static final RegistrySupplier<Ability> TWIN_IMPACT_MARK;
    public static final RegistrySupplier<Ability> TWIN_IMPACT_TRIGGER;
    public static final RegistrySupplier<Ability> LARCENY;
    public static final RegistrySupplier<Ability> GENE_QUALITY_SCALING_ATTRIBUTE;
    public static final RegistrySupplier<Ability> DASHSTEP;
	public static final RegistrySupplier<Ability> BLACKWHIP_RESTRAIN;
	public static final RegistrySupplier<Ability> BLACKWHIP_TAG;
	public static final RegistrySupplier<Ability> BLACKWHIP_RESTRAIN_TAGGED;
	public static final RegistrySupplier<Ability> BLACKWHIP_LIFT;
	public static final RegistrySupplier<Ability> BLACKWHIP_SLAM;
	public static final RegistrySupplier<Ability> BLACKWHIP_LASH;
	public static final RegistrySupplier<Ability> BLACKWHIP_ZIP;
	public static final RegistrySupplier<Ability> BLACKWHIP_PUPPET;
	public static final RegistrySupplier<Ability> BLACKWHIP_DETACH;
	public static final RegistrySupplier<Ability> BLACKWHIP_AUTO_REFRESH;
    public static final RegistrySupplier<Ability> BLACKWHIP_AURA;


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
        REGENERATION = ABILITIES.register("regeneration", RegenerationAbility::new);
        DETROIT_SMASH = ABILITIES.register("detroit_smash", DetroitSmashAbility::new);
        ENHANCED_PUNCH = ABILITIES.register("enhanced_punch", EnhancedPunchAbility::new);
        ENHANCED_KICK = ABILITIES.register("enhanced_kick", EnhancedKickAbility::new);
        CHARGED_PUNCH = ABILITIES.register("charged_punch", ChargedPunchAbility::new);
        SCALING_GENE_DURATION_EFFECT = ABILITIES.register("scaling_gene_duration_effect", ScalingGeneDurationEffect::new);
        SCALING_GENE_AMP_EFFECT = ABILITIES.register("scaling_gene_amp_effect", ScalingGeneAmpEffect::new);
        MINOR_OBJECT_ATTRACTION = ABILITIES.register("minor_object_attraction", MinorObjectAttractionAbility::new);
        ZOOM = ABILITIES.register("zoom", ZoomAbility::new);
        TWIN_IMPACT_MARK = ABILITIES.register("twin_impact_mark", TwinImpactMarkAbility::new);
        TWIN_IMPACT_TRIGGER = ABILITIES.register("twin_impact_trigger", TwinImpactTriggerAbility::new);
        LARCENY = ABILITIES.register("larceny", LarcenyAbility::new);
        GENE_QUALITY_SCALING_ATTRIBUTE = ABILITIES.register("gene_quality_scaling_attribute", GeneQualityScalingAttributeAbility::new);
        DASHSTEP = ABILITIES.register("dashstep", DashstepAbility::new);
		BLACKWHIP_RESTRAIN = ABILITIES.register("blackwhip_restrain", BlackwhipRestrainAbility::new);
		BLACKWHIP_TAG = ABILITIES.register("blackwhip_tag", BlackwhipTagAbility::new);
		BLACKWHIP_RESTRAIN_TAGGED = ABILITIES.register("blackwhip_restrain_tagged", BlackwhipRestrainTaggedAbility::new);
		BLACKWHIP_LIFT = ABILITIES.register("blackwhip_lift", BlackwhipLiftAbility::new);
		BLACKWHIP_SLAM = ABILITIES.register("blackwhip_slam", BlackwhipSlamAbility::new);
		BLACKWHIP_LASH = ABILITIES.register("blackwhip_lash", BlackwhipLashAbility::new);
		BLACKWHIP_ZIP = ABILITIES.register("blackwhip_zip", BlackwhipZipAbility::new);
		BLACKWHIP_PUPPET = ABILITIES.register("blackwhip_puppet", BlackwhipPuppetAbility::new);
		BLACKWHIP_DETACH = ABILITIES.register("blackwhip_detach", BlackwhipDetachAbility::new);
		BLACKWHIP_AUTO_REFRESH = ABILITIES.register("blackwhip_auto_refresh", BlackwhipAutoRefreshAbility::new);
        BLACKWHIP_AURA = ABILITIES.register("blackwhip_aura", BlackwhipAuraAbility::new);

    }
}
