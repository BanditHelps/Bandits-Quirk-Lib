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
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.Objects;

public class BodyCheckCondition extends Condition {

    private final BodyPart part;
    private final DamageStage stage;

    public BodyCheckCondition(BodyPart part, DamageStage stage) {
        this.part = part;
        this.stage = stage;
    }

    @Override
    public boolean active(DataContext dataContext) {
        LivingEntity entity = dataContext.getLivingEntity();

        // Accept both ServerPlayer and LocalPlayer (client-side)
        if (!(entity instanceof net.minecraft.world.entity.player.Player player)) {
            return false;
        }

        // Check if the body part matches the stage
        // This now works on both client and server thanks to synchronization
        return Objects.equals(BodyStatusHelper.getDamageStage(player, part.getName()), stage.getName());
    }

    @Override
    public ConditionSerializer getSerializer() {
        return CustomConditionSerializers.BODY_CHECK.get();
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

        public static final PalladiumProperty<DamageStage> STAGE = (new EnumPalladiumProperty<DamageStage>("damage_state") {


            @Override
            public DamageStage[] getValues() {
                return DamageStage.values();
            }

            @Override
            public String getNameFromEnum(DamageStage damageStage) {
                return damageStage.getName();
            }
        }).configurable("Damage State");

        public Serializer() {
            this.withProperty(PART, BodyPart.HEAD);
            this.withProperty(STAGE, DamageStage.HEALTHY);
        }

        public ConditionEnvironment getContextEnvironment() {
            return ConditionEnvironment.ALL;
        }


        @Override
        public Condition make(JsonObject jsonObject) {
            return new BodyCheckCondition(this.getProperty(jsonObject, PART), this.getProperty(jsonObject, STAGE));
        }

        public String getDocumentationDescription() {
            return "A condition that checks if a specific body part is in a damage stage.";
        }
    }
}
