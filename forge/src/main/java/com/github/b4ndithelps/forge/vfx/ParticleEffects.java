package com.github.b4ndithelps.forge.vfx;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A collection of special effects that can be used throughout this library and any addon.
 */
public class ParticleEffects {

    public static void plusUltraVfx(Player player) {
        Level level = player.level();

        // Spawn particles on the server side
        if (level instanceof ServerLevel serverLevel) {
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION,
                    x, y, z,
                    10,
                    2.0, 2.0, 2.0,
                    0.1
            );

            serverLevel.sendParticles(
                    ParticleTypes.FLAME,
                    x, y, z,
                    50,
                    1.0, 2.0, 1.0,
                    0.3
            );
        }


    }

    public static void powersDisabledVfx(Player player) {
        Level level = player.level();

        // Spawn particles on the server side
        if (level instanceof ServerLevel serverLevel) {
            double x = player.getX();
            double y = player.getY() + 1.0;
            double z = player.getZ();

            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    x, y, z,
                    20,
                    0.5, 0.5, 0.5,
                    0.01
            );
        }


    }
}
