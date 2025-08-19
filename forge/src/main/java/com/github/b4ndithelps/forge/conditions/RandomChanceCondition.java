package com.github.b4ndithelps.forge.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionEnvironment;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

import java.util.Random;

public class RandomChanceCondition extends Condition {

    private final float chance;
    private static final Random random = new Random();

    public RandomChanceCondition(float chance) {
        this.chance = chance;
    }

    @Override
    public boolean active(DataContext dataContext) {
        // Generate a random number between 0.0 and 1.0 and compare it to the chance
        return random.nextFloat() < chance;
    }

    @Override
    public ConditionSerializer getSerializer() {
        return CustomConditionSerializers.RANDOM_CHANCE.get();
    }

    public static class Serializer extends ConditionSerializer {

        public static final PalladiumProperty<String> CHANCE = (new StringProperty("chance"))
                .configurable("Chance that the condition will be true. 0.5 = 50% chance, 0.1 = 10% chance, etc.");

        public Serializer() {
            this.withProperty(CHANCE, "0.5");
        }

        public ConditionEnvironment getContextEnvironment() {
            return ConditionEnvironment.ALL;
        }

        @Override
        public Condition make(JsonObject jsonObject) {
            String chanceStr = this.getProperty(jsonObject, CHANCE);
            float chance;
            try {
                chance = Float.parseFloat(chanceStr);
                // Clamp the chance between 0.0 and 1.0
                chance = Math.max(0.0f, Math.min(1.0f, chance));
            } catch (NumberFormatException e) {
                // Default to 50% chance if parsing fails
                chance = 0.5f;
            }
            return new RandomChanceCondition(chance);
        }

        public String getDocumentationDescription() {
            return "A condition that is true randomly based on the specified chance. Chance should be between 0.0 and 1.0.";
        }
    }
}
