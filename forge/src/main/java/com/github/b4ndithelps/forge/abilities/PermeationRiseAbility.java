package com.github.b4ndithelps.forge.abilities;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.threetag.palladium.power.IPowerHolder;
import net.threetag.palladium.power.ability.Ability;
import net.threetag.palladium.power.ability.AbilityInstance;
import net.threetag.palladium.util.property.BooleanProperty;
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
    public static final PalladiumProperty<Boolean> APPLY_BLINDNESS = new BooleanProperty("apply_blindness").configurable("Apply blindness while permeating");


    public PermeationRiseAbility() {
        super();
        this.withProperty(MAX_UPWARD_VELOCITY, 0.35F)
            .withProperty(HORIZONTAL_DRAG, 1.0F)
            .withProperty(APPLY_BLINDNESS, true);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;
        if (!(entity instanceof ServerPlayer player)) return;

        // Ensure no-clip while rising
        player.noPhysics = true;
        player.fallDistance = 0.0F;

        applyActiveEffects(player, entry);

        // Apply short effect so client mirrors no-clip
        if (!player.getTags().contains("Bql.PermeateActive")) {
            player.addTag("Bql.PermeateActive");
        }

        // Feedback sound
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.5f, 1.3f);
        }

    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!enabled) return;

        // Client and server conflict here, registering collisions that shouldn't happen
        // So, we make the client have noPhysics to avoid conflicts
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

        Vec3 prev = player.getDeltaMovement();
        float horDrag = Math.max(0.0F, Math.min(1.0F, entry.getProperty(HORIZONTAL_DRAG)));
        // Preserve horizontal momentum; optionally damp using configurable drag
        Vec3 next = new Vec3(prev.x * horDrag, vy * 2, prev.z * horDrag);
        next = this.adjustMoveToRespectBedrock(player, next);
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

        if (entry.getEnabledTicks() % 20 == 0) {
            applyActiveEffects(player, entry);
        }

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

        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        player.removeEffect(MobEffects.BLINDNESS);
    }


    private void applyActiveEffects(ServerPlayer player, AbilityInstance entry) {
        if (entry.getProperty(APPLY_BLINDNESS)) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 10, true, false));
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Handles the upward rise phase of permeation: while active, keeps the player intangible and moves them to the nearest air pocket above, then pops slightly on exit.";
    }
    
    private boolean intersectsBedrock(ServerPlayer player, AABB box) {
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (player.level().getBlockState(pos).is(Blocks.BEDROCK)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Vec3 adjustMoveToRespectBedrock(ServerPlayer player, Vec3 proposedMove) {
        boolean insideNow = intersectsBedrock(player, player.getBoundingBox());
        if (insideNow) {
            if (proposedMove.y > 0.0) {
                return new Vec3(0.0, proposedMove.y, 0.0);
            } else {
                return Vec3.ZERO;
            }
        }

        AABB moved = player.getBoundingBox().move(proposedMove);
        if (!intersectsBedrock(player, moved)) {
            return proposedMove;
        }

        Vec3 noY = new Vec3(proposedMove.x, 0.0, proposedMove.z);
        if (!intersectsBedrock(player, player.getBoundingBox().move(noY))) {
            return noY;
        }

        Vec3 noXZ = new Vec3(0.0, proposedMove.y, 0.0);
        if (!intersectsBedrock(player, player.getBoundingBox().move(noXZ))) {
            return noXZ;
        }

        return Vec3.ZERO;
    }
}