package com.github.b4ndithelps.forge.conditions;

import com.github.b4ndithelps.forge.capabilities.Body.BodyPart;
import com.github.b4ndithelps.forge.capabilities.Body.DamageStage;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionEnvironment;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.EnumPalladiumProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

public class BodyFloatCondition extends Condition {

    private final BodyPart part;
    private final String key;
    private final float value;

    public BodyFloatCondition(BodyPart part, String key, Float value) {
        this.part = part;
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean active(DataContext dataContext) {
        LivingEntity entity = dataContext.getLivingEntity();

        if (!(entity instanceof ServerPlayer player)) return false;

        float storedValue = BodyStatusHelper.getCustomFloat(player, part.getName(), key);

        return storedValue == value;
    }

    @Override
    public ConditionSerializer getSerializer() {
        return CustomConditionSerializers.BODY_FLOAT_CHECK.get();
    }

    public static class Serializer extends ConditionSerializer {

        public static final PalladiumProperty<BodyPart> PART = (new EnumPalladiumProperty<BodyPart>("body_part") {
            @Override
            public BodyPart[] getValues() {
                return BodyPart.values();
            }

            @Override
            public String getNameFromEnum(BodyPart bodyPart) {
                return bodyPart.getName();
            }
        }).configurable("The body part to check");

        public static final PalladiumProperty<String> KEY = (new StringProperty("key")).configurable("The key that corresponds to the value you want retrieved");
        public static final PalladiumProperty<Float> VALUE = (new FloatProperty("value")).configurable("The value you want to see the equivalence too.");

        public Serializer() {
            this.withProperty(PART, BodyPart.HEAD);
            this.withProperty(KEY, "value_name");
            this.withProperty(VALUE, 0.0f);
        }

        public ConditionEnvironment getContextEnvironment() {
            return ConditionEnvironment.ALL;
        }


        @Override
        public Condition make(JsonObject jsonObject) {
            return new BodyFloatCondition(this.getProperty(jsonObject, PART), this.getProperty(jsonObject, KEY), this.getProperty(jsonObject, VALUE));
        }

        public String getDocumentationDescription() {
            return "A condition to check if a stored float for a value in the body system matches a certain value.";
        }
    }
}
