package com.github.b4ndithelps.forge.abilities;


import com.github.b4ndithelps.forge.network.BQLNetwork;
import com.github.b4ndithelps.forge.network.PlayerAnimationPacket;
import com.github.b4ndithelps.forge.systems.BodyStatusHelper;
import com.github.b4ndithelps.forge.systems.QuirkFactorHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import org.joml.Vector3f;

public class SuperBurstExplosionGeneAbility extends Ability {
    private static final float FACTOR_SCALING = 10F;

    public SuperBurstExplosionGeneAbility() {
        super();
    }


    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        if (!enabled) {
            return;
        }

        float charge = BodyStatusHelper.getCustomFloat(player, "chest", "super_burst_charge");
        if (charge > 0) {
                BQLNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new PlayerAnimationPacket("x")
                );
                executeBurst(player, serverLevel);
        }

    }


    private void executeBurst(ServerPlayer player, ServerLevel level) {
        float charge = BodyStatusHelper.getCustomFloat(player, "chest", "super_burst_charge");
        float factor = (float) (QuirkFactorHelper.getQuirkFactor(player)+1) * charge/20;
        player.hurt(level.damageSources().generic(), factor*FACTOR_SCALING);
        level.explode(
                player,                  // entity that caused it (null = no source)
                player.getX(),           // x
                player.getY() + 1,           // y
                player.getZ(),           // z
                factor * FACTOR_SCALING * 1.5F,                    // factor * FACTOR_SCALING * 1.5F
                Level.ExplosionInteraction.TNT // what kind of explosion (controls block damage)
        );
        float playersHealthResistance = (float) Math.max(0.01 ,1 - ((player.getMaxHealth() - factor*FACTOR_SCALING) / player.getMaxHealth()));
        BodyStatusHelper.damageAll(player, playersHealthResistance*50);
        BodyStatusHelper.setCustomFloat(player, "chest", "super_burst_charge", 0);

        double x = player.getX();
        double y = player.getY() + 1.0; // slightly above feet
        double z = player.getZ();

        RandomSource random = player.level().random;

        for (int i = 0; i < (charge/20) * 3; i++) { // number of particles
            double randomN = random.nextDouble() * (charge/10) * 3; // random angle

            // Random speed multiplier
            double speed = 0.2 + random.nextDouble() * 0.5; // 0.2–0.7


            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0F, 0.5F, 0.0F), 1.0F), // orange color, size=1
                    x, y, z,
                    10, randomN, randomN, randomN, 1
            );
        }
    }


    @Override
    public String getDocumentationDescription() {
        return "Burst end ability";
    }
}
