package com.github.b4ndithelps.forge.abilities;

import net.minecraft.world.entity.LivingEntity;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.BooleanProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * Passive marker ability for Twin Impact. When enabled, server-side events will
 * record the player's most recent impact (entity hurt or block attack) so a
 * later trigger can cause a stronger, remote second impact.
 */
@SuppressWarnings("removal")
public class TwinImpactMarkAbility extends Ability {

    public static final PalladiumProperty<Boolean> STORE_ENTITIES = new BooleanProperty("store_entities").configurable("Whether entity hits create a Twin Impact mark");
    public static final PalladiumProperty<Boolean> STORE_BLOCKS = new BooleanProperty("store_blocks").configurable("Whether block attacks create a Twin Impact mark");
    public static final PalladiumProperty<Integer> EXPIRE_TICKS = new IntegerProperty("expire_ticks").configurable("How long the mark remains valid (in ticks)");

    public TwinImpactMarkAbility() {
        super();
        this.withProperty(STORE_ENTITIES, true)
            .withProperty(STORE_BLOCKS, true)
            .withProperty(EXPIRE_TICKS, 200);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        // No-op: actual mark recording is handled by TwinImpactEvents
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        // Passive ability: nothing to do per tick
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        // No-op
    }

    @Override
    public String getDocumentationDescription() {
        return "Marks the last entity or block you impacted. Another ability can remotely trigger a much stronger second impact on that mark.";
    }
}