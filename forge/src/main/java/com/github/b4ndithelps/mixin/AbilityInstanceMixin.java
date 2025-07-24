package com.github.b4ndithelps.mixin;

import com.github.b4ndithelps.forge.systems.StaminaHelper;
import net.minecraft.server.level.ServerPlayer;
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

import static com.github.b4ndithelps.forge.systems.StaminaProperties.STAMINA_COST;
import static com.github.b4ndithelps.forge.systems.StaminaProperties.STAMINA_DRAIN_INTERVAL;

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

    @Unique
    private void bandits_quirk_lib$customFirstTick(LivingEntity entity, IPowerHolder powerHolder) {
        PropertyManager manager = abilityConfiguration.getPropertyManager();

        // Do the stamina logic. If stamina_drain_type = 0, then we sub the stamina once from the player
        try {
            if (manager == null || !(entity instanceof ServerPlayer player)) {
                System.out.println("AbilityInstance: null");
                return;
            }

            Integer staminaDrain = (Integer) manager.get(STAMINA_DRAIN_INTERVAL);

            if (staminaDrain == null || staminaDrain != 0) return;

            Integer staminaCost = (Integer) manager.get(STAMINA_COST);

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

    private void lastTick() {
        System.out.println("Custom lastTick called!");
    }

    private void bandits_quirk_lib$customTick(LivingEntity entity, IPowerHolder powerHolder) {
        PropertyManager manager = abilityConfiguration.getPropertyManager();

        // Do the stamina logic. If stamina_drain_type != 0, then count the stamina every tick
        try {
            if (manager == null || !(entity instanceof ServerPlayer player)) {
                System.out.println("AbilityInstance: null");
                return;
            }

            Integer staminaDrain = (Integer) manager.get(STAMINA_DRAIN_INTERVAL);

            if (staminaDrain == null || staminaDrain != 0) return;

            Integer staminaCost = (Integer) manager.get(STAMINA_COST);

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

}
