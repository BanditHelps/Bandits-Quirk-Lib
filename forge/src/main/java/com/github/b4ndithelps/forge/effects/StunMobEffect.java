package com.github.b4ndithelps.forge.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class StunMobEffect extends MobEffect {

    public StunMobEffect() {
        // Color: 0xFFD700 (gold), Category: HARMFUL
        super(MobEffectCategory.HARMFUL, 0xFFD700);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;

        // If it's a player, use the existing movement restriction system
        if (entity instanceof ServerPlayer player) {
            // Add the movement restriction tag if not already present
            if (!player.getTags().contains("Bql.RestrictMove")) {
                player.addTag("Bql.RestrictMove");
            }
            
            // Only restrict horizontal movement and upward movement, allow falling
            Vec3 currentVelocity = player.getDeltaMovement();
            double yVelocity = Math.min(currentVelocity.y, 0.0); // Only allow downward movement (falling)
            player.setDeltaMovement(0.0, yVelocity, 0.0);
            
            // Prevent any residual velocity from previous ticks
            player.hurtMarked = false;
        } else {
            // For non-players, restrict horizontal movement but allow falling
            Vec3 currentVelocity = entity.getDeltaMovement();
            double yVelocity = Math.min(currentVelocity.y, 0.0); // Only allow downward movement
            entity.setDeltaMovement(0.0, yVelocity, 0.0);
        }

        // Don't reset fall distance since we want falling to work normally
        
        // Spawn visual effects every 10 ticks (half second) to indicate stunned state
        if (entity.tickCount % 10 == 0) {
            spawnStunEffects(entity);
        }
    }

    private void spawnStunEffects(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        Vec3 pos = entity.position().add(0, entity.getBbHeight() + 0.5, 0);
        RandomSource random = entity.getRandom();

        // Spawn enchantment glint particles in a circle around the entity's head
        for (int i = 0; i < 6; i++) {
            double angle = (i / 6.0) * 2 * Math.PI;
            double offsetX = Math.cos(angle) * 0.6;
            double offsetZ = Math.sin(angle) * 0.6;
            
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    pos.x + offsetX, pos.y, pos.z + offsetZ,
                    1, 0, -0.1, 0, 0.05);
        }

        // Occasionally spawn additional effects
        if (random.nextFloat() < 0.3f) {
            // Spawn a few crit particles for extra visual impact
            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.4;
                double offsetY = (random.nextDouble() - 0.5) * 0.4;
                double offsetZ = (random.nextDouble() - 0.5) * 0.4;
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        1, 0, 0, 0, 0.1);
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Tick every tick to ensure movement is consistently prevented
        return true;
    }
}