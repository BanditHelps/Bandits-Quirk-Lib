package com.github.b4ndithelps.forge.conditions;

import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladiumcore.registry.DeferredRegister;
import net.threetag.palladiumcore.registry.RegistrySupplier;

import static com.github.b4ndithelps.BanditsQuirkLib.MOD_ID;

public class CustomConditionSerializers {
    public static final RegistrySupplier<ConditionSerializer> UPGRADE_POINT_BUYABLE;
    public static final DeferredRegister<ConditionSerializer> CUSTOM_SERIALIZERS;

    public CustomConditionSerializers() {

    }

    static {
        CUSTOM_SERIALIZERS = DeferredRegister.create(MOD_ID, ConditionSerializer.REGISTRY);
        UPGRADE_POINT_BUYABLE = CUSTOM_SERIALIZERS.register("upgrade_point_buy", UpgradePointBuyCondition.Serializer::new);
    }
}
