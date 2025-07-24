package com.github.b4ndithelps.forge.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionEnvironment;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;

public class PowersEnabledCondition extends Condition {

    public PowersEnabledCondition() {}

    @Override
    public boolean active(DataContext dataContext) {
        LivingEntity entity = dataContext.getLivingEntity();
        return !entity.getTags().contains("MineHa.PowersDisabled");
    }

    @Override
    public ConditionSerializer getSerializer() {
        return CustomConditionSerializers.POWERS_ENABLED.get();
    }

    public static class Serializer extends ConditionSerializer {
        public Serializer() {
        }

        public ConditionEnvironment getContextEnvironment() {
            return ConditionEnvironment.ALL;
        }

        @Override
        public Condition make(JsonObject jsonObject) {
            return new PowersEnabledCondition();
        }

        public String getDocumentationDescription() {
            return "A condition that checks if the player has the \"MineHa.PowersDisabled\" tag. If they do, returns false. Otherwise returns true";
        }
    }
}