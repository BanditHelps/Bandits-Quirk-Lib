package com.github.b4ndithelps.forge.abilities.blackwhip;

import com.github.b4ndithelps.forge.systems.BlackwhipTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * Lightweight background ability to periodically refresh Blackwhip tag state.
 * Runs server-side every {@code interval_ticks} (default 20 = 1s).
 */
@SuppressWarnings("removal")
public class BlackwhipAutoRefreshAbility extends Ability {

    public static final PalladiumProperty<Integer> INTERVAL_TICKS = new IntegerProperty("interval_ticks")
            .configurable("How often to refresh Blackwhip tags (in ticks)");

    public BlackwhipAutoRefreshAbility() {
        super();
        this.withProperty(INTERVAL_TICKS, 20);
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        int interval = Math.max(1, entry.getProperty(INTERVAL_TICKS));
        if (player.tickCount % interval == 0) {
            BlackwhipTags.tick(player);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;

        // The idea is that if this method runs, it means that the power was disabled or removed, so remove all active tags
        BlackwhipTags.clearTags(player);
    }
}