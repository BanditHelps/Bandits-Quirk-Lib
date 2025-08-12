package com.github.b4ndithelps.forge.abilities;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

public class LimitedAirAbility extends Ability {

    public static final PalladiumProperty<Integer> DECREASE_AMOUNT = new IntegerProperty("decrease_amount").configurable("The amount of air that the player loses while the ability is active.");

    public LimitedAirAbility() {
        super();
        this.withProperty(DECREASE_AMOUNT, 6);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;

        int currentAir = player.getAirSupply();
        if (currentAir > -100) {
            player.setAirSupply(currentAir - entry.getProperty(DECREASE_AMOUNT));
        }
    }
}
