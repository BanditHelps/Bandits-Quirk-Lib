package com.github.b4ndithelps.forge.particle.custom;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public class RisingDustParticle extends TextureSheetParticle {
    protected RisingDustParticle(ClientLevel level, double x, double y, double z,
                                 double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.gravity = 0.0F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.alpha = 0;
        this.lifetime = 30; // ticks
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        fadeIn(); // keep moving up
    }

    public void fadeIn() {
        this.alpha = Math.min(1, (1/(float)lifetime) * age * 10);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }


        public Particle createParticle(SimpleParticleType data, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RisingDustParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}