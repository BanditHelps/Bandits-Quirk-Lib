package com.github.b4ndithelps.forge.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.context.DataContextType;

public class HoldingShiftCondition extends Condition{
    public HoldingShiftCondition() {
    }

    public boolean active(DataContext context) {
        Entity entity = (Entity)context.get(DataContextType.ENTITY);
        return entity != null && entity.isShiftKeyDown();
    }

    public ConditionSerializer getSerializer() {
        return (ConditionSerializer) CustomConditionSerializers.HOLDING_SHIFT.get();
    }

    public static class Serializer extends ConditionSerializer {
        public Serializer() {
        }

        public Condition make(JsonObject json) {
            return new HoldingShiftCondition();
        }

        public String getDocumentationDescription() {
            return "Checks if the entity is holding the shift key. Useful for flight related shift keybinds";
        }
    }
}