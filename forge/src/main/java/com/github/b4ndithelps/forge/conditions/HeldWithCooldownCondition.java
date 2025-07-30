package com.github.b4ndithelps.forge.conditions;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.condition.*;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.Power;
import net.threetag.palladium.power.ability.AbilityConfiguration;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.context.DataContext;
import net.threetag.palladium.util.context.DataContextType;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

public class HeldWithCooldownCondition extends KeyCondition {
    public HeldWithCooldownCondition(int cooldown, AbilityConfiguration.KeyType type, boolean needsEmptyHand) {
        super(cooldown, type, needsEmptyHand, true);
    }

    public boolean active(DataContext context) {
        Entity entity = (Entity)context.get(DataContextType.ENTITY);
        AbilityInstance entry = (AbilityInstance)context.get(DataContextType.ABILITY);
        if (entity != null && entry != null) {
            return entry.keyPressed;
        } else {
            return false;
        }
    }

    public void onKeyPressed(LivingEntity entity, AbilityInstance entry, Power power, IPowerHolder holder) {
        if (entry.cooldown == 0) {
            entry.keyPressed = true;
        }
    }

    public void onKeyReleased(LivingEntity entity, AbilityInstance entry, Power power, IPowerHolder holder) {
        entry.keyPressed = false;
        if (entry.cooldown == 0 && this.cooldown != 0) {
            entry.startCooldown(entity, this.cooldown);
        }
    }

    public AbilityConfiguration.KeyPressType getKeyPressType() {
        return AbilityConfiguration.KeyPressType.HOLD;
    }

    public ConditionSerializer getSerializer() {
        return (ConditionSerializer) CustomConditionSerializers.HELD_WITH_COOLDOWN.get();
    }

    public static class Serializer extends ConditionSerializer {
        public static final PalladiumProperty<Integer> COOLDOWN = (new IntegerProperty("cooldown")).configurable("Amount of ticks the ability can be used for");

        public Serializer() {
            this.withProperty(COOLDOWN, 0);
            this.withProperty(KeyCondition.KEY_TYPE_WITHOUT_SCROLLING, AbilityConfiguration.KeyType.KEY_BIND);
            this.withProperty(KeyCondition.NEEDS_EMPTY_HAND, false);
        }

        public Condition make(JsonObject json) {
            return new HeldWithCooldownCondition((Integer) this.getProperty(json, COOLDOWN), (AbilityConfiguration.KeyType) this.getProperty(json, KeyCondition.KEY_TYPE_WITHOUT_SCROLLING), (Boolean) this.getProperty(json, KeyCondition.NEEDS_EMPTY_HAND));
        }

        public ConditionEnvironment getContextEnvironment() {
            return ConditionEnvironment.DATA;
        }

        public String getDocumentationDescription() {
            return "Allows the ability to be used while holding a key bind. When released, start a cooldown to prevent reactivation.";
        }
    }
}
