package com.github.b4ndithelps.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
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

import java.util.Map;

@Mixin(value = AbilityInstance.class, remap = false)
public abstract class AbilityInstanceMixin {

    @Shadow
    private AbilityConfiguration abilityConfiguration;

    @Shadow public abstract Object getPropertyByName(String key);

    @Shadow public abstract <T> T getProperty(PalladiumProperty<T> property);

    @Inject(method = "tick(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/IPowerHolder;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/threetag/palladium/power/ability/AbilityConfiguration;getAbility()Lnet/threetag/palladium/power/ability/Ability;",
                    ordinal = 1))
    private void injectBeforeFirstTick(LivingEntity entity, IPowerHolder powerHolder, CallbackInfo ci) {
        this.bandits_quirk_lib$customFirstTick(entity, powerHolder);
    }

    // Redirect completely
//    @Redirect(method = "tick(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/IPowerHolder;)V",
//                at = @At(value = "INVOKE",
//                target = "Lnet/threetag/palladium/power/ability/Ability;firstTick(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/ability/AbilityInstance;Lnet/threetag/palladium/power/IPowerHolder;Z)V"))
//    private void redirectFirstTick(Ability ability, LivingEntity entity, AbilityInstance instance, IPowerHolder powerHolder, boolean isEnabled) {
//        customFirstTick(entity, powerHolder);
//        ability.firstTick(entity, instance, powerHolder, isEnabled);
//    }

    @Unique
    private void bandits_quirk_lib$customFirstTick(LivingEntity entity, IPowerHolder powerHolder) {

        System.out.println("AbilityInstance: First Tick");

        PropertyManager manager = abilityConfiguration.getPropertyManager();

        // Access the STAMINA_COST property and print its value
        try {
            // Replace "YourAbilityClass" with the actual class name that has STAMINA_COST
//            java.lang.reflect.Field staminaCostField = ability.getClass().getField("STAMINA_COST");
//            PalladiumProperty<Integer> staminaCost = (PalladiumProperty<Integer>) staminaCostField.get(null);

            System.out.println("AbilityInstance: Attempting to acquire stamina_cost");

            if (manager == null) {
                System.out.println("AbilityInstance: null");
            } else {
//                Integer staminaCost = (Integer) getPropertyByName("stamina_cost");
//                PalladiumProperty<?> prop = manager.getPropertyByName("stamina_cost");
//                Integer staminaCost = prop.
//
                Map<PalladiumProperty<?>, Object> map = manager.values();

                PalladiumProperty<?> plsWork = manager.getPropertyByName("stamina_cost");
                Integer staminaCost = (Integer) map.get(plsWork);
                System.out.println("Stamina Cost is: " + staminaCost);

                for (Map.Entry<PalladiumProperty<?>, Object> entry : map.entrySet()) {
                    System.out.println(entry.getKey().getKey() + " = " + entry.getValue());
                }

                System.out.println("AbilityInstance: Acquisition successful");

                // Get the actual value - try both property managers
//            Integer staminaValue = null;
//            if (this.propertyManager.isRegistered(staminaCost)) {
//                staminaValue = this.propertyManager.get(staminaCost);
//            } else {
//                staminaValue = this.abilityConfiguration.get(staminaCost);
//            }

                System.out.println("AbilityInstance: Stamina Cost: " + staminaCost);
            }


        } catch (Exception e) {
            System.out.println("Could not access STAMINA_COST in firstTick: " + e.getMessage());
        }
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
