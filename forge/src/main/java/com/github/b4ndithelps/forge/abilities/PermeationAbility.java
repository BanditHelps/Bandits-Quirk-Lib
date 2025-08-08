package com.github.b4ndithelps.forge.abilities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
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
    public static final PalladiumProperty<String> MODE = new StringProperty("mode").configurable("Permeation mode. Supported: 'full_body'");
    public static final PalladiumProperty<Float> DESCENT_SPEED = new FloatProperty("descent_speed").configurable("Blocks per tick to descend while permeating");
    public static final PalladiumProperty<Float> POP_STRENGTH = new FloatProperty("pop_strength").configurable("Upward velocity applied when exiting permeation");
    public static final PalladiumProperty<Boolean> APPLY_BLINDNESS = new BooleanProperty("apply_blindness").configurable("Apply blindness while permeating");
    public static final PalladiumProperty<Boolean> APPLY_WATER_BREATHING = new BooleanProperty("apply_water_breathing").configurable("Apply water breathing while permeating");

    public PermeationAbility() {
        super();
        this.withProperty(MODE, "full_body")
            .withProperty(DESCENT_SPEED, 0.6F)
            .withProperty(POP_STRENGTH, 1.0F)
            .withProperty(APPLY_BLINDNESS, true)
            .withProperty(APPLY_WATER_BREATHING, true);
    }

    @Override
    public void firstTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!enabled) return;

        String mode = entry.getProperty(MODE).toLowerCase();
        if ("full_body".equals(mode)) {
            // Initial effects when entering permeation
            applyActiveEffects(player, entry);

            // Nudge down immediately for instant feedback
            float speed = entry.getProperty(DESCENT_SPEED);
            teleportDown(player, speed);

            // Muffle sound for feedback
            if (entity.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.3f, 0.6f);
            }
        }
    }

    @Override
    public void tick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;
        if (!enabled) return;

        String mode = entry.getProperty(MODE).toLowerCase();
        if ("full_body".equals(mode)) {
            // Keep falling through terrain by bypassing collisions
            float speed = entry.getProperty(DESCENT_SPEED);
            teleportDown(player, speed);

            // Prevent fall damage accumulation and keep movement stable
            player.fallDistance = 0.0F;
            player.setDeltaMovement(Vec3.ZERO);

            // Maintain effects while active using short refresh durations
            applyActiveEffects(player, entry);
        }
    }

    @Override
    public void lastTick(LivingEntity entity, AbilityInstance entry, IPowerHolder holder, boolean enabled) {
        if (!(entity instanceof ServerPlayer player)) return;

        String mode = entry.getProperty(MODE).toLowerCase();
        if ("full_body".equals(mode)) {
            // Pop the player back to the surface above their current X/Z
            if (entity.level() instanceof ServerLevel level) {
                BlockPos target = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(player.getBlockX(), 0, player.getBlockZ()));

                // Teleport just above the surface
                player.teleportTo(target.getX() + 0.5, target.getY() + 0.1, target.getZ() + 0.5);

                // Apply upward pop velocity
                float pop = entry.getProperty(POP_STRENGTH);
                player.setDeltaMovement(0.0, Math.max(0.0F, pop), 0.0);
                player.connection.send(new ClientboundSetEntityMotionPacket(player));

                // Feedback
                level.playSound(null, player.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 0.6f, 1.2f);
            }

            // Cleanup immediate effects (durations are short, but ensure removal)
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.WATER_BREATHING);
        }
    }

    private void teleportDown(ServerPlayer player, float speed) {
        double x = player.getX();
        double y = player.getY() - Math.max(0.05F, speed);
        double z = player.getZ();

        // Clamp to world min height to avoid invalid positions
        int minY = player.serverLevel().getMinBuildHeight();
        if (y < minY + 1) {
            y = minY + 1;
        }

        player.teleportTo(x, y, z);
    }

    private void applyActiveEffects(ServerPlayer player, AbilityInstance entry) {
        if (entry.getProperty(APPLY_BLINDNESS)) {
            // Very short duration and refreshed every tick; hidden particles
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 8, 0, true, false));
        }
        if (entry.getProperty(APPLY_WATER_BREATHING)) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 8, 0, true, false));
        }
    }

    @Override
    public String getDocumentationDescription() {
        return "Permeation that allows the user to phase through terrain. In 'full_body' mode, toggling on pulls the player straight into the ground, applying blindness and water breathing. Toggling off launches the player back up to the surface with a buoyant pop.";
    }
}
