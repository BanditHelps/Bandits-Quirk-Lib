package com.github.b4ndithelps.forge.conditions;

import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.condition.Condition;
import net.threetag.palladium.condition.ConditionEnvironment;
import net.threetag.palladium.condition.ConditionSerializer;
import net.threetag.palladium.util.context.DataContext;

public class SkulkCoreCondition extends Condition {

    @Override
    public boolean active(DataContext dataContext) {
        LivingEntity entity = dataContext.getLivingEntity();

        // Accept both ServerPlayer and LocalPlayer (client-side)
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return false;
        }

        float charge = BodyStatusHelper.getCustomFloat(player, "chest", "skulk_core");

        if (charge == 0) {
            float recharge = BodyStatusHelper.getCustomFloat(player, "chest", "skulk_core_recharge");

            if (recharge == 400) {
                BodyStatusHelper.setCustomFloat(player, "chest", "skulk_core", 1);
                BodyStatusHelper.setCustomFloat(player, "chest", "skulk_core_recharge", 0);
                return true;
            }

            BodyStatusHelper.setCustomFloat(player, "chest", "skulk_core_recharge", ++recharge);
            return false;
        }
        return true;
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
            return new SkulkCoreCondition();
        }

        public String getDocumentationDescription() {
            return "A condition that checks for skulk core.";
        }
    }
}
