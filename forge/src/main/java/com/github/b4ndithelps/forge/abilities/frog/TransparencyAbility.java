package com.github.b4ndithelps.forge.abilities.frog;

import com.github.b4ndithelps.forge.utils.TransparencyManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

public class TransparencyAbility extends Ability {
    public static final PalladiumProperty<Float> VALUE = new FloatProperty("value").configurable("Value of transparency from 0.0 (invisible) to 1.0 (fully visible)");

    public TransparencyAbility() {
        super();
        this.withProperty(VALUE, 0.5F);
    }

    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled || !(entity instanceof ServerPlayer player)) { return;}
        TransparencyManager.addTransparency(player.getUUID(), entry.getProperty(VALUE));

    }

    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled || !(entity instanceof ServerPlayer player)) { return;}
        TransparencyManager.removeTransparency(player.getUUID(), entry.getProperty(VALUE));

    }
}
