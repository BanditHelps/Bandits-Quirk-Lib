package com.github.b4ndithelps.forge.particle.custom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ChargingDustParticle extends TextureSheetParticle {
    private final Player player;

    protected ChargingDustParticle(ClientLevel world, double x, double y, double z, Player player, double vx, double vy, double vz, SpriteSet sprites) {
        super(world, x, y, z, vx, vy, vz);
        this.player = player;
        this.lifetime = 50; // ticks before auto-despawn
        this.gravity = 0.0F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.alpha = 0;
        this.pickSprite(sprites);
//        this.setSpriteFromAge(sprites);
//        this.setColor(0.9f, 0.8f, 0.5f);
//        this.quadSize = 0.1f;
    }

    @Override
    public void tick() {
        super.tick();

        if (player == null || !player.isAlive()) {
            this.remove();
            return;
        }

        if (this.age < 16) {
            fadeIn();
            return;
        }

        // Move toward player
        Vec3 toPlayer = player.position().subtract(this.x, this.y-1, this.z);
        double distance = toPlayer.length();

        if (distance < 0.3) {
            this.remove(); // reached player
            return;
        }

        Vec3 direction = toPlayer.normalize().scale(0.2); // adjust speed
        this.xd = direction.x;
        this.yd = direction.y;
        this.zd = direction.z;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public void fadeIn() {
        this.alpha = Math.min(1, age / 10);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel world, double x, double y, double z,
                                       double dx, double dy, double dz) {
            Player player = Minecraft.getInstance().player;
            ChargingDustParticle particle = new ChargingDustParticle(world, x, y, z, player, dx, dy, dz, spriteSet);
            particle.pickSprite(spriteSet);
            return particle;
        }
    }
}
