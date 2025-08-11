package com.github.b4ndithelps.forge.abilities;

import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;

/**
 * Handles the upward rise phase of permeation as a standalone ability.
 * Triggers when the entity has the tag Bql.PermeateRise and stores control
 * data in the entity's persistent data under keys:
 *  - Bql.PermeateRiseSpeed (double)
 *  - Bql.PermeateTargetY (double)
 *  - Bql.PermeateFreeTicks (int)
 */
public class PermeationRiseAbility extends Ability {

    public static final PalladiumProperty<Float> MAX_UPWARD_VELOCITY = new FloatProperty("max_upward_velocity").configurable("Max vertical speed while rising");
    public static final PalladiumProperty<Float> HORIZONTAL_DRAG = new FloatProperty("horizontal_drag").configurable("Horizontal drag while rising (1.0 = no slow)");

    public PermeationRiseAbility() {
        super();
        this.withProperty(MAX_UPWARD_VELOCITY, 0.35F)
            .withProperty(HORIZONTAL_DRAG, 1.0F);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;

        // Ensure no-clip while rising
        player.noPhysics = true;
        player.fallDistance = 0.0F;

        // Apply short effect so client mirrors no-clip
        if (!player.getTags().contains("Bql.PermeateActive")) {
            player.addTag("Bql.PermeateActive");
        }

        // Feedback sound
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.5f, 1.3f);
        }
        BanditsQuirkLibForge.LOGGER.info("[PermeationRise] Start for {}: y={} targetY={} speed={}",
                player.getGameProfile().getName(), String.format("%.2f", player.getY()),
                String.format("%.2f", player.getPersistentData().getDouble("Bql.PermeateTargetY")),
                String.format("%.2f", player.getPersistentData().getDouble("Bql.PermeateRiseSpeed")));
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;

        if (entity.level().isClientSide) {
            entity.noPhysics = true;
            entity.fallDistance = 0.0F;
            return;
        }

        if (!(entity instanceof ServerPlayer player)) return;
        if (!player.getTags().contains("Bql.PermeateRise")) return; // only act while rising

        double desiredUp = Math.max(0.05D, player.getPersistentData().getDouble("Bql.PermeateRiseSpeed"));
        double targetY = player.getPersistentData().getDouble("Bql.PermeateTargetY");
        double maxVy = Math.max(0.1D, entry.getProperty(MAX_UPWARD_VELOCITY));

        player.noPhysics = true;
        player.fallDistance = 0.0F;

        // Compute this-tick vertical velocity toward target
        double remaining = Math.max(0.0D, targetY - player.getY());
        double vy = Math.min(maxVy, Math.max(desiredUp, Math.min(remaining, maxVy)));

        BanditsQuirkLibForge.LOGGER.info("[PermeationRise] Velocity calc: remaining={} desiredUp={} maxVy={} final_vy={}",
                remaining, desiredUp, maxVy, vy);

        Vec3 prev = player.getDeltaMovement();
        float horDrag = Math.max(0.0F, Math.min(1.0F, entry.getProperty(HORIZONTAL_DRAG)));
        // Preserve horizontal momentum; optionally damp using configurable drag
        Vec3 next = new Vec3(prev.x * horDrag, vy * 2, prev.z * horDrag);
        player.setDeltaMovement(next.x, next.y, next.z);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        boolean reachedTargetY = player.getY() >= targetY - 0.001;
        boolean free = player.level().noCollision(player, player.getBoundingBox().inflate(-0.02));
        int freeTicks = player.getPersistentData().getInt("Bql.PermeateFreeTicks");
        if (free) {
            freeTicks++;
            player.getPersistentData().putInt("Bql.PermeateFreeTicks", freeTicks);
        } else {
            player.getPersistentData().putInt("Bql.PermeateFreeTicks", 0);
        }

        BanditsQuirkLibForge.LOGGER.info("[PermeationRise] Tick {}: y={}/target={} vy={} free={} freeTicks={}",
                player.getGameProfile().getName(),
                String.format("%.2f", player.getY()),
                String.format("%.2f", targetY),
                String.format("%.2f", next.y),
                free, freeTicks);

        if (reachedTargetY || freeTicks >= 2) {
            // Finish rise
            player.noPhysics = false;
            player.removeTag("Bql.PermeateRise");
            player.removeTag("Bql.PermeateActive");
            player.getPersistentData().remove("Bql.PermeateRiseSpeed");
            player.getPersistentData().remove("Bql.PermeateTargetY");
            player.getPersistentData().remove("Bql.PermeateFreeTicks");

            // Exit pop
            Vec3 end = player.getDeltaMovement();
            double endPop = Math.min(0.42D, Math.max(0.12D, desiredUp * 0.6D));
            player.setDeltaMovement(end.x * 0.5, endPop, end.z * 0.5);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));

            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.3f, 1.2f);
            }

            BanditsQuirkLibForge.LOGGER.info("[PermeationRise] End for {} at y={} (reachedTargetY={} freeTicks={})",
                    player.getGameProfile().getName(), String.format("%.2f", player.getY()), reachedTargetY, freeTicks);
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Handles the upward rise phase of permeation: while active, keeps the player intangible and moves them to the nearest air pocket above, then pops slightly on exit.";
    }
}


