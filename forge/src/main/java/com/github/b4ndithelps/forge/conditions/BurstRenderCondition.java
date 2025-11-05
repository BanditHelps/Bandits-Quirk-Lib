package com.github.b4ndithelps.forge.conditions;

import com.github.b4ndithelps.forge.capabilities.Body.BodyPart;
import com.github.b4ndithelps.forge.capabilities.Body.DamageStage;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionEnvironment;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.property.EnumPalladiumProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.PropertyManager;
import net.threetag.palladium.util.property.StringProperty;

import java.util.Map;
import java.util.Objects;

public class BurstRenderCondition extends Condition {

    @Override
    public boolean active(DataContext dataContext) {
        LivingEntity entity = dataContext.getLivingEntity();

        // Accept both ServerPlayer and LocalPlayer (client-side)
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return false;
        }

        float charge = BodyStatusHelper.getCustomFloat(player, "chest", "super_burst_render");

        if (charge <= 30) {
            BodyStatusHelper.setCustomFloat(player, "chest", "super_burst_render", ++charge);
            return true;
        }
        return false;
    }

    @Override
    public ConditionSerializer getSerializer() {
        return CustomConditionSerializers.BODY_CHECK.get();
    }

    public static class Serializer extends ConditionSerializer {

        public ConditionEnvironment getContextEnvironment() {
            return ConditionEnvironment.ALL;
        }


        @Override
        public Condition make(JsonObject jsonObject) {
            return new BurstRenderCondition();
        }

        public String getDocumentationDescription() {
            return "A condition that checks if a specific render property is more than 0 and reduces it.";
        }
    }
}
