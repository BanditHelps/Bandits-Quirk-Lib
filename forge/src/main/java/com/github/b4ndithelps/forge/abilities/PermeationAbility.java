package com.github.b4ndithelps.forge.abilities;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import com.github.b4ndithelps.forge.BanditsQuirkLibForge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.BooleanProperty;
import net.threetag.palladium.util.property.FloatProperty;
import net.threetag.palladium.util.property.PalladiumProperty;
import net.threetag.palladium.util.property.StringProperty;

public class PermeationAbility extends Ability {

    // Configurable properties
    public static final PalladiumProperty<Float> DESCENT_SPEED = new FloatProperty("descent_speed").configurable("Gravity multiplier while permeating (1.0 = normal)");
    public static final PalladiumProperty<Float> POP_STRENGTH = new FloatProperty("pop_strength").configurable("Upward velocity applied when exiting permeation");
    public static final PalladiumProperty<Boolean> APPLY_BLINDNESS = new BooleanProperty("apply_blindness").configurable("Apply blindness while permeating");
    public static final PalladiumProperty<Float> HORIZONTAL_DRAG = new FloatProperty("horizontal_drag").configurable("Horizontal drag while permeating (1.0 = no slow)");

    public PermeationAbility() {
        super();
        this.withProperty(DESCENT_SPEED, 1.0F)
            .withProperty(POP_STRENGTH, 1.0F)
            .withProperty(APPLY_BLINDNESS, true)
            .withProperty(HORIZONTAL_DRAG, 1.0F);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;

        // Client-side: mirror spectator no-clip so local prediction doesn't collide
        if (entity.level().isClientSide) {
            entity.noPhysics = true;
            entity.fallDistance = 0.0F;
            return;
        }

        if (!(entity instanceof ServerPlayer player)) return;

        // Unified: initial effects and start sinking
        applyActiveEffects((ServerPlayer) entity, entry);

        player.noPhysics = true;
        // Reset all the upward movement stuff here, to early cancel it
        player.addTag("Bql.PermeateActive");
        player.removeTag("Bql.PermeateRise");
        player.getPersistentData().remove("Bql.PermeateRiseSpeed");
        player.getPersistentData().remove("Bql.PermeateTargetY");
        player.getPersistentData().remove("Bql.PermeateFreeTicks");
        // Small downward nudge for immediate feedback, then let gravity take over
        float speed = entry.getProperty(DESCENT_SPEED);
        Vec3 current = player.getDeltaMovement();
        double nudge = Math.max(0.08, 0.08 * 0.5 * speed);
        // Preserve horizontal momentum on activation; only adjust vertical
        player.setDeltaMovement(current.x, Math.min(current.y, -nudge), current.z);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        // Sound feedback
        if (entity.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.3f, 0.6f);
        }

    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;

        // Client-side: keep no-clip active like spectator so the client doesn't resist server updates
        if (entity.level().isClientSide) {
            entity.noPhysics = true;
            entity.fallDistance = 0.0F;
            return;
        }

        if (!(entity instanceof ServerPlayer player)) return;

        // Unified: maintain noclip and sink with gravity
        player.noPhysics = true;
        // Ensure active tag persists for client-side mirror
        if (!player.getTags().contains("Bql.PermeateActive")) {
            player.addTag("Bql.PermeateActive");
        }

        // Apply manual gravity similar to vanilla when noPhysics is enabled
        Vec3 prev = player.getDeltaMovement();
        float horDrag = Math.max(0.0F, Math.min(1.0F, entry.getProperty(HORIZONTAL_DRAG)));
        double gravity = 0.08D * Math.max(0.05F, entry.getProperty(DESCENT_SPEED));
        double newY = (prev.y - gravity) * 0.98D; // vertical drag only
        // Clamp terminal velocity roughly around vanilla
        double terminal = -3.92D * entry.getProperty(DESCENT_SPEED);
        if (newY < terminal) newY = terminal;
        // Preserve horizontal movement; only adjust vertical motion
        player.setDeltaMovement(prev.x * horDrag, newY, prev.z * horDrag);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));


        // Prevent fall damage accumulation
        player.fallDistance = 0.0F;

        // Maintain effects while active using short refresh durations
        if (entry.getEnabledTicks() % 20 == 0) {
            applyActiveEffects(player, entry);
        }


        // Oxygen handling: while phasing inside blocks (not in free air pocket), drain air; otherwise restore
        boolean inFreeSpace = player.level().noCollision(player, player.getBoundingBox().inflate(-0.02));
        if (!inFreeSpace) {
            int air = player.getAirSupply();
            if (air > -100) {
                player.setAirSupply(air - 8);
            }
        }

    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        // No special client handling here; stopping the ability will let vanilla reset noPhysics next tick
        if (!(entity instanceof ServerPlayer player)) return;

        // Unified: begin upward buoyant rise until target free space above; handled on player tick via tag
        float pop = Math.max(0.1F, entry.getProperty(POP_STRENGTH));
        player.noPhysics = true; // keep phasing while rising
        player.setDeltaMovement(0.0, pop, 0.0);
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        // Mark rising state and store speed
        player.addTag("Bql.PermeateRise");
        player.getPersistentData().putDouble("Bql.PermeateRiseSpeed", pop);
        // Compute the target Y to rise to: nearest safe air space with ground beneath, above current position
        double targetY = computeRiseTargetY(player);
        player.getPersistentData().putDouble("Bql.PermeateTargetY", targetY);

        // Keep active tag during rise so both client and server confidently mirror no-clip
        if (!player.getTags().contains("Bql.PermeateActive")) {
            player.addTag("Bql.PermeateActive");
        }

        if (entity.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.5f, 1.3f);
        }

    }


    private void applyActiveEffects(ServerPlayer player, AbilityInstance entry) {
        if (entry.getProperty(APPLY_BLINDNESS)) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 10, true, false));
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Permeation that allows the user to phase through terrain: sinks while active and springs up to the nearest free space on deactivation.";
    }

    private double computeRiseTargetY(ServerPlayer player) {
        var level = player.serverLevel();
        var aabb = player.getBoundingBox();
        int minY = Mth.floor(aabb.minY);
        int maxBuildY = level.getMaxBuildHeight() - 2; // leave headroom
        int x = Mth.floor(player.getX());
        int z = Mth.floor(player.getZ());

        // Scan upward from feet to world ceiling
        for (int y = minY; y <= maxBuildY; y++) {
            BlockPos feetPos = new BlockPos(x, y, z);
            BlockPos headPos = feetPos.above();
            BlockPos belowPos = feetPos.below();

            // Require two air blocks for player space
            if (!level.isEmptyBlock(feetPos) || !level.isEmptyBlock(headPos)) {
                continue;
            }

            // Require solid-ish ground beneath
            var belowState = level.getBlockState(belowPos);
            if (belowState.isAir() || belowState.getCollisionShape(level, belowPos).isEmpty()) {
                continue;
            }

            // Verify player's AABB fits at this Y
            double desiredMinY = y;
            var movedBox = aabb.move(0.0, desiredMinY - aabb.minY, 0.0);
            if (!level.noCollision(player, movedBox.inflate(-0.02))) {
                continue;
            }

           return desiredMinY;
        }

        // Fallback: if no suitable ground found, move to just below build height keeping space clear
        return Math.max(minY, maxBuildY);
    }
}
