package com.github.b4ndithelps.forge.abilities.blackwhip;

import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.BlackwhipAuraPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("removal")
public class BlackwhipAuraAbility extends Ability {

    public static final PalladiumProperty<Integer> TENTACLE_COUNT = new IntegerProperty("tentacle_count").configurable("Number of aura tentacles");
    public static final PalladiumProperty<Float> LENGTH = new FloatProperty("length").configurable("Approximate tentacle length");
    public static final PalladiumProperty<Float> CURVE = new FloatProperty("curve").configurable("How much tentacles arc (visual only)");
    public static final PalladiumProperty<Float> THICKNESS = new FloatProperty("thickness").configurable("Base thickness for aura ribbons");
    public static final PalladiumProperty<Float> JAGGEDNESS = new FloatProperty("jaggedness").configurable("Jagged/noise amplitude along tentacles");
    public static final PalladiumProperty<Float> ORBIT_SPEED = new FloatProperty("orbit_speed").configurable("Orbit speed around player");

    public BlackwhipAuraAbility() {
        super();
        this.withProperty(TENTACLE_COUNT, 6)
                .withProperty(LENGTH, 4.0F)
                .withProperty(CURVE, 0.80F)
                .withProperty(THICKNESS, 1.0F)
                .withProperty(JAGGEDNESS, 0.12F)
                .withProperty(ORBIT_SPEED, 0.5F);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;
        long seed = ThreadLocalRandom.current().nextLong();
        BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BlackwhipAuraPacket(
                        player.getId(),
                        true,
                        Math.max(1, entry.getProperty(TENTACLE_COUNT)),
                        Math.max(0.5F, entry.getProperty(LENGTH)),
                        Math.max(0.0F, entry.getProperty(CURVE)),
                        Math.max(0.1F, entry.getProperty(THICKNESS)),
                        Math.max(0.0F, entry.getProperty(JAGGEDNESS)),
                        Math.max(0.0F, entry.getProperty(ORBIT_SPEED)),
                        seed
                ));
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        BQLNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BlackwhipAuraPacket(player.getId(), false, 0, 0, 0, 0, 0, 0, 0L));
    }
}