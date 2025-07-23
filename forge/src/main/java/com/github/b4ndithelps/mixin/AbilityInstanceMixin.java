package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.capabilities.IStaminaData;
import com.github.b4ndithelps.forge.systems.StaminaHelper;
import com.github.b4ndithelps.forge.systems.StaminaProperties;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityConfiguration;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbilityInstance.class, remap = false)
public class AbilityInstanceMixin {

    @Shadow
    private AbilityConfiguration abilityConfiguration;

    @Shadow
    private PropertyManager propertyManager;

    @Inject(method = "tick(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/IPowerHolder;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/threetag/palladium/power/ability/Ability;firstTick(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/ability/AbilityInstance;Lnet/threetag/palladium/power/IPowerHolder;Z)V"))
    private void injectBeforeFirstTick(LivingEntity entity, IPowerHolder powerHolder, CallbackInfo ci) {
        this.customFirstTick(entity, powerHolder);
    }

    private void customFirstTick(LivingEntity entity, IPowerHolder powerHolder) {
        // Get the ability instance
        Ability ability = this.abilityConfiguration.getAbility();

        // Access the STAMINA_COST property and print its value
        try {
            // Replace "YourAbilityClass" with the actual class name that has STAMINA_COST
            java.lang.reflect.Field staminaCostField = ability.getClass().getField("STAMINA_COST");
            PalladiumProperty<Integer> staminaCost = (PalladiumProperty<Integer>) staminaCostField.get(null);

            // Get the actual value - try both property managers
            Integer staminaValue = null;
            if (this.propertyManager.isRegistered(staminaCost)) {
                staminaValue = this.propertyManager.get(staminaCost);
            } else {
                staminaValue = this.abilityConfiguration.get(staminaCost);
            }

            System.out.println("Custom FirstTick - Stamina Cost: " + staminaValue);
        } catch (Exception e) {
            System.out.println("Could not access STAMINA_COST in firstTick: " + e.getMessage());
        }

        System.out.println("Custom firstTick called!");
    }

    private void lastTick() {
//        // Similar property access for lastTick if needed
//        Ability ability = this.abilityConfiguration.getAbility();
//
//        try {
//            java.lang.reflect.Field staminaCostField = ability.getClass().getField("STAMINA_COST");
//            PalladiumProperty<Integer> staminaCost = (PalladiumProperty<Integer>) staminaCostField.get(null);
//            Integer staminaValue = ability.getPropertyManager().getValue(staminaCost);
//            System.out.println("Stamina Cost in lastTick: " + staminaValue);
//        } catch (Exception e) {
//            System.out.println("Could not access STAMINA_COST in lastTick: " + e.getMessage());
//        }

        System.out.println("Custom lastTick called!");
    }

    private void tick() {
//        // Similar property access for tick if needed
//        Ability ability = this.abilityConfiguration.getAbility();
//
//        try {
//            java.lang.reflect.Field staminaCostField = ability.getClass().getField("STAMINA_COST");
//            PalladiumProperty<Integer> staminaCost = (PalladiumProperty<Integer>) staminaCostField.get(null);
//            Integer staminaValue = ability.getPropertyManager().getValue(staminaCost);
//            System.out.println("Stamina Cost in tick: " + staminaValue);
//        } catch (Exception e) {
//            System.out.println("Could not access STAMINA_COST in tick: " + e.getMessage());
//        }

        System.out.println("Custom tick called!");
    }

}
