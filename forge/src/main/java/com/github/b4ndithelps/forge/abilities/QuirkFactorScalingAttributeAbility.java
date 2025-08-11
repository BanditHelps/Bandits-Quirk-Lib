package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.power.ability.AttributeModifierAbility;

import java.util.UUID;

public class QuirkFactorScalingAttributeAbility extends AttributeModifierAbility {

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (enabled) {
            Attribute attribute = entry.getProperty(ATTRIBUTE);
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance == null || entity.level().isClientSide) {
                return;
            }

            // Base amount from config
            double baseAmount = entry.getProperty(AMOUNT);

            // Scale with quirk factor: base + base * quirkFactor
            double scaledAmount = baseAmount;
            if (entity instanceof ServerPlayer player) {
                double quirkFactor = QuirkFactorHelper.getQuirkFactor(player);
                scaledAmount = baseAmount + (baseAmount * quirkFactor);
            }

            UUID uuid = entry.getProperty(UUID);
            AttributeModifier modifier = instance.getModifier(uuid);
            int operationValue = entry.getProperty(OPERATION);

            if (modifier != null && (modifier.getAmount() != scaledAmount || modifier.getOperation().toValue() != operationValue)) {
                instance.removeModifier(uuid);
                modifier = null;
            }

            if (modifier == null) {
                modifier = new AttributeModifier(
                        uuid,
                        entry.getConfiguration().getDisplayName().getString(),
                        scaledAmount,
                        Operation.fromValue(operationValue)
                );
                instance.addTransientModifier(modifier);
            }
        } else {
            this.lastTick(entity, entry, holder, false);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Adds an attribute modifier to the entity while enabled, scaling the configured amount by the player's quirk factor (amount = base + base * quirkFactor).";
    }
}
