package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.systems.StaminaProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.PropertyManager;

@Mixin(value = Ability.class, remap = false)
public class AbilityStaminaMixin {

    // Shadow the property manager to access it (don't assign null!)
    @Shadow
    private PropertyManager propertyManager;

    // Add a field to track stamina drain timing per ability instance
    @Unique
    private int bandits_quirk_lib$staminaDrainCounter = 0;

    // Inject into the constructor to add the properties
    @Inject(method = "<init>", at = @At("TAIL"))
    private void addStaminaProperties(CallbackInfo ci) {
        // Add default values for stamina properties using withProperty
        ((Ability)(Object)this).withProperty(StaminaProperties.STAMINA_COST, 0); // 0 means no stamina
        ((Ability)(Object)this).withProperty(StaminaProperties.STAMINA_DRAIN_INTERVAL, 0); // 0 means first tick only
    }

//    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
//    private void onTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled, CallbackInfo ci) {
//        if (!enabled || !(entity instanceof Player player)) return;
//
//        // Check if the ability requires stamina with null checking
//        Integer staminaCostValue = this.propertyManager.get(StaminaProperties.STAMINA_COST);
//        Integer drainIntervalValue = this.propertyManager.get(StaminaProperties.STAMINA_DRAIN_INTERVAL);
//
//        int staminaCost = staminaCostValue != null ? staminaCostValue : 0;
//        int drainInterval = drainIntervalValue != null ? drainIntervalValue : 0;
//
//        if (staminaCost <= 0 || drainInterval == 0) return;
//
//        // Check if we have stamina data
//        if (!StaminaHelper.isStaminaDataAvailable(player)) {
//            return;
//        }
//
//        this.bandits_quirk_lib$staminaDrainCounter++;
//
//        if (this.bandits_quirk_lib$staminaDrainCounter >= drainInterval) {
//            this.bandits_quirk_lib$staminaDrainCounter = 0;
//
//            StaminaHelper.useStamina(player, staminaCost);
//        }
//
//        // Might delete this, because exhaustion system should take care of it
//        IStaminaData staminaData = StaminaHelper.getStaminaDataSafe(player);
//        if (staminaData.isPowersDisabled()) {
//            ci.cancel();
//        }
//    }
//
//    @Inject(method = "firstTick", at = @At("HEAD"), cancellable = true)
//    private void onFirstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled, CallbackInfo ci) {
//
//        System.out.println("AbilityStaminaMixin: Calling First Tick");
//
//        if (!enabled || !(entity instanceof Player player)) {
//            return;
//        }
//
//        System.out.println("AbilityStaminaMixin: Is a player");
//
//
//        // Check if stamina data is available
//        if (!StaminaHelper.isStaminaDataAvailable(player)) {
//            return; // Just exit mixin, let original method continue
//        }
//
//        System.out.println("AbilityStaminaMixin: Data Available");
//
//        // Check if the ability requires stamina with null checking
//        Integer staminaCostValue = this.propertyManager.get(StaminaProperties.STAMINA_COST);
//        Integer drainIntervalValue = this.propertyManager.get(StaminaProperties.STAMINA_DRAIN_INTERVAL);
//
//        System.out.println("AbilityStaminaMixin: Got both properties");
//
//        int staminaCost = staminaCostValue != null ? staminaCostValue : 0;
//        int drainInterval = drainIntervalValue != null ? drainIntervalValue : 0;
//
//        System.out.println("AbilityStaminaMixin: " + staminaCost + " | " + drainInterval);
//
//        if (staminaCost <= 0 || drainInterval != 0) return;
//
//        // Reset counter when ability starts
//        this.bandits_quirk_lib$staminaDrainCounter = 0;
//
//        // Check if powers are disabled - cancel FOR PLAYERS ONLY
//        IStaminaData staminaData = StaminaHelper.getStaminaDataSafe(player);
//        System.out.println("AbilityStaminaMixin: testing if powers disabled");
//        if (staminaData.isPowersDisabled()) {
//            ci.cancel();
//            return;
//        }
//
//        System.out.println("AbilityStaminaMixin: Using stamina");
//        StaminaHelper.useStamina(player, staminaCost);
//        System.out.println("AbilityStaminaMixin: Finished");
//    }

    @Inject(method = "lastTick", at = @At("HEAD"))
    private void onLastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled, CallbackInfo ci) {
        this.bandits_quirk_lib$staminaDrainCounter = 0;
    }
}
