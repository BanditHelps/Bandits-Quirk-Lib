package com.github.b4ndithelps.mixin;

import net.threetag.palladium.util.property.PalladiumProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.threetag.palladium.power.ability.Ability;

import static com.github.b4ndithelps.forge.systems.StaminaProperties.*;

@Mixin(value = Ability.class, remap = false)
public abstract class AbilityStaminaMixin {

    @Shadow public abstract <T> Ability withProperty(PalladiumProperty<T> data, T value);

    // Inject into the constructor to add the properties
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addStaminaProperties(CallbackInfo ci) {
        withProperty(STAMINA_FIRST_TICK_COST, 0);
        withProperty(STAMINA_INTERVAL_COST, 0);
        withProperty(STAMINA_TICK_INTERVAL, 20);
    }
}
