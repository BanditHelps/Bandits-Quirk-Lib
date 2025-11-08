package com.github.b4ndithelps.forge.conditions;

import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladiumcore.registry.DeferredRegister;
import net.threetag.palladiumcore.registry.RegistrySupplier;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

public class CustomConditionSerializers {
    public static final RegistrySupplier<ConditionSerializer> UPGRADE_POINT_BUYABLE;
    public static final RegistrySupplier<ConditionSerializer> BODY_CHECK;
    public static final RegistrySupplier<ConditionSerializer> POWERS_ENABLED;
    public static final RegistrySupplier<ConditionSerializer> HELD_WITH_COOLDOWN;
    public static final RegistrySupplier<ConditionSerializer> BODY_FLOAT_CHECK;
    public static final RegistrySupplier<ConditionSerializer> RANDOM_CHANCE;
    public static final RegistrySupplier<ConditionSerializer> GENOME_HAS_GENE;


    public static final DeferredRegister<ConditionSerializer> CUSTOM_SERIALIZERS;

    public CustomConditionSerializers() {

    }

    static {
        CUSTOM_SERIALIZERS = DeferredRegister.create(MOD_ID, ConditionSerializer.REGISTRY);
        UPGRADE_POINT_BUYABLE = CUSTOM_SERIALIZERS.register("upgrade_point_buy", UpgradePointBuyCondition.Serializer::new);
        BODY_CHECK = CUSTOM_SERIALIZERS.register("body_damage", BodyCheckCondition.Serializer::new);
        POWERS_ENABLED = CUSTOM_SERIALIZERS.register("powers_enabled", PowersEnabledCondition.Serializer::new);
        HELD_WITH_COOLDOWN = CUSTOM_SERIALIZERS.register("held_with_cooldown", HeldWithCooldownCondition.Serializer::new);
        BODY_FLOAT_CHECK = CUSTOM_SERIALIZERS.register("body_float_check", BodyFloatCondition.Serializer::new);
        RANDOM_CHANCE = CUSTOM_SERIALIZERS.register("random_chance", RandomChanceCondition.Serializer::new);
        GENOME_HAS_GENE = CUSTOM_SERIALIZERS.register("genome_has_gene", GenomeHasGeneCondition.Serializer::new);
    }
}
