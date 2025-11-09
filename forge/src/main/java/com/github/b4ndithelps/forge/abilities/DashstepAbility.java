package com.github.b4ndithelps.forge.abilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.IntegerProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * Instant horizontal blink in the look direction. Vertical motion is suppressed.
 * Distance is configurable; teleports instantly to the furthest safe spot and spawns particles along the path.
 */
@SuppressWarnings("removal")
public class DashstepAbility extends Ability {

    // Configurable
    public static final PalladiumProperty<Float> DISTANCE = new FloatProperty("distance").configurable("Total horizontal distance to travel when activated");
    public static final PalladiumProperty<Integer> TRAIL_PARTICLES_PER_TICK = new IntegerProperty("trail_particles_per_tick").configurable("How many trail particles to spawn along the path");

    public DashstepAbility() {
        super();
        this.withProperty(DISTANCE, 6.0F)
            .withProperty(TRAIL_PARTICLES_PER_TICK, 10);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        // Only horizontal component
        Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
        if (horizontal.lengthSqr() < 1.0e-6) return;
        Vec3 dir = horizontal.normalize();

        float distance = Math.max(0.0F, entry.getProperty(DISTANCE));
        if (distance <= 0.0F) return;

        // Find furthest safe point using a horizontal block ray and collision check
        Vec3 start = new Vec3(player.getX(), player.getY(0.5), player.getZ());
        Vec3 desiredEnd = start.add(dir.scale(distance));

        BlockHitResult hit = player.level().clip(new ClipContext(start, desiredEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDist = distance;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            double hitDist = start.distanceTo(hit.getLocation());
            double margin = Math.max(0.3, player.getBbWidth() * 0.6);
            maxDist = Math.max(0.0, Math.min(distance, hitDist - margin));
        }

        Vec3 current = player.position();
        Vec3 target = current.add(dir.scale(maxDist));

        // Back off slightly if target collides with world
        if (!serverLevel.noCollision(player.getBoundingBox().move(target.subtract(current)))) {
            double backStep = 0.2;
            double testDist = maxDist;
            while (testDist > 0.0) {
                Vec3 t = current.add(dir.scale(testDist));
                if (serverLevel.noCollision(player.getBoundingBox().move(t.subtract(current)))) { target = t; break; }
                testDist -= backStep;
            }
        }

        // Particles along the path (before teleport)
        int count = Math.max(0, entry.getProperty(TRAIL_PARTICLES_PER_TICK));
        int segments = Math.max(1, (int)Math.ceil(maxDist * 3.0));
        double yMid = player.getY(0.5);
        for (int i = 0; i <= segments; i++) {
            double t = (double)i / (double)segments;
            Vec3 p = new Vec3(
                    current.x + (target.x - current.x) * t,
                    yMid,
                    current.z + (target.z - current.z) * t
            );
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, Math.max(1, count / Math.max(1, segments / 4)), 0.05, 0.02, 0.05, 0.0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, p.x, p.y - 0.25, p.z, Math.max(1, count / Math.max(1, segments / 3)), 0.08, 0.01, 0.08, 0.0);
        }

        // Activation burst at start and end
        serverLevel.sendParticles(ParticleTypes.CRIT, current.x, player.getY(0.5), current.z, 10, 0.15, 0.05, 0.15, 0.2);
        serverLevel.sendParticles(ParticleTypes.CRIT, target.x, player.getY(0.5), target.z, 10, 0.15, 0.05, 0.15, 0.2);

        // Teleport instantly (horizontal only)
        player.teleportTo(serverLevel, target.x, current.y, target.z, player.getYRot(), player.getXRot());
    }

    @Override
    public String getDocumentationDescription() {
        return "Instantly blinks horizontally in the look direction up to the configured distance, avoiding collisions and spawning particles along the path.";
    }
}