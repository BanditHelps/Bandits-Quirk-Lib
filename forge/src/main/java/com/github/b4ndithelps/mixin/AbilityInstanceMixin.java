package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityConfiguration;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.PropertyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.github.b4ndithelps.forge.systems.StaminaProperties.*;

@Mixin(value = AbilityInstance.class, remap = false)
public abstract class AbilityInstanceMixin {

    @Shadow
    private AbilityConfiguration abilityConfiguration;

    @Shadow
    private boolean enabled;

    // Tick counter to track intervals for stamina draining
    @Unique
    private int bandits_quirk_lib$tickCounter = 0;

    @Inject(method = "tick(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/IPowerHolder;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/threetag/palladium/power/ability/AbilityConfiguration;getAbility()Lnet/threetag/palladium/power/ability/Ability;",
                    ordinal = 0))
    private void injectBeforeFirstTick(LivingEntity entity, IPowerHolder powerHolder, CallbackInfo ci) {
        this.bandits_quirk_lib$customFirstTick(entity, powerHolder);
    }

    @Inject(method = "tick(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/IPowerHolder;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/threetag/palladium/power/ability/AbilityConfiguration;getAbility()Lnet/threetag/palladium/power/ability/Ability;",
                    ordinal = 1))
    private void injectLastTick(LivingEntity entity, IPowerHolder powerHolder, CallbackInfo ci) {
        this.bandits_quirk_lib$lastTick(entity, powerHolder);
    }

    @Inject(method = "tick(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/IPowerHolder;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/threetag/palladium/power/ability/AbilityConfiguration;getAbility()Lnet/threetag/palladium/power/ability/Ability;",
                    ordinal = 2))
    private void injectCustomTick(LivingEntity entity, IPowerHolder powerHolder, CallbackInfo ci) {
        this.bandits_quirk_lib$customTick(entity, powerHolder);
    }

    @Unique
    private void bandits_quirk_lib$customFirstTick(LivingEntity entity, IPowerHolder powerHolder) {
        if (!enabled || !(entity instanceof ServerPlayer player)) return;

        PropertyManager manager = abilityConfiguration.getPropertyManager();

        // Do the stamina logic. If stamina_drain_interval = 0, then we sub the stamina once from the player
        try {
            if (manager == null) {
                System.out.println("AbilityInstance: null");
                return;
            }

            Integer staminaCost = manager.get(STAMINA_FIRST_TICK_COST);

            // Use the appropriate stamina value
            if (staminaCost != null) {
                StaminaHelper.useStamina(player, staminaCost);
            } else {
                System.out.println("Stamina Cost is null fr");
            }
        } catch (Exception e) {
            System.out.println("Could not access STAMINA_COST in firstTick: " + e.getMessage());
        }
    }

    @Unique
    private void bandits_quirk_lib$lastTick(LivingEntity entity, IPowerHolder powerHolder) {
        // Reset the tick counter when the ability stops
        this.bandits_quirk_lib$tickCounter = 0;
    }

    @Unique
    private void bandits_quirk_lib$customTick(LivingEntity entity, IPowerHolder powerHolder) {
        if (!enabled || !(entity instanceof ServerPlayer player)) return;

        PropertyManager manager = abilityConfiguration.getPropertyManager();

        // Do the stamina logic. If stamina_drain_interval > 0, then drain stamina every N ticks
        try {
            if (manager == null ) {
                return;
            }

            Integer staminaDrainInterval = manager.get(STAMINA_TICK_INTERVAL);
            Integer staminaCost = manager.get(STAMINA_INTERVAL_COST);

            // Only proceed if interval > 0 (interval-based draining)
            if (staminaDrainInterval == null || staminaDrainInterval <= 0 || staminaCost == null || staminaCost == 0) {
                return;
            }

            // Increment the tick counter
            this.bandits_quirk_lib$tickCounter++;

            // Check if enough ticks have passed to drain stamina
            if (this.bandits_quirk_lib$tickCounter >= staminaDrainInterval) {
                // Use the appropriate stamina value
                StaminaHelper.useStamina(player, staminaCost);
                
                // Reset the counter for the next interval
                this.bandits_quirk_lib$tickCounter = 0;
            }

        } catch (Exception e) {
            System.out.println("Could not access STAMINA_COST in customTick: " + e.getMessage());
        }
    }

}
